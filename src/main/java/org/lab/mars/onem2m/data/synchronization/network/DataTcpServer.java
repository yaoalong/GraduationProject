package org.lab.mars.onem2m.data.synchronization.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Set;

import org.lab.mars.onem2m.data.synchronization.network.initialize.DataServerChannelInitializer;
import org.lab.mars.onem2m.network.NetworkEventLoopGroup;
import org.lab.mars.onem2m.web.network.WebTcpServer;

public class DataTcpServer {
    private Set<Channel> channels;

    public void bind(String host, int port) throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(NetworkEventLoopGroup.bossGroup,
                NetworkEventLoopGroup.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childHandler(new DataServerChannelInitializer());
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
        WebTcpServer tcpServer = new WebTcpServer(null);
        tcpServer.bind("localhost", 2182);
    }
}
