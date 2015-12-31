package org.xhome.mars.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketServerChannelHandler extends
		SimpleChannelInboundHandler<Object> {
	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		// Echo back the received object to the client.
		List<Person> personList = (ArrayList<Person>) msg;

		// Person person=(Person)msg;
		// System.out.println(personList.get(0).getName());
		System.out.println(msg.toString() + "dff");
		// ctx.write(msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
