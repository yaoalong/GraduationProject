package org.lab.mars.onem2m.web.nework.protol;

public enum M2mServerStatus {
    /**
     * 服务器状态枚举
     */
    STARTED(1), STOPED(0);

    M2mServerStatus(Integer status) {
        this.status = status;
    }

    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
