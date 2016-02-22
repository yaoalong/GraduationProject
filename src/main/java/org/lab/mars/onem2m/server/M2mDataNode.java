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
    public int label;
    public long zxid;
    public String id;
    public int data;

    public long value;

    public Integer getLabel() {
        return label;
    }

    public Long getZxid() {
        return zxid;
    }

    public void setZxid(Long zxid) {
        this.zxid = zxid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(Integer label) {
        this.label = label;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        archive.startRecord(this, tag);
        archive.writeInt(label, "label");
        archive.writeLong(zxid, "zxid");
        archive.writeString(id, "id");
        archive.writeInt(data, "data");
        archive.endRecord(this, tag);

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        archive.startRecord("node");

        label = archive.readInt("label");

        zxid = archive.readLong("zxid");
        id = archive.readString("id");
        data = archive.readInt("data");
        archive.endRecord(tag);
    }

    public Integer getData() {
        return data;
    }

    public void setData(Integer data) {
        this.data = data;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

}
