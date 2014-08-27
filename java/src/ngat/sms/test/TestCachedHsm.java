package ngat.sms.test;

import ngat.sms.*;
import ngat.phase2.*;
import ngat.oss.model.*;

import java.rmi.*;
import java.util.*;

public class TestCachedHsm {

	public static void main(String[] args) {
		// ===============================================
		// WARNING this code is broken due to smp changes.
		// ===============================================
		try {

			IHistoryModel history = (IHistoryModel) Naming.lookup("rmi://localhost/SynopticModelProvider");
			System.err.println("Obtained remote SynopticModel");

			DefaultBaseModelProvider bmp = new DefaultBaseModelProvider("localhost");

			BasicHistorySynopsisModel bsm = new BasicHistorySynopsisModel(bmp);
			System.err.println("Created Synoptic History Model");

			Phase2GroupModelProvider p2g = (Phase2GroupModelProvider) Naming
					.lookup("rmi://localhost/Phase2GroupModelProvider");
			System.err.println("Located Phase2 Group Model Provider: " + p2g);

			System.err.println("Ready to call: getPhase2Model() on P2G...");
			Phase2CompositeModel gphase2 = (Phase2CompositeModel) p2g.getPhase2Model();
			System.err.println("Grabbed Phase2 Group Model: " + gphase2);

			long now = System.currentTimeMillis();
			System.err.println("Loading history...");
			bsm.loadHistory(gphase2, now);

			CachedHistorySynopsisModel chsm = new CachedHistorySynopsisModel(bsm);
			System.err.println("Created cached history model");

			long gid = Integer.parseInt(args[0]);

			System.err.println("Starting tests...");
			ExecutionHistorySynopsis hist = chsm.getExecutionHistorySynopsis(gid, now);
			System.err.println("Initial history synopsis: " + hist);

			Set<IQosMetric> qos = new HashSet<IQosMetric>();
			// update 30,45,60,85 minutes
			System.err.println("Adding entries at 30,45,60,85 minutes.");
			chsm.updateHistory(gid, 0, IHistoryItem.EXECUTION_SUCCESSFUL, now + 30 * 60 * 1000L, null, qos);
			chsm.updateHistory(gid, 0, IHistoryItem.EXECUTION_SUCCESSFUL, now + 45 * 60 * 1000L, null, qos);
			chsm.updateHistory(gid, 0, IHistoryItem.EXECUTION_SUCCESSFUL, now + 60 * 60 * 1000L, null, qos);
			chsm.updateHistory(gid, 0, IHistoryItem.EXECUTION_SUCCESSFUL, now + 85 * 60 * 1000L, null, qos);

			hist = chsm.getExecutionHistorySynopsis(gid, now);
			System.err.println("Faked history synopsis now: " + hist);

			// now clear them out
			System.err.println("Clearing the cache...");
			chsm.clearCache();

			hist = chsm.getExecutionHistorySynopsis(gid, now);
			System.err.println("Faked history synopsis now: " + hist);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
