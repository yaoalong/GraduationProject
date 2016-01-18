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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.reflection.ResourceReflection;
import org.lab.mars.onem2m.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.server.cassandra.interface4.M2MDataBase;
import org.lab.mars.onem2m.server.quorum.Leader;
import org.lab.mars.onem2m.server.quorum.Leader.Proposal;
import org.lab.mars.onem2m.server.quorum.QuorumPacket;
import org.lab.mars.onem2m.server.util.SerializeUtils;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class maintains the in memory database of zookeeper server states that
 * includes the sessions, datatree and the committed logs. It is booted up after
 * reading the logs and snapshots from the disk.
 */
/*
 * 连接数据库以及内存数据
 */
public class ZKDatabase {

	private M2MDataBase m2mDataBase;
	private static final Logger LOG = LoggerFactory.getLogger(ZKDatabase.class);
	/**
	 * make sure on a clear you take care of all these members.
	 */
	// protected DataTree dataTree;
	protected M2mData m2mData;
	protected ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;
	// protected FileTxnSnapLog snapLog;
	protected long minCommittedLog, maxCommittedLog;
	public static final int commitLogCount = 500; // 临时会存储500个
	protected LinkedList<Proposal> committedLog = new LinkedList<Proposal>();// 提交的事务
	protected ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();
	volatile private boolean initialized = false;

    private NetworkPool networkPool;
	
	/*
	 * 并且进行加载
	 */
	public ZKDatabase(M2MDataBase m2mDataBase,String mySelfString) {
		this.m2mDataBase = m2mDataBase;
		m2mData = new M2mData(m2mDataBase);
		sessionsWithTimeouts = new ConcurrentHashMap<Long, Integer>();
		List<M2mDataNode> m2mDataNodes = m2mDataBase.retrieve(1L);
		for (M2mDataNode node : m2mDataNodes) {
			if(networkPool.getSock(node.getId()).equals(mySelfString)){
				m2mData.addM2mDataNode(node.getId(), node);	
			}

		}

	}

	/**
	 * checks to see if the zk database has been initialized or not.
	 * 
	 * @return true if zk database is initialized and false if not
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * clear the zkdatabase. Note to developers - be careful to see that the
	 * clear method does clear out all the data structures in zkdatabase.
	 */
	public void clear() {
		minCommittedLog = 0;
		maxCommittedLog = 0;
		/*
		 * to be safe we just create a new datatree.
		 */
		m2mData = new M2mData(m2mDataBase);
		sessionsWithTimeouts.clear();
		WriteLock lock = logLock.writeLock();
		try {
			lock.lock();
			committedLog.clear();
		} finally {
			lock.unlock();
		}
		initialized = false;
	}

	/**
	 * the datatree for this zkdatabase
	 * 
	 * @return the datatree for this zkdatabase
	 */
	public M2mData getM2mData() {
		return this.m2mData;
	}

	/**
	 * Get the lock that controls the committedLog. If you want to get the
	 * pointer to the committedLog, you need to use this lock to acquire a read
	 * lock before calling getCommittedLog()
	 * 
	 * @return the lock that controls the committed log
	 */
	// public ReentrantReadWriteLock getLogLock() {
	// return logLock;
	// }
	//

	public synchronized LinkedList<Proposal> getCommittedLog() {
		ReadLock rl = logLock.readLock();
		// only make a copy if this thread isn't already holding a lock
		if (logLock.getReadHoldCount() <= 0) {
			try {
				rl.lock();
				return new LinkedList<Proposal>(this.committedLog);
			} finally {
				rl.unlock();
			}
		}
		return this.committedLog;
	}

	/**
	 * get the last processed zxid from a datatree
	 * 
	 * @return the last processed zxid of a datatree
	 */

	/**
	 * set the datatree initialized or not
	 * 
	 * @param b
	 *            set the datatree initialized to b
	 */
	public void setM2mDataInit(boolean b) {
		m2mData.initialized = b;
	}

	/**
	 * get sessions with timeouts
	 * 
	 * @return the hashmap of sessions with timeouts
	 */
	public ConcurrentHashMap<Long, Integer> getSessionWithTimeOuts() {
		return sessionsWithTimeouts;
	}

	/**
	 * load the database from the disk onto memory and also add the transactions
	 * to the committedlog in memory.
	 * 
	 * @return the last valid zxid on disk
	 * @throws IOException
	 */
	public long loadDataBase() throws IOException {
	
		long zxid = m2mData.getLastProcessedZxid();
		initialized = true;
		return zxid;
	}

	/**
	 * maintains a list of last <i>committedLog</i> or so committed requests.
	 * This is used for fast follower synchronization.
	 * 
	 * @param request
	 *            committed request
	 */
	public void addCommittedProposal(M2mRequest request) {
		WriteLock wl = logLock.writeLock();
		try {
			wl.lock();
			if (committedLog.size() > commitLogCount) {
				committedLog.removeFirst();
				minCommittedLog = committedLog.getFirst().packet.getZxid();
			}
			if (committedLog.size() == 0) {
				minCommittedLog = request.zxid;
				maxCommittedLog = request.zxid;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			M2mBinaryOutputArchive boa = M2mBinaryOutputArchive
					.getArchive(baos);
			try {
				request.m2mTxnHeader.serialize(boa, "hdr");
				if (request.txn != null) {
					request.txn.serialize(boa, "txn");
				}
				baos.close();
			} catch (IOException e) {
				LOG.error("This really should be impossible", e);
			}
			QuorumPacket pp = new QuorumPacket(Leader.PROPOSAL, request.zxid,
					baos.toByteArray());
			Proposal p = new Proposal();
			p.packet = pp;
			p.m2mRequest = request;
			committedLog.add(p);
			maxCommittedLog = p.packet.getZxid();
		} finally {
			wl.unlock();
		}
	}

	/**
	 * the node count of the datatree
	 * 
	 * @return the node count of datatree
	 */
	public int getNodeCount() {
		return m2mData.getNodeCount();
	}

	/**
	 * the process txn on the data
	 * 
	 * @param hdr
	 *            the txnheader for the txn
	 * @param txn
	 *            the transaction that needs to be processed
	 * @return the result of processing the transaction on this
	 *         datatree/zkdatabase
	 */
	/*
	 * m2m内存数据库处理事务请求
	 */
	public ProcessTxnResult processTxn(M2mTxnHeader hdr, M2mRecord txn) {
		ProcessTxnResult processTxnResult= m2mDataBase.processTxn(hdr, txn);
		if(processTxnResult.zxid>m2mData.getLastProcessedZxid()){
			m2mData.setLastProcessedZxid(processTxnResult.zxid);
		}
		return processTxnResult;

	}

	public Object getNode(String key) {
		return m2mDataBase.retrieve(key);
	}

	/*
	 * 获取数据
	 */
	public Object getData(String key) {
		return m2mDataBase.retrieve(key);
	}

	/*
	 * 创建数据
	 */
	public Long createData(Object data) {
		return m2mDataBase.create(ResourceReflection.serialize(data));
	}

	public Long updateData(String key, Map<String, Object> updated) {
		return m2mDataBase.update(key, updated);
	}

	// /**
	// * Truncate the ZKDatabase to the specified zxid
	// * @param zxid the zxid to truncate zk database to
	// * @return true if the truncate is successful and false if not
	// * @throws IOException
	// */
	/*
	 *
	 */
	public boolean truncateLog(long zxid) throws IOException {
		clear();
		boolean truncated = m2mDataBase.truncate(zxid);
		if (!truncated) {
			return false;
		}
		return true;
	}

	/**
	 * deserialize a snapshot from an input archive
	 * 
	 * @param ia
	 *            the input archive you want to deserialize from
	 * @throws IOException
	 */
	public void deserializeSnapshot(M2mInputArchive ia) throws IOException {
		clear();
		SerializeUtils.deserializeSnapshot(getM2mData(), ia,
				getSessionWithTimeOuts());
		initialized = true;
	}

	/**
	 * serialize the snapshot
	 * 
	 * @param oa
	 *            the output archive to which the snapshot needs to be
	 *            serialized
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void serializeSnapshot( Long  peerLast,M2mOutputArchive oa) throws IOException,
			InterruptedException {
		SerializeUtils.serializeSnapshot(peerLast,getM2mData(), oa,
				getSessionWithTimeOuts());
	}

	/*
	 * 数据库的插入
	 */
	public void commit() {
		for (Entry<String, M2mDataNode> m2mDataNode : getM2mData().getNodes()
				.entrySet()) {
			m2mDataBase.create(m2mDataNode.getValue());

		}

	}

	/**
	 * append to the underlying transaction log
	 * 
	 * @param si
	 *            the request to append
	 * @return true if the append was succesfull and false if not
	 */
	// public boolean append(Request si) throws IOException {
	// return this.snapLog.append(si);
	// }

	/**
	 * roll the underlying log
	 */
	// public void rollLog() throws IOException {
	// this.snapLog.rollLog();
	// }

	/**
	 * commit to the underlying transaction log
	 * 
	 * @throws IOException
	 */
	// public void commit() throws IOException {
	// this.snapLog.commit();
	// }

	/**
	 * close this database. free the resources
	 * 
	 * @throws IOException
	 */
	// public void close() throws IOException {
	// this.snapLog.close();
	// }
	public void close() {

	}

	public long getMinCommittedLog() {
		return minCommittedLog;
	}

	public long getMaxCommittedLog() {
		return maxCommittedLog;
	}

	public ReentrantReadWriteLock getLogLock() {
		return logLock;
	}

	public void setLogLock(ReentrantReadWriteLock logLock) {
		this.logLock = logLock;
	}

	public void setCommittedLog(LinkedList<Proposal> committedLog) {
		this.committedLog = committedLog;
	}

	public M2MDataBase getM2mDataBase() {
		return m2mDataBase;
	}

	public void setM2mDataBase(M2MDataBase m2mDataBase) {
		this.m2mDataBase = m2mDataBase;
	}

	public void setM2mData(M2mData m2mData) {
		this.m2mData = m2mData;
	}

	public void setlastProcessedZxid(long zxid) {

	}

	public NetworkPool getNetworkPool() {
		return networkPool;
	}

	public void setNetworkPool(NetworkPool networkPool) {
		this.networkPool = networkPool;
	}
	

}
