/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * @author eng
 *
 */
public class DefaultExecutionFeasibilityModelService extends UnicastRemoteObject implements
		ExecutionFeasibilityModelService {

	ExecutionFeasibilityModel xfm;
	
	/**
	 * @throws RemoteException
	 */
	public DefaultExecutionFeasibilityModelService(ExecutionFeasibilityModel xfm) throws RemoteException {
		super();
		this.xfm = xfm;
	}

	public CandidateFeasibilitySummary isitFeasible(GroupItem group, long time, ExecutionHistorySynopsis history, AccountSynopsis accounts,
			EnvironmentSnapshot env, List<Disruptor> disruptors) throws RemoteException {
		return xfm.isitFeasible(group, time, history, accounts, env, disruptors);
	}

	

}
