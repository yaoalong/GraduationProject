package org.lab.mars.onem2m.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.LinkedList;
import java.util.List;
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
        System.out.println("接收到了消息");
        synchronized (TcpServerConnectionStats.connectionStats) {
            TcpServerConnectionStats.connectionStats.get(ctx);
        }
        M2mPacket m2mPacket = (M2mPacket) msg;
        if (preProcessPacket(m2mPacket, ctx)) {
            NettyServerCnxn nettyServerCnxn = ctx.attr(STATE).get();
            nettyServerCnxn.receiveMessage(ctx, m2mPacket);
            System.out.println("接收到了恢复");
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
        if (LOG.isTraceEnabled()) {
            LOG.trace("Channel disconnect caused close:{}", cause);
        }
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
        List<String> servers = networkPool.getAllSock(key);
        if (servers.contains(self)
                || (serverCnxnFactory.isTemporyAdd() && networkPool
                        .getSock(key).equals(self))) {
            return true;
        }
        for (int i = 0; i < servers.size(); i++) {
            String server = servers.get(i);// 把server
            if (ipAndChannels.containsKey(server)) {
                ipAndChannels.get(server).writeAndFlush(m2mPacket);
            } else {
                try {
                    TcpClient tcpClient = new TcpClient(pendingQueue);
                    String[] splitStrings = spilitString(server);
                    tcpClient.connectionOne(splitStrings[0],
                            Integer.valueOf(splitStrings[1]));

                    tcpClient.write(m2mPacket);
                    synchronized (m2mPacket) {
                        System.out.println("等待开始");
                        while (!m2mPacket.isFinished()) {
                            m2mPacket.wait();
                        }
                        System.out.println("等待结束");
                    }
                    ctx.writeAndFlush(m2mPacket);
                    ipAndChannels.put(server, tcpClient.getChannel());
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
