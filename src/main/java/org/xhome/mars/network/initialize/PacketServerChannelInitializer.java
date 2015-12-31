package org.xhome.mars.network.initialize;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.xhome.mars.network.handler.PacketClientChannelHandler;
import org.xhome.mars.network.handler.PacketServerChannelHandler;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketServerChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline channelPipeline = ch.pipeline();
		channelPipeline.addLast(new ObjectEncoder());
		channelPipeline.addLast(new ObjectDecoder(ClassResolvers
				.cacheDisabled(null)));
		channelPipeline.addLast(new PacketServerChannelHandler());
	}
}