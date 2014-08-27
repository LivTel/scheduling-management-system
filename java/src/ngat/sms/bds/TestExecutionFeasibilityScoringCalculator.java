/**
 * 
 */
package ngat.sms.bds;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.phase2.IObservingConstraint;
import ngat.phase2.ITimePeriod;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.CandidateFeasibilitySummary;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionFeasibilityModel;
import ngat.sms.ExecutionFeasibilityStatistics;
import ngat.sms.ExecutionFeasibilityStatisticsScoringCalculator;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.GroupItem;

/** Implementation of a ExecutionFeasibilityStatisticsScoringCalculator. This implementation is tied
 * to a scheduler which provides the following services:-
 * A mechanism for measuring the overall contention at a given time under specified conditions.
 * A mechanism for determining the score of a candidate group at a given time under specified conditions.
 * A mechanism for determining the selection decision between scored/ranked candidates.
 * @author eng
 *
 */
public class TestExecutionFeasibilityScoringCalculator extends UnicastRemoteObject implements
		ExecutionFeasibilityStatisticsScoringCalculator {

	/** Provides information about group feasibility. */
	private ExecutionFeasibilityModel xfm;

	/** Provides accounting information. */
	private AccountSynopsisModel asm;

	/** Largest number of samples we are allowed to handle. */
	private int maxSamples;

	/**
	 * Create a TestExecutionFeasibilityScoringCalculator.
	 * 
	 * @param xfm
	 *            A feasibility model to test groups with.
	 * @param asm
	 *            An account synopsis model to locate group's associated
	 *            accounts.
	 */
	public TestExecutionFeasibilityScoringCalculator(ExecutionFeasibilityModel xfm, AccountSynopsisModel asm)
			throws RemoteException {
		super();
		this.xfm = xfm;
		this.asm = asm;
	}

	/**
	 * Callers may supply a GroupItem for which they wish to determine the feasibility 
	 * (in terms of likelihood of being scheduled) under the known environmental 
	 * and weather conditions and subject to the anticipated evolution of the phase2 model.
	 * @param group The group we wish to obtain statistics for.
	 * @param period The period over which we want to determine feasibility.
	 * * @param resolution The resolution/granularity required.
	 * @return The feasibility statistics for the execution of the specified group.
	 * @throws RemoteException If anything goes awry.
	 * @seengat.sms.ExecutionFeasibilityStatisticsScoringCalculator#
	 * getFeasibilityStatistics(ngat.sms.GroupItem)
	 */
	public ExecutionFeasibilityStatistics getFeasibilityStatistics(GroupItem group, ITimePeriod period, long resolution) throws RemoteException {

		// for calls on Feasibility model, we need:-
		// group - got it
		// time - another param or derive from timing constraints?
		// history - should be empty - the group does not exist as yet
		// accounts - for the group's proposal - obtain via AccSynModel
		// env - this should be the current or predicted env or maybe we call 3
		// times with different env assumptions
		// and then convolve results ?

		// may also need an interval/granularity for the diff and cumulative
		// result vectors - not so fine as to
		// load up the system or take too long to calculate for the client...

		// TODO NOW - this may not be what we want - may want tstart whatever
		// that is...
		long now = System.currentTimeMillis();

		// generate a list of disruptors.
		List<Disruptor> disruptors = new Vector<Disruptor>();
		
		
		// work out from group's timing-constraint what overall period to look
		// at
		ITimingConstraint timing = group.getTimingConstraint();

		long start = timing.getStartTime();
		long end = timing.getEndTime();

		// terminal case - after end time
		if (now > end)
			throw new RemoteException("Scoring feasibility for group: " + group.getID()
					+ ": Timing constraint has already expired");

		// work out how many windows are available starting now - including any
		// already started

		int nw = 0;
		try {
			nw = countWindows(timing, now);
		} catch (Exception e) {
			throw new RemoteException("Scoring feasibility for group: " + group.getID(), e);
		}

		// Too many windows - ie more windows than allowed no of samples.
		if (nw > maxSamples)
			throw new RemoteException("Scoring feasibility for group: " + group.getID()
					+ ": Too many windows to process: " + nw);

		// what resolution (number of samples within each window) are we going
		// for ?
		int nsw = (int) ((double) maxSamples / (double) nw);

		// what is the number of the first window to access ?
		int iwin = 0;
		long windowSize = 0L;
		if (timing instanceof XFlexibleTimingConstraint) {
			iwin = 0;
			windowSize = timing.getEndTime() - timing.getStartTime();
		} else if (timing instanceof XMonitorTimingConstraint) {
			XMonitorTimingConstraint xmon = (XMonitorTimingConstraint) timing;
			iwin = (int) ((now - xmon.getStartDate()) / xmon.getPeriod());
			windowSize = xmon.getWindow();
		}

		// how long is the actual in-window sample size ?
		long sampleSize = windowSize / nsw;

		AccountSynopsis accounts = asm.getAccountSynopsis(group.getProposal().getID(), now);

		// now we are going to try and access each feasible window
		for (int iw = 0; iw < nw; iw++) {

			// we are in window iw relative to first window with absolute number
			// (iwin)
			int iwindow = iwin + iw;

			// what is start and end of this window ?
			long winStart = 0l;
			long winEnd = 0L;

			if (timing instanceof XFlexibleTimingConstraint) {
				winStart = Math.max(now, timing.getStartTime());
				winEnd = timing.getEndTime();
			} else if (timing instanceof XMonitorTimingConstraint) {
				XMonitorTimingConstraint xmon = (XMonitorTimingConstraint) timing;
				winStart = xmon.getStartDate() + iwindow * xmon.getPeriod() - xmon.getWindow() / 2;
				winEnd = xmon.getStartDate() + iwindow * xmon.getPeriod() + xmon.getWindow() / 2;
				winStart = Math.max(now, winStart);
			}

			// we have the start and end of the current window so we can work
			// out the sample times.
			// there may be less than the standard nsw in first window (and
			// potentially last one?)
			int answ = (int) ((winEnd - winStart) * nsw / windowSize);

			for (int j = 0; j < answ; j++) {

				long sampleTime = winStart + j * sampleSize;

				CandidateFeasibilitySummary cfs = null;
				
				EnvironmentSnapshot ePoor = new EnvironmentSnapshot(now, 3.0,
						IObservingConstraint.NON_PHOTOMETRIC);
				cfs = xfm.isitFeasible(group, sampleTime, null, accounts, ePoor, disruptors);
				boolean doPoor = cfs.isFeasible();

				EnvironmentSnapshot eAver = new EnvironmentSnapshot(now, 1.3,
						IObservingConstraint.NON_PHOTOMETRIC);
				cfs = xfm.isitFeasible(group, sampleTime, null, accounts, eAver, disruptors);
				boolean doAver = cfs.isFeasible();

				EnvironmentSnapshot eGood = new EnvironmentSnapshot(now, 0.8,
						IObservingConstraint.NON_PHOTOMETRIC);
				cfs = xfm.isitFeasible(group, sampleTime, null, accounts, eGood, disruptors);
				boolean doGood = cfs.isFeasible();

				// TODO TODO somehow we need to get contention statistics at this point !!!!
				// there may be a CandidateExtractor in the scheduler if we can access this
				// there may be a MetricGenerator / ScoringModel in the scheduler if we can access this
				
				// if we are using a scheduler without direct access to such items above ????
				// e.g. a lookahead scheduler- we would only allow a test group to jump into the queue
				// if it improved the already generated sequence and didn't cause upsets ...
				// - even then it might only get some of its requested slots so still need diff scoring
				// alternatively we might need to try various combinations or run new lookaheads to see
				// whatif I slot this one in at various points ? 
				
				
				
			}

		}
		// this is not quite correct as we cant actual multiply cumulative and
		// diff vectors like this but we can with overall score
		// return doGood*pGood + doAver*pAver + doPoor*pPoor;

		return null;
	}

	/**
	 * Counts the number of available windows for the specified timing
	 * constraint from given start time.
	 * 
	 * @param timing
	 *            The constraint to check.
	 * @param start
	 *            When to start counting.
	 * @return The number of windows available from start to timing constraint
	 *         end.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	private int countWindows(ITimingConstraint timing, long start) throws Exception {

		if (timing.getEndTime() < start)
			return 0;

		if (timing instanceof XFlexibleTimingConstraint) {
			return 1;
		} else if (timing instanceof XMonitorTimingConstraint) {
			XMonitorTimingConstraint xmon = (XMonitorTimingConstraint) timing;
			return (int) ((xmon.getEndDate() - start) / xmon.getPeriod());
		} else if (timing instanceof XFixedTimingConstraint) {
			return 1;
		} else
			throw new Exception("Unsupported timing constraint class: " + timing.getClass().getName());
	}

}
