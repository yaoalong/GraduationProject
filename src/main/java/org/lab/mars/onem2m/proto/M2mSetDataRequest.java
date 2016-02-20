package org.lab.mars.onem2m.proto;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mSetDataRequest implements M2mRecord {

    /**
	 * 
	 */
    private static final long serialVersionUID = -1185916255155713989L;

    private String key;
    private byte[] data;

    public M2mSetDataRequest() {

    }

    public M2mSetDataRequest(String key, byte[] data) {
        this.key = key;
        this.data = data;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        // TODO Auto-generated method stub
        archive.startRecord(this, tag);
        archive.writeString(key, "key");
        archive.writeBuffer(data, "data");
        archive.endRecord(this, tag);
    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        archive.startRecord(tag);
        key = archive.readString("key");
        data = archive.readBuffer("data");
        archive.endRecord(tag);

    }

}
