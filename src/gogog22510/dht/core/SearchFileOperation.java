package gogog22510.dht.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import gogog22510.dht.network.PeerClient;

/**
 * Search a file in P2P network
 * @author charles
 *
 */
public class SearchFileOperation extends DistributedOperation {
	private String filePath;
	public SearchFileOperation(String filePath) {
		super(filePath);
		this.filePath = filePath;
	}

	@Override
	protected Object doOperation(Socket socket, DataInputStream dIn, DataOutputStream dOut) throws IOException {
		// search option
		boolean exist = PeerClient.doSearch(filePath, dIn, dOut);
		if(exist) {
			setStatus(SUCCESS);
			return exist;
		}
		return null;
	}

}
