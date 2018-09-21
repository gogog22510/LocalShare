package gogog22510.dht.core;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import gogog22510.dht.message.FindNodeMessage;
import gogog22510.dht.message.LookupIpMessage;
import gogog22510.dht.message.Message;
import gogog22510.dht.message.ReturnIpMessage;
import gogog22510.dht.message.ReturnNodeIpMessage;
import gogog22510.dht.message.ReturnNodeMessage;
import gogog22510.dht.message.ReturnValueIpMessage;
import gogog22510.dht.message.ReturnValueMessage;
import gogog22510.dht.network.MessageSender;
import gogog22510.dht.util.Logger;

/**
 * Find value (IP) in DHT for search key
 * @author charles
 *
 */
public class DHTFindValueOperation {
	private Logger logger;

	private DistributedHashTable dht;
	private KademliaRoutingTable routingTable;
	private MessageSender sender;

	private String requestId;
	private String key;
	private KademliaId searchKey;
	private Set<String> result;
	private HashSet<KademliaId> unackSet, dicovered;
	private SendMessageWithReply sendMessageWithReply;
	public DHTFindValueOperation(String key, DistributedHashTable dht, KademliaRoutingTable routingTable, MessageSender sender) throws NoSuchAlgorithmException {
		setKey(key);
		setDHT(dht);
		setRoutingTable(routingTable);
		setMessageSender(sender);
		this.requestId = UUID.randomUUID().toString();
		this.result = new HashSet<String>();
		this.unackSet = new HashSet<KademliaId>();
		this.dicovered = new HashSet<KademliaId>();
		this.sendMessageWithReply = new SendMessageWithReply(sender);
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void setKey(String key) throws NoSuchAlgorithmException {
		this.key = key;
		this.searchKey = new KademliaId(key);
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

	public Set<String> execute() throws IOException, NoSuchAlgorithmException, InterruptedException {
		logger.info(this, "("+requestId+") start findValue for "+key);
		long start = System.currentTimeMillis();
		try {
			// do find node operation by search key
			logger.info(this, "("+requestId+") create searchKey["+searchKey.hashCode()+"] for "+key);

			// start from closest node
			dicovered.add(routingTable.getLocalKid());
			List<KademliaId> closest = routingTable.getClosestNode(searchKey);
			int try_size = Math.min(closest.size(), KademliaUtil.getAlphaSize());
			logger.info(this, "("+requestId+") ready to send find Node request to "+try_size+" peers");

			// ask closest node first
			for(int i=0; i<try_size; i++) {
				unackSet.add(closest.get(i));
				dicovered.add(closest.get(i));
				FindNodeMessage msg = new FindNodeMessage(searchKey);
				msg.setSearchKey(key);
				String ip = routingTable.getIp(closest.get(i));
				if(ip != null) {
					sendMessageWithReply.execute(msg, InetAddress.getByName(ip));
					logger.info(this, "\tsend find value message to ["+closest.get(i).hashCode()+"] "+ip);
				}
			}

			// keep process until over try size, time out or all acked
			boolean getValue = false;
			long startTime = System.currentTimeMillis();
			while (unackSet.size() > 0 && (System.currentTimeMillis() - startTime) < KademliaUtil.getKRPCTimeout()) {

				Message rsp = sendMessageWithReply.waitForResponse();
				if(rsp == null) {
					// wait timeout, if value already return, break the loop
					if(getValue) break;
					else continue;
				}

				KademliaId sendNode = rsp.kid;
				unackSet.remove(sendNode); // remove from unack set

				if(rsp instanceof ReturnValueMessage) {
					getValue = onReturnValueMessage((ReturnValueMessage)rsp);
				}
				else if(rsp instanceof ReturnValueIpMessage) {
					getValue = onReturnValueIpMessage((ReturnValueIpMessage)rsp);
				}
				else if(rsp instanceof ReturnNodeMessage) {
					onReturnNodeMessage((ReturnNodeMessage)rsp);
				}
				else if(rsp instanceof ReturnNodeIpMessage) {
					onReturnNodeIpMessage((ReturnNodeIpMessage)rsp);
				}
			}
		} finally {
			sendMessageWithReply.cancel();
			// remove unacked node temporarily, if they are good they will be back again
			for(KademliaId id:unackSet) {
				routingTable.removeNode(id);
				logger.info(this, "("+requestId+") remove unacked peer "+id.hashCode());
			}
			logger.info(this, "("+requestId+") request took "+(System.currentTimeMillis()-start)+" ms");
		}
		return result;
	}

	private String lookupIp(KademliaId unknownKid, InetAddress ip) throws InterruptedException, IOException {
		String lookupIp = null;
		SendMessageWithReply s = new SendMessageWithReply(sender);
		s.execute(new LookupIpMessage(unknownKid), ip);
		Message rsp1 = s.waitForResponse();
		if(rsp1 instanceof ReturnIpMessage) {
			ReturnIpMessage rip = (ReturnIpMessage)rsp1;
			lookupIp = rip.getLookupIp();
		}
		s.cancel();
		// add unknown ip back to routing table
		if(lookupIp != null) {
			routingTable.addToRoutingTable(unknownKid, lookupIp);
		}
		return lookupIp;
	}

	private boolean onReturnValueMessage(ReturnValueMessage msg) throws InterruptedException, IOException {
		// if receive find value message, add to result set
		logger.info(this, "("+requestId+") receive return value message");
		boolean getValue = false;
		List<KademliaId> klist = msg.getIdList(); // return peer's kid
		for(KademliaId k:klist) {
			String ip = routingTable.getIp(k); // convert peer's kid to ip
			if(ip == null) {
				ip = lookupIp(k, msg.ip);
			}
			if(ip != null) {
				getValue = true;
				result.add(ip);
				// add to local storage
				dht.putLocal(key, ip);
				logger.info(this, "("+requestId+") find value for "+key+" from "+msg.ip);
			}
		}
		return getValue;
	}

	private boolean onReturnValueIpMessage(ReturnValueIpMessage msg) throws InterruptedException, IOException, NoSuchAlgorithmException {
		// if receive find value message, add to result set
		logger.info(this, "("+requestId+") receive return value ip message");
		boolean getValue = false;
		List<String> iplist = msg.getIpList(); // return peer's kid
		for(String ip:iplist) {
			if(!routingTable.contains(ip)) {
				routingTable.addToRoutingTable(ip);
			}
			if(ip != null) {
				getValue = true;
				result.add(ip);
				// add to local storage
				dht.putLocal(key, ip);
				logger.info(this, "("+requestId+") find value for "+key+" from "+msg.ip);
			}
		}
		return getValue;
	}

	private void onReturnNodeMessage(ReturnNodeMessage msg) throws IOException, InterruptedException {
		// if receive find node message, send find message again
		logger.info(this, "("+requestId+") receive return node message from "+msg.ip);
		List<KademliaId> klist = msg.getIdList();
		for(KademliaId k:klist) {
			if(!dicovered.contains(k)) {
				dicovered.add(k);
				String ip = routingTable.getIp(k);
				if(ip == null) {
					ip = lookupIp(k, msg.ip);
				}
				if(ip != null) {
					FindNodeMessage fnm = new FindNodeMessage(searchKey);
					sendMessageWithReply.execute(fnm, InetAddress.getByName(ip));
					unackSet.add(k);
					logger.info(this, "("+requestId+") send search to ["+k.hashCode()+"] "+ip);
				}
			}
		}
	}

	private void onReturnNodeIpMessage(ReturnNodeIpMessage msg) throws IOException, InterruptedException, NoSuchAlgorithmException {
		// if receive find node message, send find message again
		logger.info(this, "("+requestId+") receive return node ip message from "+msg.ip);
		List<String> iplist = msg.getIpList();
		for(String ip:iplist) {
			if(!routingTable.contains(ip)) {
				routingTable.addToRoutingTable(ip);
			}
			KademliaId k = routingTable.getKid(ip);
			if(!dicovered.contains(k)) {
				dicovered.add(k);
				if(ip != null) {
					FindNodeMessage fn = new FindNodeMessage(searchKey);
					sendMessageWithReply.execute(fn, InetAddress.getByName(ip));
					unackSet.add(k);
				}
				logger.info(this, "("+requestId+") send search to ["+k.hashCode()+"] "+ip);
			}
		}
	}
}
