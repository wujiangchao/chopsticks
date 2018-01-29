package com.chopsticks.mvc.route;

import java.lang.reflect.Method;

import com.chopsticks.kit.ChopsticksKit;
import com.chopsticks.kit.ReflectKit;
import com.chopsticks.mvc.annotation.DeleteRoute;
import com.chopsticks.mvc.annotation.GetRoute;
import com.chopsticks.mvc.annotation.Path;
import com.chopsticks.mvc.annotation.PostRoute;
import com.chopsticks.mvc.annotation.PutRoute;
import com.chopsticks.mvc.annotation.Route;
import com.chopsticks.mvc.hook.Signature;
import com.chopsticks.mvc.http.HttpMethod;

import lombok.extern.slf4j.Slf4j;

/**
 * Route builder
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
@Slf4j
public class RouteBuilder {

    private RouteMatcher routeMatcher;

    public RouteBuilder(RouteMatcher routeMatcher) {
        this.routeMatcher = routeMatcher;
    }

    public void addWebHook(final Class<?> webHook, String pattern, Object hook) {
        Method before = ReflectKit.getMethod(webHook, HttpMethod.BEFORE.name().toLowerCase(), Signature.class);
        Method after  = ReflectKit.getMethod(webHook, HttpMethod.AFTER.name().toLowerCase(), Signature.class);

        routeMatcher.addRoute(com.chopsticks.mvc.route.Route.builder()
                .target(hook)
                .targetType(webHook)
                .action(before)
                .path(pattern)
                .httpMethod(HttpMethod.BEFORE)
                .build());

        routeMatcher.addRoute(com.chopsticks.mvc.route.Route.builder()
                .target(hook)
                .targetType(webHook)
                .action(after)
                .path(pattern)
                .httpMethod(HttpMethod.AFTER)
                .build());
    }

    /**
     * Parse all routing in a controller
     *
     * @param routeType resolve the routing class,
     *                  e.g RouteHandler.class or some controller class
     */
    public  void addRouter(final Class<?> routeType, Object controller) {

        Method[] methods = routeType.getDeclaredMethods();
        if (ChopsticksKit.isEmpty(methods)) {
            return;
        }

        String nameSpace = null, suffix = null;
        if (null != routeType.getAnnotation(Path.class)) {
            nameSpace = routeType.getAnnotation(Path.class).value();
            suffix = routeType.getAnnotation(Path.class).suffix();
        }

        if (null == nameSpace) {
            log.warn("Route [{}] not path annotation", routeType.getName());
            return;
        }

        for (Method method : methods) {

            Route mapping     = method.getAnnotation(Route.class);
            GetRoute                       getRoute    = method.getAnnotation(GetRoute.class);
            PostRoute                      postRoute   = method.getAnnotation(PostRoute.class);
            PutRoute                       putRoute    = method.getAnnotation(PutRoute.class);
            DeleteRoute                    deleteRoute = method.getAnnotation(DeleteRoute.class);

            this.parseRoute(RouteStruct.builder().mapping(mapping)
                    .getRoute(getRoute).postRoute(postRoute)
                    .putRoute(putRoute).deleteRoute(deleteRoute)
                    .nameSpace(nameSpace)
                    .suffix(suffix).routeType(routeType)
                    .controller(controller).method(method)
                    .build());
        }
    }

    // register route object to ioc
    public void register() {
        routeMatcher.register();
    }

    private void parseRoute(RouteStruct routeStruct) {
        // build multiple route
        HttpMethod methodType = routeStruct.getMethod();
        String[]   paths      = routeStruct.getPaths();
        if (paths.length > 0) {
            for (String path : paths) {
                String pathV = getRoutePath(path, routeStruct.nameSpace, routeStruct.suffix);

                routeMatcher.addRoute(com.chopsticks.mvc.route.Route.builder()
                        .target(routeStruct.controller)
                        .targetType(routeStruct.routeType)
                        .action(routeStruct.method)
                        .path(pathV)
                        .httpMethod(methodType)
                        .build());
            }
        }
    }

    private String getRoutePath(String value, String nameSpace, String suffix) {
        String path = value.startsWith("/") ? value : "/" + value;
        nameSpace = nameSpace.startsWith("/") ? nameSpace : "/" + nameSpace;
        path = nameSpace + path;
        path = path.replaceAll("[/]+", "/");
        path = path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        path = path + suffix;
        return path;
    }

}