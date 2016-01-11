package org.lab.mars.onem2m.txn;

import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mCsvOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mCreateTxn implements M2mRecord {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1255742539970640049L;
	private String path;
	private byte[] data;

	public M2mCreateTxn() {
	}

	public M2mCreateTxn(String path, byte[] data) {
		this.path = path;
		this.data = data;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String m_) {
		path = m_;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] m_) {
		data = m_;
	}

	public void serialize(M2mOutputArchive a_, String tag)
			throws java.io.IOException {
		a_.startRecord(this, tag);
		a_.writeString(path, "path");
		a_.writeBuffer(data, "data");
		a_.endRecord(this, tag);
	}

	public void deserialize(M2mInputArchive a_, String tag)
			throws java.io.IOException {
		a_.startRecord(tag);
		path = a_.readString("path");
		data = a_.readBuffer("data");
		a_.endRecord(tag);
	}

	public String toString() {
		try {
			java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
			M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
			a_.startRecord(this, "");
			a_.writeString(path, "path");
			a_.writeBuffer(data, "data");
			a_.endRecord(this, "");
			return new String(s.toByteArray(), "UTF-8");
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
		return "ERROR";
	}

	public void write(java.io.DataOutput out) throws java.io.IOException {
		M2mBinaryOutputArchive archive = new M2mBinaryOutputArchive(out);
		serialize(archive, "");
	}

	public void readFields(java.io.DataInput in) throws java.io.IOException {
		M2mBinaryInputArchive archive = new M2mBinaryInputArchive(in);
		deserialize(archive, "");
	}

	public int compareTo(Object peer_) throws ClassCastException {
		throw new UnsupportedOperationException(
				"comparing CreateTxn is unimplemented");
	}

	public boolean equals(Object peer_) {
		if (peer_ == this) {
			return true;
		}
		boolean ret = false;
		return ret;
	}

	public int hashCode() {
		int result = 17;

		return result;
	}

	public static String signature() {
		return "LCreateTxn(sB[LACL(iLId(ss))]zi)";
	}
}