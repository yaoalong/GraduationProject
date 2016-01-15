package org.lab.mars.cassandra.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.lab.mars.onem2m.ZooDefs.OpCode;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;
import org.lab.mars.onem2m.txn.M2mSetDataTxn;
import org.lab.mars.onem2m.txn.M2mTxnHeader;

public class M2mDataBaseTest {
	M2MDataBaseImpl m2mDataBase = new M2MDataBaseImpl(false, "tests",
			"student2", "192.168.10.133");

	@Test
	public void test() {
		m2mDataBase.truncate((long) 4);
	}

	@Test
	public void testRetrieve() {

		M2mDataNode m2mDataNode = m2mDataBase.retrieve("3333430");
		System.out.println(m2mDataNode.getId());
		System.out.println(m2mDataNode.getLabel());
		System.out.println(m2mDataNode.getZxid());
	}


	@Test
	public void testDelete() {
		m2mDataBase.delete("51634");
	}

	@Test
	public void testRetrieve1() {

		List<M2mDataNode> m2mDataNodes = m2mDataBase.retrieve(111151634L);
		for (M2mDataNode m2mDataNode : m2mDataNodes) {
			System.out.println(m2mDataNode.getId());
			System.out.println(m2mDataNode.getLabel());
			System.out.println(m2mDataNode.getZxid());
		}
	}
	@Test
	public void testUpdate(){
		Map<String,Object> map=new HashMap<String,Object>();
		map.put("data", 100);
		m2mDataBase.update("51634", map);
	}
	@Test
	public void testCreate(){
		M2mDataNode m2mDataNode=new M2mDataNode();
		m2mDataNode.setZxid(Long.valueOf(140200+""));
		m2mDataNode.setData(2);
		m2mDataNode.setId(3333430+"");
		m2mDataNode.setLabel(0);
		m2mDataBase.create(m2mDataNode);
	}
	@Test
	public void testProcessTxn() throws IOException{
		M2mTxnHeader m2mTxnHeader=new M2mTxnHeader();
		m2mTxnHeader.setType(OpCode.setData);
		M2mSetDataTxn m2mSetDataTxn =new M2mSetDataTxn();
		m2mSetDataTxn.setPath("11111");;
		M2mDataNode m2mDataNode = new M2mDataNode();
		m2mDataNode.setId(11111+"");
		m2mDataNode.setLabel(0);
		m2mDataNode.setZxid(Long.valueOf("9999"));
		m2mDataNode.setData(1331);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
		m2mDataNode.serialize(boa, "m2mDataNode");
		m2mSetDataTxn.setData(baos.toByteArray());
		m2mDataBase.processTxn(m2mTxnHeader, m2mSetDataTxn);

		
	}
	
	@Test
	public void testMod(){
		System.out.println((-1+6)%6);
		
	}
}
