package org.lab.mars.onem2m.proto;

import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mCsvOutputArchive;
import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mGetDataRequest implements M2mRecord {
    /**
     * 
     */
    private static final long serialVersionUID = -3945786396384809466L;
    private String path;

    public M2mGetDataRequest() {
    }

    public M2mGetDataRequest(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String m_) {
        path = m_;
    }

    public void serialize(M2mOutputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(this, tag);
        a_.writeString(path, "path");
        a_.endRecord(this, tag);
    }

    public void deserialize(M2mInputArchive a_, String tag)
            throws java.io.IOException {
        a_.startRecord(tag);
        path = a_.readString("path");
        a_.endRecord(tag);
    }

    public String toString() {
        try {
            java.io.ByteArrayOutputStream s = new java.io.ByteArrayOutputStream();
            M2mCsvOutputArchive a_ = new M2mCsvOutputArchive(s);
            a_.startRecord(this, "");
            a_.writeString(path, "path");
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
        if (!(peer_ instanceof GetDataRequest)) {
            throw new ClassCastException(
                    "Comparing different types of records.");
        }
        int ret = 0;
        if (ret != 0)
            return ret;
        if (ret != 0)
            return ret;
        return ret;
    }

    public boolean equals(Object peer_) {
        if (!(peer_ instanceof GetDataRequest)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        boolean ret = false;
        if (!ret)
            return ret;
        if (!ret)
            return ret;
        return ret;
    }

    public int hashCode() {
        int result = 17;
        int ret;
        ret = path.hashCode();
        result = 37 * result + ret;
        result = 37 * result + ret;
        return result;
    }

    public static String signature() {
        return "LGetDataRequest(sz)";
    }
}
