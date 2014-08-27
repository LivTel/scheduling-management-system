/**
 * 
 */
package ngat.sms.test;

import ngat.sms.*;
import ngat.sms.bds.*;
import ngat.sms.models.standard.*;
import ngat.sms.genetic.test.*;
import ngat.icm.InstrumentRegistry;
import ngat.phase2.*;
import ngat.astrometry.*;
import ngat.util.*;

import java.io.*;
import java.rmi.Naming;
import java.text.*;
import java.util.*;

/**
 * @author eng
 * 
 */
public class ExtractVolatilityLogs {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {

			ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

			File base = new File(config.getProperty("base"));
			BufferedReader bin = new BufferedReader(new FileReader(base));

			System.err.println("Extracting volatility info from: " + base);

			// Telescope location
			double lat = Math.toRadians(config.getDoubleValue("lat"));
			double lon = Math.toRadians(config.getDoubleValue("long"));

			BasicSite site = new BasicSite("Obs", lat, lon);

			ChargeAccountingModel cam = new StandardChargeAccountingModel();

			TestResourceUsageEstimator tru = new TestResourceUsageEstimator();

			// pre-scan with optimal info
			BasicTelescopeSystemsSynopsis optscope = new BasicTelescopeSystemsSynopsis();
			optscope.setDomeLimit(Math.toRadians(25.0));
			optscope.setAutoguiderStatus(true);

			String iregHost = config.getProperty("ireg-host", "localhost");
			String iregUrl = "rmi://" + iregHost + "/InstrumentRegistry";
			//BasicInstrumentSynopsisModel bism = new BasicInstrumentSynopsisModel(iregUrl);
			//bism.asynchLoadFromRegistry();
			InstrumentRegistry ireg = (InstrumentRegistry)Naming.lookup(iregUrl);
			OptimisticLiveInstrumentSynopsisModel optism = new OptimisticLiveInstrumentSynopsisModel();
			optism.loadFromRegistry(ireg);

			// TODO is there another one of these thats better ???
			//ngat.sms.genetic.test.OptimisticInstrumentSynopsisModel optism = new ngat.sms.genetic.test.OptimisticInstrumentSynopsisModel(
				//	bism);

			AstrometryCalculator astro = new BasicAstrometryCalculator();

			TestResourceUsageEstimator xrm = new TestResourceUsageEstimator();

			// automatically fix rotator configs which have no instrument focal plane set.
			ComponentSet.autofix = true;

			StandardExecutionFeasibilityModel xfm = new StandardExecutionFeasibilityModel(astro, xrm, cam, site,
					optscope, optism);

			// scan the log file...
			String line = null;
			while ((line = bin.readLine()) != null) {

				System.err.println("Processing Line: " + line);

				StringTokenizer st = new StringTokenizer(line);
				String strDate = st.nextToken() + " " + st.nextToken();
				String strop = st.nextToken();

				if (strop.equals("ADD_GROUP")) {
					String file1 = st.nextToken();
					Date date = sdf.parse(strDate);

					System.err.println("Ignoring ADD_GROUP");
					// GroupItem g = (GroupItem) readGroup(file1);
					// System.err.println("-----------------------------------------------------------------------");
					// System.err.println("Add group >> " + g.getName() + " " +
					// g.getTimingConstraint());
					// System.err.println("-----------------------------------------------------------------------\n\n\n");

				} else if (strop.equals("UPDATE_GROUP")) {
					String file1 = st.nextToken();
					String file2 = st.nextToken();

					Date date = sdf.parse(strDate);

					System.err.println("Process UPDATE_GROUP");

					try {
						GroupItem g1 = (GroupItem) readGroup(file1);
						GroupItem g2 = (GroupItem) readGroup(file2);

						System.err.println("-----------------------------------------------------------------------");
						System.err.println("Old group >> " + g1.getName() + " " + g1.getTimingConstraint());
						System.err.println("New group >> " + g2.getName() + " " + g2.getTimingConstraint());

						System.err
								.println("-----------------------------------------------------------------------\n\n\n");

					} catch (Exception e) {
						System.err.println("Processing error: " + e.getMessage());
					}

				} else if (strop.equals("UPDATE_SEQ")) {

					String file1 = st.nextToken();
					String file2 = st.nextToken();

					Date date = sdf.parse(strDate);
					long time = date.getTime();

					System.err.println("Process UPDATE_SEQ");

					try {

						// when is sunrise
						SolarCalculator sun = new SolarCalculator();
						Coordinates c = sun.getCoordinates(time);
						double ha = astro.getHourAngle(c, site, time);
						long start = time;
						if (astro.isRisen(c, site, Math.toRadians(0.0), time)) {
							long ttset = astro.getTimeUntilNextSet(c, site, Math.toRadians(-2.0), time);
							start = time + ttset;
						}

						long tts = astro.getTimeUntilNextRise(c, site, 0.0, start);
						long sunrise = start + tts;

						GroupItem g1 = (GroupItem) readGroup(file1);
						GroupItem g2 = (GroupItem) readGroup(file2);

						// workout exec timing
						ISequenceComponent s1 = g1.getSequence();
						double t1 = 0L;
						if (s1 != null)
							t1 = xrm.getExecTime(s1);

						ISequenceComponent s2 = g2.getSequence();
						double t2 = 0L;
						if (s2 != null)
							t2 = xrm.getExecTime(s2);

						System.err.println("-----------------------------------------------------------------------");
						System.err.println("Old group >> " + g1.getName() + " X=" + (t1 / 60000.0) + "m");
						System.err.println("New group >> " + g2.getName() + " X=" + (t2 / 60000.0) + "m");

						analyse(xrm, xfm, g1, g2, time, start, sunrise);

						System.err
								.println("-----------------------------------------------------------------------\n\n\n");

					} catch (Exception e) {
						System.err.println("Processing error: " + e.getMessage());
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Object readGroup(String file) throws Exception {

		ObjectInputStream oin = new ObjectInputStream(new FileInputStream(file));
		Object obj = oin.readObject();
		oin.close();
		return obj;

	}

	static void analyse(TestResourceUsageEstimator xrm, StandardExecutionFeasibilityModel xfm, GroupItem before,
			GroupItem after, long time, long start, long end) throws Exception {

		// start is the event time, end is probably sunrise
		System.err.println("Run analyser...");

		long interval = 60000L; // may become input param

		// setup fake account synopsis
		/*Map<ISemester, IAccount> amap = new HashMap<ISemester, IAccount>();
		AccountSynopsis accounts = new AccountSynopsis(amap);
		XAccount acc = new XAccount();
		acc.setName("allocation");
		acc.setAllocated(1000.0);
		acc.setConsumed(0.0);
		accounts.addAccountBySemester(new XSemester(start - 30*24*2600*1000L, end + 30*24*2600*1000L), acc);
*/
		// TODO setup fake history synopsis
		ExecutionHistorySynopsis hist = new ExecutionHistorySynopsis();
		hist.setLastExecution(0L);
		hist.setCountExecutions(0);

		// TODO setup fake env snapshot
		EnvironmentSnapshot env = new EnvironmentSnapshot(start, 0.5, EnvironmentSnapshot.EXTINCTION_PHOTOM);

		// public boolean isFeasible(GroupItem group, long time,
		// ExecutionHistorySynopsis exec, AccountSynopsis accounts,
		// EnvironmentSnapshot env, List<Disruptor> disruptors) {

		List<Disruptor> nodisruptors = new Vector<Disruptor>();

		int nab = (int) ((double) (end - start) / (double) interval) + 1;

		double ctb = 0.0; // count reach
		double[] fb = new double[nab];

		long t = start;
		while (t < end) {
		/*	int ib = (int) ((double) (t - start) / (double) interval) + 1;
			CandidateFeasibilitySummary cfs = xfm.isitFeasible(before, t, hist, accounts, env, nodisruptors);
			if (cfs.isFeasible()) {
				ctb += interval;
				fb[ib] = 1.0; // record the feasibility at t
			}
			t += interval;*/
		}

		// we have a vector showing feasibility at t (s,e) and the number of
		// entries (bef-reach)
		double dmdb = 0.0;
		ISequenceComponent sqb = before.getSequence();
		double xb = 0.0;
		if (sqb != null) {
			xb = xrm.getExecTime(sqb);
			dmdb = xb / (ctb + xb);
		}

		// same for after to get its feas windows and feas time
		double cta = 0.0;
		double[] fa = new double[nab];

		t = start;
		while (t < end) {
			/*int ia = (int) ((double) (t - start) / (double) interval) + 1;
			CandidateFeasibilitySummary cfs = xfm.isitFeasible(after, t, hist, accounts, env, nodisruptors);
			if (cfs.isFeasible()) {
				cta += interval;
				fa[ia] = 1.0; // record the feasibility at t
			}
			t += 60000L;*/
		}

		double dmda = 0.0;
		ISequenceComponent sqa = after.getSequence();
		double xa = 0.0;
		if (sqa != null) {
			xa = xrm.getExecTime(sqa);
			dmda = xa / (cta + xa);
		}

		// scan feas vectors count all segments where one or other or both are
		// feasible, add 1 to reach
		int nt = 0;
		int nd = 0;
		int nc = 0;
		int pp = -1;
		for (int i = 0; i < nab; i++) {
			if (fb[i] > 0.0 || fa[i] > 0.0) {
				if (pp == -1)
					pp = i; // proximity
				// rho T (total effected)
				nt++; // record combined reach
			}
			// rho C (changed)
			if (fb[i] != fa[i])
				nc++;
			// rho D (difference)
			if (fb[i] == 0 && fa[i] == 1)
				nd += 1;
			if (fb[i] == 1 && fa[i] == 0)
				nd -= 1;
		}

	
		System.err.printf("S_bef: %4d, S_aft %4d, D_b: %4.2f, D_a: %4.2f, Proximity: %4d, pT: %4d, pD: %4d, pC: %4d\n",
				(int) ctb, (int) cta, dmdb, dmda, pp, nt, nd, nc);

		// DATA 2010-09-11 19:22:22 
		if (nt != 0)
			System.err.printf("DATA %tF %tT %4d %4d %4.2f %4.2f %4.2f %4.2f %4d %4d %4d %4d \n", time, time, (int)ctb, (int)cta, dmdb, dmda, xb, xa, pp, nt, nd, nc);
		
		StringBuffer abuff = new StringBuffer("A:");
		StringBuffer bbuff = new StringBuffer("B:");

		int n10 = nab / 10 + 1;
		for (int j = 0; j < n10; j++) {
			int ns = j * 10;
			int ne = Math.min(ns + 10, nab);
			int nak = 0;
			int nbk = 0;
			for (int k = ns; k < ne; k++) {
				if (fa[k] == 1)
					nak++;
				if (fb[k] == 1)
					nbk++;
			}
			String sa = (nak == 0 ? "-" : "" + (nak - 1));
			String sb = (nbk == 0 ? "-" : "" + (nbk - 1));

			abuff.append(sa);
			bbuff.append(sb);

		}
		System.err.println(abuff.toString());
		System.err.println(bbuff.toString());

	}

}