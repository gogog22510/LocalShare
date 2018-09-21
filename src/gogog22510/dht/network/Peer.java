package gogog22510.dht.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import gogog22510.dht.util.ListenerSupportWithCheck;

/**
 * Peer for P2P network
 * @author charles
 *
 */
public class Peer {
	private String peerId;
	private String address;
	private int port;
	public ServerSocket serverSocket;

	// listener
	private ListenerSupportWithCheck listeners = new ListenerSupportWithCheck(PeerOperationListener.class);
	private PeerOperationListener listenerProxy = (PeerOperationListener)listeners.getMulticaster();

	public Peer(String peerId, String address, int port) {
		this.peerId = peerId;
		this.address = address;
		this.port = port;
	}

	public void addPeerListener(PeerOperationListener l) {
		this.listeners.addListener(l);
	}

	public void removePeerListener(PeerOperationListener l) {
		this.listeners.removeListener(l);
	}

	// getters
	public String getPeerId() {
		return peerId;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public void openServer() throws IOException {

		try {
			serverSocket = new ServerSocket(port);
		} catch (Exception e) {
			return;
		}

		while (true) {
			//System.out.println("waiting for connection");
			Socket socket = serverSocket.accept();
			//System.out.println("connection accepted");
			OpenServer os = new OpenServer(socket);
			os.setListener(listenerProxy);
			os.start();
		}
	}
}
