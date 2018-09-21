package gogog22510.dht.core;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import gogog22510.dht.message.Message;
import gogog22510.dht.network.MessageSender;

/**
 * Send UDP message with reply
 * @author charles
 *
 */
public class SendMessageWithReply {
	private MessageSender sender;
	private BlockingQueue<Message> replyQueue;
	private List<Integer> unregisterId;

	public SendMessageWithReply(MessageSender sender) {
		this(sender, new LinkedBlockingQueue<Message>());
	}

	public SendMessageWithReply(MessageSender sender, BlockingQueue<Message> replyQueue) {
		this.sender = sender;
		this.replyQueue = new LinkedBlockingQueue<Message>();
		this.unregisterId = new ArrayList<Integer>();
	}

	public void execute(Message msg, InetAddress ip) throws IOException {
		sender.sendMessageWithReply(msg, replyQueue, ip);
		this.unregisterId.add(msg.msgid);
	}

	/**
	 * wait and get the response
	 * @return null if timeout
	 * @throws InterruptedException 
	 */
	public Message waitForResponse() throws InterruptedException {
		return waitForResponse(KademliaUtil.getRPCWait());
	}

	/**
	 * wait and get the response
	 * @return null if timeout
	 * @throws InterruptedException 
	 */
	public Message waitForResponse(long timeout) throws InterruptedException {
		Message rsp = replyQueue.poll(timeout, TimeUnit.MILLISECONDS);
		return rsp;
	}

	public void cancel() {
		for(Integer i:unregisterId) {
			sender.unregister(i);
		}
	}
}
