/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.IGroup;

/**
 * @author eng
 *
 */
public class GroupAddedNotification extends Phase2UpdateNotification {

	private long pid;
	
	private IGroup group;

	/**
	 * @param pid
	 * @param group
	 */
	public GroupAddedNotification(long pid, IGroup group) {
		this.pid = pid;
		this.group = group;
	}

	/**
	 * @return the pid
	 */
	public long getPid() {
		return pid;
	}

	/**
	 * @return the group
	 */
	public IGroup getGroup() {
		return group;
	}
		
	
}
