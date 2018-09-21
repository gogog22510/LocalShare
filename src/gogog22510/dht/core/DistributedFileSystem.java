package gogog22510.dht.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import gogog22510.dht.network.NetworkAdapter;
import gogog22510.dht.network.PeerUtil;
import gogog22510.dht.util.Logger;

/**
 * Distributed file system based on P2P network
 * @author charles
 *
 */
public class DistributedFileSystem {
	private static File baseFolder = null;
	/*
	 * This method need to be called before the shared instance be created
	 */
	public static void setBaseFolder(File folder) {
		synchronized (DistributedFileSystem.class) {
			baseFolder = folder;
		}
	}

	private static DistributedFileSystem INSTANCE = null;
	public static DistributedFileSystem getInstance() {
		if (INSTANCE == null) {
			synchronized (DistributedFileSystem.class) {
				if (INSTANCE == null) {
					if(baseFolder == null) {
						baseFolder = Settings.getInstance().getBaseFolder();
					}
					INSTANCE = new DistributedFileSystem();
				}
			}
		}
		return INSTANCE;
	}

	private DistributedFileSystem() {
		init();
	}

	private void init() {
		// initialize network adapter
		NetworkAdapter.getInstance().startTCP();

		// init peer cache
		PeerCache.getInstance();
	}

	// distributed network operation

	// search a file in network, return file found status
	public boolean search(String filePath) throws IOException {
		SearchFileOperation opt = new SearchFileOperation(filePath);
		Object obj = opt.start();
		if(opt.isSuccess()) {
			if(obj != null) {
				return ((Boolean)obj).booleanValue();
			} else {
				Logger.getInstance().info(this, "Cannot find "+filePath+" from network.");
				return false;
			}
		}
		else {
			Logger.getInstance().error(this, "Search file from network failed "+filePath);
		}
		return false;
	}

	public File download(String filePath) throws IOException {
		return download(filePath, null);
	}
	// search and download file from network
	public File download(String filePath, File targetFile) throws IOException {
		Logger.getInstance().info(this, "Start download operation from network for "+filePath+" ...");
		DownloadFileOperation opt = new DownloadFileOperation(filePath, targetFile);
		Object obj = opt.start();
		if(opt.isSuccess()) {
			if(obj != null) {
				return (File)obj;
			} else {
				Logger.getInstance().info(this, "Cannot find "+filePath+" from network.");
				return null;
			}
		}
		else {
			Logger.getInstance().error(this, "Download file from network failed "+filePath);
		}
		return null;
	}

	public boolean upload(String filePath, String ip) throws IOException {
		return upload(filePath, null, ip);
	}
	// upload file to network
	public boolean upload(String filePath, File targetFile, String ip) throws IOException {
		Logger.getInstance().info(this, "Start upload operation to "+ip+" for "+filePath+" ...");
		UploadFileOperation opt = new UploadFileOperation(filePath, targetFile, ip);
		Object obj = opt.start();
		if(opt.isSuccess()) {
			if(obj != null) {
				return (Boolean)obj;
			} else {
				Logger.getInstance().info(this, "Cannot upload "+filePath+" to network.");
				return false;
			}
		}
		else {
			Logger.getInstance().error(this, "Upload file from network failed "+filePath);
		}
		return false;
	}

	/**
	 * search the file
	 * @param filePath file path
	 * @return true if exist
	 */
	public static boolean searchLocal(String filePath) {
		File file = new File(baseFolder, filePath);
		return file.exists();
	}

	/**
	 * send file to peer
	 * @param filePath file path
	 * @param dOut peer output
	 * @return true if success
	 * @throws IOException any network issue
	 */
	public static boolean sendFile(String filePath, DataOutputStream dOut) throws IOException {
		File file = new File(baseFolder, filePath);
		return sendFile(file, dOut);
	}
	public static boolean sendFile(File file, DataOutputStream dOut) throws IOException {
		InputStream in = new FileInputStream(file);
		long fileSize = file.length();
		//System.out.println("trying");
		dOut.writeLong(fileSize);
		dOut.flush();
		//System.out.println("fileSize writen");

		PeerUtil.copy(in, dOut, fileSize);
		in.close();
		return true;
	}

	public static File receiveFile(String filePath, long fileSize, DataInputStream dIn) throws IOException {
		return receiveFile(filePath, fileSize, dIn, getFile(filePath));
	}
	/**
	 * receive file from peer
	 * @param filePath file path
	 * @param fileSize expected file size
	 * @param dIn peer input
	 * @param targetFile target download file
	 * @return target file, NULL if failed
	 * @throws IOException any network issue
	 */
	public static File receiveFile(String filePath, long fileSize, DataInputStream dIn, File targetFile) throws IOException {
		if(targetFile == null) {
			targetFile = getFile(filePath);
		}
		else {
			ensureFolder(targetFile);
		}
		File f = targetFile;
		File partFile = new File(f.getAbsolutePath() + ".part");
		if(partFile.exists()) {
			partFile.delete();
		}

		// download to part file
		OutputStream out = new FileOutputStream(partFile);
		PeerUtil.copy(dIn, out, fileSize);
		out.close();

		// validate and move part file to target
		if(partFile.length() != fileSize) {
			Logger.getInstance().info(DistributedFileSystem.class, "Receive file size != "+filePath+". Delete part file.");
			partFile.delete();
			return null;
		}

		if (f.exists()) {
			if (!f.delete()) {
				Logger.getInstance().error(DistributedFileSystem.class, "Unable to replace "+f.getAbsolutePath());
				return null;
			}
		}

		partFile.renameTo(f);
		return f;
	}

	// ensure parent folders
	private static void ensureFolder(File targetFile) {
		File f = targetFile;
		File folder = f.getParentFile();
		if(folder != null && !folder.exists()) {
			folder.mkdirs();
		}
	}

	// get file and create parent folders
	private static File getFile(String filePath) {
		File f = new File(baseFolder, filePath);
		File folder = f.getParentFile();
		if(folder != null && !folder.exists()) {
			folder.mkdirs();
		}
		return f;
	}

	public static void main(String[] args) throws Exception {
		getInstance();
		Scanner scanner = new Scanner(System.in);
		int option = 0;
		String key = null;
		while (true) {
			System.out.println("\n\nSelect the option:");
			System.out.println("\t0 - Search");
			System.out.println("\t1 - Download");
			System.out.println("\t99 - Exit");
			try {
				option = scanner.nextInt();
				if (option == 0) {
					System.out.print("File Path: ");
					key = scanner.next();
					
					boolean exist = getInstance().search(key);
					System.out.println(exist);
				}
				else if(option == 1) {
					System.out.print("File Path: ");
					key = scanner.next();

					File f = getInstance().download(key);
					if(f != null) {
						System.out.println(f.getAbsolutePath());
					}
					else {
						System.out.println("Download Failed.");
					}
				}
				else if(option == 99) {
					break;
				}
			} catch (Exception e) {
				// e.printStackTrace();
				System.out.println("Oops, something went wrong.");
				scanner = new Scanner(System.in);
				// break;
			}
		}
		scanner.close();
	}
}
