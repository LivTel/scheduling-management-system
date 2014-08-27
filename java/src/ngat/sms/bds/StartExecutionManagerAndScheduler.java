/**
 * 
 */
package ngat.sms.bds;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.AstrometryProvider;
import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.ems.SkyModel;
import ngat.icm.InstrumentRegistry;
import ngat.oss.impl.mysql.model.AccountModel;
import ngat.oss.model.IAccountModel;
import ngat.oss.model.IHistoryModel;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XGroup;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.phase2.XProgram;
import ngat.phase2.XProposal;
import ngat.phase2.XTag;
import ngat.phase2.XUser;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BasicAccountSynopsisModel;
import ngat.sms.BasicHistorySynopsisModel;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.BasicSkyModelProvider;
import ngat.sms.BasicSynopticModelProxy;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.DefaultBaseModelProvider;
import ngat.sms.DefaultExecutionFeasibilityModelService;
import ngat.sms.DefaultExecutionUpdateManager;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.GroupItem;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.Phase2GroupModelProvider;
import ngat.sms.RealTimeModel;
import ngat.sms.SchedulingArchiveGateway;
import ngat.sms.SkyModelProvider;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TimeModel;
import ngat.sms.VetoManager;
import ngat.sms.legacy.GroupRef;
import ngat.sms.legacy.ScheduleServer;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.sms.util.FeasibilityPrescan;
import ngat.sms.util.PrescanEntry;
import ngat.sms.bds.test.FixedTestScheduler;
import ngat.tcm.Telescope;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * Starts an ExecutionUpdateManager
 * 
 * @author eng
 * 
 */
public class StartExecutionManagerAndScheduler {
	
	static final String SCHED_URL = "jdbc:mysql://localhost/telemetry?user=data&password=banoffeelogger67";
	
	static SimpleDateFormat udf = new SimpleDateFormat("yyyyMMddHHmm");
	static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// System.setSecurityManager(new RMISecurityManager());
		TimeZone.setDefault(UTC);
		udf.setTimeZone(UTC);

		ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

		// FUDGE add some extra system properties
		try {
            Properties sysextra = new Properties();
            sysextra.load(new FileInputStream("/occ/rcs/config/system.properties"));
            Enumeration e = sysextra.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = sysextra.getProperty(key);
                System.setProperty(key, val);
            }          
        } catch (Exception ex) {
            System.err.println("WARNING - SMS_Startup: unable to load extra system properties: " + ex);
        }

		
		int loglevel = config.getIntValue("log-level", 2);
		Logger alogger = LogManager.getLogger("SMS");
		alogger.setLogLevel(loglevel);
		ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
		console.setLogLevel(loglevel);
		alogger.addExtendedHandler(console);

		LogGenerator logger = alogger.generate().system("SMS")
				.subSystem("SchedulerStartup")
				.srcCompClass("StartExMgr");

		LogCollator collator = logger.create().extractCallInfo().info().level(1);

		Logger gss = LogManager.getLogger("GSS");
		gss.setLogLevel(2);
		ConsoleLogHandler gconsole = new ConsoleLogHandler(new BogstanLogFormatter());
		gconsole.setLogLevel(2);
		gss.addExtendedHandler(gconsole);

		Logger astroLog = LogManager.getLogger("ASTRO");
		astroLog.setLogLevel(0);
		// switched off for now
		ConsoleLogHandler aconsole = new ConsoleLogHandler(new BasicLogFormatter(150));
		aconsole.setLogLevel(5);
		astroLog.addExtendedHandler(aconsole);
		
		collator.msg("Configuring...").send();
		// String ref = config.getProperty("ref", "ScheduleDespatcher");

		// where we will bind the scheduler and xmgr
		String host = config.getProperty("bind-host", "localhost");

		String ihost = config.getProperty("ireg-host", "localhost");

		String thost = config.getProperty("tel-host", "localhost");

		// where we will locate the base models
		// String bhost = config.getProperty("base-host", "localhost");

		// where we will locate the aggregate/composite models
		String ghost = config.getProperty("comp-host", "localhost");

		// skymodel host
		String smhost = config.getProperty("sm-host", "localhost");

		String rcshost = config.getProperty("rcs-host", "localhost");

		int rcsPort = config.getIntValue("rcs-port", 9110);

		String aghost = config.getProperty("ag-host", "autoguider1");

		int agPort = config.getIntValue("ag-port", 6571);

		// the rmi port on all hosts ?
		int port = config.getIntValue("port", 1099);

		// legacy SMS message server JMS port.
		int smsPort = config.getIntValue("sms-server-port", 3979);

		// SchedulingStatusProvider
		BasicDespatchScheduler bds = null;

		// a veto map..*/
		Map<Long, GroupRef> gmap = null;

		ISite site = null;
		BasicSynopticModelProxy smp = null;
		TestResourceUsageEstimator tru = null;
		ChargeAccountingModel chargeModel = null;
		FeasibilityPrescan fp = null;
		VetoManager vetoManager = null;
		
		boolean bound = false;
		// keep trying until we bind successfully and can see all the external
		// model providers
		while (!bound) {

			try {

				collator.msg("Starting main launcher").send();

				TimeModel timeModel = new RealTimeModel();

				smp = new BasicSynopticModelProxy(ghost);
				smp.asynchLoadSynopticModel();

				// Charging
				chargeModel = new StandardChargeAccountingModel();

				// Create execution update manager
				DefaultExecutionUpdateManager xm = new DefaultExecutionUpdateManager(smp, chargeModel);
				collator.msg("Created DefaultExecMgr").send();

				String exmgrUrl = "rmi://" + host + ":" + port + "/ExecutionUpdateManager";
				Naming.rebind(exmgrUrl, xm);

				logger.create().extractCallInfo().info().level(1).msg("Bound Execution Update Manager as: " + exmgrUrl)
						.send();

				// AstrometryProvider provider = (AstrometryProvider)Naming
				// .lookup("rmi://" + host+":"+port+ "/AstrometryProvider");
				// collator.msg("Got astrometry provider ref: "+provider).send();
				// AstrometryCalculator calc = provider.getCalculator();

				BasicAstrometryCalculator calc = new BasicAstrometryCalculator();

				collator.msg("Retrieved astrometry calculator: " + calc).send();

				double lat = Math.toRadians(config.getDoubleValue("lat"));
				double lon = Math.toRadians(config.getDoubleValue("long"));

				site = new BasicSite("Obs", lat, lon);

				tru = new TestResourceUsageEstimator();

				// Instrumentals
				String iregUrl = "rmi://" + ihost + ":" + port + "/InstrumentRegistry";
				BasicInstrumentSynopsisModel bit = new BasicInstrumentSynopsisModel(iregUrl);
				bit.asynchLoadFromRegistry();

			       
				// Telescope - if this fail we fall over !!!! 
				// TODO URGENT needs a asynchLoad() in BasicTelescopeSynopsis like BasicInstSynopsis
				String telescopeUrl = "rmi://" + thost + ":" + port + "/Telescope";
				//Telescope telescope = (Telescope)Naming.lookup(telescopeUrl);

				BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();
				try {
					PropertiesConfigurator.use(new File("telescope.properties")).configure(scope);
				} catch (Exception cx) {
					scope.setDomeLimit(Math.toRadians(25.0));
				}
				
				
				collator.msg("Attempting to load telescope info...").send();
				scope.asynchLoadFromTelescope(telescopeUrl);				
						
				// TODO this should be replaced by talupdates which we should register for....
				scope.startMonitoring(rcshost, rcsPort, aghost, agPort, 60000L);

				StandardExecutionFeasibilityModel bxm = new StandardExecutionFeasibilityModel(calc, tru, chargeModel,
						site, scope, bit);
				collator.msg("Created Test ExecutionFeasibilityModel").send();
				
							
				// Skymodel
				String smUrl = "rmi://" + smhost + ":" + port + "/SkyModel";
				SkyModelProvider skyp = new BasicSkyModelProvider(smUrl);

				// SchedulingStatusProvider
				bds = new BasicDespatchScheduler(timeModel, xm, bxm, smp, skyp, site);
				collator.msg("Created SchedulingStatusProvider").send();

				String schedUrl = "rmi://" + host + ":" + port + "/SchedulingStatusProvider";
				Naming.rebind(schedUrl, bds);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulingStatusProvider as: " + schedUrl).send();

				String sdUrl = "rmi://" + host + ":" + port + "/ScheduleDespatcher";
				Naming.rebind(sdUrl, bds);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulingStatusProvider as: " + sdUrl).send();

				String asdUrl = "rmi://" + host + ":" + port + "/AsynchScheduleDespatcher";
				Naming.rebind(asdUrl, bds);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulingStatusProvider as: " + asdUrl).send();

				DefaultExecutionFeasibilityModelService dxp = new DefaultExecutionFeasibilityModelService(bxm);
				String xfpUrl = "rmi://" + host + ":" + port + "/FeasibilityModelProvider";
				Naming.rebind(xfpUrl, dxp);
				logger.create().extractCallInfo().info().level(1).msg("Bound XFM Provider as as: " + xfpUrl).send();

				// Schedule Archive gateway
				
				SchedulingArchiveGateway shag = new SchedulingArchiveGateway(bds);
				bds.addSchedulingUpdateListener(shag);
				Naming.rebind("SchedulerGateway", shag);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulerArchiveGateway as: " +"SchedulerGateway").send();
				shag.setProcessInterval(30000L);
				shag.setCullInterval(10*60*1000L);
				shag.setBackingStoreAgeLimit(10*60*1000L);
				//shag.setBackingStore(new SchedulingBackingStoreHelper(SCHED_URL));
				shag.startProcessor();

				try {
					// pre-scan with optimal info
					BasicTelescopeSystemsSynopsis optscope = new BasicTelescopeSystemsSynopsis();
					try {
						PropertiesConfigurator.use(new File("telescope.properties")).configure(optscope);
					} catch (Exception cx) {
						optscope.setDomeLimit(Math.toRadians(25.0));
					}
					optscope.setAutoguiderStatus(true);
					fp = new FeasibilityPrescan(site, smp, tru, chargeModel, optscope, bit);
					Naming.rebind("FeasibilityPrescanner", fp);
					logger.create().extractCallInfo().info().level(1).msg("Bound FeasibilityPrescanner").send();
				} catch (Exception ex) {
					ex.printStackTrace();
					// shame but whatever....
				}

				gmap = new HashMap<Long, GroupRef>();
				vetoManager = bds.getVetoManager();
				
				// Test scheduler for local testing of fg OCs
				// FixedTestScheduler fts = new FixedTestScheduler();

				bound = true;

			} catch (Exception ex) {
				ex.printStackTrace();
				// sleep a minute then try again
				try {
					Thread.sleep(60000L);
				} catch (Exception e) {
				}
			}

		} // next synch/bind attempt

		boolean serverBound = false;
		while (!serverBound) {
			try {
				ScheduleServer server = new ScheduleServer("SMS_SERVER", smsPort, bds, vetoManager, gmap);
				server.start();
				logger.create().extractCallInfo().info().level(1).msg("Started legacy server on port: " + smsPort)
						.send();
				serverBound = true;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			// sleep a minute then try again
			try {
				Thread.sleep(60000L);
			} catch (Exception e) {
			}
		} // next server bind attempt

		
		// attach contention/score logger
		
		try {
			ContentionLogger clog = new ContentionLogger(new File("/occ/logs/scorelog_"+udf.format(new Date())+".txt"));
			bds.addSchedulingUpdateListener(clog);
			logger.create().extractCallInfo().info().level(1).msg("Added score logger").send();
		} catch (Exception e) {
			e.printStackTrace();
			// whatever
		}
		
		
		if (config.getProperty("prescan") != null) {
			
			// Run prescan check...

			try {
				String pfilebase = config.getProperty("prescan-base", "/occ/logs/volatility");			
				File psfile = new File(pfilebase+"/prescan_"+udf.format(new Date())+".txt");
				PrintStream psout = new PrintStream(psfile);
			
				logger.create().extractCallInfo().info().level(1).msg("Running prescan, output to: "+psfile+ "...").send();

				// prescan done with 10 minute blocks
				List candidates = fp.prescan(System.currentTimeMillis(), 60 * 1000L);

				long start = 0L;
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
					psout.printf("GROUP %s %25.25s %6.2f %6.2f %s\n",pse.gname, grname, (gexec/60000.0), totmins, pse.display());

					start = pse.start;
					end = pse.end;
					exectot += gexec;
				}
				logger.create().extractCallInfo().info().level(2).msg(
						"Total available exectime: " + (exectot / 3600000.0) + " H").send();
				psout.printf("TOTAL_TIME %4.2f \n",(exectot / 3600000.0));
				
				File csfile = new File(pfilebase+"/contention_"+udf.format(new Date())+".txt");
				PrintStream csout = new PrintStream(csfile);
			
				logger.create().extractCallInfo().info().level(1).msg("Running contention scan, output to: "+csfile+ "...").send();

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
				// failed to do this stuff but no matter
			}
		} // do prescan and contention

		// now lets see about pruning...
		if (config.getProperty("prune") != null) {

			logger.create().extractCallInfo().info().level(1).msg("Running phase2 pruning scan...").send();
			smp.asynchPruneWhenReady(fp);

		}

		// keep rmi alive
		while (true) {
			try {
				Thread.sleep(60000L);
			} catch (Exception e) {
			}
		}

	}
}
