package ngat.sms.models.standard;

import ngat.phase2.IGroup;

public class GroupDeletedNotification extends Phase2UpdateNotification {

	private long gid;

	/**
	 * @param group
	 */
	public GroupDeletedNotification(long gid) {
		this.gid = gid;
	}

	/**
	 * @return the group
	 */
	public long getGid() {
		return gid;
	}
	
}
