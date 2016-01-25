package org.lab.mars.onem2m.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.HashSet;
import java.util.Set;

import org.lab.mars.onem2m.network.intialize.PacketServerChannelInitializer;
import org.lab.mars.onem2m.server.ServerCnxnFactory;

/**
 * Created by Administrator on 2015/12/21.
 */
/*
 * TCP服务器
 */
public class TcpServer {
    private Set<Channel> channels;
    private ServerCnxnFactory serverCnxnFactory;

    public TcpServer(ServerCnxnFactory serverCnxnFactory) {
        this.serverCnxnFactory = serverCnxnFactory;
        this.channels = new HashSet<Channel>();
    }

    public void bind(String host, int port) throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(NetworkEventLoopGroup.bossGroup,
                NetworkEventLoopGroup.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childHandler(
                        new PacketServerChannelInitializer(serverCnxnFactory));
        b.bind(host, port).addListener((ChannelFuture channelFuture) -> {
            channels.add(channelFuture.channel());
        });
    }

    public void close() {
        for (Channel channel : channels) {
            channel.close();
        }
    }

    public static void main(String args[]) throws InterruptedException {
        TcpServer tcpServer = new TcpServer(null);
        tcpServer.bind("localhost", 2182);
    }
}
