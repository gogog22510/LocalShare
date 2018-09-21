package gogog22510.dht.message;

import java.lang.reflect.Method;

import gogog22510.dht.core.KademliaUtil;
import gogog22510.dht.message.Message.MsgType;

public class MessageCracker {
	public static Message crack(byte[] data, int length) {
		MsgType type = MsgType.valueOf(KademliaUtil.getCmdFromMessage(data));
		if(type != null) {
			String classString = type.classpath();
			try {
				// decode message
				Class<?> msgClass = Class.forName(classString);
				Method method = msgClass.getMethod("parseFrom", byte[].class, int.class);
				Message msg = (Message) method.invoke(null, data, length);
				msg.setTo(KademliaUtil.getToFromMessage(data));
				msg.setIsResponse(type.isResponse());
				return msg;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
