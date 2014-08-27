/**
 * 
 */
package ngat.sms.bds;

import ngat.sms.AccountSynopsis;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.GroupItem;
import ngat.sms.ScoreMetricsSet;
import ngat.sms.ScoringException;
import ngat.sms.ScoringModel;

/**
 * @author eng
 *
 */
public class TestScoringModel implements ScoringModel {

	/* (non-Javadoc)
	 * @see ngat.sms.ScoringModel#scoreGroup(ngat.sms.GroupItem, ngat.sms.AccountSynopsis, long, ngat.sms.EnvironmentSnapshot, ngat.sms.ExecutionHistorySynopsis)
	 */
	public ScoreMetricsSet scoreGroup(GroupItem group, AccountSynopsis accounts, long time, EnvironmentSnapshot env,
			ExecutionHistorySynopsis history) throws ScoringException {
		
		ScoreMetricsSet sms = new ScoreMetricsSet();
	
		// TODO need generators for each metric
		
		// OH Metric - target elevation as fraction of best elevation (in window, rest of night, visible night?).
		
		// Priority - normalized to all active groups in group's proposal.
		
		// Condition matching - matches actual seeing to requested (via obs constraints).
		
		// Project completion - boosts groups where proposal is nearing completion towards MUF.
		
				
		return sms;
	}

	
	public double generateScore(ScoreMetricsSet metrics) throws ScoringException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	

}
