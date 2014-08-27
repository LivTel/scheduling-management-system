/**
 * 
 */
package ngat.sms.simulation;

import ngat.util.BooleanLock;

/**
 * @author eng
 * 
 */
public class DefaultTimeSignalReceiver implements TimeSignalListener {

	private long time;

	private BooleanLock lock;

	private TimeSignalGenerator tsg;

	public DefaultTimeSignalReceiver(TimeSignalGenerator tsg) {
		this.tsg = tsg;
		lock = new BooleanLock(false);
	}

	/** Handle timing signal, releases semaphore. */
	public void timingSignal(long time) {
		System.err.println("DTSR:Time signal callback received, releasing lock...");
		synchronized (lock) {
			this.time = time;
			lock.setValue(true);
		}
	}

	/** A blocking method which callers can use to wait for a time-signal. */
	public long waitTimeSignal(long t) {
		synchronized (lock) {
			lock.setValue(false);

			System.err.println("DTSR:waiting time signal, lock is false");
			tsg.awaitTimingSignal(this, t);
			try {
				System.err.println("DTSR:waiting time signal waiting for lock release...");
				lock.waitUntilTrue(0); // wait forever...
			} catch (InterruptedException ix) {
			}
		}
		return t;
	}

}
