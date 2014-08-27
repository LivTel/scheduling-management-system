package ngat.sms.bds.test;

import ngat.sms.*;
import ngat.sms.bds.*;
import ngat.phase2.*;
import ngat.util.*;
import ngat.astrometry.*;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;


public class FixedTestScheduler extends UnicastRemoteObject implements ScheduleDespatcher, AsynchronousScheduler, VetoManager {

    static int it = 0;

    public FixedTestScheduler() throws RemoteException {
	super();	
    }

    public static void main(String args[]) {

	try {

	    FixedTestScheduler fts = new FixedTestScheduler();

	    Naming.rebind("AsynchScheduler", fts);

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    public ScheduleItem nextScheduledJob() throws RemoteException {
	it++;

	// make up a group
	XGroup g = new XGroup();
	g.setName("test-"+it);
	g.setID((long)(it*1234));
	g.setTimingConstraint(new XFixedTimingConstraint(System.currentTimeMillis(), 60000L));
	// wipe the OC at this point
	g.setObservingConstraints(null);
	
	XIteratorComponent seq = new XIteratorComponent ("root", new XIteratorRepeatCountCondition(1));
	XExtraSolarTarget star = new XExtraSolarTarget("thestar");
	star.setRa(Math.random()*6.0);
	star.setDec((Math.random()-0.5)*2.0);
	XRotatorConfig rot = new XRotatorConfig(IRotatorConfig.CARDINAL, 0.0, "RATCAM");
	XSlew slew = new XSlew(star, rot, false);
	seq.addElement(new XExecutiveComponent("Slew",slew));
	
	GroupItem group = new GroupItem(g, seq);
	
	group.setProposal(new XProposal("TL10X01"));
	group.setProgram(new XProgram("XTL10X"));
	XTag tag = new XTag();
	tag.setName("XXT");
	group.setTag(tag);
	group.setUser(new XUser("TestUser"));
	
	// history id
	group.setHId(0);
	
	ScheduleItem item = new TestScheduleItem(group, null);

	System.err.println("FST:: nextSchedJob(): I shall be returning the following group: \n"+g);

	return item;

    }

    public void requestSchedule(AsynchronousScheduleResponseHandler asrh) throws RemoteException {
	// spin off a numbered thread and let it reply after a while

	AsynchResponder ar = new AsynchResponder(asrh);
	(new Thread(ar)).start();

    }

    public void vetoGroup(long gid, long time) throws RemoteException {
	System.err.println("FST:: Requested to veto group: "+gid);
    }

public void removeVeto(long gid) throws RemoteException {
	// TODO Auto-generated method stub
	
}

public long getVetoTime(long gid) throws RemoteException {
	// TODO Auto-generated method stub
	return 0;
}

public List<Veto> listActiveVetos() throws RemoteException {
	// TODO Auto-generated method stub
	return null;
}

    private class AsynchResponder implements Runnable {
	
	AsynchronousScheduleResponseHandler asrh;

	private AsynchResponder(AsynchronousScheduleResponseHandler asrh) {
	    this.asrh = asrh;
	}

	public void run() {

	    ScheduleItem sched = null;

	    // let the client know we are working on it...
	    try {
		asrh.asynchronousScheduleProgress("Asynch responder will return a schedule to you shortly");
	    } catch (Exception ee) {
		
		System.err.println("FST::Unable to send progress message to handler: " + ee);
	    }

	    // make the schedule request...
	    try {
		System.err.println("FST::Calling nextSchedJob() for handler: " + asrh);
		sched = nextScheduledJob();
		System.err.println("FST::Schedule request completed, got: " + sched);
	    } catch (Exception e) {
		System.err.println("FST::Error obtaining schedule: " +e);
		e.printStackTrace();
		try {
		    String message = "Unable to generate schedule: " + e;
		    System.err.println("FST::Sending error message to handler: [" + message + "]");
		    asrh.asynchronousScheduleFailure(5566, message);
		} catch (Exception e2) {
		    System.err.println("FST::Unable to send error message to handler: " + e2);
		    e2.printStackTrace();
		}
		return;
	    }

	    // finally let the client know the result...
	    try {
		System.err.println("FST::Sending schedule reply to handler: " + asrh);
		asrh.asynchronousScheduleResponse(sched);
	    } catch (Exception e3) {
		System.err.println("FST::Unable to send schedule reply to handler: " + e3);
		e3.printStackTrace();
	    }
	}

    }
	


}