/**
 * 
 */
package ngat.sms;

import ngat.phase2.ISequenceComponent;

/** Calculates the cost of running observation sequences - a more advanced model will
 * allow additional factors such as Environment condition to be included.
 * AN example would be where an exposure is set based on sky-brightness and hence
 * dependant on environment at the time it is performed.
 * @author eng
 *
 */
public interface ChargeAccountingModel {

	/** Calculate the cost of executing a particular observation sequence or sub-sequence.
	 * @param sequence The sequence to cost.
	 * @return The cost of executing the sequence.
	 */
	public double calculateCost(ISequenceComponent sequence);
	
}
