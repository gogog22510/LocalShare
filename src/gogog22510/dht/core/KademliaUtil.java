package gogog22510.dht.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import gogog22510.dht.message.Message;

/**
 * Utility method for Kademlia
 * @author charles
 *
 */
public class KademliaUtil {
	// max process size
	private static int K_SIZE = 5;
	private static int ALPHA_SIZE = 5;
	private static long RPC_WAIT = 500; // 0.5 seconds
	private static long RPC_TIME_OUT = 5000; // 5 seconds
	private static long REPUBLISH_INTERVAL = 30 * 60 * 1000; // 0.5 hr
	private static long REFRESH_CHECK = 10 * 60 * 1000; // 10 mins

	/*
	 * init kademlia parameters from config file
	 */
	public static void init(Settings config) {
		K_SIZE = config.getInt("kademlia.k", 5);
		ALPHA_SIZE = config.getInt("kademlia.alpha", 5);
		RPC_WAIT = config.getInt("kademlia.rpcwait", 500); // 0.5 seconds
		RPC_TIME_OUT = config.getInt("kademlia.rpctimeout", 5000); // 5 seconds
		REPUBLISH_INTERVAL = config.getInt("kademlia.republish", 30 * 60 * 1000); // 0.5 hr
		REFRESH_CHECK = config.getInt("kademlia.refresh", 10 * 60 * 1000); // 10 mins
	}

	public static int getKSize() {
		return K_SIZE;
	}

	public static int getAlphaSize() {
		return ALPHA_SIZE;
	}

	public static long getRPCWait() {
		return RPC_WAIT;
	}

	public static long getKRPCTimeout() {
		return RPC_TIME_OUT;
	}

	public static long getDataRepublishInterval() {
		return REPUBLISH_INTERVAL;
	}

	public static long getDataRefreshCheckInterval() {
		return REFRESH_CHECK;
	}

	/**
	 * Computes the SHA-1 Hash.
	 *
	 * @param toHash The string to hash
	 *
	 * @return byte[20] The hashed string
	 *
	 * @throws java.security.NoSuchAlgorithmException
	 */
	public static byte[] sha1Hash(String toHash) throws NoSuchAlgorithmException
	{
		/* Create a MessageDigest */
		MessageDigest md = MessageDigest.getInstance("SHA-1");

		/* Add password bytes to digest */
		md.update(toHash.getBytes());

		/* Get the hashed bytes */
		return md.digest();
	}

	/**
	 * Computes the SHA-1 Hash using a Salt.
	 *
	 * @param toHash The string to hash
	 * @param salt   A salt used to blind the hash
	 *
	 * @return byte[20] The hashed string
	 *
	 * @throws java.security.NoSuchAlgorithmException
	 */
	public static byte[] sha1Hash(String toHash, String salt) throws NoSuchAlgorithmException
	{
		/* Create a MessageDigest */
		MessageDigest md = MessageDigest.getInstance("SHA-1");

		/* Add password bytes to digest */
		md.update(toHash.getBytes());

		/* Get the hashed bytes */
		return md.digest(salt.getBytes());
	}

	/**
	 * Computes the MD5 Hash.
	 *
	 * @param toHash The string to hash
	 *
	 * @return byte[16] The hashed string
	 *
	 * @throws java.security.NoSuchAlgorithmException
	 */
	public static byte[] md5Hash(String toHash) throws NoSuchAlgorithmException
	{
		/* Create a MessageDigest */
		MessageDigest md = MessageDigest.getInstance("MD5");

		/* Add password bytes to digest */
		md.update(toHash.getBytes());

		/* Get the hashed bytes */
		return md.digest();
	}

	/**
	 * Computes the MD5 Hash using a salt.
	 *
	 * @param toHash The string to hash
	 * @param salt   A salt used to blind the hash
	 *
	 * @return byte[16] The hashed string
	 *
	 * @throws java.security.NoSuchAlgorithmException
	 */
	public static byte[] md5Hash(String toHash, String salt) throws NoSuchAlgorithmException
	{
		/* Create a MessageDigest */
		MessageDigest md = MessageDigest.getInstance("MD5");

		/* Add password bytes to digest */
		md.update(toHash.getBytes());

		/* Get the hashed bytes */
		return md.digest(salt.getBytes());
	}

	// construct broadcast message
	public static byte[] toMessage(int cmd, byte[] buf, byte[] kid, int msgid) {
		// message structure:
		//      command         kid        msgid
		// | 4 bytes (int) |  20 bytes |   4 bytes   |
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(cmd);
		bb.put(kid);
		bb.putInt(msgid);
		return bb.array();
	}

	/**
	 * write message full header with msgid
	 * @param out
	 * @param cmd
	 * @param kid
	 * @param msgid
	 * @param origin
	 * @throws IOException
	 */
	public static void writeMessageHeader(OutputStream out, int cmd, byte[] kid, int msgid, String to) throws IOException {
		// message full header:
		//      command         kid        msgid         to len         to
		// | 4 bytes (int) |  20 bytes |   4 bytes   |   4 bytes   |  n bytes  |
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeInt(cmd);
		dout.write(kid);
		dout.writeInt(msgid);
		
		// write to
		int tolen = 0;
		if(to != null && !to.isEmpty()) {
			ByteArrayOutputStream d1 = new ByteArrayOutputStream();
			new DataOutputStream(d1).writeUTF(to);
			byte[] tod = d1.toByteArray();
			dout.writeInt(tod.length);
			dout.write(tod);
			tolen = tod.length;
		}
		else {
			dout.writeInt(tolen);
		}
		
		int rest = Message.HEADER_LENGTH - 32 - tolen;
		for(int i=0; i<rest; i++) dout.writeByte(0);
		dout.flush();
	}

	// get cmd from message
	public static int getCmdFromMessage(byte[] msg) {
		return ByteBuffer.wrap(msg, 0, 4).getInt();
	}

	// get uuid from message
	public static KademliaId getKademliaIdFromMessage(byte[] msg) {
		return new KademliaId(Arrays.copyOfRange(msg, 4, 24));
	}

	// get msgid from message
	public static int getMsgIDFromMessage(byte[] msg) {
		return ByteBuffer.wrap(msg, 24, 4).getInt();
	}

	// get to from message
	public static int getToLengthFromMessage(byte[] msg) {
		return ByteBuffer.wrap(msg, 28, 4).getInt();
	}
	// get to from message
	public static String getToFromMessage(byte[] msg) {
		int len = getToLengthFromMessage(msg);
		String to = null;
		if(len > 0) {
			DataInputStream din = new DataInputStream(new ByteArrayInputStream(Arrays.copyOfRange(msg, 32, 32+len)));
			try {
				to = din.readUTF();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return to;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException {
		String key = "Hello world";
		System.out.println(key);
		KademliaId id = new KademliaId(key);
		byte[] bytes = id.getBytes();
		System.out.println("bytes size: "+bytes.length);
		System.out.println(id.hashCode());

		// test xor
		System.out.println("distance: "+id.getDistance(id));
	}
}
