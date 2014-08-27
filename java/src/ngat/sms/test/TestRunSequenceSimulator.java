/**
 * 
 */
package ngat.sms.test;

import ngat.astrometry.BasicSite;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.XDetectorConfig;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XExtraSolarTarget;
import ngat.phase2.XFilterDef;
import ngat.phase2.XFilterSpec;
import ngat.phase2.XImagerInstrumentConfig;
import ngat.phase2.XInstrumentConfigSelector;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.phase2.XMultipleExposure;
import ngat.phase2.XPositionOffset;
import ngat.phase2.XRotatorConfig;
import ngat.phase2.XSlew;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.util.SequenceSimulator;

/**
 * @author eng
 *
 */
public class TestRunSequenceSimulator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*root {
						
			slew
			
			repeat x 6
			
				offset_rel x
			
				cfg 
			
				expose
				
				rotoff
			
			}
			
		}*/
		
		
		XIteratorComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));
		
		double ra = Math.random()*Math.PI*2.0;
		double dec = Math.random()*Math.PI*0.5;
		XExtraSolarTarget target = new XExtraSolarTarget("star");
		target.setRa(ra);
		target.setDec(dec);
		XRotatorConfig rotator = new XRotatorConfig(XRotatorConfig.CARDINAL, 0.0);
		rotator.setInstrumentName("IO:O");
		XSlew slew = new XSlew(target, rotator, false);
		XExecutiveComponent x = new XExecutiveComponent("slew", slew);
		root.addElement(x);
		
		XIteratorComponent inner = new XIteratorComponent("repeat", new XIteratorRepeatCountCondition(10));
		
		XPositionOffset poff = new XPositionOffset(true, 1.0, 1.0);
		x = new XExecutiveComponent("poff", poff);
		inner.addElement(x);
		
		XImagerInstrumentConfig config = new XImagerInstrumentConfig("sdss-u");
		XFilterSpec filters = new XFilterSpec();
		filters.addFilter(new XFilterDef("SDSS-U"));
		config.setFilterSpec(filters);
		config.setInstrumentName("IO:O");
		XDetectorConfig xdet = new XDetectorConfig();
		xdet.setXBin(2);
		xdet.setYBin(2);
		config.setDetectorConfig(xdet);
		x = new XExecutiveComponent("cfg", new XInstrumentConfigSelector(config));
		inner.addElement(x);
		
		x = new XExecutiveComponent("exp", new XMultipleExposure(30000, 5, true));
		inner.addElement(x);
		
		double ra2 = ra + Math.random()*0.05;
		double dec2 = dec +Math.random()*0.05;
		XExtraSolarTarget target2 = new XExtraSolarTarget("star2");
		target2.setRa(ra2);
		target2.setDec(dec2);
		XRotatorConfig rotator2 = new XRotatorConfig(XRotatorConfig.CARDINAL, 0.0);
		rotator2.setInstrumentName("IO:O");
		XSlew slew2 = new XSlew(target2, rotator2, false);
		x = new XExecutiveComponent("slew2", slew2);
		inner.addElement(x);
		
		root.addElement(inner);
		
		
		TestSimulationListener tsl = new TestSimulationListener();
		
		TestResourceUsageEstimator trx = new TestResourceUsageEstimator();
		SequenceSimulator ss = new SequenceSimulator(root, trx);
		ss.addSimulationListener(tsl);
		
		try {
		ss.startSimulation();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		System.err.println("Part 2 -----------------------------------------------");
		System.err.println();
		
		try {
			BasicSite site = new BasicSite("", Math.toRadians(28.0), Math.toRadians(-17.0));
			BasicInstrumentSynopsisModel ism = new BasicInstrumentSynopsisModel("rmi://ltsim1/InstrumentRegistry");
			ism.asynchLoadFromRegistry();
			System.err.println("Loaded ism data");
			
			try {Thread.sleep(5000L);} catch (InterruptedException ix) {}
			SimulationTrackingMonitorTest test = new SimulationTrackingMonitorTest(site, ism);
			test.testSequence(root, System.currentTimeMillis());
			System.err.println("Completed ok");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
