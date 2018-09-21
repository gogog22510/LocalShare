package gogog22510.dht.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import gogog22510.dht.network.PeerClient;

/**
 * Upload a file to P2P network
 * @author charles
 *
 */
public class UploadFileOperation extends DistributedOperation {
	private String filePath;
	private File targetFile;
	private String ip;
	public UploadFileOperation(String filePath, File targetFile, String ip) {
		super(filePath);
		this.filePath = filePath;
		this.targetFile = targetFile;
		this.ip = ip;
	}

	@Override
	public String[] getPeerList(String searchKey) {
		return new String[] {ip};
	}

	@Override
	protected Object doOperation(Socket socket, DataInputStream dIn, DataOutputStream dOut) throws IOException {
		// send option
		PeerClient.doSend(filePath, dIn, dOut);
		if(targetFile == null) {
			DistributedFileSystem.sendFile(filePath, dOut);
			setStatus(SUCCESS);
		}
		else {
			DistributedFileSystem.sendFile(targetFile, dOut);
			setStatus(SUCCESS);
		}
		return true;
	}

}
