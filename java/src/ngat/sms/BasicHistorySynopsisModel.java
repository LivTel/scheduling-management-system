/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import ngat.oss.model.*;
import ngat.phase2.*;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class BasicHistorySynopsisModel extends UnicastRemoteObject implements ExecutionHistorySynopsisModel {

	private BaseModelProvider bmp;

	/** A raw history model to extract data from. */
	private IHistoryModel historyModel;

	Map<Long, ExecutionHistorySynopsis> cache;

	LogGenerator logger;

	/**
	 * @param historyModel
	 */
	public BasicHistorySynopsisModel(BaseModelProvider bmp) throws RemoteException {
		// public BasicHistorySynopsisModel(IHistoryModel historyModel) throws
		// RemoteException {
		super();
		// this.historyModel = historyModel;
		this.bmp = bmp;
		cache = new HashMap<Long, ExecutionHistorySynopsis>();

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("bhsm");
	}

	public void loadHistory(Phase2CompositeModel p2g, long time) throws Exception {

		// historyModel = bmp.getHistoryModel();
		logger.create().block("loadHistory").info().level(1).msg("Start loading history synopsis").send();

		// problem here when we load the history stuff
		List<GroupItem> lgroups = p2g.listGroups();
		Iterator<GroupItem> groups = lgroups.iterator();
		while (groups.hasNext()) {
			GroupItem group = groups.next();
			ExecutionHistorySynopsis exec = getRemoteExecutionHistorySynopsis(group.getID(), time);
			cache.put(new Long(group.getID()), exec);

			logger.create().block("loadHistory").info().level(2)
					.msg("Added " + group.getID() + "," + group.getName() + "->" + exec).send();
		}
		logger.create().block("loadHistory").info().level(1).msg("Loading history synopsis completed").send();
	}

	public void loadProposalHistory(Phase2CompositeModel p2g, long pid, long time) throws Exception {

		// historyModel = bmp.getHistoryModel();
		logger.create().block("loadHistory").info().level(1).msg("Start loading history synopsis").send();

		// problem here when we load the history stuff
		List<GroupItem> lgroups = p2g.listGroups();
		Iterator<GroupItem> groups = lgroups.iterator();
		while (groups.hasNext()) {
			GroupItem group = groups.next();
			IProposal proposal = group.getProposal();
			if (proposal == null)
				continue;
			long gpid = proposal.getID();
			if (gpid != pid)
				continue;
			ExecutionHistorySynopsis exec = getRemoteExecutionHistorySynopsis(group.getID(), time);
			cache.put(new Long(group.getID()), exec);

			logger.create().block("loadProposalHistory").info().level(2)
					.msg("Added " + group.getID() + "," + group.getName() + "->" + exec).send();
		}
		logger.create().block("loadHistory").info().level(1).msg("Loading history synopsis completed").send();
	}

	
	
	public ExecutionHistorySynopsis getExecutionHistorySynopsis(long gid, long time) throws RemoteException {
		try {
			if (cache.containsKey(gid))
				return cache.get(gid);

			// grab this from remote, then cache it..
			ExecutionHistorySynopsis exec = getRemoteExecutionHistorySynopsis(gid, time);
			cache.put(new Long(gid), exec);
			logger.create().block("getExecutionHistorySynopsis").info().level(2)
					.msg("Added synopsis for group:" + gid + "->" + exec).send();

			return exec;
		} catch (Exception e) {
			throw new RemoteException(this.getClass().getName() + ".getExecHistSynopsis()", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.ExecutionHistorySynopsisModel#getExecutionHistorySynopsis(long,
	 * long)
	 */
	public ExecutionHistorySynopsis getRemoteExecutionHistorySynopsis(long gid, long time) throws Exception {

		IHistoryModel historyModel = bmp.getHistoryModel();

		ExecutionHistorySynopsis exec = new ExecutionHistorySynopsis();

		long latest = 0L;
		int count = 0;
		int hsize = 0;

		List list = historyModel.listHistoryItems(gid);
		hsize = list.size();
		Iterator hist = list.iterator();
		while (hist.hasNext()) {
			IHistoryItem item = (IHistoryItem) hist.next();
			long ctime = item.getCompletionTime();
			if (item.getCompletionStatus() == IHistoryItem.EXECUTION_SUCCESSFUL && ctime < time) {
				count++;
				if (ctime > latest)
					latest = ctime;
			}
		}
		exec.setLastExecution(latest);
		exec.setCountExecutions(count);

		logger.create().block("getRemoteExecutionHistorySynopsis(").info().level(2)
				.msg("Returning remote history for gid: " + gid + " " + count + "/" + hsize).send();

		return exec;

	}

	public void updateHistory(long gid, long hid, int cstat, long ctime, IExecutionFailureContext efc, Set qosStats)
			throws RemoteException {
		try {

			// update cache only if this was successful execution
			if (cstat == IHistoryItem.EXECUTION_SUCCESSFUL) {
				// update local cache
				if (cache.containsKey(gid)) {
					ExecutionHistorySynopsis exec = cache.get(gid);
					exec.setLastExecution(ctime);
					exec.setCountExecutions(exec.getCountExecutions() + 1);

					logger.create()
							.block("updateHistory")
							.info()
							.level(2)
							.msg("Update locally cached history: Ctime=" + ctime + ", Count = "
									+ exec.getCountExecutions()).send();
				}
			}

			// now update remote history
			IHistoryModel historyModel = bmp.getHistoryModel();
			historyModel.updateHistory(hid, cstat, ctime, efc, qosStats);

		} catch (Exception e) {
			throw new RemoteException(this.getClass().getName() + ".getRemoteExecHist()", e);
		}

	}

	/**
	 * @param gid
	 *            The ID of the group for which a history entry is to be
	 *            created.
	 */
	public long addHistoryItem(long gid) throws RemoteException {
		// all we do is relay the info
		logger.create().block("addHistoryItem").info().level(2).msg("Adding a history item for gid: " + gid).send();

		try {
			IHistoryModel historyModel = bmp.getHistoryModel();
			return historyModel.addHistoryItem(gid);
		} catch (Exception e) {
			throw new RemoteException(this.getClass().getName() + ".addHistItem()", e);
		}

	}

}
