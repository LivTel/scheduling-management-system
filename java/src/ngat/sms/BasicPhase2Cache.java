/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.oss.listeners.Phase2ModelUpdateListener;
import ngat.oss.monitor.Phase2Monitor;
import ngat.oss.transport.RemotelyPingable;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.IGroup;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ISlew;
import ngat.phase2.ITarget;
import ngat.phase2.ITargetSelector;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XSlew;
import ngat.phase2.XTargetSelector;
import ngat.sms.models.standard.GroupAddedNotification;
import ngat.sms.models.standard.GroupDeletedNotification;
import ngat.sms.models.standard.GroupSequenceUpdatedNotification;
import ngat.sms.models.standard.GroupUpdatedNotification;
import ngat.sms.models.standard.Phase2UpdateNotification;
import ngat.sms.models.standard.ProposalUpdatedNotification;
import ngat.sms.models.standard.TargetAddedNotification;
import ngat.sms.models.standard.TargetUpdatedNotification;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class BasicPhase2Cache extends UnicastRemoteObject implements Phase2CompositeModel, Phase2Monitor,
		Phase2ModelUpdateListener, RemotelyPingable {

	public static final long UPDATE_LIST_POLLING_INTERVAL = 60000L;

	/** Records updates. */
	private List updateList;

	/** List of listeners. */
	private List<Phase2ModelUpdateListener> listeners;

	/** Mapping from prop ID to proposal. */
	Map<Long, IProposal> proposals;

	/** Mapping from group ID to group. */
	Map<Long, GroupItem> groups;

	/** Mapping from target ID to target. */
	Map<Long, ITarget> targets;

	LogGenerator logger;

	private String name;

	/**
	 * @throws RemoteException
	 */
	public BasicPhase2Cache(String name) throws RemoteException {
		super();
		this.name = name;
		groups = new HashMap<Long, GroupItem>();
		targets = new HashMap<Long, ITarget>();
		proposals = new HashMap<Long, IProposal>();

		updateList = new Vector();
		listeners = new Vector<Phase2ModelUpdateListener>();

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId(name);
	}

	public List<GroupItem> listGroups() throws RemoteException {
		List<GroupItem> groupList = new Vector<GroupItem>();
		Iterator<Long> ig = groups.keySet().iterator();
		while (ig.hasNext()) {
			long gid = ig.next();
			GroupItem group = (GroupItem) groups.get(gid);
			groupList.add(group);
		}

		return groupList;
	}

	/**
	 * Instruct the cache to load data in using the supplied loader.
	 * 
	 * Usage:-
	 * 
	 * MyLoader myloader = new MyLoader(new MyDataSource()); Phase2Cache cache =
	 * new Phase2Cache(); cache.loadCache(myloader);
	 * 
	 * */
	public void loadCache(Phase2Loader loader) throws Exception {
		loader.loadPhase2Data(this);
	}

	public void startUpdateMonitor(Phase2UpdateProcessor processor) {
		UpdateMonitor monitor = new UpdateMonitor(processor);
		monitor.start();

		logger.create().extractCallInfo().info().level(1).msg(
				"Started update monitor using: " + processor.getClass().getName()).send();
	}

	public void addProposal(IProposal proposal) throws Exception {
		proposals.put(proposal.getID(), proposal);
	}

	public IProposal findProposal(long pid) throws Exception {
		return proposals.get(pid);
	}

	public void addGroup(GroupItem group) throws Exception {
		groups.put(group.getID(), group);
	}

	public void deletegroup(long gid) throws Exception {
		if (groups.containsKey(gid))
			groups.remove(gid);
		
	}
	
	public GroupItem findGroup(long gid) throws Exception {
		return groups.get(gid);
	}

	public void addTarget(ITarget target) throws Exception {
		targets.put(target.getID(), target);
	}

	public ITarget findTarget(long tid) throws Exception {
		return targets.get(tid);
	}

	/** Locate any target refs and link em up. */
	public void linkTargets(ISequenceComponent seq) throws Exception {
		if (seq == null) {
			logger.create().block("linkTargets").info().level(1).msg("Group sequence is null, no targets to link")
					.send();
			return;
		}
		if (seq instanceof XIteratorComponent) {
			// extract from each sub-element

			XIteratorComponent xit = (XIteratorComponent) seq;
			List list = xit.listChildComponents();
			Iterator ic = list.iterator();
			while (ic.hasNext()) {
				ISequenceComponent cseq = (ISequenceComponent) ic.next();
				linkTargets(cseq);
			}
		} else if (seq instanceof XBranchComponent) {
			// there should be no targets in here !

		} else {
			// this is an executive - only check slew and target-select

			XExecutiveComponent xec = (XExecutiveComponent) seq;
			IExecutiveAction action = xec.getExecutiveAction();

			if (action instanceof ITargetSelector) {
				ITarget target = ((ITargetSelector) action).getTarget();
				if (target == null)
					throw new Exception("TargetSelector:" + seq.getComponentName() + " had null target");
				// link the target to one from table
				ITarget ctarget = targets.get(target.getID());
				if (ctarget == null) {
					// add this as a new target
					addTarget(target);
					ctarget = target;
				}
					//throw new Exception("Target: " + target.getName() + " is not known");
				((XTargetSelector) action).setTarget(ctarget);

			} else if (action instanceof ISlew) {
				ITarget target = ((ISlew) action).getTarget();
				if (target == null)
					throw new Exception("TargetSelector:" + seq.getComponentName() + " had null target");
				// link the target to one from table
				ITarget ctarget = targets.get(target.getID());
				if (ctarget == null) {
					// add this as a new target
					addTarget(target);
					ctarget = target;
				}
					//throw new Exception("Target: " + target.getName() + " is not known");
				((XSlew) action).setTarget(ctarget);


			}
		}
	}

	
	// -----------------------------
	//  Callbacks from Base model.
	// -----------------------------
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.listeners.Phase2ModelUpdateListener#groupAdded(long,
	 * ngat.phase2.IGroup)
	 */
	public void groupAdded(long pid, IGroup group) throws RemoteException {
		// TODO Auto-generated method stub
		logger.create().block("groupAdded").info().level(1).msg(
				"Adding addGroup request to update queue for: pid: " + pid + ", group: " + group).send();
		updateList.add(new GroupAddedNotification(pid, group));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.listeners.Phase2ModelUpdateListener#groupDeleted(long)
	 */
	public void groupDeleted(long gid) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("groupDeleted").info().level(1).msg(
				"Adding deleteGroup request to update queue for gid: " + gid).send();
		updateList.add(new GroupDeletedNotification(gid));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#groupObsSequenceAdded(long,
	 * ngat.phase2.ISequenceComponent)
	 */
	public void groupObsSequenceAdded(long gid, ISequenceComponent sequence) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("groupObsSequenceAdded").info().level(1).msg(
				"Adding addSequence request to update queue for gid: " + gid + ", seq: " + sequence).send();

		updateList.add(new GroupSequenceUpdatedNotification(gid, sequence));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#groupObsSequenceDeleted(
	 * long)
	 */
	public void groupObsSequenceDeleted(long gid) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("groupObsSequenceDeleted").info().level(1).msg(
				"Adding deleteSequence request to update queue for gid: " + gid).send();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#groupObsSequenceUpdated(
	 * long, ngat.phase2.ISequenceComponent)
	 */
	public void groupObsSequenceUpdated(long gid, ISequenceComponent sequence) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("groupObsSequenceUpdated").info().level(1).msg(
				"Adding updateSequence request to update queue for gid: " + gid + ", seq: " + sequence).send();

		updateList.add(new GroupSequenceUpdatedNotification(gid, sequence));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#groupUpdated(ngat.phase2
	 * .IGroup)
	 */
	public void groupUpdated(IGroup group) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("groupUpdated").info().level(1).msg(
				"Adding updateGroup request to update queue for group: " + group).send();

		updateList.add(new GroupUpdatedNotification(group));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#proposalAdded(ngat.phase2
	 * .IProposal)
	 */
	public void proposalAdded(IProposal proposal) throws RemoteException {
		// TODO Auto-generated method stub
		logger.create().block("proposalAdded").info().level(1).msg(
				"Adding addProposal request to update queue for proposal: pid:" + proposal.getID() + ", name: "
						+ proposal.getName()).send();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.listeners.Phase2ModelUpdateListener#proposalDeleted(long)
	 */
	public void proposalDeleted(long pid) throws RemoteException {
		// TODO Auto-generated method stub
		logger.create().block("proposalDeleted").info().level(1).msg(
				"Adding deleteProposal request to update queue for pid: " + pid).send();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#proposalUpdated(ngat.phase2
	 * .IProposal)
	 */
	public void proposalUpdated(IProposal proposal) throws RemoteException {
		// TODO Auto-generated method stub
		logger.create().block("proposalUpdated").info().level(1).msg(
				"Adding updateProposal request to update queue for proposal: pid: " + proposal.getID() + ", name:"
						+ proposal.getName()).send();
		// will need to grab content if we dont already have this -
		// eg the proposal may have been disabled before but now enabled.

		updateList.add(new ProposalUpdatedNotification(proposal));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.listeners.Phase2ModelUpdateListener#targetAdded(long,
	 * ngat.phase2.ITarget)
	 */
	public void targetAdded(long pid, ITarget target) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("targetAdded").info().level(1).msg(
				"Adding addTarget request to update queue for pid: " + pid + ", target: " + target).send();

		updateList.add(new TargetAddedNotification(pid, target));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.listeners.Phase2ModelUpdateListener#targetDeleted(long)
	 */
	public void targetDeleted(long tid) throws RemoteException {
		// TODO Auto-generated method stub

		logger.create().block("targetDeleted").info().level(1).msg(
				"Adding deleteTarget request to update queue for tid: " + tid).send();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.oss.listeners.Phase2ModelUpdateListener#targetUpdated(ngat.phase2
	 * .ITarget)
	 */
	public void targetUpdated(ITarget target) throws RemoteException {
		// TODO Auto-generated method stub
		logger.create().block("targetUpdated").info().level(1).msg(
				"Adding updateTarget request to update queue for target: " + target).send();

		updateList.add(new TargetUpdatedNotification(target));
	}

	private class UpdateMonitor extends Thread {

		private Phase2UpdateProcessor processor;

		/**
		 * @param processor
		 */
		private UpdateMonitor(Phase2UpdateProcessor processor) {
			super();
			this.processor = processor;
		}

		/** Keep track of runs. */
		int nc = 0;

		public void run() {

			while (true) {
				nc++;

				// we only log the test every 5 cycles, but actual updates are
				// all recorded
				if (nc % 5 == 0)
					logger.create().block("monitor.run").info().level(1).msg("Checking updatelist").send();
				// check for updates..
				while (!updateList.isEmpty()) {

					logger.create().block("monitor.run").info().level(1).msg(
							"Update list contains " + updateList.size() + " items for processing").send();

					Object update = updateList.remove(0);

					if (update != null) {
						logger.create().block("monitor.run").info().level(1).msg(
								"Processing update class: " + update.getClass().getSimpleName()).send();

						try {
							if (update instanceof GroupAddedNotification) {

								processor.processGroupAddedNotification(update);

							} else if (update instanceof GroupSequenceUpdatedNotification) {

								processor.processGroupSequenceUpdatedNotification(update);

							} else if (update instanceof GroupUpdatedNotification) {

								processor.processGroupUpdatedNotification(update);
								
							} else if (update instanceof GroupDeletedNotification) {
								
								processor.processGroupDeletedNotification(update);

							} else if (update instanceof TargetAddedNotification) {

								processor.processTargetAddedNotification(update);

							} else if (update instanceof TargetUpdatedNotification) {

								processor.processTargetUpdatedNotification(update);

							} else if (update instanceof ProposalUpdatedNotification) {

								processor.processProposalUpdatedNotification(update);

							}
							// } else if (update instanceof
							// ProposalAddedNotification {
						} catch (Exception e) {
							// error during update ....
							logger.create().block("monitor.run").info().level(1).msg(
									"Error while processing update class: " + update.getClass().getSimpleName()).send();
							e.printStackTrace();
						}

						if (update instanceof Phase2UpdateNotification)
							notifyListenersPhase2Update((Phase2UpdateNotification) update);

					} // update not null
				} // next update in list

				try {
					Thread.sleep(UPDATE_LIST_POLLING_INTERVAL);
				} catch (InterruptedException ix) {
				}

			} // keep running

		} // run()

	} // [UpdateMonitor]

	public void addPhase2UpdateListener(Phase2ModelUpdateListener l) throws RemoteException {
		if (listeners.contains(l))
			return;
		listeners.add(l);
		logger.create().extractCallInfo().info().level(1).msg("Adding listener: " + l).send();
	}

	public void removePhase2UpdateListener(Phase2ModelUpdateListener l) throws RemoteException {
		if (!listeners.contains(l))
			return;
		listeners.remove(l);
		logger.create().extractCallInfo().info().level(1).msg("Removing listener: " + l).send();
	}

	/** Notify listeners of a phase2 update event. */
	public void notifyListenersPhase2Update(Phase2UpdateNotification event) {
		logger.create().extractCallInfo().info().level(1).msg(
				"Phase2 update event: " + event + ", notifying upto: " + listeners.size() + " listeners").send();

		Iterator<Phase2ModelUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			Phase2ModelUpdateListener l = il.next();
			try {
				// TODO BOLLOCKS - This is where I wish we had gone for
				// Phase2UpdateEvents !!
				// /l.phase2UpdateEvent(event);
				if (event instanceof GroupAddedNotification) {
					GroupAddedNotification gan = (GroupAddedNotification) event;
					l.groupAdded(gan.getPid(), gan.getGroup());
				} else if (event instanceof GroupUpdatedNotification) {
					GroupUpdatedNotification gun = (GroupUpdatedNotification) event;
					l.groupUpdated(gun.getGroup());
				} else if (event instanceof TargetAddedNotification) {
					TargetAddedNotification tan = (TargetAddedNotification) event;
					l.targetAdded(tan.getPid(), tan.getTarget());
				} else if (event instanceof TargetUpdatedNotification) {
					TargetUpdatedNotification tun = (TargetUpdatedNotification) event;
					l.targetUpdated(tun.getTarget());
				} else if (event instanceof GroupSequenceUpdatedNotification) {
					GroupSequenceUpdatedNotification gsun = (GroupSequenceUpdatedNotification) event;
					l.groupObsSequenceUpdated(gsun.getGid(), gsun.getSequence());
				} else if (event instanceof ProposalUpdatedNotification) {
					ProposalUpdatedNotification pun = (ProposalUpdatedNotification) event;
					l.proposalUpdated(pun.getProposal());
				}
			} catch (Exception e) {
				logger.create().extractCallInfo().warn().level(3).msg("Removing unresponsive listener: " + l).send();
				il.remove();
			}
		}

	}

	public void ping() throws RemoteException {
		logger.create().block("ping").info().level(3).msg("Ping received").send();
	}
} // [Phase2Cache]
