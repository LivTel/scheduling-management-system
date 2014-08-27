/**
 * 
 */
package ngat.sms;

/**
 * @author eng
 *
 */
public class RealTimeModel implements TimeModel {

	/** @return The actual system time.
	 * @see ngat.sms.TimeModel#getTime()
	 */
	public long getTime() {
		return System.currentTimeMillis();
	}

}
