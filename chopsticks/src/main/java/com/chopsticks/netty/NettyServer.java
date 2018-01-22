package com.chopsticks.netty;

import java.util.List;

import org.omg.CORBA.Environment;

import com.chopsticks.Chopsticks;
import com.chopsticks.server.Server;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

public class NettyServer implements Server{
	 private Chopsticks               chopsticks;
	    private Environment         environment;
	    private EventLoopGroup      bossGroup;
	    private EventLoopGroup      workerGroup;
	    private Channel             channel;
	    private RouteBuilder        routeBuilder;
	    private List<BeanProcessor> processors;

	@Override
	public void start(Chopsticks chopsticks, String[] args) throws Exception {
		
	}

	@Override
	public void join() throws Exception {
		
	}

	@Override
	public void stop() {
		
	}

	@Override
	public void stopAndWait() {
		
	}

}
