package org.lab.mars.onem2m.network.intialize;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.network.handler.PacketServerChannelHandler;
import org.lab.mars.onem2m.server.ServerCnxnFactory;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketServerChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	private ServerCnxnFactory serverCnxnFactory;
	private NetworkPool networkPool;
	 public PacketServerChannelInitializer(ServerCnxnFactory serverCnxnFactory,NetworkPool networkPool) {
		this.serverCnxnFactory=serverCnxnFactory;
		this.networkPool=networkPool;
	}
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline channelPipeline = ch.pipeline();
		channelPipeline.addLast(new ObjectEncoder());
		channelPipeline.addLast(new ObjectDecoder(ClassResolvers
				.cacheDisabled(null)));
		channelPipeline.addLast(new PacketServerChannelHandler(serverCnxnFactory,networkPool));
	}
}