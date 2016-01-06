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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.Watcher;
import org.lab.mars.onem2m.data.ACL;
import org.lab.mars.onem2m.data.Stat;
import org.lab.mars.onem2m.jute.InputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.jute.OutputArchive;
import org.lab.mars.onem2m.persistence.FileTxnSnapLog;
import org.lab.mars.onem2m.reflection.ResourceReflection;
import org.lab.mars.onem2m.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.server.cassandra.interface4.M2MDataBase;
import org.lab.mars.onem2m.server.quorum.Leader.Proposal;
import org.lab.mars.onem2m.server.util.SerializeUtils;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

/**
 * This class maintains the in memory database of zookeeper
 * server states that includes the sessions, datatree and the
 * committed logs. It is booted up  after reading the logs
 * and snapshots from the disk.
 */
public class ZKDatabase {
    
    private M2MDataBase m2mDataBase;
    
    /**
     * make sure on a clear you take care of 
     * all these members.
     */
    protected DataTree dataTree;
    protected ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;
   // protected FileTxnSnapLog snapLog;
    protected long minCommittedLog, maxCommittedLog;
    //public static final int commitLogCount = 500;
    //protected static int commitLogBuffer = 700;
    protected LinkedList<Proposal> committedLog = new LinkedList<Proposal>();
    protected ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();
    volatile private boolean initialized = false;
    
    /**
     * the filetxnsnaplog that this zk database
     * maps to. There is a one to one relationship
     * between a filetxnsnaplog and zkdatabase.
     * @param snapLog the FileTxnSnapLog mapping this zkdatabase
     */
    
    public ZKDatabase(M2MDataBase m2mDataBase){
    	 dataTree = new DataTree();
         sessionsWithTimeouts = new ConcurrentHashMap<Long, Integer>();
    	this.m2mDataBase=m2mDataBase;
    }
    public ZKDatabase(FileTxnSnapLog snapLog) {
        dataTree = new DataTree();
        sessionsWithTimeouts = new ConcurrentHashMap<Long, Integer>();
    }
    
    /**
     * checks to see if the zk database has been
     * initialized or not.
     * @return true if zk database is initialized and false if not
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * clear the zkdatabase. 
     * Note to developers - be careful to see that 
     * the clear method does clear out all the
     * data structures in zkdatabase.
     */
    public void clear() {
        //minCommittedLog = 0;
        //maxCommittedLog = 0;
        /* to be safe we just create a new 
         * datatree.
         */
        dataTree = new DataTree();
        sessionsWithTimeouts.clear();
//        WriteLock lock = logLock.writeLock();
//        try {            
//            lock.lock();
//            committedLog.clear();
//        } finally {
//            lock.unlock();
//        }
        initialized = false;
    }
    
    /**
     * the datatree for this zkdatabase
     * @return the datatree for this zkdatabase
     */
    public DataTree getDataTree() {
        return this.dataTree;
    }
 
    /**
     * the committed log for this zk database
     * @return the committed log for this zkdatabase
     */
//    public long getmaxCommittedLog() {
//        return maxCommittedLog;
//    }
//    
    
    /**
     * the minimum committed transaction log
     * available in memory
     * @return the minimum committed transaction
     * log available in memory
     */
//    public long getminCommittedLog() {
//        return minCommittedLog;
//    }
    /**
     * Get the lock that controls the committedLog. If you want to get the pointer to the committedLog, you need
     * to use this lock to acquire a read lock before calling getCommittedLog()
     * @return the lock that controls the committed log
     */
//    public ReentrantReadWriteLock getLogLock() {
//        return logLock;
//    }
//    

//    public synchronized LinkedList<Proposal> getCommittedLog() {
//        ReadLock rl = logLock.readLock();
//        // only make a copy if this thread isn't already holding a lock
//        if(logLock.getReadHoldCount() <=0) {
//            try {
//                rl.lock();
//                return new LinkedList<Proposal>(this.committedLog);
//            } finally {
//                rl.unlock();
//            }
//        } 
//        return this.committedLog;
//    }      
    
    /**
     * get the last processed zxid from a datatree
     * @return the last processed zxid of a datatree
     */
    public long getDataTreeLastProcessedZxid() {
        return dataTree.lastProcessedZxid;
    }
    
    /**
     * set the datatree initialized or not
     * @param b set the datatree initialized to b
     */
    public void setDataTreeInit(boolean b) {
        dataTree.initialized = b;
    }
    
    /**
     * return the sessions in the datatree
     * @return the data tree sessions
     */
    public Collection<Long> getSessions() {
        return dataTree.getSessions();
    }
    
    /**
     * get sessions with timeouts
     * @return the hashmap of sessions with timeouts
     */
    public ConcurrentHashMap<Long, Integer> getSessionWithTimeOuts() {
        return sessionsWithTimeouts;
    }

    
    /**
     * load the database from the disk onto memory and also add 
     * the transactions to the committedlog in memory.
     * @return the last valid zxid on disk
     * @throws IOException
     */
    public long loadDataBase() throws IOException {
        long zxid=m2mDataBase.getLastProcessZxid();
        initialized = true;
        return zxid;
    }
    
    /**
     * maintains a list of last <i>committedLog</i>
     *  or so committed requests. This is used for
     * fast follower synchronization.
     * @param request committed request
     */
//    public void addCommittedProposal(Request request) {
//        WriteLock wl = logLock.writeLock();
//        try {
//            wl.lock();
//            if (committedLog.size() > commitLogCount) {
//                committedLog.removeFirst();
//                minCommittedLog = committedLog.getFirst().packet.getZxid();
//            }
//            if (committedLog.size() == 0) {
//                minCommittedLog = request.zxid;
//                maxCommittedLog = request.zxid;
//            }
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
//            try {
//                request.hdr.serialize(boa, "hdr");
//                if (request.txn != null) {
//                    request.txn.serialize(boa, "txn");
//                }
//                baos.close();
//            } catch (IOException e) {
//                LOG.error("This really should be impossible", e);
//            }
//            QuorumPacket pp = new QuorumPacket(Leader.PROPOSAL, request.zxid,
//                    baos.toByteArray(), null);
//            Proposal p = new Proposal();
//            p.packet = pp;
//            p.request = request;
//            committedLog.add(p);
//            maxCommittedLog = p.packet.getZxid();
//        } finally {
//            wl.unlock();
//        }
//    }

    
    /**
     * remove a cnxn from the datatree
     * @param cnxn the cnxn to remove from the datatree
     */
    public void removeCnxn(ServerCnxn cnxn) {
        dataTree.removeCnxn(cnxn);
    }

    /**
     * kill a given session in the datatree
     * @param sessionId the session id to be killed
     * @param zxid the zxid of kill session transaction
     */
    public void killSession(long sessionId, long zxid) {
        dataTree.killSession(sessionId, zxid);
    }

    /**
     * write a text dump of all the ephemerals in the datatree
     * @param pwriter the output to write to
     */
    public void dumpEphemerals(PrintWriter pwriter) {
        dataTree.dumpEphemerals(pwriter);
    }

    /**
     * the node count of the datatree
     * @return the node count of datatree
     */
    public int getNodeCount() {
        return dataTree.getNodeCount();
    }

    /**
     * the paths for  ephemeral session id 
     * @param sessionId the session id for which paths match to 
     * @return the paths for a session id
     */
//    public HashSet<String> getEphemerals(long sessionId) {
//        return dataTree.getEphemerals(sessionId);
//    }

    /**
     * the last processed zxid in the datatree
     * @param zxid the last processed zxid in the datatree
     */
    public void setlastProcessedZxid(long zxid) {
        dataTree.lastProcessedZxid = zxid;
    }

    /**
     * the process txn on the data
     * @param hdr the txnheader for the txn
     * @param txn the transaction that needs to be processed
     * @return the result of processing the transaction on this
     * datatree/zkdatabase
     */
    /*
     * m2m内存数据库处理事务请求
     */
    public  ProcessTxnResult processTxn(M2mTxnHeader hdr,M2mRecord txn){
    	return m2mDataBase.processTxn(hdr, txn);
    	
    }

    /**
     * stat the path 
     * @param path the path for which stat is to be done
     * @param serverCnxn the servercnxn attached to this request
     * @return the stat of this node
     * @throws KeeperException.NoNodeException
     */
    public Stat statNode(String path, ServerCnxn serverCnxn) throws KeeperException.NoNodeException {
        return dataTree.statNode(path, serverCnxn);
    }
    
    /**
     * get the datanode for this path
     * @param path the path to lookup
     * @return the datanode for getting the path
     */
//    public DataNode getNode(String path) {
//      return dataTree.getNode(path);
//    }
    
   

    /**
     * convert from long to the acl entry
     * @param aclL the long for which to get the acl
     * @return the acl corresponding to this long entry
     */
    public List<ACL> convertLong(Long aclL) {
        return dataTree.convertLong(aclL);
    }
    
    public Object getNode(String key){
    	return m2mDataBase.retrieve(key);
    }
    /*
     *获取数据
     */
    public Object getData(String key){
    	return m2mDataBase.retrieve(key);
    }
    /*
     * 创建数据
     */
    public Long createData(Object  data){
    	return m2mDataBase.create(ResourceReflection.serialize(data));
    }
    
    public Long updateData(String key, Map<String,Object> updated){
    	return m2mDataBase.update(key, updated);
    }
    /**
     * get acl for a path
     * @param path the path to query for acl
     * @param stat the stat for the node
     * @return the acl list for this path
     * @throws NoNodeException
     */
    public List<ACL> getACL(String path, Stat stat) throws NoNodeException {
        return dataTree.getACL(path, stat);
    }

    /**
     * get children list for this path
     * @param path the path of the node
     * @param stat the stat of the node
     * @param watcher the watcher function for this path
     * @return the list of children for this path
     * @throws KeeperException.NoNodeException
     */
    public List<String> getChildren(String path, Stat stat, Watcher watcher)
    throws KeeperException.NoNodeException {
        return dataTree.getChildren(path, stat, watcher);
    }

    /**
     * check if the path is special or not
     * @param path the input path
     * @return true if path is special and false if not
     */
    public boolean isSpecialPath(String path) {
        return dataTree.isSpecialPath(path);
    }

    /**
     * get the acl size of the datatree
     * @return the acl size of the datatree
     */
    public int getAclSize() {
        return dataTree.longKeyMap.size();
    }

//    /**
//     * Truncate the ZKDatabase to the specified zxid
//     * @param zxid the zxid to truncate zk database to
//     * @return true if the truncate is successful and false if not
//     * @throws IOException
//     */
//    public boolean truncateLog(long zxid) throws IOException {
//        clear();
//
//        // truncate the log
//        boolean truncated = snapLog.truncateLog(zxid);
//
//        if (!truncated) {
//            return false;
//        }
//
//        loadDataBase();
//        return true;
//    }
    
    /**
     * deserialize a snapshot from an input archive 
     * @param ia the input archive you want to deserialize from
     * @throws IOException
     */
    public void deserializeSnapshot(InputArchive ia) throws IOException {
        clear();
        SerializeUtils.deserializeSnapshot(getDataTree(),ia,getSessionWithTimeOuts());
        initialized = true;
    }   
    
    /**
     * serialize the snapshot
     * @param oa the output archive to which the snapshot needs to be serialized
     * @throws IOException
     * @throws InterruptedException
     */
    public void serializeSnapshot(OutputArchive oa) throws IOException,
    InterruptedException {
        SerializeUtils.serializeSnapshot(getDataTree(), oa, getSessionWithTimeOuts());
    }

    /**
     * append to the underlying transaction log 
     * @param si the request to append
     * @return true if the append was succesfull and false if not
     */
//    public boolean append(Request si) throws IOException {
//        return this.snapLog.append(si);
//    }

    /**
     * roll the underlying log
     */
//    public void rollLog() throws IOException {
//        this.snapLog.rollLog();
//    }

    /**
     * commit to the underlying transaction log
     * @throws IOException
     */
//    public void commit() throws IOException {
//        this.snapLog.commit();
//    }
    
    /**
     * close this database. free the resources
     * @throws IOException
     */
//    public void close() throws IOException {
//        this.snapLog.close();
//    }
    public void close(){
    	
    }
	public long getMinCommittedLog() {
		return minCommittedLog;
	}
	public void setMinCommittedLog(long minCommittedLog) {
		this.minCommittedLog = minCommittedLog;
	}
	public long getMaxCommittedLog() {
		return maxCommittedLog;
	}
	public void setMaxCommittedLog(long maxCommittedLog) {
		this.maxCommittedLog = maxCommittedLog;
	}
	public ReentrantReadWriteLock getLogLock() {
		return logLock;
	}
	public void setLogLock(ReentrantReadWriteLock logLock) {
		this.logLock = logLock;
	}
	public LinkedList<Proposal> getCommittedLog() {
		return committedLog;
	}
	public void setCommittedLog(LinkedList<Proposal> committedLog) {
		this.committedLog = committedLog;
	}

    
}
