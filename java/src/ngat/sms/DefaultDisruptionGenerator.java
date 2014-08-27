package ngat.sms;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ngat.phase2.ITimePeriod;
import ngat.phase2.XTimePeriod;
import ngat.sms.simulation.DisruptionGenerator;

/**
 * @author eng
 *
 */
public class DefaultDisruptionGenerator implements DisruptionGenerator {

	private List<Disruptor> events;
	
	/**
	 * Create a DefaultDisruptionGenerator.
	 */
	public DefaultDisruptionGenerator() {
		events = new Vector<Disruptor>();
	}
	
	/** Are there any disruptors in force at time.
	 * @see ngat.sms.simulation.DisruptionGenerator#hasDisruptor(long)
	 */
	public Disruptor hasDisruptor(long time) {
		Iterator<Disruptor> ie = events.iterator();
		while (ie.hasNext()) {
			Disruptor d = ie.next();
			if (d.getPeriod().contains(time))
				return d;
		}
		return null;
	}

	/** What if anything is first disruptor strictly after t1 and before t2.
	 * @see ngat.sms.simulation.DisruptionGenerator#nextDisruptor(long, long)
	 */
	public Disruptor nextDisruptor(long time1, long time2) {
		Disruptor first = null;
		long earliest = time2;
		ITimePeriod period = new XTimePeriod(time1, time2);
		Iterator<Disruptor> ie = events.iterator();
		while (ie.hasNext()) {
			Disruptor d = ie.next();
			if (d.getPeriod().getStart() > time1 && d.getPeriod().getStart() < time2) {
				if (d.getPeriod().getStart() < earliest) {
					earliest = d.getPeriod().getStart();
					first = d;
				}
			}
		}
		return first; // may be null
	}

	public void addDisruptor(Disruptor d) {
		events.add(d);	
	}
}
