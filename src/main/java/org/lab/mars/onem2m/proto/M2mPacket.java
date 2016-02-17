package org.lab.mars.onem2m.proto;

import java.io.IOException;

import org.lab.mars.onem2m.jute.M2mInputArchive;
import org.lab.mars.onem2m.jute.M2mOutputArchive;
import org.lab.mars.onem2m.jute.M2mRecord;

public class M2mPacket implements M2mRecord {

    /**
	 * 
	 */
    private static final long serialVersionUID = 8081949053926818924L;

    M2mRequestHeader m2mRequestHeader;

    M2mReplyHeader m2mReplyHeader;

    M2mRecord request;

    M2mRecord response;
    boolean finished;

    public M2mPacket(M2mRequestHeader m2mRequestHeader,
            M2mReplyHeader m2mReplyHeader, M2mRecord request, M2mRecord response) {
        this.m2mRequestHeader = m2mRequestHeader;
        this.m2mReplyHeader = m2mReplyHeader;
        this.request = request;
        this.response = response;
        this.finished = false;
    }

    public M2mRequestHeader getM2mRequestHeader() {
        return m2mRequestHeader;
    }

    public void setM2mRequestHeader(M2mRequestHeader m2mRequestHeader) {
        this.m2mRequestHeader = m2mRequestHeader;
    }

    public M2mReplyHeader getM2mReplyHeader() {
        return m2mReplyHeader;
    }

    public void setM2mReplyHeader(M2mReplyHeader m2mReplyHeader) {
        this.m2mReplyHeader = m2mReplyHeader;
    }

    public M2mRecord getRequest() {
        return request;
    }

    public void setRequest(M2mRecord request) {
        this.request = request;
    }

    public M2mRecord getResponse() {
        return response;
    }

    public void setResponse(M2mRecord response) {
        this.response = response;
    }

    @Override
    public void serialize(M2mOutputArchive archive, String tag)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deserialize(M2mInputArchive archive, String tag)
            throws IOException {
        // TODO Auto-generated method stub

    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

}
