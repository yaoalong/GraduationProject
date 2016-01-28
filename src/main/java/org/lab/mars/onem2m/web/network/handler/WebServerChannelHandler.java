package org.lab.mars.onem2m.web.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.server.ZKDatabase;
import org.lab.mars.onem2m.server.ZooKeeperServer;
import org.lab.mars.onem2m.web.network.WebTcpClient;
import org.lab.mars.onem2m.web.network.constant.OperateCode;
import org.lab.mars.onem2m.web.nework.protol.M2mServerStatusDO;
import org.lab.mars.onem2m.web.nework.protol.M2mServerStatusDOs;
import org.lab.mars.onem2m.web.nework.protol.M2mWebPacket;
import org.lab.mars.onem2m.web.nework.protol.M2mWebRetriveKeyResponse;
import org.lab.mars.onem2m.web.nework.protol.M2mWebServerStatusResponse;
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

    private AtomicInteger zxid = new AtomicInteger(0);
    static ConcurrentHashMap<Integer, RetriveServerAndCtx> result = new ConcurrentHashMap<Integer, RetriveServerAndCtx>();
    static ConcurrentHashMap<Integer, Integer> serverResult = new ConcurrentHashMap<Integer, Integer>();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            System.out.println("接收到了数据");
            M2mWebPacket m2mPacket = (M2mWebPacket) msg;
            int operateType = m2mPacket.getM2mRequestHeader().getType();
            System.out.println(operateType + "操作类型");
            if (operateType == OperateCode.getStatus.getCode()) {
                try {
                    lookAllServerStatus(m2mPacket, ctx);
                } catch (IOException e) {
                    if (LOG.isInfoEnabled()) {
                        LOG.error("channelRead is error:because of:{}",
                                e.getMessage());
                    }
                }
            } else if (operateType == OperateCode.retriveLocalKey.getCode()) { // 查看本地是否包含一个key
                String key = m2mPacket.getM2mRequestHeader().getKey();
                if (key == null) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("key is null");
                    }
                }
                final ConcurrentHashMap<String, ZooKeeperServer> zookeeperServers = serverCnxnFactory
                        .getZkServers();
                Set<String> servers = new HashSet<String>();
                for (Entry<String, ZooKeeperServer> entry : zookeeperServers
                        .entrySet()) {
                    ZooKeeperServer zooKeeperServer = entry.getValue();
                    ZKDatabase zkDatabase = zooKeeperServer.getZKDatabase();
                    if (zkDatabase.getM2mData().getNodes().containsKey(key)) {
                        servers.add(entry.getKey());
                    }
                }
                m2mPacket.getM2mRequestHeader().setType(
                        OperateCode.ReplyRetriverRemoteKey.getCode());
                M2mWebPacket m2mWebPacket = new M2mWebPacket(
                        m2mPacket.getM2mRequestHeader(),
                        m2mPacket.getM2mReplyHeader(), m2mPacket.getRequest(),
                        new M2mWebRetriveKeyResponse(servers));
                ctx.writeAndFlush(m2mWebPacket);
            } else if (operateType == OperateCode.retriveRemoteKey.getCode()) {
                System.out.println("进入到这里");
                String server = networkPool.getSock(m2mPacket
                        .getM2mRequestHeader().getKey());
                int zxid = getNextZxid();
                result.put(zxid, new RetriveServerAndCtx(ctx,
                        new HashSet<String>()));
                serverResult.put(zxid, 0);
                m2mPacket.getM2mRequestHeader().setXid(zxid);
                m2mPacket.getM2mRequestHeader().setType(
                        OperateCode.retriveLocalKey.getCode());
                Long position = networkPool.getServerPosition().get(server);
                for (int i = 0; i < serverCnxnFactory.getReplicationFactor(); i++) {
                    WebTcpClient tcpClient = new WebTcpClient(
                            serverCnxnFactory.getReplicationFactor());
                    tcpClient.connectionOne(spilitString(networkPool
                            .getPositionToServer().get(position + i))[0],
                            NetworkPool.webPort.get(networkPool
                                    .getPositionToServer().get(position + i)));
                    tcpClient.write(m2mPacket);
                }

            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.info("invalid operate type : {}", m2mPacket
                            .getM2mRequestHeader().getType());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 去查询所有的server的状态
     * 
     * @param m2mWebPacket
     * @param channel
     * @throws IOException
     */
    public void lookAllServerStatus(M2mWebPacket m2mWebPacket,
            ChannelHandlerContext ctx) throws IOException {
        M2mServerStatusDOs m2mServerStatuses = new M2mServerStatusDOs();
        final ConcurrentHashMap<Long, String> survivalServers = networkPool
                .getPositionToServer();
        List<M2mServerStatusDO> m2mServerStatusDOs = new ArrayList<>();
        List<String> serverStrings = survivalServers
                .entrySet()
                .stream()
                .map(entry -> {
                    M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
                    m2mServerStatusDO.setId(entry.getKey());
                    m2mServerStatusDO.setIp(entry.getValue());
                    m2mServerStatusDO.setStatus(1);
                    m2mServerStatusDOs.add(m2mServerStatusDO);
                    return entry.getValue();
                }).collect(Collectors.toList());

        serverCnxnFactory.getAllServer().entrySet().stream().map(entry -> {
            if (!serverStrings.contains(entry.getKey())) {
                M2mServerStatusDO m2mServerStatusDO = new M2mServerStatusDO();
                m2mServerStatusDO.setId(entry.getValue());
                m2mServerStatusDO.setIp(entry.getKey());
                m2mServerStatusDO.setStatus(0);
                m2mServerStatusDOs.add(m2mServerStatusDO);
            }
            return entry.getKey();
        }).count();

        m2mServerStatuses.setM2mServerStatusDOs(m2mServerStatusDOs);
        M2mWebServerStatusResponse m2mWebServerStatusResponse = new M2mWebServerStatusResponse();
        M2mWebPacket m2mPacket = M2mWebPacketHandle.createM2mWebPacket(
                m2mWebPacket.getM2mRequestHeader(),
                m2mWebPacket.getM2mReplyHeader(), m2mWebPacket.getRequest(),
                m2mWebServerStatusResponse, m2mServerStatuses,
                "m2mWebServerStatuses");
        ctx.writeAndFlush(m2mPacket);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

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
        return zxid.getAndIncrement();
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
