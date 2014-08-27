/**
 * 
 */
package ngat.sms.tlas;

import java.util.Iterator;
import java.util.List;

import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.GroupItem;

/**
 * Container for information relating to a scheduling sequence sweep.
 * 
 * @author eng
 * 
 */
public class HorizonSweep {

	// TODO start and end or horizon length
	
	/**
	 * The group execution sequence.
	 */
	private List<SweepItem> sequence;

	/**
	 * The tally or score associated with executing the associated sequence.
	 */
	private double tally;

	/** Records estimated idle time in the sequence. */
	private long idleTime;

	/**
	 * Create a new empty HorizonSweep.
	 */
	public HorizonSweep() {
	}

	/**
	 * @param sequence
	 * @param tally
	 * @param idleTime
	 */
	public HorizonSweep(List<SweepItem> sequence, double tally, long idleTime) {
		this();
		this.sequence = sequence;
		this.tally = tally;
		this.idleTime = idleTime;
	}

	public boolean isEmpty() {
		return ((sequence == null) || sequence.isEmpty());
	}

	/**
	 * @return the idleTime
	 */
	public long getIdleTime() {
		return idleTime;
	}

	/**
	 * @param idleTime
	 *            the idleTime to set
	 */
	public void setIdleTime(long idleTime) {
		this.idleTime = idleTime;
	}

	/**
	 * @return the sequence
	 */
	public List<SweepItem> getSequence() {
		return sequence;
	}

	/**
	 * @param sequence
	 *            the sequence to set
	 */
	public void setSequence(List<SweepItem> sequence) {
		this.sequence = sequence;
	}

	/**
	 * @return the tally
	 */
	public double getTally() {
		return tally;
	}

	/**
	 * @param tally
	 *            the tally to set
	 */
	public void setTally(double tally) {
		this.tally = tally;
	}

	/** Calculate the estimated execution time.*/
	public long calculateTotalTime() {
		long total = 0L;
		Iterator<SweepItem> is = sequence.iterator();
		while (is.hasNext()) {
			SweepItem s = is.next();
			total += (long) s.getExec();
		}

		return total;

	}

	public String display() {
		StringBuffer display = new StringBuffer();
		Iterator<SweepItem> is = sequence.iterator();
		while (is.hasNext()) {
			SweepItem s = is.next();

			String gname = s.getGenome();
			display.append(gname + "-");
		}
		return display.toString();
	}

}
