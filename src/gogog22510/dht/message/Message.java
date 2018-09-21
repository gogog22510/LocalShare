package gogog22510.dht.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

import gogog22510.dht.core.KademliaId;

/**
 * Kademlia message structure
 * @author charles
 *
 */
public abstract class Message {
	public static final int HEADER_LENGTH 		= 49;
	public static final int MAX_LENGTH 			= 512 /*128*/;
	public static final int NOREPLY_MSGID		= 0;

	// type index, is response message, class path
	public enum MsgType {
		HELLO			(0, false, "gogog22510.dht.message.HelloMessage"),
		DISCOVER		(1, false, "gogog22510.dht.message.DiscoverMessage"),
		FIND_NODE		(2, false, "gogog22510.dht.message.FindNodeMessage"),
		RETURN_NODE		(3, true, "gogog22510.dht.message.ReturnNodeMessage"),
		RETURN_VALUE	(4, true, "gogog22510.dht.message.ReturnValueMessage"),
		STORE_VALUE		(5, false, "gogog22510.dht.message.StoreValueMessage"),
		LOOKUP_IP		(6, false, "gogog22510.dht.message.LookupIpMessage"),
		RETURN_IP		(7, true, "gogog22510.dht.message.ReturnIpMessage"),
		RETURN_NODE_IP	(8, true, "gogog22510.dht.message.ReturnNodeIpMessage"),
		RETURN_VALUE_IP	(9, true, "gogog22510.dht.message.ReturnValueIpMessage")
		;
		private int cmd;
		private boolean response;
		private String cp;
		MsgType(int cmd, boolean response, String cp) {
			this.cmd = cmd;
			this.response = response;
			this.cp = cp;
		}

		public int cmd() {
			return cmd;
		}

		public String classpath() {
			return cp;
		}

		public boolean isResponse() {
			return response;
		}

		public static MsgType valueOf(int cmd) {
			try {
				return values()[cmd];
			} catch (Exception e) {
				return null;
			}
		}
	}

	private static int counter = 1;
	public static synchronized int nextMsgId() {
		if(counter > 1024) {
			counter = 1;
		}
		return counter++;
	}

	public int msgid;
	public KademliaId kid;
	public boolean isResponse = false;
	public InetAddress ip = null;
	public String to = null;

	public Message(KademliaId kid) {
		this(NOREPLY_MSGID, kid);
	}
	public Message(int msgid, KademliaId kid) {
		this.msgid = msgid;
		this.kid = kid;
	}

	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	public void setIsResponse(boolean b) {
		this.isResponse = b;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public byte[] toBytes() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		serialize(bout);
		bout.flush();
		return bout.toByteArray();
	}

	public abstract void serialize(OutputStream out) throws IOException;
}
