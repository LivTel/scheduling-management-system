package ngat.sms.util;

import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.GroupItem;

/** Interface specification for utility calculators.*/
public interface UtilityCalculator {

    /** Return the utility for the specified group at time under env.*/
    public double getUtility(GroupItem group, long time, EnvironmentSnapshot env, ExecutionHistorySynopsis hist);

}
	
