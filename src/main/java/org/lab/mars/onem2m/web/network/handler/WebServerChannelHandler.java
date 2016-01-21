package org.lab.mars.onem2m.web.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.web.nework.protol.M2mServerStatusDO;
import org.lab.mars.onem2m.web.nework.protol.M2mServerStatusDOs;
import org.lab.mars.onem2m.web.nework.protol.M2mWebGetDataResponse;
import org.lab.mars.onem2m.web.nework.protol.M2mWebPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static Logger LOG = LoggerFactory
            .getLogger(WebServerChannelHandler.class);
    private ServerCnxnFactory serverCnxnFactory;
    private ConcurrentHashMap<String, Channel> ipAndChannels = new ConcurrentHashMap<>();
    private String self;
    private NetworkPool networkPool;
    private Integer replicationFactor;
    private List<String> allServers;

    public WebServerChannelHandler(ServerCnxnFactory serverCnxnFactory) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.self = serverCnxnFactory.getMyIp();
        this.networkPool = serverCnxnFactory.getNetworkPool();
        this.replicationFactor = serverCnxnFactory.getReplicationFactor();

    }

    @SuppressWarnings("deprecation")
    private static final AttributeKey<Channel> STATE = new AttributeKey<Channel>(
            "MyHandler.nettyServerCnxn");

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        M2mWebPacket m2mPacket = (M2mWebPacket) msg;
        if (m2mPacket.getM2mRequestHeader().getType() == 1) {
            try {
                lookAllServerStatus(m2mPacket, ctx.attr(STATE).get());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {

        }
    }

    public void lookAllServerStatus(M2mWebPacket m2mWebPacket, Channel channel)
            throws IOException {
        M2mServerStatusDOs m2mServerStatuses = new M2mServerStatusDOs();
        final ConcurrentHashMap<Long, String> survivalServers = networkPool
                .getPositionToServer();
        List<M2mServerStatusDO> m2mServerStatusDOs = new ArrayList<>();
        for (Entry<Long, String> survivalServer : survivalServers.entrySet()) {
            M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
            m2mServerStatusDO.setId(survivalServer.getKey());
            m2mServerStatusDO.setIp(survivalServer.getValue());
            m2mServerStatusDO.setStatus(1);
            m2mServerStatusDOs.add(m2mServerStatusDO);
        }
        m2mServerStatuses.setM2mServerStatusDOs(m2mServerStatusDOs);
        M2mWebGetDataResponse m2mWebGetDataResponse = new M2mWebGetDataResponse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        m2mServerStatuses.serialize(boa, "m2mServerStatuses");
        byte[] bytes = baos.toByteArray();
        m2mWebGetDataResponse.setData(bytes);
        M2mWebPacket m2mPacket = new M2mWebPacket(
                m2mWebPacket.getM2mRequestHeader(),
                m2mWebPacket.getM2mReplyHeader(), m2mWebPacket.getRequest(),
                m2mWebGetDataResponse);
        channel.writeAndFlush(m2mPacket);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        ctx.attr(STATE).set(ctx.channel());
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

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
