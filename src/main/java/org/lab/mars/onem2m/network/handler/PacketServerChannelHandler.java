package org.lab.mars.onem2m.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.network.TcpClient;
import org.lab.mars.onem2m.network.TcpServerConnectionStats;
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
    private ConcurrentHashMap<String, Channel> ipAndChannels = new ConcurrentHashMap<>();
    private String self;
    private NetworkPool networkPool;

    private final LinkedList<M2mPacket> pendingQueue = new LinkedList<M2mPacket>();

    public PacketServerChannelHandler(ServerCnxnFactory serverCnxnFactory) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.self = serverCnxnFactory.getMyIp();
        this.networkPool = serverCnxnFactory.getNetworkPool();

    }

    private static final AttributeKey<NettyServerCnxn> STATE = AttributeKey
            .valueOf("MyHandler.nettyServerCnxn");

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        synchronized (TcpServerConnectionStats.connectionStats) {
            TcpServerConnectionStats.connectionStats.get(ctx);
        }
        M2mPacket m2mPacket = (M2mPacket) msg;
        if (preProcessPacket(m2mPacket, ctx)) {
            NettyServerCnxn nettyServerCnxn = ctx.attr(STATE).get();
            nettyServerCnxn.receiveMessage(ctx, m2mPacket);
        } else {// 需要增加对错误的处理

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyServerCnxn nettyServerCnxn = new NettyServerCnxn(ctx.channel(),
                serverCnxnFactory.getZkServers(), serverCnxnFactory);
        nettyServerCnxn.setNetworkPool(serverCnxnFactory.getNetworkPool());
        ctx.attr(STATE).set(nettyServerCnxn);
        synchronized (TcpServerConnectionStats.connectionStats) {
            TcpServerConnectionStats.connectionStats.put(ctx, 0L);
        }

        ctx.fireChannelRegistered();

    };

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.info("Channel disconnect caused close:{}", cause);
        cause.printStackTrace();
        synchronized (TcpServerConnectionStats.connectionStats) {
            TcpServerConnectionStats.connectionStats.remove(ctx);
        }

        ctx.close();
    }

    /**
     * 对数据包进行处理
     * 
     * @param m2mPacket
     * @return
     */
    public boolean preProcessPacket(M2mPacket m2mPacket,
            ChannelHandlerContext ctx) {
        String key = m2mPacket.getM2mRequestHeader().getKey();

        String server = networkPool.getSock(key);
        if (server.equals(self)) {
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (ipAndChannels.containsKey(server)) {
                ipAndChannels.get(server).writeAndFlush(m2mPacket);
            } else {
                try {
                    TcpClient tcpClient = new TcpClient(pendingQueue);
                    String[] splitStrings = spilitString(server);
                    tcpClient.connectionOne(splitStrings[0],
                            Integer.valueOf(splitStrings[1]));

                    tcpClient.write(m2mPacket);
                    ctx.writeAndFlush(m2mPacket);
                    ipAndChannels.put(server, tcpClient.getChannel());
                    return false;
                } catch (Exception e) {

                    LOG.error("process packet error:{}", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException error:{}", e);
                }
                server = networkPool.getSock(key);

            }
        }
        return false;
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
