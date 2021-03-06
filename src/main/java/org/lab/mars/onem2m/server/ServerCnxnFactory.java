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

package org.lab.mars.onem2m.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.JMException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;

import org.lab.mars.onem2m.Environment;
import org.lab.mars.onem2m.Login;
import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.lab.mars.onem2m.jmx.MBeanRegistry;
import org.lab.mars.onem2m.server.auth.SaslServerCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServerCnxnFactory {

    public static final String ZOOKEEPER_SERVER_CNXN_FACTORY = "zookeeper.serverCnxnFactory";

    public interface PacketProcessor {
        public void processPacket(ByteBuffer packet, ServerCnxn src);
    }

    Logger LOG = LoggerFactory.getLogger(ServerCnxnFactory.class);

    /**
     * The buffer will cause the connection to be close when we do a send.
     */
    static final ByteBuffer closeConn = ByteBuffer.allocate(0);

    public abstract int getLocalPort();

    public abstract Iterable<ServerCnxn> getConnections();

    public int getNumAliveConnections() {
        synchronized (cnxns) {
            return cnxns.size();
        }
    }

    public abstract void closeSession(long sessionId);

    public abstract void configure(InetSocketAddress addr, int maxClientCnxns)
            throws IOException;

    protected SaslServerCallbackHandler saslServerCallbackHandler;
    public Login login;

    /** Maximum number of connections allowed from particular host (ip) */
    public abstract int getMaxClientCnxnsPerHost();

    /** Maximum number of connections allowed from particular host (ip) */
    public abstract void setMaxClientCnxnsPerHost(int max);

    public abstract void startup(ZooKeeperServer zkServer) throws IOException,
            InterruptedException;

    public abstract void join() throws InterruptedException;

    public abstract void shutdown();

    public abstract void start();

    public abstract String getMyIp();

    protected ZooKeeperServer zkServer;

    /**
     * 多个ZooKeeper 不同的ip对应的是不同的ZooKeeperServer
     */
    protected ConcurrentHashMap<String, ZooKeeperServer> zkServers = new ConcurrentHashMap<String, ZooKeeperServer>();

    /**
     * 设置zkServer
     * 
     * @param zk
     */
    /**
     * 每个特定的QuorumPeer都会添加自己的ZooKeeperServer
     * 
     * @param ip
     * @param zooKeeperServer
     */
    final public void addZooKeeperServer(String ip,
            ZooKeeperServer zooKeeperServer) {
        this.zkServers.put(ip, zooKeeperServer);
        zooKeeperServer.setServerCnxnFactory(this);
    }

    /**
     * 删除特定Ip的ZooKeeper
     * 
     * @param ip
     */
    final public void removeZookeeper(String ip) {
        this.zkServers.remove(ip);
    }

    public abstract void closeAll();

    static public ServerCnxnFactory createFactory() throws IOException {
        String serverCnxnFactoryName = System
                .getProperty(ZOOKEEPER_SERVER_CNXN_FACTORY);
        if (serverCnxnFactoryName == null) {
            serverCnxnFactoryName = NIOServerCnxnFactory.class.getName();
        }
        try {
            return (ServerCnxnFactory) Class.forName(serverCnxnFactoryName)
                    .newInstance();
        } catch (Exception e) {
            IOException ioe = new IOException("Couldn't instantiate "
                    + serverCnxnFactoryName);
            ioe.initCause(e);
            throw ioe;
        }
    }

    static public ServerCnxnFactory createFactory(int clientPort,
            int maxClientCnxns) throws IOException {
        return createFactory(new InetSocketAddress(clientPort), maxClientCnxns);
    }

    static public ServerCnxnFactory createFactory(InetSocketAddress addr,
            int maxClientCnxns) throws IOException {
        ServerCnxnFactory factory = createFactory();
        factory.configure(addr, maxClientCnxns);
        return factory;
    }

    public abstract InetSocketAddress getLocalAddress();

    private final Map<ServerCnxn, ConnectionBean> connectionBeans = new ConcurrentHashMap<ServerCnxn, ConnectionBean>();

    protected final HashSet<ServerCnxn> cnxns = new HashSet<ServerCnxn>();

    public void unregisterConnection(ServerCnxn serverCnxn) {
        ConnectionBean jmxConnectionBean = connectionBeans.remove(serverCnxn);
        if (jmxConnectionBean != null) {
            MBeanRegistry.getInstance().unregister(jmxConnectionBean);
        }
    }

    public void registerConnection(ServerCnxn serverCnxn) {
        if (zkServer != null) {
            ConnectionBean jmxConnectionBean = new ConnectionBean(serverCnxn,
                    zkServer);
            try {
                MBeanRegistry.getInstance().register(jmxConnectionBean,
                        zkServer.jmxServerBean);
                connectionBeans.put(serverCnxn, jmxConnectionBean);
            } catch (JMException e) {
                LOG.warn("Could not register connection", e);
            }
        }

    }

    /**
     * Initialize the server SASL if specified.
     *
     * If the user has specified a "ZooKeeperServer.LOGIN_CONTEXT_NAME_KEY" or a
     * jaas.conf using "java.security.auth.login.config" the authentication is
     * required and an exception is raised. Otherwise no authentication is
     * configured and no exception is raised.
     *
     * @throws IOException
     *             if jaas.conf is missing or there's an error in it.
     */
    protected void configureSaslLogin() throws IOException {
        String serverSection = System.getProperty(
                ZooKeeperSaslServer.LOGIN_CONTEXT_NAME_KEY,
                ZooKeeperSaslServer.DEFAULT_LOGIN_CONTEXT_NAME);

        // Note that 'Configuration' here refers to
        // javax.security.auth.login.Configuration.
        AppConfigurationEntry entries[] = null;
        SecurityException securityException = null;
        try {
            entries = Configuration.getConfiguration()
                    .getAppConfigurationEntry(serverSection);
        } catch (SecurityException e) {
            // handle below: might be harmless if the user doesn't intend to use
            // JAAS authentication.
            securityException = e;
        }

        // No entries in jaas.conf
        // If there's a configuration exception fetching the jaas section and
        // the user has required sasl by specifying a LOGIN_CONTEXT_NAME_KEY or
        // a jaas file
        // we throw an exception otherwise we continue without authentication.
        if (entries == null) {
            String jaasFile = System.getProperty(Environment.JAAS_CONF_KEY);
            String loginContextName = System
                    .getProperty(ZooKeeperSaslServer.LOGIN_CONTEXT_NAME_KEY);
            if (securityException != null
                    && (loginContextName != null || jaasFile != null)) {
                String errorMessage = "No JAAS configuration section named '"
                        + serverSection + "' was found";
                if (jaasFile != null) {
                    errorMessage += "in '" + jaasFile + "'.";
                }
                if (loginContextName != null) {
                    errorMessage += " But "
                            + ZooKeeperSaslServer.LOGIN_CONTEXT_NAME_KEY
                            + " was set.";
                }
                LOG.error(errorMessage);
                throw new IOException(errorMessage);
            }
            return;
        }

        // jaas.conf entry available
        try {
            saslServerCallbackHandler = new SaslServerCallbackHandler(
                    Configuration.getConfiguration());
            login = new Login(serverSection, saslServerCallbackHandler);
            login.startThreadIfNeeded();
        } catch (LoginException e) {
            throw new IOException(
                    "Could not configure server because SASL configuration did not allow the "
                            + " ZooKeeper server to authenticate itself properly: "
                            + e);
        }
    }

    public ZooKeeperServer getZkServer() {
        return zkServer;
    }

    public void setZkServer(ZooKeeperServer zkServer) {
        this.zkServer = zkServer;
    }

    public abstract NetworkPool getNetworkPool();

    public abstract Integer getReplicationFactor();

    public ConcurrentHashMap<String, ZooKeeperServer> getZkServers() {
        return zkServers;
    }

    public abstract boolean isTemporyAdd();

    public abstract Map<String, Long> getAllServer();

}
