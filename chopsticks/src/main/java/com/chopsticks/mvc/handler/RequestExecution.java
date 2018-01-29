package com.chopsticks.mvc.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.chopsticks.exception.ChopsticksException;
import com.chopsticks.exception.InternalErrorException;
import com.chopsticks.exception.NotFoundException;
import com.chopsticks.kit.ChopsticksKit;
import com.chopsticks.kit.ReflectKit;
import com.chopsticks.mvc.Const;
import com.chopsticks.mvc.WebContext;
import com.chopsticks.mvc.annotation.JSON;
import com.chopsticks.mvc.annotation.Path;
import com.chopsticks.mvc.hook.Signature;
import com.chopsticks.mvc.hook.WebHook;
import com.chopsticks.mvc.http.HttpRequest;
import com.chopsticks.mvc.http.HttpResponse;
import com.chopsticks.mvc.http.Request;
import com.chopsticks.mvc.http.Response;
import com.chopsticks.mvc.route.Route;
import com.chopsticks.mvc.route.RouteMatcher;
import com.chopsticks.mvc.ui.ModelAndView;
import com.chopsticks.server.netty.HttpConst;
import com.chopsticks.server.netty.HttpServerInitializer;
import com.chopsticks.server.netty.StaticFileHandler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Http Request Execution Handler
 *
 * @date 2017/12/24
 */
@Slf4j
public class RequestExecution implements Runnable {

    private final ChannelHandlerContext ctx;
    private final FullHttpRequest fullHttpRequest;
    private final ExceptionHandler exceptionHandler = WebContext.chopsticks().exceptionHandler();

    private final static Set<String> STATICS = WebContext.chopsticks().getStatics();
    private final static RouteMatcher ROUTE_MATCHER = WebContext.chopsticks().routeMatcher();
    private final static boolean hasMiddleware = ROUTE_MATCHER.getMiddleware().size() > 0;
    private final static boolean hasBeforeHook = ROUTE_MATCHER.hasBeforeHook();
    private final static boolean hasAfterHook = ROUTE_MATCHER.hasAfterHook();
    private final static StaticFileHandler STATIC_FILE_HANDLER = new StaticFileHandler(WebContext.chopsticks());

    public RequestExecution(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        this.ctx = ctx;
        this.fullHttpRequest = fullHttpRequest;
    }

    @Override
    public void run() {
        Request request = HttpRequest.build(ctx, fullHttpRequest);
        Response response = HttpResponse.build(ctx, HttpServerInitializer.date);
        boolean isStatic = false;
        // route signature
        Signature signature = Signature.builder().request(request).response(response).build();
        try {

            // request uri
            String uri = request.uri();

            // write session
            WebContext.set(new WebContext(request, response));

            if (isStaticFile(uri)) {
                STATIC_FILE_HANDLER.handle(ctx, request, response);
                isStatic = true;
                return;
            }

            Route route = ROUTE_MATCHER.lookupRoute(request.method(), uri);
            if (null == route) {
                log.warn("Not Found\t{}", uri);
                throw new NotFoundException(uri);
            }

            log.info("{}\t{}\t{}", request.protocol(), request.method(), uri);

            request.initPathParams(route);

            // get method parameters
            signature.setRoute(route);

            // middleware
            if (hasMiddleware && !invokeMiddleware(ROUTE_MATCHER.getMiddleware(), signature)) {
                this.sendFinish(response);
                return;
            }

            // web hook before
            if (hasBeforeHook && !invokeHook(ROUTE_MATCHER.getBefore(uri), signature)) {
                this.sendFinish(response);
                return;
            }

            // execute
            signature.setRoute(route);
            this.routeHandle(signature);

            // webHook
            if (hasAfterHook) {
                this.invokeHook(ROUTE_MATCHER.getAfter(uri), signature);
            }
        } catch (Exception e) {
            if (null != exceptionHandler) {
                exceptionHandler.handle(e);
            } else {
                log.error("Chopsticks Invoke Error", e);
            }
        } finally {
            if (!isStatic) this.sendFinish(response);
            WebContext.remove();
        }
    }

    /**
     * Actual routing method execution
     *
     * @param signature
     *            signature
     */
    public void routeHandle(Signature signature) throws Exception {
        Object target = signature.getRoute().getTarget();
        if (null == target) {
            Class<?> clazz = signature.getAction().getDeclaringClass();
            target = WebContext.chopsticks().getBean(clazz);
            signature.getRoute().setTarget(target);
        }
        if (signature.getRoute().getTargetType() == RouteHandler.class) {
            RouteHandler routeHandler = (RouteHandler) target;
            routeHandler.handle(signature.request(), signature.response());
        } else {
            this.handle(signature);
        }
    }

    /**
     * handle route signature
     *
     * @param signature
     *            route request signature
     * @throws Exception
     *             throw like parse param exception
     */
    public void handle(Signature signature) throws Exception {
        try {
            Method actionMethod = signature.getAction();
            Object target = signature.getRoute().getTarget();
            Class<?> returnType = actionMethod.getReturnType();

            Response response = signature.response();

            Path path = target.getClass().getAnnotation(Path.class);
            JSON JSON = actionMethod.getAnnotation(JSON.class);

            boolean isRestful = (null != JSON) || (null != path && path.restful());

            // if request is restful and not InternetExplorer userAgent
            if (isRestful && !signature.request().userAgent().contains(HttpConst.IE_UA)) {
                signature.response().contentType(Const.CONTENT_TYPE_JSON);
            }

            int len = actionMethod.getParameterTypes().length;
            Object returnParam = ReflectKit.invokeMethod(target, actionMethod, len > 0 ? signature.getParameters() : null);
            if (null == returnParam) return;

            if (isRestful) {
                response.json(returnParam);
                return;
            }
            if (returnType == String.class) {
                response.render(returnParam.toString());
                return;
            }
            if (returnType == ModelAndView.class) {
                ModelAndView modelAndView = (ModelAndView) returnParam;
                response.render(modelAndView);
            }
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) e = (Exception) e.getCause();
            throw e;
        }
    }

    /**
     * invoke webhook
     *
     * @param routeSignature
     *            current execute route handler signature
     * @param hookRoute
     *            current webhook route handler
     * @return Return true then next handler, and else interrupt request
     * @throws Exception
     *             throw like parse param exception
     */
    public boolean invokeHook(Signature routeSignature, Route hookRoute) throws Exception {
        Method hookMethod = hookRoute.getAction();
        Object target = hookRoute.getTarget();
        if (null == target) {
            Class<?> clazz = hookRoute.getAction().getDeclaringClass();
            target = WebContext.chopsticks().ioc().getBean(clazz);
            hookRoute.setTarget(target);
        }

        // execute
        int len = hookMethod.getParameterTypes().length;
        hookMethod.setAccessible(true);

        Object returnParam;
        if (len > 0) {
            if (len == 1) {
                returnParam = ReflectKit.invokeMethod(target, hookMethod, routeSignature);
            } else if (len == 2) {
                returnParam = ReflectKit.invokeMethod(target, hookMethod, routeSignature.request(), routeSignature.response());
            } else {
                throw new InternalErrorException("Bad web hook structure");
            }
        } else {
            returnParam = ReflectKit.invokeMethod(target, hookMethod);
        }

        if (null == returnParam) return true;

        Class<?> returnType = returnParam.getClass();
        if (returnType == Boolean.class || returnType == boolean.class) { return Boolean.valueOf(returnParam.toString()); }
        return true;
    }

    public boolean invokeMiddleware(List<Route> middleware, Signature signature) throws ChopsticksException {
        if (ChopsticksKit.isEmpty(middleware)) { return true; }
        for (Route route : middleware) {
            WebHook webHook = (WebHook) route.getTarget();
            boolean flag = webHook.before(signature);
            if (!flag) return false;
        }
        return true;
    }

    /**
     * invoke hooks
     *
     * @param hooks
     *            webHook list
     * @param signature
     *            http request
     * @return return invoke hook is abort
     */
    public boolean invokeHook(List<Route> hooks, Signature signature) throws Exception {
        for (Route hook : hooks) {
            if (hook.getTargetType() == RouteHandler.class) {
                RouteHandler routeHandler = (RouteHandler) hook.getTarget();
                routeHandler.handle(signature.request(), signature.response());
            } else {
                boolean flag = this.invokeHook(signature, hook);
                if (!flag) return false;
            }
        }
        return true;
    }

    private boolean isStaticFile(String uri) {
        Optional<String> result = STATICS.stream().filter(s -> s.equals(uri) || uri.startsWith(s)).findFirst();
        return result.isPresent();
    }

    private void sendFinish(Response response) {
        if (!response.isCommit()) {
            response.body(Unpooled.EMPTY_BUFFER);
        }
    }

}
