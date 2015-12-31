package org.xhome.mars.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Set;

/**
 * Created by Administrator on 2015/12/21.
 */
public class AddChannelHandler extends ChannelInboundHandlerAdapter {
	private Set<Channel> channels;

	public AddChannelHandler(Set<Channel> channels) {
		this.channels = channels;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		channels.add(ctx.channel());
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		channels.remove(ctx.channel());
		super.channelInactive(ctx);
	}
}
