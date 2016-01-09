package org.lab.mars.onem2m.proto;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.server.M2mDataNode;

public class M2mCreateRequest implements M2mRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8063966206269689445L;

	private String key;

	private byte[] data;

	public M2mCreateRequest() {

	}

	public M2mCreateRequest(String key, byte[] data) {
		this.key = key;
		this.data = data;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public void serialize(M2mOutputArchive archive, String tag)
			throws IOException {
		archive.startRecord(this, tag);
		archive.writeString(key, "key");
		archive.writeBuffer(data, "data");
		archive.endRecord(this, tag);

	}

	@Override
	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException {
		archive.startRecord(tag);
		key = archive.readString("key");
		data = archive.readBuffer("data");
		archive.endRecord(tag);

	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
