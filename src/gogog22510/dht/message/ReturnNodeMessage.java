package gogog22510.dht.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gogog22510.dht.core.KademliaId;
import gogog22510.dht.core.KademliaUtil;

public class ReturnNodeMessage extends Message {

	private List<KademliaId> idList;
	public ReturnNodeMessage(int msgid, KademliaId kid) {
		super(msgid, kid);
		this.idList = new ArrayList<KademliaId>();
	}

	public void addId(KademliaId id) {
		this.idList.add(id);
	}

	public List<KademliaId> getIdList() {
		return idList;
	}

	@Override
	public void serialize(OutputStream out) throws IOException {
		KademliaUtil.writeMessageHeader(out, MsgType.RETURN_NODE.cmd(), kid.getBytes(), msgid, to);
		for(KademliaId id:idList) {
			out.write(id.getBytes());
		}
	}

	public static ReturnNodeMessage parseFrom(byte[] buf, int length) throws IOException {
		int msgid = KademliaUtil.getMsgIDFromMessage(buf);
		KademliaId kid = KademliaUtil.getKademliaIdFromMessage(buf);
		ReturnNodeMessage msg = new ReturnNodeMessage(msgid, kid);
		ByteArrayInputStream in = new ByteArrayInputStream(buf, HEADER_LENGTH, length-HEADER_LENGTH);
		byte[] tmp = new byte[KademliaId.ID_LENGTH / 8];
		while(in.read(tmp) > 0) {
			msg.addId(new KademliaId(Arrays.copyOfRange(tmp, 0, tmp.length)));
		}
		in.close();
		return msg;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("[Message]");
		KademliaId kid = KademliaId.randomId();
		System.out.println("kid: "+kid.hashCode());
		ReturnNodeMessage msg = new ReturnNodeMessage(87, kid);
		System.out.println("msgid: "+msg.msgid);
		System.out.println("origin: "+msg.to);
		for(int i=0; i<5; i++) {
			KademliaId id = KademliaId.randomId();
			msg.addId(id);
			System.out.println("["+i+"]: "+id.hashCode());
		}
		System.out.println();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		msg.serialize(out);
		byte[] data = out.toByteArray();

		System.out.println("[Parse] size: "+data.length);
		ReturnNodeMessage r = (ReturnNodeMessage) MessageCracker.crack(data, data.length);
		System.out.println("key: "+r.kid.hashCode());
		System.out.println("msgid: "+r.msgid);
		System.out.println("origin: "+r.to);
		int j = 0;
		while (j < r.getIdList().size()) {
			System.out.println("["+j+"]: "+r.getIdList().get(j).hashCode());
			j++;
		}
	}
}
