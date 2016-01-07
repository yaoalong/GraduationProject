package org.lab.mars.onem2m.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.test.Test;


/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketClientChannelHandler extends
		SimpleChannelInboundHandler<Object> {
	private M2mPacket m2mPacket;
   
	/**
	 * Creates a client-side handler.
	 */
	public PacketClientChannelHandler() throws KeeperException,
			InterruptedException {
		m2mPacket=Test.createM2mPacket();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// Send the first message if this handler is a client-side handler.
		ctx.writeAndFlush(m2mPacket);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		// Echo back the received object to the server.
		System.out.println("接收到了消息");
		ctx.channel().writeAndFlush(m2mPacket);
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
