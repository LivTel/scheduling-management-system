/**
 * 
 */
package ngat.sms.simulation;

import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public interface StochasticExecutionTimingModel {

	
	/** Implementors shoudl work out how long the group will take.
	 * @param group The group whose execution time we want to decide on.
	 * @return An execution time for the group.
	 */
	public long getExecutionTime(GroupItem group);
	
}
