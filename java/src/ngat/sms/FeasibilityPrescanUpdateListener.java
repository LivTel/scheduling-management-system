/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.sms.util.PrescanEntry;

/**
 * Handler for updates from a prescanner.
 * 
 * @author eng
 * 
 */
public interface FeasibilityPrescanUpdateListener extends Remote {

	/**
	 * Notification that a prescan entry has been updated. This may be the first
	 * entry for a group, or may be an update or may indicate that the group is
	 * no longer feasible depending on the content and the handler's existing
	 * knowledge.
	 * 
	 * @param pse
	 *            The new entry.
	 * @throws RemoteException
	 */
	public void prescanUpdate(PrescanEntry pse) throws RemoteException;

	/**
	 * Notification that a full prescan is starting and any data held should be
	 * cleared ready for new updates.
	 * @param numberGroups The number of groups to be checked.
	 * @throws RemoteException
	 */
	public void prescanStarting(int numberGroups) throws RemoteException;

	
	/** 
	 * Notification that a prescan has completed.
	 * @throws RemoteException
	 */
	public void prescanCompleted() throws RemoteException;
	
	
}
