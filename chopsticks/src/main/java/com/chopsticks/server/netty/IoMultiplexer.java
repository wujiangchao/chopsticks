package com.chopsticks.server.netty;

public enum IoMultiplexer {
    EPOLL, KQUEUE, JDK
}