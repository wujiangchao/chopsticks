package com.chopsticks.server.netty;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.chopsticks.Chopsticks;
import com.chopsticks.Environment;
import com.chopsticks.event.BeanProcessor;
import com.chopsticks.event.EventType;
import com.chopsticks.ioc.DynamicContext;
import com.chopsticks.ioc.Ioc;
import com.chopsticks.ioc.annotation.Bean;
import com.chopsticks.ioc.bean.BeanDefine;
import com.chopsticks.ioc.bean.ClassInfo;
import com.chopsticks.ioc.bean.OrderComparator;
import com.chopsticks.kit.ChopsticksKit;
import com.chopsticks.kit.NamedThreadFactory;
import com.chopsticks.kit.ReflectKit;
import com.chopsticks.kit.StringKit;
import com.chopsticks.mvc.Const;
import com.chopsticks.mvc.WebContext;
import com.chopsticks.mvc.annotation.Path;
import com.chopsticks.mvc.annotation.UrlPattern;
import com.chopsticks.mvc.handler.DefaultExceptionHandler;
import com.chopsticks.mvc.handler.ExceptionHandler;
import com.chopsticks.mvc.hook.WebHook;
import com.chopsticks.mvc.route.RouteBuilder;
import com.chopsticks.mvc.route.RouteMatcher;
import com.chopsticks.mvc.ui.template.DefaultEngine;
import com.chopsticks.server.Server;
import com.chopsticks.watcher.EnvironmentWatcher;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ResourceLeakDetector;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import static com.chopsticks.mvc.Const.*;
/**
 * @author biezhi
 * 2017/5/31
 */
@Slf4j
public class NettyServer implements Server {

    private Chopsticks          chopsticks;
    private Environment         environment;
    private EventLoopGroup      bossGroup;
    private EventLoopGroup      workerGroup;
    private Channel             channel;
    private RouteBuilder        routeBuilder;
    private List<BeanProcessor> processors;

    @Override
    public void start(Chopsticks chopsticks, String[] args) throws Exception {
        this.chopsticks = chopsticks;
        this.environment = chopsticks.environment();
        this.processors = chopsticks.processors();

        long initStart = System.currentTimeMillis();
        log.info("Environment: jdk.version    => {}", System.getProperty("java.version"));
        log.info("Environment: user.dir       => {}", System.getProperty("user.dir"));
        log.info("Environment: java.io.tmpdir => {}", System.getProperty("java.io.tmpdir"));
        log.info("Environment: user.timezone  => {}", System.getProperty("user.timezone"));
        log.info("Environment: file.encoding  => {}", System.getProperty("file.encoding"));
        log.info("Environment: classpath      => {}", CLASSPATH);

        this.loadConfig(args);
        this.initConfig();

        WebContext.init(chopsticks, "/");

        this.initIoc();

        this.shutdownHook();

        this.watchEnv();

        this.startServer(initStart);

    }

    private void initIoc() {
        RouteMatcher routeMatcher = chopsticks.routeMatcher();
        routeMatcher.initMiddleware(chopsticks.middleware());

        routeBuilder = new RouteBuilder(routeMatcher);

        chopsticks.scanPackages().stream()
                .flatMap(DynamicContext::recursionFindClasses)
                .map(ClassInfo::getClazz)
                .filter(ReflectKit::isNormalClass)
                .forEach(this::parseCls);

        routeBuilder.register();

        this.processors.stream().sorted(new OrderComparator<>()).forEach(b -> b.preHandle(chopsticks));

        Ioc ioc = chopsticks.ioc();
        if (ChopsticksKit.isNotEmpty(ioc.getBeans())) {
            log.info("⬢ Register bean: {}", ioc.getBeans());
        }

        List<BeanDefine> beanDefines = ioc.getBeanDefines();
        if (ChopsticksKit.isNotEmpty(beanDefines)) {
            beanDefines.forEach(b -> {
                ChopsticksKit.injection(ioc, b);
                ChopsticksKit.injectionValue(environment,b);
            });
        }

        this.processors.stream().sorted(new OrderComparator<>()).forEach(b -> b.processor(chopsticks));
    }

    private void startServer(long startTime) throws Exception {

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        boolean SSL = environment.getBoolean(ENV_KEY_SSL, false);
        // Configure SSL.
        SslContext sslCtx = null;
        if (SSL) {
            String certFilePath       = environment.get(ENV_KEY_SSL_CERT, null);
            String privateKeyPath     = environment.get(ENE_KEY_SSL_PRIVATE_KEY, null);
            String privateKeyPassword = environment.get(ENE_KEY_SSL_PRIVATE_KEY_PASS, null);

            log.info("⬢ SSL CertChainFile  Path: {}", certFilePath);
            log.info("⬢ SSL PrivateKeyFile Path: {}", privateKeyPath);
            sslCtx = SslContextBuilder.forServer(new File(certFilePath), new File(privateKeyPath), privateKeyPassword).build();
        }

        // Configure the server.
        int backlog = environment.getInt(ENV_KEY_NETTY_SO_BACKLOG, 8192);

        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, backlog);
        b.option(ChannelOption.SO_REUSEADDR, true);
        b.childOption(ChannelOption.SO_REUSEADDR, true);

        int acceptThreadCount = environment.getInt(ENC_KEY_NETTY_ACCEPT_THREAD_COUNT, 0);
        int ioThreadCount     = environment.getInt(ENV_KEY_NETTY_IO_THREAD_COUNT, 0);

        // enable epoll
        if (ChopsticksKit.epollIsAvailable()) {
            log.info("⬢ Use EpollEventLoopGroup");
            b.option(EpollChannelOption.SO_REUSEPORT, true);

            NettyServerGroup nettyServerGroup = EpollKit.group(acceptThreadCount, ioThreadCount);
            this.bossGroup = nettyServerGroup.getBoosGroup();
            this.workerGroup = nettyServerGroup.getWorkerGroup();
            b.group(bossGroup, workerGroup).channel(nettyServerGroup.getSocketChannel());
        } else {
            log.info("⬢ Use NioEventLoopGroup");

            this.bossGroup = new NioEventLoopGroup(acceptThreadCount, new NamedThreadFactory("nio-boss@"));
            this.workerGroup = new NioEventLoopGroup(ioThreadCount, new NamedThreadFactory("nio-worker@"));
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
        }

        b.handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new HttpServerInitializer(sslCtx, chopsticks, bossGroup.next()));

        String address = environment.get(ENV_KEY_SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS);
        int    port    = environment.getInt(ENV_KEY_SERVER_PORT, DEFAULT_SERVER_PORT);

        channel = b.bind(address, port).sync().channel();
        String appName = environment.get(ENV_KEY_APP_NAME, "chopsticks");

        log.info("⬢ {} initialize successfully, Time elapsed: {} ms", appName, (System.currentTimeMillis() - startTime));
        log.info("⬢ chopsticks start with {}:{}", address, port);
        log.info("⬢ Open your web browser and navigate to {}://{}:{} ⚡", "http", address.replace(DEFAULT_SERVER_ADDRESS, LOCAL_IP_ADDRESS), port);

        chopsticks.eventManager().fireEvent(EventType.SERVER_STARTED, chopsticks);
    }


    private void parseCls(Class<?> clazz) {
        if (null != clazz.getAnnotation(Bean.class) || null != clazz.getAnnotation(Value.class)) {
            chopsticks.register(clazz);
        }
        if (null != clazz.getAnnotation(Path.class)) {
            if (null == chopsticks.ioc().getBean(clazz)) {
                chopsticks.register(clazz);
            }
            Object controller = chopsticks.ioc().getBean(clazz);
            routeBuilder.addRouter(clazz, controller);
        }
        if (ReflectKit.hasInterface(clazz, WebHook.class) && null != clazz.getAnnotation(Bean.class)) {
            Object     hook       = chopsticks.ioc().getBean(clazz);
            UrlPattern urlPattern = clazz.getAnnotation(UrlPattern.class);
            if (null == urlPattern) {
                routeBuilder.addWebHook(clazz, "/.*", hook);
            } else {
                Stream.of(urlPattern.values())
                        .forEach(pattern -> routeBuilder.addWebHook(clazz, pattern, hook));
            }
        }
        if (ReflectKit.hasInterface(clazz, BeanProcessor.class) && null != clazz.getAnnotation(Bean.class)) {
            this.processors.add((BeanProcessor) chopsticks.ioc().getBean(clazz));
        }
        if (isExceptionHandler(clazz)) {
            ExceptionHandler exceptionHandler = (ExceptionHandler) chopsticks.ioc().getBean(clazz);
            chopsticks.exceptionHandler(exceptionHandler);
        }
    }

    private boolean isExceptionHandler(Class<?> clazz) {
        return (null != clazz.getAnnotation(Bean.class) && (
                ReflectKit.hasInterface(clazz, ExceptionHandler.class) || clazz.getSuperclass().equals(DefaultExceptionHandler.class)));
    }

    private void watchEnv() {
        boolean watchEnv = environment.getBoolean(ENV_KEY_APP_WATCH_ENV, true);
        log.info("⬢ Watched environment: {}", watchEnv);

        if (watchEnv) {
            Thread t = new Thread(new EnvironmentWatcher());
            t.setName("watch@thread");
            t.start();
        }
    }

    private void loadConfig(String[] args) {

        String bootConf = chopsticks.environment().get(ENV_KEY_BOOT_CONF, "classpath:app.properties");

        Environment bootEnv = Environment.of(bootConf);

        if (bootEnv != null) {
            bootEnv.props().forEach((key, value) -> environment.set(key.toString(), value));
        }

        if (null != args) {
            Optional<String> envArg = Stream.of(args).filter(s -> s.startsWith(Const.TERMINAL_CHOPSTICKS_ENV)).findFirst();
            envArg.ifPresent(arg -> {
                String envName = "app-" + arg.split("=")[1] + ".properties";
                log.info("current environment file is: {}", envName);
                Environment customEnv = Environment.of(envName);
                if (customEnv != null) {
                    customEnv.props().forEach((key, value) -> environment.set(key.toString(), value));
                }
            });
        }

        chopsticks.register(environment);

        // load terminal param
        if (!ChopsticksKit.isEmpty(args)) {
            for (String arg : args) {
                if (arg.startsWith(TERMINAL_SERVER_ADDRESS)) {
                    int    pos     = arg.indexOf(TERMINAL_SERVER_ADDRESS) + TERMINAL_SERVER_ADDRESS.length();
                    String address = arg.substring(pos);
                    environment.set(ENV_KEY_SERVER_ADDRESS, address);
                } else if (arg.startsWith(TERMINAL_SERVER_PORT)) {
                    int    pos  = arg.indexOf(TERMINAL_SERVER_PORT) + TERMINAL_SERVER_PORT.length();
                    String port = arg.substring(pos);
                    environment.set(ENV_KEY_SERVER_PORT, port);
                }
            }
        }
    }

    private void initConfig() {

        if (null != chopsticks.bootClass()) {
            chopsticks.scanPackages(chopsticks.bootClass().getPackage().getName());
        }

        // print banner text
        this.printBanner();

        String statics = environment.get(ENV_KEY_STATIC_DIRS, "");
        if (StringKit.isNotBlank(statics)) {
            chopsticks.addStatics(statics.split(","));
        }

        String templatePath = environment.get(ENV_KEY_TEMPLATE_PATH, "templates");
        if (templatePath.charAt(0) == HttpConst.CHAR_SLASH) {
            templatePath = templatePath.substring(1);
        }
        if (templatePath.endsWith(HttpConst.SLASH)) {
            templatePath = templatePath.substring(0, templatePath.length() - 1);
        }
        DefaultEngine.TEMPLATE_PATH = templatePath;
    }

    private void shutdownHook() {
        Thread shutdownThread = new Thread(this::stop);
        shutdownThread.setName("shutdown@thread");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    @Override
    public void stop() {
        log.info("⬢ chopsticks shutdown ...");
        try {
            if (this.bossGroup != null) {
                this.bossGroup.shutdownGracefully();
            }
            if (this.workerGroup != null) {
                this.workerGroup.shutdownGracefully();
            }
            log.info("⬢ chopsticks shutdown successful");
        } catch (Exception e) {
            log.error("chopsticks shutdown error", e);
        }
    }

    @Override
    public void stopAndWait() {
        log.info("⬢ chopsticks shutdown ...");
        try {
            if (this.bossGroup != null) {
                this.bossGroup.shutdownGracefully().sync();
            }
            if (this.workerGroup != null) {
                this.workerGroup.shutdownGracefully().sync();
            }
            log.info("⬢ chopsticks shutdown successful");
        } catch (Exception e) {
            log.error("chopsticks shutdown error", e);
        }
    }

    @Override
    public void join() throws InterruptedException {
        channel.closeFuture().sync();
    }

    /**
     * print chopsticks start banner text
     */
    private void printBanner() {
        if (null != chopsticks.bannerText()) {
            System.out.println(chopsticks.bannerText());
        } else {
            StringBuilder text = new StringBuilder();
            text.append(Const.BANNER_TEXT);
            text.append("\r\n")
                    .append(BANNER_SPACE)
                    .append(" :: chopsticks :: (v")
                    .append(Const.VERSION + ") \r\n");
            System.out.println(text.toString());
        }
    }

}
