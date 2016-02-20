package org.lab.mars.onem2m.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.network.TcpClient;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/12/21.
 */
public class PacketClientChannelHandler extends
        SimpleChannelInboundHandler<Object> {

    private static final Logger LOG = LoggerFactory
            .getLogger(PacketClientChannelHandler.class);
    private TcpClient tcpClient;

    public PacketClientChannelHandler(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public PacketClientChannelHandler() throws KeeperException,
            InterruptedException {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            readResponse((M2mPacket) msg);
        } catch (IOException e) {
            LOG.error("channel read error:{}", e);
        }
    }

    private void readResponse(M2mPacket m2mPacket) throws IOException {
        M2mPacket packet;
        synchronized (tcpClient.getPendingQueue()) {
            if (tcpClient.getPendingQueue().size() == 0) {
                throw new IOException("Nothing in the queue, but got "
                        + m2mPacket.getM2mReplyHeader().getXid());
            }
            packet = tcpClient.getPendingQueue().remove();
            packet.setFinished(true);
            synchronized (packet) {
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
        LOG.info("close ctx,because of:{}", cause);
        ctx.close();
    }
}
