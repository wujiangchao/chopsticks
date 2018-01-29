
package com.chopsticks;

import static com.chopsticks.mvc.Const.DEFAULT_SERVER_ADDRESS;
import static com.chopsticks.mvc.Const.DEFAULT_SERVER_PORT;
import static com.chopsticks.mvc.Const.DEFAULT_STATICS;
import static com.chopsticks.mvc.Const.DEFAULT_THREAD_NAME;
import static com.chopsticks.mvc.Const.ENV_KEY_APP_NAME;
import static com.chopsticks.mvc.Const.ENV_KEY_APP_THREAD_NAME;
import static com.chopsticks.mvc.Const.ENV_KEY_APP_WATCH_ENV;
import static com.chopsticks.mvc.Const.ENV_KEY_BANNER_PATH;
import static com.chopsticks.mvc.Const.ENV_KEY_BOOT_CONF;
import static com.chopsticks.mvc.Const.ENV_KEY_CORS_ENABLE;
import static com.chopsticks.mvc.Const.ENV_KEY_DEV_MODE;
import static com.chopsticks.mvc.Const.ENV_KEY_GZIP_ENABLE;
import static com.chopsticks.mvc.Const.ENV_KEY_SERVER_ADDRESS;
import static com.chopsticks.mvc.Const.ENV_KEY_SERVER_PORT;
import static com.chopsticks.mvc.Const.ENV_KEY_STATIC_LIST;
import static com.chopsticks.mvc.Const.PLUGIN_PACKAGE_NAME;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.chopsticks.event.BeanProcessor;
import com.chopsticks.event.EventListener;
import com.chopsticks.event.EventManager;
import com.chopsticks.event.EventType;
import com.chopsticks.exception.ChopsticksException;
import com.chopsticks.ioc.Ioc;
import com.chopsticks.ioc.SimpleIoc;
import com.chopsticks.kit.Assert;
import com.chopsticks.kit.ChopsticksKit;
import com.chopsticks.kit.StringKit;
import com.chopsticks.mvc.SessionManager;
import com.chopsticks.mvc.handler.DefaultExceptionHandler;
import com.chopsticks.mvc.handler.ExceptionHandler;
import com.chopsticks.mvc.handler.RouteHandler;
import com.chopsticks.mvc.handler.WebSocketHandler;
import com.chopsticks.mvc.hook.WebHook;
import com.chopsticks.mvc.http.HttpMethod;
import com.chopsticks.mvc.http.HttpSession;
import com.chopsticks.mvc.http.Session;
import com.chopsticks.mvc.route.RouteMatcher;
import com.chopsticks.mvc.ui.template.DefaultEngine;
import com.chopsticks.mvc.ui.template.TemplateEngine;
import com.chopsticks.server.Server;
import com.chopsticks.server.netty.NettyServer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Chopsticks Core
 * <p>
 * The Chopsticks is the core operating class of the framework,
 * which can be used to register routes,
 * modify the template engine, set the file list display,
 * static resource directory, and so on.
 *
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Chopsticks {

    /**
     * Project middleware list,
     * the default is empty, when you use the time you can call the use of methods to add.
     * <p>
     * Chopsticks provide you with BasicAuthMiddleware, CsrfMiddleware,
     * you can customize the implementation of some middleware
     */
    private List<WebHook> middleware = new ArrayList<>();

    /**
     * BeanProcessor list, which stores all the actions that were performed before the project was started
     */
    private List<BeanProcessor> processors = new ArrayList<>();

    /**
     * All need to be scanned by the package, when you do not set the time will scan com.Chopsticks.plugin package
     */
    private Set<String> packages = new LinkedHashSet<>(PLUGIN_PACKAGE_NAME);

    /**
     * All static resource URL prefixes,
     * defaults to "/favicon.ico", "/robots.txt", "/static/", "/upload/", "/webjars/",
     * which are located under classpath
     */
    private Set<String> statics = new HashSet<>(DEFAULT_STATICS);

    /**
     * The default IOC container implementation
     */
    private Ioc ioc = new SimpleIoc();

    /**
     * The default template engine implementation, this is a very simple, generally not put into production
     */
    private TemplateEngine templateEngine = new DefaultEngine();

    /**
     * Event manager, which manages all the guys that will trigger events
     */
    private EventManager eventManager = new EventManager();

    /**
     * Session manager, which manages session when you enable session
     */
    private SessionManager sessionManager = new SessionManager();

    /**
     * Used to wait for the start to complete the lock
     */
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * Web server implementation, currently only netty
     */
    private Server server = new NettyServer();

    /**
     * A route matcher that matches whether a route exists
     */
    private RouteMatcher routeMatcher = new RouteMatcher();

    /**
     * Chopsticks environment, which stores the parameters of the app.properties configuration file
     */
    private Environment environment = Environment.empty();

    /**
     * Exception handling, it will output some logs when the error is initiated
     */
    private Consumer<Exception> startupExceptionHandler = (e) -> log.error("Start Chopsticks failed", e);

    /**
     * Exception handler, default is DefaultExceptionHandler.
     * <p>
     * When you need to customize the handling of exceptions can be inherited from DefaultExceptionHandler
     */
    private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

    /**
     * Used to identify whether the web server has started
     */
    private boolean started = false;

    /**
     * Project main class, the main category is located in the root directory of the basic package,
     * all the features will be in the sub-package below
     */
    private Class<?> bootClass = null;

    /**
     * Session implementation type, the default is HttpSession.
     * <p>
     * When you need to be able to achieve similar RedisSession
     */
    private Class<? extends Session> sessionImplType = HttpSession.class;

    /**
     * WebSocket path
     */
    private String webSocketPath;

    /**
     * Chopsticks app start banner, default is Const.BANNER
     */
    private String bannerText;

    /**
     * Chopsticks app start thread name, default is Const.DEFAULT_THREAD_NAME
     */
    private String threadName;

    /**
     * WebSocket Handler
     */
    private WebSocketHandler webSocketHandler;

    /**
     * Give your Chopsticks instance, from then on will get the energy
     *
     * @return return Chopsticks instance
     */
    public static Chopsticks me() {
        return new Chopsticks();
    }

    /**
     * Get Chopsticks ioc container, default is SimpleIoc implement.
     * <p>
     * IOC container will help you hosting Bean or component, it is actually a Map inside.
     * In the Chopsticks in a single way to make objects reuse,
     * you can save resources, to avoid the terrible memory leak
     *
     * @return return ioc container
     */
    public Ioc ioc() {
        return ioc;
    }

    /**
     * Add a get route to routes
     *
     * @param path    your route path
     * @param handler route implement
     * @return return Chopsticks instance
     */
    public Chopsticks get(@NonNull String path, @NonNull RouteHandler handler) {
        routeMatcher.addRoute(path, handler, HttpMethod.GET);
        return this;
    }

    /**
     * Add a post route to routes
     *
     * @param path    your route path
     * @param handler route implement
     * @return return Chopsticks instance
     */
    public Chopsticks post(@NonNull String path, @NonNull RouteHandler handler) {
        routeMatcher.addRoute(path, handler, HttpMethod.POST);
        return this;
    }

    /**
     * Add a put route to routes
     *
     * @param path    your route path
     * @param handler route implement
     * @return return Chopsticks instance
     */
    public Chopsticks put(@NonNull String path, @NonNull RouteHandler handler) {
        routeMatcher.addRoute(path, handler, HttpMethod.PUT);
        return this;
    }

    /**
     * Add a delete route to routes
     *
     * @param path    your route path
     * @param handler route implement
     * @return return Chopsticks instance
     */
    public Chopsticks delete(@NonNull String path, @NonNull RouteHandler handler) {
        routeMatcher.addRoute(path, handler, HttpMethod.DELETE);
        return this;
    }

    /**
     * Add a before route to routes, the before route will be executed before matching route
     *
     * @param path    your route path
     * @param handler route implement
     * @return return Chopsticks instance
     */
    public Chopsticks before(@NonNull String path, @NonNull RouteHandler handler) {
        routeMatcher.addRoute(path, handler, HttpMethod.BEFORE);
        return this;
    }

    /**
     * Add a after route to routes, the before route will be executed after matching route
     *
     * @param path    your route path
     * @param handler route implement
     * @return return Chopsticks instance
     */
    public Chopsticks after(@NonNull String path, @NonNull RouteHandler handler) {
        routeMatcher.addRoute(path, handler, HttpMethod.AFTER);
        return this;
    }

    /**
     * Setting Chopsticks mvc default templateEngine
     *
     * @param templateEngine TemplateEngine object
     * @return Chopsticks
     */
    public Chopsticks templateEngine(@NonNull TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        return this;
    }

    /**
     * Get TemplateEngine, default is DefaultEngine
     *
     * @return return TemplateEngine
     */
    public TemplateEngine templateEngine() {
        return templateEngine;
    }

    /**
     * Get RouteMatcher
     *
     * @return return RouteMatcher
     */
    public RouteMatcher routeMatcher() {
        return routeMatcher;
    }

    /**
     * Register bean to ioc container
     *
     * @param bean bean object
     * @return Chopsticks
     */
    public Chopsticks register(@NonNull Object bean) {
        ioc.addBean(bean);
        return this;
    }

    /**
     * Register bean to ioc container
     *
     * @param cls bean class, the class must provide a no args constructor
     * @return Chopsticks
     */
    public Chopsticks register(@NonNull Class<?> cls) {
        ioc.addBean(cls);
        return this;
    }

    /**
     * Add multiple static resource file
     * the default provides the static, upload
     *
     * @param folders static resource directory
     * @return Chopsticks
     */
    public Chopsticks addStatics(@NonNull String... folders) {
        statics.addAll(Arrays.asList(folders));
        return this;
    }

    /**
     * Set whether to show the file directory, default doesn't show
     *
     * @param fileList show the file directory
     * @return Chopsticks
     */
    public Chopsticks showFileList(boolean fileList) {
        this.environment(ENV_KEY_STATIC_LIST, fileList);
        return this;
    }

    /**
     * Set whether open gzip, default disabled
     *
     * @param gzipEnable enabled gzip
     * @return Chopsticks
     */
    public Chopsticks gzip(boolean gzipEnable) {
        this.environment(ENV_KEY_GZIP_ENABLE, gzipEnable);
        return this;
    }

    /**
     * Get ioc bean
     *
     * @param cls bean class type
     * @return return bean instance
     */
    public <T> T getBean(@NonNull Class<T> cls) {
        return ioc.getBean(cls);
    }

    /**
     * Get ExceptionHandler
     *
     * @return return ExceptionHandler
     */
    public ExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Set ExceptionHandler, when you need a custom exception handling
     *
     * @param exceptionHandler your ExceptionHandler instance
     * @return return Chopsticks instance
     */
    public Chopsticks exceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    /**
     * Get current is developer mode
     *
     * @return return true is developer mode, else not.
     */
    public boolean devMode() {
        return environment.getBoolean(ENV_KEY_DEV_MODE, true);
    }

    /**
     * Whether encoding setting mode for developers
     * The default mode is developers
     *
     * @param devMode developer mode
     * @return Chopsticks
     */
    public Chopsticks devMode(boolean devMode) {
        this.environment(ENV_KEY_DEV_MODE, devMode);
        return this;
    }

    public Class<?> bootClass() {
        return this.bootClass;
    }

    /**
     * Set whether to enable cors
     *
     * @param enableCors enable cors
     * @return Chopsticks
     */
    public Chopsticks enableCors(boolean enableCors) {
        this.environment(ENV_KEY_CORS_ENABLE, enableCors);
        return this;
    }

    /**
     * Get Chopsticks statics list.
     * e.g: "/favicon.ico", "/robots.txt", "/static/", "/upload/", "/webjars/"
     *
     * @return return statics
     */
    public Set<String> getStatics() {
        return statics;
    }

    /**
     * When set to start Chopsticks scan packages
     *
     * @param packages package name
     * @return Chopsticks
     */
    public Chopsticks scanPackages(@NonNull String... packages) {
        this.packages.addAll(Arrays.asList(packages));
        return this;
    }

    /**
     * Get scan the package set.
     *
     * @return return packages set
     */
    public Set<String> scanPackages() {
        return packages;
    }

    /**
     * Set to start Chopsticks configuration file by default
     * Boot config properties file in classpath directory.
     * <p>
     * Without setting will read the classpath -> app.properties
     *
     * @param bootConf boot config file name
     * @return Chopsticks
     */
    public Chopsticks bootConf(@NonNull String bootConf) {
        this.environment(ENV_KEY_BOOT_CONF, bootConf);
        return this;
    }

    /**
     * Set the environment variable for global use here
     *
     * @param key   environment key
     * @param value environment value
     * @return Chopsticks
     */
    public Chopsticks environment(@NonNull String key, @NonNull Object value) {
        environment.set(key, value);
        return this;
    }

    public Environment environment() {
        return environment;
    }

    public Chopsticks environment(Environment environment) {
        this.environment = environment;
        return this;
    }

    /**
     * Set to start the web server to monitor port, the default is 9000
     *
     * @param port web server port
     * @return Chopsticks
     */
    public Chopsticks listen(int port) {
        Assert.greaterThan(port, 0, "server port not is negative number.");
        this.environment(ENV_KEY_SERVER_PORT, port);
        return this;
    }

    /**
     * Set to start the web server to listen the IP address and port
     * The default will listen 0.0.0.0:9000
     *
     * @param address ip address
     * @param port    web server port
     * @return Chopsticks
     */
    public Chopsticks listen(@NonNull String address, int port) {
        Assert.greaterThan(port, 0, "server port not is negative number.");
        this.environment(ENV_KEY_SERVER_ADDRESS, address);
        this.environment(ENV_KEY_SERVER_PORT, port);
        return this;
    }

    /**
     * The use of multiple middleware, if any
     *
     * @param middleware middleware object array
     * @return Chopsticks
     */
    public Chopsticks use(@NonNull WebHook... middleware) {
        if (!ChopsticksKit.isEmpty(middleware)) {
            this.middleware.addAll(Arrays.asList(middleware));
        }
        return this;
    }

    /**
     * Get middleware list
     *
     * @return return middleware list
     */
    public List<WebHook> middleware() {
        return this.middleware;
    }

    /**
     * Set in the name of the app Chopsticks application
     *
     * @param appName application name
     * @return Chopsticks
     */
    public Chopsticks appName(@NonNull String appName) {
        this.environment(ENV_KEY_APP_NAME, appName);
        return this;
    }

    /**
     * Add a event watcher
     * When the trigger event is executed eventListener
     *
     * @param eventType     event type
     * @param eventListener event watcher
     * @return Chopsticks
     */
    public <T> Chopsticks event(@NonNull EventType eventType, @NonNull EventListener<T> eventListener) {
        eventManager.addEventListener(eventType, eventListener);
        return this;
    }

    /**
     * Get session implements Class Type
     *
     * @return return Chopsticks Session Type
     */
    public Class<? extends Session> sessionType() {
        return this.sessionImplType;
    }

    /**
     * Set session implements Class Type, e.g: RedisSession
     *
     * @param sessionImplType Session Type implement
     * @return return Chopsticks instance
     */
    public Chopsticks sessionType(Class<? extends Session> sessionImplType) {
        this.sessionImplType = sessionImplType;
        return this;
    }

    /**
     * Event on started
     *
     * @param processor bean processor
     * @return return Chopsticks instance
     */
    public Chopsticks onStarted(@NonNull BeanProcessor processor) {
        processors.add(processor);
        return this;
    }

    /**
     * Get processors
     *
     * @return return processors
     */
    public List<BeanProcessor> processors() {
        return processors;
    }

    /**
     * Get EventManager
     *
     * @return return EventManager
     */
    public EventManager eventManager() {
        return eventManager;
    }

    /**
     * Get SessionManager
     *
     * @return return SessionManager
     */
    public SessionManager sessionManager() {
        return sessionManager;
    }

    /**
     * Disable session, default is open
     *
     * @return return Chopsticks instance
     */
    public Chopsticks disableSession() {
        this.sessionManager = null;
        return this;
    }

    public Chopsticks watchEnvChange(boolean watchEnvChange){
        this.environment.set(ENV_KEY_APP_WATCH_ENV, watchEnvChange);
        return this;
    }

    /**
     * Start Chopsticks application.
     * <p>
     * When all the routing in the main function of situations you can use,
     * Otherwise please do not call this method.
     *
     * @return return Chopsticks instance
     */
    public Chopsticks start() {
        return this.start(null, null);
    }

    /**
     * Start Chopsticks application
     *
     * @param mainCls main Class, the main class bag is basic package
     * @param args    command arguments
     * @return return Chopsticks instance
     */
    public Chopsticks start(Class<?> mainCls, String... args) {
        return this.start(mainCls, DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT, args);
    }

    /**
     * Start the Chopsticks web server
     *
     * @param bootClass Start the boot class, used to scan the class in all of the packages
     * @param address   web server bind ip address
     * @param port      web server bind port
     * @param args      launch parameters
     * @return Chopsticks
     */
    public Chopsticks start(Class<?> bootClass, @NonNull String address, int port, String... args) {
        try {
            environment.set(ENV_KEY_SERVER_ADDRESS, address);
            Assert.greaterThan(port, 0, "server port not is negative number.");
            this.bootClass = bootClass;
            eventManager.fireEvent(EventType.SERVER_STARTING, this);
            Thread thread = new Thread(() -> {
                try {
                    server.start(Chopsticks.this, args);
                    latch.countDown();
                    server.join();
                } catch (Exception e) {
                    startupExceptionHandler.accept(e);
                }
            });

            String threadName = null != this.threadName ? this.threadName : environment.get(ENV_KEY_APP_THREAD_NAME, null);
            threadName = null != threadName ? threadName : DEFAULT_THREAD_NAME;

            thread.setName(threadName);
            thread.start();
            started = true;
        } catch (Exception e) {
            startupExceptionHandler.accept(e);
        }
        return this;
    }

    /**
     * Await web server started
     *
     * @return return Chopsticks instance
     */
    public Chopsticks await() {
        if (!started) {
            throw new IllegalStateException("Server hasn't been started. Call start() before calling this method.");
        }
        try {
            latch.await();
        } catch (Exception e) {
            log.error("await error", e);
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * Stop current Chopsticks application
     * <p>
     * Will stop synchronization waiting netty service
     */
    public void stop() {
        eventManager.fireEvent(EventType.SERVER_STOPPING, this);
        server.stopAndWait();
        eventManager.fireEvent(EventType.SERVER_STOPPED, this);
    }

    /**
     * Register WebSocket path
     *
     * @param path    websocket path
     * @param handler websocket handler
     * @return return Chopsticks instance
     */
    public Chopsticks webSocket(@NonNull String path, @NonNull WebSocketHandler handler) {
        if (null != this.webSocketHandler) {
            throw new ChopsticksException(500, "There is already a WebSocket path.");
        }
        this.webSocketPath = path;
        this.webSocketHandler = handler;
        System.out.println(String.format("\n\t\t\t\t\t\t\t\t\t\t\t\t\t" +
                "\t\t\t\t\t Register WebSocket Path: %s\n", path));
        return this;
    }

    /**
     * Get webSocket path
     *
     * @return return websocket path
     */
    public String webSocketPath() {
        return webSocketPath;
    }

    /**
     * Set Chopsticks start banner text
     *
     * @param bannerText banner text
     * @return return Chopsticks instance
     */
    public Chopsticks bannerText(String bannerText) {
        this.bannerText = bannerText;
        return this;
    }

    /**
     * Get banner text
     *
     * @return return Chopsticks start banner text
     */
    public String bannerText() {
        if (null != bannerText) return bannerText;
        String bannerPath = environment.get(ENV_KEY_BANNER_PATH, null);
        if (StringKit.isNotBlank(bannerPath) && Files.exists(Paths.get(bannerPath))) {
            try {
                BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(bannerPath));
                bannerText = bufferedReader.lines().collect(Collectors.joining("\r\n"));
            } catch (Exception e) {
                log.error("Load Start Banner file error", e);
            }
            return bannerText;
        }
        return null;
    }

    /**
     * Set Chopsticks start thread name
     *
     * @param threadName thread name
     * @return return Chopsticks instance
     */
    public Chopsticks threadName(String threadName) {
        this.threadName = threadName;
        return this;
    }

    /**
     * Get WebSocket Handler
     *
     * @return return websocket handler
     */
    public WebSocketHandler webSocketHandler() {
        return webSocketHandler;
    }

}
