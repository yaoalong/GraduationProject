package org.lab.mars.onem2m.network;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Created by Administrator on 2015/12/21.
 */
public class NetworkEventLoopGroup {

    public static final int NCPU=Runtime.getRuntime().availableProcessors();

    public static final EventLoopGroup bossGroup;

    public static final EventLoopGroup workerGroup;

    static{
        bossGroup=new NioEventLoopGroup();
        workerGroup=new NioEventLoopGroup();
    }
    public static void shutdown(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
