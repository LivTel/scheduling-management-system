package ngat.sms.legacy;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import ngat.astrometry.ISite;
import ngat.astrometry.ReferenceFrame;
import ngat.message.SMS.SCHEDULE_REQUEST_DONE;
import ngat.message.base.COMMAND;
import ngat.net.JMSExecutionMonitor;
import ngat.net.JMSServerProtocolRequestHandler;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XAcquisitionConfig;
import ngat.phase2.XAirmassConstraint;
import ngat.phase2.XDetectorConfig;
import ngat.phase2.XDualBeamSpectrographInstrumentConfig;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XExtraSolarTarget;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XGroup;
import ngat.phase2.XInstrumentConfigSelector;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.phase2.XMultipleExposure;
import ngat.phase2.XProgram;
import ngat.phase2.XProposal;
import ngat.phase2.XRotatorConfig;
import ngat.phase2.XSlew;
import ngat.phase2.XTag;
import ngat.phase2.XUser;
import ngat.sms.AsynchronousScheduleResponseHandler;
import ngat.sms.GroupItem;
import ngat.sms.ScheduleItem;

public class FrodoScheduleRequestHandler implements JMSServerProtocolRequestHandler {
	
	private static int itest = 1;
	

	public FrodoScheduleRequestHandler() {
	
	}

	public void handleRequest(COMMAND command, JMSExecutionMonitor monitor) throws Exception {
		SCHEDULE_REQUEST_DONE reply = new SCHEDULE_REQUEST_DONE("success-reply");
		reply.setSuccessful(true);
		GroupItem group = createGroup();
		reply.setGroup(group);
		monitor.setReply(reply);
	}

	private GroupItem createGroup() {
		itest++;
		long now = System.currentTimeMillis();
		
		XGroup group = new XGroup();
		group.setName("FrodoTest#"+itest);
		group.setActive(true);
		group.setID(itest);
		group.setPriority(1);
		
		List observingConstraints = new Vector();
		XAirmassConstraint air = new XAirmassConstraint(2.5);
		observingConstraints.add(air);
		group.setObservingConstraints(observingConstraints);
		
		XFlexibleTimingConstraint timing = new XFlexibleTimingConstraint(now - 15*60*1000L, now + 15*60*1000L);
		group.setTimingConstraint(timing);
			
		XIteratorComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));		
		
		// SLEW
		XExtraSolarTarget target = new XExtraSolarTarget("Star-"+itest);
		target.setRa(Math.random()*Math.PI*2.0);
		target.setDec(Math.random()*Math.PI*0.5);
		target.setEpoch(2000.0);
		target.setFrame(ReferenceFrame.FK5);
		
		XRotatorConfig rotator = new XRotatorConfig(XRotatorConfig.CARDINAL, 0.0);
		rotator.setInstrumentName("IO:O");
		XSlew slew = new XSlew(target, rotator, false);
		
		XExecutiveComponent eslew = new XExecutiveComponent("Slew", slew);		
		root.addElement(eslew);
		
		// APERTURE
		XAcquisitionConfig aperture = new XAcquisitionConfig(XAcquisitionConfig.INSTRUMENT_CHANGE, "IO:O", null, false);
		XExecutiveComponent eaperture = new XExecutiveComponent("aperture", aperture);
		root.addElement(eaperture);
		
		// FINE_TUNE
		XAcquisitionConfig acquire = new XAcquisitionConfig(XAcquisitionConfig.WCS_FIT, "FRODO", "IO:O", true);
		XExecutiveComponent eacquire = new XExecutiveComponent("acquire", acquire);
		root.addElement(eacquire);
		
		// CONFIG
		XDualBeamSpectrographInstrumentConfig frodo = new XDualBeamSpectrographInstrumentConfig("Hi-Blue");
		frodo.setInstrumentName("FRODO_BLUE");
		frodo.setResolution(XDualBeamSpectrographInstrumentConfig.HIGH_RESOLUTION);
		XDetectorConfig dc = new XDetectorConfig();
		dc.setXBin(1);
		dc.setYBin(1);		
		frodo.setDetectorConfig(dc);
		XExecutiveComponent efrodo = new XExecutiveComponent("frodo", new XInstrumentConfigSelector(frodo));
		root.addElement(efrodo);
		
		// Exposure
		XMultipleExposure mult = new XMultipleExposure(10000.0, 1);
		mult.setName("mult");
		XExecutiveComponent emult = new XExecutiveComponent("mult", mult);
		root.addElement(emult);
		
		GroupItem gi = new GroupItem(group, root);
		gi.setHId(666);
		XProgram program = new XProgram("XTEST");
		gi.setProgram(program);
		XTag tag = new XTag();
		tag.setName("Testing");
		gi.setTag(tag);
		XUser user = new XUser("Berty.Smith");
		gi.setUser(user);
		XProposal proposal = new XProposal("XTL14A01");
		gi.setProposal(proposal);
		
		return gi;
		
	}
	
}
