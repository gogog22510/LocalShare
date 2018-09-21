package gogog22510.dht.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import gogog22510.dht.core.Settings;

/**
 * Network adapter handling TCP/UDP connection
 * @author charles
 *
 */
public class NetworkAdapter {
	private static NetworkAdapter INSTANCE = null;
	public static NetworkAdapter getInstance() {
		if (INSTANCE == null) {
			synchronized (NetworkAdapter.class) {
				if (INSTANCE == null) {
					INSTANCE = new NetworkAdapter();
				}
			}
		}
		return INSTANCE;
	}

	// default settings
	public static final int TCP_PORT = 15001;
	public static final int UDP_PORT = 15002;
	private final int CONNECT_TO = 2000; // socket connect timeout
	private final int IO_TO = 10000; // I/O timeout

	// TCP
	private Peer peer;
	private Thread tcp_thread = null;

	// UDP
	private List<InetAddress> addresses;
	private DatagramSocket usocket, userver;
	private String externalip = null, localip = null;

	private NetworkAdapter() {
		init();
	}

	private void init() {
		String clientId = Settings.getInstance().getClientId();
		// init external ip
		try {
			this.externalip = PeerUtil.getExternalIP();
		} catch (IOException e) {
			// fallback to company name as external ip
			this.externalip = "testip";
		}

		// init local ip
		try {
			this.localip = PeerUtil.getLocalIP();
		} catch (IOException e) {
			// fallback to company name as external ip
			try {
				this.localip = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				this.localip = null;
			}
		}
		this.peer = new Peer(clientId, localip, TCP_PORT);
	}

	public String getExternalip() {
		return this.externalip;
	}

	public String getLocalip() {
		return this.localip;
	}

	public void startTCP() {
		// start TCP server
		if(tcp_thread == null) {
			synchronized (this) {
				if(tcp_thread == null) {
					tcp_thread = new Thread() {
						public void run() {
							try {
								peer.openServer();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					tcp_thread.setName("NetworkAdapter-TCPServer");
					tcp_thread.setDaemon(true);
					tcp_thread.start();
				}
			}
		}
	}

	public Peer getPeer() {
		return peer;
	}

	public Socket openTCPSocket(String ip) throws IOException {
		Socket socket = new Socket();
		socket.setSoTimeout(IO_TO);
		socket.connect(new InetSocketAddress(ip, TCP_PORT), CONNECT_TO);
		return socket;
	}

	public void startUDP() throws SocketException {
		if(addresses == null || usocket == null || usocket.isClosed() || userver == null || userver.isClosed()) {
			synchronized (this) {
				if(addresses == null) {
					addresses = PeerUtil.listAllBroadcastAddresses();
					if(userver != null) {
						userver.close();
					}
					userver = new DatagramSocket(UDP_PORT);
					userver.setReuseAddress(true);
					if(usocket != null) {
						usocket.close();
					}
					usocket = new DatagramSocket();
				}
			}
		}
	}

	public DatagramSocket getUDPSocket() {
		return this.usocket;
	}

	public DatagramSocket getUDPServer() {
		return this.userver;
	}

	public DatagramPacket receiveUDP(byte[] revbuf, int length) throws IOException {
		if(userver == null) throw new RuntimeException("UDP connection is not up");
		DatagramPacket packet = new DatagramPacket(revbuf, length);
		userver.receive(packet);
		return packet;
	}

	public void broadcast(byte[] tosend) throws IOException {
		if(usocket == null) throw new RuntimeException("UDP connection is not up");
		for(InetAddress a:addresses) {
			DatagramPacket packet = new DatagramPacket(tosend, tosend.length, a, UDP_PORT);
			usocket.send(packet);
		}
	}

	public void sendUDP(byte[] tosend, InetAddress... arr) throws IOException {
		if(usocket == null) throw new RuntimeException("UDP connection is not up");
		for(InetAddress a:arr) {
			DatagramPacket packet = new DatagramPacket(tosend, tosend.length, a, UDP_PORT);
			usocket.send(packet);
		}
	}

	public void closeUDP() {
		synchronized (this) {
			if(usocket != null) {
				usocket.close();
			}
			if(userver != null) {
				userver.close();
			}
			addresses = null;
		}
	}
}
