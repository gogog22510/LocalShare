package gogog22510.dht.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Peer client for P2P network
 * @author charles
 *
 */
public class PeerClient {
	public static final int OPT_SEARCH = 0;
	public static final int OPT_DOWNLOAD = 1;
	public static final int OPT_UPLOAD = 2;

	/**
	 *  do search operation, return file found status
	 *  @param filePath file path
	 *  @param dIn peer dIn
	 *  @param dOut peer dOut
	 *  @return true if file exist in peer
	 */
	public static boolean doSearch(String filePath, DataInputStream dIn, DataOutputStream dOut) throws IOException {
		// search option
		dOut.writeInt(OPT_SEARCH);
		dOut.flush();
		// send file path
		dOut.writeUTF(filePath);
		dOut.flush();

		// read boolean
		boolean exist = dIn.readBoolean();
		return exist;
	}

	/**
	 *  do download operation, the dOut will be ready to receive file after this call
	 *  @param filePath file path
	 *  @param dIn peer dIn
	 *  @param dOut peer dOut
	 *  @return file size
	 */
	public static long doDownload(String filePath, DataInputStream dIn, DataOutputStream dOut) throws IOException {
		// download option
		dOut.writeInt(OPT_DOWNLOAD);
		dOut.flush();
		// send file path
		dOut.writeUTF(filePath);
		dOut.flush();

		// receive file
		long fileSize = dIn.readLong();
		return fileSize;
	}

	/**
	 * do send operation, the dOut will be ready to send file after this call
	 * @param filePath file path
	 * @param dIn peer dIn
	 * @param dOut peer dOut
	 * @throws IOException
	 */
	public static void doSend(String filePath, DataInputStream dIn, DataOutputStream dOut) throws IOException {
		// send option
		dOut.writeInt(OPT_UPLOAD);
		dOut.flush();
		// send file path
		dOut.writeUTF(filePath);
		dOut.flush();
	}
}
