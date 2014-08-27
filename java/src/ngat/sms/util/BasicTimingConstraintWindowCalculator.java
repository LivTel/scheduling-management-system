package ngat.sms.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import ngat.astrometry.ISite;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XEphemerisTimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.sms.AccountSynopsis;
import ngat.sms.CandidateFeasibilitySummary;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionFeasibilityModel;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.util.TimeWindow;


/** Calculates how much time is left for a group to execute in a given window. */
public class BasicTimingConstraintWindowCalculator implements TimingConstraintWindowCalculator {

    public static final long SAMP_INT = 10 * 60 * 1000L;

    public static final long FGB = 600000;

    public static final long DAYMS = 24 * 3600 * 1000L;

    /** An exec model. */
    ExecutionResourceUsageEstimationModel execModel;

    /** A feasibility model.*/
    ExecutionFeasibilityModel xfm;
	
    /** The site. */
    ISite site;

    /** Time resolution (ms). */
    long dt;

	
    /**
     * Create a BasicTimingConstraintWindowCalculator using the supplied
     * execModel and time resolution. Generally we will want to have disabled checking of accounts
     * in the feasibility model if this is possible with the specific implementation.
     */
    public BasicTimingConstraintWindowCalculator(ExecutionResourceUsageEstimationModel execModel, ExecutionFeasibilityModel xfm, ISite site, long dt) {
	this.execModel = execModel;
	this.xfm = xfm;
	this.site = site;
	this.dt = dt;
		
    }

    /** Returns the window which includes t or null. */
    public TimeWindow getWindow(GroupItem group, ExecutionHistorySynopsis history, long time) {

	//logger.method("getWindow(G,h,t)").log(
	//	2,
	//	"Checking feasible window for: Group: " + group.getName() + ", class=" + group.getClass().getName()
	//		+ ", including T=" + ScheduleSimulator.sdf.format(new Date(time)));

	ITimingConstraint timing = group.getTimingConstraint();
		
	if (timing == null)
	    return null;
		
	long grpExpDate = timing.getEndTime();

	// Special case: Expired before start of search interval.
	if (grpExpDate <= time) {
	    //logger.log(2, "Group " + group.getName() + " failed: POST_EXPIRY");
	    return null;
	}

	if (timing instanceof XMonitorTimingConstraint) {

	    XMonitorTimingConstraint mg = (XMonitorTimingConstraint)timing;
		
	    long startDate = mg.getStartDate();
	    long period = mg.getPeriod();
	    long window = mg.getWindow();
			
	    // special case: Not started monitoring.
	    if (startDate > time) 
		return null;

	    // special case: Monitoring completed.
	    long endDate = mg.getEndDate();
	    if (endDate <= time)
		return null;

	    // find first period before t.
	    double fPeriod = (double) (time - startDate) / (double) period;
	    int i = (int) Math.rint(fPeriod);

	    // calculate the window
	    long startFloat = startDate + (long) ((double) i * (double) period - (double) window / 2.0);
	    long endFloat = startDate + (long) ((double) i * (double) period + (double) window / 2.0);

	    // check we are actually in a window
	    if ((startFloat <= time) && (endFloat >= time)) {
				
		// Check it wasnt done already inside this window			
		if (history.getLastExecution() < startFloat) {
		    return new TimeWindow(startFloat, endFloat);
		} 
				
	    } 

	} else if (timing instanceof XFixedTimingConstraint) {

	    // the only possible window is (fg-b, fg+b)

	    XFixedTimingConstraint xfix = (XFixedTimingConstraint)timing;

	    // Special case: Already done before t
	    if (history.getLastExecution() > 0 && history.getLastExecution() < time) 				
		return null;
			
	    // FG can be done within specified minutes of the fixed time.
	    long fixed = xfix.getFixedTime();
	    long window = xfix.getSlack();
	
	    // check if outside fixed window			
	    if (((fixed - window/2) > time) || ((fixed + window/2) < time)) 			
		return null;


	    return new TimeWindow(fixed - window/2, fixed + window/2);

	} else if (timing instanceof XMinimumIntervalTimingConstraint) {

	    XMinimumIntervalTimingConstraint xmin = (XMinimumIntervalTimingConstraint)timing;
			
	    long startDate = xmin.getStartTime();
	    long endDate = xmin.getEndTime();
	    long minInterval = xmin.getMinimumInterval();
	    int maxRepeats = xmin.getMaximumRepeats();

	    // Special case: Not started monitoring.
	    if (startDate > time) 
		return null;
			
	    // Special case: Monitoring completed.
	    if (endDate < time) 
		return null;

	    // Special case: Too soon after last exec
	    if (time - history.getLastExecution() < minInterval) 
		return null;


	    // Special case: Repeat count exceeded
	    if (maxRepeats < history.getCountExecutions())
		return null;
		
	    // lets restrict the end time to now plus minInterval
	    return new TimeWindow(history.getLastExecution() + minInterval, time + minInterval);

	} else if (timing instanceof XEphemerisTimingConstraint) {

	    // similar to mg but using phase - only if not already done by start
	    XEphemerisTimingConstraint xephem = (XEphemerisTimingConstraint)timing;

	    long startDate = xephem.getStart();
	    long endDate = xephem.getEnd();
	    long period = xephem.getCyclePeriod();
	    double phase = xephem.getPhase();
	    long window = xephem.getWindow();
		
	    // Special case: Already done before t
	    if (history.getLastExecution() > 0 && history.getLastExecution() < time) 
		return null;

	    // Special case: Not started monitoring.
	    if (startDate > time)
		return null;
			
	    // Special case: Monitoring completed.
	    if (endDate <= time) 
		return null;
		
	    // first period before t
	    double fPeriod = Math.floor((time - startDate) / period);
	    int i = (int) fPeriod;

	    // TODO calculate the window - MAY need to cast stuff here !!!!
	    long startWindow = startDate + (long) ((i + phase)*period - window / 2.0);
	    long endWindow = startDate + (long) ((i + phase) * period + window / 2.0);

	    if (((startWindow <= time) && (endWindow >= time)))
		return new TimeWindow(startWindow, endWindow);

	} else {

	    // (fx.start, fx.end) but only if not already done by start.

	    XFlexibleTimingConstraint xflex = (XFlexibleTimingConstraint)timing;
			
	    long startFlex = xflex.getStartTime();
	    long endFlex = xflex.getEndTime();

	    // Special case: Already done before ts
	    if (history.getLastExecution() > 0 && history.getLastExecution() < time) 
		return null;

	    if ((startFlex < time) && (endFlex > time))
		return new TimeWindow(startFlex, endFlex);
	}

	return null;

    }

    /**
     * Returns a List of feasible windows for the specified group which overlap
     * the interval [ts,te]. The windows may start any time before te and end
     * anytime after ts.
     * 
     * @param group
     *            The group for which feasible windows are required.
     * @param history
     *            The execution history of the group.
     * @param ts
     *            Start of the overlap interval.
     * @param te
     *            End of the overlap interval.
     */
    public List<TimeWindow> listFeasibleWindows(GroupItem group, ExecutionHistorySynopsis history, long ts, long te) {

	// Find any windows which start before te and end after ts and have NOT
	// already been executed.
		
		
	List<TimeWindow> list = new Vector<TimeWindow>();

	ITimingConstraint timing = group.getTimingConstraint();
		
	if (timing == null)
	    return null;
		
	long grpExpDate = timing.getEndTime();

	// Special case: Expired before start of search interval.
	if (grpExpDate <= ts)
	    return null;

	if (timing instanceof XMonitorTimingConstraint) {

	    XMonitorTimingConstraint xmon = (XMonitorTimingConstraint)timing;
			
	    // work out all monitor periods from start to end of expiry/endmon

	    // gi = [s + i*p - 0.5*w, s + i*p + 0.5*w] for i = 0 to ibig
	    // ibig = (e-s)/p + 1

	    long startDate = xmon.getStartDate();
	    long period = xmon.getPeriod();		
	    long mwindow = xmon.getWindow();
			
	    // Special case: Not started monitoring.
	    if (startDate > te) 
		return null;
			
	    // Special case: Monitoring completed.
	    long endDate = xmon.getEndDate();
	    if (endDate <= ts) 
		return null;

	    // first period before ts.
	    double fPeriod = (double) (ts - startDate) / (double) period;
	    int i1 = (int) Math.floor(fPeriod);

	    // last period after te.
	    fPeriod = (double) (te - startDate) / (double) period;
	    int i2 = (int) Math.rint(fPeriod) + 1;

	    // Run through windows and see if overlapping
	    for (int i = i1; i < i2; i++) {

		// calculate the window
			
		long startFloat = startDate + (long) ((double) i * (double) period - (double) mwindow / 2.0);
		long endFloat = startDate + (long) ((double) i * (double) period + (double) mwindow / 2.0);
				
		TimeWindow window = new TimeWindow(startFloat, endFloat);

		//logger.log(2, "Group " + group.getName() + " Monitor: Checking " + i + "th window: " + window);

		if ((startFloat <= te) && (endFloat >= ts)) {
		    // Check it wasnt done already inside the window
		    if (history.getLastExecution() < startFloat) {
			list.add(window);
			//logger.log(2, "Group " + group.getName() + " Monitor: Window OK");
		    }
		}
	    }

	} else if (timing instanceof XFixedTimingConstraint) {

	    // the only possible window is (fg-b, fg+b)

	    XFixedTimingConstraint xfix = (XFixedTimingConstraint)timing;

	    // Special case: Already done before ts
	    if (history.getLastExecution() > 0 && history.getLastExecution() < ts) {
		//logger.log(2, "Group " + group.getName() + " failed: FG_DONE");
		return null;
	    }

	    // FG can be done within specified minutes of the fixed time.
	    long fixed = xfix.getFixedTime();			
	    long fwindow = xfix.getSlack();
			
	    // check if outside fixed window			
	    if (((fixed - fwindow/2) > te) || ((fixed + fwindow/2) < ts)) 			
		return null;
	    list.add(new TimeWindow(fixed - fwindow/2, fixed + fwindow/2));

	} else if (timing instanceof XMinimumIntervalTimingConstraint) {

	    // depends on last exec from hist. Cannot predict future beyond one
	    // window
	    XMinimumIntervalTimingConstraint xmin = (XMinimumIntervalTimingConstraint)timing;
			
	    long startDate = xmin.getStartTime();
	    long endDate = xmin.getEndTime();
	    long minInterval = xmin.getMinimumInterval();
	    int maxRepeats = xmin.getMaximumRepeats();

	    // Special case: Not started monitoring.
	    if (startDate > te) 
		return null;
			
	    // Special case: Monitoring completed.
	    if (endDate < ts) 
		return null;
			
	    // Special case: Too soon after last exec
	    // if((ts - history.lastExecuted) < minInterval) {
	    // logger.log(2,
	    // "Group "+group.getName()+" failed: REPEATABLE_TOO_SOON_AFTER_LAST_EXEC");
	    // return null;
	    // }

	    // Special case: Repeat count exceeded // we really need to be able
	    // to do hist.getLastExecBefore(time)
	    // as last exec could be after this sim time ? or can we purge this
	    // in the histModel.load(p2model, uptoTime) ?
	    if (maxRepeats < history.getCountExecutions()) 				
		return null;
			
	    // lets restrict the end time to the earlier of
	    // group.end, group.exp - we may well run over the TE value but
	    // thats just fine
	    // long intend = Math.min(grpExpDate, endDate);
	    // list.add(new TimeWindow(history.lastExecuted + minInterval,
	    // intend));
	    int i1 = (int) Math.floor((double) (ts - history.getLastExecution()) / (double) minInterval);
	    int i2 = (int) Math.floor((double) (te - history.getLastExecution()) / (double) minInterval);

	    for (int i = i1; i < i2; i++) {

		TimeWindow window = new TimeWindow(history.getLastExecution() + i * minInterval, history.getLastExecution()
						   + (i + 1) * minInterval);
		list.add(window);
				
	    }

	} else if (timing instanceof XEphemerisTimingConstraint) {

	    // similar to mg but using phase - only if not already done by start
			
	    XEphemerisTimingConstraint xephem = (XEphemerisTimingConstraint)timing;

	    long startDate = xephem.getStart();
	    long endDate = xephem.getEnd();
	    long period = xephem.getCyclePeriod();
	    double phase = xephem.getPhase();
	    long window = xephem.getWindow();
		
	    // Special case: Already done before ts
	    if (history.getLastExecution() > 0 && history.getLastExecution() < ts) 			
		return null;
			
	    // Special case: Not started monitoring.
	    if (startDate > te) 
		return null;
			
	    // Special case: Monitoring completed.
	    if (endDate <= ts) 
		return null;
			
	    // first period before ts.
	    double fp = Math.floor((ts - startDate) / period);
	    int i = (int) fp;
			
	    // calculate the window
			
	    long startWindow = startDate + (long) ((i + phase)*period - window / 2.0);
	    long endWindow = startDate + (long) ((i + phase) * period + window / 2.0);
	    if (((startWindow <= te) && (endWindow >= ts)))
		list.add(new TimeWindow(startWindow, endWindow));

	} else {

	    // (fx.start, fx.end) but only if not already done by start.

	    XFlexibleTimingConstraint xflex = (XFlexibleTimingConstraint)timing;
			
	    long startFlex = xflex.getStartTime();
	    long endFlex = xflex.getEndTime();

	    // Special case: Already done before ts
	    if (history.getLastExecution() > 0 && history.getLastExecution() < ts) 
		return null;

	    if ((startFlex < te) && (endFlex > ts))
		list.add(new TimeWindow(startFlex, endFlex));
			
	}

	return list;

    }

    /**
     * Returns how much time is left for a group to execute in the specified
     * (pre-calculated) execution window starting from the specified time.
     * 
     * @param group
     *            The group we are interested in.
     * @param window
     *            The pre-calculated execution window to check.
     * @param t
     *            The time to start counting from.
     * @param env
     *            The group's environment.
     * @param hist
     *            The group's execution history (upto t ?).
     */
    public long calculateRemainingTime(GroupItem group, TimeWindow window, long time, EnvironmentSnapshot env,
				       ExecutionHistorySynopsis hist) {

	ITimingConstraint timing = group.getTimingConstraint();
		
	if (timing == null)
	    return 0L;
		
	long grpExpDate = timing.getEndTime();

	// Special case: Expired before start of search interval.
	if (grpExpDate <= time) 
	    return 0L;
	
	List<Disruptor> dl = new Vector<Disruptor>();
		
	long sum = 0L;
	long tt = Math.max(time, window.start);
	while (tt < window.end) {
	    if (xfm.isitFeasible(group, time, hist, null, env, dl).isFeasible())
		sum += dt;
	    tt += dt;
	}

	return sum;

    }

    /**
     * Returns how many nights the nominated group is executable on in the
     * current window.
     */
    public int countRemainingNights(GroupItem group, TimeWindow window, long t, EnvironmentSnapshot env,
				    ExecutionHistorySynopsis hist) {

	// first work out which day this time contains. Nights start at local noon.
		
	long sofn = calculatePreviousLocalNoon(t, site);

	// any days hereafter are relative to this

	// can we do the group on night 0 - this starts at t not 1200 ?
	int countExecutableNights = 0;
	if (executable(group, t, sofn + DAYMS, env, hist)) {
	    countExecutableNights++;
	}

	int nn = (int) ((double) (window.end - t) / (double) DAYMS) + 1;

	// for each following night...1200 to 1200
		
	for (int id = 1; id < nn; id++) {

	    long sd = sofn + (long) id * DAYMS;
	    long ed = sd + DAYMS;

	    if (executable(group, sd, ed, env, hist)) {
		countExecutableNights++;
	    }

	}

	//logger.method("countRemainingNights(G,w,t,e,h)").log(1,
	//"Group has " + countExecutableNights + " nights remaining in window");

	//if (countExecutableNights == 0) no executable nights in window
			
	return countExecutableNights;

    }

    /** Work out when the last local noon occurred. */
    private long calculatePreviousLocalNoon(long time, ISite site) {
	Calendar cal = Calendar.getInstance();
	cal.setTime(new Date(time));
	cal.set(Calendar.MINUTE, 0);
	cal.set(Calendar.SECOND, 0);
	long nts = cal.getTime().getTime();

	int hh = cal.get(Calendar.HOUR_OF_DAY);
	hh = hh + (int) Math.floor(Math.toDegrees(site.getLongitude()) / 15.0);

	if (hh < 0)
	    hh += 24;
	if (hh > 24)
	    hh -= 24;

	// compute last valid local noon of first active night
	long sofn = 0L;
	if (hh <= 12)
	    sofn = nts - 3600000L * (12 + hh);
	else
	    sofn = nts - 3600000L * (hh - 12);

	return sofn;
    }

    /** Is the group executable under env given hist. */
    private boolean executable(GroupItem group, long t1, long t2, EnvironmentSnapshot env, ExecutionHistorySynopsis hist) {

	// run over the night check for xm.canDo();

	int nsamp = (int) ((t2 - t1) / SAMP_INT);
	//logger.method("executable").log(3, "Testing with " + nsamp + " samples");

	long t = t1;
	while (t < t2) {
	    // fallout at first valid time if any
						
	    List<Disruptor> dl = new Vector<Disruptor>();
			
	    CandidateFeasibilitySummary cfs = (xfm.isitFeasible(group, t, hist, null, env, dl));
	    if (cfs.isFeasible())
		return true;
	    t += SAMP_INT;
	}

	return false;
    }

}
