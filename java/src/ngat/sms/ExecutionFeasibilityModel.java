package ngat.sms;

import java.util.List;

/**
 * @author eng
 *
 */
public interface ExecutionFeasibilityModel {

	
	/**
	 * Determines whether a group can be executed subject to the supplied
	 * conditions and with the given history.
	 * 
	 * @param group
	 *            The group item to consider for execution.
	 * @param time
	 *            The time for which we want the determination.
	 * @param history
	 *            The group's execution history.
	 * @param accounts
	 *            The group's set of accounts.
	 * @param env
	 *            Environmental conditions/prediction at time.
	 * @return True if the group is feasible under supplied conditions.
	 */
	public CandidateFeasibilitySummary isitFeasible(GroupItem group, long time, ExecutionHistorySynopsis history, AccountSynopsis accounts,
			EnvironmentSnapshot env, List<Disruptor> disruptors) ;
	
//	/**
//	 * Determines whether a group can be executed subject to the supplied
//	 * conditions and with the given history at any time in the specified window (t1, t2).
//	 * 
//	 * @param group
//	 *            The group item to consider for execution.
//	 * @param t1
//	 *            The start of the window for which we want the determination.
//	 * @param t2
//	 *            The end of the window for which we want the determination.
//	 * @param history
//	 *            The group's execution history.
//	 * @param accounts
//	 *            The group's set of accounts.
//	 * @param env
//	 *            Environmental conditions/prediction at time.
//	 * @return True if the group is feasible under supplied conditions.
//	 */
//	public boolean isFeasible(GroupItem group, long t1, long t2, ExecutionHistorySynopsis history,
//			AccountSynopsis accounts, EnvironmentSnapshot env);

	
//	/** Calculates the remaining (feasible) time for the specified group in the specified window under the supplied conditions.
//	 * @param group The group item to consider.
//	 * @param t1 The start of the window for which we want the determination.
//	 * @param t2 The end of the window for which we want the determination.
//	 * @param env Environmental conditions/prediction at time.
//	 * @return Remaining feasible time in window (may be zero).
//	 *//*
//	public long calculateRemainingTime(GroupItem group, long t1, long t2, ExecutionHistorySynopsis history, EnvironmentSnapshot env);
//	
//	*//** Calculates the number of nights on which the group is feasible in the specified window.
//	 * @param group The group item to consider.
//	 * @param t1 The start of the window for which we want the determination.
//	 * @param t2 The end of the window for which we want the determination. 
//	 * @param env Environmental conditions/prediction at time.
//	 * @return The number of nights on which the group is feasible (may be zero).
//	 *//*
//	public int countRemainingNights(GroupItem group, long t1, long t2, ExecutionHistorySynopsis history, EnvironmentSnapshot env);
//	*/
}