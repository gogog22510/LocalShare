package gogog22510.dht.core;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import gogog22510.dht.message.StoreValueMessage;
import gogog22510.dht.network.MessageSender;
import gogog22510.dht.network.NetworkAdapter;
import gogog22510.dht.network.PeerClient;
import gogog22510.dht.network.PeerOperationListener;
import gogog22510.dht.util.Logger;

/**
 * Distributed HashTable based on Kademlia algorithm (simplified version)
 * @author charles
 *
 */
public class DistributedHashTable implements PeerOperationListener, IPeerCache {
	private static DistributedHashTable INSTANCE = null;
	public static DistributedHashTable getInstance() throws NoSuchAlgorithmException, IOException {
		if (INSTANCE == null) {
			synchronized (DistributedHashTable.class) {
				if (INSTANCE == null) {
					INSTANCE = new DistributedHashTable();
				}
			}
		}
		return INSTANCE;
	}

	private Logger logger;

	private Hashtable<String, Set<String>> table;
	private KademliaRoutingTable routingTable;
	private MessageSender sender;
	private DataRefreshThread dataRefresher;
	private DHTOperationHandler optHandler;

	private DistributedHashTable() throws NoSuchAlgorithmException, IOException {
		this.logger = Logger.getInstance();
		this.table = new Hashtable<String, Set<String>>();
		NetworkAdapter.getInstance().getPeer().addPeerListener(this);
		this.sender = new MessageSender();
		this.routingTable = new KademliaRoutingTable(this.sender);
		this.optHandler = new DHTOperationHandler(this, routingTable, sender);
		this.optHandler.setLogger(logger);
		this.sender.addSenderListener(this.optHandler);
		this.dataRefresher = new DataRefreshThread(this);
		this.dataRefresher.setLogger(this.logger);
		this.dataRefresher.start();
		this.logger.info(this, "Distributed Hashtable startup");
	}

	Hashtable<String, Set<String>> getTable() {
		return this.table;
	}

	KademliaRoutingTable getRoutingTable() {
		return this.routingTable;
	}

	public KademliaId getLocalKid() {
		return routingTable.getLocalKid();
	}

	// put value with local ip
	public void put(String key) {
		put(key, routingTable.getLocalIp());
	}
	// put value to dht
	public void put(String key, String value) {
		if(key == null) return;
		putLocal(key, value);
		storeValue(key, value);
	}

	// put pair to local cache
	public void putLocal(String key, String value) {
		if(!table.containsKey(key)) {
			synchronized (table) {
				if(!table.containsKey(key)) {
					table.put(key, new HashSet<String>());
				}
			}
		}
		Set<String> s = table.get(key);
		synchronized (s) {
			s.add(value);
		}
		this.dataRefresher.addRefreshEntry(key);
	}

	// get value from local hash table
	public Set<String> get(String key) {
		if(key == null) return null;
		if(table.containsKey(key)) {
			if(table.get(key).size() == 0) {
				return findValue(key);
			}
			else {
				return table.get(key);
			}
		}
		else {
			return findValue(key);
		}
	}

	/*
	 * filter and keep available ip (exclude local ip)
	 */
	public Collection<String> filterAvailableIp(Collection<String> ips) {
		if(ips == null) return null;
		List<String> list = new ArrayList<String>();
		for(String x:ips) {
			if(routingTable.getLocalIp().equals(x)) continue;
			if(routingTable.contains(x)) {
				list.add(x);
			}
		}
		return list;
	}

	// store pair to nodes, key is specified and value is local ip
	public void storeValue(String key, String value) {
		if(!isReady()) {
			logger.info(this, "skip store value operation, component is not ready or teardown.");
			return;
		}
		try {
			// do store value operation by search key
			KademliaId searchKey = new KademliaId(key);

			// start from closest node
			List<KademliaId> closest = routingTable.getClosestNode(searchKey);
			int try_size = Math.min(closest.size(), KademliaUtil.getAlphaSize());

			// save value at closest nodes
			StoreValueMessage msg = new StoreValueMessage(routingTable.getLocalKid());
			msg.setSaveData(key, value);

			List<InetAddress> al = new ArrayList<InetAddress>();
			for(int i=0; i<try_size; i++) {
				String ip = routingTable.getIp(closest.get(i));
				if(ip != null) {
					al.add(InetAddress.getByName(ip));
				}
			}
			sender.sendMessage(msg, al.toArray(new InetAddress[al.size()]));
		} catch (Exception e) {
			logger.error(this, e);
		}
	}

	public Set<String> findValue(String key) {
		if(!isReady()) {
			logger.info(this, "skip find value operation, component is not ready or teardown.");
			return null;
		}
		Set<String> result = null;
		try {
			DHTFindValueOperation fvOpt = new DHTFindValueOperation(key, this, routingTable, sender);
			fvOpt.setLogger(logger);
			result = fvOpt.execute();
		} catch (Exception e) {
			logger.error(this, e);
			if(sender.isClosed()) {
				teardown();
			}
		}
		return result;
	}

	@Override
	public String[] getPeerList(String key) {
		String[] result = null;
		Set<String> r = get(key);
		if(r == null) {
//			logger.info(this, "cannot find "+key+" in distributed hash table, return all peers");
//			result = routingTable.getAllIps();
			logger.info(this, "cannot find "+key+" in distributed hash table");
		}
		else {
			synchronized (r) {
				Collection<String> l = filterAvailableIp(r);
				result = l.toArray(new String[l.size()]);
			}
		}
		return result;
	}

	@Override
	public boolean isReady() {
		return !routingTable.isClosed();
	}

	@Override
	public void onOperation(int cmd, Object... params) {
		switch (cmd) {
			case PeerClient.OPT_UPLOAD:
				if(params != null && params.length > 0) {
					String key = (String)params[0];
					put(key, routingTable.getLocalIp());
					logger.info(this, "receive upload operation "+key+", put to local table");
				}
				break;
			default:
				break;
		}
	}

	public void teardown() {
		synchronized (this) {
			if(dataRefresher.isAlive()) {
				dataRefresher.teardown();
			}
			if(dataRefresher.isAlive()) {
				dataRefresher.teardown();
			}
			sender.removeSenderListener(this.optHandler);
			if(!routingTable.isClosed()) {
				routingTable.teardown();
			}
			table.clear();
		}
	}

	public static void main(String[] args) throws Exception {
		// init kademlia parameters
		KademliaUtil.init(Settings.getInstance());
		
		DistributedFileSystem.getInstance();
		Scanner scanner = new Scanner(System.in);
		int option = 0;
		String key = null;
		while (true) {
			System.out.println("\n\nSelect the option:");
			System.out.println("\t0 - Get");
			System.out.println("\t1 - Put");
			System.out.println("\t2 - Search Local");
			System.out.println("\t3 - Download");
			System.out.println("\t4 - Upload");
			System.out.println("\t5 - Show All Entries");
			System.out.println("\t6 - Show All Ips");
			System.out.println("\t7 - Show Local Ip");
			System.out.println("\t99 - Exit");
			try {
				option = scanner.nextInt();
				if (option == 0) {
					System.out.print("Key: ");
					key = scanner.next();
					Set<String> s = getInstance().get(key);
					System.out.println(s);
				}
				else if(option == 1) {
					System.out.print("Key: ");
					key = scanner.next();

					getInstance().put(key);
				}
				else if(option == 2) {
					System.out.print("key: ");
					key = scanner.next();
					System.out.println("Search Local: "+DistributedFileSystem.searchLocal(key));
				}
				else if(option == 3) {
					System.out.print("key: ");
					key = scanner.next();

					File f = DistributedFileSystem.getInstance().download(key);
					if(f != null) {
						System.out.println(f.getAbsolutePath());
					}
					else {
						System.out.println("Download Failed.");
					}
				}
				else if(option == 4) {
					System.out.print("key: ");
					key = scanner.next();
					System.out.print("ip: ");
					String ip = scanner.next();
					boolean b = DistributedFileSystem.getInstance().upload(key, ip);
					System.out.println("Upload Status: "+b);
				}
				else if(option == 5) {
					System.out.println(getInstance().table.entrySet());
				}
				else if(option == 6) {
					System.out.println(Arrays.toString(getInstance().routingTable.getAllIps()));
				}
				else if(option == 7) {
					System.out.println(getInstance().routingTable.getLocalIp());
				}
				else if(option == 99) {
					break;
				}
			} catch (Exception e) {
				// e.printStackTrace();
				System.out.println("Oops, something went wrong.");
				scanner = new Scanner(System.in);
				// break;
			}
		}
		scanner.close();
	}
}
