package com.chopsticks.server.netty;

import com.chopsticks.Chopsticks;
import com.chopsticks.mvc.handler.ExceptionHandler;
import com.chopsticks.mvc.handler.WebSocketHandler;
import com.chopsticks.mvc.websocket.WebSocketContext;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * Http Server Handler
 *
 * @author biezhi
 * 2017/5/31
 */
@Slf4j
@ChannelHandler.Sharable
public class WebSockerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private WebSocketHandler webSocketHandler;
    private ExceptionHandler exceptionHandler;

    WebSockerHandler(Chopsticks chopsticks) {
        this.webSocketHandler = chopsticks.webSocketHandler();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        WebSocketContext webSocketContext = new WebSocketContext(ctx);
        webSocketHandler.onConnect(webSocketContext);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        WebSocketContext webSocketContext = new WebSocketContext(ctx);
        webSocketHandler.onDisConnect(webSocketContext);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        WebSocketContext webSocketContext = new WebSocketContext(ctx);
        if (frame instanceof TextWebSocketFrame) {
            // Send the uppercase string back.
            String request = ((TextWebSocketFrame) frame).text();
            webSocketContext.setReqText(request);
            webSocketHandler.onText(webSocketContext);
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (null != exceptionHandler) {
            exceptionHandler.handle((Exception) cause);
        } else {
            log.error("Blade Invoke Error", cause);
        }
        ctx.close();
    }

}