/**
 * 
 */
package ngat.sms;

/**
 * @author eng
 *
 */
public interface WeatherPredictionModel {

	/** Implementors should return a prediction of the weather condition at time.
	 * @param time The time for which the prediction is required.
	 * @return A prediction of the weather condition at time.
	 * @throws Exception If something goes awry.
	 */
	public WeatherSnapshot predictWeather(long time) throws Exception;
	
}
