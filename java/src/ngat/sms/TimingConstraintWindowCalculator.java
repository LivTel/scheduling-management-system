/**
 * 
 */
package ngat.sms;

import java.util.Iterator;
import ngat.util.TimeWindow;

/**
 * @author eng
 *
 */
public interface TimingConstraintWindowCalculator {

	
	/** Which execution window is the group in.
	 * @param group The group to do the calculations for.
	 * @param history The group's execution history upto-date (time).
	 * @param time When.
	 * @return The window which the group is in (if any - may be null).
	 */
	public TimeWindow getWindow(GroupItem group, ExecutionHistorySynopsis history, long time);
	
	
	/** Returns list of execution windows for a group between times.
	 * @param group The group to do the calculations for.
	 * @param history The group's execution history upto-date (t1).
	 * @param t1 The time to start calculating for.
	 * @param t2 The time to stop calculating for.
	 * @return An iterator over the list of windows for the group between t12 and t2 (inclusive).
	 */
	public Iterator <TimeWindow> listWindows(GroupItem group, ExecutionHistorySynopsis history, long t1, long t2);
	
	
}
