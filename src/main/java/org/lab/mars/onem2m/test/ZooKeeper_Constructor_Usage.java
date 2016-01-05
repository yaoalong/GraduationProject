package org.lab.mars.onem2m.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;



public class ZooKeeper_Constructor_Usage implements Watcher{
	private static CountDownLatch countDownLatch=new CountDownLatch(1);
	
	public static void main(String args[]) throws IOException{
		ZooKeeper zooKeeper=new ZooKeeper("192.168.10.139:2182", 5000, new ZooKeeper_Constructor_Usage());
		System.out.println(zooKeeper.getState());
		try {
			countDownLatch.await();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	@Override
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		System.out.println("Receive watched event:"+event);
		if(KeeperState.SyncConnected==event.getState()){
			countDownLatch.countDown();
		}
		
	}

}
