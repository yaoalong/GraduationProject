package org.lab.mars.onem2m.web.network.initialize;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import org.lab.mars.onem2m.server.ServerCnxnFactory;
import org.lab.mars.onem2m.web.network.handler.WebServerChannelHandler;

public class WebServerChannelInitializer extends
        ChannelInitializer<SocketChannel> {
    private ServerCnxnFactory serverCnxnFactory;

    public WebServerChannelInitializer(ServerCnxnFactory serverCnxnFactory) {
        this.serverCnxnFactory = serverCnxnFactory;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers
                .cacheDisabled(null)));
        channelPipeline.addLast(new WebServerChannelHandler(serverCnxnFactory));
    }
}