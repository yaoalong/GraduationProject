package org.lab.mars.onem2m.data.synchronization.network.protol;

import java.io.Serializable;

public class DataGetRequest implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2318469970056909225L;

    private String ipAndPort;

    public String getIpAndPort() {
        return ipAndPort;
    }

    public void setIpAndPort(String ipAndPort) {
        this.ipAndPort = ipAndPort;
    }

}
