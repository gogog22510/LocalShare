package gogog22510.dht.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class ReturnIpMessage extends Message {

	private String lookupIp = null;

	public ReturnIpMessage(int msgid, KademliaId kid) {
		super(msgid, kid);
	}

	public void setLookupIp(String ip) {
		this.lookupIp = ip;
	}

	public String getLookupIp() {
		if(this.lookupIp != null && this.lookupIp.isEmpty()) return null;
		return this.lookupIp;
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.RETURN_IP.cmd(), kid.getBytes(), msgid, to);
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeUTF(lookupIp == null? "":lookupIp);
		oout.flush();
	}

	public static ReturnIpMessage parseFrom(byte[] buf, int length) throws IOException {
		int msgid = KademliaUtil.getMsgIDFromMessage(buf);
		KademliaId kid = KademliaUtil.getKademliaIdFromMessage(buf);
		ReturnIpMessage msg = new ReturnIpMessage(msgid, kid);
		ObjectInputStream oin = new ObjectInputStream(
				new ByteArrayInputStream(buf, HEADER_LENGTH, length-HEADER_LENGTH));
		String d = oin.readUTF();
		oin.close();
		msg.setLookupIp(d);
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId key = KademliaId.randomId();
		System.out.println("key: "+key.hashCode());
		ReturnIpMessage msg = new ReturnIpMessage(87, key);
		System.out.println("msgid: "+msg.msgid);
		String d = null;//"192.168.0.1";
		System.out.println("lookupIp: "+d);
		System.out.println("origin: "+msg.to);
		msg.setLookupIp(d);
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		ReturnIpMessage r = (ReturnIpMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("msgid: "+r.msgid);
		System.out.println("lookupIp: "+r.getLookupIp());
		System.out.println("origin: "+r.to);
	}
}
