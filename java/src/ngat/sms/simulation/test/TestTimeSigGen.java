/**
 * 
 */
package ngat.sms.simulation.test;

import ngat.sms.simulation.TimeSignalGenerator;
import ngat.sms.simulation.TimeSignalListener;

/**
 * @author eng
 *
 */
public class TestTimeSigGen implements TimeSignalGenerator {

	private long time;
	
	/* (non-Javadoc)
	 * @see ngat.sms.simulation.TimeSignalGenerator#awaitTimingSignal(ngat.sms.simulation.TimeSignalListener, long)
	 */
	public void awaitTimingSignal(TimeSignalListener tsl, long time) {
		System.err.println("TSG: time signal requested");
		// fire a thread to do this after a bit
		final long ftime = time;
		final TimeSignalListener ftsl = tsl;
		Runnable r = new Runnable() {			
			public void run() {
				try {Thread.sleep(10000L);}catch (InterruptedException ix) {}
				TestTimeSigGen.this.time = ftime;
				System.err.println("TSG:sending time signal");
				ftsl.timingSignal(ftime);
			}
		};
		(new Thread(r)).start();
	}

	/* (non-Javadoc)
	 * @see ngat.sms.TimeModel#getTime()
	 */
	public long getTime() {	
		return time;
	}

}
