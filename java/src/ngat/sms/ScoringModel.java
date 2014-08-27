/**
 * 
 */
package ngat.sms;


/**
 * @author eng
 *
 */
public interface ScoringModel {
	
	 /** Generate a score for a group under specified conditions.
	 * @param group The group to generate the score for.
	 * @param accounts The group's accounts.
	 * @param time When the score is for.
	 * @param env The conditions under which the group is to be scored.
	 * @param history The group's execution history upto time.
	 * @return The group's score.
	 * @throws ScoringException
	 */
	public ScoreMetricsSet scoreGroup(GroupItem group, AccountSynopsis accounts, long time, EnvironmentSnapshot env, ExecutionHistorySynopsis history) throws ScoringException;

	
	/** Generate an overall score from the supplied metrics.
	 * @param metrics The set of metrics.
	 * @return The overall/aggregate score based on the metrics.
	 * @throws ScoringException
	 */
	public double generateScore(ScoreMetricsSet metrics) throws ScoringException;
	
}
