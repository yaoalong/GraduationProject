package org.lab.mars.onem2m.proto;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mDeleteRequest implements M2mRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2541426058581643372L;

	private String key;

	public M2mDeleteRequest() {

	}

	public M2mDeleteRequest(String key) {
		this.key = key;
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
		// TODO Auto-generated method stub
		archive.startRecord(this, tag);
		archive.writeString(key, "key");
		archive.endRecord(this, tag);

	}

	@Override
	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException {
		archive.startRecord(tag);
		key = archive.readString("key");
		archive.endRecord(tag);

	}

}
