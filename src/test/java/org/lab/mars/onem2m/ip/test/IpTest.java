package org.lab.mars.onem2m.ip.test;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.junit.Test;

public class IpTest {
	@Test
	public void testGetIp() throws UnknownHostException, SocketException {
		 InetSocketAddress inetAddress=new InetSocketAddress("192.168.10.03", 22);
		 System.out.println(inetAddress.getHostName());
	}
}
