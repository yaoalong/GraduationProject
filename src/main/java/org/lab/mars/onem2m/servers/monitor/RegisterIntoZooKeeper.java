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

public class RegisterIntoZooKeeper implements Watcher {

	private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private String server;
	public  void register(String ip) throws IOException, KeeperException,
			InterruptedException {
		ZooKeeper zooKeeper = new ZooKeeper(server, 5000,
				new RegisterIntoZooKeeper());
		System.out.println(zooKeeper.getState());
		try {
			countDownLatch.await();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		zooKeeper.create("/server/"+ip, ip.getBytes(),
				Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		Thread.sleep(10000);

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
