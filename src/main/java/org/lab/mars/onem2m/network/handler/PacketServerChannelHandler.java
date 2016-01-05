package org.lab.mars.onem2m.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.server.NettyServerCnxn;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketServerChannelHandler extends
		SimpleChannelInboundHandler<Object> {
	private static Logger LOG = LoggerFactory
			.getLogger(PacketServerChannelHandler.class);
	private ServerCnxnFactory serverCnxnFactory;

	public PacketServerChannelHandler(ServerCnxnFactory serverCnxnFactory) {
		this.serverCnxnFactory = serverCnxnFactory;
	}

	@SuppressWarnings("deprecation")
	private static final AttributeKey<NettyServerCnxn> STATE = new AttributeKey<NettyServerCnxn>(
			"MyHandler.nettyServerCnxn");

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) {
		M2mPacket m2mPacket = (M2mPacket) msg;
		if (preProcessPacket(m2mPacket)) {

		} else {
			NettyServerCnxn nettyServerCnxn = ctx.attr(STATE).get();
			nettyServerCnxn.receiveMessage(ctx, m2mPacket);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {

		ctx.attr(STATE).set(
				new NettyServerCnxn(ctx.channel(), serverCnxnFactory
						.getZkServer(), serverCnxnFactory));
		ctx.fireChannelRegistered();
	};

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Channel disconnect caused close:{}", cause);
		}
		ctx.close();
	}

	public boolean preProcessPacket(M2mPacket m2mPacket) {

		String key = m2mPacket.getM2mRequestHeader().getKey();

		return false;
	}
}
