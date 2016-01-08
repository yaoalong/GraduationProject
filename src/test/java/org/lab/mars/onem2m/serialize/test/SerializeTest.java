package org.lab.mars.onem2m.serialize.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.proto.M2mCreateRequest;
import org.lab.mars.onem2m.server.M2mData;

public class SerializeTest {
	static ByteArrayOutputStream baos = new ByteArrayOutputStream();
	static M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
	static byte[] bytes;

	@Test
	public void testSerialize() throws IOException {
		M2mData m2mData = new M2mData();
		m2mData.serialize(boa, "m2mData");
		bytes = baos.toByteArray();
		ByteArrayInputStream inbaos = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream( inbaos );
		M2mBinaryInputArchive inboa = M2mBinaryInputArchive.getArchive(dis);
		m2mData.deserialize(inboa, "m2mData");
	}

}
