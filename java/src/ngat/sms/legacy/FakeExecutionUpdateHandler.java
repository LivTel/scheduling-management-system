package ngat.sms.legacy;

import ngat.message.SMS.EXECUTION_UPDATE_DONE;
import ngat.message.base.COMMAND;
import ngat.net.JMSExecutionMonitor;
import ngat.net.JMSServerProtocolRequestHandler;

public class FakeExecutionUpdateHandler implements JMSServerProtocolRequestHandler {

	public void handleRequest(COMMAND command, JMSExecutionMonitor monitor) throws Exception {
		
		EXECUTION_UPDATE_DONE reply = new EXECUTION_UPDATE_DONE("reply");
		reply.setSuccessful(true);
		
		monitor.setReply(reply);
	}

}
