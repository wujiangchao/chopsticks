package com.chopsticks.mvc;

import com.chopsticks.Chopsticks;
import com.chopsticks.mvc.http.Request;
import com.chopsticks.mvc.http.Response;

import io.netty.util.concurrent.FastThreadLocal;

/**
 * Cached current thread context request and response instance
 */
public class WebContext {

    /**
     * ThreadLocal, used netty fast theadLocal
     */
    private static final FastThreadLocal<WebContext> fastThreadLocal = new FastThreadLocal<>();

    /**
     * chopsticks instance, when the project is initialized when it will permanently reside in memory
     */
    private static Chopsticks chopsticks;

    /**
     * ContextPath, are currently /
     */
    private static String contextPath;

    /**
     * Http Request instance of current thread context
     */
    private Request request;

    /**
     * Http Response instance of current thread context
     */
    private Response response;

    public WebContext(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    /**
     * Set current thread context WebContext instance
     *
     * @param webContext webContext instance
     */
    public static void set(WebContext webContext) {
        fastThreadLocal.set(webContext);
    }

    /**
     * Get current thread context WebContext instance
     *
     * @return WebContext instance
     */
    public static WebContext get() {
        return fastThreadLocal.get();
    }

    /**
     * Remove current thread context WebContext instance
     */
    public static void remove() {
        fastThreadLocal.remove();
    }

    /**
     * Get current thread context Request instance
     *
     * @return Request instance
     */
    public static Request request() {
        WebContext webContext = get();
        return null != webContext ? webContext.request : null;
    }

    /**
     * Get current thread context Response instance
     *
     * @return Response instance
     */
    public static Response response() {
        WebContext webContext = get();
        return null != webContext ? webContext.response : null;
    }

    /**
     * Initializes the project when it starts
     *
     * @param chopsticks       Chopsticks instance
     * @param contextPath context path
     */
    public static void init(Chopsticks chopsticks, String contextPath) {
        WebContext.chopsticks = chopsticks;
        WebContext.contextPath = contextPath;
    }
    
    public static SessionManager sessionManager(){
        return null != chopsticks() ? chopsticks().sessionManager() : null;
    }


    /**
     * Get chopsticks instance
     *
     * @return return chopsticks
     */
    public static Chopsticks chopsticks() {
        return chopsticks;
    }

    /**
     * Get context path
     *
     * @return return context path string, e.g: /
     */
    public static String contextPath() {
        return contextPath;
    }

}
