package org.lab.mars.onem2m.web.network.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mBinaryOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;
import org.lab.mars.onem2m.proto.M2mReplyHeader;
import org.lab.mars.onem2m.proto.M2mRequestHeader;
import org.lab.mars.onem2m.web.nework.protol.M2mWebPacket;
import org.lab.mars.onem2m.web.nework.protol.M2mWebServerStatusResponse;

public class M2mWebPacketHandle {
    static final ByteArrayOutputStream baos;
    static final M2mBinaryOutputArchive boa;
    static {
        baos = new ByteArrayOutputStream();
        boa = M2mBinaryOutputArchive.getArchive(baos);
    }

    public static M2mWebPacket createM2mWebPacket(
            M2mRequestHeader m2mRequestHeader, M2mReplyHeader m2mReplyHeader,
            M2mRecord request, M2mRecord response, M2mRecord m2mRecord,
            String tag) throws IOException {
        if (response instanceof M2mWebServerStatusResponse) {
            M2mWebServerStatusResponse m2mWebServerStatusResponse = new M2mWebServerStatusResponse();
            m2mRecord.serialize(boa, tag);
            byte[] bytes = baos.toByteArray();
            m2mWebServerStatusResponse.setData(bytes);
            return new M2mWebPacket(m2mRequestHeader, m2mReplyHeader, request,
                    m2mWebServerStatusResponse);
        }
        return new M2mWebPacket(m2mRequestHeader, m2mReplyHeader, request,
                response);

    }
}
