package gogog22510.dht.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import gogog22510.dht.network.PeerClient;
import gogog22510.dht.util.Logger;

/**
 * Search and download a file in P2P network
 * @author charles
 *
 */
public class DownloadFileOperation extends DistributedOperation {
	private String filePath;
	private File targetFile;
	public DownloadFileOperation(String filePath, File targetFile) {
		super(filePath);
		this.filePath = filePath;
		this.targetFile = targetFile;
	}

	@Override
	protected Object doOperation(Socket socket, DataInputStream dIn, DataOutputStream dOut) throws IOException {
		// search option
		boolean exist = PeerClient.doSearch(filePath, dIn, dOut);
		if(exist) {
			long fileSize = PeerClient.doDownload(filePath, dIn, dOut);
			File f = DistributedFileSystem.receiveFile(filePath, fileSize, dIn, targetFile);
			if(f != null && f.length() == fileSize) {
				setStatus(SUCCESS);
				return f;
			}
			else {
				Logger.getInstance().error(this, "Receive file from network failed "+filePath);
				setStatus(SUCCESS);
				return null;
			}
		}
		return null;
	}

}
