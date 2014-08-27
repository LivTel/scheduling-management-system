/**
 * 
 */
package ngat.sms.events;

import ngat.sms.GroupItem;
import ngat.sms.ScoreMetricsSet;

/**
 * @author eng
 *
 */
public class CandidateRejectedEvent extends SchedulingStatus {

	private String queueId;
	
	private GroupItem group;
	
    private String reason;
	
	
	
	/**
	 * @return the queueId
	 */
	public String getQueueId() {
		return queueId;
	}



	/**
	 * @param queueId the queueId to set
	 */
	public void setQueueId(String queueId) {
		this.queueId = queueId;
	}



	/**
	 * @return the group
	 */
	public GroupItem getGroup() {
		return group;
	}



	/**
	 * @param group the group to set
	 */
	public void setGroup(GroupItem group) {
		this.group = group;
	}

    public void setReason(String reason) {
	this.reason = reason;
    }

    public String getReason() {
	return reason;
    }

    public String getCategoryName() { 

		return "CRJ";
	}

	
	public String toString() {
		return "CRJ: "+ statusTimeStamp+" "+group;
	}
}
