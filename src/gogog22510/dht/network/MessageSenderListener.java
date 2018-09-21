package gogog22510.dht.network;

import gogog22510.dht.message.Message;

public interface MessageSenderListener {
	public void onNoneResponseMessage(Message msg);
}
