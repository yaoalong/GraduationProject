package org.lab.mars.onem2m.web.network.constant;

public enum OperateCode {

    getStatus(1, "查询服务器状态"), retriveLocalKey(2, "查询本地是否含有某个key"), retriveRemoteKey(
            3, "查询远程服务器是否含有某个key"), ReplyRetriverRemoteKey(4, "远程服务器对检索key的回复");

    private Integer code;
    private String desc;

    OperateCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
