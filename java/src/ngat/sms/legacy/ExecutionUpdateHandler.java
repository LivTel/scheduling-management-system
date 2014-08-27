/**
 * 
 */
package ngat.sms.legacy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import ngat.message.SMS.EXECUTION_UPDATE;
import ngat.message.SMS.EXECUTION_UPDATE_DONE;
import ngat.message.base.COMMAND;
import ngat.net.JMSExecutionMonitor;
import ngat.net.JMSServerProtocolRequestHandler;
import ngat.phase2.IExecutionFailureContext;
import ngat.phase2.IQosMetric;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;
import ngat.sms.VetoManager;

/**
 * @author eng
 * 
 */
public class ExecutionUpdateHandler implements JMSServerProtocolRequestHandler {

	private Map<Long, GroupRef> refs;

	private VetoManager vetoManager;

	/**
	 * 
	 */
	public ExecutionUpdateHandler(VetoManager vetoManager, Map<Long, GroupRef> refs) {
		this.refs = refs;
		this.vetoManager = vetoManager;
		System.err.println("Creating Execupdater...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.net.JMSServerProtocolRequestHandler#handleRequest(ngat.message.base
	 * .COMMAND, ngat.net.JMSExecutionMonitor)
	 */
	public void handleRequest(COMMAND command, JMSExecutionMonitor monitor) throws Exception {
		// this is not asynch so should really de-thread this.
		System.err.println("Execupdater, process: " + command);

		EXECUTION_UPDATE exec = (EXECUTION_UPDATE) command;

		long gid = exec.getGroupId();
		long historyId = exec.getHistoryId(); // could be -999 for no remote
												// history
		boolean success = exec.getSuccess();
		long time = exec.getTime();
		IExecutionFailureContext efc = exec.getExecutionFailureContext();
		// Map erb = exec.getExecutionResourceBundle(); // as a map only
		ExecutionResourceBundle erb = new ExecutionResourceBundle();

		// boolean vetoed = exec.isVetoed();
		// long vetoedUntil = System.currentTimeMillis() +
		// exec.getVetoDuration();

		int vetoLevel = exec.getVetoLevel();
		// TODO work out the veto duration from the level none=0, perm=24h, and
		// e.g. low=30m

		// make the call...
		GroupRef gref = refs.get(historyId);
		System.err.println("Execupdater, found gref: " + gref);
		GroupItem group = gref.getGroup();
		ExecutionUpdater updater = gref.getUpdater();

		EXECUTION_UPDATE_DONE reply = new EXECUTION_UPDATE_DONE("reply");
		reply.setSuccessful(true);
		if (success) {
			// Success update
			try {
				System.err.println("Execupdater, update for success..");
				updater.groupExecutionCompleted(group, time, erb, new HashSet<IQosMetric>());
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Execupdater, Error while calling updater: " + e);
				reply.setSuccessful(false);
				reply.setErrorNum(666);
				reply.setErrorString("Exception calling updater with success: " + e);
			}
		} else {
			// Error update
			try {
				System.err.println("Execupdater, update for failure...");
				updater.groupExecutionAbandoned(group, time, erb, efc, new HashSet<IQosMetric>());
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Execupdater, Error while calling updater: " + e);
				reply.setSuccessful(false);
				reply.setErrorNum(666);
				reply.setErrorString("Exception calling updater with failure: " + e);
			}

			long vetoDuration = 0L;
			String vetoLevelName = "NONE";
			switch (vetoLevel) {
			case EXECUTION_UPDATE.VETO_LEVEL_NONE:
				vetoDuration = 0L;
				vetoLevelName = "NONE";
				break;
			case EXECUTION_UPDATE.VETO_LEVEL_LOW: // 1H
				vetoDuration = 60 * 60 * 1000L;
				vetoLevelName = "LOW";
				break;
			case EXECUTION_UPDATE.VETO_LEVEL_MEDIUM: // 2H
				vetoDuration = 120 * 60 * 1000L;
				vetoLevelName = "MEDIUM";
				break;
			case EXECUTION_UPDATE.VETO_LEVEL_HIGH: // 4H
				vetoDuration = 240 * 60 * 1000L;
				vetoLevelName = "HIGH";
				break;
			case EXECUTION_UPDATE.VETO_LEVEL_PERMANENT: // 24H
				vetoDuration = 1440 * 60 * 1000L;
				vetoLevelName = "PERMANENT";
				break;
			}
			
			long vetoedUntil = System.currentTimeMillis() + vetoDuration;
			
			// Veto management
			try {			
				System.err.println("Execupdater, vetoing group: " + group.getName() + " at level [" + vetoLevelName
						+ "]" + " for " + (vetoDuration / 1000) + "s " + " until " + (new Date(vetoedUntil)));		
				vetoManager.vetoGroup(group.getID(), vetoedUntil);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Execupdater, Error while vetoing group: " + e);
			}
		}

		monitor.setReply(reply);
	}
}
