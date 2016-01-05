package org.lab.mars.onem2m.consistent.hash;

import org.junit.Test;

public class NetworkPoolTest {
	@Test
	public void testMd5HashingAlg() {
		String string = "key";
		for (int i = 0; i < 10; i++) {
			string += i;
			System.out.println(NetworkPool.md5HashingAlg(string));
		}

	}
}
