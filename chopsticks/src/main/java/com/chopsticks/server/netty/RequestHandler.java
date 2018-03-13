package com.chopsticks.server.netty;

import com.chopsticks.mvc.http.Request;
import com.chopsticks.mvc.http.Response;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author biezhi
 *         2017/6/3
 */
@FunctionalInterface
public interface RequestHandler<R> {

    R handle(ChannelHandlerContext ctx, Request request, Response response) throws Exception;

}
