package org.lab.mars.onem2m.web.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.test.Test;
import org.lab.mars.onem2m.web.nework.protol.M2mWebPacket;
import org.lab.mars.onem2m.web.nework.protol.M2mWebRetriveKeyResponse;

public class WebClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    /**
     * Creates a client-side handler.
     */
    public WebClientChannelHandler() throws KeeperException,
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
        // ctx.writeAndFlush(m2mPacket);
        // System.out.println("发送成功");
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
            System.out.println("发送成功");
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