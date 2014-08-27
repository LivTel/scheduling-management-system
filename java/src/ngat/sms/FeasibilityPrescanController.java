/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ngat.sms.util.PrescanEntry;

/**
 * @author eng
 *
 */
public interface FeasibilityPrescanController extends Remote {

	/** Execute a prescan based on the specified time.*/
	public List prescan(long time, long interval) throws RemoteException;
	
	/** Execute a prescan for the specified group based on the specified time.*/
	public PrescanEntry prescan(GroupItem group, long time, long interval) throws RemoteException;
	
}
