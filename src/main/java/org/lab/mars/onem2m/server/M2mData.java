package org.lab.mars.onem2m.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger LOG = LoggerFactory.getLogger(M2mData.class);
	private static final ConcurrentHashMap<Integer, M2mDataNode> nodes = new ConcurrentHashMap<Integer, M2mDataNode>();
	public boolean initialized = false;// 数据是否初始化

	public M2mData() {
		M2mDataNode m2mDataNode = new M2mDataNode();
	}

	public void addM2mDataNode(Integer key, M2mDataNode m2mDataNode) {
		nodes.put(key, m2mDataNode);
	}

	public Integer getNodeCount() {
		return nodes.size();
	}

	@Override
	public void serialize(M2mOutputArchive archive, String tag)
			throws IOException {
		archive.writeInt(nodes.size(), "count");
		for (Map.Entry<Integer, M2mDataNode> m2mDataNode : nodes.entrySet()) {
			archive.writeInt(m2mDataNode.getKey(), "key");
			archive.writeRecord(m2mDataNode.getValue(), "m2mDataNode");
		}

	}

	@Override
	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException {
		int count = archive.readInt("count");
		while (count > 0) {
			M2mDataNode m2mDataNode = new M2mDataNode();
			Integer key = archive.readInt("key");
			archive.readRecord(m2mDataNode, "m2mDataNode");
			nodes.put(key, m2mDataNode);
			count--;

		}

	}

	public ConcurrentHashMap<Integer, M2mDataNode> getNodes() {
		return nodes;
	}

	public void clear() {
		nodes.clear();
	}
}
