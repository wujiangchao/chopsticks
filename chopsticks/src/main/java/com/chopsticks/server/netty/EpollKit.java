package com.chopsticks.server.netty;

import com.chopsticks.kit.NamedThreadFactory;

import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;

/**
 * Epoll kit
 * <p>
 * enable epool event loop group
 *
 * @author biezhi
 * @date 2017/9/22
 */
class EpollKit {

    static NettyServerGroup group(int threadCount, int workers) {
        EpollEventLoopGroup bossGroup   = new EpollEventLoopGroup(threadCount, new NamedThreadFactory("epoll-boss@"));
        EpollEventLoopGroup workerGroup = new EpollEventLoopGroup(workers, new NamedThreadFactory("epoll-worker@"));
        return NettyServerGroup.builder().boosGroup(bossGroup).workerGroup(workerGroup).socketChannel(EpollServerSocketChannel.class).build();
    }

}
