/**
 * 
 */
package ngat.sms.simulation;

import ngat.sms.Disruptor;

/** Generates disruption events
 * @author eng
 *
 */
public interface DisruptionGenerator {

	/** Find the next disruptor at or after time1 and strictly before time2.*/
	public Disruptor nextDisruptor(long time1, long time2);
	
	/** Find any disruptor in force at time.*/
	public Disruptor hasDisruptor(long time);
	
}
