/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.util.ResourceBundle;

import ngat.phase2.ISequenceComponent;

/**
 * @author eng
 *
 */
/**
 * @author eng
 *
 */
public interface ScheduleItem {

	/** @return The ID tags for the group.*/
	public GroupItem getGroup();
	
	/** @return The ExecutionUpdater handling this execution instance.*/
	public ExecutionUpdater getExecutionUpdater();
	
	/** @return The score of the selected group - TBD this should be metrics.*/	
	public double getScore();
	
}
