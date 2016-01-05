package org.lab.mars.onem2m.txn;

import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mCsvOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mTxnHeader implements M2mRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3788714406614518665L;
	private int cxid;
	private long zxid;
	private long time;
	private int type;

	public M2mTxnHeader() {
	}

	public M2mTxnHeader(int cxid, long zxid, long time, int type) {
		this.cxid = cxid;
		this.zxid = zxid;
		this.time = time;
		this.type = type;
	}

	public int getCxid() {
		return cxid;
	}

	public void setCxid(int m_) {
		cxid = m_;
	}

	public long getZxid() {
		return zxid;
	}

	public void setZxid(long m_) {
		zxid = m_;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long m_) {
		time = m_;
	}

	public int getType() {
		return type;
	}

	public void setType(int m_) {
		type = m_;
	}

	public void serialize(M2mOutputArchive a_, String tag)
			throws java.io.IOException {
		a_.startRecord(this, tag);
		a_.writeInt(cxid, "cxid");
		a_.writeLong(zxid, "zxid");
		a_.writeLong(time, "time");
		a_.writeInt(type, "type");
		a_.endRecord(this, tag);
	}

	public void deserialize(M2mInputArchive a_, String tag)
			throws java.io.IOException {
		a_.startRecord(tag);
		cxid = a_.readInt("cxid");
		zxid = a_.readLong("zxid");
		time = a_.readLong("time");
		type = a_.readInt("type");
		a_.endRecord(tag);
	}

	public String toString() {
		try {
			java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
			M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
			a_.startRecord(this, "");
			a_.writeInt(cxid, "cxid");
			a_.writeLong(zxid, "zxid");
			a_.writeLong(time, "time");
			a_.writeInt(type, "type");
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

		int ret = 0;

		return ret;
	}

	public boolean equals(Object peer_) {

		boolean ret = false;

		return ret;
	}

	public int hashCode() {
		int result = 17;
		return result;
	}

	public static String signature() {
		return "LTxnHeader(lilli)";
	}

}
