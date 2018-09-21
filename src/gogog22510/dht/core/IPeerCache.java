package gogog22510.dht.core;

public interface IPeerCache {
	public String[] getPeerList(String key);
	public boolean isReady();
}
