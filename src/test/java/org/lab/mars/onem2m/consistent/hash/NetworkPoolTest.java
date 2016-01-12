package org.lab.mars.onem2m.consistent.hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
		for(Entry<String,Long> map:networkPool.getServerPosition().entrySet()){
			System.out.println("server address:"+map.getKey()+"  ,position:"+map.getValue());
		}
	}
}
