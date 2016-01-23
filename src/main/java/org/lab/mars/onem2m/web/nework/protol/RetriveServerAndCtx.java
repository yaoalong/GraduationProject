package org.lab.mars.onem2m.web.nework.protol;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class RetriveServerAndCtx {

    private ChannelHandlerContext ctx;

    private List<String> servers;

    public RetriveServerAndCtx(ChannelHandlerContext ctx, List<String> servers) {
        this.ctx = ctx;
        this.servers = servers;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

}
