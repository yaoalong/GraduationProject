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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.LinkedBlockingQueue;

import org.lab.mars.onem2m.KeeperException;
import org.lab.mars.onem2m.KeeperException.Code;
import org.lab.mars.onem2m.MultiTransactionRecord;
import org.lab.mars.onem2m.Op;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.proto.M2mCreateRequest;
import org.lab.mars.onem2m.proto.M2mDeleteRequest;
import org.lab.mars.onem2m.proto.M2mSetDataRequest;
import org.lab.mars.onem2m.server.ZooKeeperServer.ChangeRecord;
import org.lab.mars.onem2m.server.quorum.Leader.XidRolloverException;
import org.lab.mars.onem2m.txn.M2mCreateTxn;
import org.lab.mars.onem2m.txn.M2mDeleteTxn;
import org.lab.mars.onem2m.txn.M2mErrorTxn;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This request processor is generally at the start of a RequestProcessor
 * change. It sets up any transactions associated with requests that change the
 * state of the system. It counts on ZooKeeperServer to update
 * outstandingRequests, so that it can take into account transactions that are
 * in the queue to be applied when generating a transaction.
 */
public class PrepRequestProcessor extends Thread implements RequestProcessor {
    private static final Logger LOG = LoggerFactory
            .getLogger(PrepRequestProcessor.class);

    static boolean skipACL;
    static {
        skipACL = System.getProperty("zookeeper.skipACL", "no").equals("yes");
        if (skipACL) {
            LOG.info("zookeeper.skipACL==\"yes\", ACL checks will be skipped");
        }
    }

    LinkedBlockingQueue<M2mRequest> submittedRequests = new LinkedBlockingQueue<M2mRequest>();

    RequestProcessor nextProcessor;

    ZooKeeperServer zks;

    public PrepRequestProcessor(ZooKeeperServer zks,
            RequestProcessor nextProcessor) {
        super("ProcessThread(sid:" + zks.getServerId() + " cport:"
                + zks.getClientPort() + "):");
        this.nextProcessor = nextProcessor;
        this.zks = zks;
    }

    /**
     * method for tests to set failCreate
     * 
     * @param b
     */
    public static void setFailCreate(boolean b) {
    }

    /**
     * 不断从submitedRequests中取出
     */
    @Override
    public void run() {
        try {
            while (true) {
                M2mRequest request = submittedRequests.take();
                pRequest(request);
            }
        } catch (InterruptedException e) {
            LOG.error("Unexpected interruption", e);
        } catch (RequestProcessorException e) {
            if (e.getCause() instanceof XidRolloverException) {
                LOG.info(e.getCause().getMessage());
            }
            LOG.error("Unexpected exception", e);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
        }
        LOG.info("PrepRequestProcessor exited loop!");
    }

    ChangeRecord getRecordForPath(String path)
            throws KeeperException.NoNodeException {
        ChangeRecord lastChange = null;
        synchronized (zks.outstandingChanges) {
            lastChange = zks.outstandingChangesForPath.get(path);

        }
        if (lastChange == null || lastChange.stat == null) {
            throw new KeeperException.NoNodeException(path);
        }
        return lastChange;
    }

    /*
     * 添加修改事件
     */
    void addChangeRecord(ChangeRecord c) {
        synchronized (zks.outstandingChanges) {
            zks.outstandingChanges.add(c);
            zks.outstandingChangesForPath.put(c.path, c);
        }
    }

    /**
     * Grab current pending change records for each op in a multi-op.
     * 
     * This is used inside MultiOp error code path to rollback in the event of a
     * failed multi-op.
     *
     * @param multiRequest
     */
    HashMap<String, ChangeRecord> getPendingChanges(
            MultiTransactionRecord multiRequest) {
        HashMap<String, ChangeRecord> pendingChangeRecords = new HashMap<String, ChangeRecord>();

        for (Op op : multiRequest) {
            String path = op.getPath();

            try {
                ChangeRecord cr = getRecordForPath(path);
                if (cr != null) {
                    pendingChangeRecords.put(path, cr);
                }
                /*
                 * ZOOKEEPER-1624 - We need to store for parent's ChangeRecord
                 * of the parent node of a request. So that if this is a
                 * sequential node creation request, rollbackPendingChanges()
                 * can restore previous parent's ChangeRecord correctly.
                 * 
                 * Otherwise, sequential node name generation will be incorrect
                 * for a subsequent request.
                 */
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || path.indexOf('\0') != -1) {
                    continue;
                }
                String parentPath = path.substring(0, lastSlash);
                ChangeRecord parentCr = getRecordForPath(parentPath);
                if (parentCr != null) {
                    pendingChangeRecords.put(parentPath, parentCr);
                }
            } catch (KeeperException.NoNodeException e) {
                // ignore this one
            }
        }

        return pendingChangeRecords;
    }

    /**
     * Rollback pending changes records from a failed multi-op.
     *
     * If a multi-op fails, we can't leave any invalid change records we created
     * around. We also need to restore their prior value (if any) if their prior
     * value is still valid.
     *
     * @param zxid
     * @param pendingChangeRecords
     */
    void rollbackPendingChanges(long zxid,
            HashMap<String, ChangeRecord> pendingChangeRecords) {

        synchronized (zks.outstandingChanges) {
            // Grab a list iterator starting at the END of the list so we can
            // iterate in reverse
            ListIterator<ChangeRecord> iter = zks.outstandingChanges
                    .listIterator(zks.outstandingChanges.size());
            while (iter.hasPrevious()) {
                ChangeRecord c = iter.previous();
                if (c.zxid == zxid) {
                    iter.remove();
                    zks.outstandingChangesForPath.remove(c.path);
                } else {
                    break;
                }
            }

            boolean empty = zks.outstandingChanges.isEmpty();
            long firstZxid = 0;
            if (!empty) {
                firstZxid = zks.outstandingChanges.get(0).zxid;
            }

            Iterator<ChangeRecord> priorIter = pendingChangeRecords.values()
                    .iterator();
            while (priorIter.hasNext()) {
                ChangeRecord c = priorIter.next();

                /* Don't apply any prior change records less than firstZxid */
                if (!empty && (c.zxid < firstZxid)) {
                    continue;
                }

                zks.outstandingChangesForPath.put(c.path, c);
            }
        }
    }

    /**
     * This method will be called inside the ProcessRequestThread, which is a
     * singleton, so there will be a single thread calling this code.
     *
     * @param type
     * @param zxid
     * @param request
     * @param record
     */
    protected void pRequest2Txn(int type, long zxid, M2mRequest request,
            M2mRecord record, boolean deserialize) throws KeeperException,
            IOException, RequestProcessorException {
        request.m2mTxnHeader = new M2mTxnHeader(request.cxid, zxid,
                zks.getTime(), type);
        switch (type) {
        case OpCode.create:
            M2mCreateRequest createRequest = (M2mCreateRequest) record;
            if (deserialize)
                M2mByteBufferInputStream.byteBuffer2Record(request.request,
                        createRequest);
            request.txn = new M2mCreateTxn(createRequest.getKey(),
                    createRequest.getData());
            break;
        case OpCode.delete:

            M2mDeleteRequest deleteRequest = (M2mDeleteRequest) record;
            if (deserialize)
                M2mByteBufferInputStream.byteBuffer2Record(request.request,
                        deleteRequest);
            request.txn = new M2mDeleteTxn(deleteRequest.getKey());
            break;
        case OpCode.setData:
            M2mSetDataRequest setDataRequest = (M2mSetDataRequest) record;
            if (deserialize)
                M2mByteBufferInputStream.byteBuffer2Record(request.request,
                        setDataRequest);
            request.txn = new M2mSetDataTxn(setDataRequest.getKey(),
                    setDataRequest.getData());
            break;

        }
    }

    /**
     * This method will be called inside the ProcessRequestThread, which is a
     * singleton, so there will be a single thread calling this code.
     *
     * @param request
     */
    protected void pRequest(M2mRequest request)
            throws RequestProcessorException {
        // LOG.info("Prep>>> cxid = " + request.cxid + " type = " +
        // request.type + " id = 0x" + Long.toHexString(request.sessionId));
        request.m2mTxnHeader = null;
        request.txn = null;

        try {
            switch (request.type) {
            case OpCode.create:

                M2mCreateRequest createRequest = new M2mCreateRequest();
                pRequest2Txn(request.type, zks.getNextZxid(), request,
                        createRequest, true);
                break;
            case OpCode.delete:
                M2mDeleteRequest deleteRequest = new M2mDeleteRequest();
                pRequest2Txn(request.type, zks.getNextZxid(), request,
                        deleteRequest, true);
                break;
            case OpCode.setData:
                M2mSetDataRequest setDataRequest = new M2mSetDataRequest();
                pRequest2Txn(request.type, zks.getNextZxid(), request,
                        setDataRequest, true);
                break;
            }
        } catch (KeeperException e) {
            if (request.m2mTxnHeader != null) {
                request.m2mTxnHeader.setType(OpCode.error);
                request.txn = new M2mErrorTxn(e.code().intValue());
            }
            LOG.info("Got user-level KeeperException when processing "
                    + request.toString() + " Error Path:" + e.getPath()
                    + " Error:" + e.getMessage());
            request.setException(e);
        } catch (Exception e) {
            // log at error level as we are returning a marshalling
            // error to the user
            LOG.error("Failed to process " + request, e);

            StringBuilder sb = new StringBuilder();
            ByteBuffer bb = request.request;
            if (bb != null) {
                bb.rewind();
                while (bb.hasRemaining()) {
                    sb.append(Integer.toHexString(bb.get() & 0xff));
                }
            } else {
                sb.append("request buffer is null");
            }

            LOG.error("Dumping request buffer: 0x" + sb.toString());
            if (request.m2mTxnHeader != null) {
                request.m2mTxnHeader.setType(OpCode.error);
                request.txn = new M2mErrorTxn(Code.MARSHALLINGERROR.intValue());
            }
        }
        request.zxid = zks.getZxid();

        nextProcessor.processRequest(request);
    }

    public void processRequest(M2mRequest m2mRequest) {
        // request.addRQRec(">prep="+zks.outstandingChanges.size());
        submittedRequests.add(m2mRequest);
    }

    public void shutdown() {
        LOG.info("Shutting down");
        submittedRequests.clear();
        // submittedRequests.add(Request.requestOfDeath);
        nextProcessor.shutdown();
    }
}
