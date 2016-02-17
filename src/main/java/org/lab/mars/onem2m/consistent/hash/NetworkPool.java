package org.lab.mars.onem2m.consistent.hash;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.lab.mars.onem2m.server.quorum.QuorumPeer;
import org.lab.mars.onem2m.server.quorum.QuorumPeer.LearnerType;
import org.lab.mars.onem2m.server.quorum.QuorumPeer.QuorumServer;
import org.lab.mars.onem2m.server.quorum.QuorumPeerOperator;
import org.lab.mars.onem2m.server.quorum.QuorumPeerStatistics;
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

    private volatile List<String> allServers;
    private volatile String mySelfIpAndPort;
    /**
     * server对应的位置
     */
    private ConcurrentHashMap<String, Long> allServerToPosition = new ConcurrentHashMap<String, Long>();

    /**
     * 位置对应的server
     */
    private ConcurrentHashMap<Long, String> allPositionToServer = new ConcurrentHashMap<Long, String>();

    protected HashMap<Long, QuorumServer> allQuorumServers = null;

    public static ConcurrentHashMap<String, Integer> webPort = new ConcurrentHashMap<String, Integer>();
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

    private volatile List<String> deadServers = new ArrayList<String>();

    private Integer replicationFactor;
    protected Map<String, Long> allServersToSid;

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
    public synchronized void initialize() {
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
        TreeMap<Long, String> newConsistentBuckets = new TreeMap<Long, String>();
        MessageDigest md5 = MD5.get();
        if (this.totalWeight <= 0 && this.weights != null) {
            for (int i = 0; i < this.weights.length; i++)
                this.totalWeight += (this.weights[i] == null) ? 1
                        : this.weights[i];
        } else if (this.weights == null) {
            this.totalWeight = this.servers.length;
        }
        for (int i = 0; i < servers.length; i++) {
            long factor = 1;
            for (long j = 0; j < factor; j++) {
                byte[] d = md5.digest((servers[i] + "-" + j).getBytes());
                for (int h = 0; h < 1; h++) {
                    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
                            | ((long) (d[2 + h * 4] & 0xFF) << 16)
                            | ((long) (d[1 + h * 4] & 0xFF) << 8)
                            | ((long) (d[0 + h * 4] & 0xFF));

                    newConsistentBuckets.put(k, servers[i]);
                }
            }
        }
        long position = 0;
        for (Map.Entry<Long, String> map : newConsistentBuckets.entrySet()) {
            System.out.println("position:" + position + " ip: "
                    + map.getValue());
            serverToPosition.put(map.getValue(), position);
            positionToServer.put(position, map.getValue());
            position++;
        }
        this.consistentBuckets = newConsistentBuckets;
        initialized = true;
    }

    public final String getSock(String key) {
        if (initialized == false) {
            log.error("can't get sock becaus network is not intialzed!");
            throw new NullPointerException();
        }
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

    /**
     * 在设置最新的server列表 servers 所有存活的节点
     * 
     * 
     * @param servers
     */
    public synchronized void setServers(String[] servers, boolean isOk) {
        this.servers = servers;
        if (isOk) {
            List<String> nowDeadServers = new ArrayList<String>();
            List<String> survivalServers = new ArrayList<String>();
            for (String server : servers) {
                survivalServers.add(server);
            }
            for (String server : allServers) {
                if (!survivalServers.contains(server)) {
                    nowDeadServers.add(server);
                }
            }
            for (String server : nowDeadServers) {
                if (!deadServers.contains(server)) {
                    deadServers.add(server);
                    if (QuorumPeerStatistics.quorums.get(server) != null) {
                        continue;
                    }
                    startQuorumPeer(server);

                }
            }
            for (String server : deadServers) {
                if (!nowDeadServers.contains(server)) {
                    deadServers.remove(server);
                    stopQuorumPeer(server);
                }
            }
        }

    }

    /**
     * 开启对应的服务
     * 
     * @param server
     */
    public void startQuorumPeer(String server) {
        Long position = this.allServerToPosition.get(mySelfIpAndPort);
        Long deadPosition = this.allServerToPosition.get(server);
        System.out.println("position:" + position);
        System.out.println("deadPosition:" + deadPosition);
        long index = 0;
        int i = 0;
        for (i = 1; i < allPositionToServer.size() * replicationFactor
                && index < replicationFactor; i++) {
            if (!deadServers
                    .contains(allPositionToServer.get(deadPosition + i))) {
                index++;
            }
        }
        System.out.println(i + "LL");
        System.out.println("index:" + index);
        if (index == replicationFactor) {
            long myPosition = deadPosition + i - 1 >= allPositionToServer
                    .size() ? (deadPosition + i - 1)
                    % allPositionToServer.size() : deadPosition + i - 1;
            System.out.println("myPosition:" + myPosition);
            if (allPositionToServer.get(myPosition).equals(mySelfIpAndPort)) {
                HashMap<Long, QuorumServer> map = new HashMap<Long, QuorumServer>();
                for (i = 0; i < replicationFactor; i++) {
                    System.out.println(i + "III");
                    if (positionToServer.size() == 0) {
                        System.out.println("为空");
                    }
                    Long newPosition = deadPosition + 1 >= positionToServer
                            .size() ? (deadPosition + 1)
                            % positionToServer.size() : (deadPosition + 1);

                    String newServer = positionToServer.get(newPosition);
                    long allDeadPosition = this.allServerToPosition.get(server);// 挂掉节点的位置
                    long allNewServer = this.allServerToPosition.get(newServer);// 最新节点的位置
                    long sid = allServersToSid.get(newServer);
                    QuorumServer quorumServer = allQuorumServers.get(sid);
                    String address = quorumServer.addr.getAddress()
                            .getHostAddress();
                    Integer firstPort = quorumServer.addr.getPort();
                    Integer secondPort = quorumServer.electionAddr.getPort();
                    Integer distance = (int) ((allNewServer - allDeadPosition) > 0 ? (allNewServer - allDeadPosition)
                            : (allNewServer - allDeadPosition + allServers
                                    .size()));
                    InetSocketAddress firstInetSocketAddress = new InetSocketAddress(
                            address, firstPort - distance);
                    InetSocketAddress secondInetSocketAddress = new InetSocketAddress(
                            address, secondPort - distance);
                    QuorumServer myQuorumServer = new QuorumServer(sid,
                            firstInetSocketAddress, secondInetSocketAddress,
                            LearnerType.PARTICIPANT);
                    map.put(sid, myQuorumServer);

                }
                QuorumPeerOperator.startQuorumPeer(map, server);
            }
        }

    }

    public void stopQuorumPeer(String server) {
        Long position = this.serverToPosition.get(mySelfIpAndPort);
        Long deadPosition = this.serverToPosition.get(server);
        if (position == null || deadPosition == null) {
            return;
        }
        if ((deadPosition + replicationFactor) % serverToPosition.size() == position) {
            QuorumPeer quorumPeer = QuorumPeerStatistics.quorums.get(server);
            if (quorumPeer == null) {
                return;
            }
            quorumPeer.shutdown();
            QuorumPeerStatistics.quorums.remove(server);
        }
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

    public List<String> getAllServers() {
        return allServers;
    }

    /**
     * 设置完成以后,进行相应的处理
     * 
     * @param allServers
     */
    public void setAllServers(List<String> allServers) {
        this.allServers = allServers;
        TreeMap<Long, String> newConsistentBuckets = new TreeMap<Long, String>();
        MessageDigest md5 = MD5.get();

        for (int i = 0; i < allServers.size(); i++) {
            long factor = 1;
            for (long j = 0; j < factor; j++) {
                byte[] d = md5.digest((allServers.get(i) + "-" + j).getBytes());
                for (int h = 0; h < 1; h++) {
                    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
                            | ((long) (d[2 + h * 4] & 0xFF) << 16)
                            | ((long) (d[1 + h * 4] & 0xFF) << 8)
                            | ((long) (d[0 + h * 4] & 0xFF));

                    newConsistentBuckets.put(k, allServers.get(i));
                }
            }
        }
        long position = 0;
        for (Map.Entry<Long, String> map : newConsistentBuckets.entrySet()) {
            allServerToPosition.put(map.getValue(), position);
            allPositionToServer.put(position, map.getValue());
            position++;
        }
    }

    public HashMap<Long, QuorumServer> getAllQuorumServers() {
        return allQuorumServers;
    }

    public void setAllPositionToServer(
            ConcurrentHashMap<Long, String> allPositionToServer) {
        this.allPositionToServer = allPositionToServer;
    }

    public String getMySelfIpAndPort() {
        return mySelfIpAndPort;
    }

    public void setMySelfIpAndPort(String mySelfIpAndPort) {
        this.mySelfIpAndPort = mySelfIpAndPort;
    }

    public Integer getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Map<String, Long> getAllServersToSid() {
        return allServersToSid;
    }

    public void setAllServersToSid(Map<String, Long> allServersToSid) {
        this.allServersToSid = allServersToSid;
    }

    public void setAllQuorumServers(HashMap<Long, QuorumServer> allQuorumServers) {
        this.allQuorumServers = allQuorumServers;
    }

}
