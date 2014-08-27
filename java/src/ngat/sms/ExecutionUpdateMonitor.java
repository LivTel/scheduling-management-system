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
public interface ExecutionUpdateMonitor extends Remote {


    /** Register an ExecutionUpdateListener for updates.
     * @param l A listener to register.
     * @throws RemoteException
     */
	public void addExecutionUpdateListener (ExecutionUpdater l) throws RemoteException;
	
    /** Remove registration of an ExecutionUpdateListener.
     * @param l A listener to de-register.
     * @throws RemoteException
     */
    public void removeExecutionUpdateListener (ExecutionUpdater l) throws RemoteException;
    
	
}
