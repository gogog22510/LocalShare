package gogog22510.dht.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gogog22510.dht.util.Logger;
import gogog22510.dht.util.ThreadingUtilities;

/**
 * DHT data refresh thread<br>
 * (1) Handle republish data entries<br>
 * (2) Handle expire stale entries
 * @author charles
 *
 */
public class DataRefreshThread extends Thread {
	private Logger logger;
	private DistributedHashTable dht;
	private Map<String, Long> refreshMap;
	private transient boolean isClosed = false;

	public DataRefreshThread(DistributedHashTable dht) {
		super("DataRefreshThread");
		this.dht = dht;
		this.refreshMap = new HashMap<String, Long>();
		setDaemon(true);
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void teardown() {
		this.isClosed = true;
	}

	public void addRefreshEntry(String k) {
		synchronized (refreshMap) {
			this.refreshMap.put(k, System.currentTimeMillis());
		}
	}

	public void removeRefreshEntry(String k) {
		synchronized (refreshMap) {
			this.refreshMap.remove(k);
		}
	}

	@Override
	public void run() {
		while(!isClosed) {
			ThreadingUtilities.uninterruptibleSleep(KademliaUtil.getDataRefreshCheckInterval());
			try {
				doExpireEntries();
				doRepublish();
			} catch (Exception e) {
				// Do Nothing
			}
		}
	}

	private void doExpireEntries() {
		try {
			String[] keys = dht.getTable().keySet().toArray(new String[0]);
			for(String k: keys) {
				Set<String> values = dht.getTable().get(k);
				synchronized (values) {
					Iterator<String> iter = values.iterator();
					while(iter.hasNext()) {
						String ip = iter.next();
						if(!dht.getRoutingTable().contains(ip)) {
							iter.remove();
							if(logger != null) logger.info(this, "entry stale, remove "+k+","+ip+" from table");
						}
					}
				}
				if(values.size() == 0) {
					dht.getTable().remove(k);
					synchronized (refreshMap) {
						refreshMap.remove(k);
					}
				}
			}
		} catch (Exception e) {
			// Do Nothing
		}
	}

	private void doRepublish() {
		try {
			if(refreshMap.size() > 0) {
				String[] keys = null;
				synchronized (refreshMap) {
					keys = refreshMap.keySet().toArray(new String[refreshMap.size()]);
				}
				long end = System.currentTimeMillis();
				for(String k:keys) {
					long start = refreshMap.get(k);
					if((end - start) >= KademliaUtil.getDataRepublishInterval()) {
						Set<String> vSet = dht.getTable().get(k);
						if(vSet != null) {
							String[] ss = null;
							synchronized (vSet) {
								ss = vSet.toArray(new String[vSet.size()]);
							}
							for(String v:ss) {
								dht.storeValue(k, v);
								this.logger.info(this, "refresh data to network "+k+" "+v);
							}
							addRefreshEntry(k);
						}
						else {
							removeRefreshEntry(k);
						}
					}
				}
			}
		} catch (Exception e) {
			// Do Nothing
		}
	}
}
