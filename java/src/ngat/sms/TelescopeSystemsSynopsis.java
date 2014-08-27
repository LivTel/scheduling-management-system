/**
 * 
 */
package ngat.sms;

import ngat.astrometry.ISite;
import ngat.phase2.IBeamSteeringConfig;

/**
 * @author eng
 *
 */
public interface TelescopeSystemsSynopsis {

	/**
	 * 
	 * @return Details of the observatory site.
	 */
	public ISite getSiteDetails();
	
	
	/**
	 * @return Rotator sky base offset.
	 */
	public double getRotatorBaseOffset();
	
	/**
	 * 
	 * @return The lower limit on altitude due to dome obstruction (rads).
	 */
	public double getDomeLimit();
	
	/**
	 * 
	 * @return The size of the ZAZ (rads).
	 */
	public double getZenithAvoidanceZoneSize();
	
	/**
	 * 
	 * @return The maximum axis slew rate (rad/sec).
	 */
	public double getMaximumAxisSlewRate();

	/**
	 * 
	 * @return The range of the azimuth wrap.
	 */
	public double getAzimuthWrapLimits();
	
	/**
	 * 
	 * @return The range of the altitude wrap.
	 */
	public double getAltitudeWrapLimits();
	
	/**
	 * 
	 * @return The range of the rotator wrap.
	 */
	public double getRotatorWrapLimits();

    /**
     *
     * @return The time taken for the axes to settle after a slew.
     */
    public long getAxisSettleTime();
	
    /**
     *
     * @return The autoguider acquisition time.
     * // TODO may need to be parameter for target characteristics (whatever they may be)
     */
    public long getAutoguiderAcquireTime();

	/**
	 * 
	 * @return True if the AG is online.
	 */
	public boolean isAutoguiderOperational();
	
	
	/**
	 * 
	 * @return True if the AG is funcional.
	 */
	public boolean isAutoguiderFunctional();
	
	public boolean isValidBeamSteeringConfig(IBeamSteeringConfig beam);
	
}
