/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.ISequenceComponent;

/**
 * @author eng
 *
 */
public class GroupSequenceUpdatedNotification extends Phase2UpdateNotification {
	
	private long gid;
	
	private ISequenceComponent sequence;

	/**
	 * @param gid
	 * @param sequence
	 */
	public GroupSequenceUpdatedNotification(long gid, ISequenceComponent sequence) {
		this.gid = gid;
		this.sequence = sequence;
	}

	/**
	 * @return the gid
	 */
	public long getGid() {
		return gid;
	}

	/**
	 * @return the sequence
	 */
	public ISequenceComponent getSequence() {
		return sequence;
	}
	
}
