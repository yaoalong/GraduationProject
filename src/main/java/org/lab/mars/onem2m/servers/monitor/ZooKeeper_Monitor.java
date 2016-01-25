package org.lab.mars.onem2m.servers.monitor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.lab.mars.onem2m.consistent.hash.NetworkPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 监控zookeeper,从而可以获取在线机器列表
 */
public class ZooKeeper_Monitor extends Thread implements Watcher {

    private static final Logger LOG = LoggerFactory
            .getLogger(ZooKeeper_Monitor.class);
    private static final String ROOT_NODE = "/server";
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private ZooKeeper zooKeeper;
    /*
     * zooKeeper服务器的地址
     */
    private String server;
    private NetworkPool networkPool;

    public void run() {
        try {
            zooKeeper = new ZooKeeper(server, 5000, this);
            countDownLatch.await();
            getChildrens();
            while (true) {
                zooKeeper.getChildren("/server", this);
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            LOG.error("zookeepeer_monitor is error because of:{}",
                    e.getMessage());
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (KeeperState.SyncConnected == event.getState()) {
            countDownLatch.countDown();
        } else if (EventType.NodeChildrenChanged == event.getType()
                && event.getPath().startsWith("/server")) {
            try {
                if (zooKeeper == null) {
                    return;
                }
                getChildrens();
            } catch (KeeperException | InterruptedException e) {
                LOG.error("error:{}", e.getMessage());
            }
        }
    }

    /*
     * 去修改networkPool的服务器列表
     */
    private void getChildrens() throws KeeperException, InterruptedException {
        if (zooKeeper == null) {
            LOG.error("zookeeper is empty");
            return;
        }
        List<String> serverStrings = zooKeeper.getChildren(ROOT_NODE, null);
        networkPool.setServers(serverStrings.toArray(new String[serverStrings
                .size()]));
        networkPool.initialize();

    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public NetworkPool getNetworkPool() {
        return networkPool;
    }

    public void setNetworkPool(NetworkPool networkPool) {
        this.networkPool = networkPool;
    }

    public static void main(String args[]) {
        ZooKeeper_Monitor zooKeeper_Monitor = new ZooKeeper_Monitor();
        zooKeeper_Monitor.setServer("192.168.10.139:2181");
        NetworkPool networkPool = new NetworkPool();
        zooKeeper_Monitor.setNetworkPool(networkPool);
        zooKeeper_Monitor.start();
    }
}
