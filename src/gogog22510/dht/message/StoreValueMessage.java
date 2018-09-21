package gogog22510.dht.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class StoreValueMessage extends Message {

	private String saveData;

	public StoreValueMessage(KademliaId kid) {
		super(kid);
	}

	public void setSaveData(String d) {
		this.saveData = d;
	}

	public void setSaveData(String k, String v) {
		// our buffer size is MAX_LENGTH - 28
		String data = k+";"+v;
		if(data.length() > (MAX_LENGTH-HEADER_LENGTH)) {
			this.saveData = data.substring(0, MAX_LENGTH-HEADER_LENGTH);
		}
		else {
			this.saveData = data;
		}
	}

	/* assume format "key;value" */
	public String getSaveData() {
		return this.saveData;
	}

	public String[] getParts() {
		return this.saveData.split(";");
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.STORE_VALUE.cmd(), kid.getBytes(), msgid, to);
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeUTF(saveData);
		oout.flush();
	}

	public static StoreValueMessage parseFrom(byte[] buf, int length) throws IOException {
		KademliaId kid = KademliaUtil.getKademliaIdFromMessage(buf);
		StoreValueMessage msg = new StoreValueMessage(kid);
		ObjectInputStream oin = new ObjectInputStream(
				new ByteArrayInputStream(buf, HEADER_LENGTH, length-HEADER_LENGTH));
		String d = oin.readUTF();
		oin.close();
		msg.setSaveData(d);
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId key = KademliaId.randomId();
		System.out.println("key: "+key.hashCode());
		StoreValueMessage msg = new StoreValueMessage(key);
		System.out.println("msgid: "+msg.msgid);
		System.out.println("origin: "+msg.to);
		String d = "testfile;192.168.0.1";
		System.out.println("saveData: "+d);
		msg.setSaveData(d);
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		StoreValueMessage r = (StoreValueMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("msgid: "+r.msgid);
		System.out.println("origin: "+r.to);
		System.out.println("saveData: "+r.saveData);
	}
}
