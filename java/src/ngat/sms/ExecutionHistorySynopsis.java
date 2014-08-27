/**
 * 
 */
package ngat.sms;

import java.io.Serializable;

/** A synopsis of a group's execution history to-date.
 * @author eng
 *
 */
public class ExecutionHistorySynopsis implements Serializable {
	
	/** Time of last successful execution.*/
	private long lastExecution;
	
	/** Count of number of successful executions to-date.*/
	private int countExecutions;

	/** Time upto which this synopsis is valid.*/
	private long synopsisTime;	
		
	/**
	 * Create an ExecutionHistorySynopsis.
	 */
	public ExecutionHistorySynopsis() {
		super();
	}

	/**
	 * @return the lastExecution
	 */
	public long getLastExecution() {
		return lastExecution;
	}

	/**
	 * @param lastExecution the lastExecution to set
	 */
	public void setLastExecution(long lastExecution) {
		this.lastExecution = lastExecution;
	}

	/**
	 * @return the countExecutions
	 */
	public int getCountExecutions() {
		return countExecutions;
	}

	/**
	 * @param countExecutions the countExecutions to set
	 */
	public void setCountExecutions(int countExecutions) {
		this.countExecutions = countExecutions;
	}

	/**
	 * @return the synopsisTime
	 */
	public long getSynopsisTime() {
		return synopsisTime;
	}

	/**
	 * @param synopsisTime the synopsisTime to set
	 */
	public void setSynopsisTime(long synopsisTime) {
		this.synopsisTime = synopsisTime;
	}
	
	/**
	 * @return A readable description of this {@link ExecutionHistorySynopsis}
	 */
	public String toString() {
		return "[ExecutionHistory: Last="+lastExecution+", Count="+countExecutions+"]";
	}
	

}
