/**
 * 
 */
package ngat.sms;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IPhase2Model;
import ngat.phase2.IGroup;
import ngat.phase2.IMutableProposal;
import ngat.phase2.IProgram;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITag;
import ngat.phase2.ITarget;
import ngat.phase2.IUser;
import ngat.phase2.XEphemerisTarget;
import ngat.phase2.XExtraSolarTarget;
import ngat.phase2.XSlaNamedPlanetTarget;
import ngat.phase2.XUser;
import ngat.sms.models.standard.GroupAddedNotification;
import ngat.sms.models.standard.GroupDeletedNotification;
import ngat.sms.models.standard.GroupSequenceUpdatedNotification;
import ngat.sms.models.standard.GroupUpdatedNotification;
import ngat.sms.models.standard.ProposalUpdatedNotification;
import ngat.sms.models.standard.TargetAddedNotification;
import ngat.sms.models.standard.TargetUpdatedNotification;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * @author eng
 * 
 */
public class BaseModelPhase2UpdaterPlugin implements Phase2UpdateProcessor {

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	static SimpleDateFormat udf = new SimpleDateFormat("yyyyMMddS");

	static int filecount = 0;

	private BasicPhase2Cache cache;

	private BaseModelProvider bmp;

	LogGenerator logger;

	PrintStream ps;

	/**
	 * @param cache
	 * @param bmp
	 */
	public BaseModelPhase2UpdaterPlugin(BasicPhase2Cache cache, BaseModelProvider bmp) {
		super();
		this.cache = cache;
		this.bmp = bmp;
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("bmu");

		try {
			ps = new PrintStream(new FileOutputStream("/occ/logs/volatility/vphase2.log", true));
			logger.create().extractCallInfo().info().level(1).msg("Opened volitility logging to: /occ/tmp/vphase2.log")
					.send();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processGroupAddedNotification(java.lang
	 * .Object)
	 */
	public void processGroupAddedNotification(Object update) throws Exception {

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		GroupAddedNotification gan = (GroupAddedNotification) update;

		long pid = gan.getPid();
		IGroup group = gan.getGroup();
		long gid = group.getID();

		GroupItem sgroup = new GroupItem(group, null);

		IProposal proposal = phase2.getProposal(pid);
		sgroup.setProposal(proposal);
		ITag tag = phase2.getTagOfProposal(pid);
		sgroup.setTag(tag);
		IProgram prog = phase2.getProgrammeOfProposal(pid);
		sgroup.setProgram(prog);
		long uid = access.getProposalPI(pid);
		IUser user = access.getUser(uid);// could be null?
		if (user == null) {
			user = new XUser("UNKNOWN");
			// System.err.println("Proposal has no PI !, Assigning to 'The Unknown User'");
		}
		sgroup.setUser(user);

		cache.addGroup(sgroup);

		logger.create().extractCallInfo().info().level(1).msg("Group has been added to cache").send();

		try {
			File f = dump(sgroup);
			ps.println(sdf.format(new Date()) + " ADD_GROUP " + f.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processGroupDeletedNotification(java.lang
	 * .Object)
	 */
	public void processGroupDeletedNotification(Object update) throws Exception {
		
		GroupDeletedNotification gdn = (GroupDeletedNotification) update;

		long gid = gdn.getGid();

		// find original in the table
		GroupItem sgroup = cache.findGroup(gid);
		if (sgroup != null) {
			cache.deletegroup(gid);
			logger.create().extractCallInfo().info().level(1)
			.msg("Group has been removed from cache if found").send();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processGroupSequenceUpdatedNotification
	 * (java.lang.Object)
	 */
	public void processGroupSequenceUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		GroupSequenceUpdatedNotification gsun = (GroupSequenceUpdatedNotification) update;

		long gid = gsun.getGid();
		ISequenceComponent sequence = gsun.getSequence();

		GroupItem sgroup = cache.findGroup(gid);

		if (sgroup == null) {
			logger.create().extractCallInfo().info().level(1).msg("No such group in cache");
			return;
		}

		try {
			File f = dump(sgroup);
			ps.print(sdf.format(new Date()) + " UPDATE_SEQ " + f.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// set the group's sequence
		sgroup.setSequence(sequence);

		// link the targets to target table
		cache.linkTargets(sequence);

		logger.create().extractCallInfo().info().level(1).msg("Sequence has been added to cached group").send();

		try {
			File f = dump(sgroup);
			ps.println(" " + f.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processGroupUpdatedNotification(java.lang
	 * .Object)
	 */
	public void processGroupUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		GroupUpdatedNotification gun = (GroupUpdatedNotification) update;

		IGroup group = gun.getGroup();
		long gid = group.getID();

		// find original in the table
		GroupItem sgroup = cache.findGroup(gid);
		if (sgroup == null) {
			// a new group or one that we didnt see before eg
			// deactivated
			sgroup = new GroupItem(group, null);

			IProposal proposal = phase2.getProposalOfGroup(gid);
			long pid = proposal.getID();
			
			// TODO use the master copy of prop, dont create a new copy of it !!!
			IProposal oproposal = cache.findProposal(pid);			
			
			// if the proposal is not there due to being not loaded previously .. load it now but how ??
			if (oproposal == null) {
				cache.addProposal(proposal);
				oproposal = proposal;
			}
		
			sgroup.setProposal(oproposal);
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

			cache.addGroup(sgroup);

			logger.create().extractCallInfo().info().level(1).msg("Group has been added to cache").send();

		}

		try {
			File f = dump(sgroup);
			ps.print(sdf.format(new Date()) + " UPDATE_GROUP " + f.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// copy group parameters across
		sgroup.setGroup(group);

		logger.create().extractCallInfo().info().level(1).msg("Group has been updated").send();

		try {
			File f = dump(sgroup);
			ps.println(" " + f.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processProposalUpdatedNotification(java
	 * .lang.Object)
	 */
	public void processProposalUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		ProposalUpdatedNotification pun = (ProposalUpdatedNotification) update;
		IProposal proposal = pun.getProposal();
		long pid = proposal.getID();

		// find original in the table
		IProposal oproposal = cache.findProposal(pid);
		if (oproposal == null) {
			// a new proposal or one that we didnt see before eg
			// deactivated
			cache.addProposal(proposal);
			// TODO - we may now need to load all the groups for
			// this monkey !

			logger.create().extractCallInfo().info().level(1).msg("Proposal has been added to cache").send();

		} else {

			// copy new details onto existing proposal
			copyProposal((IMutableProposal) proposal, (IMutableProposal) oproposal);

			logger.create().extractCallInfo().info().level(1).msg("Proposal has been updated").send();

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processTargetAddedNotification(java.lang
	 * .Object)
	 */
	public void processTargetAddedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		TargetAddedNotification tan = (TargetAddedNotification) update;

		long pid = tan.getPid();
		ITarget target = tan.getTarget();
		long tid = target.getID();

		// add this target to the table if not already there
		cache.addTarget(target);
		logger.create().extractCallInfo().info().level(1).msg("Target has been added to cache").send();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.Phase2UpdateProcessor#processTargetUpdatedNotification(java.
	 * lang.Object)
	 */
	public void processTargetUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		TargetUpdatedNotification tun = (TargetUpdatedNotification) update;
		ITarget target = tun.getTarget();
		long tid = target.getID();

		ITarget otarget = cache.findTarget(tid);
		if (otarget == null) {
			cache.addTarget(target);
			logger.create().extractCallInfo().info().level(1).msg("Target has been add to cache").send();

		} else {

			copyTarget(target, otarget);
			logger.create().extractCallInfo().info().level(1).msg("Target has been updated").send();

		}
	}

	private void copyProposal(IMutableProposal newProposal, IMutableProposal oldProposal) throws Exception {

		logger.create().block("copyProposal").info().level(1).msg(
				"Over-writing proposal description: old: " + oldProposal + ", new: " + newProposal).send();

		if (oldProposal == null)
			throw new Exception("Old proposal for overlay is null");

		if (newProposal == null)
			throw new Exception("New proposal for overlay is null");

		oldProposal.setActivationDate(newProposal.getActivationDate());
		oldProposal.setExpiryDate(newProposal.getExpiryDate());
		oldProposal.setEnabled(newProposal.isEnabled());
		oldProposal.setPriority(newProposal.getPriority());
		oldProposal.setPriorityOffset(newProposal.getPriorityOffset());
		// Note there are a few things we dont copy over as they dont affect scheduling
		// e.g. setAllowFixedGroups(boolean) is never used.
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

	private File dump(Object obj) throws Exception {

		File file = new File("/occ/logs/volatility/p2update_" + udf.format(new Date()) + "_" + (++filecount) + ".dat");

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.flush();
		oos.writeObject(obj);
		oos.flush();
		oos.close();

		return file;

	}

}
