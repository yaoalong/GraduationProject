package org.lab.mars.onem2m.web.nework.protol;

import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mCsvOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mWebGetDataResponse implements M2mRecord {

    /**
     * 
     */
    private static final long serialVersionUID = -1163005631887805265L;
    private byte[] data;
    private org.apache.zookeeper.data.Stat stat;

    public M2mWebGetDataResponse() {
    }

    public M2mWebGetDataResponse(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] m_) {
        data = m_;
    }

    public org.apache.zookeeper.data.Stat getStat() {
        return stat;
    }

    public void setStat(org.apache.zookeeper.data.Stat m_) {
        stat = m_;
    }

    public void serialize(M2mOutputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(this, tag);
        a_.writeBuffer(data, "data");
        a_.endRecord(this, tag);
    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(tag);
        data = a_.readBuffer("data");
        stat = new org.apache.zookeeper.data.Stat();
        a_.endRecord(tag);
    }

    public String toString() {
        try {
            java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
            M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
            a_.startRecord(this, "");
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

    public int hashCode() {
        int result = 17;
        int ret;
        ret = java.util.Arrays.toString(data).hashCode();
        result = 37 * result + ret;
        ret = stat.hashCode();
        result = 37 * result + ret;
        return result;
    }

    public static String signature() {
        return "LGetDataResponse(BLStat(lllliiiliil))";
    }

}
