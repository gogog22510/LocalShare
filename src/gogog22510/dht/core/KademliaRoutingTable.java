package gogog22510.dht.core;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import gogog22510.dht.message.DiscoverMessage;
import gogog22510.dht.message.HelloMessage;
import gogog22510.dht.message.Message;
import gogog22510.dht.network.MessageSender;
import gogog22510.dht.network.MessageSenderListener;
import gogog22510.dht.network.NetworkAdapter;
import gogog22510.dht.util.Logger;
import gogog22510.dht.util.ThreadingUtilities;

public class KademliaRoutingTable implements MessageSenderListener {

	private Logger logger;

	private KademliaId mkid;
	private String mip;

	// node list
//	private TwoWayMap<KademliaId, String> nodeMap;
	private KBuckets[] buckets;
	private Map<String, Long> healthMap;

	private MessageSender sender;

	private transient boolean isClosed = false;
	private Thread healthThread;

	// message
	private HelloMessage hello;

	public KademliaRoutingTable(MessageSender sender) throws NoSuchAlgorithmException, IOException {
		this.logger = Logger.getInstance();
		this.mip = NetworkAdapter.getInstance().getLocalip();
		this.mkid = new KademliaId(this.mip);
		this.mkid.hashCode();
		this.hello = new HelloMessage(mkid);
		this.sender = sender;
		this.buckets = new KBuckets[KademliaId.ID_LENGTH+1]; // the distance could be 0 ~ 160 (0 is itself)
		for(int i=0; i<this.buckets.length; i++) {
			this.buckets[i] = new KBuckets(KademliaUtil.getKSize());
		}
//		this.nodeMap = new TwoWayMap<KademliaId, String>();
		this.healthMap = new Hashtable<String, Long>();
		this.healthMap.put(getLocalIp(), System.currentTimeMillis());
		init();
	}

	public KademliaId getLocalKid() {
		return this.mkid;
	}

	public String getLocalIp() {
		return this.mip;
	}

	public boolean isClosed() {
		return isClosed;
	}

	private void init() throws IOException {
		// set listener to ack on discovery
		sender.addSenderListener(this);
		braodcastDiscover();

		// start health thread
		healthThread = new Thread(new Runnable() {
			@Override
			public void run() {
//				int checkCnt = 0;
				while(!isClosed) {
					try {
						// broadcast existance to network, stale ip will be kicked off by DHT operation
						ThreadingUtilities.interruptibleSleep(KademliaUtil.getDataRefreshCheckInterval());
//						checkCnt++;
						broadcastHello();
					} catch (IOException e) {
						logger.error(this, e);
						if(NetworkAdapter.getInstance().getUDPSocket().isClosed()) {
							logger.error(this, "udp socket closed. stop the health thread");
							teardown();
						}
					}/* finally {
						if(checkCnt == 6) {
							// do check and remove stale node
							checkList();
							checkCnt = 0;
						}
					}*/
				}
			}
		}, getClass().getSimpleName()+"-health");
		healthThread.setDaemon(true);
		healthThread.start();
	}

	/*private void checkList() {
		// do check and remove non exist peer
		synchronized (this) {
			String[] keySet = healthMap.keySet().toArray(new String[healthMap.size()]);
			for(String k:keySet) {
				long lastNotfiy = healthMap.get(k);
				int diffSec = (int)((System.currentTimeMillis() - lastNotfiy) / 1000);
				if(diffSec > 30) {
					removeNode(k);
					logger.info(this, "remove stale peer "+k+", not responding for "+diffSec+" secs");
				}
			}
		}
	}*/

	private void braodcastDiscover() throws IOException {
		sender.broadcastMessage(new DiscoverMessage(mkid));
	}

	private void broadcastHello() throws IOException {
		sender.broadcast(hello.toBytes());
	}

	private void sendHello(InetAddress... arr) throws IOException {
		sender.sendMessage(hello, arr);
	}

	private synchronized void addNode(String ip) throws NoSuchAlgorithmException {
		KademliaId id = new KademliaId(ip);
		addNode(id, ip);
	}

	private synchronized void addNode(KademliaId id, String ip) {
//		if(!nodeMap.containsKeyFirst(id) && !nodeMap.containsKeySecond(ip)) {
//			nodeMap.put(id, ip);
//			logger.info(this, "adding new node ["+id.hashCode()+"] "+ip);
//		}
		int d = this.mkid.getDistance(id);
		if(buckets[d].addNode(id, ip)) {
			logger.info(this, "adding new node "+id+"["+id.hashCode()+"] "+ip+" to buckets["+d+"]");
		}
		
		healthMap.put(ip, System.currentTimeMillis());
	}

	public synchronized void removeNode(KademliaId id) {
//		String ip = nodeMap.getFirst(id);
		int d = this.mkid.getDistance(id);
		String ip = buckets[d].getIp(id);
		buckets[d].removeNode(id);
		healthMap.remove(ip);
	}

	public synchronized void removeNode(String ip) {
//		nodeMap.removeSecond(ip);
//		healthMap.remove(ip);
		try {
			KademliaId id = new KademliaId(ip);
			removeNode(id);
		} catch (NoSuchAlgorithmException e) {
		}
	}

	public void addToRoutingTable(KademliaId id, String ip) {
		// if coming from itself, ignore it
		if(id.equals(mkid)) {
			return;
		}
		addNode(id, ip);
	}

	public void addToRoutingTable(String ip) throws NoSuchAlgorithmException {
		// if coming from itself, ignore it
		if(ip.equals(mip)) {
			return;
		}
		addNode(ip);
	}

	@Override
	public void onNoneResponseMessage(Message msg) {
		String ip = msg.ip.getHostAddress();
		try {
			if(!contains(ip)) {
				addToRoutingTable(ip);
			}
			if(msg instanceof DiscoverMessage) {
				sendHello(msg.ip);
			}
		} catch (NoSuchAlgorithmException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Return closest node list sorted by distance 
	 */
	public List<KademliaId> getClosestNode(KademliaId key) {
		List<KademliaId> klist = new ArrayList<KademliaId>();
		synchronized (this) {
//			klist.addAll(nodeMap.keySetFirst());
			for(int i=0; i<buckets.length; i++) {
				klist.addAll(buckets[i].getKidSet());
			}
		}
		KademliaIdComparator c = new KademliaIdComparator(key);
		Collections.sort(klist, c);
		return klist;
	}

	// get all ips
	public String[] getAllIps() {
		String[] r = null;
		synchronized (this) {
			r = healthMap.keySet().toArray(new String[healthMap.size()]);
		}
		return r;
	}

	// get ip by kid
	public String getIp(KademliaId key) {
		if(key.equals(mkid)) return mip;
		synchronized (this) {
//			return nodeMap.getFirst(key);
			int d = this.mkid.getDistance(key);
			return buckets[d].getIp(key);
		}
	}

	// get kid by ip
	public KademliaId getKid(String ip) {
		if(ip.equals(mip)) return mkid;
		synchronized (this) {
//			return nodeMap.getSecond(ip);
			try {
				KademliaId id = new KademliaId(ip);
				int d = this.mkid.getDistance(id);
				return buckets[d].getKid(ip);
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
		}
	}

	// get ip list by kid list
	public List<String> getIp(List<KademliaId> keys) {
		List<String> klist = new ArrayList<String>();
		synchronized (this) {
			for(KademliaId k:keys) {
				klist.add(getIp(k));
			}
		}
		return klist;
	}

	// get kid list by ip list
	public List<KademliaId> getKid(List<String> ips) {
		List<KademliaId> klist = new ArrayList<KademliaId>();
		synchronized (this) {
			for(String i:ips) {
				klist.add(getKid(i));
			}
		}
		return klist;
	}

	// check ip is in routing table
	public boolean contains(String ip) {
		// utilize health map to check ip
		return healthMap.containsKey(ip);
	}

	// check kid is in routing table
	public boolean contains(KademliaId kid) {
		int d = this.mkid.getDistance(kid);
		return buckets[d].contains(kid);
	}

	public void start() throws IOException {
		if(isClosed) {
			synchronized (this) {
				if(isClosed) {
					isClosed = true;
					init();
				}
			}
		}
	}

	public void teardown() {
		synchronized (this) {
			isClosed = true;
			sender.removeSenderListener(this);
			for(int i=0; i<buckets.length; i++) {
				buckets[i].teardown();
			}
			healthMap.clear();
			if(healthThread.isAlive()) {
				healthThread.interrupt();
			}
		}
	}
}
