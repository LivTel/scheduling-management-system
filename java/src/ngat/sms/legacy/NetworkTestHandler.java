/**
 * 
 */
package ngat.sms.legacy;

import ngat.message.SMS.NETWORK_TEST_DONE;
import ngat.message.base.COMMAND;
import ngat.net.JMSExecutionMonitor;
import ngat.net.JMSServerProtocolRequestHandler;

/**
 * @author eng
 *
 */
public class NetworkTestHandler implements JMSServerProtocolRequestHandler {

	/**
	 * 
	 */
	public NetworkTestHandler() {
		System.err.println("Creating Network test handler...");
	}

	/* (non-Javadoc)
	 * @see ngat.net.JMSServerProtocolRequestHandler#handleRequest(ngat.message.base.COMMAND, ngat.net.JMSExecutionMonitor)
	 */
	public void handleRequest(COMMAND command, JMSExecutionMonitor monitor) throws Exception {
		
		NETWORK_TEST_DONE reply = new NETWORK_TEST_DONE("reply");
		reply.setSuccessful(true);
		
		// TODO gather all sorts of useful info and stick it into reply
		
		
		monitor.setReply(reply);
	}

}
