/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ngat.oss.impl.mysql.accessors.SemesterAccessor;
import ngat.oss.model.IAccountModel;
import ngat.oss.model.IHistoryModel;
import ngat.oss.model.IPhase2Model;
import ngat.phase2.IAccount;
import ngat.phase2.IExecutionFailureContext;
import ngat.phase2.IHistoryItem;
import ngat.phase2.IObservingConstraint;
import ngat.phase2.IProposal;
import ngat.phase2.IQosMetric;
import ngat.phase2.ISemester;
import ngat.phase2.ITag;
import ngat.phase2.XHistoryItem;
import ngat.phase2.XSemester;
import ngat.phase2.XTransaction;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class DefaultExecutionUpdateManager extends UnicastRemoteObject implements ExecutionUpdateManager, ExecutionUpdateMonitor, ExecutionUpdater {
	
	private SynopticModelProvider smp;
	
	//AccountSynopsisModel proposalAccountModel;
	
	//AccountSynopsisModel tagAccountModel;
	
	//ExecutionHistorySynopsisModel hsm;
	
	private ChargeAccountingModel chargeModel;
	
	private List<ExecutionUpdater> listeners;
	
	/** The updater.*/
	private DefaultExecutionUpdater dux;

	private LogGenerator logger;
	

	/** Create a DefaultExecutionUpdateManager.*/
	/*public DefaultExecutionUpdateManager(ExecutionHistorySynopsisModel hsm, 
			AccountSynopsisModel proposalAccountModel,
			AccountSynopsisModel tagAccountModel) throws RemoteException  {*/
	
	public DefaultExecutionUpdateManager(SynopticModelProvider smp, ChargeAccountingModel chargeModel) throws RemoteException  {
		// TODO Auto-generated constructor stub
		super();
		this.smp = smp;
		this.chargeModel = chargeModel;
		//this.hsm = hsm;
		//this.proposalAccountModel = proposalAccountModel;
		//this.tagAccountModel = tagAccountModel;
		//updaters = new HashMap<Long, ExecutionUpdater>();
		
		dux = new DefaultExecutionUpdater(smp);	
		listeners = new Vector<ExecutionUpdater>();
		
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate();
		logger.system("SMS").subSystem("SchedulingStatusProvider").srcCompClass("DefaultXU");
		logger.create().extractCallInfo().info().level(1).msg("New Execution updater").send();
	}
	
	/** Obtains the ExecutionUpdater for the specified group execution. 
	 * @param gid The ID of the group for which we want an updater.
	 * @return The ExecutionUpdater handling the specified execution instance.
	 * @throws Exception When something goes awry.
	 */
	public ExecutionUpdater getExecutionUpdater(long gid) throws RemoteException {
		return this;		
	}

	public void addExecutionUpdateListener(ExecutionUpdater l) throws RemoteException {
	
		if (listeners.contains(l))
			return;
		listeners.add(l);	
		
	}

	public void removeExecutionUpdateListener(ExecutionUpdater l) throws RemoteException {

		if (! listeners.contains(l))
			return;
		listeners.remove(l);	
	}
	
	public void notifyListenersGroupCompleted(GroupItem group, long time) {
		
		Iterator<ExecutionUpdater> il = listeners.iterator();
		while (il.hasNext()) {
		try {
			ExecutionUpdater xu = il.next();
			xu.groupExecutionCompleted(group, time, null, null);			
		} catch (Exception e) {
			e.printStackTrace();
			il.remove();
			// removed l
		}
			
		}
		
	}
	
	public void notifyListenersGroupAbandoned(GroupItem group, long time, IExecutionFailureContext efc) {
		
		Iterator<ExecutionUpdater> il = listeners.iterator();
		while (il.hasNext()) {
		try {
			ExecutionUpdater xu = il.next();
			xu.groupExecutionAbandoned(group, time, null, efc, null);		
		} catch (Exception e) {
			e.printStackTrace();
			il.remove();
			// removed l
		}
			
			
		}
		
	}
	
	/**
	 * Notification that group execution was abandonned.
	 * 
	 * @param group
	 *            The ID of the group.
	 * @param time
	 *            The time the group was abandonned.
	 * @param erb
	 *            A bundle of resources used in executing the group.
	 * @param efc
	 *            Information about the failure.
	 * @param qos
	 *            A set of QOS statistics.
	 * @throws RemoteException
	 *             If something goes awry.
	 * @see ngat.sms.ExecutionUpdater#groupExecutionAbandoned(ngat.sms.Ident,
	 *      long, ngat.sms.ExecutionResourceBundle,
	 *      ngat.sms.ExecutionFailureContext)
	 */
	public void groupExecutionAbandoned(GroupItem group, long time, ExecutionResourceBundle erb,
			IExecutionFailureContext efc, Set<IQosMetric> qosMetrics) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().extractCallInfo().info().level(2).msg(
				"Group failed: " + group.getName() + " due to: " + efc + " used: " + erb + " , preparing update.")
				.send();

		// TODO First locate models.
		ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();

		// Do we have a valid HID ?
		long hid = group.getHId();
		if (hid == -999) {
			logger.create().extractCallInfo().info().level(2)
			.msg("Group has no History id, attempting to obtain one").send();	
			try {
				hid = hsm.addHistoryItem(group.getID());	
			} catch (Exception e) {
				e.printStackTrace();
				logger.create().extractCallInfo().info().level(2)
				.msg("Still unable to obtain history item").send();
			}
		} 
		
		hsm.updateHistory(group.getID(), hid, IHistoryItem.EXECUTION_FAILED, time, efc, qosMetrics);

		// Scan the resources used and add accounting entries for these.
		long propId = group.getProposal().getID();
		long semId = -1L;

		AccountSynopsisModel proposalAccountingModel = smp.getProposalAccountSynopsisModel();
		/*try {
			ISemester semester = proposalAccountingModel.getSemesterOfDate(time);
			semId = semester.getID();
		} catch (RemoteException re) {
			// log and bail out - cant do anything now
			// log exception and carry on
			logger.create().extractCallInfo().error().level(1).msg(
					"Error while determining semester id for current semester: " + re).send();
			throw re;
		}*/

		// there may be some resource usage we want to account for here
		Iterator<ExecutionResource> ers = erb.listResources();
		while (ers.hasNext()) {
			ExecutionResource resource = ers.next();
			String resName = resource.getResourceName();
			double resUsage = resource.getResourceUsage();

			logger.create().extractCallInfo().info().level(3).msg("Checking resource: " + resource).send();

			// TODO Find AccModel, extract ERB and update accounts.

			// Find the account belonging to the group's owner (proposal) for
			// THIS semester. Then deduct the resource usage from it...

			//IAccount acc = proposalAccountingModel.findAccount(resName, propId, semId);

			String comment = "Exec for: " + group.getName() + " Hid: " + group.getHId();
			//proposalAccountingModel.modifyConsumed(acc.getID(), resUsage / 3600000.0, comment, "Executor");

			proposalAccountingModel.chargeAccount(propId, resUsage/ 3600000.0, comment, "Executor");
			
			logger.create().extractCallInfo().info().level(3).msg("Updating account: " + resName).send();

		}

		// TODO There is no deduction from the standard account whena group
		// fails - this may change later

		logger.create().extractCallInfo().info().level(3).msg("Completed Group failure update for: " + group.getName())
				.send();

		notifyListenersGroupAbandoned(group, time, efc);
		
	}

	/**
	 * Notification that group execution has completed.
	 * 
	 * @param group
	 *            The ID of the group.
	 * @param time
	 *            The time the group completed.
	 * @param erb
	 *            A bundle of resources used in executing the group.
	 * @param qos
	 *            A set of QOS statistics.
	 * @throws RemoteException
	 * @see ngat.sms.ExecutionUpdater#groupExecutionCompleted(ngat.sms.Ident,
	 *      long, ngat.sms.ExecutionResourceBundle)
	 */
	public void groupExecutionCompleted(GroupItem group, long time, ExecutionResourceBundle erb,
			Set<IQosMetric> qosMetrics) throws RemoteException {

		logger.create().extractCallInfo().info().level(3).msg(
				"Group completed: " + group.getName() + ", preparing update.").send();
		
		ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();

		// Do we have a valid HID ?
		long hid = group.getHId();
		if (hid == -999) {
			logger.create().extractCallInfo().info().level(2)
			.msg("Group has no History id, attempting to obtain one").send();		
			try {
				hid = hsm.addHistoryItem(group.getID());	
			} catch (Exception e) {
				e.printStackTrace();
				logger.create().extractCallInfo().info().level(2)
				.msg("Still unable to obtain history item").send();
			}		
		} 
		
		logger.create().extractCallInfo().info().level(2)
		.msg("Updating execution history...").send();
				
		// first update the history model - this should cache the update
		hsm.updateHistory(group.getID(), hid, IHistoryItem.EXECUTION_SUCCESSFUL, time, null, qosMetrics);

		// Scan the resources used and notified by the executor and add
		// accounting entries for these.

		IProposal proposal = group.getProposal();
		ITag tag = group.getTag();
		
		long propId = (proposal != null ? proposal.getID(): -1);
		long tagId = (tag != null ? tag.getID() : -1);
		long semId = -1L;
		
		AccountSynopsisModel proposalAccountingModel = smp.getProposalAccountSynopsisModel();
		/*try {
			ISemester semester = proposalAccountingModel.getSemesterOfDate(time);
			semId = semester.getID();
		} catch (RemoteException re) {
			// log and bail out - cant do anything now
			// log exception and carry on
			logger.create().extractCallInfo().error().level(1).msg(
					"Error while determining semester id for current semester: " + re).send();
			throw re;
		}*/

		logger.create().extractCallInfo().info().level(2)
			.msg("Updating ERB resources...").send();
		
		try {
			Iterator<ExecutionResource> ers = erb.listResources();
			while (ers.hasNext()) {
				ExecutionResource resource = ers.next();
				String resName = resource.getResourceName();
				double resUsage = resource.getResourceUsage();
				String resDesc = resource.getResourceDescription();

				logger.create().extractCallInfo().info().level(2)
				.msg("Checking ERB resource: " + resource).send();

				// Find AccModel, extract ERB and update accounts.

				// Find the account belonging to the group's owner (proposal)
				// for
				// THIS semester. Then deduct the notified resource usage from
				// it...
				//XAccount acc = null;
				/*try {
					acc = (XAccount) proposalAccountingModel.findAccount(resName, propId, semId);
				} catch (Exception e) {
					// maybe it doesnt exist, 
					// TODO WARNING - FOR NOW WE CANT CREATE VIA THE SYNOPSIS MODEL !!!!
					try {
						acc = new XAccount(resName);
						acc.setChargeable(false);
						acc.setDescription(resDesc);
						long acid = proposalAccountingModel.addAccount(propId, semId, acc);
						acc.setID(acid);
						logger.create().extractCallInfo().info().level(3).msg("Created new resource account: " + acc)
								.send();
					} catch (Exception e2) {
						logger.create().extractCallInfo().info().level(3).msg(
								"Failed to create new resource account: " + resName).send();
						// shagged, so onto next one
						continue;
					}*/

				//}
				// only get here if we dont skip to EOLoop

				String comment = resDesc + ":" + group.getName() + "<" + group.getHId() + ">";
				// TotalExecutionTime:GRB56445<38>
				//proposalAccountingModel.modifyConsumed(acc.getID(), resUsage / 3600000.0, comment, "Executor");
				
				logger.create().extractCallInfo().info().level(2)
					.msg("Updating resource account "+resName+" for "+propId).send();
				
				proposalAccountingModel.chargeAccount(propId, resUsage/ 3600000.0, comment, "Executor");
				logger.create().extractCallInfo().info().level(3).msg("Updated account: " + resName).send();

			}
		} catch (Exception e) {
			// log exception and carry on
			logger.create().extractCallInfo().error().level(1).msg(
					"Error while updating execution resource usage supplied by executor: " + e).send();
		}

		// use the charging model to work out the "cost" of the sequence
		// we may need to extract additional charging parameters...
		// e.g. calculateCost(group.seq, env) ->
		// cost of group under these specific conditions as opposed to any.

		double timeCost = chargeModel.calculateCost(group.getSequence());

		// this is the TOTAL time cost account
		//IAccount allocAcc = proposalAccountingModel.findAccount("allocation", propId, semId);
		String comment = "Billing:" + group.getName() + "<" + group.getHId() + ">";
		//proposalAccountingModel.modifyConsumed(allocAcc.getID(), timeCost / 3600000.0, comment, "Executor");
		
		logger.create().extractCallInfo().info().level(2)
		.msg("Updating proposal time allocation account for "+propId).send();
		
		proposalAccountingModel.chargeAccount(propId, timeCost/ 3600000.0, comment, "Executor");
		logger.create().extractCallInfo().info().level(2).msg("Updated proposal account: allocation").send();
		
		AccountSynopsisModel tagAccountingModel = smp.getTagAccountSynopsisModel();
		// this is the TOTAL time cost account
		//IAccount tallocAcc = tagAccountingModel.findAccount("allocation", tagId, semId);
		// String comment = "Billing:" + group.getName() + "<" + group.getHId()
		// + ">";
		//tagAccountingModel.modifyConsumed(tallocAcc.getID(), timeCost / 3600000.0, comment, "Executor");
		
		logger.create().extractCallInfo().info().level(2)
		.msg("Updating TAG time allocation account for: "+tagId).send();
		
		tagAccountingModel.chargeAccount(tagId, timeCost / 3600000.0, comment, "Executor");
		logger.create().extractCallInfo().info().level(2).msg("Updated TAG account: allocation").send();

/*		// And now for any specific types of time costings - DKPR, BRAV, FIXED,
		ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);
		XSeeingConstraint see = oca.getSeeingConstraint();
		XLunarElevationConstraint lunar = oca.getLunarElevationConstraint();
		String seeCatName = getSeeCatName(see.getSeeingCategory());
		String lunCatName = getLunCatName(lunar.getLunarElevationCategory());
		String lunSeeName = "allocation." + lunCatName + "." + seeCatName;

		// Dont update if they asked for BU or DU
		if (see.getSeeingCategory() != IObservingConstraint.UNCONSTRAINED_SEEING) {
			IAccount condAcc = proposalAccountingModel.findAccount(lunSeeName, propId, semId);
			String ccomment = "Billing:" + group.getName() + "<" + group.getHId() + ">";
			proposalAccountingModel.modifyConsumed(condAcc.getID(), timeCost / 3600000.0, ccomment, "Executor");
			logger.create().extractCallInfo().info().level(3).msg("Updating account: " + condAcc).send();
		}

		// Dont update if they asked for BU or DU
		if (see.getSeeingCategory() != IObservingConstraint.UNCONSTRAINED_SEEING) {
			IAccount tcondAcc = tagAccountingModel.findAccount(lunSeeName, tagId, semId);
			String ccomment = "Billing:" + group.getName() + "<" + group.getHId() + ">";
			tagAccountingModel.modifyConsumed(tcondAcc.getID(), timeCost / 3600000.0, ccomment, "Executor");
			logger.create().extractCallInfo().info().level(3).msg("Updating account: " + tcondAcc).send();
		}

		// Fixed time accounting for Proposal
		ITimingConstraint timing = group.getTimingConstraint();
		if (timing instanceof XFixedTimingConstraint) {

			IAccount fixedAcc = proposalAccountingModel.findAccount("allocation.fixed", propId, semId);
			String fcomment = "Billing:" + group.getName() + "<" + group.getHId() + ">";
			proposalAccountingModel.modifyConsumed(fixedAcc.getID(), timeCost / 3600000.0, fcomment, "Executor");
			logger.create().extractCallInfo().info().level(3).msg("Updating account: " + fixedAcc).send();

		}

		// Fixed time accounting for TAG.
		if (timing instanceof XFixedTimingConstraint) {

			IAccount tfixedAcc = tagAccountingModel.findAccount("allocation.fixed", tagId, semId);
			String fcomment = "Billing:" + group.getName() + "<" + group.getHId() + ">";
			tagAccountingModel.modifyConsumed(tfixedAcc.getID(), timeCost / 3600000.0, fcomment, "Executor");
			logger.create().extractCallInfo().info().level(3).msg("Updating account: " + tfixedAcc).send();

		}
*/
		// TODO we could update priority account shere if they existed
		// allocation.priority.high, medium, low, background etc

		// TODO What about any non-standard additional costs which are specific
		// to this group
		// e.g. bid time - how do we know this ? maybe SchedulingStatusProvider sets these
		// up on despatch
		// so the XUM knows how to associate them to a group-exec ???
		// NOTE: The histID is effectively a unique Group_ExecID

		// TODO now lets inform our program/project delegate if there is one....
		//
		// lookup delegate, pass completion or failure notice to him..
		// maybe also any resource and charging performed....
		//

		// TODO now lets update any listeners that the group succeeded
		//notifyListenersGroupCompleted(group.getID(), time, IHistoryItem.EXECUTION_SUCCESSFUL, noerrcode, noerrmesg);


		logger.create().extractCallInfo().info().level(3).msg("Completed Group success update for: " + group.getName())
				.send();
		
		notifyListenersGroupCompleted(group, time);

	}

	/**
	 * Handle notification of an exposure update.
	 * 
	 * @param group
	 *            The ID of the group.
	 * @param time
	 *            The time the exposure was performed (completed?).
	 * @param expId
	 *            The ID of the specific exposure (observation in old money).
	 * @param fileName
	 *            Name of the image file (may include mount/path/host/url?).
	 * @throws RemoteException
	 */
	public void groupExposureCompleted(GroupItem group, long time, long expId, String fileName) throws RemoteException {

		long hid = group.getHId();

		// TODO This model call is not finalized...
		//hsm.addExposureUpdate(hid, expId, time, fileName);

	}

	/*private String getSeeCatName(int seeCat) {

		switch (seeCat) {
		case IObservingConstraint.GOOD_SEEING:
			return "good";
		case IObservingConstraint.AVERAGE_SEEING:
			return "aver";
		case IObservingConstraint.POOR_SEEING:
			return "poor";
		case IObservingConstraint.UNCONSTRAINED_SEEING:
			return "usab";
		}
		return "poor"; // unknown !

	}

	private String getLunCatName(int lunCat) {
		switch (lunCat) {
		case IObservingConstraint.MOON_BRIGHT:
			return "bright";
		case IObservingConstraint.MOON_DARK:
			return "dark";
		}
		return "unk";
	}*/

	
}
