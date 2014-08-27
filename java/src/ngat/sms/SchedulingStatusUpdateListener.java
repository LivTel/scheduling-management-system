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
public interface SchedulingStatusUpdateListener extends Remote {

	/** Pass a message from the scheduler.
	 * @param messageType Type of message.
	 * @param message The message content.
	 * @throws RemoteException
	 */
	public void schedulerMessage(int messageType, String message) throws RemoteException;
	
	/** Indicates that a new sweep has started. 
	 * @param time The time the sweep started.
	 * @param sweepId An ID for this sweep.
	 * @throws RemoteException
	 */
	public void scheduleSweepStarted(long time, int sweepId) throws RemoteException;

	
	/**
	 * Called to notify listener that a candidate of specified rank and score
	 * with specified metrics has been added to the specified queue.
	 * 
	 * @param qId
	 * @param group
	 * @param metrics
	 * @param score
	 * @param rank
	 * @throws RemoteException
	 */
	public void candidateAdded(String qId, GroupItem group, ScoreMetricsSet metrics, double score, int rank)
			throws RemoteException;

    /**
     * Called to notify listener that a candidate has been rejected.
     *
     *
     * @param qId The queue id.
     * @param group The group.
     * @param reason Description of why it was rejected. MAY BE EXPANDED
     */
    public void candidateRejected(String qId, GroupItem group, String reason) throws RemoteException;

	/** Indicates that scheduling sweep ended with a selection of a candidate for execution. */
	public void candidateSelected(long time, ScheduleItem schedule) throws RemoteException;

}
