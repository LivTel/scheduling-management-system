/**
 * 
 */
package ngat.sms.tlas;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface SchedulingLookaheadStatusProvider extends Remote {

	/** Add a listener to the list of registered SchedulingLookaheadUpdateListeners.
	 * @param l A listener.
	 * @throws RemoteException
	 */
	public void addSchedulingLookaheadUpdateListener(SchedulingLookaheadUpdateListener l) throws RemoteException;
	
	
	/** Remove a listener from the list of registered SchedulingLookaheadUpdateListeners.
	 * @param l A listener.
	 * @throws RemoteException
	 */
	public void removeSchedulingLookaheadUpdateListener(SchedulingLookaheadUpdateListener l) throws RemoteException;
}
