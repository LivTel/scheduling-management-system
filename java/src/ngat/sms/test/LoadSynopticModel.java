/**
 * 
 */
package ngat.sms.test;

import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.sms.BaseModelLoader;
import ngat.sms.BasicAccountSynopsisModel;
import ngat.sms.BasicHistorySynopsisModel;
import ngat.sms.BasicPhase2Cache;
import ngat.sms.DefaultBaseModelProvider;
import ngat.sms.DefaultPhase2LoadContoller;
import ngat.sms.DefaultSynopticModelProvider;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.FileLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class LoadSynopticModel {
	
	private String remoteHost;
	
	private Logger alogger;
	
	private LogGenerator logger;
	
	/**
	 * 
	 */
	public LoadSynopticModel(String remoteHost) {
		this.remoteHost = remoteHost;
		
		alogger = LogManager.getLogger("SMS");
		alogger.setLogLevel(5);
		ConsoleLogHandler console = new ConsoleLogHandler(new BasicLogFormatter(150));
		console.setLogLevel(4);
		alogger.addExtendedHandler(console);
		
		/*try {
			FileLogHandler flog = new FileLogHandler("smp", new BasicLogFormatter(150), FileLogHandler.DAILY_ROTATION);
			flog.setLogLevel(4);
			alogger.addExtendedHandler(flog); // NOT AN EXTENDED HANDLER TYPE!
		} catch (Exception e) {
			e.printStackTrace();		
		}*/
		logger = alogger.generate().system("SMS")
		.subSystem("Synoptics")
		.srcCompClass(this.getClass().getSimpleName())
		.srcCompId("loader");
		
	}
	
	public void exec(ISite site) throws Exception {
		
		DefaultBaseModelProvider bmp = new DefaultBaseModelProvider(remoteHost);
		logger.create().block("exec").info().level(1)
			.msg("Found Base Model Provider").send();
		
		BasicPhase2Cache cache = new BasicPhase2Cache("L1");
		BaseModelLoader loader = new BaseModelLoader(bmp);
		
		DefaultSynopticModelProvider smp = new DefaultSynopticModelProvider(bmp, loader, cache, site);
		logger.create().block("exec").info().level(1)
			.msg("Built Synoptic Model Provider").send();
		
		smp.asynchBind();
		
		smp.asynchLoadModels();
		
		smp.asynchReregister();
				
		smp.asynchBindLoadController();
			
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
			try {

				String remoteHost = args[0];
			
				double lat = Math.toRadians(Double.parseDouble(args[1]));
				double lon = Math.toRadians(Double.parseDouble(args[2]));
				
				BasicSite site = new BasicSite("Test", lat, lon);
				
				final LoadSynopticModel loader = new LoadSynopticModel(remoteHost);								
				// Register a Shutdown hook for unexpected termination.
				Runtime.getRuntime().addShutdownHook(new Thread() {
				    public void run() {
					loader.emergencyShutdown();
				    }
				});
								
				loader.exec(site);
								
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	public void emergencyShutdown() {		
		logger.create().block("emergencyShutdown").info().level(1)
		.msg("Shutting down logging streams").send();
		
		alogger.close();
	}



}
