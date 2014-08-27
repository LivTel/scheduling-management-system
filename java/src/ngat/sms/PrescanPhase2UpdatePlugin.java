/**
 * 
 */
package ngat.sms;

import javax.sound.midi.SysexMessage;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IPhase2Model;
import ngat.phase2.IGroup;
import ngat.phase2.IProgram;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITag;
import ngat.phase2.IUser;
import ngat.phase2.XUser;
import ngat.sms.models.standard.GroupAddedNotification;
import ngat.sms.models.standard.GroupSequenceUpdatedNotification;
import ngat.sms.util.FeasibilityPrescan;
import ngat.sms.util.PrescanEntry;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class PrescanPhase2UpdatePlugin implements Phase2UpdateProcessor {

	private BasicPhase2Cache cache;

	private FeasibilityPrescan prescan;

	private BaseModelProvider bmp;
	
	private LogGenerator logger;
	
	/**
	 * @param cache
	 * @param prescan
	 */
	public PrescanPhase2UpdatePlugin(BasicPhase2Cache cache, BaseModelProvider bmp, FeasibilityPrescan prescan) {
		super();
		this.cache = cache;
		this.bmp = bmp;
		this.prescan = prescan;
		
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
		.srcCompId("psu");
	
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processGroupAddedNotification(java.lang.Object)
	 */
	public void processGroupAddedNotification(Object update) throws Exception {

		IPhase2Model phase2 = bmp.getPhase2Model();
		IAccessModel access = bmp.getAccessModel();
		
		GroupAddedNotification gan = (GroupAddedNotification) update;

		long pid = gan.getPid();
		IGroup group = gan.getGroup();
		long gid = group.getID();

		GroupItem sgroup = new GroupItem(group, null);

		// TODO How the bejabbers do we get hold of the prop,prog,tag,user inof ????
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
		
		logger.create().extractCallInfo().info().level(1)
			.msg("Group has been added to cache and will shortly be tested for feasibility").send();
				
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processGroupDeletedNotification(java.lang.Object)
	 */
	public void processGroupDeletedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub
		logger.create().extractCallInfo().info().level(1).msg("Exec: "+update).send();
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processGroupSequenceUpdatedNotification(java.lang.Object)
	 */
	public void processGroupSequenceUpdatedNotification(Object update) throws Exception {
		
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
		
		// test the group now we have its sequence
		PrescanEntry pse = prescan.prescan(sgroup, System.currentTimeMillis(), 60*1000L);
		
		logger.create().extractCallInfo().info().level(1)
		.msg("Prescan completed: "+pse).send();
		
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processGroupUpdatedNotification(java.lang.Object)
	 */
	public void processGroupUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub
		logger.create().extractCallInfo().info().level(1).msg("Exec: "+update).send();
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processProposalUpdatedNotification(java.lang.Object)
	 */
	public void processProposalUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub
		logger.create().extractCallInfo().info().level(1).msg("Exec: "+update).send();
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processTargetAddedNotification(java.lang.Object)
	 */
	public void processTargetAddedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub
		logger.create().extractCallInfo().info().level(1).msg("Exec: "+update).send();
	}

	/* (non-Javadoc)
	 * @see ngat.sms.Phase2UpdateProcessor#processTargetUpdatedNotification(java.lang.Object)
	 */
	public void processTargetUpdatedNotification(Object update) throws Exception {
		// TODO Auto-generated method stub
		logger.create().extractCallInfo().info().level(1).msg("Exec: "+update).send();
	}

}
