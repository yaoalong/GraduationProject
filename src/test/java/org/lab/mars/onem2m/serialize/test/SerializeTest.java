package org.lab.mars.onem2m.serialize.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.Test;
import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.server.M2mData;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;

public class SerializeTest {
	static ByteArrayOutputStream baos = new ByteArrayOutputStream();
	static M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
	static byte[] bytes;

	@Test
	public void testSerialize() throws IOException {
		M2mData m2mData = new M2mData(new M2MDataBaseImpl());
		M2mDataNode m2mDataNode=new M2mDataNode();
		m2mDataNode.setData(2);
		m2mDataNode.setId(4+"");
		m2mDataNode.setLabel(4);
		m2mDataNode.setZxid(5L);
		m2mData.serialize(boa, "m2mData");
		bytes = baos.toByteArray();
		ByteArrayInputStream inbaos = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream( inbaos );
		M2mBinaryInputArchive inboa = M2mBinaryInputArchive.getArchive(dis);
		m2mData.deserialize(inboa, "m2mData");
	}

}
