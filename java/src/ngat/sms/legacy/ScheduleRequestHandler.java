/**
 * 
 */
package ngat.sms.legacy;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.message.SMS.SCHEDULE_REQUEST_DONE;
import ngat.message.base.COMMAND;
import ngat.net.JMSExecutionMonitor;
import ngat.net.JMSServerProtocolRequestHandler;
import ngat.phase2.IGroup;
import ngat.phase2.IObservingConstraint;
import ngat.phase2.XGroup;
import ngat.sms.AsynchronousScheduleResponseHandler;
import ngat.sms.AsynchronousScheduler;
import ngat.sms.DefaultExecutionUpdater;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;
import ngat.sms.ScheduleItem;

/**
 * @author eng
 * 
 */
public class ScheduleRequestHandler implements JMSServerProtocolRequestHandler, AsynchronousScheduleResponseHandler {

	private AsynchronousScheduler scheduler;
	private Map<Long, GroupRef> refs;

	private ScheduleItem schedule;

	private JMSExecutionMonitor monitor;

	/**
	 * 
	 */
	public ScheduleRequestHandler(AsynchronousScheduler scheduler, Map<Long, GroupRef> refs) {
		this.scheduler = scheduler;
		this.refs = refs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.net.JMSServerProtocolRequestHandler#handleRequest(ngat.message.base
	 * .COMMAND, ngat.net.JMSExecutionMonitor)
	 */
	public void handleRequest(COMMAND command, JMSExecutionMonitor monitor) throws Exception {
		this.monitor = monitor;
		// send a request
		try {
			scheduler.requestSchedule(this);
			monitor.setTimeToCompletion(120000L);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("SchedReqHandler, Error while calling asynch scheduler: " + e);
			SCHEDULE_REQUEST_DONE reply = new SCHEDULE_REQUEST_DONE("error-reply");
			reply.setSuccessful(false);
			reply.setErrorNum(777);
			reply.setErrorString("Exception calling asynch scheduler: " + e);
			monitor.setReply(reply);
		}
	}

	public void asynchronousScheduleFailure(int code, String message) throws RemoteException {
		SCHEDULE_REQUEST_DONE reply = new SCHEDULE_REQUEST_DONE("failure-reply");
		reply.setSuccessful(false);
		reply.setErrorNum(code);
		reply.setErrorString(message);

		monitor.setReply(reply);
	}

	public void asynchronousScheduleProgress(String message) throws RemoteException {
		// print this for now, maybe an ack...
		System.err.println("SchedReqHandler:: Progess report from scheduler: " + message);
	}

	public void asynchronousScheduleResponse(ScheduleItem sched) throws RemoteException {
		schedule = sched;
		System.err.println("SRH:: asynchronousScheduleResponse() with: " + sched);

		if (schedule == null) {
			SCHEDULE_REQUEST_DONE reply = new SCHEDULE_REQUEST_DONE("error-reply");
			reply.setSuccessful(false);
			reply.setErrorNum(555);
			reply.setErrorString("SchedulingStatusProvider reply contained no available groups");
			monitor.setReply(reply);
			return;
		}

		try {
			// break the wait cycle..
			GroupItem group = schedule.getGroup();
			//XGroup xgroup = extractGroup(group);
			SCHEDULE_REQUEST_DONE reply = new SCHEDULE_REQUEST_DONE("success-reply");
			reply.setSuccessful(true);
			reply.setGroup(group);
			//reply.setTag(group.getTag());
			//reply.setUser(group.getUser());
			//reply.setProgram(group.getProgram());
			//reply.setProposal(group.getProposal());
			//reply.setSequence(group.getSequence());
			//reply.setHistoryId(group.getHId());

			// link the nominated updater - TODO may change this to a singleton
			// passed into <init>
			ExecutionUpdater updater = schedule.getExecutionUpdater();
			GroupRef gref = new GroupRef(group, updater);
			refs.put(group.getHId(), gref);
			System.err.println("SRH: added updater ref to map: " + gref);

			monitor.setReply(reply);

		} catch (Exception e) {
			e.printStackTrace();
			SCHEDULE_REQUEST_DONE reply = new SCHEDULE_REQUEST_DONE("error-reply");
			reply.setErrorNum(557);
			reply.setErrorString("An error occurred while processing scheduler reply: " + e);
			reply.setSuccessful(false);
			monitor.setReply(reply);

		}

	}

	/**
	 * Replace the received (1.5) ngat.sms.GroupItem with a (1.4) XGroup which
	 * RCS can read.
	 */
	private XGroup extractGroup(GroupItem group) {
		XGroup xg = new XGroup();
		xg.setID(group.getID());
		xg.setName(group.getName());
		xg.setPriority(group.getPriority());
		xg.setTimingConstraint(group.getTimingConstraint());
		List oc = group.listObservingConstraints();
		if (oc != null) {
			Iterator ioc = oc.iterator();
			while (ioc.hasNext()) {
				xg.addObservingConstraint((IObservingConstraint) ioc.next());
			}
		}
		return xg;
	}
}
