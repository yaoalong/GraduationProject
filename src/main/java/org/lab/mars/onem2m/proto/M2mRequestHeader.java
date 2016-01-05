package org.lab.mars.onem2m.proto;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

/*
 * 需要在传统请求头上添加key,方便判断由哪个机器进行处理
 */
public class M2mRequestHeader implements M2mRecord {

	private int xid;
	private int type;
	private String key;

	/**
	 * 
	 */
	private static final long serialVersionUID = -6543894418218834453L;

	public M2mRequestHeader() {
	}

	public M2mRequestHeader(int xid, int type) {
		this.xid = xid;
		this.type = type;
	}

	public int getXid() {
		return xid;
	}

	public void setXid(int m_) {
		xid = m_;
	}

	public int getType() {
		return type;
	}

	public void setType(int m_) {
		type = m_;
	}

	@Override
	public void serialize(M2mOutputArchive archive, String tag)
			throws IOException {
		archive.startRecord(this, tag);
		archive.writeInt(xid, "xid");
		archive.writeInt(type, "type");
		archive.writeString(key, "key");
		archive.endRecord(this, tag);

	}

	@Override
	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException {
		archive.startRecord(tag);
		xid = archive.readInt("xid");
		type = archive.readInt("type");
		key = archive.readString("key");
		archive.endRecord(tag);

	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
