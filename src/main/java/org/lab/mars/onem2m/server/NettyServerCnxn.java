package org.lab.mars.onem2m.server;

import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.lab.mars.onem2m.Environment;
import org.lab.mars.onem2m.Version;
import org.lab.mars.onem2m.WatchedEvent;
import org.lab.mars.onem2m.jute.BinaryOutputArchive;
import org.lab.mars.onem2m.jute.Record;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.ReplyHeader;
import org.lab.mars.onem2m.proto.WatcherEvent;
import org.lab.mars.onem2m.server.quorum.Leader;
import org.lab.mars.onem2m.server.quorum.LeaderZooKeeperServer;
import org.lab.mars.onem2m.server.util.OSMXBean;
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

    /** The ZooKeeperServer for this connection. May be null if the server
     * is not currently serving requests (for example if the server is not
     * an active quorum participant.
     */
    private volatile ZooKeeperServer zkServer;

    ServerCnxnFactory factory;
    boolean initialized;
    public NettyServerCnxn(Channel ctx,ZooKeeperServer zk,ServerCnxnFactory serverCnxnFactory){
    	this.channel=ctx;
    	this.zkServer=zk;
    	this.factory=serverCnxnFactory;
    	
    }
    @Override
    public void close() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("close called for sessionid:0x"
                    + Long.toHexString(sessionId));
        }
        synchronized(factory.cnxns){
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
            ZooTrace.logTraceMessage(LOG, ZooTrace.EVENT_DELIVERY_TRACE_MASK,
                                     "Deliver event " + event + " to 0x"
                                     + Long.toHexString(this.sessionId)
                                     + " through " + this);
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
        public Object getMessage() {return null;}
        @Override
        public SocketAddress getRemoteAddress() {return null;}

        @Override
        public ChannelFuture getFuture() {return null;}
		@Override
		public org.jboss.netty.channel.Channel getChannel() {
			// TODO Auto-generated method stub
			return null;
		}
    };
    
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
            if (!zkServer.shouldThrottle(outstandingCount.decrementAndGet())) {
                enableRecv();
            }
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
     * clean up the socket related to a command and also make sure we flush the
     * data before we do that
     * 
     * @param pwriter
     *            the pwriter for a command socket
     */
    private void cleanupWriterSocket(PrintWriter pwriter) {
        try {
            if (pwriter != null) {
                pwriter.flush();
                pwriter.close();
            }
        } catch (Exception e) {
            LOG.info("Error closing PrintWriter ", e);
        } finally {
            try {
                close();
            } catch (Exception e) {
                LOG.error("Error closing a command socket ", e);
            }
        }
    }

    /**
     * This class wraps the sendBuffer method of NIOServerCnxn. It is
     * responsible for chunking up the response to a client. Rather
     * than cons'ing up a response fully in memory, which may be large
     * for some commands, this class chunks up the result.
     */
    private class SendBufferWriter extends Writer {
        private StringBuffer sb = new StringBuffer();
        
        /**
         * Check if we are ready to send another chunk.
         * @param force force sending, even if not a full chunk
         */
        private void checkFlush(boolean force) {
            if ((force && sb.length() > 0) || sb.length() > 2048) {
                sendBuffer(ByteBuffer.wrap(sb.toString().getBytes()));
                // clear our internal buffer
                sb.setLength(0);
            }
        }

        @Override
        public void close() throws IOException {
            if (sb == null) return;
            checkFlush(true);
            sb = null; // clear out the ref to ensure no reuse
        }

        @Override
        public void flush() throws IOException {
            checkFlush(true);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            sb.append(cbuf, off, len);
            checkFlush(false);
        }
    }

    private static final String ZK_NOT_SERVING =
        "This ZooKeeper instance is not currently serving requests";
    
    /**
     * Set of threads for commmand ports. All the 4
     * letter commands are run via a thread. Each class
     * maps to a correspoding 4 letter command. CommandThread
     * is the abstract class from which all the others inherit.
     */
    private abstract class CommandThread /*extends Thread*/ {
        PrintWriter pw;
        
        CommandThread(PrintWriter pw) {
            this.pw = pw;
        }
        
        public void start() {
            run();
        }

        public void run() {
            try {
                commandRun();
            } catch (IOException ie) {
                LOG.error("Error in running command ", ie);
            } finally {
                cleanupWriterSocket(pw);
            }
        }
        
        public abstract void commandRun() throws IOException;
    }
    
    private class RuokCommand extends CommandThread {
        public RuokCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            pw.print("imok");
            
        }
    }
    
    private class TraceMaskCommand extends CommandThread {
        TraceMaskCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            long traceMask = ZooTrace.getTextTraceLevel();
            pw.print(traceMask);
        }
    }
    
    private class SetTraceMaskCommand extends CommandThread {
        long trace = 0;
        SetTraceMaskCommand(PrintWriter pw, long trace) {
            super(pw);
            this.trace = trace;
        }
        
        @Override
        public void commandRun() {
            pw.print(trace);
        }
    }
    
    private class EnvCommand extends CommandThread {
        EnvCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            List<Environment.Entry> env = Environment.list();

            pw.println("Environment:");
            for(Environment.Entry e : env) {
                pw.print(e.getKey());
                pw.print("=");
                pw.println(e.getValue());
            }
            
        } 
    }
    
    private class ConfCommand extends CommandThread {
        ConfCommand(PrintWriter pw) {
            super(pw);
        }
            
        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            } else {
                zkServer.dumpConf(pw);
            }
        }
    }
    
    private class StatResetCommand extends CommandThread {
        public StatResetCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            }
            else { 
                zkServer.serverStats().reset();
                pw.println("Server stats reset.");
            }
        }
    }
    
    private class CnxnStatResetCommand extends CommandThread {
        public CnxnStatResetCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            } else {
                synchronized(factory.cnxns){
                    for(ServerCnxn c : factory.cnxns){
                        c.resetStats();
                    }
                }
                pw.println("Connection stats reset.");
            }
        }
    }

    private class DumpCommand extends CommandThread {
        public DumpCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            }
            else {
                pw.println("SessionTracker dump:");
                zkServer.sessionTracker.dumpSessions(pw);
                pw.println("ephemeral nodes dump:");
              //  zkServer.dumpEphemerals(pw);
            }
        }
    }
    
    private class StatCommand extends CommandThread {
        int len;
        public StatCommand(PrintWriter pw, int len) {
            super(pw);
            this.len = len;
        }
        
        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            }
            else {   
                pw.print("Zookeeper version: ");
                pw.println(Version.getFullVersion());
                if (len == statCmd) {
                    LOG.info("Stat command output");
                    pw.println("Clients:");
                    // clone should be faster than iteration
                    // ie give up the cnxns lock faster
                    HashSet<ServerCnxn> cnxns;
                    synchronized(factory.cnxns){
                        cnxns = new HashSet<ServerCnxn>(factory.cnxns);
                    }
                    for(ServerCnxn c : cnxns){
                        c.dumpConnectionInfo(pw, true);
                        pw.println();
                    }
                    pw.println();
                }
                pw.print(zkServer.serverStats().toString());
                pw.print("Node count: ");
                pw.println(zkServer.getZKDatabase().getNodeCount());
            }
            
        }
    }
    
    private class ConsCommand extends CommandThread {
        public ConsCommand(PrintWriter pw) {
            super(pw);
        }
        
        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            } else {
                // clone should be faster than iteration
                // ie give up the cnxns lock faster
                AbstractSet<ServerCnxn> cnxns;
                synchronized (factory.cnxns) {
                    cnxns = new HashSet<ServerCnxn>(factory.cnxns);
                }
                for (ServerCnxn c : cnxns) {
                    c.dumpConnectionInfo(pw, false);
                    pw.println();
                }
                pw.println();
            }
        }
    }
    
    private class WatchCommand extends CommandThread {
        int len = 0;
        public WatchCommand(PrintWriter pw, int len) {
            super(pw);
            this.len = len;
        }

        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.println(ZK_NOT_SERVING);
            } else {
                M2mData dt = zkServer.getZKDatabase().getM2mData();
             
                pw.println();
            }
        }
    }

    private class MonitorCommand extends CommandThread {

        MonitorCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if(zkServer == null) {
                pw.println(ZK_NOT_SERVING);
                return;
            }
            ZKDatabase zkdb = zkServer.getZKDatabase();
            ServerStats stats = zkServer.serverStats();

            print("version", Version.getFullVersion());

            print("avg_latency", stats.getAvgLatency());
            print("max_latency", stats.getMaxLatency());
            print("min_latency", stats.getMinLatency());

            print("packets_received", stats.getPacketsReceived());
            print("packets_sent", stats.getPacketsSent());
            print("num_alive_connections", stats.getNumAliveClientConnections());

            print("outstanding_requests", stats.getOutstandingRequests());

            print("server_state", stats.getServerState());
            print("znode_count", zkdb.getNodeCount());

            OSMXBean osMbean = new OSMXBean();
            if (osMbean != null && osMbean.getUnix() == true) {
                print("open_file_descriptor_count", osMbean.getOpenFileDescriptorCount());
                print("max_file_descriptor_count", osMbean.getMaxFileDescriptorCount());
            }
          
            if(stats.getServerState().equals("leader")) {
                Leader leader = ((LeaderZooKeeperServer)zkServer).getLeader();

                print("followers", leader.getLearners().size());
                print("synced_followers", leader.getForwardingFollowers().size());
                print("pending_syncs", leader.getNumPendingSyncs());
            }
        }

        private void print(String key, long number) {
            print(key, "" + number);
        }

        private void print(String key, String value) {
            pw.print("zk_");
            pw.print(key);
            pw.print("\t");
            pw.println(value);
        }

    }

    private class IsroCommand extends CommandThread {

        public IsroCommand(PrintWriter pw) {
            super(pw);
        }

        @Override
        public void commandRun() {
            if (zkServer == null) {
                pw.print("null");
            }else {
                pw.print("rw");
            }
        }
    }

    /** Return if four letter word found and responded to, otw false **/
    private boolean checkFourLetterWord(final Channel channel,
            ChannelBuffer message, final int len) throws IOException
    {
        // We take advantage of the limited size of the length to look
        // for cmds. They are all 4-bytes which fits inside of an int
        String cmd = cmd2String.get(len);
        if (cmd == null) {
            return false;
        }
      
        packetReceived();

        final PrintWriter pwriter = new PrintWriter(
                new BufferedWriter(new SendBufferWriter()));
        if (len == ruokCmd) {
            RuokCommand ruok = new RuokCommand(pwriter);
            ruok.start();
            return true;
        } else if (len == getTraceMaskCmd) {
            TraceMaskCommand tmask = new TraceMaskCommand(pwriter);
            tmask.start();
            return true;
        } else if (len == setTraceMaskCmd) {
            ByteBuffer mask = ByteBuffer.allocate(4);
            message.readBytes(mask);

            bb.flip();
            long traceMask = mask.getLong();
            ZooTrace.setTextTraceLevel(traceMask);
            SetTraceMaskCommand setMask = new SetTraceMaskCommand(pwriter, traceMask);
            setMask.start();
            return true;
        } else if (len == enviCmd) {
            EnvCommand env = new EnvCommand(pwriter);
            env.start();
            return true;
        } else if (len == confCmd) {
            ConfCommand ccmd = new ConfCommand(pwriter);
            ccmd.start();
            return true;
        } else if (len == srstCmd) {
            StatResetCommand strst = new StatResetCommand(pwriter);
            strst.start();
            return true;
        } else if (len == crstCmd) {
            CnxnStatResetCommand crst = new CnxnStatResetCommand(pwriter);
            crst.start();
            return true;
        } else if (len == dumpCmd) {
            DumpCommand dump = new DumpCommand(pwriter);
            dump.start();
            return true;
        } else if (len == statCmd || len == srvrCmd) {
            StatCommand stat = new StatCommand(pwriter, len);
            stat.start();
            return true;
        } else if (len == consCmd) {
            ConsCommand cons = new ConsCommand(pwriter);
            cons.start();
            return true;
        } else if (len == wchpCmd || len == wchcCmd || len == wchsCmd) {
            WatchCommand wcmd = new WatchCommand(pwriter, len);
            wcmd.start();
            return true;
        } else if (len == mntrCmd) {
            MonitorCommand mntr = new MonitorCommand(pwriter);
            mntr.start();
            return true;
        } else if (len == isroCmd) {
            IsroCommand isro = new IsroCommand(pwriter);
            isro.start();
            return true;
        }
        return false;
    }
    /**
     * 应该在这里进行判断在哪个ZkServer进行处理
     * @param ctx
     * @param m2mPacket
     */
    public void receiveMessage(ChannelHandlerContext ctx,M2mPacket m2mPacket ){
    	
      zkServer.processPacket(ctx, m2mPacket);
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

    /** Send close connection packet to the client.
     */
    @Override
    public void sendCloseSession() {
        sendBuffer(ServerCnxnFactory.closeConn);
    }

    @Override
    protected ServerStats serverStats() {
        if (zkServer == null) {
            return null;
        }
        return zkServer.serverStats();
    }
	@Override
	void disableRecv() {
		// TODO Auto-generated method stub
		
	}

}

