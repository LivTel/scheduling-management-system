/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IPhase2Model;
import ngat.phase2.IGroup;
import ngat.phase2.IProgram;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITag;
import ngat.phase2.ITarget;
import ngat.phase2.IUser;
import ngat.phase2.XUser;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class BaseModelLoader implements Phase2Loader{

	/** Base model provider to use. */
	private BaseModelProvider bmp;

	private LogGenerator logger;

	/**
	 * @param bmp
	 */
	public BaseModelLoader(BaseModelProvider bmp) {
		super();
		this.bmp = bmp;
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("bml");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.Phase2Loader#loadPhase2Data(ngat.sms.BasicPhase2Cache)
	 */
	public void loadPhase2Data(BasicPhase2Cache cache) throws Exception {

		long now = System.currentTimeMillis();

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		int nprp = 0;
		int ngrp = 0;
		int nag = 0;
		List proglist = phase2.listProgrammes();
		Iterator iprog = proglist.iterator();
		while (iprog.hasNext()) {

			IProgram program = (IProgram) iprog.next();
			long progId = program.getID();

			logger.create().block("loadPhase2").info().level(1)
					.msg("Load program: [" + progId + "] " + program.getName()).send();

			List targetList = phase2.listTargets(progId);
			Iterator itarg = targetList.iterator();
			while (itarg.hasNext()) {

				ITarget target = (ITarget) itarg.next();
				long targetId = target.getID();

				cache.addTarget(target);

				logger.create().block("loadPhase2").info().level(1)
						.msg("Load target: [" + targetId + "] " + target.getName()).send();
			}

			List proplist = phase2.listProposalsOfProgramme(progId);
			Iterator iprop = proplist.iterator();
			while (iprop.hasNext()) {

				IProposal proposal = (IProposal) iprop.next();
				long propId = proposal.getID();

				logger.create().block("loadPhase2").info().level(1)
						.msg("Load proposal: [" + propId + "] " + proposal.getName()).send();

				if (proposal.getExpiryDate() < now || proposal.getActivationDate() > now + 24 * 3600 * 1000L)
					continue;

				logger.create().block("loadPhase2").info().level(1).msg("Proposal is within activation period").send();

				ITag tag = phase2.getTagOfProposal(propId);

				// TODO on second thoughts this should be rejected if NULL
				if (tag == null) {
					logger.create().block("loadPhase2").info().level(1)
							.msg("Proposal has no associated TAG, assigning UNKNOWN").send();
				} else {
					logger.create().block("loadPhase2").info().level(1).msg("Proposal is in TAG: " + tag.getName())
							.send();
				}

				long uid = access.getProposalPI(propId);
				IUser user = access.getUser(uid);

				// TODO on second thoughts this should be rejected if NULL
				if (user == null) {
					user = new XUser("UNKNOWN");
					logger.create().block("loadPhase2").info().level(1)
							.msg("Proposal has no associated PI, assigning UNKNOWN").send();
				} else {
					logger.create().block("loadPhase2").info().level(1)
							.msg("Proposal has PI: [" + uid + "] " + user.getName()).send();
				}

				cache.addProposal(proposal);
				nprp++;
				logger.create().block("loadPhase2").info().level(1).msg("Proposal has been cached: "+propId).send();
				try {

					// NOTE: only want active and unexpired groups
					// NOTE: groups with no timing-constr will NOT be found or loaded by this
					List glist = phase2.listActiveUnexpiredGroups(propId);
					
					Iterator ig = glist.iterator();
					while (ig.hasNext()) {

						IGroup group = (IGroup) ig.next();
						ngrp++;

						logger.create().block("loadPhase2").info().level(1)
								.msg("Load AU group: [" + group.getID() + "] " + group.getName()).send();

						try {
							ISequenceComponent seq = phase2.getObservationSequenceOfGroup(group.getID());

							// Run thro the sequence and locate any
							// targets/slews - these need linking to
							// their target objects from the target table.

							cache.linkTargets(seq);

							// we have a group
							GroupItem sgroup = new GroupItem(group, seq);
							sgroup.setProposal(proposal);
							sgroup.setProgram(program);
							sgroup.setTag(tag);
							sgroup.setUser(user);

							cache.addGroup(sgroup);
							nag++;
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
		System.err.println("Loaded: " + nprp + " Proposals, " + nag + "/" + ngrp + " Groups in " + (end - now) + "ms");

	}

	public void loadProposal(long propId, BasicPhase2Cache cache) throws Exception {

		long now = System.currentTimeMillis();

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();

		IProposal proposal = phase2.getProposal(propId);

		logger.create().block("loadProposal").info().level(1)
				.msg("Load proposal: [" + propId + "] " + proposal.getName()).send();

		if (proposal.getExpiryDate() < now || proposal.getActivationDate() > now + 24 * 3600 * 1000L)
			return;

		logger.create().block("loadProposal").info().level(1).msg("Proposal is within activation period").send();

		ITag tag = phase2.getTagOfProposal(propId);

		// TODO on second thoughts this should be rejected if NULL
		if (tag == null) {
			logger.create().block("loadProposal").info().level(1)
					.msg("Proposal has no associated TAG, assigning UNKNOWN").send();
		} else {
			logger.create().block("loadProposal").info().level(1).msg("Proposal is in TAG: " + tag.getName()).send();
		}

		long uid = access.getProposalPI(propId);
		IUser user = access.getUser(uid);

		// TODO on second thoughts this should be rejected if NULL
		if (user == null) {
			user = new XUser("UNKNOWN");
			logger.create().block("loadProposal").info().level(1)
					.msg("Proposal has no associated PI, assigning UNKNOWN").send();
		} else {
			logger.create().block("loadProposal").info().level(1).msg("Proposal has PI: [" + uid + "] " + user.getName())
					.send();
		}

		IProgram program = phase2.getProgrammeOfProposal(propId);

		cache.addProposal(proposal);

		logger.create().block("loadProposal").info().level(1).msg("Proposal has been cached").send();
		try {

			// NOTE: only want active and unexpired groups
			List glist = phase2.listActiveUnexpiredGroups(propId);
			// List glist = phase2.listGroups(propId, false);
			Iterator ig = glist.iterator();
			while (ig.hasNext()) {

				IGroup group = (IGroup) ig.next();

				logger.create().block("loadProposal").info().level(1)
						.msg("Load AU group: [" + group.getID() + "] " + group.getName()).send();

				try {
					ISequenceComponent seq = phase2.getObservationSequenceOfGroup(group.getID());

					// Run thro the sequence and locate any
					// targets/slews - these need linking to
					// their target objects from the target table.

					cache.linkTargets(seq);

					// we have a group
					GroupItem sgroup = new GroupItem(group, seq);
					sgroup.setProposal(proposal);
					sgroup.setProgram(program);
					sgroup.setTag(tag);
					sgroup.setUser(user);

					cache.addGroup(sgroup);

				} catch (Exception gx) {
					gx.printStackTrace();
				}
			} // next group

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
