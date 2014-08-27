package ngat.sms;
import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.phase2.ITimePeriod;

/** Provides an interface to query the scheduling system as to the expected feasibility of a group.
 * @author eng
 *
 */
public interface ExecutionFeasibilityStatisticsScoringCalculator extends Remote {

	
	/** Callers may supply a GroupItem for which they wish to determine the feasibility 
	 * (in terms of likelihood of being scheduled) under the known environmental 
	 * and weather conditions and subject to the anticipated evolution of the phase2 model.
	 * @param group The group we wish to obtain statistics for.
	 * @param period The period over which we want to determine feasibility.
	 * * @param resolution The resolution/granularity required.
	 * @return The feasibility statistics for the execution of the specified group.
	 * @throws RemoteException If anything goes awry.
	 */
	public ExecutionFeasibilityStatistics getFeasibilityStatistics(GroupItem group, ITimePeriod period, long resolution) throws RemoteException;
	
}
