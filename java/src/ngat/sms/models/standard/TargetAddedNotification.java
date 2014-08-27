/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.ITarget;

/**
 * @author eng
 *
 */
public class TargetAddedNotification extends Phase2UpdateNotification {

	private long pid;
	
	private ITarget target;

	/**
	 * @param pid
	 * @param target
	 */
	public TargetAddedNotification(long pid, ITarget target) {
		this.pid = pid;
		this.target = target;
	}

	/**
	 * @return the pid
	 */
	public long getPid() {
		return pid;
	}

	/**
	 * @return the target
	 */
	public ITarget getTarget() {
		return target;
	}
	
	
	
}
