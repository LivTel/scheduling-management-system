/**
 * 
 */
package ngat.sms.util;

import java.rmi.Naming;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import ngat.astrometry.*;
import ngat.phase2.ISequenceComponent;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.CachedHistorySynopsisModel;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TelescopeSystemsSynopsis;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.genetic.test.OptimisticInstrumentSynopsisModel;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * Performs a lookahead, generating a tree of possible future executions.
 * 
 * @author eng
 * 
 */
public class LookaheadTreeScan {

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	/** Background standards exec time. */
	public static long BGEXEC = 2 * 60 * 1000L;

	LogGenerator logger;

	ExecutionHistorySynopsisModel hsm;

	long start;
	long end;

	public LookaheadTreeScan() {
		Logger alogger = LogManager.getLogger("GSS");
		logger = new LogGenerator(alogger);
		logger.system("SMS").subSystem("GSS").srcCompClass("Prescan");
		sdf.setTimeZone(UTC);
	}

	public TreeNode lookaheadTreeScan(ISite site, long time) throws Exception {

		// TODO setup test environment - later we will pass this in via
		// constructor

		AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);
		SolarCalculator sun = new SolarCalculator();
		Coordinates sunTrack = sun.getCoordinates(time);
		double sunlev = astro.getAltitude(sunTrack, time);

		logger.create().extractCallInfo().level(2).msg("Sun elevation is: " + Math.toDegrees(sunlev)).send();
		boolean daytime = (sunlev > 0.0);

		start = 0L;
		end = 0L;
		if (daytime) {
			logger.create().extractCallInfo().level(2).msg("It is currently daytime").send();
			start = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
			end = start + astro.getTimeUntilNextRise(sunTrack, 0.0, start);
		} else {
			logger.create().extractCallInfo().level(2).msg("It is currently nighttime").send();
			start = time;
			end = time + astro.getTimeUntilNextRise(sunTrack, 0.0, time);
		}
		logger.create().extractCallInfo().level(2).msg(
				"Start prescan at: " + sdf.format(new Date(start)) + ", End at: " + sdf.format(new Date(end))
						+ " Night duration: " + ((end - start) / 3600000) + "H").send();

		// need the following astrocalc, site, exresmodl, cam, telsyn,
		// imstsynmodl
		AstrometryCalculator astrocalc = new BasicAstrometryCalculator();
		ExecutionResourceUsageEstimationModel xrm = new TestResourceUsageEstimator();
		ChargeAccountingModel cam = new StandardChargeAccountingModel();
		TelescopeSystemsSynopsis tel = new BasicTelescopeSystemsSynopsis();
		((BasicTelescopeSystemsSynopsis) tel).setDomeLimit(Math.toRadians(20.0));
		((BasicTelescopeSystemsSynopsis) tel).setAutoguiderStatus(true);

		// TODO sort out the Oism and base ISM
		InstrumentSynopsisModel ism = new OptimisticInstrumentSynopsisModel(null);

		StandardExecutionFeasibilityModel xfm = new StandardExecutionFeasibilityModel(astrocalc, xrm, cam, site, tel, ism);

		// Check all groups from cache model.
		SynopticModelProvider smp = (SynopticModelProvider) Naming.lookup("SynopticModelProvider");
		logger.create().extractCallInfo().level(2).msg("Located external synoptic model").send();

		Phase2CompositeModel p2g = smp.getPhase2CompositeModel();
		logger.create().extractCallInfo().level(2).msg("Located external phase2 composite model").send();

		AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
		logger.create().extractCallInfo().level(2).msg("Located external proposal account synopsis model").send();

		hsm = smp.getHistorySynopsisModel();
		logger.create().extractCallInfo().level(2).msg("Located external history synopsis model").send();

		// Env snapshot
		EnvironmentSnapshot env = new EnvironmentSnapshot(time, 0.3,
				EnvironmentSnapshot.EXTINCTION_PHOTOM);

		// Disruptor list
		List<Disruptor> disruptors = new Vector<Disruptor>();

		long t = start;
		GroupNodeDescriptor rootDesc = new GroupNodeDescriptor(null);
		rootDesc.startTime = start;
		rootDesc.execTime = BGEXEC;
		rootDesc.bgstd = true;
		rootDesc.hist = new Vector<ExecutionHistorySynopsis>();

		// root node - no content
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootDesc);

		processNode(root);

		return root;
	}

	private void processNode(DefaultMutableTreeNode node) {

		// we have a node, see if we can generate any subnodes

		GroupNodeDescriptor gnd = (GroupNodeDescriptor) node.getUserObject();

		CachedHistorySynopsisModel csm = new CachedHistorySynopsisModel(hsm);
		// TODO csm.updateHistories(gnd.hist);

		int nsub = 0;

		// 4 cases to handle ?
		if (gnd.bgstd || gnd.std) {

			// try create upto 4 subnodes - these will have been added already
			// some or all may be BG nodes
			createBranchNode(node, EnvironmentSnapshot.SEEING_EXCELLENT, csm);
			createBranchNode(node, EnvironmentSnapshot.SEEING_AVERAGE, csm);
			createBranchNode(node, EnvironmentSnapshot.SEEING_POOR, csm);
			createBranchNode(node, EnvironmentSnapshot.SEEING_USABLE, csm);

		} else {

			// this was an ord science node - conditions cannot have changed
			// try create 1 subnode with same conditions
			createBranchNode(node, gnd.env, csm);

		}

	}

	private GroupItem selectGroup(long time, int env, ExecutionHistorySynopsisModel hsm) {
		return null;
	}

	private void createBranchNode(DefaultMutableTreeNode node, int env, ExecutionHistorySynopsisModel hsm) {

		GroupNodeDescriptor nd = (GroupNodeDescriptor) node.getUserObject();
		long time = nd.startTime + nd.execTime;

		// we are past the end of the period of interest - recursor terminating
		// condition
		if (time > end)
			return;

		GroupItem gx = selectGroup(time, env, hsm);
		if (gx == null) {
			// a background group

			GroupNodeDescriptor gnx = new GroupNodeDescriptor(null);
			gnx.startTime = time;
			gnx.env = env;
			gnx.execTime = BGEXEC; // todo add bgexec to this group
			gnx.hist = null; // TODO want copy of hist plus this group at t
			//ExecutionHistorySynopsis xthis = new ExecutionHistorySynopsis();
			//xthis.s
			//gnx.hist = copyHistory(nd.hist, )
			gnx.bgstd = true;

			DefaultMutableTreeNode subnode = new DefaultMutableTreeNode(gnx);

			node.add(subnode);
			processNode(subnode);

		} else {
			// real science group

			GroupNodeDescriptor gnx = new GroupNodeDescriptor(gx);
			gnx.startTime = time;
			gnx.env = env;
			gnx.execTime = 300000 + (long) (30 * 60 * 1000 * Math.random()); // TODO
			// use
			// exec
			// model
			gnx.hist = null; // TODO want copy of hist plus this group at t

			if (checkStd(gx))
				gnx.std = true;

			DefaultMutableTreeNode subnode = new DefaultMutableTreeNode(gnx);

			node.add(subnode);
			processNode(subnode);
		}

	}

	private boolean checkStd(GroupItem group) {

		ISequenceComponent seq = group.getSequence();
		if (seq == null)
			return false;

		return (Math.random() > 0.9);
		// return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {

			Logger alogger = LogManager.getLogger("GSS");
			alogger.setLogLevel(3);
			ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
			console.setLogLevel(3);
			alogger.addExtendedHandler(console);

			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

			// Is it day or night at this time at this location ?

			long time = System.currentTimeMillis();

			double lat = Math.toRadians(cfg.getDoubleValue("lat"));
			double lon = Math.toRadians(cfg.getDoubleValue("long"));
			ISite site = new BasicSite("test-site", lat, lon);

			LookaheadTreeScan ls = new LookaheadTreeScan();
			ls.lookaheadTreeScan(site, time);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
