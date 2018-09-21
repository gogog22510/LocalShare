package gogog22510.dht.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class ReturnNodeIpMessage extends Message {

	private List<String> ipList;
	public ReturnNodeIpMessage(int msgid, KademliaId kid) {
		super(msgid, kid);
		this.ipList = new ArrayList<String>();
	}

	public void addIp(String ip) {
		this.ipList.add(ip);
	}

	public List<String> getIpList() {
		return ipList;
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.RETURN_NODE_IP.cmd(), kid.getBytes(), msgid, to);
		String data = Joiner.on(";").join(ipList);
		if(data.length() > (MAX_LENGTH-HEADER_LENGTH)) {
			data = data.substring(0, MAX_LENGTH-HEADER_LENGTH);
		}
		ObjectOutputStream oo = new ObjectOutputStream(out);
		oo.writeUTF(data);
		oo.flush();
	}

	public static ReturnNodeIpMessage parseFrom(byte[] buf, int length) throws IOException {
		int msgid = KademliaUtil.getMsgIDFromMessage(buf);
		KademliaId kid = KademliaUtil.getKademliaIdFromMessage(buf);
		ReturnNodeIpMessage msg = new ReturnNodeIpMessage(msgid, kid);
		ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(buf, HEADER_LENGTH, length-HEADER_LENGTH));
		String d = oin.readUTF();
		oin.close();
		msg.ipList.addAll(Arrays.asList(d.split(";")));
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId kid = KademliaId.randomId();
		System.out.println("kid: "+kid.hashCode());
		ReturnNodeIpMessage msg = new ReturnNodeIpMessage(87, kid);
		System.out.println("msgid: "+msg.msgid);
		System.out.println("origin: "+msg.to);
		for(int i=0; i<5; i++) {
			String ip = "192.168.111.11"+i;
			msg.addIp(ip);
			System.out.println("["+i+"]: "+ip);
		}
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		ReturnNodeIpMessage r = (ReturnNodeIpMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("msgid: "+r.msgid);
		System.out.println("origin: "+r.to);
		int j = 0;
		while (j < r.getIpList().size()) {
			System.out.println("["+j+"]: "+r.getIpList().get(j));
			j++;
		}
	}
}
