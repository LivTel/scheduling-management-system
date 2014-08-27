/**
 * 
 */
package ngat.sms;

import java.util.Iterator;
import java.util.List;
import ngat.phase2.IGroup;
import ngat.phase2.IObservingConstraint;
import ngat.phase2.XAirmassConstraint;
import ngat.phase2.XHourAngleConstraint;
//import ngat.phase2.XLunarDistanceConstraint;
//import ngat.phase2.XLunarElevationConstraint;
//import ngat.phase2.XLunarPhaseConstraint;
import ngat.phase2.XPhotometricityConstraint;
import ngat.phase2.XSeeingConstraint;
import ngat.phase2.XSkyBrightnessConstraint;
//import ngat.phase2.XSolarElevationConstraint;

/**
 * @author eng
 * 
 */
public class ObservingConstraintAdapter {

	private XPhotometricityConstraint photometricityConstraint;

	private XSeeingConstraint seeingConstraint;

	private XAirmassConstraint airmassConstraint;

	private XHourAngleConstraint hourAngleConstraint;

	private XSkyBrightnessConstraint skyBrightnessConstraint;

	public ObservingConstraintAdapter(IGroup group) {

		// first create some defaults..
		seeingConstraint = new XSeeingConstraint(1.5);
		
		List ocs = group.listObservingConstraints();
		Iterator ioc = ocs.iterator();
		while (ioc.hasNext()) {
			IObservingConstraint oc = (IObservingConstraint) ioc.next();

			//if (oc instanceof XLunarElevationConstraint)
			//	(lunarElevationConstraint = (XLunarElevationConstraint) oc;
			if (oc instanceof XPhotometricityConstraint)
				photometricityConstraint = (XPhotometricityConstraint) oc;
			//else if (oc instanceof XLunarPhaseConstraint)
			//	lunarPhaseConstraint = (XLunarPhaseConstraint) oc;
			else if (oc instanceof XSeeingConstraint)
				seeingConstraint = (XSeeingConstraint) oc;
			else if (oc instanceof XAirmassConstraint)
				airmassConstraint = (XAirmassConstraint) oc;
			else if (oc instanceof XHourAngleConstraint)
				hourAngleConstraint = (XHourAngleConstraint) oc;
			else if (oc instanceof XSkyBrightnessConstraint)
				skyBrightnessConstraint = (XSkyBrightnessConstraint) oc;
			//else if (oc instanceof XLunarDistanceConstraint)
			//	lunarDistanceConstraint = (XLunarDistanceConstraint) oc;
			//else if (oc instanceof XSolarElevationConstraint)
			//	solarElevationConstraint = (XSolarElevationConstraint) oc;

		}

	}

	/**
	 * @return the lunarElevationConstraint
	 *//*
	public XLunarElevationConstraint getLunarElevationConstraint() {
		return lunarElevationConstraint;
	}
*/
	/**
	 * @return the photometricityConstraint
	 */
	public XPhotometricityConstraint getPhotometricityConstraint() {
		return photometricityConstraint;
	}

	/**
	 * @return the lunarPhaseConstraint
	 *//*
	public XLunarPhaseConstraint getLunarPhaseConstraint() {
		return lunarPhaseConstraint;
	}*/

	/**
	 * @return the seeingConstraint
	 */
	public XSeeingConstraint getSeeingConstraint() {
		return seeingConstraint;
	}

	/**
	 * @return the airmassConstraint
	 */
	public XAirmassConstraint getAirmassConstraint() {
		return airmassConstraint;
	}

	/**
	 * @return the hourAngleConstraint
	 */
	public XHourAngleConstraint getHourAngleConstraint() {
		return hourAngleConstraint;
	}

	/**
	 * @return the skyBrightnessConstraint
	 */
	public XSkyBrightnessConstraint getSkyBrightnessConstraint() {
		return skyBrightnessConstraint;
	}

	/**
	 * @return the lunarDistanceConstraint
	 *//*
	public XLunarDistanceConstraint getLunarDistanceConstraint() {
		return lunarDistanceConstraint;
	}

	*//**
	 * @return the solarElevationConstraint
	 *//*
	public XSolarElevationConstraint getSolarElevationConstraint() {
		return solarElevationConstraint;
	}*/
	
	

}
