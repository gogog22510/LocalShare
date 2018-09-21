package gogog22510.dht.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import gogog22510.dht.core.KademliaUtil;
import gogog22510.dht.core.Settings;
import gogog22510.dht.message.Message;
import gogog22510.dht.message.MessageCracker;
import gogog22510.dht.util.ListenerSupportWithCheck;
import gogog22510.dht.util.Logger;

public class MessageSender {
	// threads
	private Thread receiveThread;
	private transient boolean isClosed = false;

	// buffer
	private byte[] revbuf;

	// routing
	private ListenerSupportWithCheck listeners = new ListenerSupportWithCheck(MessageSenderListener.class);
	private MessageSenderListener listenerProxy = (MessageSenderListener)listeners.getMulticaster();

	private Map<Integer, BlockingQueue<Message>> receiveQ;

	public MessageSender() throws IOException {
		this.receiveQ = new HashMap<Integer, BlockingQueue<Message>>();
		this.revbuf = new byte[Message.MAX_LENGTH];
		init();
	}

	public void addSenderListener(MessageSenderListener l) {
		this.listeners.addListener(l);
	}

	public void removeSenderListener(MessageSenderListener l) {
		this.listeners.removeListener(l);
	}

	private void init() throws IOException {
		NetworkAdapter.getInstance().startUDP();
		receiveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(!isClosed) {
						try {
							DatagramPacket packet = NetworkAdapter.getInstance().receiveUDP(revbuf, revbuf.length);
							Logger.getInstance().info(this, "receive packet "+packet.getAddress());
							// if coming from itself, ignore it
							if(packet.getAddress().getHostAddress().equals(NetworkAdapter.getInstance().getLocalip())) {
								continue;
							}
	
							// check msgId exist
							byte[] rev = packet.getData();
							// has destination and is not us, null means broadcast
							String dest = KademliaUtil.getToFromMessage(rev);
							if(dest != null && !NetworkAdapter.getInstance().getLocalip().equals(dest)) {
								continue;
							}
							int rev_size = packet.getLength();
							int msgId = KademliaUtil.getMsgIDFromMessage(rev);
							Logger.getInstance().info(this, "receive message["+msgId+"]");

							if(rev_size < Message.HEADER_LENGTH) {
								continue;
							}

							Message msg = MessageCracker.crack(rev, rev_size);
							msg.setIp(packet.getAddress());
							Logger.getInstance().info(this, "message["+msgId+"] = "+msg);

							if(msg.isResponse) {
								synchronized (receiveQ) {
									if(receiveQ.containsKey(msgId)) {
										receiveQ.get(msgId).add(msg);
										receiveQ.remove(msgId);
									}
								}
							}
							else {
								listenerProxy.onNoneResponseMessage(msg);
							}
						}
						catch (Exception e) {
							Logger.getInstance().error(this, e);
							if(NetworkAdapter.getInstance().getUDPServer().isClosed()) {
								break;
							}
						}
					}
				} finally {
					NetworkAdapter.getInstance().closeUDP();
					isClosed = true;
				}
			}
		}, getClass().getSimpleName()+"-receive");
		receiveThread.setDaemon(true);
		receiveThread.start();
	}

	public void close() {
		if(isClosed) {
			synchronized (this) {
				if(isClosed) {
					synchronized (receiveQ) {
						receiveQ.clear();
					}
					isClosed = true;
					if(receiveThread.isAlive()) {
						receiveThread.interrupt();
					}
				}
			}
		}
	}

	public boolean isClosed() {
		return this.isClosed;
	}

	public void sendMessage(Message msg, InetAddress... arr) throws IOException {
		// do unicast to each addresses
		for(InetAddress a:arr) {
			msg.setTo(a.getHostAddress());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			msg.serialize(out);
			byte[] tosend = out.toByteArray();
			// do unicast to each addresses
			if(Settings.getInstance().useUDPUnicast()) {
				send(tosend, a);
			}
			else {
				broadcast(tosend);
			}
		}
	}

	public void sendMessageWithReply(Message msg, BlockingQueue<Message> replyQueue, InetAddress... arr) throws IOException {
		synchronized (receiveQ) {
			receiveQ.put(msg.msgid, replyQueue);
		}
		sendMessage(msg, arr);
	}

	public void broadcastMessage(Message msg) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] tosend = out.toByteArray();
		broadcast(tosend);
	}

	public void broadcast(byte[] tosend) throws IOException {
		NetworkAdapter.getInstance().broadcast(tosend);
	}

	private void send(byte[] tosend, InetAddress... arr) throws IOException {
		NetworkAdapter.getInstance().sendUDP(tosend, arr);
	}

	public void unregister(Integer msgId) {
		synchronized (receiveQ) {
			receiveQ.remove(msgId);
		}
	}
}
