/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface FeasibilityPrescanMonitor extends Remote {

	/** Add a listener to the list of registered prescan update listeners.
	 * @param l The listener to add.
	 * @throws RemoteException
	 */
	public void addFeasibilityPrescanUpdateListener(FeasibilityPrescanUpdateListener l) throws RemoteException;
	
	/** Remove a listener from the list of registered prescan update listeners.
	 * @param l The listener to remove.
	 * @throws RemoteException
	 */
	public void removeFeasibilityPrescanUpdateListener(FeasibilityPrescanUpdateListener l) throws RemoteException;
	
	
}
