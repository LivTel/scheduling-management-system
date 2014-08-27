/**
 * 
 */
package ngat.sms.bds;

import java.util.Iterator;
import java.util.List;

import ngat.icm.Imager;
import ngat.icm.InstrumentCapabilities;
import ngat.phase2.*;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class TestResourceUsageEstimator implements ExecutionResourceUsageEstimationModel {

	public static final double SLEW_TIME = 60000.0;
	public static final double ACQUIRE_TIME = 10000.0;
	public static final double AUTO_ACQUIRE_TIME = 10000.0;
	public static final double DEFOCUS_TIME = 5000.0;
	public static final double OFFSET_TIME = 8000.0;
	public static final double ROTATE_TIME = 30000.0;
	public static final double BEAM_CONFIG_TIME = 10000.0;

	// TODO simple calib timing - needs configuring properly
	public static final double CALIB_TIME = 20000.0;
	public static final double DEFAULT_CONFIG_TIME = 5000.0;

	public static final double IO_O_DEFAULT_CONFIG_TIME = 5000.0;

	public static final double CCD_READOUT_TIME_MULT_FACTOR = 10.667;
	public static final double CCD_READOUT_TIME_FIXED_OVERHEAD = 9.33;

	public static final double IRCAM_DEFAULT_READOUT_TIME = 12000.0;

	public static final double POLARIMETER_READOUT_TIME = 5000.0;

	public static final double LOWRES_SPEC_READOUT_TIME = 20000.0;

	public static final double RISE_DEFAULT_READOUT_TIME = 35.0;

	public static final double FRODO_DEFAULT_READOUT_TIME = 20000.0;

	public static final double IO_THOR_DEFAULT_READOUT_TIME = 0.0;

	public static final double IO_O_DEFAULT_READOUT_TIME = 19000.0;

	public static final double GENERIC_DEFAULT_READOUT_TIME = 10000.0;

	private volatile IInstrumentConfig lastConfig = null;

	private LogGenerator logger;

	/**
	 * 
	 */
	public TestResourceUsageEstimator() {
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider")
				.srcCompClass("TestresourceUsageEstimator");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.sms.ExecutionResourceUsageEstimationModel#getEstimatedResourceUsage
	 * (ngat.sms.GroupItem)
	 */
	public ExecutionResourceBundle getEstimatedResourceUsage(GroupItem group) {

		ExecutionResourceBundle erb = new ExecutionResourceBundle();

		// total exec time
		double timeUsed = getExecTime(group.getSequence());
		ExecutionResource timeUsedRes = new ExecutionResource("TIME", timeUsed);
		erb.addResource(timeUsedRes);

		// shutter open time
		double shutter = getShutterTotal(group.getSequence());
		ExecutionResource shutterRes = new ExecutionResource("EXPOSURE", shutter);
		erb.addResource(shutterRes);

		// observation count
		int obsCount = getNumberObservations(group.getSequence());
		ExecutionResource obsCountRes = new ExecutionResource("OBSCOUNT", obsCount);
		erb.addResource(obsCountRes);

		return erb;

	}

	/**
	 * Calculates the total open-shutter (exposing) time.
	 * 
	 * @param seq
	 *            The sequenc element to evaluate.
	 * @return The contribution to shutter-open time from the specified element.
	 */
	private double getShutterTotal(ISequenceComponent seq) {
		double total = 0.0;

		if (seq instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) seq;
			XIteratorRepeatCountCondition cc = (XIteratorRepeatCountCondition) iter.getCondition();
			int count = cc.getCount();
			List list = seq.listChildComponents();
			Iterator il = list.iterator();
			while (il.hasNext()) {
				ISequenceComponent sc = (ISequenceComponent) il.next();
				total += getShutterTotal(sc);
			}
			return count * total;
		} else if (seq instanceof XExecutiveComponent) {
			total += getShutterExecTime((XExecutiveComponent) seq);
		}
		return total;
	}

	private int getNumberObservations(ISequenceComponent seq) {

		int numobs = 0;

		if (seq instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) seq;
			XIteratorRepeatCountCondition cc = (XIteratorRepeatCountCondition) iter.getCondition();
			int count = cc.getCount();
			List list = seq.listChildComponents();
			Iterator il = list.iterator();
			while (il.hasNext()) {
				ISequenceComponent sc = (ISequenceComponent) il.next();
				numobs += getNumberObservations(sc);
			}
			return count * numobs;
		} else if (seq instanceof XBranchComponent) {

			numobs += getBranchCount((XBranchComponent) seq);

		} else if (seq instanceof XExecutiveComponent) {

			numobs += getObsCount((XExecutiveComponent) seq);

		}
		return numobs;

	}

	private int getObsCount(XExecutiveComponent exec) {

		IExecutiveAction action = exec.getExecutiveAction();

		if (action instanceof IExposure)
			return 1;

		return 0;

	}

	public double getExecTime(ISequenceComponent seq) {
		double total = 0.0;

		if (seq instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) seq;
			XIteratorRepeatCountCondition cc = (XIteratorRepeatCountCondition) iter.getCondition();
			int count = cc.getCount();

			List list = seq.listChildComponents();
			Iterator il = list.iterator();
			while (il.hasNext()) {
				ISequenceComponent sc = (ISequenceComponent) il.next();
				total += getExecTime(sc);
			}
			return count * total;

		} else if (seq instanceof XBranchComponent) {

			total += getBranchExecTime((XBranchComponent) seq);
		} else if (seq instanceof XExecutiveComponent) {
			total += getActionExecTime((XExecutiveComponent) seq);
		}

		return total;

	}

	/** Count observations in a branch. */
	private int getBranchCount(XBranchComponent bran) {

		int branchTotal = 0;

		List list = bran.listChildComponents();
		Iterator il = list.iterator();
		// these are 2 iterators
		while (il.hasNext()) {
			ISequenceComponent sc = (ISequenceComponent) il.next();
			branchTotal += getNumberObservations(sc);
		}

		return branchTotal;

	}

	/**
	 * For now we estimate the longest branch with no consideration of
	 * compatibility.
	 */
	private double getBranchExecTime(XBranchComponent bran) {

		double maxTime = 0.0;
		List list = bran.listChildComponents();
		Iterator il = list.iterator();
		while (il.hasNext()) {
			ISequenceComponent sc = (ISequenceComponent) il.next();
			double branTime = getExecTime(sc);
			if (branTime > maxTime)
				maxTime = branTime;
		}
		return maxTime;

	}

	private boolean compatible(XExecutiveComponent mac, XExecutiveComponent sac) {

		IExecutiveAction maa = mac.getExecutiveAction();
		IExecutiveAction saa = sac.getExecutiveAction();

		if (maa instanceof XCalibration && saa instanceof IExposure)
			return false;

		return true;

	}

	private double getShutterExecTime(XExecutiveComponent comp) {

		IExecutiveAction action = comp.getExecutiveAction();
		if (action instanceof IInstrumentConfigSelector) {
			lastConfig = ((IInstrumentConfigSelector) action).getInstrumentConfig();
		} else if (action instanceof IExposure) {
			return calculateShutterTime((IExposure) action, lastConfig);
		}
		return 0.0;
	}

	private double calculateShutterTime(IExposure exposure, IInstrumentConfig config) {

		if (exposure instanceof XMultipleExposure) {
			XMultipleExposure xmult = (XMultipleExposure) exposure;
			double exp = xmult.getExposureTime();
			double count = xmult.getRepeatCount();

			return count * exp;

		} else if (exposure instanceof XPeriodExposure) {

			XPeriodExposure xtrig = (XPeriodExposure) exposure;
			double exp = xtrig.getExposureTime();
			return exp;

		} else if (exposure instanceof XPeriodRunAtExposure) {
			XPeriodRunAtExposure xper = (XPeriodRunAtExposure) exposure;
			double exp = xper.getExposureLength();
			double duration = xper.getTotalExposureDuration();
			return duration + exp + 15000; // add some time on for setup
		}
		return 0.0;
	}

	private double getActionExecTime(XExecutiveComponent comp) {

		IExecutiveAction action = comp.getExecutiveAction();
		// see what sort it is e.g. exp, cfg, offset, setups
		// depending on the instrument (of last IC) define the readout etc.
		// need to keep track of last instrument change...

		if (action instanceof ITargetSelector) {
			return SLEW_TIME;
		} else if (action instanceof ISlew) {
			return SLEW_TIME;
		} else if (action instanceof IAcquisitionConfig) {
			return ACQUIRE_TIME;
		} else if (action instanceof IAutoguiderConfig) {
			return AUTO_ACQUIRE_TIME;
		} else if (action instanceof IBeamSteeringConfig) {
			return BEAM_CONFIG_TIME;
		} else if (action instanceof IExposure) {
			return calculateExposureTime((IExposure) action, lastConfig);
		} else if (action instanceof IFocusOffset) {
			return DEFOCUS_TIME;
		} else if (action instanceof IInstrumentConfigSelector) {
			lastConfig = ((IInstrumentConfigSelector) action).getInstrumentConfig();
			return calculateConfigTime(lastConfig);
		} else if (action instanceof IMosaicOffset) {
			return OFFSET_TIME;
		} else if (action instanceof IRotatorConfig) {
			return ROTATE_TIME;
		} else if (action instanceof ICalibration) {
			// calib + calib readout
			return 20000 + calculateCalibrationTime(((ICalibration) action), lastConfig);
		}
		return 0.0;

	}

	private double calculateConfigTime(IInstrumentConfig config) {
		String instName = config.getInstrumentName();
		if (instName.equalsIgnoreCase("IO:O"))
			return IO_O_DEFAULT_CONFIG_TIME;
		return DEFAULT_CONFIG_TIME;
	}

	/*
	 * frodospec.arc.red.low.Ne.exposure_length =1000
	 * frodospec.arc.red.high.Ne.exposure_length =500
	 * frodospec.arc.blue.low.Ne.exposure_length =1000
	 * frodospec.arc.blue.high.Ne.exposure_length =1000
	 * 
	 * frodospec.arc.red.low.Xe.exposure_length =1000
	 * frodospec.arc.red.high.Xe.exposure_length =60000
	 * frodospec.arc.blue.low.Xe.exposure_length =60000
	 * frodospec.arc.blue.high.Xe.exposure_length =60000
	 * 
	 * frodospec.arc.red.low.W.exposure_length =1000
	 * frodospec.arc.red.high.W.exposure_length =1000
	 * frodospec.arc.blue.low.W.exposure_length =100000
	 * frodospec.arc.blue.high.W.exposure_length =200000
	 */
	private double calculateCalibrationTime(ICalibration calib, IInstrumentConfig config) {
		if (calib instanceof XArc) {
			if (config instanceof XDualBeamSpectrographInstrumentConfig) {
				XDualBeamSpectrographInstrumentConfig dual = (XDualBeamSpectrographInstrumentConfig) config;
				int res = dual.getResolution();
				String instName = dual.getInstrumentName();
				String lamp = ((XArc) calib).getLamp().getLampName();
				if (lamp.equalsIgnoreCase("Ne")) {
					if (instName.equals("FRODO_RED")) {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 500;
					} else {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 1000;
					}

				} else if (lamp.equalsIgnoreCase("Xe")) {
					if (instName.equals("FRODO_RED")) {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 60000;
					} else {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 60000;
						else
							return 60000;
					}
				} else if (lamp.equalsIgnoreCase("W")) {
					if (instName.equals("FRODO_RED")) {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 1000;
					} else {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 100000;
						else
							return 200000;
					}
				}
			}
		} else if (calib instanceof XLampFlat) {
			if (config instanceof XDualBeamSpectrographInstrumentConfig) {
				XDualBeamSpectrographInstrumentConfig dual = (XDualBeamSpectrographInstrumentConfig) config;
				int res = dual.getResolution();
				String instName = dual.getInstrumentName();
				String lamp = ((XLampFlat) calib).getLamp().getLampName();
				if (lamp.equalsIgnoreCase("Ne")) {
					if (instName.equals("FRODO_RED")) {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 500;
					} else {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 1000;
					}

				} else if (lamp.equalsIgnoreCase("Xe")) {
					if (instName.equals("FRODO_RED")) {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 60000;
					} else {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 60000;
						else
							return 60000;
					}
				} else if (lamp.equalsIgnoreCase("W")) {
					if (instName.equals("FRODO_RED")) {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 1000;
						else
							return 1000;
					} else {
						if (res == XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION)
							return 100000;
						else
							return 200000;
					}
				}
			}

		}
		return 60000; // no idea just a guess
	}

	/** Calculate the exposure time given the current config. */
	private double calculateExposureTime(IExposure exposure, IInstrumentConfig config) {

		// TODO We should actually go off and grab the relevant ICap and call
		// InstReg.getCapProvider(cfg.getInstName(), ICap.getExposureTime(IExp)

		if (exposure instanceof XMultipleExposure) {
			XMultipleExposure xmult = (XMultipleExposure) exposure;
			double exp = xmult.getExposureTime();
			double count = xmult.getRepeatCount();

			return count * (exp + calculateReadoutTime(config));

		} else if (exposure instanceof XPeriodExposure) {

			XPeriodExposure xtrig = (XPeriodExposure) exposure;
			double exp = xtrig.getExposureTime();
			return exp + calculateReadoutTime(config);

		} else if (exposure instanceof XPeriodRunAtExposure) {
			XPeriodRunAtExposure xper = (XPeriodRunAtExposure) exposure;
			double exp = xper.getExposureLength();
			double duration = xper.getTotalExposureDuration();
			return duration + exp + 15000; // add some time on for setup
		} 
		
		return 0.0;
	}

	/** Calculate readout times for different configs. */
	private double calculateReadoutTime(IInstrumentConfig config) {
		if (config == null)
			return 1000.0;
		// TODO A fudge to workround problem in migration/extraction with
		// meaburn configs
		XInstrumentConfig xcfg = (XInstrumentConfig) config;
		String instName = xcfg.getInstrumentName();
		XDetectorConfig xdc = (XDetectorConfig) xcfg.getDetectorConfig();
		int xb = xdc.getXBin();
		int yb = xdc.getYBin();
		int binxy = xb * yb;
		// catch dodgy zero binning
		if (binxy == 0)
			binxy = 1;

		if (instName.equals("RATCAM")) {
			return (double) (1000.0 * (CCD_READOUT_TIME_MULT_FACTOR / (double) binxy + CCD_READOUT_TIME_FIXED_OVERHEAD));
		} else if (instName.equals("SUPIRCAM")) {
			return IRCAM_DEFAULT_READOUT_TIME;
		} else if (instName.equals("RINGO2")) {
			return POLARIMETER_READOUT_TIME;
		} else if (instName.equals("RINGO3")) {
			return POLARIMETER_READOUT_TIME;
		} else if (instName.equals("MEABURN")) {
			return LOWRES_SPEC_READOUT_TIME;
		} else if (instName.equals("RISE")) {
			return RISE_DEFAULT_READOUT_TIME;
		} else if (instName.startsWith("FRODO")) {
			return FRODO_DEFAULT_READOUT_TIME;
		} else if (instName.equals("IO:O")) {
			return IO_O_DEFAULT_READOUT_TIME;
		} else if (instName.equals("IO:THOR")) {
			return IO_THOR_DEFAULT_READOUT_TIME;
		} else
			return GENERIC_DEFAULT_READOUT_TIME;

	}

}
