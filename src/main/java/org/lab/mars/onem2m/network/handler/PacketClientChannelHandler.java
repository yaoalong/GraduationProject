package org.lab.mars.onem2m.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.network.TcpClient;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.test.Test;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private TcpClient tcpClient;

    public PacketClientChannelHandler(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

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
        // Test.deserialGetDataPacket((M2mPacket) msg);
        try {
            readResponse((M2mPacket) msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readResponse(M2mPacket m2mPacket) throws IOException {
        M2mPacket packet;
        System.out.println("收到了");
        synchronized (tcpClient.getPendingQueue()) {
            if (tcpClient.getPendingQueue().size() == 0) {
                throw new IOException("Nothing in the queue, but got "
                        + m2mPacket.getM2mReplyHeader().getXid());
            }
            packet = tcpClient.getPendingQueue().remove();
            packet.setFinished(true);
            synchronized (packet) {
                System.out.println("是否为空:" + m2mPacket.getResponse() == null);
                packet.setResponse(m2mPacket.getResponse());
                packet.notifyAll();
            }

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
