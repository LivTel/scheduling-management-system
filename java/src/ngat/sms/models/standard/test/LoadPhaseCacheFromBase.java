/**
 * 
 */
package ngat.sms.models.standard.test;

import java.rmi.Naming;
import java.rmi.RemoteException;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IAccountModel;
import ngat.oss.model.IHistoryModel;
import ngat.oss.model.IPhase2Model;
import ngat.oss.monitor.Phase2Monitor;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BaseModelLoader;
import ngat.sms.BaseModelPhase2UpdaterPlugin;
import ngat.sms.BaseModelProvider;
import ngat.sms.BasicPhase2Cache;
import ngat.sms.DefaultBaseModelProvider;
import ngat.sms.DefaultSynopticModelProvider;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.Phase2UpdateProcessor;
import ngat.sms.SynopticModelProvider;
import ngat.sms.models.standard.BasicPhase2CompositeModel;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class LoadPhaseCacheFromBase {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {

			Logger alogger = LogManager.getLogger("SMS");
			alogger.setLogLevel(5);
			ConsoleLogHandler console = new ConsoleLogHandler(new BasicLogFormatter(150));
			console.setLogLevel(4);
			alogger.addExtendedHandler(console);
			LogGenerator logger = alogger.generate().system("SMS")
			.subSystem("Synoptics")
			.srcCompClass("LoadTest")
			.srcCompId("loadtest");
			
			String host = args[0];			
			BaseModelProvider bmp = new DefaultBaseModelProvider(host);
			
			BaseModelLoader loader = new BaseModelLoader(bmp);
			
			BasicPhase2Cache cache = new BasicPhase2Cache("L1");
			
			Phase2Monitor phase2 = (Phase2Monitor)bmp.getPhase2Model();
			phase2.addPhase2UpdateListener(cache);
			logger.create().extractCallInfo().level(1).msg("Registered L1 for phase2 updates from BASE").send();
			
			try {Thread.sleep(5000L);} catch (InterruptedException ix) {}
			
			cache.loadCache(loader);
			logger.create().extractCallInfo().level(1).msg("Cache L1 is loaded").send();
			
			// Bind
			Naming.rebind("rmi://localhost/Phase2CompositeModel_L1", cache);
			logger.create().extractCallInfo().level(1).msg("Bound L1 cache...").send();
			
			Phase2UpdateProcessor processor = new BaseModelPhase2UpdaterPlugin(cache,bmp);
			cache.startUpdateMonitor(processor);
			
			while(true){try {Thread.sleep(30000L);} catch (InterruptedException ix) {}}
						
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
