/**
 * 
 */
package ngat.sms.events;

import java.io.Serializable;

import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public abstract class SchedulingStatus implements Serializable, StatusCategory {

	protected long statusTimeStamp;

	/**
	 * @return the statusTimeStamp
	 */
	public long getStatusTimeStamp() {
		return statusTimeStamp;
	}

	/**
	 * @param statusTimeStamp the statusTimeStamp to set
	 */
	public void setStatusTimeStamp(long statusTimeStamp) {
		this.statusTimeStamp = statusTimeStamp;
	}


}
