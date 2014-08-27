/**
 * 
 */
package ngat.sms.legacy;

import ngat.message.SMS.EXECUTION_UPDATE;
import ngat.message.SMS.EXECUTION_UPDATE_DONE;
import ngat.message.SMS.SCHEDULE_REQUEST;
import ngat.message.SMS.SCHEDULE_REQUEST_DONE;
import ngat.message.base.ACK;
import ngat.message.base.COMMAND_DONE;
import ngat.net.JMSClientProtocolImplementor;
import ngat.net.JMSClientProtocolResponseHandler;
import ngat.net.SocketConnection;
import ngat.phase2.*;


/**
 * @author eng
 *
 */
public class TestSendSchedReq implements JMSClientProtocolResponseHandler {

	private SCHEDULE_REQUEST_DONE srd;
	
	public TestSendSchedReq() {
	
	}
	
	public void exec() throws Exception {
	
			
			SCHEDULE_REQUEST request = new SCHEDULE_REQUEST("test");
			
			JMSClientProtocolImplementor jms1 = new JMSClientProtocolImplementor(false);
			jms1.implementProtocol(this, new SocketConnection("localhost", 8776), request);
			
			
			if (srd == null) {
				System.err.println("TSR:: No schedule recieved so quiting");
				return;
			} else {
				System.err.println("TSR:: Waiting 4 seconds while rpetending to execute...");			
			}
			try {Thread.sleep(4000L);} catch (InterruptedException ix) {}

			IExecutionFailureContext efc = new XBasicExecutionFailureContext(111, "Just pretending to fail");

			
			EXECUTION_UPDATE xu = new EXECUTION_UPDATE("test");
			xu.setGroupId(srd.getGroup().getID());
			//xu.setHistoryId(srd.getHistoryId());
			xu.setSuccess(true);
			xu.setExecutionFailureContext(efc);
			xu.setTime(System.currentTimeMillis());
			xu.setVetoLevel(EXECUTION_UPDATE.VETO_LEVEL_NONE);
			
			JMSClientProtocolImplementor jms2 = new JMSClientProtocolImplementor(false);
			jms2.implementProtocol(this, new SocketConnection("localhost", 8776), xu);
			
			
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			
		TestSendSchedReq tsr = new TestSendSchedReq();
		
		tsr.exec();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleAck(ACK ack) throws Exception {
		System.err.println("TSR::Handler: ACK: "+ack);		
	}

	public void handleDone(COMMAND_DONE done) throws Exception {
		System.err.println("TSR::Handler: DONE: "+done);		
		if (!done.getSuccessful()) {
			System.err.println("TSR::Failure: "+done.getErrorNum()+", "+done.getErrorString());
		} else {
		if (done instanceof SCHEDULE_REQUEST_DONE)
			srd = (SCHEDULE_REQUEST_DONE)done;
		}	
	}

}
