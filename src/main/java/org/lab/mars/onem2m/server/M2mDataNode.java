package org.lab.mars.onem2m.server;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mDataNode implements M2mRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3291328270207258803L;
	public Integer label;
	public Integer zxid;
	public Integer id;
	public Integer data;

	public Integer getLabel() {
		return label;
	}

	public void setLabel(Integer label) {
		this.label = label;
	}

	public Integer getZxid() {
		return zxid;
	}

	public void setZxid(Integer zxid) {
		this.zxid = zxid;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public void serialize(M2mOutputArchive archive, String tag)
			throws IOException {
		archive.startRecord(this, tag);
		archive.writeInt(label, "label");
		archive.writeInt(zxid, "zxid");
		archive.writeInt(id, "id");
		archive.writeInt(data, "data");
		archive.endRecord(this, tag);

	}

	@Override
	public void deserialize(M2mInputArchive archive, String tag)
			throws IOException {
		archive.startRecord("node");
		label = archive.readInt("label");
		zxid = archive.readInt("zxid");
		id = archive.readInt("id");
		data=archive.readInt("data");
		archive.endRecord(tag);
	}

	public Integer getData() {
		return data;
	}

	public void setData(Integer data) {
		this.data = data;
	}

}