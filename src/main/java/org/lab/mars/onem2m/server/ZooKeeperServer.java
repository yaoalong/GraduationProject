/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab.mars.onem2m.server;

import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.lab.mars.onem2m.Environment;
import org.lab.mars.onem2m.KeeperException.SessionExpiredException;
import org.lab.mars.onem2m.data.ACL;
import org.lab.mars.onem2m.data.StatPersisted;
import org.lab.mars.onem2m.jmx.MBeanRegistry;
import org.lab.mars.onem2m.jute.BinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.persistence.FileTxnSnapLog;
import org.lab.mars.onem2m.proto.ConnectResponse;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.reflection.Person;
import org.lab.mars.onem2m.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.server.RequestProcessor.RequestProcessorException;
import org.lab.mars.onem2m.server.SessionTracker.Session;
import org.lab.mars.onem2m.server.SessionTracker.SessionExpirer;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple standalone ZooKeeperServer. It sets up the
 * following chain of RequestProcessors to process requests:
 * PrepRequestProcessor -> SyncRequestProcessor -> FinalRequestProcessor
 */
public class ZooKeeperServer implements SessionExpirer, ServerStats.Provider {
	protected static final Logger LOG;

	static {
		LOG = LoggerFactory.getLogger(ZooKeeperServer.class);

		Environment.logEnv("Server environment:", LOG);
	}

	protected ZooKeeperServerBean jmxServerBean;
	protected DataTreeBean jmxDataTreeBean;

	/**
	 * The server delegates loading of the tree to an instance of the interface
	 */
	public interface DataTreeBuilder {
		public DataTree build();
	}

	static public class BasicDataTreeBuilder implements DataTreeBuilder {
		public DataTree build() {
			return new DataTree();
		}
	}

	public static final int DEFAULT_TICK_TIME = 3000;
	protected int tickTime = DEFAULT_TICK_TIME;
	/** value of -1 indicates unset, use default */
	protected int minSessionTimeout = -1;
	/** value of -1 indicates unset, use default */
	protected int maxSessionTimeout = -1;
	protected SessionTracker sessionTracker;
	private FileTxnSnapLog txnLogFactory = null;
	private ZKDatabase zkDb;
	protected long hzxid = 0;
	public final static Exception ok = new Exception("No prob");
	protected RequestProcessor firstProcessor;
	protected volatile boolean running;

	/**
	 * This is the secret that we use to generate passwords, for the moment it
	 * is more of a sanity check.
	 */
	static final private long superSecret = 0XB3415C00L;

	int requestsInProcess;
	final List<ChangeRecord> outstandingChanges = new ArrayList<ChangeRecord>();
	// this data structure must be accessed under the outstandingChanges lock
	final HashMap<String, ChangeRecord> outstandingChangesForPath = new HashMap<String, ChangeRecord>();

	private ServerCnxnFactory serverCnxnFactory;

	private final ServerStats serverStats;

	/**
	 * Creates a ZooKeeperServer instance. Nothing is setup, use the setX
	 * methods to prepare the instance (eg datadir, datalogdir, ticktime,
	 * builder, etc...)
	 * 
	 * @throws IOException
	 */
	public ZooKeeperServer() {
		serverStats = new ServerStats(this);
	}

	/**
	 * Creates a ZooKeeperServer instance. It sets everything up, but doesn't
	 * actually start listening for clients until run() is invoked.
	 * 
	 * @param dataDir
	 *            the directory to put the data
	 */
	public ZooKeeperServer(FileTxnSnapLog txnLogFactory, int tickTime,
			int minSessionTimeout, int maxSessionTimeout,
			DataTreeBuilder treeBuilder, ZKDatabase zkDb) {
		serverStats = new ServerStats(this);
		this.txnLogFactory = txnLogFactory;
		this.zkDb = zkDb;
		this.tickTime = tickTime;
		this.minSessionTimeout = minSessionTimeout;
		this.maxSessionTimeout = maxSessionTimeout;

		LOG.info("Created server with tickTime " + tickTime
				+ " minSessionTimeout " + getMinSessionTimeout()
				+ " maxSessionTimeout " + getMaxSessionTimeout() + " datadir "
				+ txnLogFactory.getDataDir() + " snapdir "
				+ txnLogFactory.getSnapDir());
	}

	/**
	 * creates a zookeeperserver instance.
	 * 
	 * @param txnLogFactory
	 *            the file transaction snapshot logging class
	 * @param tickTime
	 *            the ticktime for the server
	 * @param treeBuilder
	 *            the datatree builder
	 * @throws IOException
	 */
	public ZooKeeperServer(FileTxnSnapLog txnLogFactory, int tickTime,
			DataTreeBuilder treeBuilder) throws IOException {
		this(txnLogFactory, tickTime, -1, -1, treeBuilder, new ZKDatabase(
				 null,null,null));
	}

	public ServerStats serverStats() {
		return serverStats;
	}

	public void dumpConf(PrintWriter pwriter) {
		pwriter.print("clientPort=");
		pwriter.println(getClientPort());
		pwriter.print("dataDir=");
		pwriter.print("dataLogDir=");
		pwriter.print("tickTime=");
		pwriter.println(getTickTime());
		pwriter.print("maxClientCnxns=");
		pwriter.println(serverCnxnFactory.getMaxClientCnxnsPerHost());
		pwriter.print("minSessionTimeout=");
		pwriter.println(getMinSessionTimeout());
		pwriter.print("maxSessionTimeout=");
		pwriter.println(getMaxSessionTimeout());

		pwriter.print("serverId=");
		pwriter.println(getServerId());
	}

	/**
	 * This constructor is for backward compatibility with the existing unit
	 * test code. It defaults to FileLogProvider persistence provider.
	 */
	public ZooKeeperServer(File snapDir, File logDir, int tickTime)
			throws IOException {
		this(new FileTxnSnapLog(snapDir, logDir), tickTime,
				new BasicDataTreeBuilder());
	}

	/**
	 * Default constructor, relies on the config for its agrument values
	 *
	 * @throws IOException
	 */
	public ZooKeeperServer(FileTxnSnapLog txnLogFactory,
			DataTreeBuilder treeBuilder) throws IOException {
		this(txnLogFactory, DEFAULT_TICK_TIME, -1, -1, treeBuilder,
				new ZKDatabase( null,null,null));
	}

	/**
	 * get the zookeeper database for this server
	 * 
	 * @return the zookeeper database for this server
	 */
	public ZKDatabase getZKDatabase() {
		return this.zkDb;
	}

	/**
	 * set the zkdatabase for this zookeeper server
	 * 
	 * @param zkDb
	 */
	public void setZKDatabase(ZKDatabase zkDb) {
		this.zkDb = zkDb;
	}

	/**
	 * Restore sessions and data
	 */
	public void loadData() throws IOException, InterruptedException {
		if (zkDb.isInitialized()) {
			setZxid(zkDb.getM2mData().getLastProcessedZxid());
		} else {
			setZxid(zkDb.loadDataBase());
		}

		zkDb.setM2mDataInit(true);
	}

	public void takeSnapshot() {

		// try {
		// txnLogFactory.save(zkDb.getDataTree(),
		// zkDb.getSessionWithTimeOuts());
		// } catch (IOException e) {
		// LOG.error("Severe unrecoverable error, exiting", e);
		// // This is a severe error that we cannot recover from,
		// // so we need to exit
		// System.exit(10);
		// }
	}

	/**
	 * This should be called from a synchronized block on this!
	 */
	synchronized public long getZxid() {
		return hzxid;
	}

	synchronized long getNextZxid() {
		return ++hzxid;
	}

	synchronized public void setZxid(long zxid) {
		hzxid = zxid;
	}

	long getTime() {
		return System.currentTimeMillis();
	}

	// private void close(long sessionId) {
	// submitRequest(null, sessionId, OpCode.closeSession, 0, null, null);
	// }

	// public void closeSession(long sessionId) {
	// LOG.info("Closing session 0x" + Long.toHexString(sessionId));
	//
	// // we do not want to wait for a session close. send it as soon as we
	// // detect it!
	// close(sessionId);
	// }

	protected void killSession(long sessionId, long zxid) {
		// zkDb.killSession(sessionId, zxid);
		// if (LOG.isTraceEnabled()) {
		// ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
		// "ZooKeeperServer --- killSession: 0x"
		// + Long.toHexString(sessionId));
		// }
		// if (sessionTracker != null) {
		// sessionTracker.removeSession(sessionId);
		// }
	}

	// public void expire(Session session) {
	// long sessionId = session.getSessionId();
	// LOG.info("Expiring session 0x" + Long.toHexString(sessionId)
	// + ", timeout of " + session.getTimeout() + "ms exceeded");
	// close(sessionId);
	// }

	public static class MissingSessionException extends IOException {
		private static final long serialVersionUID = 7467414635467261007L;

		public MissingSessionException(String msg) {
			super(msg);
		}
	}

	void touch(ServerCnxn cnxn) throws MissingSessionException {
		if (cnxn == null) {
			return;
		}
		long id = cnxn.getSessionId();
		int to = cnxn.getSessionTimeout();
		if (!sessionTracker.touchSession(id, to)) {
			throw new MissingSessionException("No session with sessionid 0x"
					+ Long.toHexString(id)
					+ " exists, probably expired and removed");
		}
	}

	protected void registerJMX() {
		// register with JMX
		// try {
		// jmxServerBean = new ZooKeeperServerBean(this);
		// MBeanRegistry.getInstance().register(jmxServerBean, null);
		//
		// try {
		// jmxDataTreeBean = new DataTreeBean(zkDb.getM2mData());
		// MBeanRegistry.getInstance().register(jmxDataTreeBean, jmxServerBean);
		// } catch (Exception e) {
		// LOG.warn("Failed to register with JMX", e);
		// jmxDataTreeBean = null;
		// }
		// } catch (Exception e) {
		// LOG.warn("Failed to register with JMX", e);
		// jmxServerBean = null;
		// }
	}

	public void startdata() throws IOException, InterruptedException {
		// check to see if zkDb is not null
		if (zkDb == null) {
			zkDb = new ZKDatabase( null,null,null);
		}
		if (!zkDb.isInitialized()) {
			loadData();
		}
	}

	public void startup() {
		if (sessionTracker == null) {
			createSessionTracker();
		}
		startSessionTracker();
		setupRequestProcessors();

		registerJMX();

		synchronized (this) {
			running = true;
			notifyAll();
		}
	}

	protected void setupRequestProcessors() {
		RequestProcessor finalProcessor = new FinalRequestProcessor(this);
		RequestProcessor syncProcessor = new SyncRequestProcessor(this,
				finalProcessor);
		((SyncRequestProcessor) syncProcessor).start();
		firstProcessor = new PrepRequestProcessor(this, syncProcessor);
		((PrepRequestProcessor) firstProcessor).start();
	}

	protected void createSessionTracker() {
		sessionTracker = new SessionTrackerImpl(this,
				zkDb.getSessionWithTimeOuts(), tickTime, 1);
	}

	protected void startSessionTracker() {
		((SessionTrackerImpl) sessionTracker).start();
	}

	public boolean isRunning() {
		return running;
	}

	public void shutdown() {
		LOG.info("shutting down");

		// new RuntimeException("Calling shutdown").printStackTrace();
		this.running = false;
		// Since sessionTracker and syncThreads poll we just have to
		// set running to false and they will detect it during the poll
		// interval.
		if (sessionTracker != null) {
			sessionTracker.shutdown();
		}
		if (firstProcessor != null) {
			firstProcessor.shutdown();
		}
		if (zkDb != null) {
			zkDb.clear();
		}

		unregisterJMX();
	}

	protected void unregisterJMX() {
		// unregister from JMX
		try {
			if (jmxDataTreeBean != null) {
				MBeanRegistry.getInstance().unregister(jmxDataTreeBean);
			}
		} catch (Exception e) {
			LOG.warn("Failed to unregister with JMX", e);
		}
		try {
			if (jmxServerBean != null) {
				MBeanRegistry.getInstance().unregister(jmxServerBean);
			}
		} catch (Exception e) {
			LOG.warn("Failed to unregister with JMX", e);
		}
		jmxServerBean = null;
		jmxDataTreeBean = null;
	}

	synchronized public void incInProcess() {
		requestsInProcess++;
	}

	synchronized public void decInProcess() {
		requestsInProcess--;
	}

	public int getInProcess() {
		return requestsInProcess;
	}

	/**
	 * This structure is used to facilitate information sharing between PrepRP
	 * and FinalRP.
	 */
	static class ChangeRecord {
		ChangeRecord(long zxid, String path, StatPersisted stat,
				int childCount, List<ACL> acl) {
			this.zxid = zxid;
			this.path = path;
			this.stat = stat;
			this.childCount = childCount;
			this.acl = acl;
		}

		long zxid;

		String path;

		StatPersisted stat; /* Make sure to create a new object when changing */

		int childCount;

		List<ACL> acl; /* Make sure to create a new object when changing */

		@SuppressWarnings("unchecked")
		ChangeRecord duplicate(long zxid) {
			StatPersisted stat = new StatPersisted();
			if (this.stat != null) {
				DataTree.copyStatPersisted(this.stat, stat);
			}
			return new ChangeRecord(zxid, path, stat, childCount,
					acl == null ? new ArrayList<ACL>() : new ArrayList(acl));
		}
	}

	byte[] generatePasswd(long id) {
		Random r = new Random(id ^ superSecret);
		byte p[] = new byte[16];
		r.nextBytes(p);
		return p;
	}

	// protected boolean checkPasswd(long sessionId, byte[] passwd) {
	// return sessionId != 0
	// && Arrays.equals(passwd, generatePasswd(sessionId));
	// }

	// long createSession(ChannelHandlerContext ctx, byte passwd[], int timeout)
	// {
	// long sessionId = sessionTracker.createSession(timeout);
	// Random r = new Random(sessionId ^ superSecret);
	// r.nextBytes(passwd);
	// ByteBuffer to = ByteBuffer.allocate(4);
	// to.putInt(timeout);
	// // cnxn.setSessionId(sessionId);
	// submitRequest(ctx, sessionId, OpCode.createSession, 0, to, null);
	// return sessionId;
	// }

	/**
	 * set the owner of this session as owner
	 * 
	 * @param id
	 *            the session id
	 * @param owner
	 *            the owner of the session
	 * @throws SessionExpiredException
	 */
	// public void setOwner(long id, Object owner) throws
	// SessionExpiredException {
	// sessionTracker.setOwner(id, owner);
	// }

	protected void revalidateSession(ServerCnxn cnxn, long sessionId,
			int sessionTimeout) throws IOException {
		boolean rc = sessionTracker.touchSession(sessionId, sessionTimeout);
		if (LOG.isTraceEnabled()) {
			ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
					"Session 0x" + Long.toHexString(sessionId) + " is valid: "
							+ rc);
		}
		finishSessionInit(cnxn, rc);
	}

	//
	// public void reopenSession(ServerCnxn cnxn, long sessionId, byte[] passwd,
	// int sessionTimeout) throws IOException {
	// if (!checkPasswd(sessionId, passwd)) {
	// finishSessionInit(cnxn, false);
	// } else {
	// revalidateSession(cnxn, sessionId, sessionTimeout);
	// }
	// }

	public void finishSessionInit(ServerCnxn cnxn, boolean valid) {
		// register with JMX
		try {
			if (valid) {
				serverCnxnFactory.registerConnection(cnxn);
			}
		} catch (Exception e) {
			LOG.warn("Failed to register with JMX", e);
		}

		try {
			ConnectResponse rsp = new ConnectResponse(0,
					valid ? cnxn.getSessionTimeout() : 0,
					valid ? cnxn.getSessionId() : 0, // send 0 if session is no
					// longer valid
					valid ? generatePasswd(cnxn.getSessionId()) : new byte[16]);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BinaryOutputArchive bos = BinaryOutputArchive.getArchive(baos);
			bos.writeInt(-1, "len");
			rsp.serialize(bos, "connect");

			baos.close();
			ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());
			bb.putInt(bb.remaining() - 4).rewind();
			cnxn.sendBuffer(bb);

			if (!valid) {
				LOG.info("Invalid session 0x"
						+ Long.toHexString(cnxn.getSessionId())
						+ " for client " + cnxn.getRemoteSocketAddress()
						+ ", probably expired");
				cnxn.sendBuffer(ServerCnxnFactory.closeConn);
			} else {
				LOG.info("Established session 0x"
						+ Long.toHexString(cnxn.getSessionId())
						+ " with negotiated timeout "
						+ cnxn.getSessionTimeout() + " for client "
						+ cnxn.getRemoteSocketAddress());
				cnxn.enableRecv();
			}

		} catch (Exception e) {
			LOG.warn("Exception while establishing session, closing", e);
			cnxn.close();
		}
	}

	// public void closeSession(ServerCnxn cnxn, RequestHeader requestHeader) {
	// closeSession(cnxn.getSessionId());
	// }

	public long getServerId() {
		return 0;
	}

	/**
	 * @param cnxn
	 * @param sessionId
	 * @param xidkio
	 * @param bb
	 */
	/*
	 * 添加
	 */

	public void submitRequest(M2mRequest si) {
		if (firstProcessor == null) {
			synchronized (this) {
				try {
					while (!running) {
						wait(1000);
					}
				} catch (InterruptedException e) {
					LOG.warn("Unexpected interruption", e);
				}
				if (firstProcessor == null) {
					throw new RuntimeException("Not started");
				}
			}
		}
		try {
			boolean validpacket = Request.isValid(si.type);// 判断请求是否是支持的type
			if (validpacket) {
				firstProcessor.processRequest(si);
			} else {
				LOG.warn("Received packet at server of unknown type " + si.type);
			}
			// } catch (MissingSessionException e) {
			// if (LOG.isDebugEnabled()) {
			// LOG.debug( "Dropping request: " + e.getMessage() );
			// }
		} catch (RequestProcessorException e) {
			LOG.error("Unable to process request:" + e.getMessage(), e);
		}
	}

	public static int getSnapCount() {
		String sc = System.getProperty("zookeeper.snapCount");
		try {
			int snapCount = Integer.parseInt(sc);

			// snapCount must be 2 or more. See
			// org.apache.zookeeper.server.SyncRequestProcessor
			if (snapCount < 2) {
				LOG.warn("SnapCount should be 2 or more. Now, snapCount is reset to 2");
				snapCount = 2;
			}
			return snapCount;
		} catch (Exception e) {
			return 100000;
		}
	}

	public int getGlobalOutstandingLimit() {
		String sc = System.getProperty("zookeeper.globalOutstandingLimit");
		int limit;
		try {
			limit = Integer.parseInt(sc);
		} catch (Exception e) {
			limit = 1000;
		}
		return limit;
	}

	public void setServerCnxnFactory(ServerCnxnFactory factory) {
		serverCnxnFactory = factory;
	}

	public ServerCnxnFactory getServerCnxnFactory() {
		return serverCnxnFactory;
	}

	/**
	 * return the last proceesed id from the datatree
	 */
	public long getLastProcessedZxid() {
		return zkDb.getM2mData().getLastProcessedZxid();
	}

	/**
	 * return the outstanding requests in the queue, which havent been processed
	 * yet
	 */
	public long getOutstandingRequests() {
		return getInProcess();
	}

	/**
	 * trunccate the log to get in sync with others if in a quorum
	 * 
	 * @param zxid
	 *            the zxid that it needs to get in sync with others
	 * @throws IOException
	 */
	/*
	 * TODO回滚操作
	 */
	public void truncateLog(long zxid) throws IOException {
		// this.zkDb.truncateLog(zxid);
	}

	public int getTickTime() {
		return tickTime;
	}

	public void setTickTime(int tickTime) {
		LOG.info("tickTime set to " + tickTime);
		this.tickTime = tickTime;
	}

	public int getMinSessionTimeout() {
		return minSessionTimeout == -1 ? tickTime * 2 : minSessionTimeout;
	}

	public void setMinSessionTimeout(int min) {
		LOG.info("minSessionTimeout set to " + min);
		this.minSessionTimeout = min;
	}

	public int getMaxSessionTimeout() {
		return maxSessionTimeout == -1 ? tickTime * 20 : maxSessionTimeout;
	}

	public void setMaxSessionTimeout(int max) {
		LOG.info("maxSessionTimeout set to " + max);
		this.maxSessionTimeout = max;
	}

	public int getClientPort() {
		return serverCnxnFactory != null ? serverCnxnFactory.getLocalPort()
				: -1;
	}

	public void setTxnLogFactory(FileTxnSnapLog txnLog) {
		this.txnLogFactory = txnLog;
	}

	public FileTxnSnapLog getTxnLogFactory() {
		return this.txnLogFactory;
	}

	public String getState() {
		return "standalone";
	}

	/**
	 * return the total number of client connections that are alive to this
	 * server
	 */
	public int getNumAliveConnections() {
		return serverCnxnFactory.getNumAliveConnections();
	}

	public void processConnectRequest(ChannelHandlerContext ctx, Person person) {
		ctx.writeAndFlush(person.toString());
	}

	public void processConnectRequest(ChannelHandlerContext ctx,
			ByteBuffer incomingBuffer) throws IOException {
		// BinaryInputArchive bia = BinaryInputArchive.getArchive(new
		// ByteBufferInputStream(incomingBuffer));
		// ConnectRequest connReq = new ConnectRequest();
		// connReq.deserialize(bia, "connect");
		// // if (LOG.isDebugEnabled()) {
		// // LOG.debug("Session establishment request from client "
		// // + cnxn.getRemoteSocketAddress()
		// // + " client's lastZxid is 0x"
		// // + Long.toHexString(connReq.getLastZxidSeen()));
		// // }
		// boolean readOnly = false;
		// try {
		// readOnly = bia.readBool("readOnly");
		// // cnxn.isOldClient = false;
		// } catch (IOException e) {
		// // this is ok -- just a packet from an old client which
		// // doesn't contain readOnly field
		// LOG.warn("Connection request from old client "
		// + cnxn.getRemoteSocketAddress()
		// + "; will be dropped if server is in r-o mode");
		// }
		// if (readOnly == false && this instanceof ReadOnlyZooKeeperServer) {
		// String msg = "Refusing session request for not-read-only client "
		// + cnxn.getRemoteSocketAddress();
		// LOG.info(msg);
		// throw new CloseRequestException(msg);
		// }
		// if (connReq.getLastZxidSeen() > zkDb.dataTree.lastProcessedZxid) {
		// String msg = "Refusing session request for client "
		// + cnxn.getRemoteSocketAddress()
		// + " as it has seen zxid 0x"
		// + Long.toHexString(connReq.getLastZxidSeen())
		// + " our last zxid is 0x"
		// + Long.toHexString(getZKDatabase().getDataTreeLastProcessedZxid())
		// + " client must try another server";
		//
		// LOG.info(msg);
		// throw new CloseRequestException(msg);
		// }
		// int sessionTimeout = connReq.getTimeOut();
		// byte passwd[] = connReq.getPasswd();
		// int minSessionTimeout = getMinSessionTimeout();
		// if (sessionTimeout < minSessionTimeout) {
		// sessionTimeout = minSessionTimeout;
		// }
		// int maxSessionTimeout = getMaxSessionTimeout();
		// if (sessionTimeout > maxSessionTimeout) {
		// sessionTimeout = maxSessionTimeout;
		// }
		// cnxn.setSessionTimeout(sessionTimeout);
		// // We don't want to receive any packets until we are sure that the
		// // session is setup
		// cnxn.disableRecv();
		// long sessionId = connReq.getSessionId();
		// if (sessionId != 0) {
		// long clientSessionId = connReq.getSessionId();
		// LOG.info("Client attempting to renew session 0x"
		// + Long.toHexString(clientSessionId)
		// + " at " + cnxn.getRemoteSocketAddress());
		// serverCnxnFactory.closeSession(sessionId);
		// cnxn.setSessionId(sessionId);
		// reopenSession(cnxn, sessionId, passwd, sessionTimeout);
		// } else {
		// LOG.info("Client attempting to establish new session at "
		// + cnxn.getRemoteSocketAddress());
		// createSession(cnxn, passwd, sessionTimeout);
		// }
	}

	public boolean shouldThrottle(long outStandingCount) {
		if (getGlobalOutstandingLimit() < getInProcess()) {
			return outStandingCount > 0;
		}
		return false;
	}

	/*
	 * 将请求发送给processor
	 */
	public void processPacket(ChannelHandlerContext ctx, M2mPacket m2mPacket) {
		M2mRequestHeader m2mRequestHeader = m2mPacket.getM2mRequestHeader();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
		try {
			m2mPacket.getRequest().serialize(boa, "request");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			baos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("xx:"+m2mRequestHeader.getType());
		M2mRequest m2mRequest = new M2mRequest(ctx, m2mRequestHeader.getXid(),
				m2mRequestHeader.getType(), ByteBuffer.wrap(baos.toByteArray()));
		submitRequest(m2mRequest);
	}

	/*
	 * 在这里处理事务请求,应用到数据数据库
	 */
	public ProcessTxnResult processTxn(M2mTxnHeader hdr, M2mRecord txn) {
		ProcessTxnResult rc;
		rc = getZKDatabase().processTxn(hdr, txn);
		return rc;
	}

	@Override
	public void expire(Session session) {

	}

}
