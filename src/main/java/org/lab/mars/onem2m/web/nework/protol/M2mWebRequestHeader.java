package org.lab.mars.onem2m.web.nework.protol;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mWebRequestHeader implements M2mRecord {
    private int xid;
    private int type;
    /**
     * 
     */
    private static final long serialVersionUID = -1242444569099197793L;

    public M2mWebRequestHeader() {
    }

    public M2mWebRequestHeader(int xid, int type) {
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
        archive.endRecord(this, tag);

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        archive.startRecord(tag);
        xid = archive.readInt("xid");
        type = archive.readInt("type");
        archive.endRecord(tag);

    }

}
