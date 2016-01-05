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

import org.lab.mars.onem2m.jmx.MBeanRegistry;
import org.lab.mars.onem2m.persistence.FileTxnSnapLog;
import org.lab.mars.onem2m.server.DataTreeBean;
import org.lab.mars.onem2m.server.FinalRequestProcessor;
import org.lab.mars.onem2m.server.PrepRequestProcessor;
import org.lab.mars.onem2m.server.RequestProcessor;
import org.lab.mars.onem2m.server.ZKDatabase;
import org.lab.mars.onem2m.server.ZooKeeperServer;
import org.lab.mars.onem2m.server.ZooKeeperServer.DataTreeBuilder;
import org.lab.mars.onem2m.server.ZooKeeperServerBean;

/**
 * A ZooKeeperServer which comes into play when peer is partitioned from the
 * majority. Handles read-only clients, but drops connections from not-read-only
 * ones.
 * <p>
 * The very first processor in the chain of request processors is a
 * ReadOnlyRequestProcessor which drops state-changing requests.
 */
public class ReadOnlyZooKeeperServer extends QuorumZooKeeperServer {

    private volatile boolean shutdown = false;
    ReadOnlyZooKeeperServer(FileTxnSnapLog logFactory, QuorumPeer self,
            DataTreeBuilder treeBuilder, ZKDatabase zkDb) {
        super(logFactory, self.tickTime, self.minSessionTimeout, self.maxSessionTimeout,
                treeBuilder, zkDb, self);
    }

    @Override
    protected void setupRequestProcessors() {
        RequestProcessor finalProcessor = new FinalRequestProcessor(this);
        RequestProcessor prepProcessor = new PrepRequestProcessor(this, finalProcessor);
        ((PrepRequestProcessor) prepProcessor).start();
   
    }

    @Override
    public synchronized void startup() {
        // check to avoid startup follows shutdown
        if (shutdown) {
            LOG.warn("Not starting Read-only server as startup follows shutdown!");
            return;
        }
        registerJMX(new ReadOnlyBean(this), self.jmxLocalPeerBean);
        super.startup();
        self.cnxnFactory.setZooKeeperServer(this);
        LOG.info("Read-only server started");
    }

    @Override
    protected void registerJMX() {
        // register with JMX
        try {
            jmxDataTreeBean = new DataTreeBean(getZKDatabase().getDataTree());
            MBeanRegistry.getInstance().register(jmxDataTreeBean, jmxServerBean);
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
            jmxDataTreeBean = null;
        }
    }

    public void registerJMX(ZooKeeperServerBean serverBean, LocalPeerBean localPeerBean) {
        // register with JMX
        try {
            jmxServerBean = serverBean;
            MBeanRegistry.getInstance().register(serverBean, localPeerBean);
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
            jmxServerBean = null;
        }
    }

    @Override
    protected void unregisterJMX() {
        // unregister from JMX
        try {
            if (jmxDataTreeBean != null) {
                MBeanRegistry.getInstance().unregister(jmxDataTreeBean);
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister with JMX", e);
        }
        jmxDataTreeBean = null;
    }

    protected void unregisterJMX(ZooKeeperServer zks) {
        // unregister from JMX
        try {
            if (jmxServerBean != null) {
                MBeanRegistry.getInstance().unregister(jmxServerBean);
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister with JMX", e);
        }
        jmxServerBean = null;
    }

    @Override
    public String getState() {
        return "read-only";
    }

    /**
     * Returns the id of the associated QuorumPeer, which will do for a unique
     * id of this server.
     */
    @Override
    public long getServerId() {
        return self.getId();
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        unregisterJMX(this);

        // set peer's server to null
        self.cnxnFactory.setZooKeeperServer(null);
        // clear all the connections
        self.cnxnFactory.closeAll();

        // shutdown the server itself
        super.shutdown();
    }

}
