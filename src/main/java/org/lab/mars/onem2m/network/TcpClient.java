package org.lab.mars.onem2m.network;

import java.io.IOException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

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

	public void connectionOne(String host, int port) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(NetworkEventLoopGroup.workerGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new PacketClientChannelInitializer());
		bootstrap.connect(host, port).addListener((ChannelFuture future) -> {
			channel = future.channel();
		});

	}

	public void write(Object msg) {
		while(channel==null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			tcpClient.connectionOne("localhost", 2182);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		try {
			tcpClient.write(Test.createM2mCreatePacket());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
