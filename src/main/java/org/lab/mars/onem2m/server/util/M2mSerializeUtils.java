package org.lab.mars.onem2m.server.util;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.InputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.jute.OutputArchive;
import org.lab.mars.onem2m.server.DataTree;
import org.lab.mars.onem2m.server.ZooTrace;
import org.lab.mars.onem2m.txn.M2mCreateTxn;
import org.lab.mars.onem2m.txn.M2mDeleteTxn;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M2mSerializeUtils {
    private static final Logger LOG = LoggerFactory
            .getLogger(M2mSerializeUtils.class);

    public static M2mRecord deserializeTxn(byte txnBytes[], M2mTxnHeader hdr)
            throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(txnBytes);
        M2mInputArchive ia = M2mBinaryInputArchive.getArchive(bais);

        hdr.deserialize(ia, "hdr");
        bais.mark(bais.available());
        M2mRecord txn = null;
        switch (hdr.getType()) {
        case OpCode.create:
            txn = new M2mCreateTxn();
            break;
        case OpCode.delete:
            txn = new M2mDeleteTxn();
            break;
        case OpCode.setData:
            txn = new M2mSetDataTxn();
            break;
        default:
            throw new IOException("Unsupported Txn with type=%d"
                    + hdr.getType());
        }
        if (txn != null) {
            try {
                txn.deserialize(ia, "txn");
            } catch (EOFException e) {
                throw e;
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

}
