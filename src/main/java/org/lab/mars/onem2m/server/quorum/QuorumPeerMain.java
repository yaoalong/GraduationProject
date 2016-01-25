/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lab.mars.onem2m.server.quorum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.JMException;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.jmx.ManagedUtil;
import org.lab.mars.onem2m.persistence.FileTxnSnapLog;
import org.lab.mars.onem2m.server.NettyServerCnxnFactory;
import org.lab.mars.onem2m.server.ZKDatabase;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;
import org.lab.mars.onem2m.server.quorum.QuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.QuorumPeerConfig.ConfigException;
import org.lab.mars.onem2m.server.quorum.flexible.QuorumMaj;
import org.lab.mars.onem2m.servers.monitor.RegisterIntoZooKeeper;
import org.lab.mars.onem2m.servers.monitor.ZooKeeper_Monitor;
import org.lab.mars.onem2m.web.network.WebTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * <h2>Configuration file</h2>
 *
 * When the main() method of this class is used to start the program, the first
 * argument is used as a path to the config file, which will be used to obtain
 * configuration information. This file is a Properties file, so keys and values
 * are separated by equals (=) and the key/value pairs are separated by new
 * lines. The following is a general summary of keys used in the configuration
 * file. For full details on this see the documentation in docs/index.html
 * <ol>
 * <li>dataDir - The directory where the ZooKeeper data is stored.</li>
 * <li>dataLogDir - The directory where the ZooKeeper transaction log is stored.
 * </li>
 * <li>clientPort - The port used to communicate with clients.</li>
 * <li>tickTime - The duration of a tick in milliseconds. This is the basic unit
 * of time in ZooKeeper.</li>
 * <li>initLimit - The maximum number of ticks that a follower will wait to
 * initially synchronize with a leader.</li>
 * <li>syncLimit - The maximum number of ticks that a follower will wait for a
 * message (including heartbeats) from the leader.</li>
 * <li>server.<i>id</i> - This is the host:port[:port] that the server with the
 * given id will use for the quorum protocol.</li>
 * </ol>
 * In addition to the config file. There is a file in the data directory called
 * "myid" that contains the server id as an ASCII decimal value.
 *
 */
public class QuorumPeerMain {
    private static final Logger LOG = LoggerFactory
            .getLogger(QuorumPeerMain.class);

    private static final String USAGE = "Usage: QuorumPeerMain configfile";

    /**
     * To start the replicated server specify the configuration file name on the
     * command line.
     * 
     * @param args
     *            path to the configfile
     */
    public static void main(String[] args) {
        QuorumPeerMain main = new QuorumPeerMain();
        try {
            main.initializeAndRun(args);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid arguments, exiting abnormally", e);
            LOG.info(USAGE);
            System.err.println(USAGE);
            System.exit(2);
        } catch (ConfigException e) {
            LOG.error("Invalid config, exiting abnormally", e);
            System.err.println("Invalid config, exiting abnormally");
            System.exit(2);
        } catch (Exception e) {
            LOG.error("Unexpected exception, exiting abnormally", e);
            System.exit(1);
        }
        LOG.info("Exiting normally");
        System.exit(0);
    }

    protected void initializeAndRun(String[] args) throws ConfigException,
            IOException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        }
        if (args.length == 1 && config.servers.size() > 0) {
            runFromConfig(config);
        } else {
            LOG.error("Either no config or no quorum defined in config, running "
                    + " in standalone mode");
            System.exit(0);

        }
    }

    public void runFromConfig(QuorumPeerConfig config) throws IOException {
        try {
            ManagedUtil.registerLog4jMBeans();
        } catch (JMException e) {
            LOG.warn("Unable to register log4j JMX control", e);
        }

        LOG.info("Starting quorum peer");
        try {
            NetworkPool networkPool = new NetworkPool();
            NettyServerCnxnFactory cnxnFactory = new NettyServerCnxnFactory();
            cnxnFactory.setNetworkPool(networkPool);
            cnxnFactory.configure(config.getClientPortAddress().getPort(), 5);
            cnxnFactory.setMyIp(config.getMyIp());
            cnxnFactory.setAllServers(config.allServers);
            cnxnFactory.setReplicationFactory(config.getReplication_factor());// 设置复制因子
            cnxnFactory.setNetworkPool(config.getNetworkPool());
            cnxnFactory.setTemporyAdd(config.isTemporyAdd());
            List<QuorumPeer> quorumPeers = new ArrayList<QuorumPeer>();
            long minValue = config.isTemporyAdd() ? 1
                    : config.replication_factor;
            for (long i = 0; i < minValue; i++) {
                QuorumPeer quorumPeer = new QuorumPeer();
                M2mQuorumServer m2mQuorumServer = config.getM2mQuorumServers();
                HashMap<Long, QuorumServer> servers = m2mQuorumServer
                        .getPositionToServers().get(i);

                if (i == minValue - 1) {
                    quorumPeer = new QuorumPeer(true);
                } else {
                    quorumPeer = new QuorumPeer();
                }
                quorumPeer.setHandleIp(m2mQuorumServer.getServers().get(
                        Integer.valueOf((i) + "")));
                quorumPeer.setQuorumVerifier(new QuorumMaj(servers.size()));
                quorumPeer.setQuorumPeers(servers);// 设置对应的服务器信息
                quorumPeer.setElectionType(config.getElectionAlg());
                quorumPeer.setCnxnFactory(cnxnFactory);

                quorumPeer.setZKDatabase(new ZKDatabase(
                        config.getNetworkPool(), new M2MDataBaseImpl(
                                config.m2mDataBase.isClean(),
                                config.m2mDataBase.getKeyspace(),
                                config.m2mDataBase.getTable(),
                                config.m2mDataBase.getNode()), m2mQuorumServer
                                .getServers().get(Integer.valueOf((i) + ""))));
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
                quorumPeer.setQuorumListenOnAllIPs(config
                        .getQuorumListenOnAllIPs());
                RegisterIntoZooKeeper registerIntoZooKeeper = new RegisterIntoZooKeeper();
                registerIntoZooKeeper.setServer(config.getZooKeeperServer());
                ZooKeeper_Monitor zooKeeper_Monitor = new ZooKeeper_Monitor();
                zooKeeper_Monitor.setServer(config.getZooKeeperServer());
                zooKeeper_Monitor.setNetworkPool(networkPool);
                quorumPeer.setZooKeeper_Monitor(zooKeeper_Monitor);
                quorumPeer.setRegisterIntoZooKeeper(registerIntoZooKeeper);
                quorumPeer.setMyIp(config.getMyIp());

                quorumPeer.start();

                quorumPeers.add(quorumPeer);

            }
            WebTcpServer webTcpServer = new WebTcpServer(cnxnFactory);
            webTcpServer.bind(config.getMyIp(),
                    config.sidAndWebPort.get(config.serverId));
            for (QuorumPeer quorumPeer : quorumPeers) {
                quorumPeer.join();
            }
            webTcpServer.close();

        } catch (InterruptedException e) {
            // warn, but generally this is ok
            LOG.warn("Quorum Peer interrupted", e);
        }
    }
}
