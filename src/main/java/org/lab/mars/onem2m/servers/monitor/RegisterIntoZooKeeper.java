package org.lab.mars.onem2m.servers.monitor;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterIntoZooKeeper extends Thread implements Watcher {

    private static final Logger LOG = LoggerFactory
            .getLogger(RegisterIntoZooKeeper.class);
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private String server;
    private ZooKeeper zooKeeper;
    private String ip;

    public void register(String ip) throws IOException, KeeperException,
            InterruptedException {
        zooKeeper = new ZooKeeper(server, 5000, new RegisterIntoZooKeeper());
        this.ip = ip;

    }

    @Override
    public void run() {
        try {
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            zooKeeper.create("/server/" + ip, "1".getBytes(),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (KeeperException | InterruptedException e) {
            LOG.trace("error because of:{}", e);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("Receive watched event:" + event);
        if (KeeperState.SyncConnected == event.getState()) {
            countDownLatch.countDown();
        }

    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

}
