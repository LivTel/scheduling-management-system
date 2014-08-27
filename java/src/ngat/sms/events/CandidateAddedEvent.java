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
public class CandidateAddedEvent extends SchedulingStatus {

	private String queueId;
	
	private GroupItem group;
	
	private ScoreMetricsSet metrics;
	
	private double score;
	
	private int rank;
	
	
	
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



	/**
	 * @return the metrics
	 */
	public ScoreMetricsSet getMetrics() {
		return metrics;
	}



	/**
	 * @param metrics the metrics to set
	 */
	public void setMetrics(ScoreMetricsSet metrics) {
		this.metrics = metrics;
	}



	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}



	/**
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}



	/**
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}



	/**
	 * @param rank the rank to set
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}



	public String getCategoryName() {
		return "CAA";
	}

	
	public String toString() {
		return "CAA: "+ statusTimeStamp+" "+group;
	}
}
