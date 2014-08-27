/**
 * 
 */
package ngat.sms.models.standard;

import java.util.Iterator;
import java.util.List;

import ngat.phase2.*;
import ngat.sms.*;

/**
 * @author eng
 * 
 */
public class StandardChargeAccountingModel implements ChargeAccountingModel {

	/** General housekeeping at start and end of group execution.*/
	public static final double GROUP_SETUP_TIME = 20000L;
	
	public static final double SLEW_TIME = 60000.0;
	public static final double ACQUIRE_TIME = 10000.0;
	public static final double AUTO_ACQUIRE_TIME = 10000.0;
	public static final double DEFOCUS_TIME = 5000.0;
	public static final double OFFSET_TIME = 8000.0;
	public static final double ROTATE_TIME = 30000.0;
	public static final double BEAM_CONFIG_TIME = 10000.0;

	// TODO catch all for ALL calibs, this needs detailed configuration
	public static final double CALIB_TIME = 20000.0; 
	public static final double DEFAULT_CONFIG_TIME = 10000.0;

   public static final double IO_O_DEFAULT_CONFIG_TIME = 5000.0;
    
	public static final double CCD_READOUT_TIME_MULT_FACTOR = 10.667;
	public static final double CCD_READOUT_TIME_FIXED_OVERHEAD = 9.33;
	public static final double IRCAM_DEFAULT_READOUT_TIME = 20000.0;
	public static final double POLARIMETER_READOUT_TIME = 5000.0;
	public static final double LOWRES_SPEC_READOUT_TIME = 20000.0;
	public static final double RISE_DEFAULT_READOUT_TIME = 35.0;
    public static final double IO_THOR_DEFAULT_READOUT_TIME = 0.0;
	public static final double FRODO_DEFAULT_READOUT_TIME = 20000.0;
    public static final double IO_O_DEFAULT_READOUT_TIME = 19000.0;
	public static final double GENERIC_DEFAULT_READOUT_TIME = 10000.0;

	private volatile IInstrumentConfig lastConfig = null;


    public double calculateCost(GroupItem group) {
	return calculateCost(group.getSequence());
    }

	/**
	 *  Note I have added a setup/teardown cost here so this cannot be used for subsequences.
	 * 
	 * @see
	 * ngat.sms.ChargeAccountingModel#calculateCost(ngat.phase2.ISequenceComponent
	 * )
	 */
	public double calculateCost(ISequenceComponent sequence) {
		return GROUP_SETUP_TIME + getExecTime(sequence);
	}

	private double getExecTime(ISequenceComponent seq) {
		double total = 0.0;

		if (seq instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) seq;
			XIteratorRepeatCountCondition cc = (XIteratorRepeatCountCondition) iter.getCondition();
			int count = cc.getCount();

			List list = seq.listChildComponents();
			if (list == null) {
				System.err.println("WARNING - TCAM::getExecTime():Null component list returned from iterator");
				return 0.0;
			}
			Iterator il = list.iterator();
			while (il.hasNext()) {
				ISequenceComponent sc = (ISequenceComponent) il.next();
				total += getExecTime(sc);
			}
			return count * total;

		} else if (seq instanceof XBranchComponent) {
			
			XBranchComponent branch = (XBranchComponent)seq;
			
			double maxTime = 0.0;
			List list = seq.listChildComponents();
			if (list == null) {
				System.err.println("WARNING - TCAM::getExecTime():Null component list returned from branch");
				return 0.0;
			}
			Iterator il = list.iterator();
			while (il.hasNext()) {
				ISequenceComponent sc = (ISequenceComponent) il.next();
				double branchTime = getExecTime(sc);
				if (branchTime > maxTime) {
					maxTime = branchTime;
				}
			}
			return maxTime;
			
		} else if (seq instanceof XExecutiveComponent) {
			total += getActionExecTime((XExecutiveComponent) seq);
		}

		return total;
		
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
		} else if(action instanceof IBeamSteeringConfig) {
			return BEAM_CONFIG_TIME;
		} else if (action instanceof IExposure) {
			return calculateExposureTime((IExposure) action, lastConfig);
		} else if (action instanceof IFocusOffset) {
			return DEFOCUS_TIME;
		} else if (action instanceof IInstrumentConfigSelector) {
			IInstrumentConfigSelector ics = (IInstrumentConfigSelector)action;
			lastConfig = ics.getInstrumentConfig();
			return calculateConfigTime(lastConfig);
		} else if (action instanceof IMosaicOffset) {
			return OFFSET_TIME;
		} else if (action instanceof IRotatorConfig) {
			return ROTATE_TIME;
		} else if (action instanceof ICalibration) {
			 return 20000 + calculateCalibrationTime(((ICalibration)action), lastConfig);
		}
		return 0.0;

	}
	
	private double calculateCalibrationTime(ICalibration calib, IInstrumentConfig config) {
		if (calib instanceof XArc) {
			if (config instanceof XDualBeamSpectrographInstrumentConfig) {
				XDualBeamSpectrographInstrumentConfig dual = (XDualBeamSpectrographInstrumentConfig)config;
				int res = dual.getResolution();
				String instName = dual.getInstrumentName();
				String lamp = ((XArc)calib).getLamp().getLampName();
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
					
				} else if 
				(lamp.equalsIgnoreCase("Xe")) {
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
				} else if
				(lamp.equalsIgnoreCase("W")) {
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
		else if (calib instanceof XLampFlat) {
			if (config instanceof XDualBeamSpectrographInstrumentConfig) {
				XDualBeamSpectrographInstrumentConfig dual = (XDualBeamSpectrographInstrumentConfig)config;
				int res = dual.getResolution();
				String instName = dual.getInstrumentName();
				String lamp = ((XLampFlat)calib).getLamp().getLampName();
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
					
				} else if 
				(lamp.equalsIgnoreCase("Xe")) {
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
				} else if
				(lamp.equalsIgnoreCase("W")) {
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

		if (exposure instanceof XMultipleExposure) {
			XMultipleExposure xmult = (XMultipleExposure) exposure;
			double exp = xmult.getExposureTime();
			double count = xmult.getRepeatCount();

			return count * (exp + calculateReadoutTime(config));
			
		} else if 
			(exposure instanceof XPeriodExposure) {
		    XPeriodExposure xtrig = (XPeriodExposure)exposure;
		    double duration = xtrig.getExposureTime();
		    
		    return duration + calculateReadoutTime(config);
		} else if
			(exposure instanceof XPeriodRunAtExposure) {
			XPeriodRunAtExposure xper = (XPeriodRunAtExposure)exposure;
			double exp = xper.getExposureLength();
			double duration = xper.getTotalExposureDuration();
			return duration+exp+15000; // add some time on for setup
		}   
		return 0.0;
	}

	private double calculateConfigTime(IInstrumentConfig config) {

	    String instName = config.getInstrumentName();
	    if (instName.equalsIgnoreCase("IO:O"))
	    	return IO_O_DEFAULT_CONFIG_TIME;
	    return DEFAULT_CONFIG_TIME;
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

		if (instName.equalsIgnoreCase("RATCAM")) {
			return (double) (1000.0 * (CCD_READOUT_TIME_MULT_FACTOR / (double) binxy + CCD_READOUT_TIME_FIXED_OVERHEAD));
		} else if (instName.equalsIgnoreCase("SUPIRCAM")) {
			return IRCAM_DEFAULT_READOUT_TIME;
		} else if (instName.equalsIgnoreCase("RINGO2")) {
			return POLARIMETER_READOUT_TIME;
		} else if (instName.equalsIgnoreCase("RINGO3")) {
			return POLARIMETER_READOUT_TIME;
		} else if (instName.equalsIgnoreCase("MEABURN")) {
			return LOWRES_SPEC_READOUT_TIME;
		} else if (instName.equalsIgnoreCase("IO:THOR")) {
		    return IO_THOR_DEFAULT_READOUT_TIME;
		} else if (instName.equalsIgnoreCase("IO:O")) {
		    return IO_O_DEFAULT_READOUT_TIME;
		} else if (instName.equalsIgnoreCase("RISE")) {
			return RISE_DEFAULT_READOUT_TIME;
		} else if (instName.toUpperCase().startsWith("FRODO")) {
			return FRODO_DEFAULT_READOUT_TIME;
		} else
			return GENERIC_DEFAULT_READOUT_TIME;
	}

}
