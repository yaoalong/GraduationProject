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

package org.lab.mars.onem2m.server.util;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.BinaryInputArchive;
import org.lab.mars.onem2m.jute.InputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.OutputArchive;
import org.lab.mars.onem2m.jute.Record;
import org.lab.mars.onem2m.server.DataTree;
import org.lab.mars.onem2m.server.M2mData;
import org.lab.mars.onem2m.server.ZooTrace;
import org.lab.mars.onem2m.txn.CreateSessionTxn;
import org.lab.mars.onem2m.txn.CreateTxn;
import org.lab.mars.onem2m.txn.CreateTxnV0;
import org.lab.mars.onem2m.txn.DeleteTxn;
import org.lab.mars.onem2m.txn.ErrorTxn;
import org.lab.mars.onem2m.txn.MultiTxn;
import org.lab.mars.onem2m.txn.SetACLTxn;
import org.lab.mars.onem2m.txn.SetDataTxn;
import org.lab.mars.onem2m.txn.TxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeUtils {
    private static final Logger LOG = LoggerFactory
            .getLogger(SerializeUtils.class);

    public static Record deserializeTxn(byte txnBytes[], TxnHeader hdr)
            throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(txnBytes);
        InputArchive ia = BinaryInputArchive.getArchive(bais);

        hdr.deserialize(ia, "hdr");
        bais.mark(bais.available());
        Record txn = null;
        switch (hdr.getType()) {
        case OpCode.createSession:
            // This isn't really an error txn; it just has the same
            // format. The error represents the timeout
            txn = new CreateSessionTxn();
            break;
        case OpCode.closeSession:
            return null;
        case OpCode.create:
            txn = new CreateTxn();
            break;
        case OpCode.delete:
            txn = new DeleteTxn();
            break;
        case OpCode.setData:
            txn = new SetDataTxn();
            break;
        case OpCode.setACL:
            txn = new SetACLTxn();
            break;
        case OpCode.error:
            txn = new ErrorTxn();
            break;
        case OpCode.multi:
            txn = new MultiTxn();
            break;
        default:
            throw new IOException("Unsupported Txn with type=%d"
                    + hdr.getType());
        }
        if (txn != null) {
            try {
                txn.deserialize(ia, "txn");
            } catch (EOFException e) {
                // perhaps this is a V0 Create
                if (hdr.getType() == OpCode.create) {
                    CreateTxn create = (CreateTxn) txn;
                    bais.reset();
                    CreateTxnV0 createv0 = new CreateTxnV0();
                    createv0.deserialize(ia, "txn");
                    // cool now make it V1. a -1 parentCVersion will
                    // trigger fixup processing in processTxn
                    create.setPath(createv0.getPath());
                    create.setData(createv0.getData());
                    create.setAcl(createv0.getAcl());
                    create.setEphemeral(createv0.getEphemeral());
                    create.setParentCVersion(-1);
                } else {
                    throw e;
                }
            }
        }
        return txn;
    }

    public static void deserializeSnapshot(DataTree dt, InputArchive ia,
            Map<Long, Integer> sessions) throws IOException {
        int count = ia.readInt("count");
        while (count > 0) {
            long id = ia.readLong("id");
            int to = ia.readInt("timeout");
            sessions.put(id, to);
            if (LOG.isTraceEnabled()) {
                ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
                        "loadData --- session in archive: " + id
                                + " with timeout: " + to);
            }
            count--;
        }
        dt.deserialize(ia, "tree");
    }

    public static void serializeSnapshot(DataTree dt, OutputArchive oa,
            Map<Long, Integer> sessions) throws IOException {
        HashMap<Long, Integer> sessSnap = new HashMap<Long, Integer>(sessions);
        oa.writeInt(sessSnap.size(), "count");
        for (Entry<Long, Integer> entry : sessSnap.entrySet()) {
            oa.writeLong(entry.getKey().longValue(), "id");
            oa.writeInt(entry.getValue().intValue(), "timeout");
        }
        dt.serialize(oa, "tree");
    }

    public static void deserializeSnapshot(M2mData m2mData, M2mInputArchive ia)
            throws IOException {
        m2mData.deserialize(ia, "m2mData");
    }

    public static void serializeSnapshot(Long peerLast, M2mData m2mData,
            M2mOutputArchive oa) throws IOException {

        m2mData.serialize(peerLast, oa, "m2mData");
    }

}
