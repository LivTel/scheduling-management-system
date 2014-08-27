/**
 * 
 */
package ngat.sms.genetic.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;

import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCalculator;
import ngat.sms.BasicSynopticModelProxy;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.util.FeasibilityPrescan;
import ngat.sms.util.PrescanEntry;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/** Generates some sequences by looking ahead for a shortish period and generates multiple futures 
 * based on a range of possible environmental scenarios.
 * @author eng
 *
 */
public class ShortHorizonLookaheadMultiEnv {
	
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			sdf.setTimeZone(UTC);

			Logger alogger = LogManager.getLogger("GSS");
			alogger.setLogLevel(3);
			ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
			console.setLogLevel(3);
			alogger.addExtendedHandler(console);

			LogGenerator logger = new LogGenerator(alogger);
			logger.system("SMS").subSystem("GSS").srcCompClass("SeqGen");
			
			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
			
			// Is it day or night at this time at this location ?

			long time = System.currentTimeMillis();

			double lat = Math.toRadians(cfg.getDoubleValue("lat"));
			double lon = Math.toRadians(cfg.getDoubleValue("long"));
			
			ISite site = new BasicSite("test-site", lat, lon);

			BasicSynopticModelProxy smp = new BasicSynopticModelProxy("localhost");
			smp.asynchLoadSynopticModel();
			ChargeAccountingModel chargeModel = new StandardChargeAccountingModel(); 
			TestResourceUsageEstimator tru = new TestResourceUsageEstimator();
			BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();
			scope.setDomeLimit(Math.toRadians(25.0));
			scope.setAutoguiderStatus(true);

			// TODO no ISM !
			FeasibilityPrescan fp =new FeasibilityPrescan(site, smp, tru, chargeModel, scope, null);
			List candidates = fp.prescan(time, 60*1000L);

			double exectot = 0.0;
			Iterator ig = candidates.iterator();
			while (ig.hasNext()) {
				PrescanEntry pse = (PrescanEntry) ig.next();
				exectot += pse.execTime;
			}
			System.err.println("Total available exectime: " + (exectot / 3600000.0) + " H");

			System.err.println("Ready to generate sequences...");

			AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);

			SolarCalculator sun = new SolarCalculator();
			Coordinates sunTrack = sun.getCoordinates(time);
			double sunlev = astro.getAltitude(sunTrack, time);

			logger.create().extractCallInfo().level(2).msg("Sun elevation is: " + Math.toDegrees(sunlev)).send();
			boolean daytime = (sunlev > 0.0);

			long son = 0l;
			long eon = 0l;
			if (daytime) {
				logger.create().extractCallInfo().level(2).msg("It is currently daytime").send();
				son = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
				eon = son + astro.getTimeUntilNextRise(sunTrack, 0.0, son);
			} else {
				logger.create().extractCallInfo().level(2).msg("It is currently nighttime").send();
				son = time;
				eon = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
			}
			logger.create().extractCallInfo().level(2).msg(
					"Start seqeunces at: " + sdf.format(new Date(son)) + ", End at: " + sdf.format(new Date(eon))
							+ " Night duration: " + ((eon - son) / 3600000) + "H").send();

			
			// ready to roll...
			// ith slot has 4^i possible paths leading to it
				
				
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
