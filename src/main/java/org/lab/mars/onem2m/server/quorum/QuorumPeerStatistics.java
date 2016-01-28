package org.lab.mars.onem2m.server.quorum;

import java.util.concurrent.ConcurrentHashMap;

public class QuorumPeerStatistics {

    public static volatile ConcurrentHashMap<String, QuorumPeer> quorums = new ConcurrentHashMap<String, QuorumPeer>();

}
