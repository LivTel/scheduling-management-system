/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.IGroup;

/**
 * @author eng
 *
 */
public class GroupUpdatedNotification extends Phase2UpdateNotification{
	
	private IGroup group;

	/**
	 * @param group
	 */
	public GroupUpdatedNotification(IGroup group) {
		this.group = group;
	}

	/**
	 * @return the group
	 */
	public IGroup getGroup() {
		return group;
	}

}
