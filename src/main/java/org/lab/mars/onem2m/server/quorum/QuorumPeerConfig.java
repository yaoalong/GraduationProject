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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.server.ZooKeeperServer;
import org.lab.mars.onem2m.server.cassandra.impl.M2MDataBaseImpl;
import org.lab.mars.onem2m.server.cassandra.interface4.M2MDataBase;
import org.lab.mars.onem2m.server.quorum.QuorumPeer.LearnerType;
import org.lab.mars.onem2m.server.quorum.QuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.flexible.QuorumHierarchical;
import org.lab.mars.onem2m.server.quorum.flexible.QuorumMaj;
import org.lab.mars.onem2m.server.quorum.flexible.QuorumVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class QuorumPeerConfig {
    private static final Logger LOG = LoggerFactory
            .getLogger(QuorumPeerConfig.class);

    protected InetSocketAddress clientPortAddress;
    protected String dataDir;
    protected String dataLogDir;
    protected int tickTime = ZooKeeperServer.DEFAULT_TICK_TIME;
    protected int maxClientCnxns = 60;
    /** defaults to -1 if not set explicitly */
    protected int minSessionTimeout = -1;
    /** defaults to -1 if not set explicitly */
    protected int maxSessionTimeout = -1;

    protected int initLimit;
    protected int syncLimit;
    protected int electionAlg = 3;
    protected int electionPort = 2182;
    protected boolean quorumListenOnAllIPs = false;
    protected final HashMap<Long, QuorumServer> servers = new HashMap<Long, QuorumServer>(); // sid、服务器选举的配置信息

    /**
     * 对应的是不同的Zab需要监听的QuorumServer
     */
    protected final HashMap<Long, HashMap<Long, QuorumServer>> positionToServers = new HashMap<>();

    protected HashMap<Long, Integer> sidAndWebPort = new HashMap<Long, Integer>();
    protected long serverId;
    protected String myIp;
    protected HashMap<Long, Long> serverWeight = new HashMap<Long, Long>();
    protected HashMap<Long, Long> serverGroup = new HashMap<Long, Long>();
    protected int numGroups = 0;
    protected QuorumVerifier quorumVerifier;
    protected int snapRetainCount = 3;
    protected boolean syncEnabled = true;

    protected LearnerType peerType = LearnerType.PARTICIPANT;

    protected M2MDataBase m2mDataBase;

    protected Boolean cleaned = false;
    protected String node = "127.0.0.1";
    protected String keySpace = "tests";
    protected String table = "student";

    protected String zooKeeperServer;
    protected Integer replication_factor;
    protected Integer clientPort;

    /**
     * 用这个来判断自己在环中的位置
     */
    protected NetworkPool networkPool;

    /**
     * 一个服务器对应的sid
     */

    protected final Map<String, Long> allServers = new HashMap<String, Long>();
    protected List<M2mAddressToId> addressToSid = new ArrayList<>();

    M2mQuorumServer m2mQuorumServers = new M2mQuorumServer();

    private HashMap<Long, Integer> sidToClientPort = new HashMap<>();

    private boolean isTemporyAdd = false;

    private Integer webPort;

    @SuppressWarnings("serial")
    public static class ConfigException extends Exception {
        public ConfigException(String msg) {
            super(msg);
        }

        public ConfigException(String msg, Exception e) {
            super(msg, e);
        }
    }

    /**
     * Parse a ZooKeeper configuration file
     * 
     * @param path
     *            the patch of the configuration file
     * @throws ConfigException
     *             error processing configuration
     */
    public void parse(String path) throws ConfigException {
        File configFile = new File(path);

        LOG.info("Reading configuration from: " + configFile);

        try {
            if (!configFile.exists()) {
                throw new IllegalArgumentException(configFile.toString()
                        + " file is missing");
            }

            Properties cfg = new Properties();
            FileInputStream in = new FileInputStream(configFile);
            try {
                cfg.load(in);
            } finally {
                in.close();
            }

            parseProperties(cfg);
        } catch (IOException e) {
            throw new ConfigException("Error processing " + path, e);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Error processing " + path, e);
        }
    }

    /**
     * Parse config from a Properties.
     * 
     * @param zkProp
     *            Properties to parse from.
     * @throws IOException
     * @throws ConfigException
     */
    public void parseProperties(Properties zkProp) throws IOException,
            ConfigException {
        String clientPortAddress = null;
        for (Entry<Object, Object> entry : zkProp.entrySet()) {
            String key = entry.getKey().toString().trim();
            String value = entry.getValue().toString().trim();
            if (key.equals("dataDir")) {
                dataDir = value;
            } else if (key.equals("dataLogDir")) {
                dataLogDir = value;
            } else if (key.equals("clientPort")) {
                clientPort = Integer.parseInt(value);
            } else if (key.equals("clientPortAddress")) {
                clientPortAddress = value.trim();
            } else if (key.equals("tickTime")) {
                tickTime = Integer.parseInt(value);
            } else if (key.equals("initLimit")) {
                initLimit = Integer.parseInt(value);
            } else if (key.equals("syncLimit")) {
                syncLimit = Integer.parseInt(value);
            } else if (key.equals("electionAlg")) {
                electionAlg = Integer.parseInt(value);
            } else if (key.equals("quorumListenOnAllIPs")) {
                quorumListenOnAllIPs = Boolean.parseBoolean(value);
            } else if (key.equals("peerType")) {
                if (value.toLowerCase().equals("observer")) {
                    peerType = LearnerType.OBSERVER;
                } else if (value.toLowerCase().equals("participant")) {
                    peerType = LearnerType.PARTICIPANT;
                } else {
                    throw new ConfigException("Unrecognised peertype: " + value);
                }
            } else if (key.equals("syncEnabled")) {
                syncEnabled = Boolean.parseBoolean(value);
            } else if (key.equals("autopurge.snapRetainCount")) {
                snapRetainCount = Integer.parseInt(value);
            } else if (key.startsWith("server.")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                String parts[] = value.split(":");
                if ((parts.length != 2) && (parts.length != 3)
                        && (parts.length != 4)) {
                    LOG.error(value
                            + " does not have the form host:port or host:port:port "
                            + " or host:port:port:type");
                }
                InetSocketAddress addr = new InetSocketAddress(parts[0],
                        Integer.parseInt(parts[1]));
                addressToSid.add(new M2mAddressToId(sid, parts[0]));
                if (parts.length == 2) {
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, addr));
                } else if (parts.length == 3) {
                    InetSocketAddress electionAddr = new InetSocketAddress(
                            parts[0], Integer.parseInt(parts[2]));// 用来选举的信息
                    servers.put(Long.valueOf(sid), new QuorumServer(sid, addr,
                            electionAddr));
                } else if (parts.length == 4) {
                    InetSocketAddress electionAddr = new InetSocketAddress(
                            parts[0], Integer.parseInt(parts[2]));
                    LearnerType type = LearnerType.PARTICIPANT;
                    if (parts[3].toLowerCase().equals("participant")) {
                        type = LearnerType.PARTICIPANT;
                        servers.put(Long.valueOf(sid), new QuorumServer(sid,
                                addr, electionAddr, type));
                    } else {
                        throw new ConfigException("Unrecognised peertype: "
                                + value);
                    }
                }
            } else if (key.startsWith("group")) {
                int dot = key.indexOf('.');
                long gid = Long.parseLong(key.substring(dot + 1));

                numGroups++;

                String parts[] = value.split(":");
                for (String s : parts) {
                    long sid = Long.parseLong(s);
                    if (serverGroup.containsKey(sid))
                        throw new ConfigException("Server " + sid
                                + "is in multiple groups");
                    else
                        serverGroup.put(sid, gid);
                }

            } else if (key.startsWith("weight")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                serverWeight.put(sid, Long.parseLong(value));
            } else if (key.equals("cassandra.node")) {
                node = value;
            } else if (key.equals("cassandra.keyspace")) {
                keySpace = value;
            } else if (key.equals("cassandra.table")) {
                table = value;
            } else if (key.equals("cassandra.cleaned")) {
                cleaned = Boolean.valueOf(value);
            } else if (key.equals("zooKeeper.server")) {
                zooKeeperServer = value;
            } else if (key.equals("replication.factor")) {
                replication_factor = Integer.valueOf(value);
            } else if (key.startsWith("client.")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                sidToClientPort.put(sid, Integer.valueOf(value));
            } else if (key.equals("is.tempory.add")) {
                isTemporyAdd = Boolean.valueOf(value);
            } else if (key.startsWith("webPort")) {
                int dot = key.indexOf('.');
                long sid = Long.parseLong(key.substring(dot + 1));
                webPort = Integer.valueOf(value);
                sidAndWebPort.put(sid, webPort);
            } else {
                System.setProperty("zookeeper." + key, value);
            }
        }

        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir is not set");
        }
        if (dataLogDir == null) {
            dataLogDir = dataDir;
        } else {
            if (!new File(dataLogDir).isDirectory()) {
                throw new IllegalArgumentException("dataLogDir " + dataLogDir
                        + " is missing.");
            }
        }
        if (clientPort == 0) {
            throw new IllegalArgumentException("clientPort is not set");
        }
        if (clientPortAddress != null) {
            this.clientPortAddress = new InetSocketAddress(
                    InetAddress.getByName(clientPortAddress), clientPort);
        } else {
            this.clientPortAddress = new InetSocketAddress(clientPort);
        }

        if (tickTime == 0) {
            throw new IllegalArgumentException("tickTime is not set");
        }
        if (minSessionTimeout > maxSessionTimeout) {
            throw new IllegalArgumentException(
                    "minSessionTimeout must not be larger than maxSessionTimeout");
        }
        if (servers.size() == 0) {
            return;
        } else if (servers.size() == 1) {

            // HBase currently adds a single server line to the config, for
            // b/w compatibility reasons we need to keep this here.
            LOG.error("Invalid configuration, only one server specified (ignoring)");
            servers.clear();
        } else if (servers.size() > 1) {
            if (servers.size() == 2) {
                LOG.warn("No server failure will be tolerated. "
                        + "You need at least 3 servers.");
            } else if (servers.size() % 2 == 0) {
                LOG.warn("Non-optimial configuration, consider an odd number of servers.");
            }
            if (initLimit == 0) {
                throw new IllegalArgumentException("initLimit is not set");
            }
            if (syncLimit == 0) {
                throw new IllegalArgumentException("syncLimit is not set");
            }
            /*
             * If using FLE, then every server requires a separate election
             * port.
             */
            if (electionAlg != 0) {
                for (QuorumServer s : servers.values()) {
                    if (s.electionAddr == null)
                        throw new IllegalArgumentException(
                                "Missing election port for server: " + s.id);
                }
            }

            /*
             * Default of quorum config is majority
             */
            if (serverGroup.size() > 0) {
                if (servers.size() != serverGroup.size())
                    throw new ConfigException(
                            "Every server must be in exactly one group");
                /*
                 * The deafult weight of a server is 1
                 */
                for (QuorumServer s : servers.values()) {
                    if (!serverWeight.containsKey(s.id))
                        serverWeight.put(s.id, (long) 1);
                }

                /*
                 * Set the quorumVerifier to be QuorumHierarchical
                 */
                quorumVerifier = new QuorumHierarchical(numGroups,
                        serverWeight, serverGroup);
            } else {
                /*
                 * The default QuorumVerifier is QuorumMaj
                 */

                LOG.info("Defaulting to majority quorums");
                quorumVerifier = new QuorumMaj(servers.size());
            }

            m2mDataBase = new M2MDataBaseImpl(cleaned, keySpace, table, node);

            File myIdFile = new File(dataDir, "myid");
            if (!myIdFile.exists()) {
                throw new IllegalArgumentException(myIdFile.toString()
                        + " file is missing");
            }
            BufferedReader br = new BufferedReader(new FileReader(myIdFile));
            String myIdString;
            try {
                myIdString = br.readLine();
            } finally {
                br.close();
            }
            try {
                serverId = Long.parseLong(myIdString);
                MDC.put("myid", myIdString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("serverid " + myIdString
                        + " is not a number");
            }

            myIp = servers.get(serverId).addr.getAddress().getHostAddress();
            LearnerType roleByServersList = LearnerType.PARTICIPANT;
            if (roleByServersList != peerType) {
                LOG.warn("Peer type from servers list (" + roleByServersList
                        + ") doesn't match peerType (" + peerType
                        + "). Defaulting to servers list.");

                peerType = roleByServersList;
            }
        }
        setAllReplicationServers();
    }

    /**
     * 设置不同zab集群的server
     *
     */
    public void setAllReplicationServers() {
        networkPool = new NetworkPool();

        List<String> serversStrings = new ArrayList<String>();
        for (M2mAddressToId m2mAddressToId : addressToSid) {
            Long sid = m2mAddressToId.getSid();
            String address = m2mAddressToId.getAddress();
            serversStrings.add(address + ":" + sidToClientPort.get(sid));
            NetworkPool.webPort.put(address + ":" + sidToClientPort.get(sid),
                    sidAndWebPort.get(sid));
            allServers.put(address + ":" + sidToClientPort.get(sid), sid);
        }
        networkPool.setServers(serversStrings.toArray(new String[serversStrings
                .size()]));
        networkPool.initialize();
        Long myIdInRing = networkPool.getServerPosition().get(
                myIp + ":" + clientPort);
        List<String> list = new ArrayList<>();
        if (isTemporyAdd) {

            HashMap<Long, QuorumServer> map = new HashMap<Long, QuorumServer>();
            for (int j = 0; j < replication_factor + 1; j++) {
                String leftServer = networkPool.getPositionToServer().get(
                        ((myIdInRing + j) + serversStrings.size())
                                % serversStrings.size());// 最左边
                Long sid = allServers.get(leftServer);// 找出对应的sid;

                QuorumServer quorumServer = servers.get(sid);
                String address = quorumServer.addr.getAddress()
                        .getHostAddress();

                Integer firstPort = quorumServer.addr.getPort();
                Integer secondPort = quorumServer.electionAddr.getPort();
                InetSocketAddress firstInetSocketAddress = new InetSocketAddress(
                        address, j == 0 ? firstPort : firstPort - j + 1);
                InetSocketAddress secondInetSocketAddress = new InetSocketAddress(
                        address, j == 0 ? secondPort : secondPort - j + 1);
                QuorumServer myQuorumServer = new QuorumServer(sid,
                        firstInetSocketAddress, secondInetSocketAddress,
                        LearnerType.PARTICIPANT);
                map.put(sid, myQuorumServer);
                if (j == 0) {
                    list.add(leftServer);
                }

            }

            positionToServers.put(0L, map);
        } else {
            for (long i = 0; i < replication_factor; i++) {

                HashMap<Long, QuorumServer> map = new HashMap<Long, QuorumServer>();
                for (int j = 0; j < replication_factor; j++) {
                    String leftServer = networkPool
                            .getPositionToServer()
                            .get(((myIdInRing - (replication_factor - 1 - j - i)) + serversStrings
                                    .size()) % serversStrings.size());// 最左边
                    Long sid = allServers.get(leftServer);// 找出对应的sid;
                    QuorumServer quorumServer = servers.get(sid);
                    String address = quorumServer.addr.getAddress()
                            .getHostAddress();

                    Integer firstPort = quorumServer.addr.getPort();
                    Integer secondPort = quorumServer.electionAddr.getPort();
                    InetSocketAddress firstInetSocketAddress = new InetSocketAddress(
                            address, firstPort - j);
                    InetSocketAddress secondInetSocketAddress = new InetSocketAddress(
                            address, secondPort - j);
                    QuorumServer myQuorumServer = new QuorumServer(sid,
                            firstInetSocketAddress, secondInetSocketAddress,
                            LearnerType.PARTICIPANT);
                    map.put(sid, myQuorumServer);
                    if (j == 0) {
                        list.add(leftServer);
                    }

                }

                positionToServers.put(i, map);

            }
        }

        m2mQuorumServers.setPositionToServers(positionToServers);
        m2mQuorumServers.setServers(list);

    }

    public InetSocketAddress getClientPortAddress() {
        return clientPortAddress;
    }

    public String getDataDir() {
        return dataDir;
    }

    public String getDataLogDir() {
        return dataLogDir;
    }

    public int getTickTime() {
        return tickTime;
    }

    public int getMaxClientCnxns() {
        return maxClientCnxns;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }

    public int getInitLimit() {
        return initLimit;
    }

    public int getSyncLimit() {
        return syncLimit;
    }

    public int getElectionAlg() {
        return electionAlg;
    }

    public int getElectionPort() {
        return electionPort;
    }

    public int getSnapRetainCount() {
        return snapRetainCount;
    }

    public boolean getSyncEnabled() {
        return syncEnabled;
    }

    public QuorumVerifier getQuorumVerifier() {
        return quorumVerifier;
    }

    public Map<Long, QuorumServer> getServers() {
        return Collections.unmodifiableMap(servers);
    }

    public long getServerId() {
        return serverId;
    }

    public boolean isDistributed() {
        return servers.size() > 1;
    }

    public LearnerType getPeerType() {
        return peerType;
    }

    public Boolean getQuorumListenOnAllIPs() {
        return quorumListenOnAllIPs;
    }

    public String getMyIp() {
        return myIp;
    }

    public void setMyIp(String myIp) {
        this.myIp = myIp;
    }

    public String getZooKeeperServer() {
        return zooKeeperServer;
    }

    public void setZooKeeperServer(String zooKeeperServer) {
        this.zooKeeperServer = zooKeeperServer;
    }

    public Integer getReplication_factor() {
        return replication_factor;
    }

    public void setReplication_factor(Integer replication_factor) {
        this.replication_factor = replication_factor;
    }

    public M2mQuorumServer getM2mQuorumServers() {
        return m2mQuorumServers;
    }

    public void setM2mQuorumServers(M2mQuorumServer m2mQuorumServers) {
        this.m2mQuorumServers = m2mQuorumServers;
    }

    public NetworkPool getNetworkPool() {
        return networkPool;
    }

    public boolean isTemporyAdd() {
        return isTemporyAdd;
    }

    public void setTemporyAdd(boolean isTemporyAdd) {
        this.isTemporyAdd = isTemporyAdd;
    }

    public Integer getWebPort() {
        return webPort;
    }

    public void setWebPort(Integer webPort) {
        this.webPort = webPort;
    }

}
