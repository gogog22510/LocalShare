package gogog22510.dht.core;

import gogog22510.dht.util.Logger;

public class PeerCache implements IPeerCache {
	private static PeerCache INSTANCE = null;
	public static PeerCache getInstance() {
		if (INSTANCE == null) {
			synchronized (PeerCache.class) {
				if (INSTANCE == null) {
					INSTANCE = new PeerCache();
				}
			}
		}
		return INSTANCE;
	}

	private IPeerCache holder = null;
	private PeerCache() {
		try {
			holder = DistributedHashTable.getInstance();
		} catch (Exception e) {
			Logger.getInstance().info(this, "cannot instantiate DistributedHashTable.");
		}
	}

	@Override
	public String[] getPeerList(String key) {
		if(holder != null) {
			return holder.getPeerList(key);
		}
		return null;
	}

	@Override
	public boolean isReady() {
		if(holder != null) {
			return holder.isReady();
		}
		return false;
	}
}
