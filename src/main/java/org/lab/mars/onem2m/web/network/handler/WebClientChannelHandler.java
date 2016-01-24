package org.lab.mars.onem2m.web.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

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

    public WebClientChannelHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        M2mWebPacket m2mPacket = (M2mWebPacket) msg;
        System.out.println("收到了对应的web回复");
        M2mWebRetriveKeyResponse m2mWebRetriveKeyResponse = (M2mWebRetriveKeyResponse) m2mPacket
                .getResponse();
        for (String server : m2mWebRetriveKeyResponse.getServers()) {
            System.out.println("server:" + server);
            System.out.println("key:"
                    + m2mPacket.getM2mRequestHeader().getKey());

            WebServerChannelHandler.result
                    .get(m2mPacket.getM2mRequestHeader().getXid()).getServers()
                    .add(server);

        }
        System.out.println("zxid::"
                + WebServerChannelHandler.serverResult.get(m2mPacket
                        .getM2mRequestHeader().getXid()));
        WebServerChannelHandler.serverResult.put(m2mPacket
                .getM2mRequestHeader().getXid(),
                WebServerChannelHandler.serverResult.get(m2mPacket
                        .getM2mRequestHeader().getXid()) + 1);
        System.out.println("接受了:"
                + WebServerChannelHandler.serverResult.get(m2mPacket
                        .getM2mRequestHeader().getXid()));
        if (WebServerChannelHandler.serverResult.get(m2mPacket
                .getM2mRequestHeader().getXid()) >= 2) {
            M2mWebPacket m2mWebPacket = new M2mWebPacket(
                    m2mPacket.getM2mRequestHeader(),
                    m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                    new M2mWebRetriveKeyResponse(WebServerChannelHandler.result
                            .get(m2mPacket.getM2mRequestHeader().getXid())
                            .getServers()));
            WebServerChannelHandler.result
                    .get(m2mPacket.getM2mRequestHeader().getXid()).getCtx()
                    .writeAndFlush(m2mWebPacket);
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