/**
 * 
 */
package ngat.sms.models.standard.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import ngat.astrometry.BasicCardinalPointingCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.phase2.XExtraSolarTarget;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/** Checks and logs the rotator angle supplied.
 * @author eng
 *
 */
public class CheckSkyAngle {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
		
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
			sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
			
			Logger astroLog = LogManager.getLogger("ASTRO");
			astroLog.setLogLevel(0);
			// switched off for now
			ConsoleLogHandler aconsole = new ConsoleLogHandler(new BasicLogFormatter(150));
			aconsole.setLogLevel(5);
			astroLog.addExtendedHandler(aconsole);
			
			double IOFF = Math.toRadians(56.0);
			
			ISite site = new BasicSite("", Math.toRadians(28.0), Math.toRadians(-17.0));
			BasicCardinalPointingCalculator cpc = new BasicCardinalPointingCalculator(site);
			
			XExtraSolarTarget tgt = new XExtraSolarTarget("test");
			tgt.setRa(Math.toRadians(330.0));
			tgt.setDec(Math.toRadians(20.6));
			double instoff = Math.toRadians(-90.0);
			long time = (sdf.parse("2013-10-30T19:04:00")).getTime();
			
			System.err.println("Computed date: "+(new Date(time)) );
			astroLog.setLogLevel(5);
			boolean f = cpc.isFeasibleSkyAngle(Math.toRadians(295.0), tgt, IOFF - instoff, time, time + 137000L);
			//astroLog.setLogLevel(0);
			System.err.println("Return status: "+f);
			
			
			System.err.println("Test offsets for mount");
			double mount = 2.5*(Math.random()-0.5)*Math.PI;
			System.err.println("Testing mount angle: "+Math.toDegrees(mount));
			//for (int i = 0; i<50; i++) {
				double offset = 2.0*(Math.random()-0.5)*Math.PI; // -90 +90
				double initSkyAngle = cpc.getSkyAngle(mount, tgt, offset, time);
				// see if its actually a feasible skyangle.
				boolean ok = cpc.isFeasibleSkyAngle(initSkyAngle, tgt, offset, time, time + 137000L);
				System.err.printf("CheckRotator: RotMount: %4.2f InitSky: %4.2f = %b\n", 
					  Math.toDegrees(mount), Math.toDegrees(initSkyAngle), ok);
			
			//}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
