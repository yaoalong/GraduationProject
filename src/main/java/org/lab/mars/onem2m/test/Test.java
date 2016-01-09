package org.lab.mars.onem2m.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.lab.mars.onem2m.ZooDefs;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.proto.M2mCreateRequest;
import org.lab.mars.onem2m.proto.M2mCreateResponse;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.server.M2mDataNode;

public class Test {
	public static M2mPacket createM2mPacket() throws IOException {
		M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
		m2mRequestHeader.setType(ZooDefs.OpCode.create);
		M2mCreateRequest m2mCreateRequest = new M2mCreateRequest();
		M2mCreateResponse m2mCreateResponse = new M2mCreateResponse();
		M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
		m2mCreateRequest.setKey("1111");
		M2mDataNode m2mDataNode=new M2mDataNode();
		m2mDataNode.setId(11111);
		m2mDataNode.setLabel(0);
		m2mDataNode.setZxid(999);
		m2mDataNode.setData(11);
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
		 m2mDataNode.serialize(boa, "m2mData");
			byte[ ] bytes = baos.toByteArray();
		m2mCreateRequest.setData(bytes);
		M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
				m2mCreateRequest, m2mCreateResponse);
		return m2mPacket;
	}

}
