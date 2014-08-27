/**
 * 
 */
package ngat.sms;

/**
 * @author eng
 *
 */
public class DefaultMutableTimeModel implements MutableTimeModel {

	/** The current model time.*/
	private long time;
	
	/** @return Current model time.
	 * @see ngat.sms.TimeModel#getTime()
	 */
	public long getTime() {
		return time;
	}

	/** 
	 * Set the current model time.
	 * @param time The current model time.
	 */
	public void setTime(long time) {this.time = time;}
	
}
