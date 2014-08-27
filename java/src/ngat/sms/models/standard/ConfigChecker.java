/**
 * 
 */
package ngat.sms.models.standard;

import java.util.Iterator;
import java.util.List;

import ngat.astrometry.BasicCardinalPointingCalculator;
import ngat.astrometry.CardinalPointingCalculator;
import ngat.astrometry.ISite;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITarget;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XRotatorConfig;
import ngat.phase2.XSlew;
import ngat.phase2.XTargetSelector;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.sms.InstrumentSynopsis;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.TelescopeSystemsSynopsis;
import ngat.sms.bds.TestInstrumentSynopsis;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class ConfigChecker {

	private double rotatorBaseOffset = Math.toRadians(56.6);

	
	ExecutionResourceUsageEstimationModel xrm;
	InstrumentSynopsisModel ism;
	TelescopeSystemsSynopsis tsm;
	ISite site;
	ITarget target;
	CardinalPointingCalculator cpc;
	
	Logger astroLog;

	/**
	 * @param site
	 */
	public ConfigChecker(ExecutionResourceUsageEstimationModel xrm, InstrumentSynopsisModel ism, TelescopeSystemsSynopsis tsm, ISite site) {
		this.xrm = xrm;
		this.ism = ism;
		this.tsm = tsm;
		this.site = site;
		cpc = new BasicCardinalPointingCalculator(site);
		try {
		    //IOFF = Math.toRadians(Double.parseDouble(System.getProperty("rotator.sky.base.offset")));
		    rotatorBaseOffset = tsm.getRotatorBaseOffset();
		    System.err.println("CONFIG CHECKER: Using initial instrument base offset: "+Math.toDegrees(rotatorBaseOffset));
		} catch (Exception e) {
		    System.err.println("CONFIG CHECKER: Unable to set instrument base offset: "+e);
		}
	}

	public boolean checkRotator(ISequenceComponent root, long time) throws Exception {
	    rotatorBaseOffset = tsm.getRotatorBaseOffset();
	    System.err.println("CONFIG CHECKER: Using instrument base offset: "+Math.toDegrees(rotatorBaseOffset));
		long duration = calculateDuration(root);
		return checkComponent(root, time, duration);
	}

	private boolean checkComponent(ISequenceComponent sequence, long time, long duration) throws Exception {

		if (sequence instanceof XIteratorComponent) {
			XIteratorComponent xiter = (XIteratorComponent) sequence;

			// get the duration of this iterator to pass down the tree
			long componentDuration = calculateDuration(xiter);

			List components = sequence.listChildComponents();
			Iterator ic = components.iterator();
			while (ic.hasNext()) {
				ISequenceComponent component = (ISequenceComponent) ic.next();
				// if ANY sub component fails we have failed
				if (!checkComponent(component, time, componentDuration))
					return false;
			}
		} else if (sequence instanceof XBranchComponent) {
			XBranchComponent branch = (XBranchComponent) sequence;
			// get the duration of this branch to pass down the tree
			long componentDuration = calculateDuration(branch);

			List components = sequence.listChildComponents();
			Iterator ic = components.iterator();
			while (ic.hasNext()) {
				ISequenceComponent component = (ISequenceComponent) ic.next();
				// if ANY sub component fails we have failed
				if (!checkComponent(component, time, componentDuration))
					return false;
			}

		} else {
			// an executive
			XExecutiveComponent xec = (XExecutiveComponent) sequence;
			IExecutiveAction action = xec.getExecutiveAction();
			if (action instanceof XSlew) {
				target = ((XSlew) action).getTarget();
				IRotatorConfig rotator = ((XSlew) action).getRotatorConfig();
				// test if rotator setting is valid with this target at time
				
				String instName = rotator.getInstrumentName();
				if (instName != null)
					instName = instName.toUpperCase().trim();
									
				if (!feasible(rotator, target, instName, time, duration))
					return false;
			} else if (action instanceof XTargetSelector) {
				target = ((XTargetSelector) action).getTarget();
			} else if (action instanceof XRotatorConfig) {
				IRotatorConfig rotator = (XRotatorConfig) action;
				// test if rotator setting is valid with current target at time
			
				String instName = rotator.getInstrumentName();
				if (instName != null)
					instName = instName.toUpperCase().trim();
					
				if (!feasible(rotator, target, instName, time, duration))
					return false;
			}
		}

		return true;
	}

	/**
	 * Test if the supplied rotator setting is valid at time for target for
	 * duration.
	 * 
	 * @param rotator
	 *            The rotator setting to test.
	 * @param target
	 *            The target we are observing.
	 * @param time
	 *            When the observation is taking place.
	 * @param duration
	 *            How long the observation will take.
	 * @return True if the rotator setting is NOT feasible for supplied
	 *         parameters.
	 */
	private boolean feasible(IRotatorConfig rotator, ITarget target, String instName, long time, long duration) throws Exception {
		InstrumentSynopsis isyn = null;
		double instOffset = 0.0;
		System.err.println("CheckRotator: "+
				   XRotatorConfig.getRotatorModeName(rotator.getRotatorMode())+":"+
				   Math.toDegrees(rotator.getRotatorAngle())+" for "+
				   (target != null ? target.getName() : "NO_TARGET")+ " using "+
				   instName+
				  " duration: "+duration);
		
		// CheckRotator: SKY:0.0 for CRAB_offset using RATCAM duration: 1822000
		
		switch (rotator.getRotatorMode()) {
		case IRotatorConfig.CARDINAL:	
			isyn = ism.getInstrumentSynopsis(instName);
			instOffset = isyn.getInstrumentCapabilities().getRotatorOffset();	
		
			System.err.println("CheckRotator: Cardinal: instOffset "+instName+" "+
					   Math.toDegrees(instOffset)+" -> "+Math.toDegrees(rotatorBaseOffset - instOffset));
			
			// try each cardinal angle
			boolean ok000 = cpc.isFeasibleSkyAngle(Math.toRadians(0.0), target,  rotatorBaseOffset- instOffset, time, time + duration);
			boolean ok090 = cpc.isFeasibleSkyAngle(Math.toRadians(90.0), target,  rotatorBaseOffset- instOffset, time, time + duration);
			boolean ok180 = cpc.isFeasibleSkyAngle(Math.toRadians(180.0), target, rotatorBaseOffset - instOffset, time, time + duration);
			boolean ok270 = cpc.isFeasibleSkyAngle(Math.toRadians(270.0), target,  rotatorBaseOffset- instOffset, time, time + duration);
			System.err.printf("CheckRotator: Cardinal: 0(%.1B), 90(%.1B), 180(%.1B), 270(%.1B)\n", 
					  ok000, ok090, ok180, ok270);
			if (ok000 || ok090 || ok180 || ok270)
			    return true;
			break;
		case IRotatorConfig.MOUNT:
			// NOTE we dont need to use the correct inst-offset here, all should give same result...
			// find the sky angle at the start given the specified mount angle
			double initSkyAngle = cpc.getSkyAngle(rotator.getRotatorAngle(), target, rotatorBaseOffset, time);
			// see if its actually a feasible skyangle.
			boolean ok = cpc.isFeasibleSkyAngle(initSkyAngle, target, rotatorBaseOffset, time, time + duration);
			System.err.printf("CheckRotator: RotMount: %4.2f InitSky: %4.2f = %b\n", 
					  Math.toDegrees(rotator.getRotatorAngle()), Math.toDegrees(initSkyAngle), ok);
			return ok;

		case IRotatorConfig.SKY:	
			isyn = ism.getInstrumentSynopsis(instName);
			instOffset = isyn.getInstrumentCapabilities().getRotatorOffset();		
					   
			System.err.printf("CheckRotator: Sky: %4.2f instOffset %s %4.2f -> %4.2f \n",
					   Math.toDegrees(rotator.getRotatorAngle()),
					  instName,
					  Math.toDegrees(instOffset),
					  Math.toDegrees(rotatorBaseOffset - instOffset));

			// switch the logging on then off again ...
			astroLog = LogManager.getLogger("ASTRO");
			astroLog.setLogLevel(3);
			boolean f = cpc.isFeasibleSkyAngle(rotator.getRotatorAngle(), target, rotatorBaseOffset - instOffset, time, time + duration);
			astroLog.setLogLevel(0);
			return f;
		}

		return false;
	}

	private long calculateDuration(ISequenceComponent comp) {
		double xt = xrm.getExecTime(comp);
		return (long) xt;
	}

}
