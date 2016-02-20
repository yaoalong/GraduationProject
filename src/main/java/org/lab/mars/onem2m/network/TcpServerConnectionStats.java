package org.lab.mars.onem2m.network;

import io.netty.channel.ChannelHandlerContext;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * 
 * @author yaoalong
 * @Date 2016年2月17日
 * @Email yaoalong@foxmail.com
 */

public class TcpServerConnectionStats {
    /**
     * 利用了LinkedHashMap实现了LRU 连接池
     */
    public static final LinkedHashMap<ChannelHandlerContext, Long> connectionStats = new LinkedHashMap<ChannelHandlerContext, Long>(
            16, 0.5f, true) {
        /**
                 * 
                 */
        private static final long serialVersionUID = 3033453005289310613L;

        @Override
        protected boolean removeEldestEntry(
                Entry<ChannelHandlerContext, Long> eldest) {
            if (size() > 50) {
                return true;
            }
            return false;
        }
    };
}
