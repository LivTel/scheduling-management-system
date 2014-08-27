package ngat.sms.test;

import ngat.astrometry.*;
import ngat.phase2.*;
import ngat.sms.*;

public class TestCalcTargetSeeing {

    public static void main(String args[]) {

	try {

	    ISite site=  new BasicSite("Obs", Math.toRadians(28.0), Math.toRadians(-17.0));
	    
	    XExtraSolarTarget target = new XExtraSolarTarget("star");
	    target.setRa(Math.toRadians(240.0));
	    target.setDec(Math.toRadians(25.0));

	    double seeing = 1.4;

	    BasicAstrometryCalculator astro = new BasicAstrometryCalculator();

	    TargetTrackCalculator track = new BasicTargetCalculator(target, site);

	    //double targetMinElev = astro.getMinimumAltitude(track, site, time, time + (long) execTime);

	    long start = System.currentTimeMillis();
	    long time = start;
	    while (time < start +24*3600*1000L) { // 1 day
		Coordinates c = track.getCoordinates(time);
		double targetElev = astro.getAltitude(c, site, time);
		double targetZd = 0.5*Math.PI-targetElev;
		double targetSeeing = seeing/Math.pow(Math.cos(targetZd), 0.5);
		int targetSeeingState = EnvironmentSnapshot.getSeeingCategory(targetSeeing);
		
		//    date      time     alt    see  cat (name)
		// 2009-09-30 01:22:22   35.0  0.23   2 (GOOD)
		
		System.err.printf("%tF %tT  %3.2f  %3.2f %2d (%s) \n", time, time, Math.toDegrees(targetElev), targetSeeing, targetSeeingState, EnvironmentSnapshot.getSeeingCategoryName(targetSeeingState)); 
		
		time += 10*60*1000L; // 10 minutes
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}