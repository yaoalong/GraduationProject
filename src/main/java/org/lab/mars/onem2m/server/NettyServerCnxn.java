package org.lab.mars.onem2m.server;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.lab.mars.onem2m.WatchedEvent;
import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.jute.BinaryOutputArchive;
import org.lab.mars.onem2m.jute.Record;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.ReplyHeader;
import org.lab.mars.onem2m.proto.WatcherEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServerCnxn extends ServerCnxn {
    Logger LOG = LoggerFactory.getLogger(NettyServerCnxn.class);
    Channel channel;
    ChannelBuffer queuedBuffer;
    volatile boolean throttled;
    ByteBuffer bb;
    ByteBuffer bbLen = ByteBuffer.allocate(4);
    long sessionId;
    int sessionTimeout;
    AtomicLong outstandingCount = new AtomicLong();

    /**
     * The ZooKeeperServer for this connection. May be null if the server is not
     * currently serving requests (for example if the server is not an active
     * quorum participant.
     */

    private ConcurrentHashMap<String, ZooKeeperServer> zookeeperServers;
    ServerCnxnFactory factory;
    boolean initialized;
    /**
     * 一致性hash环
     */
    private NetworkPool networkPool;

    public NettyServerCnxn(Channel ctx,
            ConcurrentHashMap<String, ZooKeeperServer> zooKeeperServers,
            ServerCnxnFactory serverCnxnFactory) {
        this.channel = ctx;
        this.zookeeperServers = zooKeeperServers;
        this.factory = serverCnxnFactory;

    }

    @Override
    public void close() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("close called for sessionid:0x"
                    + Long.toHexString(sessionId));
        }
        synchronized (factory.cnxns) {
            // if this is not in cnxns then it's already closed
            if (!factory.cnxns.remove(this)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("cnxns size:" + factory.cnxns.size());
                }
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("close in progress for sessionid:0x"
                        + Long.toHexString(sessionId));
            }
        }

        if (channel.isOpen()) {
            channel.close();
        }
        factory.unregisterConnection(this);
    }

    @Override
    public long getSessionId() {
        return sessionId;
    }

    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public void process(WatchedEvent event) {
        ReplyHeader h = new ReplyHeader(-1, -1L, 0);
        if (LOG.isTraceEnabled()) {
            ZooTrace.logTraceMessage(
                    LOG,
                    ZooTrace.EVENT_DELIVERY_TRACE_MASK,
                    "Deliver event " + event + " to 0x"
                            + Long.toHexString(this.sessionId) + " through "
                            + this);
        }

        // Convert WatchedEvent to a type that can be sent over the wire
        WatcherEvent e = event.getWrapper();

        try {
            sendResponse(h, e, "notification");
        } catch (IOException e1) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Problem sending to " + getRemoteSocketAddress(), e1);
            }
            close();
        }
    }

    private static final byte[] fourBytes = new byte[4];

    static class ResumeMessageEvent implements MessageEvent {
        Channel channel;

        ResumeMessageEvent(Channel channel) {
            this.channel = channel;
        }

        @Override
        public Object getMessage() {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public ChannelFuture getFuture() {
            return null;
        }

        @Override
        public org.jboss.netty.channel.Channel getChannel() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    /**
     * 发送对应的响应
     * 
     * @param h
     * @param r
     * @param tag
     * @throws IOException
     */
    @Override
    public void sendResponse(ReplyHeader h, Record r, String tag)
            throws IOException {
        if (!channel.isOpen()) {
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Make space for length
        BinaryOutputArchive bos = BinaryOutputArchive.getArchive(baos);
        try {
            baos.write(fourBytes);
            bos.writeRecord(h, "header");
            if (r != null) {
                bos.writeRecord(r, tag);
            }
            baos.close();
        } catch (IOException e) {
            LOG.error("Error serializing response");
        }
        byte b[] = baos.toByteArray();
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.putInt(b.length - 4).rewind();
        sendBuffer(bb);
        if (h.getXid() > 0) {
            // zks cannot be null otherwise we would not have gotten here!
            // if (!zkServer.shouldThrottle(outstandingCount.decrementAndGet()))
            // {
            // enableRecv();
            // }
        }
    }

    @Override
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void enableRecv() {
        if (throttled) {
            throttled = false;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending unthrottle event " + this);
            }

        }
    }

    @Override
    public void sendBuffer(ByteBuffer sendBuffer) {
        if (sendBuffer == ServerCnxnFactory.closeConn) {
            close();
            return;
        }
        channel.write(wrappedBuffer(sendBuffer));
        packetSent();
    }

    /**
     * 应该在这里进行判断在哪个ZkServer进行处理
     * 
     * @param ctx
     * @param m2mPacket
     */
    public void receiveMessage(ChannelHandlerContext ctx, M2mPacket m2mPacket) {
        String server = networkPool.getSock(m2mPacket.getM2mRequestHeader()
                .getKey());
        System.out.println("要处理的server:" + server);
        for (Entry<String, ZooKeeperServer> entry : zookeeperServers.entrySet()) {
            System.out.println("ent" + entry.getKey());
        }
        while (!zookeeperServers.containsKey(server)) {
            server = networkPool.getPositionToServer().get(
                    networkPool.getServerPosition().get(server) + 1L
                            % networkPool.getPositionToServer().size());
        }
        zookeeperServers.get(server).processPacket(ctx, m2mPacket);
    }

    @Override
    public long getOutstandingRequests() {
        return outstandingCount.longValue();
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    @Override
    public int getInterestOps() {
        return 0;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return null;
    }

    /**
     * Send close connection packet to the client.
     */
    @Override
    public void sendCloseSession() {
        sendBuffer(ServerCnxnFactory.closeConn);
    }

    @Override
    protected ServerStats serverStats() {
        return null;
    }

    @Override
    void disableRecv() {
        // TODO Auto-generated method stub

    }

    public void setNetworkPool(NetworkPool networkPool) {
        this.networkPool = networkPool;
    }

}
