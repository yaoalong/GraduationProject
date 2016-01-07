package org.lab.mars.cassandra.test;

import org.junit.Test;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;

public class M2mDataBaseTest {
 
	@Test
	public void test(){
		M2MDataBaseImpl m2mDataBase=new M2MDataBaseImpl(false,"tests","student","192.168.10.139");
		System.out.println(m2mDataBase.getLastProcessZxid());
		
	}
}
