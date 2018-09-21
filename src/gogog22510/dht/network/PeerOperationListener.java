package gogog22510.dht.network;
/**
 * TCP Peer operation listener
 * @author charles
 *
 */
public interface PeerOperationListener {
	public void onOperation(int cmd, Object... params);
}
