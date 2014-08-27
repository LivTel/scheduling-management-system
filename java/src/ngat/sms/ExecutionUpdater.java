/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

import ngat.phase2.IExecutionFailureContext;
import ngat.phase2.IQosMetric;

/**
 * @author eng
 *
 */
public interface ExecutionUpdater extends Remote {

	/**
	 * 
	 * @param group The ID of the group.
	 * @param time The time the group completed.
	 * @param erb A bundle of resources used in executing the group.
	 * @param qos A set of QOS statistics.
	 * @throws RemoteException
	 */
    public void groupExecutionCompleted(GroupItem group, long time, ExecutionResourceBundle erb, Set<IQosMetric> qosStatistics) throws RemoteException;
	
	/**
	 * 
	 * @param group The ID of the group.
	 * @param time The time the group was abandoned.
	 * @param erb A bundle of resources used in executing the group.
	 * @param efc Information about why the group was abandoned.
	 * @param qos A set of QOS statistics.
	 * @throws RemoteException
	 */
    public void groupExecutionAbandoned(GroupItem group, long time, ExecutionResourceBundle erb, IExecutionFailureContext efc, Set<IQosMetric> qosStatistics) throws RemoteException;
		
	/** Handle notification of an exposure update.
	 * @param group The ID of the group.
	 * @param time The time the exposure was performed (completed?).
	 * @param expId The ID of the specific exposure (observation in old money).
	 * @param fileName Name of the image file (may include mount/path/host/url?).
	 * @throws RemoteException
	 */
	public void groupExposureCompleted(GroupItem group, long time, long expId, String fileName) throws RemoteException;
	
	
}
