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
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;

import ngat.astrometry.BasicSite;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.GroupItem;
import ngat.sms.SynopticModelProvider;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;

/** Run a prescan check.
 * @author eng
 *
 */
public class RunPrescan {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	 
		try {
			
			SimpleDateFormat udf = new SimpleDateFormat("yyyyMMddHHmm");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
			udf.setTimeZone(UTC);
			sdf.setTimeZone(UTC);
		 
			Calendar.getInstance().setTimeZone(UTC);
			
			ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

			// Telescope location
			double lat = Math.toRadians(config.getDoubleValue("lat"));
			double lon = Math.toRadians(config.getDoubleValue("long"));

			BasicSite site = new BasicSite("Obs", lat, lon);

			// where is the SMP ?
			String smpHost = config.getProperty("smp-host", "localhost");
			SynopticModelProvider smp = (SynopticModelProvider) Naming.lookup("rmi://" + smpHost
					+ "/SynopticModelProvider");
			
			String iregHost = config.getProperty("ireg-host", "localhost");
			String iregUrl = "rmi://" + iregHost + "/InstrumentRegistry";
			BasicInstrumentSynopsisModel bism = new BasicInstrumentSynopsisModel(iregUrl);
			bism.asynchLoadFromRegistry();

			ChargeAccountingModel cam = new StandardChargeAccountingModel();

			TestResourceUsageEstimator xrm = new TestResourceUsageEstimator();
			
			// pre-scan with optimal info
			BasicTelescopeSystemsSynopsis optscope = new BasicTelescopeSystemsSynopsis();
			optscope.setDomeLimit(Math.toRadians(25.0));
			optscope.setAutoguiderStatus(true);
			
			// start time (yyyy-MM-dd HH:mm:ss) - usually midday
			long start = (sdf.parse(config.getProperty("start"))).getTime();

			// interval (ms) supplied in minutes
			long interval = (long) (config.getDoubleValue("interval", 1.0) * 60000.0);

			FeasibilityPrescan prescan = new FeasibilityPrescan(site, smp, xrm, cam, optscope, bism);
		
			// prescan done with 10 minute blocks
			List candidates = prescan.prescan(start, interval);
			
			String pfilebase = config.getProperty("prescan-base", "/occ/logs/volatility");
			File psfile = new File(pfilebase + "/prescan_" + udf.format(new Date()) + ".txt");		
			PrintStream psout = new PrintStream(psfile);
			
			long end = 0L;
			double exectot = 0.0;
			Iterator ig = candidates.iterator();
			while (ig.hasNext()) {
				PrescanEntry pse = (PrescanEntry) ig.next();
				// gname, group.name, exectime
				GroupItem g = pse.group;
				double gexec = pse.execTime;
				String grname =( g != null ? g.getName(): "NONE");
				long grid = (g != null ? g.getID() : -1);
				double totmins = pse.feasibleTotal()/60000.0;
				// how many execs?
				int nx = 1;
				ITimingConstraint timing = g.getTimingConstraint();
				if (timing instanceof XMonitorTimingConstraint) {
					long period = ((XMonitorTimingConstraint) timing).getPeriod();
					nx = (int) ((double) (pse.feasibleTotal()) / (double) period) + 1;
				} else if (timing instanceof XMinimumIntervalTimingConstraint) {
					long period = ((XMinimumIntervalTimingConstraint) timing).getMinimumInterval();
					nx = (int) ((double) (pse.feasibleTotal()) / (double) period) + 1;
				}
				psout.printf("GROUP %s %25.25s %6.2f %6.2f [x%-2d] %s\n",pse.gname, grname, (gexec/60000.0), totmins, nx, pse.display());

				start = pse.start;
				end = pse.end;
				exectot += gexec;
			}
		
			psout.printf("TOTAL_TIME %4.2f \n",(exectot / 3600000.0));
			
			File csfile = new File(pfilebase +"/prescan_cd_"+udf.format(new Date())+".txt");
			PrintStream csout = new PrintStream(csfile);
		
			// now lets do a contention and demand scan
			int nn = (int) ((end - start) / 60000) + 1;
			double[] cc = new double[nn];
			double[] cd = new double[nn];
			Iterator ic = candidates.iterator();
			while (ic.hasNext()) {
				PrescanEntry pse = (PrescanEntry) ic.next();
				double xt = pse.execTime;
				double gw = pse.feasibleTotal() / (double) pse.nx;
				double cg = xt / (xt + gw);
				for (int it = 0; it < nn; it++) {
					long t = start + it * 60000L;
					if (pse.isFeasible(t)) {
						cd[it] += cg;
						cc[it]++;
					}
				}
			}

			// plot contention
			for (int it = 0; it < nn; it++) {
				long t = start + it * 60000L;
				System.err.printf("%tF %tT %4.2f %4.2f \n", t, t, cc[it], cd[it]);
				csout.printf("%tF %tT %4.2f %4.2f \n", t, t, cc[it], cd[it]);
			}
			csout.close();
			
		 
	 } catch (Exception e) {
		 e.printStackTrace();
	 }
	 	System.err.println("Completed");
		System.exit(0);
	}

}
