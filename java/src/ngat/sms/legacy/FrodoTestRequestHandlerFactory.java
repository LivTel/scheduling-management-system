/**
 * 
 */
package ngat.sms.legacy;

import ngat.message.SMS.EXECUTION_UPDATE;
import ngat.message.SMS.NETWORK_TEST;
import ngat.message.SMS.SCHEDULE_REQUEST;
import ngat.message.base.COMMAND;
import ngat.net.JMSServerProtocolRequestHandler;
import ngat.net.JMSServerProtocolRequestHandlerFactory;

/**
 * @author eng
 *
 */
public class FrodoTestRequestHandlerFactory implements JMSServerProtocolRequestHandlerFactory {

	/* (non-Javadoc)
	 * @see ngat.net.JMSServerProtocolRequestHandlerFactory#createRequestHandler(ngat.message.base.COMMAND)
	 */
	public JMSServerProtocolRequestHandler createRequestHandler(COMMAND command) {
		System.err.println("SMSHFactory: Recieved: "+command);
		
		if (command instanceof SCHEDULE_REQUEST) {

			return new FrodoScheduleRequestHandler();

		} else if (command instanceof EXECUTION_UPDATE) {
			
		    return new FakeExecutionUpdateHandler();
		
		} else if (command instanceof NETWORK_TEST) {
			
			return new NetworkTestHandler();
		}
			return null;
	}

}
