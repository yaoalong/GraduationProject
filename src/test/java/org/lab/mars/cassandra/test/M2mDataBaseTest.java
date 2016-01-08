package org.lab.mars.cassandra.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;

public class M2mDataBaseTest {
	M2MDataBaseImpl m2mDataBase = new M2MDataBaseImpl(false, "tests",
			"student", "192.168.10.139");

	@Test
	public void test() {
		System.out.println(m2mDataBase.getLastProcessZxid());
		m2mDataBase.truncate((long) 4);
	}

	@Test
	public void testRetrieve() {

		M2mDataNode m2mDataNode = m2mDataBase.retrieve("14");
		System.out.println(m2mDataNode.getId());
		System.out.println(m2mDataNode.getLabel());
		System.out.println(m2mDataNode.getZxid());
	}

	@Test
	public void testGetLastProcessZxid() {
		System.out.println(m2mDataBase.getLastProcessZxid());
	}

	@Test
	public void testDelete() {
		m2mDataBase.delete("14");
	}

	@Test
	public void testRetrieve1() {

		List<M2mDataNode> m2mDataNodes = m2mDataBase.retrieve(22);
		for (M2mDataNode m2mDataNode : m2mDataNodes) {
			System.out.println(m2mDataNode.getId());
			System.out.println(m2mDataNode.getLabel());
			System.out.println(m2mDataNode.getZxid());
		}
	}
	@Test
	public void testUpdate(){
		Map<String,Object> map=new HashMap<String,Object>();
		//map.put("id", 54);
		//map.put("zxid", 222);
		map.put("data", 10);
		m2mDataBase.update("51634", map);
	}
}
