package org.lab.mars.onem2m.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.proto.M2mPacket;
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
            e.printStackTrace();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Test.deserialGetDataPacket((M2mPacket) msg);
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
