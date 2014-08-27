/**
 * 
 */
package ngat.sms;

import ngat.phase2.ISequenceComponent;

/**
 * TODO This may need to be changed so we can name specific resources we want estimations for...
 * @author eng
 *
 */
public interface ExecutionResourceUsageEstimationModel {

	/** Calculates the estimated usage of resources by a group.
	 * @param group The group for which we want the resource estimation.
	 * @return Estimated resource usage as a bundle.
	 */
	public ExecutionResourceBundle getEstimatedResourceUsage(GroupItem group);
	
	public double getExecTime(ISequenceComponent seq);
	
}
