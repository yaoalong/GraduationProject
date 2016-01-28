package org.lab.mars.onem2m.data.synchronization.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.lab.mars.onem2m.web.nework.protol.RetriveServerAndCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M2mDataServerChannelHandler extends
        SimpleChannelInboundHandler<Object> {
    private static Logger LOG = LoggerFactory
            .getLogger(M2mDataServerChannelHandler.class);

    private AtomicInteger zxid = new AtomicInteger(0);
    static ConcurrentHashMap<Integer, RetriveServerAndCtx> result = new ConcurrentHashMap<Integer, RetriveServerAndCtx>();
    static ConcurrentHashMap<Integer, Integer> serverResult = new ConcurrentHashMap<Integer, Integer>();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        ctx.fireChannelRegistered();
    };

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Channel disconnect caused close:{}", cause);
        }
        ctx.close();
    }

    public int getNextZxid() {
        return zxid.getAndIncrement();
    }

    /*
     * 将server拆分为ip以及port
     */
    private String[] spilitString(String ip) {
        String[] splitMessage = ip.split(":");
        return splitMessage;
    }

}
