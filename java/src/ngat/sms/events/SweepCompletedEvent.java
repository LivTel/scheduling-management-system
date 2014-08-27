/**
 * 
 */
package ngat.sms.events;

import ngat.sms.ScheduleItem;

/**
 * @author eng
 *
 */
public class SweepCompletedEvent extends SchedulingStatus {

	private ScheduleItem schedule;
	
	
	/**
	 * @return the schedule
	 */
	public ScheduleItem getSchedule() {
		return schedule;
	}


	/**
	 * @param schedule the schedule to set
	 */
	public void setSchedule(ScheduleItem schedule) {
		this.schedule = schedule;
	}


	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getCategoryName()
	 */
	public String getCategoryName() {
		return "SWE";
	}
	
	public String toString() {
		return "SWE: "+statusTimeStamp+" "+schedule;
	}

}
