/**
 * 
 */
package ngat.sms;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.sms.events.SchedulingStatus;

/**
 * @author eng
 *
 */
public interface SchedulingStatusArchive extends Remote {

	/** Request for archived status information.
	 * @param t1 Start time for archive search.
	 * @param t2 End time for archive search.
	 * @return Archived status information between search time limits.
	 * @throws RemoteException
	 */
	public List<SchedulingStatus> getSchedulerStatusHistory(long t1, long t2) throws RemoteException;
	
	
}
