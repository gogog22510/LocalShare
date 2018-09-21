package gogog22510.dht.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import gogog22510.dht.network.NetworkAdapter;
import gogog22510.dht.util.Logger;

/**
 *** IMPORTANT: DistributedOperation implementation must be STATELESS ***
 *
 * Generic Operation for P2P Network
 *
 * @author charles
 *
 */
public abstract class DistributedOperation {
	// status code
	public static final int NEW 	= 0;
	public static final int SUCCESS = 1;
	public static final int ERROR 	= 2;
	
	private int status;
	protected String searchKey = null;
	protected DistributedOperation(String filePath) {
		this.status = NEW;
		setSearchKey(filePath);
	}

	public void setSearchKey(String k) {
		this.searchKey = k;
	}

	// return operation status
	public boolean isNew() {
		return isStatus(NEW);
	}

	public boolean isSuccess() {
		return isStatus(SUCCESS);
	}

	public boolean isError() {
		return isStatus(ERROR);
	}

	private boolean isStatus(int code) {
		return this.status == code;
	}

	// need to call this method to mark the operation success
	protected void setStatus(int code) {
		this.status = code;
	}

	public String[] getPeerList(String searchKey) {
		return PeerCache.getInstance().getPeerList(searchKey);
	}

	// start the operation
	public Object start() throws IOException {
		String[] peerList = getPeerList(searchKey);
		if(peerList != null && peerList.length > 0) {
			int rs = (int) (Math.random() * peerList.length);
			for(int i=0; i<peerList.length; i++) {
				String peerip = peerList[(i + rs) % peerList.length];
				Socket socket = null;
				DataOutputStream dOut = null;
				DataInputStream dIn = null;
				Object obj = null;
				try {
					socket = NetworkAdapter.getInstance().openTCPSocket(peerip);
					dOut = new DataOutputStream(socket.getOutputStream());
					dIn = new DataInputStream(socket.getInputStream());

					// do operation
					obj = doOperation(socket, dIn, dOut);
					if(isSuccess()) {
						return obj;
					}
				} catch (IOException e) {
					// socket error
					Logger.getInstance().error(this, "Operation failed on peer"+i+". Try the next one...");
				} finally {
					if(dIn != null) {
						try {
							dIn.close();
						} catch (IOException e1) {
						}
					}
					if(dOut != null) {
						try {
							dOut.close();
						} catch (IOException e1) {
						}
					}
					if(socket != null) {
						try {
							socket.close();
						} catch (IOException e1) {
						}
					}
				}
			}
		}
		setStatus(SUCCESS);
		return null;
	}

	/**
	 * This method will be called when iterate through each peer
	 * @param socket peer socket
	 * @param dIn peer inputstream
	 * @param dOut peer outpust stream
	 * @return result object
	 * @throws IOException when there is a network issue
	 */
	protected abstract Object doOperation(Socket socket, DataInputStream dIn, DataOutputStream dOut) throws IOException;
}
