/**
 * 
 */
package ngat.sms;

import java.io.Serializable;
import java.util.Date;

/** Represents a snapshot of the weather at a time.
 * TODO may add howLongInStete.
 * @author eng
 *
 */
public class WeatherSnapshot implements Serializable {


	/** Time for which this snapshot is valid. */
	private long timeStamp;
	
	/** True if the weather is good.*/
	private boolean good;
	
	// TODO how long has the weather been in the current state (good or bad).
	//private long howLongStable;

	/**
	 * @param timeStamp
	 * @param good
	 */
	public WeatherSnapshot(long timeStamp, boolean good) {
		super();
		this.timeStamp = timeStamp;
		this.good = good;
	}
	
	/**
	 * @return The time of the snapshot.
	 */
	public long getTimeStamp() {
		return timeStamp;
	}
	
	/**
	 * @return True if the weather is good.
	 */
	public boolean isGood() {  return good; }
	
	public String toString() {
		return "[Weather:t=" + (new Date(timeStamp)) +(good ? "Good" : "Bad");
	}
}
