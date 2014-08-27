/**
 * 
 */
package ngat.sms;

/**
 * @author eng
 *
 */
public interface Phase2UpdateProcessor {

	public void processGroupAddedNotification(Object update) throws Exception;
	
	public void processGroupDeletedNotification(Object update) throws Exception;
	
	public void processGroupUpdatedNotification(Object update) throws Exception;
	
	public void processGroupSequenceUpdatedNotification(Object update) throws Exception;

	public void processTargetAddedNotification(Object update) throws Exception;

	public void processTargetUpdatedNotification(Object update) throws Exception;
	
	public void processProposalUpdatedNotification(Object update) throws Exception;
	
}
