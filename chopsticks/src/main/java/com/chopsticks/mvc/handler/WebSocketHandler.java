package com.chopsticks.mvc.handler;

import com.chopsticks.mvc.websocket.WebSocketContext;

/**
 * @date 2017/10/30
 */
public interface WebSocketHandler {

    void onConnect(WebSocketContext ctx);

    void onText(WebSocketContext ctx);

    void onDisConnect(WebSocketContext ctx);

}
