package gogog22510.dht.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class FindNodeMessage extends Message {

	private String searchKey;

	public FindNodeMessage(KademliaId key) {
		this(nextMsgId(), key);
	}
	public FindNodeMessage(int msgid, KademliaId key) {
		super(msgid, key);
	}

	public void setSearchKey(String k) {
		// our buffer size is 128 - 28 = 100 (UTF-8 100 char)
		if(k.length() > (MAX_LENGTH-HEADER_LENGTH)) {
			this.searchKey = this.searchKey.substring(0, MAX_LENGTH-HEADER_LENGTH);
		}
		else {
			this.searchKey = k;
		}
	}

	public String getSearchKey() {
		return this.searchKey;
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.FIND_NODE.cmd(), kid.getBytes(), msgid, to);
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeUTF(searchKey);
		oout.flush();
	}

	public static FindNodeMessage parseFrom(byte[] buf, int length) throws IOException {
		int msgid = KademliaUtil.getMsgIDFromMessage(buf);
		KademliaId key = KademliaUtil.getKademliaIdFromMessage(buf);
		FindNodeMessage msg = new FindNodeMessage(msgid, key);
		ObjectInputStream oin = new ObjectInputStream(
				new ByteArrayInputStream(buf, HEADER_LENGTH, length-HEADER_LENGTH));
		String k = oin.readUTF();
		oin.close();
		msg.setSearchKey(k);
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId key = KademliaId.randomId();
		System.out.println("key: "+key.hashCode());
		FindNodeMessage msg = new FindNodeMessage(key);
		System.out.println("msgid: "+msg.msgid);
		String sk = "Hello World !!!!";
		System.out.println("searchKey: "+sk);
		System.out.println("to: "+msg.to);
		msg.setSearchKey(sk);
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		FindNodeMessage r = (FindNodeMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("msgid: "+r.msgid);
		System.out.println("searchKey: "+r.searchKey);
		System.out.println("to: "+r.to);
	}
}
