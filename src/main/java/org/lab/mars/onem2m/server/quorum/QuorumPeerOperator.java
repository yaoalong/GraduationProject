package org.lab.mars.onem2m.server.quorum;

import java.io.File;
import java.util.HashMap;

import org.lab.mars.onem2m.persistence.FileTxnSnapLog;
import org.lab.mars.onem2m.server.ZKDatabase;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;
import org.lab.mars.onem2m.server.quorum.QuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.flexible.QuorumMaj;

public class QuorumPeerOperator {
    public static QuorumPeerConfig config;

    public static void startQuorumPeer(HashMap<Long, QuorumServer> quorumPeers,
            String handleIp) {
        try {
            QuorumPeer quorumPeer = new QuorumPeer();

            quorumPeer = new QuorumPeer();
            quorumPeer.setHandleIp(handleIp);
            quorumPeer.setQuorumVerifier(new QuorumMaj(quorumPeers.size()));
            quorumPeer.setQuorumPeers(quorumPeers);// 设置对应的服务器信息
            quorumPeer.setElectionType(config.getElectionAlg());

            quorumPeer.setZKDatabase(new ZKDatabase(config.getNetworkPool(),
                    new M2MDataBaseImpl(config.m2mDataBase.isClean(),
                            config.m2mDataBase.getKeyspace(),
                            config.m2mDataBase.getTable(), config.m2mDataBase
                                    .getNode()), handleIp));
            quorumPeer.setClientPortAddress(config.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config
                    .getDataLogDir()), new File(config.getDataDir())));
            quorumPeer.setMyid(config.getServerId());
            quorumPeer.setTickTime(config.getTickTime());
            quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
            quorumPeer.setInitLimit(config.getInitLimit());
            quorumPeer.setSyncLimit(config.getSyncLimit());
            quorumPeer.setLearnerType(config.getPeerType());
            quorumPeer.setSyncEnabled(config.getSyncEnabled());
            quorumPeer.setMyIp(config.getMyIp());

            quorumPeer.start();
        } catch (Exception ex) {

        }

    }

}
