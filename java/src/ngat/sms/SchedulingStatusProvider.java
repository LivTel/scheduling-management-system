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
public interface SchedulingStatusProvider extends Remote {

    /** Register a SchedulingStatusUpdateListener for updates.
     * @param l A listener to register.
     * @throws RemoteException
     */
	public void addSchedulingUpdateListener (SchedulingStatusUpdateListener l) throws RemoteException;
	
    /** Remove registration of a SchedulingStatusUpdateListener.
     * @param l A listener to de-register.
     * @throws RemoteException
     */
    public void removeSchedulingUpdateListener (SchedulingStatusUpdateListener l) throws RemoteException;
    
    /** Returns a list (by name) of the available candidate queues.
     * @return A list of candidate queue names used by this scheduler.
     * @throws RemoteException
     */
    public List<String> listCandidateQueues() throws RemoteException;
	
    /** Return a reference to the scheduler's despatcher.
	@return A reference to the scheduler's despatcher.
	* @throws RemoteException
    */
	public ScheduleDespatcher getDespatcher() throws RemoteException;
	
}
