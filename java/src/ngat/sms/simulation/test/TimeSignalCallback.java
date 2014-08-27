/**
 * 
 */
package ngat.sms.simulation.test;

import java.util.Date;

import ngat.sms.simulation.DefaultTimeSignalReceiver;
import ngat.sms.simulation.TimeSignalListener;

/**
 * @author eng
 *
 */
public class TimeSignalCallback {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {

		TestTimeSigGen tsg = new TestTimeSigGen();
		
		DefaultTimeSignalReceiver tsl = new DefaultTimeSignalReceiver(tsg);
		
		while (true) {
			
			long now = System.currentTimeMillis();
			System.err.println("Waiting Time signal...");
			long t2 = now + 5*60*1000L;
			tsl.waitTimeSignal(t2);
			System.err.println("Time signal received for: "+new Date(t2));			
		}
		
	} catch (Exception e) {
		e.printStackTrace();
	}

	}
	
}
