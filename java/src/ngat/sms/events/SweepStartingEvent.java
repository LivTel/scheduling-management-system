/**
 * 
 */
package ngat.sms.events;

/**
 * @author eng
 *
 */
public class SweepStartingEvent extends SchedulingStatus {

	private int sweepId;
	
	
	/**
	 * @return the sweepId
	 */
	public int getSweepId() {
		return sweepId;
	}


	/**
	 * @param sweepId the sweepId to set
	 */
	public void setSweepId(int sweepId) {
		this.sweepId = sweepId;
	}


	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getCategoryName()
	 */
	public String getCategoryName() {
		return "SWS";
	}

	public String toString() {
		return "SWS: "+ statusTimeStamp + " SW: "+sweepId;
	}
	
}
