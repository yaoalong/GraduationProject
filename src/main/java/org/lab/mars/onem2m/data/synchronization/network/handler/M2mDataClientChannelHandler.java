package org.lab.mars.onem2m.data.synchronization.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M2mDataClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mDataClientChannelHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (LOG.isInfoEnabled()) {
            String host = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getAddress().getHostAddress();
            int port = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getPort();
            LOG.info("host:{},port:{}", host, port);
        }
    }

    /**
     * 处理接收到的别的Server对Web请求的回复
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {

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
