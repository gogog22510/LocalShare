package gogog22510.dht.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import gogog22510.dht.util.Logger;

public class PeerUtil {

	public static ArrayList<String> listFilesForFolder(final File folder) {

		ArrayList<String> fileNames = new ArrayList<String>();

		for (final File fileEntry : folder.listFiles())
			fileNames.add(fileEntry.getName());

		return fileNames;

	}

	public static void copy(InputStream in, OutputStream out, long fileSize) throws IOException {
		byte[] buffer = new byte[1024];
		int count = 0, total = 0;
		int lastPercentage = 0;
		long start = System.currentTimeMillis();
		while (total != fileSize) {
//			Logger.getInstance().info("before read "+lastPercentage);
			count = in.read(buffer);
//			Logger.getInstance().info("after read "+lastPercentage);
			out.write(buffer, 0, count);
//			Logger.getInstance().info("after write "+lastPercentage);
			total += count;
			int newPercentage = Math.round(100 * ((float) total / fileSize));
			if ((newPercentage - lastPercentage) >= 10) {
				lastPercentage = newPercentage;
				Logger.getInstance().info(PeerUtil.class, "copying stream from peer to peer "
						+ newPercentage + "% complete [" + total + " of " + fileSize + "]");
			}
		}
		out.flush();
		Logger.getInstance().info(PeerUtil.class, "process took " + (System.currentTimeMillis() - start) + " ms.");
	}

	public static long toSeconds(long start, long end) {
		return (end - start) / 1000;
	}

	public static String getExternalIP() throws IOException {
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		URLConnection con = whatismyip.openConnection();
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String ip = in.readLine(); //you get the IP as a String
		return ip;
	}

	public static String getLocalIP() throws IOException {
		/*Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    Enumeration<InetAddress> ee = n.getInetAddresses();
		    while (ee.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) ee.nextElement();
		        if(i.getHostAddress().startsWith("192.168")) {
		        	return i.getHostAddress();
		        }
		    }
		}
		return null;*/
		String ip = null;
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress("google.com", 80));
			ip = socket.getLocalAddress().getHostAddress();
			socket.close();
		} catch (Exception e) {
			ip = getLocalIpFromInterface();
		}
		return ip;
	}

	public static String getLocalIpFromInterface() throws SocketException {
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    if (n.isLoopback() || !n.isUp() || !n.isVirtual()) {
				continue;
			}
		    Enumeration<InetAddress> ee = n.getInetAddresses();
		    while (ee.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) ee.nextElement();
		        if(i.getHostAddress().startsWith("192.168")) {
		        	return i.getHostAddress();
		        }
		    }
		}
		return null;
	}

	public static double calculateAverage(ArrayList<Long> times) {
		Long sum = 0L;
		if (!times.isEmpty()) {
			for (Long time : times) {
				sum += time;
			}
			return sum.doubleValue() / times.size();
		}
		return sum;
	}

	public static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
		List<InetAddress> broadcastList = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();

			if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
				continue;
			}

			for(InterfaceAddress ia:networkInterface.getInterfaceAddresses()) {
				InetAddress a = ia.getBroadcast();
				if(a != null) {
					System.out.println(networkInterface);
					broadcastList.add(a);
				}
			}
		}
		return broadcastList;
	}

	// serialize UUID to 16 bytes array
	public static byte[] getBytesFromUUID(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());

		return bb.array();
	}

	// convert 16 bytes array to UUID
	public static UUID getUUIDFromBytes(byte[] bytes, int offset, int length) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, offset, length);
		Long high = byteBuffer.getLong();
		Long low = byteBuffer.getLong();

		return new UUID(high, low);
	}

	// construct broadcast message
	public static byte[] toMessage(int cmd, byte[] buf, byte[] uuid, int msgid) {
		// message structure:
		//      command       UUID          msgid
		// | 4 bytes (int) |  16 bytes |   4 bytes   |
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(cmd);
		bb.put(uuid);
		bb.putInt(msgid);
		return bb.array();
	}

	// get cmd from message
	public static int getCmdFromMessage(byte[] msg) {
		return ByteBuffer.wrap(msg, 0, 4).getInt();
	}

	// get uuid from message
	public static UUID getUUIDFromMessage(byte[] msg) {
		return getUUIDFromBytes(msg, 4, 16);
	}

	// get msgid from message
	public static int getMsgIDFromMessage(byte[] msg) {
		return ByteBuffer.wrap(msg, 20, 4).getInt();
	}

	public static void main(String[] args) throws SocketException {
		UUID uuid = UUID.randomUUID();
		System.out.println(uuid.toString());
		byte[] bytes = getBytesFromUUID(uuid);
		System.out.println("bytes size: "+bytes.length);
		System.out.println(getUUIDFromBytes(bytes, 0, bytes.length).toString());

		// test broadcast
		byte[] buf = new byte[24];
		byte[] msg = toMessage(1, buf, bytes, -1);
		System.out.println("msg size: "+msg.length);
		System.out.println("cmd: "+getCmdFromMessage(msg));
		System.out.println("uuid: "+getUUIDFromMessage(msg));
		System.out.println("msgid: "+getMsgIDFromMessage(msg));

		// test network interface
		System.out.println(listAllBroadcastAddresses());
	}
}
