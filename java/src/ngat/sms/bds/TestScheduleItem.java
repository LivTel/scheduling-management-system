/**
 * 
 */
package ngat.sms.bds;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ResourceBundle;

import ngat.phase2.ISequenceComponent;
import ngat.phase2.IUser;
import ngat.phase2.IProposal;
import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;
import ngat.sms.ScheduleItem;

/**
 * @author eng
 *
 */
public class TestScheduleItem implements ScheduleItem, Serializable {

	private GroupItem group;

	private ExecutionUpdater xu;

	private double score;
	
	/**
	 * @param group
	 * @param xu
	 */
	public TestScheduleItem(GroupItem group, ExecutionUpdater xu) {
		this.group = group;
		this.xu = xu;
	}

	/* (non-Javadoc)
	 * @see ngat.sms.ScheduleItem#getExecutionUpdater()
	 */
	public ExecutionUpdater getExecutionUpdater() {		
		return xu;
	}

	/* (non-Javadoc)
	 * @see ngat.sms.ScheduleItem#getIdent()
	 */
	public GroupItem getGroup()  {		
		return group;
	}
	
	public double getScore()  {		
		return score;
	}
	
	public void setScore(double score){ this.score = score;}
	
	public String toString() {
	 
	    if (group == null)
		return "SCHED_ITEM ?";

	    IProposal proposal = group.getProposal();
	    IUser user = group.getUser();
	    return "SCHED_ITEM : Group: "+group.getName()+
		" "+(proposal != null ? proposal.getName(): "?")+
		" "+(user != null ? user.getName() : "?");
	    
	    // SCHED_ITEM Group: ABC123+456 JL12B123 Smith.John
	    // SCHED_ITEM ?

	}

	

	
}
