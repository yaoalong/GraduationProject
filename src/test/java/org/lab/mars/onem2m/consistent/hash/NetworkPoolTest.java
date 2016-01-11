package org.lab.mars.onem2m.consistent.hash;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class NetworkPoolTest {
	@Test
	public void testMd5HashingAlg() {
		NetworkPool networkPool=new NetworkPool();
	    List<String> servers=new ArrayList<String>();
	    for(Integer i=0;i<10;i++){
	    	servers.add("192.168.10.10"+i);
	    }
	    networkPool.setServers(servers.toArray(new String[servers.size()]));
	    networkPool.initialize();
		String string = "key";
		for (int i = 0; i < 10; i++) {
			string += i;
			System.out.println(networkPool.getSock(string));
		}
	}
}
