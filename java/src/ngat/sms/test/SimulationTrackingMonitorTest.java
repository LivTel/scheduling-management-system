/**
 * 
 */
package ngat.sms.test;

import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.BasicCardinalPointingCalculator;
import ngat.astrometry.BasicTargetCalculator;
import ngat.astrometry.BasicTargetTrackCalculatorFactory;
import ngat.astrometry.ISite;
import ngat.astrometry.TargetTrackCalculator;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ISlew;
import ngat.phase2.ITarget;
import ngat.phase2.XRotatorConfig;
import ngat.sms.InstrumentSynopsis;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.util.SequenceSimulationUpdateListener;
import ngat.sms.util.SequenceSimulator;

/**
 * This is a test for a sequence-simulation listener which detects tracking
 * sequences and computes the correct time a target is tracked for.
 * 
 * @author eng
 * 
 */
public class SimulationTrackingMonitorTest implements SequenceSimulationUpdateListener {

	private static final double IOFF = Math.toRadians(56.0);

	private static final double DOME_LIMIT = Math.toRadians(25.0);

	private SequenceSimulator simulator;

	private ISite site;

	private BasicCardinalPointingCalculator cpc;

	private BasicAstrometrySiteCalculator astro;

	private InstrumentSynopsisModel ism;

	private long sequenceStart;

	private long timeLastSlew;

	private long timeLastRotatorChange;

	private ISlew lastSlew;

	private IRotatorConfig lastRotator;

	public SimulationTrackingMonitorTest(ISite site, InstrumentSynopsisModel ism) {
		this.ism = ism;
		cpc = new BasicCardinalPointingCalculator(site);
		astro = new BasicAstrometrySiteCalculator(site);

	}

	/**
	 * Test the supplied (root) sequence starting at specified time UTC.
	 * 
	 * @param root
	 *            The sequence root iterator.
	 * @param time
	 *            The start time.
	 * @throws Exception
	 */
	public void testSequence(ISequenceComponent root, long time) throws Exception {
		simulator = new SequenceSimulator(root, new TestResourceUsageEstimator());
		simulator.addSimulationListener(this);
		simulator.startSimulation();
	}

	/**
	 * @param time
	 *            The actual time (UTC) of the simulation start.
	 * 
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#simulationStarting(long)
	 */
	public void simulationStarting(long time) throws Exception {
		sequenceStart = time;
	}

	/**
	 * @param time
	 *            The actual time (UTC) of the simulation end.
	 * 
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#simulationFinished(long)
	 */
	public void simulationFinished(long time) throws Exception {
		

	}

	/**
	 * @param time
	 *            The time opffset from the start of the simulation.
	 * 
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#startingAction(ngat.phase2
	 *      .IExecutiveAction, long)
	 */
	public void startingAction(IExecutiveAction action, long time) throws Exception {

		long now = sequenceStart + time;

		if (action instanceof ISlew) {

			ISlew slew = (ISlew) action;
			ITarget target = slew.getTarget();
			IRotatorConfig rotator = slew.getRotatorConfig();
			String instName = rotator.getInstrumentName();
			System.err.println("Starting slew using rot: "+rotator);
			// if we have one already, work out its rot and tracking soln
			// work out target elev and rotator solution from then to now
			if (lastSlew != null) {
				
				long duration = now -timeLastSlew;
				
				// calc min elevation
				checkTracking(target, timeLastSlew, duration);

				// rotator - this may have changed since the last slew
				duration = now - timeLastRotatorChange;
				checkRotator(target, rotator, timeLastSlew, duration);

			}

			// this is now the last slew
			lastSlew = slew;
			timeLastSlew = now;

			// this is now the last rot change
			timeLastRotatorChange = now;

		} else if (action instanceof IRotatorConfig) {

			IRotatorConfig rotator = (IRotatorConfig) action;

			// work out rot solution since last rotator change or slew 
			long lastChange = Math.max(timeLastRotatorChange, timeLastSlew);
			long duration = now - lastChange;
			
			// target has not changed since last slew
			ITarget target = lastSlew.getTarget();
			checkRotator(target, rotator, lastChange, duration);

			// this is now the last rot change,
			lastRotator = rotator;
			timeLastRotatorChange = now;

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.util.SequenceSimulationUpdateListener#finishedAction(ngat.phase2
	 * .IExecutiveAction, long)
	 */
	public void finishedAction(IExecutiveAction action, long time) throws Exception {
		// nothing of interest here
	}

	/**
	 * @param target
	 *            The target being tracked.
	 * @param time
	 *            The start time.
	 * @param duration
	 *            Duration.
	 * @throws Exception
	 */
	private void checkTracking(ITarget target, long time, long duration) throws Exception {
		
		System.err.printf("CheckTarget: %s between %tT to %tT\n",
				target.getName(),
				time,
				time+duration);
				
		TargetTrackCalculator track = new BasicTargetCalculator(target, site);
		double minElev = astro.getMinimumAltitude(track, time, time + duration);
		if (minElev < DOME_LIMIT)
			throw new Exception(String.format("Target: %s sets in period: %tT - %tT ", target.getName(), time, time
					+ duration));

	}

	/**
	 * @param target
	 *            The target being tracked.
	 * @param rotator
	 *            The rotator setting.
	 * @param time
	 *            The start time.
	 * @param duration
	 *            Duration.
	 * @throws Exception
	 */
	private void checkRotator(ITarget target, IRotatorConfig rotator, long time, long duration) throws Exception {

		String instName = rotator.getInstrumentName();

		InstrumentSynopsis isyn = null;
		double instOffset = 0.0;
		//System.err.println("checkRotator(): " + XRotatorConfig.getRotatorModeName(rotator.getRotatorMode()) + ":"
			//	+ Math.toDegrees(rotator.getRotatorAngle()) + " for "
			//	+ (target != null ? target.getName() : "NO_TARGET") + " using " + instName + " from: "+" duration: " + duration);

		System.err.printf("CheckRotator: %s %4.2f from: %tT to %tT\n", 
				XRotatorConfig.getRotatorModeName(rotator.getRotatorMode()),
				Math.toDegrees(rotator.getRotatorAngle()),
				time,
				time+duration);
		
		// CheckRotator: SKY:0.0 for CRAB_offset using RATCAM duration: 1822000

		switch (rotator.getRotatorMode()) {
		case IRotatorConfig.CARDINAL:
			isyn = ism.getInstrumentSynopsis(instName);
			instOffset = isyn.getInstrumentCapabilities().getRotatorOffset();

			System.err.println("CheckRotator: Cardinal: instOffset " + instName + " " + Math.toDegrees(instOffset)
					+ " -> " + Math.toDegrees(IOFF - instOffset));

			// try each cardinal angle
			boolean ok000 = cpc.isFeasibleSkyAngle(Math.toRadians(0.0), target, IOFF - instOffset, time, time
					+ duration);
			boolean ok090 = cpc.isFeasibleSkyAngle(Math.toRadians(90.0), target, IOFF - instOffset, time, time
					+ duration);
			boolean ok180 = cpc.isFeasibleSkyAngle(Math.toRadians(180.0), target, IOFF - instOffset, time, time
					+ duration);
			boolean ok270 = cpc.isFeasibleSkyAngle(Math.toRadians(270.0), target, IOFF - instOffset, time, time
					+ duration);
			System.err.printf("CheckRotator: Cardinal: 0(%.1B), 90(%.1B), 180(%.1B), 270(%.1B)\n", ok000, ok090, ok180,
					ok270);
			if (ok000 || ok090 || ok180 || ok270)
				return;
			else
				throw new Exception(String.format(
						"No suitable cardinal solution for %s aligned to %s between %tT - %tT\n", target.getName(),
						instName, time, time + duration));			
		case IRotatorConfig.MOUNT:
			// NOTE we dont need to use the correct inst-offset here, all should
			// give same result...
			// find the sky angle at the start given the specified mount angle
			double initSkyAngle = cpc.getSkyAngle(rotator.getRotatorAngle(), target, IOFF, time);
			// see if its actually a feasible skyangle.
			boolean ok = cpc.isFeasibleSkyAngle(initSkyAngle, target, IOFF, time, time + duration);
			System.err.printf("CheckRotator: RotMount: %4.2f InitSky: %4.2f = %b\n",
					Math.toDegrees(rotator.getRotatorAngle()), Math.toDegrees(initSkyAngle), ok);
			if (ok)
				return;
			else
				throw new Exception(String.format("Mount angle %4.2f not feasible between %tT - %tT\n",
						Math.toDegrees(rotator.getRotatorAngle()), time, time + duration));

		case IRotatorConfig.SKY:
			isyn = ism.getInstrumentSynopsis(instName);
			instOffset = isyn.getInstrumentCapabilities().getRotatorOffset();

			System.err.printf("CheckRotator: Sky: %4.2f instOffset %s %4.2f -> %4.2f \n",
					Math.toDegrees(rotator.getRotatorAngle()), instName, Math.toDegrees(instOffset),
					Math.toDegrees(IOFF - instOffset));

			if (cpc.isFeasibleSkyAngle(rotator.getRotatorAngle(), target, IOFF - instOffset, time, time + duration))
				return;
			else
				throw new Exception(String.format("Sky angle %4.2f not feasible for %s between %tT - %tT\n",
						Math.toDegrees(rotator.getRotatorAngle()), target.getName(), time, time + duration));
		}
		
		throw new Exception("Unable to solve for rotator: invalid rotator mode: "+rotator.getRotatorMode());

	}
}
