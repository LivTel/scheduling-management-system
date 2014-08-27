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
public interface ExecutionUpdateManager extends Remote {

	
	/** Obtains the ExecutionUpdater for the specified group. 
	 * @param gid The ID of the group for which we want an updater.
	 * @return The ExecutionUpdater handling the specified execution instance.
	 * @throws Exception When something goes awry.
	 */
	public ExecutionUpdater getExecutionUpdater(long gid) throws RemoteException;
	
	
}
