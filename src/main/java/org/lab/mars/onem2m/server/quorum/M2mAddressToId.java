package org.lab.mars.onem2m.server.quorum;

public class M2mAddressToId {

    private Long sid;

    private String address;

    public M2mAddressToId(Long sid, String address) {
        this.sid = sid;
        this.address = address;
    }

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
