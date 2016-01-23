package org.lab.mars.onem2m.web.nework.protol;

import io.netty.channel.ChannelHandlerContext;

import java.util.Set;

public class RetriveServerAndCtx {

    private ChannelHandlerContext ctx;

    private Set<String> servers;

    public RetriveServerAndCtx(ChannelHandlerContext ctx, Set<String> servers) {
        this.ctx = ctx;
        this.servers = servers;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public Set<String> getServers() {
        return servers;
    }

    public void setServers(Set<String> servers) {
        this.servers = servers;
    }

}
