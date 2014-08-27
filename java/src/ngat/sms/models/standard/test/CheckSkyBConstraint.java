package ngat.sms.models.standard.test;

import ngat.util.*;
import ngat.phase2.*;
import ngat.astrometry.*;
import java.text.*;
import java.util.*;

public class CheckSkyBConstraint {

    public static void main(String args[]) {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	SimpleTimeZone utc = new SimpleTimeZone(0, "UTC");
	sdf.setTimeZone(utc);

	try {

	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

	    double ra  = AstroFormatter.parseHMS(config.getProperty("ra"), ":");
	    double dec = AstroFormatter.parseDMS(config.getProperty("dec"), ":");
	    
	    XExtraSolarTarget target = new XExtraSolarTarget("test");
	    target.setRa(ra);
	    target.setDec(dec);
	    
	    double lat = Math.toRadians(config.getDoubleValue("lat"));
	    double lon = Math.toRadians(config.getDoubleValue("long"));

	    ISite site = new BasicSite("LT", lat, lon);
	    
	    TargetTrackCalculator track = new BasicTargetCalculator(target, site);

	    SkyBrightnessCalculator skycalc = new SkyBrightnessCalculator(site);
	    
	    long time = (sdf.parse(config.getProperty("time"))).getTime();
	    
	    long exec = 3600*1000*config.getLongValue("exec");


	    // test at start, end and middle of exec
	    long startExec = time;
	    long midExec = time + (long)(0.5*(double)exec);
	    long endExec = time + exec;
	    int actualSkyCat1 = skycalc.getSkyBrightnessCriterion(track, startExec);
	    int actualSkyCat2 = skycalc.getSkyBrightnessCriterion(track, midExec);
	    int actualSkyCat3 = skycalc.getSkyBrightnessCriterion(track, endExec);

	    // find worst value...
	    int actualSkyCat = Math.max(actualSkyCat1, actualSkyCat2);
	    actualSkyCat = Math.max(actualSkyCat, actualSkyCat3);

	    double actualSky1 = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat1);
	    double actualSky2 = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat2);
	    double actualSky3 = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat3);

	    double actualSky = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat);


	    System.err.printf("Checking skyb for tgt: (%4.2f, %4.2f)degs, (s)= %4.2f %4d (m)= %4.2f %4d (e)= %4.2f %4d use: %4.2f %4d\n",
			      ra, dec,
			      actualSky1,actualSkyCat1,
			      actualSky2,actualSkyCat2,
			      actualSky3,actualSkyCat3,
			      actualSky,actualSkyCat);
	    
	    
	
	
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}