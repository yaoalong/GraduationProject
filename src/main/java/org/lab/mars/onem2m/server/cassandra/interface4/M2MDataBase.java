package org.lab.mars.onem2m.server.cassandra.interface4;

import java.util.Map;

import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.DataTree.ProcessTxnResult;
import org.lab.mars.onem2m.txn.M2mTxnHeader;
import java.util.List;

public interface M2MDataBase {

	public Long getLastProcessZxid();

	M2mDataNode retrieve(String key);

	List<M2mDataNode> retrieve(Integer key);

	Long create(Object object);

	Long delete(String key);

	Long update(String key, Map<String, Object> updated);

	boolean truncate(Long zxid);

	void close();

	public ProcessTxnResult processTxn(M2mTxnHeader header, M2mRecord m2mRecord);

}
