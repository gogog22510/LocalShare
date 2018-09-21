package gogog22510.dht.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class LookupIpMessage extends Message {

	public LookupIpMessage(KademliaId lookupKey) {
		this(nextMsgId(), lookupKey);
	}
	public LookupIpMessage(int msgid, KademliaId lookupKey) {
		super(msgid, lookupKey);
	}

	public KademliaId getLookupKey() {
		return this.kid;
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.LOOKUP_IP.cmd(), kid.getBytes(), msgid, to);
	}

	public static LookupIpMessage parseFrom(byte[] buf, int length) throws IOException {
		int msgid = KademliaUtil.getMsgIDFromMessage(buf);
		KademliaId key = KademliaUtil.getKademliaIdFromMessage(buf);
		LookupIpMessage msg = new LookupIpMessage(msgid, key);
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId key = KademliaId.randomId();
		System.out.println("key: "+key.hashCode());
		LookupIpMessage msg = new LookupIpMessage(key);
		System.out.println("msgid: "+msg.msgid);
		System.out.println("lookupKey: "+key.hashCode());
		System.out.println("to: "+msg.to);
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		LookupIpMessage r = (LookupIpMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("msgid: "+r.msgid);
		System.out.println("lookupKey: "+r.getLookupKey().hashCode());
		System.out.println("to: "+r.to);
	}
}
