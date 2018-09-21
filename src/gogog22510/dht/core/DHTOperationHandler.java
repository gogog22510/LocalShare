package gogog22510.dht.core;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import gogog22510.dht.message.FindNodeMessage;
import gogog22510.dht.message.LookupIpMessage;
import gogog22510.dht.message.Message;
import gogog22510.dht.message.ReturnIpMessage;
import gogog22510.dht.message.ReturnNodeIpMessage;
import gogog22510.dht.message.ReturnValueIpMessage;
import gogog22510.dht.message.StoreValueMessage;
import gogog22510.dht.network.MessageSender;
import gogog22510.dht.network.MessageSenderListener;
import gogog22510.dht.util.Logger;

/**
 * DHT operation message handler
 * @author charles
 *
 */
public class DHTOperationHandler implements MessageSenderListener {
	private Logger logger;
	private KademliaRoutingTable routingTable;
	private DistributedHashTable dht;
	private MessageSender sender;

	public DHTOperationHandler(DistributedHashTable dht, KademliaRoutingTable routingTable, MessageSender sender) {
		setDHT(dht);
		setRoutingTable(routingTable);
		setMessageSender(sender);
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void setRoutingTable(KademliaRoutingTable routingTable) {
		this.routingTable = routingTable;
	}

	public void setDHT(DistributedHashTable dht) {
		this.dht = dht;
	}

	public void setMessageSender(MessageSender sender) {
		this.sender = sender;
	}

	@Override
	public void onNoneResponseMessage(Message msg) {
		if(msg instanceof StoreValueMessage) {
			StoreValueMessage sm = (StoreValueMessage) msg;
			onStoreValueMessage(sm);
		}
		else if(msg instanceof FindNodeMessage) {
			FindNodeMessage fm = (FindNodeMessage) msg;
			onFindNodeMessage(fm);
		}
		else if(msg instanceof LookupIpMessage) {
			LookupIpMessage lm = (LookupIpMessage) msg;
			onLookupIpMessage(lm);
		}
	}

	private void onStoreValueMessage(StoreValueMessage msg) {
		String[] parts = msg.getParts();
		if(parts.length == 2) {
			String key = parts[0];
			String ip = parts[1];
			if(ip == null) {
				routingTable.addToRoutingTable(msg.kid, msg.ip.getHostAddress());
			}
			dht.putLocal(key, ip);
			logger.info(this, "receive save value "+key+", "+ip);
		}
		else {
			logger.info(this, "receive save message with incorrect data format");
		}
	}

	private void onFindNodeMessage(FindNodeMessage msg) {
		KademliaId findId = msg.kid;
		String searchKey = msg.getSearchKey();
		logger.info(this, "receive find node for "+searchKey);
		if(dht.getTable().containsKey(searchKey)) {
			// get ip list and convert to kid list
			Set<String> s = dht.getTable().get(searchKey);
			String[] ips = null;
			synchronized (s) {
				ips = s.toArray(new String[s.size()]);
			}
			logger.info(this, "find in local table size = "+ips.length);

			// send result back
			/*ReturnValueMessage rvm = new ReturnValueMessage(msg.msgid, routingTable.getLocalKid());
			for(String ip:ips) {
				KademliaId k = routingTable.getKid(ip);
				if(k != null) {
					rvm.addId(k);
				}
			}*/
			ReturnValueIpMessage rvm = new ReturnValueIpMessage(msg.msgid, routingTable.getLocalKid());
			int try_size = Math.min(ips.length, KademliaUtil.getAlphaSize());
			for(int i=0; i<try_size; i++) {
				rvm.addIp(ips[i]);
			}
			try {
				sender.sendMessage(rvm, msg.ip);
			} catch (IOException e) {
			}
		}
		else {
			// find closest node list
			List<KademliaId> closest = routingTable.getClosestNode(findId);
			/*ReturnNodeMessage rnm = new ReturnNodeMessage(fm.msgid, routingTable.getLocalKid());
			int try_size = Math.min(closest.size(), KademliaUtil.ALPHA_SIZE);
			for(int i=0; i<try_size; i++) {
				rnm.addId(closest.get(i));
			}
			logger.info(this, "cannot find it local table, return closest size = "+try_size);
			try {
				sender.sendMessage(rnm, fm.ip);
			} catch (IOException e) {
			}*/
			ReturnNodeIpMessage rnm = new ReturnNodeIpMessage(msg.msgid, routingTable.getLocalKid());
			int try_size = Math.min(closest.size(), KademliaUtil.getAlphaSize());
			for(int i=0; i<try_size; i++) {
				rnm.addIp(routingTable.getIp(closest.get(i)));
			}
			logger.info(this, "cannot find it local table, return closest size = "+try_size);
			try {
				sender.sendMessage(rnm, msg.ip);
			} catch (IOException e) {
			}
		}
	}

	private void onLookupIpMessage(LookupIpMessage msg) {
		KademliaId lkId = msg.getLookupKey();
		String ip = routingTable.getIp(lkId);
		ReturnIpMessage rim = new ReturnIpMessage(msg.msgid, lkId);
		rim.setLookupIp(ip);
		try {
			sender.sendMessage(rim, msg.ip);
		} catch (IOException e) {
		}
	}
}
