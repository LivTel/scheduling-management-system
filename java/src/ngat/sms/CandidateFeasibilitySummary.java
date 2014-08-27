/**
 * 
 */
package ngat.sms;

/**
 * @author eng
 *
 */
public class CandidateFeasibilitySummary {

	private boolean feasible;
	
	private GroupItem group;
	
	private String rejectionReason;

	
	/**
	 * 
	 */
	public CandidateFeasibilitySummary() {
	}


	/**
	 * @param group
	 * @param rejectionReason
	 */
	public CandidateFeasibilitySummary(GroupItem group, boolean feasible, String rejectionReason) {
		this();
		this.group = group;
		this.feasible = feasible;
		this.rejectionReason = rejectionReason;
	}


	/**
	 * @return the feasible
	 */
	public boolean isFeasible() {
		return feasible;
	}


	/**
	 * @param feasible the feasible to set
	 */
	public void setFeasible(boolean feasible) {
		this.feasible = feasible;
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
	 * @return the rejectionReason
	 */
	public String getRejectionReason() {
		return rejectionReason;
	}


	/**
	 * @param rejectionReason the rejectionReason to set
	 */
	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	public String toString() {
		return "CFS: "+(group != null ? group.getName():"Group=null") + (feasible ? "Feasible":"NotFeasible:"+rejectionReason);
	}

	
}
