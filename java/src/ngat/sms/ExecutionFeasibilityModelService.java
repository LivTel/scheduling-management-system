/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author eng
 *
 */
public interface ExecutionFeasibilityModelService extends Remote {

	public CandidateFeasibilitySummary isitFeasible(GroupItem group, long time, ExecutionHistorySynopsis history, AccountSynopsis accounts,
			EnvironmentSnapshot env, List<Disruptor> disruptors) throws RemoteException;

}
