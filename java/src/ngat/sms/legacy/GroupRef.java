/**
 * 
 */
package ngat.sms.legacy;

import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public class GroupRef {

	GroupItem group;
	
	ExecutionUpdater updater;

	/**
	 * @param group
	 * @param updater
	 */
	public GroupRef(GroupItem group, ExecutionUpdater updater) {
		this.group = group;
		this.updater = updater;
	}

	/**
	 * @return the group
	 */
	public GroupItem getGroup() {
		return group;
	}

	/**
	 * @return the updater
	 */
	public ExecutionUpdater getUpdater() {
		return updater;
	}
	
	
	
}
