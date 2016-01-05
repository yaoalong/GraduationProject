package org.lab.mars.onem2m.proto;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mCreateResponse implements M2mRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9213091370419848071L;

	private String path;

	public M2mCreateResponse() {
	}

	public M2mCreateResponse(String path) {
		this.path = path;
	}

	@Override
	public void serialize(M2mOutputArchive archive, String tag)
			throws IOException {
		archive.startRecord(this, tag);
		archive.writeString(path, "path");
		archive.endRecord(this, tag);

	}

	@Override
	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException {
		archive.startRecord(tag);
		path=archive.readString("path");
		archive.endRecord(tag);

	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
