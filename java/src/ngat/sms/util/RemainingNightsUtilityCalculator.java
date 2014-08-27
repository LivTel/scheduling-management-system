package ngat.sms.util;

import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.GroupItem;
import ngat.util.TimeWindow;

public class RemainingNightsUtilityCalculator implements UtilityCalculator {

	private TimingConstraintWindowCalculator tcwc;

	public RemainingNightsUtilityCalculator(TimingConstraintWindowCalculator tcwc) {
		this.tcwc = tcwc;
	}

	public double getUtility(GroupItem group, long time, EnvironmentSnapshot env, ExecutionHistorySynopsis hist) {

		TimeWindow window = tcwc.getWindow(group, hist, time);

		int rn = tcwc.countRemainingNights(group, window, time, env, hist);

		// System.err.println("RTUC: Group: "+group.getName()+" RN="+rn);
		return 1.0 / (double) rn;

	}

}
