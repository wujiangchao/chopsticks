package com.chopsticks.server.netty;

import com.chopsticks.mvc.WebContext;
import com.chopsticks.mvc.handler.ExceptionHandler;
import com.chopsticks.mvc.handler.RequestExecution;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Http Server Handler
 *
 * @author biezhi
 * 2017/5/31
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ExceptionHandler exceptionHandler = WebContext.chopsticks().exceptionHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        new RequestExecution(ctx, fullHttpRequest.copy()).run();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (ctx.channel().isOpen() && ctx.channel().isActive() && ctx.channel().isWritable()) {
            ctx.flush();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (null != exceptionHandler) {
            exceptionHandler.handle((Exception) cause);
        } else {
            log.error("Blade Invoke Error", cause);
        }
        if (ctx.channel().isOpen() && ctx.channel().isActive() && ctx.channel().isWritable()) {
            ctx.close();
        }
    }

}