package org.xhome.mars.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.xhome.mars.network.handler.AddChannelHandler;
import org.xhome.mars.network.initialize.PacketServerChannelInitializer;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Administrator on 2015/12/21.
 */
/*
TCP服务器
 */
public class TcpServer {
    private Set<Channel> channels;
    public TcpServer(){
        this.channels=new HashSet<>();
    }
    public void bind(String host,int port) throws InterruptedException {
        ServerBootstrap b=new ServerBootstrap();
        b.group(NetworkEventLoopGroup.bossGroup,NetworkEventLoopGroup.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)
                .option(ChannelOption.SO_BACKLOG,1000)
        .handler(new AddChannelHandler(channels))
        .childHandler(new PacketServerChannelInitializer());
        b.bind(host,port).sync();
    }
    public void close(){
        for(Channel channel:channels){
            channel.close();
        }
    }
    public static void main(String args[]) throws InterruptedException {
        TcpServer tcpServer=new TcpServer();
        tcpServer.bind("localhost",11111);
    }
}
