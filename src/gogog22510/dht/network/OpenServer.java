package gogog22510.dht.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import gogog22510.dht.core.DistributedFileSystem;
import gogog22510.dht.util.Logger;

class OpenServer extends Thread {

	private Socket socket;
	private PeerOperationListener listenerCaster = null;;

	public OpenServer(Socket socket) {
		this.socket = socket;
	}

	public void setListener(PeerOperationListener l) {
		this.listenerCaster = l;
	}

	private void notifyListners(int cmd, Object... params) {
		try {
			if(listenerCaster != null) {
				listenerCaster.onOperation(cmd, params);
			}
		} catch (Exception e) {
			// do nothing
		}
	}

	public void run() {
		DataInputStream dIn = null;
		DataOutputStream dout = null;
		try {
			dIn = new DataInputStream(socket.getInputStream());
			dout = new DataOutputStream(socket.getOutputStream());
			while (true) {
				String filePath;

				int option = dIn.readInt();
				switch (option) {
				case PeerClient.OPT_SEARCH:
					// search

					filePath = dIn.readUTF();
					Logger.getInstance().info(this, "receive search option "+filePath);
					boolean exist = DistributedFileSystem.searchLocal(filePath);
					dout.writeBoolean(exist);
					dout.flush();

					notifyListners(option, filePath);

					break;
				case PeerClient.OPT_DOWNLOAD:
					// send to peer

					filePath = dIn.readUTF();
					Logger.getInstance().info(this, "receive request download option "+filePath);
					DistributedFileSystem.sendFile(filePath, dout);

					notifyListners(option, filePath);

					break;
				case PeerClient.OPT_UPLOAD:
					// receive from peer

					filePath = dIn.readUTF();
					long fileSize = dIn.readLong();
					Logger.getInstance().info(this, "receive request upload option "+filePath);
					DistributedFileSystem.receiveFile(filePath, fileSize, dIn);

					notifyListners(option, filePath);

					break;
				default:
					throw new Exception();
				}
			}
		} catch (Exception e) {
			// do nothing
		} finally {
			if(dIn != null){
				try {
					dIn.close();
				} catch (IOException e) {
				}
			}
			if(dout != null){
				try {
					dout.close();
				} catch (IOException e) {
				}
			}
			if(socket != null){
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
