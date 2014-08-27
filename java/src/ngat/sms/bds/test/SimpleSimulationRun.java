/**
 * 
 */
package ngat.sms.bds.test;

import ngat.util.logging.*;
import ngat.util.*;
import ngat.astrometry.*;
import ngat.oss.model.*;
import ngat.phase2.*;
import ngat.sms.*;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.bds.BasicDespatchScheduler;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.icm.*;

import java.rmi.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SimpleTimeZone;

/**
 * @author eng
 *
 */
public class SimpleSimulationRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		SimpleTimeZone utc = new SimpleTimeZone(0, "UTC");
		sdf.setTimeZone(utc);
		
		Logger alogger = LogManager.getLogger("SMS");
		alogger.setLogLevel(5);
		ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
		console.setLogLevel(5);
		alogger.addExtendedHandler(console);
		
		LogGenerator logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider").srcCompClass("StartExMgr");
		
		LogCollator collator = logger.create().extractCallInfo().info().level(1);
		
		try {
			
			ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
			
			String glsHost = config.getProperty("gls-host", "localhost");
			int glsPort = config.getIntValue("gls-port", 2375);
			try {
				DatagramLogHandler dlh = new DatagramLogHandler(glsHost, glsPort);
				dlh.setLogLevel(5);
				alogger.addExtendedHandler(dlh);
			} catch (Exception e) {
				e.printStackTrace();
			}
			collator.msg("Configuring...");
//			String ref = config.getProperty("ref", "ScheduleDespatcher");
			String host = config.getProperty("bind-host", "localhost");
			String bhost = config.getProperty("base-host", "localhost");
			String shost = config.getProperty("s-host", "localhost");
			// skymodel host
			String smhost = config.getProperty("sm-host", "localhost");
			
			int port = config.getIntValue("port", 1099);
			collator.msg("Starting main launcher").send();
			
		/*	// Create a temporary history model
			MysqlHistoryModel history = new MysqlHistoryModel("jdbc:mysql://ltdev1/phase2odb?user=oss&password=ng@toss");
			collator.msg("Created MysqlHistoryModel").send();
			
			// Create Synoptic model
			BasicHistorySynopsisModel bsm = new BasicHistorySynopsisModel(history);
			collator.msg("Created Synoptic History Model").send();
			*/
			
			String smpUrl = "rmi://"+shost+":"+port+"/SynopticModelProvider";
			SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup(smpUrl);
			
			// Accounting models
			DefaultBaseModelProvider bmp = new DefaultBaseModelProvider(bhost);
			
/*			// Proposals			
			String accUrl = "rmi://"+bhost+":"+port+"/ProposalAccountModel";
			IAccountModel accModel = (IAccountModel)Naming.lookup(accUrl);
			collator.msg("Located Proposal Accounting Model").send();
			
			// Acc synopsis.
			BasicAccountSynopsisModel basm = new BasicAccountSynopsisModel(accModel);
			collator.msg("Created Synoptic Proposal Account Model").send();
			
			// TAGs
			String taccUrl = "rmi://"+bhost+":"+port+"/TagAccountModel";
			IAccountModel taccModel = (IAccountModel)Naming.lookup(taccUrl);
			collator.msg("Located TAG Accounting Model").send();
			
			// Acc synopsis.
			BasicAccountSynopsisModel tasm = new BasicAccountSynopsisModel(taccModel);
			collator.msg("Created Synoptic TAG Account Model").send();
						*/
			// Create execution update manager
			DefaultExecutionUpdateManager xm = new DefaultExecutionUpdateManager(smp, null);			
			collator.msg("Created DefaultExecMgr").send();
						
			// Find phase2 group model provider
			/*Phase2GroupModelProvider p2g = (Phase2GroupModelProvider)Naming.
				lookup("rmi://"+ghost+":"+port+"/Phase2GroupModelProvider");
			collator.msg("Located Phase2 Group Model Provider").send();
			
			// Grab phase2 group model
			Phase2CompositeModel gphase2 = p2g.getPhase2Model();
			collator.msg("Grabbed Phase2 Group Model");*/
			
			BasicAstrometryCalculator calc = new BasicAstrometryCalculator();

			collator.msg("Retrieved astrometry calculator: "+calc).send();
			
			double lat = Math.toRadians(config.getDoubleValue("lat"));
			double lon = Math.toRadians(config.getDoubleValue("long"));

			BasicSite site = new BasicSite("Obs", lat, lon);
			
			ChargeAccountingModel cam = new StandardChargeAccountingModel();
			
			TestResourceUsageEstimator tru = new TestResourceUsageEstimator();
			
			
			
			// Instrumentals			
			BasicInstrumentSynopsisModel bit = new BasicInstrumentSynopsisModel("rmi://"+host+":"+port+"/InstrumentRegistry");			
			bit.asynchLoadFromRegistry();
			
			// Telescope
			BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();
			scope.setDomeLimit(Math.toRadians(config.getDoubleValue("dome.limit", 25.0)));
			
			StandardExecutionFeasibilityModel bxm = new StandardExecutionFeasibilityModel(calc,tru, cam, site, scope, bit);
			collator.msg("Created Test ExecutionFeasibilityModel").send();

			// Timing			
			long startTime = sdf.parse(config.getProperty("start")).getTime();
			long endTime = sdf.parse(config.getProperty("end")).getTime();					
			long time = startTime;
			
			DefaultMutableTimeModel timeModel = new DefaultMutableTimeModel();
			timeModel.setTime(time);
			
			String smUrl = "rmi://" + smhost + ":" + port + "/SkyModel";
			SkyModelProvider skyp = new BasicSkyModelProvider(smUrl);
			
			BasicDespatchScheduler tsd = new BasicDespatchScheduler(timeModel, xm, bxm, smp, skyp, site);			
			collator.msg("Created Schedule Despatcher").send();
			
			while (time < endTime) {

				timeModel.setTime(time);
				
				ScheduleItem sched = tsd.nextScheduledJob();
				// there may not be any available so NULL or
				// NoJobsAvailableException
				if (sched == null) {
					System.err.println("There was nothing available at "+sdf.format(new Date(time)));
					time += 5*60*1000L;
					continue;
				}

				GroupItem group = sched.getGroup();

				collator.msg("Obtained schedule: " + sched).send();
				collator.msg("Group: " + group.getName()).send();

				ISequenceComponent seq = group.getSequence();
				try {
					ComponentSet cset = new ComponentSet(seq);
					System.err.println("CS: " + cset);
				} catch (Exception e) {
					e.printStackTrace();
				}

				ExecutionResourceBundle terb = tru.getEstimatedResourceUsage(group);
				ExecutionResource timeUsage = terb.getResource("TIME");

				collator.msg("Estimated time to complete: " + timeUsage.getResourceUsage()).send();

				// setup resource bundle
				ExecutionResourceBundle erb = new ExecutionResourceBundle();
				erb.addResource(timeUsage);

				for (int i = 0; i < 5; i++) {
					ExecutionResource exr = new ExecutionResource("FakeResource-" + i, Math.random() * 10.0);
					erb.addResource(exr);
					collator.msg("Add resource: " + exr).send();
				}

				ExecutionUpdater xu = sched.getExecutionUpdater();

				// do that update
				collator.msg("Prepare to update").send();
				// add the estimated exec time to now
				long execTime = (long) timeUsage.getResourceUsage();

				// maybe complete or maybe fail
				if (Math.random() > 0.2) {

					// send some exposure updates to the XU
					for (int i = 0; i < 5; i++) {
						try {
							Thread.sleep(2000L);
						} catch (Exception e) {
						}
						xu.groupExposureCompleted(group, System.currentTimeMillis(),
								(long) (Math.random() * 10000) + i, "c_e_20090101_12_" + i + "_1.fits");
					}

					Set<IQosMetric> qset = new HashSet<IQosMetric>();
					qset.add(new XQosMetric("QOS_EXP_COMP", 100.0, "Execution completion fraction"));
					qset.add(new XQosMetric("QOS_GRP_SUCC", 1.0, "Group success measure"));

					xu.groupExecutionCompleted(group, System.currentTimeMillis() + execTime, erb, qset);
				} else {

					// send some exposure updates to the XU
					for (int i = 0; i < 2; i++) {
						try {
							Thread.sleep(2000L);
						} catch (Exception e) {
						}
						xu.groupExposureCompleted(group, System.currentTimeMillis(),
								(long) (Math.random() * 10000) + i, "c_e_20090101_12_" + i + "_1.fits");
					}

					XBasicExecutionFailureContext xbf = new XBasicExecutionFailureContext((int) (Math.random() * 1000),
							"A nasty error occurred");
					Set<IQosMetric> qset = new HashSet<IQosMetric>();
					qset.add(new XQosMetric("QOS_EXP_COMP", 30.0, "Execution completion fraction"));
					qset.add(new XQosMetric("QOS_GRP_SUCC", 0.15, "Group success measure"));
					xu.groupExecutionAbandoned(group, System.currentTimeMillis() + execTime / 2, // let
																									// it
																									// run
																									// for
																									// half
																									// time
																									// to
																									// failure
							erb, xbf, qset);
				}

				// Jump forward
				time += execTime;

			} // next time

			
		} catch (Exception ex) {
			ex.printStackTrace();			
		}
		
		
		
		

	}

}
