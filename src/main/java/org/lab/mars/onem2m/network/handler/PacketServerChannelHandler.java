package org.lab.mars.onem2m.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.network.TcpClient;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.server.NettyServerCnxn;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

;

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
    private Integer replicationFactor;

    public PacketServerChannelHandler(ServerCnxnFactory serverCnxnFactory) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.self = serverCnxnFactory.getMyIp();
        this.networkPool = serverCnxnFactory.getNetworkPool();
        this.replicationFactor = serverCnxnFactory.getReplicationFactor();

    }

    private static final AttributeKey<NettyServerCnxn> STATE = AttributeKey
            .valueOf("MyHandler.nettyServerCnxn");

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        M2mPacket m2mPacket = (M2mPacket) msg;
        if (preProcessPacket(m2mPacket)) {
            NettyServerCnxn nettyServerCnxn = ctx.attr(STATE).get();
            nettyServerCnxn.receiveMessage(ctx, m2mPacket);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyServerCnxn nettyServerCnxn = new NettyServerCnxn(ctx.channel(),
                serverCnxnFactory.getZkServers(), serverCnxnFactory);
        nettyServerCnxn.setNetworkPool(serverCnxnFactory.getNetworkPool());
        ctx.attr(STATE).set(nettyServerCnxn);
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

    /**
     * 对数据包进行处理
     * 
     * @param m2mPacket
     * @return
     */
    public boolean preProcessPacket(M2mPacket m2mPacket) {
        String key = m2mPacket.getM2mRequestHeader().getKey();
        if (isShouldHandle(key)) {
            return true;
        }
        String server = networkPool.getSock(key);// 把server
        if (ipAndChannels.containsKey(server)) {
            ipAndChannels.get(server).writeAndFlush(m2mPacket);
        } else {
            try {
                TcpClient tcpClient = new TcpClient();
                String[] splitStrings = spilitString(server);
                tcpClient.connectionOne("localhost",
                        Integer.valueOf(splitStrings[1]));

                tcpClient.write(m2mPacket);
                ipAndChannels.put(server, tcpClient.getChannel());
            } catch (Exception e) {
                e.printStackTrace();
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

    /**
     * 是否应该自己处理
     * 
     * @return
     */
    private boolean isShouldHandle(String key) {
        String server = networkPool.getSock(key);
        if (serverCnxnFactory.isTemporyAdd()) {
            if (server.equals(self)) {
                return true;
            }
        }
        long myServerId = networkPool.getServerPosition().get(self);
        long handlerServerId = networkPool.getServerPosition().get(server);
        long serverSize = networkPool.getServerPosition().size();
        long distance = myServerId - handlerServerId;
        if (distance >= 0 && distance < replicationFactor) {
            return true;
        }
        if (distance < 0 && (distance + serverSize) < replicationFactor) {
            return true;
        }
        return false;
    }
}
