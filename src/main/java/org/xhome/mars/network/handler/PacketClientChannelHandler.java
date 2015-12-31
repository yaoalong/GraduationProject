package org.xhome.mars.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.zookeeper.KeeperException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketClientChannelHandler extends
		SimpleChannelInboundHandler<Object> {
	private final List<Person> firstMessage;

	/**
	 * Creates a client-side handler.
	 */
	public PacketClientChannelHandler() throws KeeperException,
			InterruptedException {
		firstMessage = new ArrayList<Person>(10);
		for (int i = 0; i < 10; i++) {
			Person person=new Person();
			person.setId(i);
			person.setName("uestc"+i);
			firstMessage.add(person);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// Send the first message if this handler is a client-side handler.
		ctx.channel().writeAndFlush(firstMessage);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		// Echo back the received object to the server.
		ctx.write(msg);
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
