/**
 * 
 */
package ngat.sms.tlas;

import ngat.sms.GroupItem;
import ngat.sms.util.PrescanEntry;

/**
 * @author eng
 *
 */
public class SweepItem {
	
	private GroupItem group;

	private double score;

	private double exec;
	
	private long slack;
	
	private String genome;
	
	private PrescanEntry pse;

	/**
	 * @param group
	 * @param score
	 * @param exec
	 */
	public SweepItem(GroupItem group, double score, double exec) {
		super();
		this.group = group;
		this.score = score;
		this.exec = exec;
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
	 * @return the slack
	 */
	public long getSlack() {
		return slack;
	}



	/**
	 * @param slack the slack to set
	 */
	public void setSlack(long slack) {
		this.slack = slack;
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
	 * @return the exec
	 */
	public double getExec() {
		return exec;
	}



	/**
	 * @param exec the exec to set
	 */
	public void setExec(double exec) {
		this.exec = exec;
	}



	/**
	 * @return the genome
	 */
	public String getGenome() {
		return genome;
	}



	/**
	 * @param genome the genome to set
	 */
	public void setGenome(String genome) {
		this.genome = genome;
	}



	/**
	 * @return the pse
	 */
	public PrescanEntry getPse() {
		return pse;
	}



	/**
	 * @param pse the pse to set
	 */
	public void setPse(PrescanEntry pse) {
		this.pse = pse;		
	}


}