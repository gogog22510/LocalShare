package gogog22510.dht.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	private static volatile Logger INSTANCE = null;
	public static Logger getInstance() {
		Logger localRef = INSTANCE;
		if (localRef == null) {
			synchronized (Logger.class) {
				localRef = INSTANCE;
				if (localRef == null) {
					INSTANCE = localRef = new Logger();
				}
			}
		}
		return localRef;
	}

	public final static String TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";

	private Logger() {}

	public static String createSenderString(Object sender) {
		if(sender!=null && (sender instanceof Class)) return String.valueOf(sender);
		String className;
		if (sender != null) {
			Class<?> klazz = sender.getClass();
			Class<?> parent = klazz;
			while (klazz != null) {
				parent = klazz;
				klazz = klazz.getDeclaringClass();
			}
			className = parent.getName();
		} else {
			className = "";
		}
		return (sender != null ? className : "");
	}

	public void info(Object sender, Object message) {
		log("INFO", sender, message);
	}

	public void error(Object sender, Object message) {
		if(message instanceof Throwable) {
			error(this, "", (Throwable)message);
		}
		else {
			log("ERROR", sender, message);
		}
	}

	public void error(Object sender, Object message, Throwable exception) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
			exception.printStackTrace(ps);
			String data = new String(baos.toByteArray(), "UTF-8");
			log("ERROR", sender, message+" - "+data);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void log(String level, Object sender, Object message) {
		String msg = message.toString();
		String formatString = "[%s] %s [%s] %s - %s";
		String dateFormatPattern = Logger.TIMESTAMP;
		SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
		String senderString = createSenderString(sender);
		String line = String.format(
						formatString,
						level,
						dateFormat.format(new Date()),
						Thread.currentThread().getName(),
						senderString,
						msg);
		System.out.println(line);
	}
}
