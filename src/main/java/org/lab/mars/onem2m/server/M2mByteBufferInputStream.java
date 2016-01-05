package org.lab.mars.onem2m.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lab.mars.onem2m.jute.M2mBinaryInputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mByteBufferInputStream extends InputStream {
    ByteBuffer bb;

    public M2mByteBufferInputStream(ByteBuffer bb) {
        this.bb = bb;
    }
 
    @Override
    public int read() throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        return bb.get() & 0xff;
    }

    @Override
    public int available() throws IOException {
        return bb.remaining();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        if (len > bb.remaining()) {
            len = bb.remaining();
        }
        bb.get(b, off, len);
        return len;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        long newPos = bb.position() + n;
        if (newPos > bb.remaining()) {
            n = bb.remaining();
        }
        bb.position(bb.position() + (int) n);
        return n;
    }
    /*
     * 从byteBuffer里面获取Record
     */
    static public void byteBuffer2Record(ByteBuffer bb, M2mRecord record)
            throws IOException {
        M2mBinaryInputArchive ia;
        ia = M2mBinaryInputArchive.getArchive(new M2mByteBufferInputStream(bb));
        record.deserialize(ia, "request");
    }

}
