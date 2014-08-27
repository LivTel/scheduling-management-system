/**
 * 
 */
package ngat.sms.bds;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.sms.GroupItem;
import ngat.sms.ScheduleItem;
import ngat.sms.SchedulingStatusProvider;
import ngat.sms.SchedulingStatusUpdateListener;
import ngat.sms.ScoreMetricsSet;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;

/**
 * @author eng
 * 
 */
public class TestScheduleListener extends UnicastRemoteObject implements SchedulingStatusUpdateListener {

	public static final long POLLING_INTERVAL = 10 * 60 * 1000L; // 10 minutes

	/**
	 * @param port
	 * @throws RemoteException
	 */
	public TestScheduleListener(int port) throws RemoteException {
		super(port);

	}

	
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SchedulingStatusUpdateListener#candidateAdded(java.lang.String,
	 * ngat.sms.GroupItem, ngat.sms.ScoreMetricsSet, double, int)
	 */
	public void candidateAdded(String qid, GroupItem group, ScoreMetricsSet metrics, double score, int rank)
			throws RemoteException {
		System.err.println("Candidate was added to : Queue [" + qid + "] " + group + ", score=" + score);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.SchedulingStatusUpdateListener#candidateQueueCleared(java.lang.String)
	 */
	public void candidateQueueCleared(String qid) throws RemoteException {
		System.err.println("Candidate Queue [" + qid + "] was cleared");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.SchedulingStatusUpdateListener#candidateSelected(ngat.sms.ScheduleItem
	 * )
	 */
	public void candidateSelected(long time, ScheduleItem schedule) throws RemoteException {
		System.err.println("Candidate selected: " + schedule);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SchedulingStatusUpdateListener#scheduleSweepStarted(long, int)
	 */
	public void scheduleSweepStarted(long time, int sweep) throws RemoteException {
		System.err.println("Schedule sweep [" + sweep + "] started");
	}

	public static void main(String args[]) {

		try {
			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

			int bport = cfg.getIntValue("bind-port", 8233);

			String shost = cfg.getProperty("sched-host", "localhost");

			while (true) {
				SchedulingStatusProvider sched = (SchedulingStatusProvider) Naming.lookup("rmi://" + shost + "/SchedulingStatusProvider");

				TestScheduleListener tsl = new TestScheduleListener(bport);
				System.err.println("Got scheduler ref: "+sched);
				
				sched.addSchedulingUpdateListener(tsl);
				System.err.println("Registered with scheduler okay");
				
				while (true) { // keep alive and rebind after 10 minutes 
					try {
						Thread.sleep(POLLING_INTERVAL);
					} catch (InterruptedException ix) {
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Usage: java ngat.sms.bds.TestScheduleListener --bind-port <>  --sched-host <>");
		}
	}

	public void candidateRejected(String qId, GroupItem group, String reason) throws RemoteException {
		// TODO Auto-generated method stub
		
	}




	public void schedulerMessage(int messageType, String message)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}

}
