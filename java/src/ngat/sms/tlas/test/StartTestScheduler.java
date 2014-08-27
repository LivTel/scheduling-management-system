/**
 * 
 */
package ngat.sms.tlas.test;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.util.HashMap;
import java.util.Map;

import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.AstrometryProvider;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.BasicSite;
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
import ngat.sms.*;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.legacy.GroupRef;
import ngat.sms.legacy.ScheduleServer;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.sms.tlas.*;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * Starts an ExecutionUpdateManager
 * 
 * @author eng
 * 
 */
public class StartTestScheduler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Logger alogger = LogManager.getLogger("SMS");
		alogger.setLogLevel(2);
		ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
		console.setLogLevel(2);
		alogger.addExtendedHandler(console);

		LogGenerator logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider").srcCompClass("StartExMgr");

		LogCollator collator = logger.create().extractCallInfo().info().level(1);

		// System.setSecurityManager(new RMISecurityManager());

		ConfigurationProperties config = CommandTokenizer.use("--").parse(args);

		boolean bound = false;
		// keep trying until we bind successfully
		while (!bound) {

			try {
				collator.msg("Configuring...");
				String ref = config.getProperty("ref", "ScheduleDespatcher");

				// where we will bind the scheduler and xmgr
				String host = config.getProperty("bind-host", "localhost");

				// where we will locate the base models
				String bhost = config.getProperty("base-host", "localhost");

				// where we will locate the aggregate/composite models
				String ghost = config.getProperty("comp-host", "localhost");

				// skymodel host
				String smhost = config.getProperty("sm-host", "localhost");

				String rcshost = config.getProperty("rcs-host", "localhost");

				int rcsPort = config.getIntValue("rcs-port", 9110);
				
				String aghost = config.getProperty("ag-host", "localhost");

				int agPort = config.getIntValue("ag-port", 6571);

				// the rmi port on all hosts ?
				int port = config.getIntValue("port", 1099);

				collator.msg("Starting main launcher").send();

				TimeModel timeModel = new RealTimeModel();


				// NOTE MUST Either comment out section (A) OR section (B) 

				// A) Create a temporary history model
				//String hmhost = config.getProperty("hist-host", "localhost");
				//MysqlHistoryModel history = new MysqlHistoryModel("jdbc:mysql://" + hmhost
					//+ "/phase2odb?user=oss&password=ng@toss");
				//collator.msg("Created MysqlHistoryModel").send();

				// B) Lookup remote history model
				//String hmhost = config.getProperty("hist-host", "localhost");
				//String hmUrl = "rmi://" + hmhost + ":" + port + "/HistoryModel";
				//IHistoryModel history = (IHistoryModel)Naming.lookup(hmUrl);
			        //collator.msg("Obtained remote HistoryModel").send();

				DefaultBaseModelProvider bmp = new DefaultBaseModelProvider(bhost);

				// Find phase2 group model provider
				
				String smpUrl = "rmi://" + ghost + ":" + port + "/SynopticModelProvider";
				collator.msg("Lookup: "+smpUrl).send();
				//Phase2GroupModelProvider p2g = (Phase2GroupModelProvider) Naming.lookup(p2gUrl);
				SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup(smpUrl);
				collator.msg("Located Synoptic Model Provider using: " + smpUrl).send();
			
				// Grab phase2 group model
/*
				Object gp2 = p2g.getPhase2Model();
				collator.msg("P2G.getPhase2Model() returned: " + gp2).send();
				Phase2CompositeModel gphase2 = (Phase2CompositeModel) gp2;
				collator.msg("Grabbed Phase2 Group Model").send();

				// Create Synoptic model
				BasicHistorySynopsisModel bsm = new BasicHistorySynopsisModel(history);
				collator.msg("Created Synoptic History Model").send();

				// Accounting models

				// Proposals
				String accUrl = "rmi://" + bhost + ":" + port + "/ProposalAccountModel";
				IAccountModel accModel = (IAccountModel) Naming.lookup(accUrl);
				collator.msg("Located Proposal Accounting Model using: " + accUrl).send();

				// Proposal Acc synopsis.
				BasicAccountSynopsisModel pasm = new BasicAccountSynopsisModel(accModel);
				collator.msg("Created Synoptic Proposal Account Model").send();

				// TAGs
				String taccUrl = "rmi://" + bhost + ":" + port + "/TagAccountModel";
				IAccountModel taccModel = (IAccountModel) Naming.lookup(taccUrl);
				collator.msg("Located TAG Accounting Model using: " + taccUrl).send();

				// TAG Acc synopsis.
                                BasicAccountSynopsisModel tasm = new BasicAccountSynopsisModel(taccModel);
                                collator.msg("Created Synoptic Proposal Account Model").send();


				// Load all synopses..
				collator.msg("Loading history").send();
				bsm.loadHistory(gphase2, timeModel.getTime());

				collator.msg("Loading proposal accounts").send();
				pasm.loadProposalAccounts(gphase2, timeModel.getTime());

				collator.msg("Loading TAG accounts").send();
                                tasm.loadTagAccounts(gphase2, timeModel.getTime());
*/
				// Charging
				ChargeAccountingModel chargeModel = new StandardChargeAccountingModel();

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

				double lat = Math.toRadians(config.getDoubleValue("lat"));
				double lon = Math.toRadians(config.getDoubleValue("long"));

				BasicSite site = new BasicSite("Obs", lat, lon);
				// AstrometryCalculator calc = provider.getCalculator();

				BasicAstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);
				BasicAstrometryCalculator calc = new BasicAstrometryCalculator();
				
				collator.msg("Retrieved astrometry calculator: " + astro).send();
				TestResourceUsageEstimator xrm = new TestResourceUsageEstimator();

				// Instrumentals
				String iregUrl = "rmi://" + rcshost + ":" + port + "/InstrumentRegistry";
				InstrumentRegistry ireg = (InstrumentRegistry) Naming.lookup(iregUrl);
				collator.msg("Located Instrument registry using: " + iregUrl).send();

				BasicInstrumentSynopsisModel bit = new BasicInstrumentSynopsisModel(iregUrl);
				bit.asynchLoadFromRegistry();

				// Telescope - grab the AG status
				BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();
				//scope.setDomeLimit(Math.toRadians(config.getDoubleValue("dome.limit", 25.0)));
				try {
				    PropertiesConfigurator.use(new File("telescope.properties")).configure(scope);				
				} catch(Exception cx) {
				    scope.setDomeLimit(Math.toRadians(25.0));
				}
				scope.startMonitoring(rcshost, rcsPort, aghost, agPort, 20000L);

				StandardExecutionFeasibilityModel bxm = new StandardExecutionFeasibilityModel(calc, xrm, chargeModel, site,
						scope, bit);
				collator.msg("Created Test ExecutionFeasibilityModel").send();

				// Skymodel
				String smUrl = "rmi://" + smhost + ":" + port + "/SkyModel";

				AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
				//AccountSynopsisModel tasm = smp.getTagAccountSynopsisModel();
				ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();
				Phase2CompositeModel gphase2 = smp.getPhase2CompositeModel();
				
				// SchedulingStatusProvider			
				TestLookAheadScheduler tlas = new TestLookAheadScheduler(timeModel,bxm, xrm, smp, astro, xm);
				
				tlas.setSkyModelUrl(smUrl);
				tlas.setHorizon(3600*1000L);
				tlas.setNumberSweeps(25);
				collator.msg("Created SchedulingStatusProvider").send();

				String schedUrl = "rmi://" + host + ":" + port + "/SchedulingStatusProvider";
				Naming.rebind(schedUrl, tlas);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulingStatusProvider as: " + schedUrl)
						.send();
				
				String sdUrl = "rmi://" + host + ":" + port + "/ScheduleDespatcher";
				Naming.rebind(sdUrl, tlas);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulingStatusProvider as: " + sdUrl).send();

				String asdUrl = "rmi://" + host + ":" + port + "/AsynchScheduleDespatcher";
				Naming.rebind(asdUrl, tlas);
				logger.create().extractCallInfo().info().level(1).msg("Bound SchedulingStatusProvider as: " + asdUrl).send();

	
				// now create the legacy server with an empty map.
				int smsPort = config.getIntValue("sms-server-port", 3979);
				Map<Long, GroupRef> gmap = new HashMap<Long, GroupRef>();
				VetoManager vetoManager = (VetoManager)tlas;
				// this is slightly risky, really we should pass a seperate VC in here rather than just
				// assume TSD is "one-of-them"
				logger.create().extractCallInfo().info().level(1).msg("Starting legacy server...").send();
				VetoManager vmgr = (VetoManager)tlas;
				ScheduleServer server = new ScheduleServer("SMS_SERVER", smsPort, tlas, vmgr, gmap);
				server.start();
				logger.create().extractCallInfo().info().level(1).msg("Started legacy server on port: " + smsPort).send();

				bound = true;
				
			} catch (Exception ex) {
				ex.printStackTrace();
				// sleep a minute then try again			
					try {
						Thread.sleep(60000L);
					} catch (Exception e) {
					}
			}

		}

		while (true) {
			try {
				Thread.sleep(60000L);
			} catch (Exception e) {
			}
		}

	}
}
