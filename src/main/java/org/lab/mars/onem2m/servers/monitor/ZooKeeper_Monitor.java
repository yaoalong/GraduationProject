package org.lab.mars.onem2m.servers.monitor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.lab.mars.onem2m.consistent.hash.NetworkPool;

/*
 * 监控zookeeper,从而可以获取在线机器列表
 */
public class ZooKeeper_Monitor implements Watcher {
	private static final String ROOT_NODE = "/server";
	private static CountDownLatch countDownLatch = new CountDownLatch(1);
	private ZooKeeper zooKeeper;
	/*
	 * zooKeeper服务器的地址
	 */
	private String server;
	private NetworkPool networkPool;

	public void start() throws IOException {
		ZooKeeper_Monitor zooKeeper_Constructor_Usage = new ZooKeeper_Monitor();
		ZooKeeper zooKeeper = new ZooKeeper(server, 5000,
				zooKeeper_Constructor_Usage);
		System.out.println(zooKeeper.getState());
		try {
			countDownLatch.await();
			while (true) {
				zooKeeper.getChildren("/server", zooKeeper_Constructor_Usage);
				Thread.sleep(1000);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if (KeeperState.SyncConnected == event.getState()) {
			countDownLatch.countDown();
		} else if (EventType.NodeChildrenChanged == event.getType()
				&& event.getPath().startsWith("/server")) {
			try {
				getChildrens(zooKeeper);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	/*
	 * 去修改networkPool的服务器列表
	 */
	private void getChildrens(ZooKeeper zooKeeper) throws KeeperException,
			InterruptedException {
		List<String> serverStrings = zooKeeper.getChildren(ROOT_NODE, null);
		networkPool.setServers(serverStrings.toArray(new String[serverStrings
				.size()]));
		networkPool.populateConsistentBuckets();
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

}
