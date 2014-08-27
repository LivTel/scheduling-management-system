/**
 * 
 */
package ngat.sms;

import java.io.Serializable;
import java.util.List;

import ngat.phase2.IGroup;
import ngat.phase2.IProgram;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITag;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.IUser;
import ngat.phase2.XEphemerisTimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;

/**
 * @author eng
 * 
 */
public class GroupItem implements IGroup, Serializable {

	// This version is only for extraction of certain dates mid 2010
    //static final long serialVersionUID = -5583499471016871262L;
	
	// This is the version to use normally
    static final long serialVersionUID = 7653839943013538689L;

	/** An internal group. */
	private IGroup group;

	/** The proposal to which this GroupItem belongs. */
	private IProposal proposal;

	/** The program to which this GroupItem belongs. */
	private IProgram program;

	/** The TAG to which this GroupItem belongs. */
	private ITag tag;

	/** The user to which this GroupItem belongs. */
	private IUser user;

	/** Groups observation sequence. */
	private ISequenceComponent sequence;

	/**
	 * The ID of the execution history item belonging to this group execution
	 * instance - may be changed to a full IHistoryItem in the fullness of time.
	 */
	private long hId;

	/**
	 * @param group
	 *            The IGroup to wrap.
	 * @param sequence
	 *            The group's observing sequence.
	 */
	public GroupItem(IGroup group, ISequenceComponent sequence) {
		this.group = group;
		this.sequence = sequence;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IGroup#getPriority()
	 */
	public int getPriority() {
		// TODO Auto-generated method stub
		return group.getPriority();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IGroup#getTimingConstraint()
	 */
	public ITimingConstraint getTimingConstraint() {
		// TODO Auto-generated method stub
		return group.getTimingConstraint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IGroup#isActive()
	 */
	public boolean isActive() {
		// TODO Auto-generated method stub
		return group.isActive();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IGroup#isUrgent()
	 */
	public boolean isUrgent() {
		return group.isUrgent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IGroup#listObservingConstraints()
	 */
	public List listObservingConstraints() {
		// TODO Auto-generated method stub
		return group.listObservingConstraints();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IGroup#listObservingPreferences()
	 */
	public List listObservingPreferences() {
		// TODO Auto-generated method stub
		return group.listObservingPreferences();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IPhase2Identity#getID()
	 */
	public long getID() {
		// TODO Auto-generated method stub
		return group.getID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.phase2.IPhase2#getName()
	 */
	public String getName() {
		// TODO Auto-generated method stub
		return group.getName();
	}

	/**
	 * @return the proposal to which this GroupItem belongs.
	 */
	public IProposal getProposal() {
		return proposal;
	}

	/**
	 * @param proposal
	 *            the proposal to set
	 */
	public void setProposal(IProposal proposal) {
		this.proposal = proposal;
	}

	/**
	 * @return the program to which this GroupItem belongs.
	 */
	public IProgram getProgram() {
		return program;
	}

	/**
	 * @param program
	 *            the program to set
	 */
	public void setProgram(IProgram program) {
		this.program = program;
	}

	/**
	 * @return the tag to which this GroupItem belongs.
	 */
	public ITag getTag() {
		return tag;
	}

	/**
	 * @param tag
	 *            the tag to set
	 */
	public void setTag(ITag tag) {
		this.tag = tag;
	}

	/**
	 * @return the user to which this GroupItem belongs.
	 */
	public IUser getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(IUser user) {
		this.user = user;
	}

	/**
	 * @return the sequence
	 */
	public ISequenceComponent getSequence() {
		return sequence;
	}

	/**
	 * @param sequence
	 *            the sequence to set
	 */
	public void setSequence(ISequenceComponent sequence) {
		this.sequence = sequence;
	}

	/**
	 * @return the hId
	 */
	public long getHId() {
		return hId;
	}

	/**
	 * @param id
	 *            the hId to set
	 */
	public void setHId(long id) {
		hId = id;
	}

	/**
	 * Update or set the encapsulated group within the GroupItem.
	 * 
	 * @param update
	 */
	public void setGroup(IGroup update) {
		this.group = update;
	}

	public String toString() {
		return "GroupItem: " + group.getName() + " TAG=" + (tag != null ? tag.getName() : "NULL") + " Prop="
				+ (proposal != null ? proposal.getName() : "NULL") + " User="
				+ (user != null ? user.getName() : "NULL") + " Seq=" + sequence + " XID=" + hId;
	}

	public String toPriorityString() {
		String priorityName = "";
		int tp = 0;
		String timingName = "unknown";
		ITimingConstraint timing = this.getTimingConstraint();

		if (timing == null)
			return "?T";
		
		if (timing instanceof XFlexibleTimingConstraint) {
			timingName = "Flex";
		} else if (timing instanceof XMonitorTimingConstraint) {
			timingName = "Mon";
			tp = 1;
		} else if (timing instanceof XEphemerisTimingConstraint) {
			timingName = "Phase";
		} else if (timing instanceof XMinimumIntervalTimingConstraint) {
			timingName = "MinInt";
			tp = 1;
		} else if (timing instanceof XFixedTimingConstraint) {
			timingName = "Fixed";
		}

		double priority = tp;
		if (this.isUrgent()) {
			priorityName += "*";
			priority += 2;
		}

		if (proposal == null)
			return "?P";
		
		switch (proposal.getPriority()) {
		case IProposal.PRIORITY_A:
			priorityName += "A";
			priority += 4;
			break;
		case IProposal.PRIORITY_B:
			priorityName += "B";
			priority += 2;
			break;
		case IProposal.PRIORITY_C:
			priorityName += "C";
			break;
		case IProposal.PRIORITY_Z:
			priorityName += "Z";
			priority -= 10;
			break;
		}
		double po = proposal.getPriorityOffset();
		if (po < 0.0)
			priorityName += "-";
		else if (po > 0.0)
			priorityName += "+";

		priority += po;
		
		return priorityName+"("+po+")";
		
		// *A+(3.4)
	}

}
