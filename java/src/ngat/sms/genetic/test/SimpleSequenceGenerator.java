package ngat.sms.genetic.test;

import ngat.sms.*;
import ngat.sms.genetic.*;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.util.FeasibilityPrescan;
import ngat.sms.util.PrescanEntry;
import ngat.sms.bds.*;

import ngat.astrometry.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.phase2.*;

import java.util.*;
import java.text.*;

public class SimpleSequenceGenerator {

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	static long start;
	static long end;

	static ISite site;

	public static void main(String args[]) {

		try {

			sdf.setTimeZone(UTC);

			Logger alogger = LogManager.getLogger("GSS");
			alogger.setLogLevel(3);
			ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
			console.setLogLevel(3);
			alogger.addExtendedHandler(console);

			LogGenerator logger = new LogGenerator(alogger);
			logger.system("SMS").subSystem("GSS").srcCompClass("SeqGen");

			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

			// Is it day or night at this time at this location ?

			long time = System.currentTimeMillis();

			double lat = Math.toRadians(cfg.getDoubleValue("lat"));
			double lon = Math.toRadians(cfg.getDoubleValue("long"));
			site = new BasicSite("test-site", lat, lon);

			BasicSynopticModelProxy smp = new BasicSynopticModelProxy("localhost");
			smp.asynchLoadSynopticModel();
			ChargeAccountingModel chargeModel = new StandardChargeAccountingModel();
			TestResourceUsageEstimator tru = new TestResourceUsageEstimator();
			BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();
			scope.setDomeLimit(Math.toRadians(25.0));
			scope.setAutoguiderStatus(true);

			// TODO NO ISM !
			FeasibilityPrescan fp = new FeasibilityPrescan(site, smp, tru, chargeModel, scope, null);
			List candidates = fp.prescan(time, 60*1000L);

			double exectot = 0.0;
			Iterator ig = candidates.iterator();
			while (ig.hasNext()) {
				PrescanEntry pse = (PrescanEntry) ig.next();
				exectot += pse.execTime;
			}
			System.err.println("Total available exectime: " + (exectot / 3600000.0) + " H");

			System.err.println("Ready to generate sequences...");

			AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);

			SolarCalculator sun = new SolarCalculator();
			Coordinates sunTrack = sun.getCoordinates(time);
			double sunlev = astro.getAltitude(sunTrack, time);

			logger.create().extractCallInfo().level(2).msg("Sun elevation is: " + Math.toDegrees(sunlev)).send();
			boolean daytime = (sunlev > 0.0);

			if (daytime) {
				logger.create().extractCallInfo().level(2).msg("It is currently daytime").send();
				start = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
				end = start + astro.getTimeUntilNextRise(sunTrack, 0.0, start);
			} else {
				logger.create().extractCallInfo().level(2).msg("It is currently nighttime").send();
				start = time;
				end = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
			}
			logger.create().extractCallInfo().level(2).msg(
					"Start seqeunces at: " + sdf.format(new Date(start)) + ", End at: " + sdf.format(new Date(end))
							+ " Night duration: " + ((end - start) / 3600000) + "H").send();

			double hiScore = -999.99;
			double loScore = 999.99;
			String loSeq = null;
			String hiSeq = null;

			// loop thro sequences
			for (int i = 0; i < 1000; i++) {
				// logger.create().extractCallInfo().level(2).
				// msg("Start sequence: "+i).send();

				StringBuffer seq = new StringBuffer(); // store sequence
				double seqscore = 0.0;
				List used = new Vector(); // used groups
				int empty = 0; // count size of empty slots
				long t = start;
				while (t < end) {

					// pick a random valid candidate
					boolean validCandidate = false;
					PrescanEntry pse = null;

					List slotCandidates = new Vector();
					for (int j = 0; j < candidates.size(); j++) {
						PrescanEntry test = (PrescanEntry) candidates.get(j);
						if (test.isFeasible(t) && (!used.contains(test)))
							slotCandidates.add(test);
					}

					if (slotCandidates.isEmpty()) {
						// increment the empty slot count
						empty++;
						// first empty slot append an X
						if (empty == 1) {
							if (t != start)
								seq.append("-");
							seq.append("X");
						}
						t += 60000L; // jump ahead 1 minute
					} else {
						// if we were empty, append the empty count
						if (empty != 0)
							seq.append("(" + empty + ")");
						// reset empty count
						empty = 0;
						int nn = (int) (Math.random() * 5885191.83648348571) % slotCandidates.size();
						pse = (PrescanEntry) slotCandidates.get(nn);
						used.add(pse);
						if (t != start)
							seq.append("-");
						seq.append(pse.gname);

						seqscore += scoreGroup(pse.group, t);

						t += (long) pse.execTime;
					}

				} // next time slot

				// logger.create().extractCallInfo().level(2)
				// .msg("Seq: "+seq.toString()).send();

				System.err.printf("%6d %4.2f : %s \n", i, seqscore, seq.toString());

				if (seqscore > hiScore) {
					hiScore = seqscore;
					hiSeq = seq.toString();
				}

				if (seqscore < loScore) {
					loScore = seqscore;
					loSeq = seq.toString();
				}

			} // next sequence

			System.err.printf("High scoring sequence: %4.2f %s ", hiScore, hiSeq);
			System.err.printf("Low scoring sequence:  %4.2f %s ", loScore, loSeq);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static double scoreGroup(GroupItem group, long time) throws Exception {

		// first compute priority score
		double pscore = 0.0;
		IProposal proposal = group.getProposal();

		switch (proposal.getPriority()) {
		case IProposal.PRIORITY_A:
			pscore += 4.0;
			if (group.isUrgent())
				pscore += 2.0;
			break;
		case IProposal.PRIORITY_B:
			pscore += 2.0;
			if (group.isUrgent())
				pscore += 2.0;
			break;
		case IProposal.PRIORITY_C:
			pscore += 0.0;
			break;
		default:
			pscore -= 100.0;
		}

		pscore += proposal.getPriorityOffset();

		ITimingConstraint timing = group.getTimingConstraint();
		if (timing instanceof XMonitorTimingConstraint || timing instanceof XMinimumIntervalTimingConstraint) {
			pscore += 1.0;
		}

		pscore /= 7.0;

		// elevation of target re max in night
		double escore = 0.0;
		try {
			escore = calculateElevationScore(group, time);
		} catch (Exception e) {
			e.printStackTrace();
			// escore will be zero now
		}

		double random = Math.random();

		double score = 0.75 * escore + 0.25 * pscore;

		// String path = (group.getTag() != null ? group.getTag().getName() :
		// "UNK_TAG") + "/"
		// + (group.getUser() != null ? group.getUser().getName() : "UNK_USR") +
		// "/"
		// + (group.getProposal() != null ? group.getProposal().getName() :
		// "UNK_PRP");

		// String result =
		// String.format("Scoring group: [%35.35s..] [%20.20s..] %2.2f %2.2f %2.2f %2.2f-> %2.4f ",
		// path,
		// group.getName(), escore, pscore, smscore, random, score);

		// ... Scoring group: [JMU/Bloggs.Fred/JL09B007..] [RS_oph_big_bonus..]
		// 0.3 0.25 0.54 0.02 -> 1.2453

		// logger.create().block("nextScheduledJob").info().level(3).msg(result).send();

		return score;

	}

	private static double calculateElevationScore(GroupItem group, long time) throws Exception {

		ISequenceComponent seq = group.getSequence();
		ComponentSet cs = new ComponentSet(seq);

		// no targets to check
		if (cs.countTargets() == 0)
			return 0.0;

		AstrometryCalculator astro = new BasicAstrometryCalculator();

		double escore = 0.0;

		// loop over targets, calculate averge value of score
		Iterator<ITarget> targets = cs.listTargets();
		while (targets.hasNext()) {

			ITarget target = targets.next();
			TargetTrackCalculator track = new BasicTargetCalculator(target, site);

			// coordinates and elevation at time
			Coordinates c = track.getCoordinates(time);
			double elev = astro.getAltitude(c, site, time);

			// highest elevation in night ahead until sunrise@-2 OR do we want
			// highest since sunset also ?
			double maxelev = astro.getMaximumAltitude(track, site, start, end);

			double tscore = elev / maxelev;
			if (Double.isNaN(tscore) || Double.isInfinite(tscore)) {
				// System.err.println("Elevation score for: " + group.getName()
				// + "/" + target.getName() + " " + tscore);
				tscore = 0.0;
			}

			escore += tscore;

		}

		return escore / cs.countTargets();

	}

}