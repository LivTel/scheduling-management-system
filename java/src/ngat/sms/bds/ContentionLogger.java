/**
 * 
 */
package ngat.sms.bds;

import java.io.File;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.sms.GroupItem;
import ngat.sms.ScheduleItem;
import ngat.sms.SchedulingStatusUpdateListener;
import ngat.sms.ScoreMetricsSet;

/** Logs the contention and score for the winning group.
 * @author eng
 *
 */
public class ContentionLogger extends UnicastRemoteObject implements SchedulingStatusUpdateListener {

	/** A file to log information to.*/
	private File file;
	
	private int bgQSize;
	private int primaryQSize;
	private int fixedQSize;
	
	private double maxScore;
	
	private PrintStream pout;
	/**
	 * @param file
	 */
	public ContentionLogger(File file) throws Exception{
		super();
		this.file = file;
		pout = new PrintStream(file);
	}

	/* (non-Javadoc)
	 * @see ngat.sms.SchedulingStatusUpdateListener#candidateAdded(java.lang.String, ngat.sms.GroupItem, ngat.sms.ScoreMetricsSet, double, int)
	 */
	public void candidateAdded(String qId, GroupItem group, ScoreMetricsSet metrics, double score, int rank)
			throws RemoteException {
		// add a candidate to the count
		if (qId.equals(BasicDespatchScheduler.FIXED_QUEUE_ID)) {
			fixedQSize++;
		} else if (qId.equals(BasicDespatchScheduler.PRIMARY_QUEUE_ID)) {
			primaryQSize++;
			if (score > maxScore)
				maxScore = score;
		} else if (qId.equals(BasicDespatchScheduler.BACKGROUND_QUEUE_ID)) {
			bgQSize++;
			if (score > maxScore)
				maxScore = score;
		}
	}

	/* (non-Javadoc)
	 * @see ngat.sms.SchedulingStatusUpdateListener#candidateSelected(ngat.sms.ScheduleItem)
	 */
	public void candidateSelected(long time, ScheduleItem schedule) throws RemoteException {
		// we can now log the contention data
		
		System.err.printf("%tT %tF %4d %4d %4d %4.2f \n", time, time, fixedQSize, primaryQSize, bgQSize, maxScore);
		pout.printf("%tT %tF %4d %4d %4d %4.2f \n", time, time, fixedQSize, primaryQSize, bgQSize, maxScore);
	}

	/* (non-Javadoc)
	 * @see ngat.sms.SchedulingStatusUpdateListener#scheduleSweepStarted(long, int)
	 */
	public void scheduleSweepStarted(long time, int sweepId) throws RemoteException {
		// clear counters
		fixedQSize = 0;
		primaryQSize = 0;
		bgQSize = 0;
		maxScore = -999.999;
	}

	public void candidateRejected(String qId, GroupItem group, String reason) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	public void schedulerMessage(int messageType, String message)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}

}
