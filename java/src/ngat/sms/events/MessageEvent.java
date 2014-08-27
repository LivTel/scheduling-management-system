/**
 * 
 */
package ngat.sms.events;

/**
 * @author eng
 *
 */
public class MessageEvent extends SchedulingStatus {

	public static final int MESSAGE_TYPE_WARNING = 1;
	
	
	/** A message content.*/
	private String message;
	
	/** type of message.*/
	private int messageType;
	
	
	

	/* (non-Javadoc)
	 * @see ngat.net.telemetry.StatusCategory#getCategoryName()
	 */
	public String getCategoryName() {
		return "MSG";
	}
	
	public String toString() {
		return "MSG: "+ statusTimeStamp+" "+message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}
}
