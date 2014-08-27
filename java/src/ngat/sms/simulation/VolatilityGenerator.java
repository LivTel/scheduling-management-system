/**
 * 
 */
package ngat.sms.simulation;

/** Generates volatility events.
 * @author eng
 *
 */
public interface VolatilityGenerator {
	
	/** Request vgen to fire any events pending in t1,t2. These cannot be re-fired unless the generator is reset.*/
	public void fireEvents(long time1, long time2);
	
	/** Reset the generator so events can be fired again, useful in a simulation where the same period is re-run many times.*/
	public void reset();

}
