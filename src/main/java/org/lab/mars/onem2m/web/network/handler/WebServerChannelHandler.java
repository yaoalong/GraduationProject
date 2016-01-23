package org.lab.mars.onem2m.web.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.ZKDatabase;
import org.lab.mars.onem2m.server.ZooKeeperServer;
import org.lab.mars.onem2m.web.network.WebTcpClient;
import org.lab.mars.onem2m.web.nework.protol.M2mServerStatusDO;
import org.lab.mars.onem2m.web.nework.protol.M2mServerStatusDOs;
import org.lab.mars.onem2m.web.nework.protol.M2mWebGetDataResponse;
import org.lab.mars.onem2m.web.nework.protol.M2mWebPacket;
import org.lab.mars.onem2m.web.nework.protol.M2mWebRetriveKeyResponse;
import org.lab.mars.onem2m.web.nework.protol.RetriveServerAndCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static Logger LOG = LoggerFactory
            .getLogger(WebServerChannelHandler.class);
    private NetworkPool networkPool;
    private ServerCnxnFactory serverCnxnFactory;

    public WebServerChannelHandler(ServerCnxnFactory serverCnxnFactory) {
        this.networkPool = serverCnxnFactory.getNetworkPool();
        this.serverCnxnFactory = serverCnxnFactory;

    }

    private int zxid = 0;
    static ConcurrentHashMap<Integer, RetriveServerAndCtx> result = new ConcurrentHashMap<Integer, RetriveServerAndCtx>();
    static ConcurrentHashMap<Integer, Integer> serverResult = new ConcurrentHashMap<Integer, Integer>();
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
                LOG.error("channelRead is error:because of:{}", e.getMessage());
            }
        } else if (m2mPacket.getM2mRequestHeader().getType() == 2) { // 查看本地是否包含一个key
            String key = m2mPacket.getM2mRequestHeader().getKey();
            final ConcurrentHashMap<String, ZooKeeperServer> zookeeperServers = serverCnxnFactory
                    .getZkServers();
            Set<String> servers = new HashSet<String>();
            for (Entry<String, ZooKeeperServer> entry : zookeeperServers
                    .entrySet()) {
                ZooKeeperServer zooKeeperServer = entry.getValue();
                ZKDatabase zkDatabase = zooKeeperServer.getZKDatabase();
                System.out.println("Key" + key);
                for (Entry<String, M2mDataNode> entry2 : zkDatabase
                        .getM2mData().getNodes().entrySet()) {
                    System.out.println("m2mDataNode:" + entry2.getKey());
                }
                if (zkDatabase.getM2mData().getNodes().containsKey(key)) {
                    servers.add(entry.getKey());
                }
            }
            m2mPacket.getM2mRequestHeader().setType(4);
            M2mWebPacket m2mWebPacket = new M2mWebPacket(
                    m2mPacket.getM2mRequestHeader(),
                    m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                    new M2mWebRetriveKeyResponse(servers));
            ctx.writeAndFlush(m2mWebPacket);
        } else if (m2mPacket.getM2mRequestHeader().getType() == 3) { // 要查看所有的key
            String server = networkPool.getSock(m2mPacket.getM2mRequestHeader()
                    .getKey());
            result.put(1, new RetriveServerAndCtx(ctx, new HashSet<String>()));
            serverResult.put(1, 0);
            m2mPacket.getM2mRequestHeader().setXid(1);
            m2mPacket.getM2mRequestHeader().setType(2);
            Long position = networkPool.getServerPosition().get(server);
            for (int i = 0; i < serverCnxnFactory.getReplicationFactor(); i++) {
                WebTcpClient tcpClient = new WebTcpClient();
                tcpClient
                        .connectionOne("localhost", NetworkPool.webPort
                                .get(networkPool.getPositionToServer().get(
                                        position++)));
                tcpClient.write(m2mPacket);
            }

        } else if (m2mPacket.getM2mRequestHeader().getType() == 4) {

        }
    }

    /**
     * 去查询所有的server的状态
     * 
     * @param m2mWebPacket
     * @param channel
     * @throws IOException
     */
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

    public int getNextZxid() {
        return zxid++;
    }

    public void setZxid(int zxid) {
        this.zxid = zxid;
    }

}
