package org.lab.mars.onem2m.server.quorum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lab.mars.onem2m.server.quorum.QuorumPeer.QuorumServer;

public class M2mQuorumServer {
	private HashMap<Long, HashMap<Long, QuorumServer>> positionToServers = new HashMap<>();

	private List<String> servers = new ArrayList<>();

   

	public HashMap<Long, HashMap<Long, QuorumServer>> getPositionToServers() {
		return positionToServers;
	}

	public void setPositionToServers(
			HashMap<Long, HashMap<Long, QuorumServer>> positionToServers) {
		this.positionToServers = positionToServers;
	}

	public List<String> getServers() {
		return servers;
	}

	public void setServers(List<String> servers) {
		this.servers = servers;
	}

}
