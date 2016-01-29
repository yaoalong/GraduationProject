package org.lab.mars.onem2m.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.lab.mars.onem2m.network.intialize.PacketClientChannelInitializer;
import org.lab.mars.onem2m.test.Test;

/**
 * Created by Administrator on 2015/12/21.
 */

/**
 * TCP客户端
 */
public class TcpClient {
    private Channel channel;
    private ReentrantLock reentrantLock = new ReentrantLock();
    private Condition condition = reentrantLock.newCondition();

    public void connectionOne(String host, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(NetworkEventLoopGroup.workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new PacketClientChannelInitializer());
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            reentrantLock.lock();
            channel = future.channel();
            condition.notifyAll();
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

    public static void main(String args[]) {
        TcpClient tcpClient = new TcpClient();
        try {
            tcpClient.connectionOne("192.168.10.131", 2182);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        try {
            tcpClient.write(Test.createM2mGetDataPacket());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

}
