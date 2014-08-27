/**
 * 
 */
package ngat.sms.simulation.test;

import ngat.astrometry.AstroLib;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.JAstroSlalib;
import ngat.phase2.XGroup;
import ngat.sms.simulation.VolatilityGenerator;

/** A test version of a VolatilityGenerator. This version generates adhoc events with a <i>reach</i> from a 
 * falt distribution between rLo and rHi and a proximity between pLo and pHi.
 * @author eng
 *
 */
public class TestVolatilityGenerator implements VolatilityGenerator {
	
	/** Dome low limit.*/
	public static final double DOME = Math.toRadians(25.0);
	
	
	/** Minimum proximity.*/	
	long proxMin;
	
	/** Maximum proximity.*/	
	long proxMax;
	
	/** Minimum reach.*/
	long reachMin;
	
	/** Maximum reach.*/
	long reachMax;
	
	/** Site.*/
	ISite site;
	
			
	/**
	 * @param site The site for which this generator is defined.
	 * @param proxMin Minimum proximity.
	 * @param proxMax Maximum proximity.
	 * @param reachMin Minimum reach.
	 * @param reachMax Maximum reach.
	 */
	public TestVolatilityGenerator(ISite site, long proxMin, long proxMax, long reachMin, long reachMax) {
		super();
		this.site = site;
		this.proxMin = proxMin;
		this.proxMax = proxMax;
		this.reachMin = reachMin;
		this.reachMax = reachMax;
	}

	
	/** Fire events for the specified time period.*/
	public void fireEvents(long time1, long time2) {
		
		// how do we decide when these events are and how many ?
		long ts; // determined by proximity
		long te; // determined by reach
		//createEvent(t, ts, te);
		
		
	}

	/** Does nothing as generator works in ad-hoc fashion.
	 * @see ngat.sms.simulation.VolatilityGenerator#reset()
	 */
	public void reset() {}

	private void createEvent(long time, long ts, long te) {
		
		// first create a group.
		XGroup group = new XGroup();		
		group.setActive(true);
		
		// decide where the target must be in order to be visible between ts and te	
		// simple case is to have it transit at 1/2(ts+te)
		
		long tmid = (ts+te)/2;
		
		// choose a dec above horizon
		double dmin = DOME - 0.5*Math.PI + site.getLatitude();
		double dmax = Math.PI;
		
		AstrometrySiteCalculator asc = new BasicAstrometrySiteCalculator(site);
		
		AstroLib lib = new JAstroSlalib();
		double lst = lib.getLST(tmid, site.getLongitude());
		
		double ra = lst;
		//Coordinates c = new Coordinates(ra, dec);
		
		// this gives us the time above horizon
		//long tsr = asc.getTimeSinceLastRise(c, DOME, tmid);
		//long tts = asc.getTimeUntilNextSet(c, DOME, tmid);
			
	}
	
}
