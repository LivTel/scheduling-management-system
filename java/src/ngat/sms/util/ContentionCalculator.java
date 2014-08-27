/**
 * 
 */
package ngat.sms.util;

import java.io.File;
import java.io.PrintStream;
import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.Vector;

import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.Coordinates;
import ngat.astrometry.SolarCalculator;
import ngat.phase2.IGroup;
import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.BasicSynopticModelProxy;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.CachedAccountSynopsisModel;
import ngat.sms.CachedHistorySynopsisModel;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.GroupItem;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.SynopticModelProvider;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.genetic.test.OptimisticInstrumentSynopsisModel;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class ContentionCalculator {

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static SimpleDateFormat udf = new SimpleDateFormat("yyyyMMddHHmm");

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
		sdf.setTimeZone(UTC);
		udf.setTimeZone(UTC);

		Calendar.getInstance().setTimeZone(UTC);
		
		/*
		 * Logger alogger = LogManager.getLogger("SMS"); alogger.setLogLevel(3);
		 * ConsoleLogHandler console = new ConsoleLogHandler(new
		 * BogstanLogFormatter()); console.setLogLevel(3);
		 * alogger.addExtendedHandler(console);
		 */

		try {

			ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

			// Telescope location
			double lat = Math.toRadians(config.getDoubleValue("lat"));
			double lon = Math.toRadians(config.getDoubleValue("long"));

			BasicSite site = new BasicSite("Obs", lat, lon);

			// where is the SMP ?
			String smpHost = config.getProperty("smp-host", "localhost");
			SynopticModelProvider smp = (SynopticModelProvider) Naming.lookup("rmi://" + smpHost
					+ "/SynopticModelProvider");

			// Check all groups from cache model.
			Phase2CompositeModel p2g = smp.getPhase2CompositeModel();

			AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
			CachedAccountSynopsisModel casm = new CachedAccountSynopsisModel(pasm);

			ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();
			CachedHistorySynopsisModel chsm = new CachedHistorySynopsisModel(hsm);

			ChargeAccountingModel cam = new StandardChargeAccountingModel();

			TestResourceUsageEstimator tru = new TestResourceUsageEstimator();

			// pre-scan with optimal info
			BasicTelescopeSystemsSynopsis optscope = new BasicTelescopeSystemsSynopsis();
			optscope.setDomeLimit(Math.toRadians(25.0));
			optscope.setAutoguiderStatus(true);

			String iregHost = config.getProperty("ireg-host", "localhost");
			String iregUrl = "rmi://" + iregHost + "/InstrumentRegistry";
			BasicInstrumentSynopsisModel bism = new BasicInstrumentSynopsisModel(iregUrl);
			bism.asynchLoadFromRegistry();

			OptimisticInstrumentSynopsisModel ism = new OptimisticInstrumentSynopsisModel(bism);

			AstrometryCalculator astrocalc = new BasicAstrometryCalculator();

			StandardExecutionFeasibilityModel xfm = new StandardExecutionFeasibilityModel(astrocalc, tru, cam, site,
					optscope, ism);

			// start time (yyyy-MM-dd HH:mm:ss) - usually midday
			long start = (sdf.parse(config.getProperty("start"))).getTime();

			// number of days to run over
			int ndays = config.getIntValue("days", 1);

			// interval (ms) supplied in minutes
			long interval = (long) (config.getDoubleValue("interval", 15.0) * 60000.0);

			// data collection
			Set<GroupItem> feasible = new HashSet<GroupItem>();
			double[] dmd = new double[1440]; // dmd at minute intervals

			String pfilebase = config.getProperty("prescan-base", "/occ/logs/volatility");
			File csfile = new File(pfilebase + "/contention_" + udf.format(new Date()) + ".txt");
			PrintStream csout = new PrintStream(csfile);

			File dsfile = new File(pfilebase + "/demand_" + udf.format(new Date()) + ".txt");
			PrintStream dsout = new PrintStream(dsfile);

			// create an array to store the contention results as we run.

			// Disruptor list
			List<Disruptor> disruptors = new Vector<Disruptor>();

			List<GroupItem> glist = p2g.listGroups();
			System.err.println("There are " + glist.size() + " groups");

			// load the account and history caches
			Iterator<GroupItem> igl = glist.iterator();
			while (igl.hasNext()) {

				GroupItem group = igl.next();
				try {

					AccountSynopsis accounts = casm.getAccountSynopsis(group.getProposal().getID(), start);
					// accountsMap.put(group.getProposal().getID(), accounts);

					ExecutionHistorySynopsis history = chsm.getExecutionHistorySynopsis(group.getID(), start);
					// histMap.put(group.getID(), history);

				} catch (Exception e) {
					System.err.printf("Exception while load accounts/history for group %s \n", group.getName());
					e.printStackTrace();
				}
			} // next group

			// start and run for n days
			long time = start;
			long startDay = start;
			for (int id = 0; id < ndays; id++) {

				System.err.printf("Start contention scan for: %tF (Day %2d of %2d) using %4d groups \n", time, (id + 1), ndays, glist.size());

				feasible.clear();
				for (int i = 0; i < 1440; i++) {
					dmd[i] = 0.0;
				}

				AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);

				SolarCalculator sun = new SolarCalculator();
				Coordinates sunTrack = sun.getCoordinates(time);
				double sunlev = astro.getAltitude(sunTrack, time);

				// step forward 1 minute at a time until sun sets..
				while (sunlev > 0.0) {
					sunTrack = sun.getCoordinates(time);
					sunlev = astro.getAltitude(sunTrack, time);
					time += 60000L; // step 1 minute till sunset
				}

				// step forward until sun rises or next day occurs
				while (time < startDay + 24 * 3600 * 1000L && sunlev < Math.toRadians(1.0)) {

					// setup count for this time step
					int contention = 0;

					// Env snapshot
					EnvironmentSnapshot env = new EnvironmentSnapshot(time, 0.3,
							EnvironmentSnapshot.EXTINCTION_PHOTOM);

					int ngt = 0;
					Iterator<GroupItem> ig = glist.iterator();
					while (ig.hasNext()) {

						GroupItem group = ig.next();
						try {

							AccountSynopsis accounts = casm.getAccountSynopsis(group.getProposal().getID(), time);
							// AccountSynopsis accounts =
							// accountsMap.get(group.getProposal().getID());

							ExecutionHistorySynopsis history = chsm.getExecutionHistorySynopsis(group.getID(), time);
							// ExecutionHistorySynopsis history =
							// histMap.get(group.getID());

							ExecutionResourceBundle xrb = tru.getEstimatedResourceUsage(group);
							ExecutionResource timeUsage = xrb.getResource("TIME");
							double execTime = timeUsage.getResourceUsage();

							if (xfm.isitFeasible(group, time, history, accounts, env, disruptors).isFeasible()) {
								// increment contention count for this time step
								contention++;
								feasible.add(group);
							}
							ngt++;
						} catch (Exception e) {
							System.err.printf("Exception while testing group %4d %s \n", ngt, group.getName());
							e.printStackTrace();
						}

					} // next group

					// log results for this time step
					System.err.printf("CONTENTION %tF %tT %4d \n", time, time, contention);
					csout.printf("%tF %tT %4.2f \n", time, time, (double) contention);

					// increment time
					sunTrack = sun.getCoordinates(time);
					sunlev = astro.getAltitude(sunTrack, time);
					time += interval;

				} // next time step in day

				// now work out demand
				System.err.printf("Starting demand calculations using %4d feasible groups\n", feasible.size());
				
				// for each group in feasibles
				int ig = 0;
				Iterator<GroupItem> iff = feasible.iterator();
				while (iff.hasNext()) {
					ig++;
					GroupItem g = iff.next();
					AccountSynopsis accounts = casm.getAccountSynopsis(g.getProposal().getID(), time);
					ExecutionHistorySynopsis history = chsm.getExecutionHistorySynopsis(g.getID(), time);
					ExecutionResourceBundle xrb = tru.getEstimatedResourceUsage(g);
					ExecutionResource timeUsage = xrb.getResource("TIME");
					double execTime = timeUsage.getResourceUsage();

					// work out the length of feasibility period
					long t = startDay;
					EnvironmentSnapshot env = new EnvironmentSnapshot(t, 0.3,
							EnvironmentSnapshot.EXTINCTION_PHOTOM);
					double txt = 0.0;
					while (t < startDay + 24 * 3600 * 1000L) {

						if (xfm.isitFeasible(g, t, history, accounts, env, disruptors).isFeasible()) {
							txt += 60000.0;
						}
						t += 60 * 1000L;
					}
					double dmdfrac = execTime / (execTime + txt);
					System.err.printf("Group %4d of %4d %s with xt %6.2fm has feasible period %6.2fm, dmdfrac = %6.2f \n", 
							ig, feasible.size(), g.getName(), (execTime/60000.0), (txt/60000.0), dmdfrac);
					// fill in the dmd values through the period
					t = startDay;
					while (t < startDay + 24 * 3600 * 1000L) {
						if (xfm.isitFeasible(g, t, history, accounts, env, disruptors).isFeasible()) {
							int it = (int) ((double) (t - startDay) / 60000.0);
							dmd[it] += dmdfrac;
						}
						t += 60 * 1000L;
					}
					
				} // next feasible group

				for (int it = 0; it < 1440; it++) {
					long t = startDay + it*60*1000L;
					System.err.printf("DEMAND %tF %tT %4.2f \n", t, t, dmd[it]);
					dsout.printf("%tF %tT %4.2f \n", t, t, dmd[it]);
				}
				
				// jump to next noon
				startDay += 24 * 3600 * 1000L;

			} // next day

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.err.println("Completed");
		System.exit(0);

	}
}
