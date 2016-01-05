package org.lab.mars.onem2m.test;

import org.apache.zookeeper.ZooDefs;
import org.lab.mars.onem2m.proto.M2mCreateRequest;
import org.lab.mars.onem2m.proto.M2mCreateResponse;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.lab.mars.onem2m.proto.M2mRequestHeader;

public class Test {
	public static M2mPacket createM2mPacket() {
		String key = "yaoalong";
		M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
		m2mRequestHeader.setType(ZooDefs.OpCode.create);
		M2mCreateRequest m2mCreateRequest = new M2mCreateRequest();
		M2mCreateResponse m2mCreateResponse = new M2mCreateResponse();
		M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
		m2mCreateRequest.setKey(key);
		m2mCreateRequest.setData("yaoalong".getBytes());
		M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
				m2mCreateRequest, m2mCreateResponse);
		return m2mPacket;
	}

}
