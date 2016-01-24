package org.lab.mars.onem2m.web.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.web.nework.protol.M2mWebPacket;
import org.lab.mars.onem2m.web.nework.protol.M2mWebRetriveKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author yaoalong
 * @Date 2016年1月24日
 * @Email yaoalong@foxmail.com
 */
public class WebClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory
            .getLogger(WebClientChannelHandler.class);
    private Integer replicationFactor;

    public WebClientChannelHandler(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

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
        if (msg == null) {
            LOG.error("error because of :nullPoint");
            return;
        }
        M2mWebPacket m2mPacket = (M2mWebPacket) msg;
        M2mWebRetriveKeyResponse m2mWebRetriveKeyResponse = (M2mWebRetriveKeyResponse) m2mPacket
                .getResponse();
        M2mRequestHeader m2mRequestHeader = m2mPacket.getM2mRequestHeader();
        for (String server : m2mWebRetriveKeyResponse.getServers()) {
            WebServerChannelHandler.result
                    .get(m2mPacket.getM2mRequestHeader().getXid()).getServers()
                    .add(server);

        }
        WebServerChannelHandler.serverResult.put(m2mRequestHeader.getXid(),
                WebServerChannelHandler.serverResult.get(m2mRequestHeader
                        .getXid()) + 1);
        if (WebServerChannelHandler.serverResult.get(m2mRequestHeader.getXid()) >= replicationFactor) {
            M2mWebPacket m2mWebPacket = new M2mWebPacket(m2mRequestHeader,
                    m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                    new M2mWebRetriveKeyResponse(WebServerChannelHandler.result
                            .get(m2mPacket.getM2mRequestHeader().getXid())
                            .getServers()));
            WebServerChannelHandler.result.get(m2mRequestHeader).getCtx()
                    .writeAndFlush(m2mWebPacket);
            WebServerChannelHandler.result.remove(m2mRequestHeader.getXid());
            WebServerChannelHandler.serverResult.remove(m2mRequestHeader
                    .getXid());
        }
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