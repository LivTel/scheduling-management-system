/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.net.telemetry.SecondaryCache;
import ngat.net.telemetry.MysqlBackingStore;
import ngat.sms.events.CandidateRejectedEvent;
import ngat.sms.events.MessageEvent;
import ngat.sms.events.SchedulingStatus;
import ngat.sms.events.SweepCompletedEvent;
import ngat.sms.events.SweepStartingEvent;
import ngat.sms.events.CandidateAddedEvent;
import ngat.util.ControlThread;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class SchedulingArchiveGateway extends UnicastRemoteObject implements SchedulingStatusUpdateListener,
		SchedulingStatusProvider, SchedulingStatusArchive {

	/** Logger. */
	private LogGenerator slogger;

	/** A list of registered SchedulingStatusUpdateListeners. */
	private List<SchedulingStatusUpdateListener> listeners;

	/** A list of candidate SchedulingStatusUpdateListeners. */
	private List<SchedulingStatusUpdateListener> addListeners;

	/** A list of SchedulingUpdateListeners to delete. */
	private List<SchedulingStatusUpdateListener> deleteListeners;

	/** Telescope status source. */
	private SchedulingStatusProvider ssp;

	private List<SchedulingStatus> archive;

	/** Temporarily restrict number of rejects we will notify to remove clients. */
	private volatile int rejectCount = 0;

	/**
	 * Counts the number of archive entries which have been forwarded to current
	 * listeners.
	 */
	private int processedCount;

	// START TEMPLATE CODE

	/** Processor cycle interval. Default to 10 sec. */
	private long processInterval = 10 * 1000L;

	/** How often do we check for culling. Default to 30 minutes. */
	private long cullInterval = 30 * 60 * 1000L;

	/** Age of oldest data to keep in local cache. Default to 1 hour. */
	private long backingStoreAgeLimit = 60 * 60 * 1000L;

	/**
	 * How often relative to process sweep do we check for culling. Default to
	 * every 10 sweeps.
	 */
	private int cullSweepIndicator = 10;

	/** the secondary cache. */
	private SecondaryCache backingStore;

	// END TEMPLATE CODE

	/**
	 * Create a SchedulingArchiveGateway.
	 * 
	 * @throws RemoteException
	 */
	public SchedulingArchiveGateway(SchedulingStatusProvider ssp) throws RemoteException {
		super();
		this.ssp = ssp;

		Logger alogger = LogManager.getLogger("SMS"); // probably should be
		// RCS.Telem
		slogger = alogger.generate().system("SMS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("SMS_Gateway");

		ssp.addSchedulingUpdateListener(this);

		archive = new Vector<SchedulingStatus>();

		listeners = new Vector<SchedulingStatusUpdateListener>();
		addListeners = new Vector<SchedulingStatusUpdateListener>();
		deleteListeners = new Vector<SchedulingStatusUpdateListener>();

		processedCount = 0;
	}

	/**
	 * @return the processInterval
	 */
	public long getProcessInterval() {
		return processInterval;
	}

	/**
	 * @param processInterval
	 *            the processInterval to set
	 */
	public void setProcessInterval(long processInterval) {
		this.processInterval = processInterval;
	}

	/**
	 * @return the cullInterval
	 */
	public long getCullInterval() {
		return cullInterval;
	}

	/**
	 * @param cullInterval
	 *            the cullInterval to set
	 */
	public void setCullInterval(long cullInterval) {
		this.cullInterval = cullInterval;
	}

	/**
	 * @return the backingStoreAgeLimit
	 */
	public long getBackingStoreAgeLimit() {
		return backingStoreAgeLimit;
	}

	/**
	 * @param backingStoreAgeLimit
	 *            the backingStoreAgeLimit to set
	 */
	public void setBackingStoreAgeLimit(long backingStoreAgeLimit) {
		this.backingStoreAgeLimit = backingStoreAgeLimit;
	}

	/**
	 * @return the backingStore
	 */
	public SecondaryCache getBackingStore() {
		return backingStore;
	}

	/**
	 * @param backingStore
	 *            the backingStore to set
	 */
	public void setBackingStore(SecondaryCache backingStore) {
		this.backingStore = backingStore;
	}

	private void notifyListenersSchedulingStatusUpdate(SchedulingStatus status) {
		// remove any kill items
		if (!deleteListeners.isEmpty()) {
			for (int id = 0; id < deleteListeners.size(); id++) {
				SchedulingStatusUpdateListener l = deleteListeners.get(id);
				if (listeners.contains(l)) {
					listeners.remove(l);
					slogger.create().info().level(2).msg("Removing listener " + l).send();
				}
			}
		}

		// add new listeners
		if (!addListeners.isEmpty()) {
			for (int ia = 0; ia < addListeners.size(); ia++) {
				SchedulingStatusUpdateListener l = addListeners.get(ia);
				if (!listeners.contains(l)) {
					listeners.add(l);
					slogger.create().info().level(2).msg("Adding new listener " + l).send();
				}
			}

		}

		// broadcast
		for (int il = 0; il < listeners.size(); il++) {
			SchedulingStatusUpdateListener l = null;
			try {
				l = listeners.get(il);

				// what type of event is this
				if (status instanceof SweepStartingEvent) {
					SweepStartingEvent sse = (SweepStartingEvent) status;
					l.scheduleSweepStarted(sse.getStatusTimeStamp(), sse.getSweepId());
				} else if (status instanceof CandidateAddedEvent) {
					CandidateAddedEvent cav = (CandidateAddedEvent) status;
					l.candidateAdded(cav.getQueueId(), cav.getGroup(), cav.getMetrics(), cav.getScore(), cav.getRank());
				} else if (status instanceof CandidateRejectedEvent) {
					CandidateRejectedEvent crj = (CandidateRejectedEvent) status;
					l.candidateRejected(crj.getQueueId(), crj.getGroup(), crj.getReason());

				} else if (status instanceof SweepCompletedEvent) {
					SweepCompletedEvent sce = (SweepCompletedEvent) status;
					l.candidateSelected(sce.getStatusTimeStamp(), sce.getSchedule());
				}

			} catch (Exception e) {
				if (l != null) {
					deleteListeners.add(l);
					slogger.create().info().level(2).msg("Adding unresponsive listener: " + l + " to kill list").send();
				}
			}
		}

	}

	public List<SchedulingStatus> getSchedulerStatusHistory(long t1, long t2) throws RemoteException {

		slogger.create().info().level(2)
				.msg(String.format("Request for archived data from: %tF %tT to %tF %tT", t1, t1, t2, t2)).send();
		List<SchedulingStatus> list = new Vector<SchedulingStatus>();

		for (int is = 0; is < archive.size(); is++) {
			SchedulingStatus status = archive.get(is);
			long time = status.getStatusTimeStamp();
			if (time >= t1 && time <= t2)
				list.add(status);
		}
		slogger.create().info().level(2).msg("Returning " + list.size() + " entries").send();
		return list;

	}

	public void addSchedulingUpdateListener(SchedulingStatusUpdateListener l) throws RemoteException {

		// ignore listener already registered
		if (listeners.contains(l))
			return;

		// note current time

		// find all archived data from now-1 hour to now

		// send data to new listener

		// add new listener to new list
		slogger.create().info().level(2).msg("Received request to add new listener: " + l).send();
		addListeners.add(l);

	}

	public void removeSchedulingUpdateListener(SchedulingStatusUpdateListener l) throws RemoteException {

		if (!listeners.contains(l))
			return;

		// add to kill list
		slogger.create().info().level(2).msg("Received request to remove listener: " + l).send();
		deleteListeners.add(l);
	}

	public ScheduleDespatcher getDespatcher() throws RemoteException {
		return ssp.getDespatcher();
	}

	public List<String> listCandidateQueues() throws RemoteException {
		return ssp.listCandidateQueues();
	}

	public void candidateRejected(String queueId, GroupItem group, String reason) throws RemoteException {

		// TEMP we only count first 100 rejects in a sweep
		if (rejectCount > 100)
			return;

		CandidateRejectedEvent cre = new CandidateRejectedEvent();
		cre.setStatusTimeStamp(System.currentTimeMillis());
		cre.setGroup(group);
		cre.setQueueId(queueId);
		cre.setReason(reason);
		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + cre).send();

		archive.add(cre);

		// TEMP count rejects
		rejectCount++;

	}
	/** Pass a message from the scheduler.
	 * @param messageType Type of message.
	 * @param message The message content.
	 * @throws RemoteException
	 */
	public void schedulerMessage(int messageType, String message) throws RemoteException {
		MessageEvent msg = new MessageEvent();
		msg.setStatusTimeStamp(System.currentTimeMillis());
		msg.setMessageType(messageType);
		msg.setMessage(message);
		
		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + msg).send();
		archive.add(msg);
	}
	
	public void candidateAdded(String queueId, GroupItem group, ScoreMetricsSet metrics, double score, int rank)
			throws RemoteException {

		CandidateAddedEvent cav = new CandidateAddedEvent();
		cav.setStatusTimeStamp(System.currentTimeMillis());
		cav.setGroup(group);
		cav.setMetrics(metrics);
		cav.setQueueId(queueId);
		cav.setRank(rank);
		cav.setScore(score);

		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + cav).send();
		archive.add(cav);
	}

	public void candidateSelected(long time, ScheduleItem schedule) throws RemoteException {

		SweepCompletedEvent sce = new SweepCompletedEvent();
		sce.setStatusTimeStamp(time);
		sce.setSchedule(schedule);

		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + sce).send();
		archive.add(sce);
	}

	// TODO add context sweep parameters here such as ENV
	public void scheduleSweepStarted(long time, int sweepId) throws RemoteException {

		SweepStartingEvent sse = new SweepStartingEvent();
		sse.setStatusTimeStamp(time);
		sse.setSweepId(sweepId);

		slogger.create().info().level(2).msg("Add status update: " + archive.size() + " to archive: " + sse).send();
		archive.add(sse);

		// TEMP start counting rejects
		rejectCount = 0;

	}

	public void startProcessor() {
		ProcessorThread pt = new ProcessorThread();
		pt.start();
	}

	private class ProcessorThread extends ControlThread {

		/** Count cycles. */
		private int ipcc = 0;

		/**
		 * @param interval
		 */
		public ProcessorThread() {
			super("TCM_G_PT", true);
		}

		@Override
		protected void initialise() {
			// TODO Auto-generated method stub

		}

		// START NEW
		@Override
		protected void mainTask() {

			try {
				Thread.sleep(processInterval);
			} catch (InterruptedException ix) {
			}

			slogger.create().info().level(3).msg("Processor sweep: " + ipcc).send();

			// Cull aged items
			backingStoreCull();

			// Process pending items
			processPendingStatus();

			ipcc++;
		}

		private void backingStoreCull() {

			// check backing store every so often ci/pi OR 10 whichever is
			// larger
			double ratio = (double) cullInterval / (double) processInterval;
			if (Double.isNaN(ratio) || Double.isInfinite(ratio))
				cullSweepIndicator = 10;
			else
				cullSweepIndicator = Math.max(10, (int) (Math.floor(ratio + 1.0)));

			slogger.create().info().level(3).msg("Sweep indicator set to: " + cullSweepIndicator).send();

			if (ipcc % cullSweepIndicator == 0) {

				List<SchedulingStatus> dumpList = new Vector<SchedulingStatus>();

				long cutoffTime = System.currentTimeMillis() - backingStoreAgeLimit;
				slogger.create()
						.info()
						.level(3)
						.msg(String.format("Purge to backing store, items dated before: %tF %tT \n", cutoffTime,
								cutoffTime)).send();

				int cullCount = 0;
				// purge oldest data into backingStore.
				for (int is = 0; is < processedCount; is++) {
					SchedulingStatus status = archive.get(is);

					if (status.getStatusTimeStamp() < cutoffTime) {
						cullCount++;
						dumpList.add(status);
					}

				}

				slogger.create().info().level(3)
						.msg("Checked " + processedCount + " entries, found " + cullCount + " aged items").send();

				// push the culled data into the backing store
				if (backingStore != null) {
					int ntb = 0;
					for (int is = 0; is < cullCount; is++) {
						SchedulingStatus status = archive.get(is);
						try {
							backingStore.storeStatus(status);
							ntb++;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					slogger.create().info().level(3)
							.msg("Successfully dumped " + ntb + " of " + cullCount + " items to backing store").send();
				} else {
					slogger.create().info().level(3).msg("No backing store so culled items will be lost").send();
				}

				// chop the culled data out of the local cache
				archive.removeAll(dumpList);

				processedCount -= cullCount;
				int ias = archive.size();
				slogger.create()
						.info()
						.level(3)
						.msg("Removed aged entries from live cache, size now " + ias + ", processing starts at: "
								+ processedCount).send();

			}

		}

		private void processPendingStatus() {
			int ias = archive.size();
			slogger.create().info().level(2).msg("Processing archived status from: " + processedCount + " to " + ias)
					.send();

			for (int is = processedCount; is < ias; is++) {
				SchedulingStatus status = archive.get(is);
				notifyListenersSchedulingStatusUpdate(status);
			}
			// we have processed all known archived status
			processedCount = ias;
		}

		// END NEW

		@Override
		protected void shutdown() {
			// TODO Auto-generated method stub

		}

	}

}