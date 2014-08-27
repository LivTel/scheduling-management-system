/**
 * 
 */
package ngat.sms.bds.test;

import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.SimpleTimeZone;

import ngat.phase2.IQosMetric;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XBasicExecutionFailureContext;
import ngat.phase2.XGroup;
import ngat.phase2.XProgram;
import ngat.phase2.XProposal;
import ngat.phase2.XQosMetric;
import ngat.phase2.XTag;
import ngat.phase2.XUser;
import ngat.sms.ComponentSet;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.DefaultExecutionUpdateManager;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionUpdateManager;
import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;
import ngat.sms.ScheduleDespatcher;
import ngat.sms.ScheduleItem;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.DatagramLogHandler;
import ngat.util.logging.LogCollator;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * Send a series of schedule requests and update with time incrementation.
 * 
 * @author eng
 * 
 */
public class SimpleExecutorTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Params:
		// host - The host to lookup for the manager.
		// ref - The reference/name used to lookup the ExecUpdateManager.

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		SimpleTimeZone utc = new SimpleTimeZone(0, "UTC");
		sdf.setTimeZone(utc);

		Logger alogger = LogManager.getLogger("SMS");
		alogger.setLogLevel(5);
		ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
		console.setLogLevel(5);
		alogger.addExtendedHandler(console);

		LogGenerator logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider").srcCompClass("SendSchedReq");

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

			String host = config.getProperty("host", "localhost");
			String ref = config.getProperty("ref", "ScheduleDespatcher");
			int port = config.getIntValue("port", 1099);

			String xref = "rmi://" + host + ":" + port + "/" + ref;
			collator.msg("Lookup: " + xref).send();

			//long startTime = sdf.parse(config.getProperty("start")).getTime();
			//long endTime = sdf.parse(config.getProperty("end")).getTime();

			ScheduleDespatcher sd = (ScheduleDespatcher) Naming.lookup(xref);

			collator.msg("Found Despatcher " + sd).send();

			//long time = startTime;
			//while (time < endTime) {

				ScheduleItem sched = sd.nextScheduledJob();
				// there may not be any available so NULL or
				// NoJobsAvailableException
				if (sched == null) {
					System.err.println("There was nothing available...");
					return;
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

				TestResourceUsageEstimator tru = new TestResourceUsageEstimator();
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

				// time += simTime;

			//} // next time

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
