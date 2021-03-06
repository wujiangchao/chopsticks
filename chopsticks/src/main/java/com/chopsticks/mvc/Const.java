package com.chopsticks.mvc;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public interface Const {
	int DEFAULT_SERVER_PORT = 9000;
	String DEFAULT_SERVER_ADDRESS = "0.0.0.0";
	String LOCAL_IP_ADDRESS = "127.0.0.1";
	String VERSION = "BETA";
	String WEB_JARS = "/webjars/";
	String CLASSPATH = new File(Const.class.getResource("/").getPath()).getPath();
	String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
	String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
	String CONTENT_TYPE_TEXT = "text/plain; charset=UTF-8";
	String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	String INTERNAL_SERVER_ERROR_HTML = "<center><h1>500 Internal Server Error</h1><hr/></center>";
	String DEFAULT_THREAD_NAME = "_(:3」∠)_";
	List<String> PLUGIN_PACKAGE_NAME = Arrays.asList("com.chopsticks.plugin");
	List<String> DEFAULT_STATICS = Arrays.asList("/favicon.ico", "/robots.txt", "/static/", "/upload/", "/webjars/");

	// Env key
	String ENV_KEY_DEV_MODE = "app.devMode";
	String ENV_KEY_APP_NAME = "app.name";
	String ENV_KEY_APP_THREAD_NAME = "app.thread-name";
	String ENV_KEY_APP_WATCH_ENV = "app.watch-env";
	String ENV_KEY_BANNER_PATH = "app.banner-path";
	String ENV_KEY_GZIP_ENABLE = "http.gzip.enable";
	String ENV_KEY_CORS_ENABLE = "http.cors.enable";
	String ENV_KEY_SESSION_KEY = "http.session.key";
	String ENV_KEY_SESSION_TIMEOUT = "http.session.timeout";
	String ENV_KEY_AUTH_USERNAME = "http.auth.username";
	String ENV_KEY_AUTH_PASSWORD = "http.auth.password";
	String ENV_KEY_PAGE_404 = "mvc.view.404";
	String ENV_KEY_PAGE_500 = "mvc.view.500";
	String ENV_KEY_STATIC_DIRS = "mvc.statics";
	String ENV_KEY_STATIC_LIST = "mvc.statics.show-list";
	String ENV_KEY_TEMPLATE_PATH = "mvc.template.path";
	String ENV_KEY_SERVER_ADDRESS = "server.address";
	String ENV_KEY_SERVER_PORT = "server.port";
	String ENV_KEY_SSL = "server.ssl.enable";
	String ENV_KEY_SSL_CERT = "server.ssl.cert-path";
	String ENE_KEY_SSL_PRIVATE_KEY = "server.ssl.private-key-path";
	String ENE_KEY_SSL_PRIVATE_KEY_PASS = "server.ssl.private-key-pass";
	String ENC_KEY_NETTY_ACCEPT_THREAD_COUNT = "server.netty.accept-thread-count";
	String ENV_KEY_NETTY_IO_THREAD_COUNT = "server.netty.io-thread-count";
	String ENV_KEY_NETTY_SO_BACKLOG = "server.netty.so-backlog";

	String ENV_KEY_BOOT_CONF = "boot_conf";

	// terminal
	String TERMINAL_SERVER_ADDRESS = "--server.address=";
	String TERMINAL_SERVER_PORT = "--server.port=";
	String TERMINAL_CHOPSTICKS_ENV = "--chopsticks.env=";

	String BANNER_SPACE = "\t\t\t\t\t\t\t  ";

	String BANNER_TEXT = "\r\n" + BANNER_SPACE + "    __, _,   _, __, __," + "\r\n" + BANNER_SPACE
			+ "    |_) |   /_\\ | \\ |_" + "\r\n" + BANNER_SPACE + "    |_) | , | | |_/ |" + "\r\n" + BANNER_SPACE
			+ "    ~   ~~~ ~ ~ ~   ~~~";
}
