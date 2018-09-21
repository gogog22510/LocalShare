package gogog22510.dht.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import gogog22510.dht.util.Logger;

public class Settings {
	private static volatile Settings INSTANCE = null;
	public static Settings getInstance() {
		Settings localRef = INSTANCE;
		if (localRef == null) {
			synchronized (Settings.class) {
				localRef = INSTANCE;
				if (localRef == null) {
					INSTANCE = localRef = new Settings();
				}
			}
		}
		return localRef;
	}

	private Properties prop;

	private Settings () {
		this.prop = new Properties();
		String configPath = System.getProperty("dht.config");
		if(configPath != null && !configPath.isEmpty()) {
			File configFile = new File(configPath);
			try (FileInputStream in = new FileInputStream(configFile)) {
				this.prop.load(in);
			} catch (Exception e) {
				Logger.getInstance().error(this, e);
			}
		}
	}

	public File getBaseFolder() {
		String path = this.prop.getProperty("basefolder", "C:/dht");
		File folder = new File(path);
		if(!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	public String getClientId() {
		String clientId = this.prop.getProperty("clientid", "testclient");
		return clientId;
	}

	public boolean useUDPUnicast() {
		String mode = getString("udp.mode", "unicast");
		return "unicast".equals(mode);
	}

	public int getInt(String key, int defaultValue) {
		String val = this.prop.getProperty(key);
		if(val != null && !val.isEmpty()) {
			return Integer.parseInt(val);
		}
		else {
			return defaultValue;
		}
	}

	public long getLong(String key, long defaultValue) {
		String val = this.prop.getProperty(key);
		if(val != null && !val.isEmpty()) {
			return Long.parseLong(val);
		}
		else {
			return defaultValue;
		}
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String val = this.prop.getProperty(key);
		if(val != null && !val.isEmpty()) {
			return Boolean.parseBoolean(val);
		}
		else {
			return defaultValue;
		}
	}

	public String getString(String key, String defaultValue) {
		String val = this.prop.getProperty(key);
		if(val != null && !val.isEmpty()) {
			return val;
		}
		else {
			return defaultValue;
		}
	}
}
