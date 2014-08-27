/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;

import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.icm.BasicInstrument;
import ngat.icm.InstrumentRegistry;
import ngat.phase2.IBeamSteeringConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XGroup;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.phase2.XProposal;
import ngat.phase2.XTag;
import ngat.phase2.XUser;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.OptimisticLiveInstrumentSynopsisModel;
import ngat.sms.TelescopeSystemsSynopsis;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.genetic.test.OptimisticInstrumentSynopsis;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.sms.util.BasicTimingConstraintWindowCalculator;
import ngat.util.TimeWindow;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogCollator;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class FindWindowsForGroup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			Logger slogger = LogManager.getLogger("SMS");
			LogGenerator l = slogger.generate();
			LogCollator logger = l.create();
	
			logger.srcCompClass("Test").srcCompId("test").system("").subSystem("");
			
			slogger.setLogLevel(5);
			ConsoleLogHandler console = new ConsoleLogHandler(new BasicLogFormatter(150));
			console.setLogLevel(5);
			
			//slogger.addExtendedHandler(console);
			
			System.setProperty("rotator.sky.base.offset", "35.0");
			InstrumentRegistry ireg = (InstrumentRegistry)Naming.lookup("rmi://ltsim1/InstrumentRegistry");
			
			OptimisticLiveInstrumentSynopsisModel oism = new OptimisticLiveInstrumentSynopsisModel();
			oism.loadFromRegistry(ireg);
			
			BasicSite site = new BasicSite("",Math.toRadians(28.0), Math.toRadians(-17.0));
			BasicTelescopeSystemsSynopsis telescope = new BasicTelescopeSystemsSynopsis();
			telescope.setAutoguiderStatus(true);
			telescope.setAutoguiderTempStatus(true);
			telescope.setDomeLimit(Math.toRadians(25.0));
			telescope.setSite(site);
			
			BasicAstrometryCalculator astro = new BasicAstrometryCalculator();
			ExecutionResourceUsageEstimationModel exec = new TestResourceUsageEstimator();
			
			StandardChargeAccountingModel charge = new StandardChargeAccountingModel();
			StandardExecutionFeasibilityModel xfm = new 
					StandardExecutionFeasibilityModel(astro, exec, charge, site, telescope, oism);
			xfm.setIgnoreAccounts(true);
			xfm.setIgnoreBeamSteering(true);
			xfm.setIgnoreInstruments(true);
			
			// 1 minute step
			BasicTimingConstraintWindowCalculator tcwc = new BasicTimingConstraintWindowCalculator(exec, xfm, site, 60000L);
			
			long DAYS = 24*3600*1000L;
			long HOURS = 3600*1000L;
			
			long today = System.currentTimeMillis();
			
			XGroup g = new XGroup();
			g.setName("testgroup");
			g.setTimingConstraint(new XMonitorTimingConstraint(today - 6*DAYS, today+30*DAYS, 3*HOURS, 1*HOURS));
			g.setActive(true);
			
			XProposal p = new XProposal("P1");
			p.setActivationDate(today - 60*DAYS);
			p.setExpiryDate(today +60*DAYS);
			p.setEnabled(true);
			
			XTag tag = new XTag();
			XUser user = new XUser("bert.smith");
			
			ISequenceComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));
			
			GroupItem group = new GroupItem(g, root);
			group.setProposal(p);
			group.setTag(tag);
			group.setUser(user);
			
			TimeWindow window = new TimeWindow(today +8*HOURS, today + 18*HOURS);
			
			EnvironmentSnapshot env = new EnvironmentSnapshot(today, 0.3, EnvironmentSnapshot.EXTINCTION_PHOTOM);
			ExecutionHistorySynopsis hist = new ExecutionHistorySynopsis();
			hist.setCountExecutions(1);
			hist.setLastExecution(today-4*HOURS);
			
			long time = today;
			long end = time + 3*DAYS;

			while (time < end) {

			    env = new EnvironmentSnapshot(time, 0.3, EnvironmentSnapshot.EXTINCTION_PHOTOM);
			    TimeWindow tw = tcwc.getWindow(group, hist, time);

			    System.err.printf("TESTING %tT %tF \n", time, time);
			    
			    if (tw != null) {
				
				long rt = tcwc.calculateRemainingTime(group, tw, time, env, hist);
				
				System.err.printf("DATA %tF %tT %4.2f \n", time, time, (double)rt/1000.0);
				
			    } else {
				System.err.printf("DATA %tF %tT 0.0 \n", time, time);

			    }

			    time += 5*60*1000L; // 5 minute steps
			}

			
			//		TimeWindow tw = tcwc.getWindow(group, hist, today + 9*HOURS);
			//System.err.println("TW: "+tw);
			
			//int rn = tcwc.countRemainingNights(group, tw, tw.start+20*60*1000L, env, hist);
			//System.err.println("RN: "+rn);
			
			//long rt = tcwc.calculateRemainingTime(group, tw, tw.start+20*60*1000L, env, hist);
			//System.err.println("RT: "+rt);
			
			
		} catch (Exception e) {
			e.printStackTrace();			
		}

	}

}
