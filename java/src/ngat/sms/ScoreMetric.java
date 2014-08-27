/**
 * 
 */
package ngat.sms;

import java.io.Serializable;

/**
 * @author eng
 *
 */
public class ScoreMetric implements Serializable {

	/** Name of the metric (for ID purposes).*/
	private String metricName;
	
	/** Value of the metric.*/
	private double metricValue;
	
	/** Description of the metric (for display purposes).*/
	private String metricDescription;

	/** Create a ScoreMetric.
	 * @param metricName Name of the metric (for ID purposes).
	 * @param metricValue Value of the metric.
	 * @param metricDescription Description of the metric (for display purposes).
	 */
	public ScoreMetric(String metricName, double metricValue, String metricDescription) {
		this.metricName = metricName;
		this.metricValue = metricValue;
		this.metricDescription = metricDescription;
	}

	/**
	 * @return the metricName
	 */
	public String getMetricName() {
		return metricName;
	}

	/**
	 * @param metricName the metricName to set
	 */
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	/**
	 * @return the metricValue
	 */
	public double getMetricValue() {
		return metricValue;
	}

	/**
	 * @param metricValue the metricValue to set
	 */
	public void setMetricValue(double metricValue) {
		this.metricValue = metricValue;
	}

	/**
	 * @return the metricDescription
	 */
	public String getMetricDescription() {
		return metricDescription;
	}

	/**
	 * @param metricDescription the metricDescription to set
	 */
	public void setMetricDescription(String metricDescription) {
		this.metricDescription = metricDescription;
	}
	
	
	
	
}
