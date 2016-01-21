package org.lab.mars.onem2m.web.nework.protol;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mWebReplyHeader implements M2mRecord {

    /**
     * 
     */
    private static final long serialVersionUID = -8072712128092074074L;

    private int xid;

    private long zxid;

    private int err;

    public M2mWebReplyHeader() {
    }

    public M2mWebReplyHeader(int xid, long zxid, int err) {
        this.xid = xid;
        this.zxid = zxid;
        this.err = err;
    }

    public int getXid() {
        return xid;
    }

    public void setXid(int m_) {
        xid = m_;
    }

    public long getZxid() {
        return zxid;
    }

    public void setZxid(long m_) {
        zxid = m_;
    }

    public int getErr() {
        return err;
    }

    public void setErr(int m_) {
        err = m_;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        archive.startRecord(this, tag);
        archive.writeInt(xid, "xid");
        archive.writeLong(zxid, "zxid");
        archive.writeInt(err, "err");
        archive.endRecord(this, tag);
    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        archive.startRecord(tag);
        xid = archive.readInt("xid");
        zxid = archive.readLong("zxid");
        err = archive.readInt("err");
        archive.endRecord(tag);

    }
}
