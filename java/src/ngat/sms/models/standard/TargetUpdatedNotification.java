/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.ITarget;

/**
 * @author eng
 *
 */
public class TargetUpdatedNotification extends Phase2UpdateNotification{

	private ITarget target;

	/**
	 * @param target
	 */
	public TargetUpdatedNotification(ITarget target) {
		this.target = target;
	}

	/**
	 * @return the target
	 */
	public ITarget getTarget() {
		return target;
	}
	
	
}
