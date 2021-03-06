package org.lab.mars.onem2m.server.cassandra.interface4;

import java.util.List;
import java.util.Map;

import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

public interface M2MDataBase {

    M2mDataNode retrieve(String key);

    List<M2mDataNode> retrieve(Long zxid);

    Long create(Object object);

    Long delete(String key);

    Long update(String key, Map<String, Object> updated);

    boolean truncate(Long zxid);

    List<M2mDataNode> getCertainData(Long low, Long high);

    Long getMaxZxid();

    void close();

    public ProcessTxnResult processTxn(M2mTxnHeader header, M2mRecord m2mRecord);

    public String getTable();

    public String getNode();

    public boolean isClean();

    public String getKeyspace();
}
