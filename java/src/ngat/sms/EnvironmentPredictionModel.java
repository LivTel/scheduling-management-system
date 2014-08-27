package ngat.sms;

/**
 * 
 */

/**
 * @author eng
 *
 */
public interface EnvironmentPredictionModel {

	
	/** Implementors should return a prediction of the environment condition at time.
	 * @param time The time for which the prediction is required.
	 * @return A prediction of the environment condition at time.
	 * @throws Exception If something goes awry.
	 */
	public EnvironmentSnapshot predictEnvironment(long time) throws Exception;
	
}
