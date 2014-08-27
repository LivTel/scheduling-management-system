/**
 * 
 */
package ngat.sms.models.standard;

import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IPhase2Model;
import ngat.oss.transport.RemotelyPingable;
import ngat.oss.listeners.*;
import ngat.phase2.IAccessPermission;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.IGroup;
import ngat.phase2.IMutableProposal;
import ngat.phase2.IProgram;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ISlew;
import ngat.phase2.ITag;
import ngat.phase2.ITarget;
import ngat.phase2.ITargetSelector;
import ngat.phase2.IUser;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XEphemerisTarget;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XExtraSolarTarget;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XSlaNamedPlanetTarget;
import ngat.phase2.XSlew;
import ngat.phase2.XTargetSelector;
import ngat.phase2.XUser;
import ngat.sms.GroupItem;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.util.FeasibilityPrescan;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class BasicPhase2CompositeModel extends UnicastRemoteObject implements Phase2CompositeModel,
		Phase2ModelUpdateListener, RemotelyPingable {

	public static final long UPDATE_LIST_POLLING_INTERVAL = 60000L;

	// IPhase2Model phase2;
	// IAccessModel access;

	/** Records updates. */
	private List updateList;

	/** Mapping from prop ID to proposal. */
	Map<Long, IProposal> proposals;

	/** Mapping from group ID to group. */
	Map<Long, GroupItem> groups;

	/** Mapping from target ID to target. */
	Map<Long, ITarget> targets;

	LogGenerator logger;

	/**
	 * @param phase2
	 * @param access
	 */
	// public BasicPhase2CompositeModel(IPhase2Model phase2, IAccessModel
	// access) throws RemoteException {
	public BasicPhase2CompositeModel() throws RemoteException {
		super();
		// this.phase2 = phase2;
		// this.access = access;
		groups = new HashMap<Long, GroupItem>();
		targets = new HashMap<Long, ITarget>();
		proposals = new HashMap<Long, IProposal>();

		updateList = new Vector();

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("pcm");
	}

	public void loadPhase2(IAccessModel access, IPhase2Model phase2) throws Exception {

		long now = System.currentTimeMillis();

		int nusr = 0;
		int nprp = 0;
		int ngrp = 0;
		List proglist = phase2.listProgrammes();
		Iterator iprog = proglist.iterator();
		while (iprog.hasNext()) {

			IProgram program = (IProgram) iprog.next();
			long progId = program.getID();

			logger.create().block("loadPhase2").info().level(1).msg(
					"Load program: [" + progId + "] " + program.getName()).send();

			List targetList = phase2.listTargets(progId);
			Iterator itarg = targetList.iterator();
			while (itarg.hasNext()) {

				ITarget target = (ITarget) itarg.next();
				long targetId = target.getID();

				targets.put(targetId, target);

				logger.create().block("loadPhase2").info().level(1).msg(
						"Load target: [" + targetId + "] " + target.getName()).send();
			}

			List proplist = phase2.listProposalsOfProgramme(progId);
			Iterator iprop = proplist.iterator();
			while (iprop.hasNext()) {

				IProposal proposal = (IProposal) iprop.next();
				long propId = proposal.getID();

				logger.create().block("loadPhase2").info().level(1).msg(
						"Load proposal: [" + propId + "] " + proposal.getName()).send();

				ITag tag = phase2.getTagOfProposal(propId);

				logger.create().block("loadPhase2").info().level(1).msg(
						"Proposal is in TAG: " + (tag != null ? tag.getName() : "NULL")).send();

				// TODO on second thoughts this should be rejected if NULL
				if (tag == null) {
					logger.create().block("loadPhase2").info().level(1).msg(
							"Proposal has no associated TAG, assigning UNKNOWN").send();
				} else {
					logger.create().block("loadPhase2").info().level(1).msg("Proposal is in TAG: " + tag.getName())
							.send();
				}

				long uid = access.getProposalPI(propId);
				IUser user = access.getUser(uid);

				// TODO on second thoughts this should be rejected if NULL
				if (user == null) {
					user = new XUser("UNKNOWN");
					logger.create().block("loadPhase2").info().level(1).msg(
							"Proposal has no PI !, Assigning to 'The Unknown User'").send();
				} else {
					logger.create().block("loadPhase2").info().level(1).msg(
							"Proposal has PI: [" + uid + "] " + user.getName()).send();
				}

				if (proposal.getExpiryDate() < now || proposal.getActivationDate() > now + 24 * 3600 * 1000L)
					continue;

				proposals.put(propId, proposal);
				nprp++;
				logger.create().block("loadPhase2").info().level(1).msg("Proposal has been cached").send();
				try {

					List glist = phase2.listGroups(propId, false);
					Iterator ig = glist.iterator();
					while (ig.hasNext()) {

						IGroup group = (IGroup) ig.next();
						ngrp++;

						logger.create().block("loadPhase2").info().level(1).msg(
								"Load group: [" + group.getID() + "] " + group.getName()).send();

						try {
							ISequenceComponent seq = phase2.getObservationSequenceOfGroup(group.getID());

							// Run thro the sequence and locate any
							// targets/slews - these need linking to
							// their target objects from the target table.

							linkTargets(seq);

							// we have a group
							GroupItem sgroup = new GroupItem(group, seq);
							sgroup.setProposal(proposal);
							sgroup.setProgram(program);
							sgroup.setTag(tag);
							sgroup.setUser(user);

							groups.put(new Long(group.getID()), sgroup);
						} catch (Exception gx) {
							gx.printStackTrace();
						}
					} // next group

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		long end = System.currentTimeMillis();
		System.err.println("Groups list contains: " + groups.size() + " entries");
		System.err.println("Loaded: " + nusr + " Users, " + nprp + " Proposals, " + ngrp + " Groups in " + (end - now)
				+ "ms");

	}

	public void startUpdateMonitor(IAccessModel access, IPhase2Model phase2) {
		UpdateMonitor monitor = new UpdateMonitor(access, phase2);
		monitor.start();
		System.err.println("Started update monitor...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.Phase2CompositeModel#listGroups()
	 */
	public List<GroupItem> listGroups() {
		List<GroupItem> groupList = new Vector<GroupItem>();
		Iterator<Long> ig = groups.keySet().iterator();
		while (ig.hasNext()) {
			long gid = ig.next();
			GroupItem group = (GroupItem) groups.get(gid);
			groupList.add(group);
		}

		return groupList;

	}

	/** Locate any target refs and link em up. */
	private void linkTargets(ISequenceComponent seq) throws Exception {
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
				if (ctarget == null)
					throw new Exception("Target: " + target.getName() + " is not known");
				((XTargetSelector) action).setTarget(ctarget);

			} else if (action instanceof ISlew) {
				ITarget target = ((ISlew) action).getTarget();
				if (target == null)
					throw new Exception("TargetSelector:" + seq.getComponentName() + " had null target");
				// link the target to one from table
				ITarget ctarget = targets.get(target.getID());
				if (ctarget == null)
					throw new Exception("Target: " + target.getName() + " is not known");
				((XSlew) action).setTarget(ctarget);
			}
		}
	}

	/**
	 * Notifies observers that a proposal has been added to the observed
	 * Phase2Model.
	 * 
	 * @param pid
	 *            The ID of the new proposal.
	 * @throws RemoteException
	 *             If anything goes wrong.
	 */
	public void proposalAdded(IProposal proposal) throws RemoteException {
		logger.create().block("proposalAdded").info().level(1).msg(
				"Adding addProposal request to update queue for proposal: pid:" + proposal.getID() + ", name: "
						+ proposal.getName()).send();
		// will need to grab all its content ?
	}

	/**
	 * Notifies observers that a proposal has been deleted from the observed
	 * Phase2Model.
	 * 
	 * @param pid
	 *            The ID of the proposal deleted.
	 * @throws RemoteException
	 *             If anything goes wrong.
	 */
	public void proposalDeleted(long pid) throws RemoteException {

		logger.create().block("proposalDeleted").info().level(1).msg(
				"Adding deleteProposal request to update queue for pid: " + pid).send();
	}

	/**
	 * Notifies observers that a proposal in the observed Phase2Model has been
	 * modified in some way.
	 * 
	 * @param pid
	 *            The ID of the proposal that was modified.
	 * @throws RemoteException
	 *             If anything goes wrong.
	 */
	// / this should be the proposal not just its pid.....
	public void proposalUpdated(IProposal proposal) throws RemoteException {
		logger.create().block("proposalUpdated").info().level(1).msg(
				"Adding updateProposal request to update queue for proposal: pid: " + proposal.getID() + ", name:"
						+ proposal.getName()).send();
		// will need to grab content if we dont already have this -
		// eg the proposal may have been disabled before but now enabled.

		updateList.add(new ProposalUpdatedNotification(proposal));

	}

	/**
	 * Notifies observers that a group in the observed Phase2Model has been
	 * added.
	 * 
	 * @param pid
	 *            The ID of the proposal to which the group was added.
	 * @param group
	 *            The group added.
	 * @throws RemoteException
	 */
	public void groupAdded(long pid, IGroup group) throws RemoteException {
		logger.create().block("groupAdded").info().level(1).msg(
				"Adding addGroup request to update queue for: pid: " + pid + ", group: " + group).send();
		updateList.add(new GroupAddedNotification(pid, group));
	}

	/**
	 * Notifies observers that a group in the observed Phase2Model has been
	 * deleted.
	 * 
	 * @param id
	 *            The ID of the group deleted.
	 * @throws RemoteException
	 */
	public void groupDeleted(long gid) throws RemoteException {

		logger.create().block("groupDeleted").info().level(1).msg(
				"Adding deleteGroup request to update queue for gid: " + gid).send();
	}

	/**
	 * Notifies observers that a group in the observed Phase2Model has been
	 * updated.
	 * 
	 * @param group
	 *            The group added.
	 * @throws RemoteException
	 */
	public void groupUpdated(IGroup group) throws RemoteException {

		logger.create().block("groupUpdated").info().level(1).msg(
				"Adding updateGroup request to update queue for group: " + group).send();

		updateList.add(new GroupUpdatedNotification(group));
	}

	/**
	 * Notifies observers that a target in the observed Phase2Model has been
	 * added.
	 * 
	 * @param pid
	 *            The ID of the program to which the group was added.
	 * @param group
	 *            The group added.
	 * @throws RemoteException
	 */
	public void targetAdded(long pid, ITarget target) throws RemoteException {

		logger.create().block("targetAdded").info().level(1).msg(
				"Adding addTarget request to update queue for pid: " + pid + ", target: " + target).send();

		updateList.add(new TargetAddedNotification(pid, target));
	}

	/**
	 * Notifies observers that a target in the observed Phase2Model has been
	 * deleted.
	 * 
	 * @param id
	 *            The ID of the target deleted.
	 * @throws RemoteException
	 */
	public void targetDeleted(long tid) throws RemoteException {

		logger.create().block("targetDeleted").info().level(1).msg(
				"Adding deleteTarget request to update queue for tid: " + tid).send();

	}

	/**
	 * Notifies observers that a target in the observed Phase2Model has been
	 * updated.
	 * 
	 * @param target
	 *            The target updated.
	 * @throws RemoteException
	 */
	public void targetUpdated(ITarget target) throws RemoteException {

		logger.create().block("targetUpdated").info().level(1).msg(
				"Adding updateTarget request to update queue for target: " + target).send();

		updateList.add(new TargetUpdatedNotification(target));
	}

	/**
	 * Notifies observers that an observation sequence has been added to a group
	 * in the observed Phase2Model.
	 * 
	 * @param gid
	 *            The ID of the group to which the sequence was added.
	 * @param sequence
	 *            The sequence added
	 * @throws RemoteException
	 */
	public void groupObsSequenceAdded(long gid, ISequenceComponent sequence) throws RemoteException {

		logger.create().block("groupObsSequenceAdded").info().level(1).msg(
				"Adding addSequence request to update queue for gid: " + gid + ", seq: " + sequence).send();

		updateList.add(new GroupSequenceUpdatedNotification(gid, sequence));
	}

	/**
	 * Notifies observers that an observation sequence has been deleted from a
	 * group in the observed Phase2Model.
	 * 
	 * @param gid
	 *            The id of the group that the sequence was removed from.
	 * @throws RemoteException
	 */
	public void groupObsSequenceDeleted(long gid) throws RemoteException {

		logger.create().block("groupObsSequenceDeleted").info().level(1).msg(
				"Adding deleteSequence request to update queue for gid: " + gid).send();
	}

	/**
	 * Notifies observers that an observation sequence has been updated in the
	 * observed Phase2Model.
	 * 
	 * @param gid
	 *            The id of the group the sequence belongs to
	 * @param sequence
	 *            The new sequence.
	 * @throws RemoteException
	 */
	public void groupObsSequenceUpdated(long gid, ISequenceComponent sequence) throws RemoteException {

		logger.create().block("groupObsSequenceUpdated").info().level(1).msg(
				"Adding updateSequence request to update queue for gid: " + gid + ", seq: " + sequence).send();

		updateList.add(new GroupSequenceUpdatedNotification(gid, sequence));
	}

	private class UpdateMonitor extends Thread {

		IAccessModel access;
		IPhase2Model phase2;

		int nc = 0;

		/**
		 * @param access
		 * @param phase2
		 */
		public UpdateMonitor(IAccessModel access, IPhase2Model phase2) {
			super();
			this.access = access;
			this.phase2 = phase2;
		}

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
					}

					if (update instanceof GroupAddedNotification) {

						GroupAddedNotification gan = (GroupAddedNotification) update;

						long pid = gan.getPid();
						IGroup group = gan.getGroup();
						long gid = group.getID();

						GroupItem sgroup = new GroupItem(group, null);
						try {
							IProposal proposal = phase2.getProposal(pid);
							sgroup.setProposal(proposal);
							ITag tag = phase2.getTagOfProposal(pid);
							sgroup.setTag(tag);
							long uid = access.getProposalPI(pid);
							IUser user = access.getUser(uid);// could be null?
							if (user == null) {
								user = new XUser("UNKNOWN");
								// System.err.println("Proposal has no PI !, Assigning to 'The Unknown User'");
							}
							sgroup.setUser(user);

							groups.put(group.getID(), sgroup);

							logger.create().block("monitor.run").info().level(1).msg("Group has been added to cache")
									.send();

						} catch (Exception e) {
							e.printStackTrace();
						}

					} else if (update instanceof GroupSequenceUpdatedNotification) {
						GroupSequenceUpdatedNotification gsun = (GroupSequenceUpdatedNotification) update;

						long gid = gsun.getGid();
						ISequenceComponent sequence = gsun.getSequence();

						GroupItem sgroup = (GroupItem) groups.get(gid);
						if (sgroup == null)
							continue;

						sgroup.setSequence(sequence);

						logger.create().block("monitor.run").info().level(1).msg(
								"Sequence has been added to cached group").send();

					} else if (update instanceof GroupUpdatedNotification) {
						GroupUpdatedNotification gun = (GroupUpdatedNotification) update;

						IGroup group = gun.getGroup();
						long gid = group.getID();

						// find original in the table
						GroupItem sgroup = (GroupItem) groups.get(gid);
						if (sgroup == null) {
							// a new group or one that we didnt see before eg
							// deactivated
							sgroup = new GroupItem(group, null);
							try {
								IProposal proposal = phase2.getProposalOfGroup(gid);
								long pid = proposal.getID();
								sgroup.setProposal(proposal);
								ITag tag = phase2.getTagOfProposal(pid);
								sgroup.setTag(tag);
								long uid = access.getProposalPI(pid);
								IUser user = access.getUser(uid);// could be
								// null?
								if (user == null) {
									user = new XUser("UNKNOWN");
									// System.err.println("Proposal has no PI !, Assigning to 'The Unknown User'");
								}
								sgroup.setUser(user);
								ISequenceComponent seq = phase2.getObservationSequenceOfGroup(gid);
								sgroup.setSequence(seq);

								groups.put(group.getID(), sgroup);

								logger.create().block("monitor.run").info().level(1).msg(
										"Group has been added to cache").send();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						// copy group parameters across
						sgroup.setGroup(group);

						logger.create().block("monitor.run").info().level(1).msg("Group has been updated").send();

					} else if (update instanceof TargetAddedNotification) {
						TargetAddedNotification tan = (TargetAddedNotification) update;

						long pid = tan.getPid();
						ITarget target = tan.getTarget();
						long tid = target.getID();

						// add this target to the table if not already there
						targets.put(tid, target);

						logger.create().block("monitor.run").info().level(1).msg("Target has been added to cache")
								.send();

					} else if (update instanceof TargetUpdatedNotification) {
						TargetUpdatedNotification tun = (TargetUpdatedNotification) update;
						ITarget target = tun.getTarget();
						long tid = target.getID();

						ITarget otarget = (ITarget) targets.get(tid);
						if (otarget == null) {
							targets.put(tid, target);

							logger.create().block("monitor.run").info().level(1).msg("Target has been add to cache")
									.send();

						} else {
							try {
								copyTarget(target, otarget);
								logger.create().block("monitor.run").info().level(1).msg("Target has been updated")
										.send();

							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					} else if (update instanceof ProposalUpdatedNotification) {
						ProposalUpdatedNotification pun = (ProposalUpdatedNotification) update;
						IProposal proposal = pun.getProposal();
						long pid = proposal.getID();

						// find original in the table
						IProposal oproposal = (IProposal) proposals.get(pid);
						if (oproposal == null) {
							// a new proposal or one that we didnt see before eg
							// deactivated
							proposals.put(pid, proposal);
							// TODO - we may now need to load all the groups for
							// this monkey !

							logger.create().block("monitor.run").info().level(1)
									.msg("Proposal has been added to cache").send();

						} else {
							try {
								// copy new details onto existing proposal
								copyProposal((IMutableProposal) proposal, (IMutableProposal) oproposal);

								logger.create().block("monitor.run").info().level(1).msg("Proposal has been updated")
										.send();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					}
				}

				try {
					Thread.sleep(UPDATE_LIST_POLLING_INTERVAL);
				} catch (InterruptedException ix) {
				}

			}
		}

		private void copyProposal(IMutableProposal newProposal, IMutableProposal oldProposal) throws Exception {

			logger.create().block("copyProposal").info().level(1).msg(
					"Over-writing proposal description: old: " + oldProposal + ", new: " + newProposal).send();

			if (oldProposal == null)
				throw new Exception("Old proposalfor overlay is null");

			if (newProposal == null)
				throw new Exception("New proposal for overlay is null");

			oldProposal.setActivationDate(newProposal.getActivationDate());
			oldProposal.setExpiryDate(newProposal.getExpiryDate());
			oldProposal.setEnabled(newProposal.isEnabled());
			oldProposal.setPriority(newProposal.getPriority());
			oldProposal.setPriorityOffset(newProposal.getPriorityOffset());

		}

		private void copyTarget(ITarget newtarget, ITarget oldtarget) throws Exception {

			logger.create().block("copyTarget").info().level(1).msg(
					"Over-writing target description: old: " + oldtarget + ", new: " + newtarget).send();

			if (oldtarget == null)
				throw new Exception("Old target for overlay is null");

			if (newtarget == null)
				throw new Exception("New target for overlay is null");

			if (oldtarget.getClass() != newtarget.getClass())
				throw new Exception("Cannot overlay " + oldtarget.getClass().getName() + " with "
						+ newtarget.getClass().getName());

			if (oldtarget instanceof XExtraSolarTarget) {
				// replace params

				XExtraSolarTarget oldstar = (XExtraSolarTarget) oldtarget;
				XExtraSolarTarget newstar = (XExtraSolarTarget) newtarget;
				oldstar.setRa(newstar.getRa());
				oldstar.setDec(newstar.getDec());
				oldstar.setEpoch(newstar.getEpoch());
				oldstar.setFrame(newstar.getFrame());
				oldstar.setName(newstar.getName());
				oldstar.setParallax(newstar.getParallax());
				oldstar.setPmRA(newstar.getPmRA());
				oldstar.setPmDec(newstar.getPmDec());
				oldstar.setRadialVelocity(newstar.getRadialVelocity());

			} else if (oldtarget instanceof XEphemerisTarget) {

				XEphemerisTarget oldephem = (XEphemerisTarget) oldtarget;
				XEphemerisTarget newephem = (XEphemerisTarget) newtarget;

				oldephem.setName(newephem.getName());
				oldephem.setEphemerisTrack(newephem.getEphemerisTrack());

			} else if (oldtarget instanceof XSlaNamedPlanetTarget) {

				((XSlaNamedPlanetTarget) oldtarget).setName(newtarget.getName());
				((XSlaNamedPlanetTarget) oldtarget).setIndex(((XSlaNamedPlanetTarget) newtarget).getIndex());

			} else {
				throw new Exception("Unknown target class: " + oldtarget.getClass().getName());
			}

		}

	}

	public void ping() throws RemoteException {
		logger.create().block("ping").info().level(3).msg("Ping received").send();
	}

}
