/**
 * 
 */
package ngat.sms;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @author eng
 *
 */
public class ScoreMetricsSet implements Serializable {
	
	/** Group's score.*/
	private double score;
	
	/** A List of metrics.*/
	List <ScoreMetric> metrics;

	
	/**
	 * 
	 */
	public ScoreMetricsSet() {
		metrics = new Vector<ScoreMetric>();
	}

	/**
	 * @param score
	 */
	public ScoreMetricsSet(double score) {
		this();
		this.score = score;
	}

	/** Add a metric to the set of metrics.*/
	public void addMetric(ScoreMetric metric) {
		metrics.add(metric);
	}

	/** Returns an iterator over the set of metrics.*/
	public Iterator<ScoreMetric> listMetrics() {
		return metrics.iterator();
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}
	
	
}
