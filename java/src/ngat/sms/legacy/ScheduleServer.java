/**
 * 
 */
package ngat.sms.legacy;

import java.util.HashMap;
import java.util.Map;

import ngat.net.JMSServer;
import ngat.net.JMSServerProtocolRequestHandlerFactory;
import ngat.sms.AsynchronousScheduler;
import ngat.sms.ExecutionUpdater;
import ngat.sms.VetoManager;

/**
 * @author eng
 *
 */
public class ScheduleServer extends JMSServer {

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @throws Exception
	 */
    public ScheduleServer(String name, int port, AsynchronousScheduler scheduler, VetoManager vetoManager, Map<Long, GroupRef> refs)  throws Exception {
	    super(name, port, new SMSRequestHandlerFactory(scheduler, vetoManager, refs));	
	    //super(name, port, new FrodoTestRequestHandlerFactory());	
	}
	
	
	

}
