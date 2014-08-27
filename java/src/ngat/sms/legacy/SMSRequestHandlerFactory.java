/**
 * 
 */
package ngat.sms.legacy;

import java.util.Map;

import ngat.message.SMS.EXECUTION_UPDATE;
import ngat.message.SMS.NETWORK_TEST;
import ngat.message.SMS.SCHEDULE_REQUEST;
import ngat.message.base.COMMAND;
import ngat.net.JMSServerProtocolRequestHandler;
import ngat.net.JMSServerProtocolRequestHandlerFactory;
import ngat.sms.AsynchronousScheduler;
import ngat.sms.ExecutionUpdater;
import ngat.sms.VetoManager;

/**
 * @author eng
 * 
 */
public class SMSRequestHandlerFactory implements JMSServerProtocolRequestHandlerFactory {

	private AsynchronousScheduler scheduler;

	private Map<Long, GroupRef> refs;

    private VetoManager vetoManager;

	/**
	 * 
	 */
    public SMSRequestHandlerFactory(AsynchronousScheduler scheduler, VetoManager vetoManager, Map<Long, GroupRef> refs) {
		this.scheduler = scheduler;
		this.vetoManager = vetoManager;
		this.refs = refs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.net.JMSServerProtocolRequestHandlerFactory#createRequestHandler(
	 * ngat.message.base.COMMAND)
	 */
	public JMSServerProtocolRequestHandler createRequestHandler(COMMAND command) {

		System.err.println("SMSHFactory: Recieved: "+command);
		
		if (command instanceof SCHEDULE_REQUEST) {

			return new ScheduleRequestHandler(scheduler, refs);

		} else if (command instanceof EXECUTION_UPDATE) {
			
		    return new ExecutionUpdateHandler(vetoManager, refs);
		
		} else if (command instanceof NETWORK_TEST) {
			
			return new NetworkTestHandler();
		    
		} else
			return null;
		// will throw an exception when the JMSImpl tries to call handle request
		// on the null handler
	}

}
