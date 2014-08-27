/**
 * 
 */
package ngat.sms.models.standard.test;

import java.rmi.Naming;

import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.oss.monitor.Phase2Monitor;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.BasicPhase2Cache;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.DefaultBaseModelProvider;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.PrescanLoader;
import ngat.sms.PrescanPhase2UpdatePlugin;
import ngat.sms.SynopticModelProvider;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.util.FeasibilityPrescan;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class LoadPhase2CacheLevel2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
	
			Logger alogger = LogManager.getLogger("SMS");
			alogger.setLogLevel(2);
			ConsoleLogHandler console = new ConsoleLogHandler(new BasicLogFormatter(150));
			console.setLogLevel(2);
			alogger.addExtendedHandler(console);
			LogGenerator logger = alogger.generate().system("SMS")
			.subSystem("Synoptics")
			.srcCompClass("LoadTest")
			.srcCompId("loadtest");
			
			ISite site = new BasicSite("Obs", Math.toRadians(28.0), Math.toRadians(-17.0));

			ExecutionResourceUsageEstimationModel xrm = new TestResourceUsageEstimator();
			ChargeAccountingModel cam = new StandardChargeAccountingModel();
			BasicTelescopeSystemsSynopsis optscope = new BasicTelescopeSystemsSynopsis();
			optscope.setDomeLimit(Math.toRadians(25.0));
			optscope.setAutoguiderStatus(true);
			
			// SMP reference
			SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup("rmi://localhost/SynopticModelProvider");
					
			BasicInstrumentSynopsisModel bism = new BasicInstrumentSynopsisModel("rmi://ltsim1/InstrumentRegistry");
			bism.asynchLoadFromRegistry();
			
			FeasibilityPrescan fp = new FeasibilityPrescan(site, smp, xrm, cam, optscope, bism);
			
			
			PrescanLoader loader = new PrescanLoader(fp);
			BasicPhase2Cache cache = new BasicPhase2Cache("L2");
			
			Phase2Monitor phase2 = (Phase2Monitor)smp.getPhase2CompositeModel();
			phase2.addPhase2UpdateListener(cache);
			logger.create().extractCallInfo().level(1).msg("Registered L2 for phase2 updates from L1").send();
			
			try {Thread.sleep(5000L);} catch (InterruptedException ix) {}
			
			cache.loadCache(loader);
			logger.create().extractCallInfo().level(1).msg("Cache L2 is loaded").send();
			
			// Bind
			Naming.rebind("rmi://localhost/Phase2CompositeModel_L2", cache);
			logger.create().extractCallInfo().level(1).msg("Bound L2 cache...").send();
			
			DefaultBaseModelProvider bmp = new DefaultBaseModelProvider("localhost");
			
			// TODO add an update processor 
			PrescanPhase2UpdatePlugin processor = new PrescanPhase2UpdatePlugin(cache, bmp, fp);
			cache.startUpdateMonitor(processor);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
