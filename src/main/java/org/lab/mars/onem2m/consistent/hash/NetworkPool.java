package org.lab.mars.onem2m.consistent.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * 一致性哈希的实现
 */
public class NetworkPool {
    public static final int CONSISTENT_HASH = 3;
    private static Logger log = LoggerFactory.getLogger(NetworkPool.class);
    private static ThreadLocal<MessageDigest> MD5 = new ThreadLocal<MessageDigest>() {
        @Override
        protected final MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                if (log.isErrorEnabled())
                    log.error("++++ no md5 algorithm found");
                throw new IllegalStateException("++++ no md5 algorythm found");
            }
        }
    };
    private volatile String[] servers;
    private Integer[] weights;
    private Integer totalWeight = 0;
    private volatile TreeMap<Long, String> consistentBuckets;
    private volatile boolean initialized = false;
    private int hashingAlg = CONSISTENT_HASH;
    /**
     * server对应的位置
     */
    private ConcurrentHashMap<String, Long> serverToPosition = new ConcurrentHashMap<String, Long>();

    /**
     * 位置对应的server
     */
    private ConcurrentHashMap<Long, String> positionToServer = new ConcurrentHashMap<Long, String>();

    /**
     * Internal private hashing method.
     * <p>
     * MD5 based hash algorithm for use in the consistent hashing approach.
     *
     * @param key
     * @return
     */
    public static long md5HashingAlg(String key) {
        MessageDigest md5 = MD5.get();
        md5.reset();
        md5.update(key.getBytes());
        byte[] bKey = md5.digest();
        long res = ((long) (bKey[3] & 0xFF) << 24)
                | ((long) (bKey[2] & 0xFF) << 16)
                | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
        return res;
    }

    /*
     * 初始化
     */
    public void initialize() {
        try {

            // if servers is not set, or it empty, then
            // throw a runtime exception
            if (servers == null || servers.length <= 0) {
                if (log.isErrorEnabled())
                    log.error("++++ trying to initialize with no servers");
                throw new IllegalStateException(
                        "++++ trying to initialize with no servers");
            }

            // only create up to maxCreate connections at once

            // initalize our internal hashing structures
            if (this.hashingAlg == CONSISTENT_HASH)
                populateConsistentBuckets();

            this.initialized = true;

        } catch (Exception ex) {
            log.error("error occur:{}", ex.getMessage());
        }
    }

    public void populateConsistentBuckets() {
        consistentBuckets = new TreeMap<Long, String>();

        MessageDigest md5 = MD5.get();
        if (this.totalWeight <= 0 && this.weights != null) {
            for (int i = 0; i < this.weights.length; i++)
                this.totalWeight += (this.weights[i] == null) ? 1
                        : this.weights[i];
        } else if (this.weights == null) {
            this.totalWeight = this.servers.length;
        }
        for (int i = 0; i < servers.length; i++) {
            // int thisWeight = 1;
            // if (this.weights != null && this.weights[i] != null)
            // thisWeight = this.weights[i];

            // double factor = Math.floor(((double) (40 * this.servers.length *
            // thisWeight)) / (double) this.totalWeight);
            long factor = 1;
            for (long j = 0; j < factor; j++) {
                byte[] d = md5.digest((servers[i] + "-" + j).getBytes());
                for (int h = 0; h < 1; h++) {
                    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
                            | ((long) (d[2 + h * 4] & 0xFF) << 16)
                            | ((long) (d[1 + h * 4] & 0xFF) << 8)
                            | ((long) (d[0 + h * 4] & 0xFF));

                    consistentBuckets.put(k, servers[i]);
                }
            }
        }
        long position = 0;
        for (Map.Entry<Long, String> map : consistentBuckets.entrySet()) {
            System.out.println("hash value:" + map.getKey() + " ip address:"
                    + map.getValue() + ":::" + position);
            serverToPosition.put(map.getValue(), position);
            positionToServer.put(position, map.getValue());
            position++;
        }
        initialized = true;
    }

    public final String getSock(String key) {
        return consistentBuckets.get(getBucket(key));
    }

    private final long getBucket(String key) {
        long hc = getHash(key);
        long result = findPointFor(hc);
        return result;
    }

    private final Long findPointFor(Long hv) {

        SortedMap<Long, String> tmap = this.consistentBuckets.tailMap(hv);

        return (tmap.isEmpty()) ? this.consistentBuckets.firstKey() : tmap
                .firstKey();
    }

    private final long getHash(String key) {
        return md5HashingAlg(key);
    }

    public void setWeights(Integer[] weights) {
        this.weights = weights;
    }

    public void setServers(String[] servers) {
        this.servers = servers;
    }

    public ConcurrentHashMap<String, Long> getServerPosition() {
        return serverToPosition;
    }

    public void setServerPosition(ConcurrentHashMap<String, Long> serverPosition) {
        this.serverToPosition = serverPosition;
    }

    public ConcurrentHashMap<Long, String> getPositionToServer() {
        return positionToServer;
    }

    public void setPositionToServer(
            ConcurrentHashMap<Long, String> positionToServer) {
        this.positionToServer = positionToServer;
    }

    public String[] getServers() {
        return servers;
    }

}
