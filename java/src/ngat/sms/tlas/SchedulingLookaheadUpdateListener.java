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
public interface SchedulingLookaheadUpdateListener extends Remote {

	
	/** Notification that a Look-ahead sweep has started.
	 * @param time The time.
	 * @throws RemoteException
	 */
	public void schedulingLookaheadSweepStarted(long time) throws RemoteException;
	
	/** Notification that a Look-ahead sequence has been generated.
	 * @param time The time.
	 * @param horizonSweep The sequence (collection of SweepItems).
	 * @throws RemoteException
	 */
	public void schedulingLookaheadSequenceGenerated(long time, HorizonSweep horizonSweep) throws RemoteException;
	
	/** Notification that an item (group) from the current Look-ahead sequence has been selected for execution.
	 * We should be able to identify the group as we should already know the sequence details.
	 * @param time The time.
	 * @param sqId The id (sequence number) within the current sequence.
	 * @throws RemoteException
	 */
	public void schedulingLookaheadItemSelected(long time, int sqId) throws RemoteException;
	
	/** Notification that an item (group) from the current Look-ahead sequence has been rejected for execution.
	 * We should be able to identify the group as we should already know the sequence details.
	 * @param time The time.
	 * @param sqId The id (sequence number) within the current sequence.
	 * @param reason The reason for rejection. One of: (ENV_BAD, OUT_OF_WINDOW, others??).
	 * @throws RemoteException
	 */
	public void schedulingLookaheadItemRejected(long time, int sqId, String reason) throws RemoteException;
}
