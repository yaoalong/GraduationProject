package org.lab.mars.onem2m.data.synchronization.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.lab.mars.onem2m.data.synchronization.network.initialize.DataClientChannelInitializer;
import org.lab.mars.onem2m.network.NetworkEventLoopGroup;

public class DataTcpClient {
    private Channel channel;
    private ReentrantLock reentrantLock = new ReentrantLock();
    private Condition condition = reentrantLock.newCondition();

    public void connectionOne(String host, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NetworkEventLoopGroup.workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new DataClientChannelInitializer());
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            reentrantLock.lock();
            channel = future.channel();
            condition.signalAll();
            reentrantLock.unlock();
        });

    }

    public void write(Object msg) {
        if (channel == null) {
            try {
                reentrantLock.lock();
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                reentrantLock.unlock();
            }
        }
        channel.writeAndFlush(msg);
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
    }
}
