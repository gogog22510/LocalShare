package gogog22510.dht.util;

public class ThreadingUtilities {
	public static boolean interruptibleSleep(long duration) {
		boolean interrupted = false;
		try {
			Thread.sleep(duration);
		} 
		catch (InterruptedException e) {
			System.err.println("sleep interrupted");
			interrupted = true;
		}
		return interrupted;
	}

	public static void uninterruptibleSleep(long duration) {
		long remainingDuration = duration;
		while(true) {
			long start = System.currentTimeMillis();
			try {
				Thread.sleep(remainingDuration);
				break;
			}
			catch(InterruptedException e) {
				long end = System.currentTimeMillis();
				remainingDuration-=(end - start);
				System.out.println("remaining duration: " + remainingDuration);
				if(remainingDuration<=0) break;
			}
		}
	}

	public static boolean waitIndefinitely() {
		final Object lock = new Object();
		boolean interrupted = false;
		synchronized(lock) {
			try {
				lock.wait();
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
				interrupted = true;
			}
		}
		return interrupted;
	}
}
