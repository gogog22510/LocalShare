package gogog22510.dht.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class HelloMessage extends Message {

	public HelloMessage(KademliaId kid) {
		super(kid);
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.HELLO.cmd(), kid.getBytes(), NOREPLY_MSGID, to);
	}

	public static HelloMessage parseFrom(byte[] buf, int length) throws IOException {
		KademliaId kid = KademliaUtil.getKademliaIdFromMessage(buf);
		HelloMessage msg = new HelloMessage(kid);
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId key = KademliaId.randomId();
		System.out.println("key: "+key.hashCode());
		HelloMessage msg = new HelloMessage(key);
		System.out.println("to: "+msg.to);
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		HelloMessage r = (HelloMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("to: "+r.to);
	}
}
