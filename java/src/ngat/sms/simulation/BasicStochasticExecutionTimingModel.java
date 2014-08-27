/**
 * 
 */
package ngat.sms.simulation;

import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public class BasicStochasticExecutionTimingModel implements StochasticExecutionTimingModel {

	private ExecutionResourceUsageEstimationModel xrm;
	
	/**
	 * @param xrm
	 */
	public BasicStochasticExecutionTimingModel(ExecutionResourceUsageEstimationModel xrm) {
		super();
		this.xrm = xrm;
	}

	/* (non-Javadoc)
	 * @see ngat.sms.simulation.StochasticExecutionTimingModel#getExecutionTime(ngat.sms.GroupItem)
	 */
	public long getExecutionTime(GroupItem group) {
		
		ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(group);
		ExecutionResource timeResource = xrb.getResource("TIME");
		long exec  = (long)timeResource.getResourceUsage();
		return exec;
		
	}

}
