package org.lab.mars.onem2m.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.cassandra.interface4.M2MDataBase;

/**
 * M2m内存数据
 * 
 * @author Administrator
 *
 */
public class M2mData implements M2mRecord {
    /**
	 * 
	 */
    private static final long serialVersionUID = -5084501890442461767L;
    private static final ConcurrentHashMap<String, M2mDataNode> nodes = new ConcurrentHashMap<String, M2mDataNode>();
    public boolean initialized = false;
    private M2MDataBase m2mDataBase;
    private volatile long lastProcessedZxid = 0;

    public M2mData(M2MDataBase m2mDataBase) {
        this.m2mDataBase = m2mDataBase;
    }

    public void addM2mDataNode(String key, M2mDataNode m2mDataNode) {
        nodes.put(key, m2mDataNode);
        if (lastProcessedZxid < m2mDataNode.getZxid()) {
            lastProcessedZxid = m2mDataNode.getZxid();
        }
    }

    public Integer getNodeCount() {
        return nodes.size();
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        archive.writeInt(nodes.size(), "count");
        for (Map.Entry<String, M2mDataNode> m2mDataNode : nodes.entrySet()) {
            archive.writeString(m2mDataNode.getKey(), "key");
            archive.writeRecord(m2mDataNode.getValue(), "m2mDataNode");
        }

    }

    public void serialize(Long peerLast, M2mOutputArchive archive, String tag)
            throws IOException {
        List<M2mDataNode> dataNodes = m2mDataBase.retrieve(peerLast);
        archive.writeInt(dataNodes.size(), "count");
        for (M2mDataNode m2mDataNode : dataNodes) {
            archive.writeString(m2mDataNode.getId(), "key");
            archive.writeRecord(m2mDataNode, "m2mDataNode");
        }

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        int count = archive.readInt("count");
        while (count > 0) {
            M2mDataNode m2mDataNode = new M2mDataNode();
            String key = archive.readString("key");
            archive.readRecord(m2mDataNode, "m2mDataNode");
            nodes.put(key, m2mDataNode);
            count--;

        }
    }

    public ConcurrentHashMap<String, M2mDataNode> getNodes() {
        return nodes;
    }

    public void clear() {
        nodes.clear();
    }

    public Long getLastProcessedZxid() {
        return lastProcessedZxid;
    }

    public void setLastProcessedZxid(Long lastProcessedZxid) {
        this.lastProcessedZxid = lastProcessedZxid;
    }

    public M2MDataBase getM2mDataBase() {
        return m2mDataBase;
    }

    public void setM2mDataBase(M2MDataBase m2mDataBase) {
        this.m2mDataBase = m2mDataBase;
    }

}
