package org.lab.mars.onem2m.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.test.Test;


/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketClientChannelHandler extends
		SimpleChannelInboundHandler<Object> {
	/**
	 * Creates a client-side handler.
	 */
	public PacketClientChannelHandler() throws KeeperException,
			InterruptedException {
		try {
			Test.createM2mCreatePacket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// Send the first message if this handler is a client-side handler.
		//ctx.writeAndFlush(m2mPacket);
		//System.out.println("发送成功");
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		// Echo back the received object to the server.
		System.out.println("接收到了消息");
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
