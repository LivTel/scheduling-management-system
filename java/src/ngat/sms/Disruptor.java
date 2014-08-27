/**
 * 
 */
package ngat.sms;

import java.io.Serializable;

import ngat.phase2.ITimePeriod;

/**
 * @author eng
 *
 */
public class Disruptor implements Serializable {

	/** The disruption period. */
	private ITimePeriod period;
	
	/** The class of disruptor. */
	private String disruptorClass;
	
	/** Name of this disruptor. */
	private String disruptorName;
	

	/**
	 * @param disruptorName
	 * @param disruptorClass
	 * @param period
	 */
	public Disruptor(String disruptorName, String disruptorClass, ITimePeriod period) {
		this.disruptorName = disruptorName;
		this.disruptorClass = disruptorClass;
		this.period = period;
	}

	/**
	 * @return the period
	 */
	public ITimePeriod getPeriod() {
		return period;
	}

	/**
	 * @return the disruptorClass
	 */
	public String getDisruptorClass() {
		return disruptorClass;
	}

	/**
	 * @return the disruptorName
	 */
	public String getDisruptorName() {
		return disruptorName;
	}
	
	public String toString() {
		return disruptorClass+":"+disruptorName+", ["+period+"]";
	}
	
	
}
