package org.lab.mars.onem2m;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.network.TcpClient;
import org.lab.mars.onem2m.proto.M2mCreateRequest;
import org.lab.mars.onem2m.proto.M2mCreateResponse;
import org.lab.mars.onem2m.proto.M2mDeleteRequest;
import org.lab.mars.onem2m.proto.M2mGetDataRequest;
import org.lab.mars.onem2m.proto.M2mGetDataResponse;
import org.lab.mars.onem2m.proto.M2mPacket;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.proto.M2mSetDataRequest;
import org.lab.mars.onem2m.reflection.ResourceReflection;
import org.lab.mars.onem2m.server.M2mDataNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author yaoalong
 * @Date 2016年2月20日
 * @Email yaoalong@foxmail.com
 *
 *        客户端入口
 */
public class OneM2m {
    private static final Logger LOG = LoggerFactory.getLogger(OneM2m.class);

    private TcpClient tcpClient;

    public OneM2m(String host, Integer port) {
        tcpClient = new TcpClient(new LinkedList<M2mPacket>());
        tcpClient.connectionOne(host, port);
    }

    public String create(final String path, byte[] data) throws IOException {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.create);
        m2mRequestHeader.setKey(path);
        M2mCreateRequest m2mCreateRequest = new M2mCreateRequest();
        M2mCreateResponse m2mCreateResponse = new M2mCreateResponse();
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        m2mCreateRequest.setKey(path);
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(11411 + "");
        m2mDataNode.setLabel(0);
        m2mDataNode.setZxid(999L);
        m2mDataNode.setData(11);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive boa = M2mBinaryOutputArchive.getArchive(baos);
        m2mDataNode.serialize(boa, "m2mData");
        byte[] bytes = baos.toByteArray();
        m2mCreateRequest.setData(bytes);
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mCreateRequest, m2mCreateResponse);
        tcpClient.write(m2mPacket);
        return "";
    }

    public void delete(final String path) {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.delete);
        m2mRequestHeader.setKey(path);
        M2mDeleteRequest m2mDeleteRequest = new M2mDeleteRequest(path);
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        M2mBinaryOutputArchive.getArchive(baos);
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mDeleteRequest, new M2mCreateResponse());
        tcpClient.write(m2mPacket);
    }

    public void setData(final String path, byte[] data) {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.setData);
        m2mRequestHeader.setKey(path);
        M2mSetDataRequest m2mSetDataRequest = new M2mSetDataRequest();
        M2mDataNode m2mDataNode = new M2mDataNode();
        m2mDataNode.setId(path);
        m2mDataNode.setData(1331);
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        m2mSetDataRequest
                .setData(ResourceReflection.serializeKryo(m2mDataNode));
        m2mSetDataRequest.setKey(path);
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mSetDataRequest, new M2mCreateResponse());
        tcpClient.write(m2mPacket);

    }

    public String getData(final String path) {
        M2mRequestHeader m2mRequestHeader = new M2mRequestHeader();
        m2mRequestHeader.setType(ZooDefs.OpCode.getData);
        m2mRequestHeader.setKey(path);
        M2mGetDataRequest m2mGetDataRequest = new M2mGetDataRequest();
        M2mReplyHeader m2mReplyHeader = new M2mReplyHeader();
        m2mGetDataRequest.setPath(path);
        M2mGetDataResponse m2mGetDataResponse = new M2mGetDataResponse();
        M2mPacket m2mPacket = new M2mPacket(m2mRequestHeader, m2mReplyHeader,
                m2mGetDataRequest, m2mGetDataResponse);
        tcpClient.write(m2mPacket);
        M2mDataNode m2mDataNode = (M2mDataNode) ResourceReflection
                .deserializeKryo(((M2mGetDataResponse) m2mPacket.getResponse())
                        .getData());
        return m2mDataNode.getData() + "";
    }

    public static void main(String args[]) {
        OneM2m oneM2m = new OneM2m("192.168.10.131", 2182);
        System.out.println("result:" + oneM2m.getData("11111"));
        oneM2m.delete("11411");
    }
}
