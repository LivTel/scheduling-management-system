package ngat.sms.util;

import java.util.List;

import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.GroupItem;
import ngat.util.TimeWindow;



/** Calculates how much time is left for a group to execute in a given window.*/
public interface TimingConstraintWindowCalculator {

    /** Returns a List of feasible windows for the specified group which overlap the interval
     * [ts,te]. The windows may start any time before te and end anytime after ts.
     * @param group The group fro which feasible windows are required.
     * @param history The execution history of the group.
     * @param ts Start of the overlap interval.
     * @param te End of the overlap interval.
     */
    public List<TimeWindow> listFeasibleWindows(GroupItem group, ExecutionHistorySynopsis history, long ts, long te);

    /** Returns the window for the specified group which includes t, given history.
     * If there are no such windows, returns null.
     */
    public TimeWindow getWindow(GroupItem group, ExecutionHistorySynopsis history, long t);

    /** Returns how much time is left for a group to execute in a  window containing specified time.*/
    public long calculateRemainingTime(GroupItem group, TimeWindow window, long t,  EnvironmentSnapshot env, ExecutionHistorySynopsis hist);

    /** Returns how many nights the nominated group is executable on in the current window.*/
    public int countRemainingNights(GroupItem group, TimeWindow window, long t, EnvironmentSnapshot env, 
				    ExecutionHistorySynopsis hist);


}
