package gogog22510.dht.core;
import java.util.Set;

import gogog22510.dht.util.TwoWayMap;

/**
 * Kademlia K buckets for routing table
 * @author charles
 *
 */

public class KBuckets {
	private final int max_size;
	private TwoWayMap<KademliaId, String> nodeMap;

	public KBuckets(int capacity) {
		this.max_size = capacity;
		this.nodeMap = new TwoWayMap<KademliaId, String>();
	}

	/**
	 * add node to buckets
	 * @param id kademlia id
	 * @param ip ip
	 * @return true if succesfully add
	 */
	public synchronized boolean addNode(KademliaId id, String ip) {
		if(!nodeMap.containsKeyFirst(id) && !nodeMap.containsKeySecond(ip)) {
			if(nodeMap.size() >= max_size) return false;
			nodeMap.put(id, ip);
			return true;
		}
		return false;
	}

	public void removeNode(KademliaId id) {
		String ip = nodeMap.getFirst(id);
		removeNode(ip);
	}

	public void removeNode(String ip) {
		nodeMap.removeSecond(ip);
	}

	// check ip is in routing table
	public boolean contains(String ip) {
		return nodeMap.containsKeySecond(ip);
	}

	// check kid is in routing table
	public boolean contains(KademliaId kid) {
		return nodeMap.containsKeyFirst(kid);
	}

	public KademliaId getKid(String ip) {
		return nodeMap.getSecond(ip);
	}

	public String getIp(KademliaId kid) {
		return nodeMap.getFirst(kid);
	}

	public Set<KademliaId> getKidSet() {
		return nodeMap.keySetFirst();
	}

	public void teardown() {
		this.nodeMap.teardown();
	}
}
