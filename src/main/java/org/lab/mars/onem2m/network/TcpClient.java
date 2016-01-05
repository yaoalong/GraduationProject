package org.lab.mars.onem2m.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.lab.mars.onem2m.network.intialize.PacketClientChannelInitializer;

/**
 * Created by Administrator on 2015/12/21.
 */

/**
 * TCP客户端
 */
public class TcpClient {
    private Channel channel;
    public void connectionOne(String host,int port){
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(NetworkEventLoopGroup.workerGroup)
                .channel(NioSocketChannel.class)  
                .option(ChannelOption.TCP_NODELAY,true)
                .handler(new PacketClientChannelInitializer());
        bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
            channel = future.channel();
        });

    }
    public void write(Object msg){
        channel.writeAndFlush(msg);
    }
    public void  close(){
        if(channel!=null){
            channel.close();
        }
    }
    public static void main(String args[]){
    TcpClient tcpClient=new TcpClient();
        tcpClient.connectionOne("localhost",2182);
    }
}
