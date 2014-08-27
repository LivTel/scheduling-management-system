/**
 * 
 */
package ngat.sms;

import ngat.phase2.ITimePeriod;

/** A disruption by fixed group event.
 * @author eng
 *
 */
public class FixedGroupDisruptor extends Disruptor {

	/** the fixed group.*/
	private GroupItem group;

	/**
	 * @param disruptorName
	 * @param disruptorClass
	 * @param period
	 * @param group
	 */
	public FixedGroupDisruptor(ITimePeriod period, GroupItem group) {
		super("FixedGroup", "FIXED_GROUP", period);
		this.group = group;
	}

	public GroupItem getGroup() {
		return group;
	}

	public void setGroup(GroupItem group) {
		this.group = group;
	}
	
	
}
