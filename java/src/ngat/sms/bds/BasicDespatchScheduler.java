/**
 * 
 */
package ngat.sms.bds;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.BasicTargetCalculator;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SkyBrightnessCalculator;
import ngat.astrometry.SolarCalculator;
import ngat.astrometry.TargetTrackCalculator;
import ngat.ems.SkyModel;
import ngat.oss.transport.RemotelyPingable;
import ngat.phase2.IHistoryItem;
import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITarget;
import ngat.phase2.ITimePeriod;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.phase2.XPhotometricityConstraint;
import ngat.phase2.XSeeingConstraint;
import ngat.phase2.XSkyBrightnessConstraint;
import ngat.phase2.XTimePeriod;
import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.AsynchronousScheduleResponseHandler;
import ngat.sms.AsynchronousScheduler;
import ngat.sms.BasicAccountSynopsisModel;
import ngat.sms.BasicHistorySynopsisModel;
import ngat.sms.BasicVetoManager;
import ngat.sms.CandidateFeasibilitySummary;
import ngat.sms.ComponentSet;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionFeasibilityModel;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionUpdateManager;
import ngat.sms.ExecutionUpdater;
import ngat.sms.FixedGroupDisruptor;
import ngat.sms.GroupItem;
import ngat.sms.ObservingConstraintAdapter;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.ScheduleDespatcher;
import ngat.sms.ScheduleItem;
import ngat.sms.SchedulingStatusProvider;
import ngat.sms.SchedulingStatusUpdateListener;
import ngat.sms.ScoreMetric;
import ngat.sms.ScoreMetricsSet;
import ngat.sms.SkyModelProvider;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TimeModel;
import ngat.sms.TimingConstraintWindowCalculator;
import ngat.sms.Veto;
import ngat.sms.VetoManager;
import ngat.sms.util.BasicTimingConstraintWindowCalculator;
import ngat.sms.util.RemainingNightsUtilityCalculator;
import ngat.util.BooleanLock;
import ngat.util.PersistentUniqueInteger;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class BasicDespatchScheduler extends UnicastRemoteObject implements
		ScheduleDespatcher, AsynchronousScheduler, SchedulingStatusProvider,
		RemotelyPingable {

	public static final String PRIMARY_QUEUE_ID = "Primary";
	public static final String BACKGROUND_QUEUE_ID = "Background";
	public static final String FIXED_QUEUE_ID = "Fixed";

	/** Elevation angle for Zenith. */
	public static final double ELEVATION_ZENITH = Math.PI / 2.0;

	/** Azimuth angle for direction South. */
	public static final double AZIMUTH_SOUTH = 0.0;

	/** RED Band wavelength nm. */
	public static final double RED_BAND = 700.0;

	TimeModel timeModel;
	// Phase2CompositeModel gphase2;
	ExecutionUpdateManager xm;
	ExecutionFeasibilityModel feasible;

	// ExecutionHistorySynopsisModel hsm;

	SynopticModelProvider smp;

	SkyModelProvider skyp;

	// AccountSynopsisModel asm;

	ISite site;

	int sweep = 0;
	long xid = 1;

	/** Records all time sweep number. */
	PersistentUniqueInteger suid;

	LogGenerator logger;

	List monitors;
	private BooleanLock monitorLock;

	static List<String> QUEUE_NAMES;

	/** Stores the id and earliest rerun timelimit for vetoed groups. */
	// Map<Long, Veto> vetoMap;

	private VetoManager vetoMgr;

	/**
	 * @param gphase2
	 * @param xm
	 * @param feasible
	 * @param histModel
	 * @param hsm
	 * @param accModel
	 * @param asm
	 */
	public BasicDespatchScheduler(TimeModel timeModel,
			ExecutionUpdateManager xm, ExecutionFeasibilityModel feasible,
			SynopticModelProvider smp, SkyModelProvider skyp, ISite site)
			throws RemoteException {
		super();
		this.timeModel = timeModel;
		// this.gphase2 = gphase2;
		this.xm = xm;
		this.feasible = feasible;
		this.smp = smp;
		this.skyp = skyp;
		this.site = site;
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS")
				.subSystem("SchedulingStatusProvider")
				.srcCompClass(this.getClass().getSimpleName()).srcCompId("tsd");

		// vetoMap = new HashMap<Long, Veto>();

		vetoMgr = new BasicVetoManager();

		monitors = new Vector();
		QUEUE_NAMES = new Vector<String>();
		QUEUE_NAMES.add(PRIMARY_QUEUE_ID);
		QUEUE_NAMES.add(BACKGROUND_QUEUE_ID);
		QUEUE_NAMES.add(FIXED_QUEUE_ID);

		monitorLock = new BooleanLock();

		try {
			suid = new PersistentUniqueInteger("/occ/tmp/%%sweep");
			sweep = suid.get();
			logger.create()
					.extractCallInfo()
					.info()
					.level(1)
					.msg("Schedule Despatcher ready, starting with sweep: "
							+ suid.get()).send();
		} catch (Exception e) {
			e.printStackTrace();
			logger.create()
					.extractCallInfo()
					.info()
					.level(1)
					.msg("Schedule Despatcher ready, using default initial sweep number: 0")
					.send();
		}

	}

	public ScheduleItem nextScheduledJob() throws RemoteException {
		// TODO - this i/f is still a bit messy, need to create it then return
		// as 2 actions...

		long time = timeModel.getTime();

		try {
			sweep = suid.increment();
		} catch (Exception e) {
			sweep++;
		}

		logger.create()
				.block("nextScheduledJob")
				.info()
				.level(1)
				.msg("Schedule sweep [" + sweep
						+ "] requested for model time: " + new Date(time))
				.send();

		logger.create().block("nextScheduledJob").info().level(1)
				.msg("START_SWEEP " + sweep).send();

		// TELEM BDS: <sweep> START_SWEEP

		// lockout any monitors trying to register while we are running...
		try {
			monitorLock.waitToSetTrue(1000);
		} catch (InterruptedException ix) {
		}
		monitorLock.setValue(true);

		notifyListenersScheduleSweepStarted(time, sweep);

		// locate synoptic models
		Phase2CompositeModel gphase2 = smp.getPhase2CompositeModel();
		AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
		ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();

		// Some astro information
		try {
			AstrometryCalculator astro = new BasicAstrometryCalculator();

			SolarCalculator sun = new SolarCalculator();
			// use rise horizon -2.0 degrees
			double horizon = Math.toRadians(-2.0);

			// check the suns not already up...
			double sunelev = astro.getAltitude(sun.getCoordinates(time), site,
					time);
			long ttsunrise = (long) astro.getTimeUntilNextRise(
					sun.getCoordinates(time), site, horizon, time);
			long tssunset = (long) astro.getTimeSinceLastSet(
					sun.getCoordinates(time), site, horizon, time);

			// ALT ttsunset = siteCalculator.getTimeSinceLastSet(sun, horizon,
			// time);

			double nf = (double) tssunset / (double) (tssunset + ttsunrise);
			logger.create()
					.block("nextScheduledJob")
					.info()
					.level(1)
					.msg("Sunset was: " + (tssunset / 1000)
							+ "s ago, Sunrise in: " + (ttsunrise / 1000)
							+ "s, NF=" + nf).send();

		} catch (Exception ax) {
			ax.printStackTrace();
		}

		long tstart = System.currentTimeMillis();

		// TODO first we need to generate a list of Fixed time groups and
		// any other disruptors...
		// TODO Get a list of disruptors from the DisruptorModel or
		// TimeReservationModel (provided by RCS)
		// List<Disruptors> disruptors = timeResModel.getReservedSlots() (a 1.4
		// model)
		// TODO actually want to add thse to the list we got from the RCS

		// List<Disruptors> fgList = lokaheadfixedgroups()
		// disruptors.add(fglist);
		List<Disruptor> disruptors = new Vector<Disruptor>();
		try {
			disruptors = lookAheadFixedGroupDisruptors(gphase2, pasm, hsm, time);
		} catch (Exception e) {
			// TODO log this and keep going
			logger.create().block("nextScheduledJob").error().level(2)
					.msg("Unable to load fixed group list: " + e).send();
			// throw new
			// RemoteException(this.getClass().getName()+":nextJob():"+e);
		}

		// TODO Assumed perfect conditions
		// EnvironmentSnapshot env = new EnvironmentSnapshot(time,
		// EnvironmentSnapshot.SEEING_EXCELLENT,
		// EnvironmentSnapshot.EXTINCTION_PHOTOM);

		// TODO EnvironmentSnapshot ...we really need an EPM at this point
		// - is it part of EMS or SMS ?
		// env = emp.predictConditions(time);

		// current seeing at zenith in red band.
		int seeCat = EnvironmentSnapshot.SEEING_BAD;
		int extCat = EnvironmentSnapshot.EXTINCTION_SPECTRO;
		double seeing = 5.0; // make this big so it looks BAD !
		try {
			SkyModel skyModel = skyp.getSkyModel();
			seeing = skyModel.getSeeing(RED_BAND, ELEVATION_ZENITH,
					AZIMUTH_SOUTH, time);
			seeCat = EnvironmentSnapshot.getSeeingCategory(seeing);

			double extinction = skyModel.getExtinction(RED_BAND,
					ELEVATION_ZENITH, AZIMUTH_SOUTH, time);

			extCat = EnvironmentSnapshot.getExtinctionCategory(extinction);
			logger.create()
					.block("nextScheduledJob")
					.info()
					.level(2)
					.msg("Current sky conditions from remote SkyModel: Seeing="
							+ seeing
							+ "("
							+ EnvironmentSnapshot.getSeeingCategoryName(seeCat)
							+ ")"
							+ " Extinction="
							+ EnvironmentSnapshot
									.getExtinctionCategoryName(extCat) + "("
							+ extCat + ")").send();

			logger.create()
					.block("nextScheduledJob")
					.info()
					.level(1)
					.msg("SKY "
							+ sweep
							+ " "
							+ seeing
							+ " "
							+ EnvironmentSnapshot
									.getExtinctionCategoryName(extCat)).send();

			// TELEM BDS: <sweep> SKY <seeing> <ext>

		} catch (Exception e) {
			e.printStackTrace();
			logger.create()
					.block("nextScheduledJob")
					.error()
					.level(4)
					.msg("Unable to obtain remote SkyModel data: Assuming BAD and NON_PHOTOM")
					.send();
		}
		EnvironmentSnapshot env = new EnvironmentSnapshot(time, seeing, extCat);

		// check feasibility and generate candidate list(s)
		List primaryCandidates = new Vector();
		List fixedGroupCandidates = new Vector();
		List backgroundCandidates = new Vector();

		// notifyListenersCandidateQueueCleared(PRIMARY_QUEUE_ID);
		// notifyListenersCandidateQueueCleared(BACKGROUND_QUEUE_ID);
		// notifyListenersCandidateQueueCleared(FIXED_QUEUE_ID);

		int ng = 0;
		int nc = 0;
		// TODO p2g = synopticModelProvider.getAggPhase2Model()
		// TODO asm = synopticModelProvider.getAccountSynopsisModel();
		// TODO hsm = synopticModelProvider.getHistorySynopsisModel();

		List<GroupItem> lgroups = gphase2.listGroups();
		Iterator<GroupItem> groups = lgroups.iterator();
		while (groups.hasNext()) {
			GroupItem group = groups.next();
			ng++;

			logger.create().block("nextScheduledJob").info().level(2)
					.msg("Checking group: " + group.getName()).send();

			ExecutionHistorySynopsis hist = null;
			try {
				hist = hsm.getExecutionHistorySynopsis(group.getID(), time);
			} catch (Exception e) {
				logger.create()
						.block("nextScheduledJob")
						.error()
						.level(3)
						.msg("Group: " + group.getName()
								+ " error accessing execution history: " + e)
						.send();
				continue;
			}
			AccountSynopsis accounts = null;
			try {
				accounts = pasm.getAccountSynopsis(group.getProposal().getID(),
						time);
			} catch (Exception e) {
				logger.create()
						.block("nextScheduledJob")
						.error()
						.level(3)
						.msg("Group: " + group.getName()
								+ " error accessing accounts: " + e).send();
				continue;
			}

			// if the veto time is in the future, reject this one
			// long vetoTime = vetoedUntil(group.getID());
			long vetoTime = vetoMgr.getVetoTime(group.getID());
			if (vetoTime > time) {
				logger.create()
						.block("nextScheduledJob")
						.info()
						.level(2)
						.msg("Group: " + group.getName() + " is vetoed until: "
								+ (new Date(vetoTime))).send();
				continue;
			}

			CandidateFeasibilitySummary cfs = feasible.isitFeasible(group,
					time, hist, accounts, env, disruptors);
			if (cfs.isFeasible()) {
				nc++;
				// TODO decide which queue to add this candidate to
				String candListName = "";
				int ncs = 0;
				if (group.getTimingConstraint() instanceof XFixedTimingConstraint) {
					fixedGroupCandidates.add(group);
					ncs = fixedGroupCandidates.size();
					candListName = "fixed-groups";
				} else if (group.getProposal().getPriority() == IProposal.PRIORITY_Z) {
					backgroundCandidates.add(group);
					ncs = backgroundCandidates.size();
					candListName = "background";
				} else {
					primaryCandidates.add(group);
					ncs = primaryCandidates.size();
					candListName = "primary";
				}
				// System.err.println("TSD::Candidate: " + group.getName());
				logger.create()
						.block("nextScheduledJob")
						.info()
						.level(3)
						.msg("Found (" + candListName + ") candidate " + ncs
								+ " :" + group.getName()).send();

				// TELEM BDS: <sweep> CAND <ncs> <bg/prim/fx> <gname>

				// If the group is a fixed one then its won
				// TODO we may have several FGs which overlap start times, in
				// this case
				// we will need to work out permutations and count max no which
				// can be executed

			} else {
				// TODO group has been rejected how do we get that info....
				notifyListenersCandidateRejected(group,
						cfs.getRejectionReason());
			}

		}
		// TODO we may have several FGs which overlap start times, in this case
		// we will need to work out permutations and count max no which can be
		// executed

		GroupItem selectedGroup = null;
		GroupItem bestFixed = null;
		GroupItem bestPrimary = null;
		GroupItem bestBackground = null;

		if (!fixedGroupCandidates.isEmpty()) {

			logger.create().block("nextScheduledJob").info().level(3)
					.msg("Checking fixed-group candidates...").send();
			bestFixed = checkCandidateList(FIXED_QUEUE_ID,
					fixedGroupCandidates, time);

		}

		if (!primaryCandidates.isEmpty()) {

			logger.create().block("nextScheduledJob").info().level(3)
					.msg("Checking primary candidates...").send();
			bestPrimary = checkCandidateList(PRIMARY_QUEUE_ID,
					primaryCandidates, time);
			logger.create()
					.block("nextScheduledJob")
					.info()
					.level(3)
					.msg("Checked " + ng + " groups, found "
							+ primaryCandidates.size() + " primary candidates")
					.send();

		}

		if (!backgroundCandidates.isEmpty()) {

			logger.create().block("nextScheduledJob").info().level(3)
					.msg("Checking background candidates...").send();
			bestBackground = checkCandidateList(BACKGROUND_QUEUE_ID,
					backgroundCandidates, time);
			logger.create()
					.block("nextScheduledJob")
					.info()
					.level(3)
					.msg("Checked " + ng + " groups, found "
							+ backgroundCandidates.size()
							+ " background candidates").send();

		}

		if (!fixedGroupCandidates.isEmpty())
			selectedGroup = bestFixed;
		else if (!primaryCandidates.isEmpty())
			selectedGroup = bestPrimary;
		else
			selectedGroup = bestBackground;

		// at this stage we have either: A Fixed Group, a Primary Group, a
		// Background Group or nothing

		long tend = System.currentTimeMillis();
		logger.create().block("nextScheduledJob").info().level(3)
				.msg("Schedule generated in: " + (tend - tstart) + "ms").send();

		// no groups found
		if (selectedGroup == null) {
			notifyListenersCandidateSelected(time, null);
			monitorLock.setValue(false);
			return null;
		}

		// TODO IMPORTANT Maybe Exception OR TSI is null ?

		// create the initial history entry and set in GI
		long hid = -999;
		try {
			hid = hsm.addHistoryItem(selectedGroup.getID());

		} catch (Exception hx) {
			hx.printStackTrace();
			// use a faked entry
			logger.create().block("nextScheduledJob").info().level(3)
					.msg("Unable to access history model, using fake entry")
					.send();
		}
		selectedGroup.setHId(hid);

		logger.create().block("nextScheduledJob").info().level(1)
				.msg("" + sweep + " SELECTED " + selectedGroup.getName())
				.send();

		// Create a new ExecutionUpdater to handle the reply
		ExecutionUpdater xu = xm.getExecutionUpdater(selectedGroup.getID());

		TestScheduleItem item = new TestScheduleItem(selectedGroup, xu);
		double maxScore = -999.9;
		try {
			ScoreMetricsSet ss = scoreGroup(selectedGroup, time);
			maxScore = ss.getScore();
		} catch (Exception e) {
			e.printStackTrace();
		}
		item.setScore(maxScore);
		notifyListenersCandidateSelected(time, item);
		monitorLock.setValue(false);
		return item;

	}

	private GroupItem checkCandidateList(String listId, List candidateList,
			long time) {

		double maxScore = -999.99;
		GroupItem selectedGroup = null;
		int ig = 0;
		Iterator cand = candidateList.iterator();
		while (cand.hasNext()) {
			GroupItem group = (GroupItem) cand.next();

			// TODO switch in the scoring model here
			double score = -99999.99;
			try {
				ScoreMetricsSet ss = scoreGroup(group, time);
				score = ss.getScore();
				if (score > maxScore) {
					selectedGroup = group;
					maxScore = score;
				}

				String groupName = group.getName();
				IProposal proposal = group.getProposal();
				String propName = (proposal != null ? proposal.getName()
						: "NULL");

				logger.create()
						.block("nextScheduledJob")
						.info()
						.level(1)
						.msg("" + sweep + " CANDIDATE " + listId + " "
								+ propName + "/" + groupName + " " + score)
						.send();

				notifyListenersCandidateAdded(listId, group, ss, score, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			ig++;
		}
		return selectedGroup;
	}

	/** Generate a list of fixed groups in the future of now as disruptors... */
	private List<Disruptor> lookAheadFixedGroupDisruptors(
			Phase2CompositeModel gphase2, AccountSynopsisModel pasm,
			ExecutionHistorySynopsisModel hsm, long time) throws Exception {

		logger.create().block("lookAheadFixedGroupDisruptors").info().level(3)
				.msg("Checking ahead for disruptors...").send();

		List<Disruptor> disruptorList = new Vector<Disruptor>();

		int nd = 0;
		int ng = 0;
		int nf = 0;
		List<GroupItem> lgroups = gphase2.listGroups();
		Iterator<GroupItem> groups = lgroups.iterator();
		while (groups.hasNext()) {
			ng++;
			GroupItem group = groups.next();
			ITimingConstraint timing = group.getTimingConstraint();
			if (timing instanceof XFixedTimingConstraint) {
				nf++;
				XFixedTimingConstraint fixed = (XFixedTimingConstraint) timing;
				// the user is now responsible for setting up a sufficient
				// buffer size
				long start = fixed.getStartTime();// - fixedGroupPreStartBuffer;
				long end = fixed.getEndTime(); // + fixedGroupPostStartBuffer;
				logger.create()
						.block("lookAheadFixedGroupDisruptors")
						.info()
						.level(2)
						.msg("Checking potential disruptor: " + group.getName()
								+ " which starts in: "
								+ ((start - time) / 60000) + "min at "
								+ (new Date(start))).send();

				// ExecutionHistorySynopsis fghist =
				// hsm.getExecutionHistorySynopsis(group.getID(), time);
				// if (fghist.getCountExecutions() != 0) {
				// logger.create().block("lookAheadFixedGroupDisruptors").info().level(3)
				// .msg("Fixed group has already been executed").send();
				// } else {

				// if it starts in the next 24 hours (should be now till
				// sunrise... note there could already be an FG which is
				// in-window but this will get selected anyway
				// so we can ignore here..
				if ((start > time) && (start < time + 24 * 3600 * 1000L)) {
					Disruptor d = new Disruptor(group.getName(), "FIXED_GROUP",
							new XTimePeriod(start, end));

					// Do feasibility test on the FG at the time when
					// it is meant to be executed - we have to change the xfm to
					// use a
					// optimistic inst synopsis model

					long fgtime = (start + end) / 2; // use midpoint

					int photom = EnvironmentSnapshot.EXTINCTION_PHOTOM;
					try {
						ObservingConstraintAdapter oca = new ObservingConstraintAdapter(
								group);
						XPhotometricityConstraint photConstr = oca
								.getPhotometricityConstraint();

						if (photConstr != null) {

							int photomActual = photConstr
									.getPhotometricityCategory();
							if (photomActual == XPhotometricityConstraint.NON_PHOTOMETRIC) {
								photom = EnvironmentSnapshot.EXTINCTION_SPECTRO;
							} else if (photomActual == XPhotometricityConstraint.PHOTOMETRIC) {
								photom = EnvironmentSnapshot.EXTINCTION_PHOTOM;
							}

						}

					} catch (Exception e) {
						logger.create()
								.block("lookAheadFixedGroupDisruptors")
								.error()
								.level(3)
								.msg("Group: " + group.getName()
										+ " error accessing constraints: " + e)
								.send();
						continue;
					}

					EnvironmentSnapshot optenv = new EnvironmentSnapshot(
							fgtime, 0.25, photom);

					ExecutionHistorySynopsis fghist = hsm
							.getExecutionHistorySynopsis(group.getID(), time);

					AccountSynopsis fgaccounts = null;
					try {
						fgaccounts = pasm.getAccountSynopsis(group
								.getProposal().getID(), time);
					} catch (Exception e) {
						logger.create()
								.block("lookAheadFixedGroupDisruptors")
								.error()
								.level(3)
								.msg("Group: " + group.getName()
										+ " error accessing accounts: " + e)
								.send();
						continue;
					}

					List<Disruptor> nodisruptors = new Vector<Disruptor>();

					CandidateFeasibilitySummary cfs = feasible.isitFeasible(
							group, fgtime, fghist, fgaccounts, optenv,
							nodisruptors);
					if (cfs.isFeasible()) {

						disruptorList.add(d);
						nd++;
						logger.create()
								.block("lookAheadFixedGroupDisruptors")
								.info()
								.level(2)
								.msg("Found new disruptor [" + nd + "]: "
										+ d.getDisruptorClass() + ":"
										+ d.getDisruptorName() + "["
										+ (d.getPeriod().getStart() - time)
										/ 60000 + ","
										+ (d.getPeriod().getEnd() - time)
										/ 60000 + "]").send();
					} else {
						logger.create()
								.block("lookAheadFixedGroupDisruptors")
								.info()
								.level(2)
								.msg("Fixed group: " + group.getName()
										+ " cannot run at: "
										+ (new Date(fgtime))).send();
					}
				} // if start

			} // if fxied

		} // next group

		logger.create()
				.block("lookAheadFixedGroupDisruptors")
				.info()
				.level(3)
				.msg("Checked " + ng + " groups (" + nf + " fixed), found "
						+ nd + " disruptors").send();

		// now check there are no conflicts - we cant do much at this stage but
		// we can warn operators
		int dls = disruptorList.size();

		// dls ==0 OR dls == 1 there can be no conflict

		// ragged (diagonal) search....
		// LEAVE THIS FOR NOW WE DONT HAVE ACCESS TO an ExecResourceModel
		// here....
		/*
		 * if (dls > 1) {
		 * 
		 * // check upto last-but-one entry for (int id = 0; id <
		 * disruptorList.size() - 1; id++) {
		 * 
		 * Disruptor d1 = disruptorList.get(id); if (d1 instanceof
		 * FixedGroupDisruptor) { GroupItem g1 = ((FixedGroupDisruptor)
		 * d1).getGroup();
		 * 
		 * // the lock-out period for a fixed group is:- // fixed_period_start
		 * TO fixed_period_end + exec_time
		 * 
		 * ITimePeriod tp1 = new XTimePeriod(d1.getPeriod().getStart(),
		 * d1.getPeriod().getEnd()+x1);
		 * 
		 * // compare to all including last entry for (int jd = id + 1; jd <
		 * disruptorList.size(); jd++) { Disruptor d2 = disruptorList.get(id);
		 * if (d2 instanceof FixedGroupDisruptor) { GroupItem g2 =
		 * ((FixedGroupDisruptor) d2).getGroup(); }
		 * 
		 * // compare g1 and g2 for overlaps - ITimePeriod tp2 = new
		 * XTimePeriod(d2.getPeriod().getStart(), d2.getPeriod().getEnd()+x2);
		 * 
		 * 
		 * long overlapStart = Math.max(tp1.getStart(), execPeriod.getStart());
		 * long overlapEnd = Math.min(disruption.getEnd(), execPeriod.getEnd());
		 * 
		 * // see if our group (exec.start, exec.end) overlaps (d.start, d.end)
		 * // ie if e starts before eod AND e ends after sod
		 * 
		 * 
		 * 
		 * if (overlapStart < overlapEnd) { logger.create() .extractCallInfo()
		 * .error() .level(3) .msg("Group exec window t=[0, +" + ((long)
		 * execTime / 60000) + "]m overlaps with disruptor: " +
		 * disruptor.getDisruptorClass() + ":" + disruptor.getDisruptorName() +
		 * " exclusion window t=[+" + (disruptor.getPeriod().getStart() - time)
		 * / 60000 + ", +" + (disruptor.getPeriod().getEnd() - time) / 60000 +
		 * "]m").send();
		 * 
		 * 
		 * } // next D2
		 * 
		 * }
		 * 
		 * } // next D1
		 * 
		 * }
		 */

		return disruptorList; // may be empty
	}

	/** TEMP method to calculate group score. */
	private ScoreMetricsSet scoreGroup(GroupItem group, long time)
			throws Exception {

		ScoreMetricsSet ss = new ScoreMetricsSet();

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
		if (timing instanceof XMonitorTimingConstraint
				|| timing instanceof XMinimumIntervalTimingConstraint) {
			pscore += 1.0;
		}

		pscore /= 7.0;

		ss.addMetric(new ScoreMetric("PRIORITY", pscore, ""));

		// now add some seeing matching stuff..
		double smscore = 0.0;
		ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);
		double maxSeeing = oca.getSeeingConstraint().getSeeingValue();

		SkyModel skyModel = skyp.getSkyModel();
		double predictedSeeing = skyModel.getSeeing(RED_BAND, ELEVATION_ZENITH,
				AZIMUTH_SOUTH, time);

		// TODO may want to change that constant ?

		// what if we have no seeing yet?
		if (Double.isNaN(predictedSeeing))
			smscore = 0.0;
		else
			smscore = 1.0 / (1.0 + maxSeeing - predictedSeeing);

		ss.addMetric(new ScoreMetric("ENV", smscore, ""));

		// TODO sky matching - WAIT TO UNCOMMENT NOT CHECKED FULLY YET
		// double skyscore = 0.0;
		// TestResourceUsageEstimator tru = new TestResourceUsageEstimator();
		// ExecutionResource ertime =
		// tru.getEstimatedResourceUsage(group).getResource("TIME");
		// long execTime = (long)ertime.getResourceUsage();
		// int skyWant =
		// oca.getSkyBrightnessConstraint().getSkyBrightnessCategory();
		// int skyActual = calculateSkyBrightness(group, time, (long)execTime);
		// // 0.0 dark to 50.0 day
		// skyscore = 1.0/(1.0 + skyWant - skyActual);
		// ss.addMetric(new ScoreMetric("SKYB", skyscore, ""));

		// elevation of target re max in night
		double escore = 0.0;
		try {
			escore = calculateElevationScore(group, time);
		} catch (Exception e) {
			e.printStackTrace();
			// escore will be zero now
		}
		ss.addMetric(new ScoreMetric("TRANS", escore, ""));

		// RN utility
		// TestResourceUsageEstimator tru2 = new TestResourceUsageEstimator();
		// BasicTimingConstraintWindowCalculator tcwc = new
		// BasicTimingConstraintWindowCalculator(tru2, feasible, site, 1000L);
		// RemainingNightsUtilityCalculator rnuc = new
		// RemainingNightsUtilityCalculator(tcwc);
		// EnvironmentSnapshot env = ...env get stuff from skymodel to make
		// snapshot
		// get history info from ....?
		// rnuc.getUtility(group, execTime, env, hist);

		double random = Math.random();

		double score = 0.5 * escore + 0.5 * pscore + 0.15 * smscore + 0.05
				* random;
		ss.setScore(score);

		String path = (group.getTag() != null ? group.getTag().getName()
				: "UNK_TAG")
				+ "/"
				+ (group.getUser() != null ? group.getUser().getName()
						: "UNK_USR")
				+ "/"
				+ (group.getProposal() != null ? group.getProposal().getName()
						: "UNK_PRP");

		String result = String
				.format("Scoring group: [%35.35s..] [%20.20s..] %2.2f %2.2f %2.2f %2.2f -> %2.4f ",
						path, group.getName(), escore, pscore, smscore, random,
						score);

		// ... Scoring group: [JMU/Bloggs.Fred/JL09B007..] [RS_oph_big_bonus..]
		// 0.3 0.25 0.54 0.02 -> 1.2453

		logger.create().block("nextScheduledJob").info().level(3).msg(result)
				.send();

		return ss;

	}

	private int calculateSkyBrightness(GroupItem group, long time, long execTime)
			throws Exception {

		ISequenceComponent seq = group.getSequence();
		ComponentSet cs = new ComponentSet(seq);

		// no targets to check so dark sky ?? weirdness !!
		if (cs.countTargets() == 0)
			return XSkyBrightnessConstraint.DARK;

		AstrometryCalculator astro = new BasicAstrometryCalculator();

		SolarCalculator sun = new SolarCalculator();
		long sunrise = 0L;

		// use rise horizon -2.0 degrees
		double horizon = Math.toRadians(-2.0);
		// check the suns not already up...
		double sunelev = astro
				.getAltitude(sun.getCoordinates(time), site, time);
		if (sunelev > horizon)
			sunrise = time; // bizarre but let it through
		else
			sunrise = time
					+ (long) astro.getTimeUntilNextRise(
							sun.getCoordinates(time), site, horizon, time);

		long lastSunset = time
				- (long) astro.getTimeSinceLastSet(sun.getCoordinates(time),
						site, horizon, time);

		// note the very worst sky brightness of all targets over the entire
		// group
		int veryWorstSky = XSkyBrightnessConstraint.DARK;

		// loop over targets, calculate worst value of score
		Iterator<ITarget> targets = cs.listTargets();
		while (targets.hasNext()) {

			ITarget target = targets.next();
			TargetTrackCalculator track = new BasicTargetCalculator(target,
					site);

			// coordinates and elevation at time - this is probably the best we
			// can do ??
			// unless we recalculate thro the duration...
			Coordinates c = track.getCoordinates(time);
			double elev = astro.getAltitude(c, site, time);

			SkyBrightnessCalculator skyb = new SkyBrightnessCalculator(site);

			// work out sky now, half way and end of obs (values are 0 dark to
			// 50 day)
			int skyTargetNow = skyb.getSkyBrightnessCriterion(track, time);
			// double skynow =
			// SkyBrightnessCalculator.getSkyBrightness(skyTargetNow);

			int skyTargetEnd = skyb.getSkyBrightnessCriterion(track, time
					+ execTime);
			// double skyend =
			// SkyBrightnessCalculator.getSkyBrightness(skyTargetEnd);

			int skyTargetMid = skyb.getSkyBrightnessCriterion(track, time
					+ execTime / 2);
			// double skymid =
			// SkyBrightnessCalculator.getSkyBrightness(skyTargetMid);

			// pick the worst case for this target
			int worstSkyTarget = Math.max(skyTargetNow, skyTargetMid);
			worstSkyTarget = Math.max(skyTargetEnd, worstSkyTarget);

			if (worstSkyTarget > veryWorstSky)
				veryWorstSky = worstSkyTarget;

		}

		return veryWorstSky;

	}

	private double calculateElevationScore(GroupItem group, long time)
			throws Exception {

		ISequenceComponent seq = group.getSequence();
		ComponentSet cs = new ComponentSet(seq);

		// no targets to check
		if (cs.countTargets() == 0)
			return 0.0;

		AstrometryCalculator astro = new BasicAstrometryCalculator();

		SolarCalculator sun = new SolarCalculator();
		long sunrise = 0L;

		// use rise horizon -2.0 degrees
		double horizon = Math.toRadians(-2.0);
		// check the suns not already up...
		double sunelev = astro
				.getAltitude(sun.getCoordinates(time), site, time);
		if (sunelev > horizon)
			sunrise = time; // bizarre but let it through
		else
			sunrise = time
					+ (long) astro.getTimeUntilNextRise(
							sun.getCoordinates(time), site, horizon, time);

		long lastSunset = time
				- (long) astro.getTimeSinceLastSet(sun.getCoordinates(time),
						site, horizon, time);

		// getTransitAltitude(Coordinates c, ISite site, long time);

		double escore = 0.0;

		// loop over targets, calculate averge value of score
		Iterator<ITarget> targets = cs.listTargets();
		while (targets.hasNext()) {

			ITarget target = targets.next();
			TargetTrackCalculator track = new BasicTargetCalculator(target,
					site);

			// coordinates and elevation at time
			Coordinates c = track.getCoordinates(time);
			double elev = astro.getAltitude(c, site, time);

			// highest elevation in night ahead until sunrise@-2 OR do we want
			// highest since sunset also ?
			double maxelev = astro.getMaximumAltitude(track, site, lastSunset,
					sunrise);

			double tscore = elev / maxelev;
			if (Double.isNaN(tscore) || Double.isInfinite(tscore)) {
				System.err.println("Elevation score for: " + group.getName()
						+ "/" + target.getName() + " " + tscore);
				tscore = 0.0;
			}

			escore += tscore;

		}

		return escore / cs.countTargets();

	}

	/*
	 * private double calculateSkyBScore(GroupItem group, long time) throws
	 * Exception {
	 * 
	 * ISequenceComponent seq = group.getSequence(); ComponentSet cs = new
	 * ComponentSet(seq);
	 * 
	 * // no targets to check if (cs.countTargets() == 0) return 0.0;
	 * 
	 * AstrometryCalculator astro = new BasicAstrometryCalculator();
	 * 
	 * SolarCalculator sun = new SolarCalculator(); long sunrise = 0L;
	 * 
	 * // use rise horizon -2.0 degrees double horizon = Math.toRadians(-2.0);
	 * // check the suns not already up... double sunelev =
	 * astro.getAltitude(sun.getCoordinates(time), site, time); if (sunelev >
	 * horizon) sunrise = time; // bizarre but let it through else sunrise =
	 * time + (long) astro.getTimeUntilNextRise(sun.getCoordinates(time), site,
	 * horizon, time);
	 * 
	 * long lastSunset = time - (long)
	 * astro.getTimeSinceLastSet(sun.getCoordinates(time), site, horizon, time);
	 * 
	 * // getTransitAltitude(Coordinates c, ISite site, long time);
	 * 
	 * double escore = 0.0;
	 * 
	 * // loop over targets, calculate averge value of score Iterator<ITarget>
	 * targets = cs.listTargets(); while (targets.hasNext()) {
	 * 
	 * ITarget target = targets.next(); TargetTrackCalculator track = new
	 * BasicTargetCalculator(target, site);
	 * 
	 * // coordinates and elevation at time Coordinates c =
	 * track.getCoordinates(time); double elev = astro.getAltitude(c, site,
	 * time);
	 * 
	 * // highest elevation in night ahead until sunrise@-2 OR do we want //
	 * highest since sunset also ? double maxelev =
	 * astro.getMaximumAltitude(track, site, lastSunset, sunrise);
	 * 
	 * double tscore = elev / maxelev; if (Double.isNaN(tscore) ||
	 * Double.isInfinite(tscore)) { System.err.println("Elevation score for: " +
	 * group.getName() + "/" + target.getName() + " " + tscore); tscore = 0.0; }
	 * 
	 * escore += tscore;
	 * 
	 * }
	 * 
	 * return escore / cs.countTargets();
	 * 
	 * }
	 */

	/** Return a schedule after a delay. */
	public void requestSchedule(AsynchronousScheduleResponseHandler asrh)
			throws RemoteException {

		// spin off a numbered thread and let it reply after a while

		AsynchResponder ar = new AsynchResponder(asrh);
		(new Thread(ar)).start();

	}

	private class AsynchResponder implements Runnable {

		AsynchronousScheduleResponseHandler asrh;

		private AsynchResponder(AsynchronousScheduleResponseHandler asrh) {
			this.asrh = asrh;
		}

		public void run() {

			ScheduleItem sched = null;

			// let the client know we are working on it...
			try {
				asrh.asynchronousScheduleProgress("Asynch responder will return a schedule to you shortly");
			} catch (Exception ee) {
				logger.create()
						.block("nextScheduledJob")
						.info()
						.level(3)
						.msg("Unable to send progress message to handler: "
								+ ee).send();
			}

			// make the schedule request...
			try {
				logger.create().block("nextScheduledJob").info().level(3)
						.msg("Calling nextSchedJob() for handler: " + asrh)
						.send();
				sched = nextScheduledJob();
				logger.create().block("nextScheduledJob").info().level(3)
						.msg("Schedule request completed, got: " + sched)
						.send();
			} catch (Exception e) {
				logger.create().block("nextScheduledJob").info().level(3)
						.msg("Error obtaining schedule: " + e).send();
				e.printStackTrace();
				try {
					String message = "Unable to generate schedule: " + e;
					logger.create()
							.block("nextScheduledJob")
							.info()
							.level(3)
							.msg("Sending error message to handler: ["
									+ message + "]").send();
					asrh.asynchronousScheduleFailure(5566, message);
				} catch (Exception e2) {
					logger.create()
							.block("nextScheduledJob")
							.info()
							.level(3)
							.msg("Unable to send error message to handler: "
									+ e2).send();
					e2.printStackTrace();
				}
				return;
			}

			// finally let the client know the result...
			try {
				logger.create().block("nextScheduledJob").info().level(3)
						.msg("Sending schedule reply to handler: " + asrh)
						.send();
				asrh.asynchronousScheduleResponse(sched);
			} catch (Exception e3) {
				logger.create().block("nextScheduledJob").info().level(3)
						.msg("Unable to send schedule reply to handler: " + e3)
						.send();
				e3.printStackTrace();
			}
		}

	}

	// /** Calculates which seeing band the specified seeing is in. */
	// private int getSeeingCategory(double seeing) {

	// if (seeing < 0.8)
	// return EnvironmentSnapshot.SEEING_EXCELLENT;
	// else if (seeing < 1.3)
	// return EnvironmentSnapshot.SEEING_AVERAGE;
	// else if (seeing < 3.0)
	// return EnvironmentSnapshot.SEEING_POOR;
	// else if (seeing < 5.0)
	// return EnvironmentSnapshot.SEEING_USABLE;
	// else
	// return EnvironmentSnapshot.SEEING_BAD;
	//
	// }

	// /** Calculates which extinction band the specified extinction is in. */
	// private int getExtinctionCategory(double extinction) {

	// if (extinction < 0.5)
	// return EnvironmentSnapshot.EXTINCTION_PHOTOM;
	// else
	// // return EnvironmentSnapshot.EXTINCTION_SPECTRO;
	// }/

	public void addSchedulingUpdateListener(SchedulingStatusUpdateListener l)
			throws RemoteException {
		boolean ready = false;
		try {
			ready = monitorLock.waitUntilFalse(30000L);
		} catch (InterruptedException ix) {
		}
		if (!ready)
			throw new RemoteException(
					"Monitor list was locked down, try again later");
		if (monitors.contains(l))
			return;
		monitors.add(l);
		logger.create().block("addSchedulingUpdateListener").info().level(2)
				.msg("Adding listener: " + l).send();
	}

	public void removeSchedulingUpdateListener(SchedulingStatusUpdateListener l)
			throws RemoteException {
		boolean ready = false;
		try {
			ready = monitorLock.waitUntilFalse(30000L);
		} catch (InterruptedException ix) {
		}
		if (!ready)
			throw new RemoteException(
					"Monitor list was locked down, try again later");
		if (!monitors.contains(l))
			return;
		monitors.remove(l);
	}

	public List<String> listCandidateQueues() throws RemoteException {
		return QUEUE_NAMES;
	}

	/**
	 * Notify listeners that a sweep has begun.
	 * 
	 * @param time
	 * @param sweepId
	 */
	private void notifyListenersScheduleSweepStarted(long time, int sweepId) {
		Iterator il = monitors.iterator();
		while (il.hasNext()) {
			SchedulingStatusUpdateListener l = (SchedulingStatusUpdateListener) il
					.next();
			try {
				l.scheduleSweepStarted(time, sweepId);
			} catch (Exception e) {
				e.printStackTrace();
				il.remove();
				logger.create().extractCallInfo().warn().level(3)
						.msg("Removed unresponsive listener: " + l).send();
			}
		}

	}

	/**
	 * Notify listeners that a candidate was added to a queue.
	 * 
	 * @param qId
	 * @param group
	 * @param metrics
	 * @param score
	 * @param rank
	 */
	private void notifyListenersCandidateAdded(String qId, GroupItem group,
			ScoreMetricsSet metrics, double score, int rank) {
		Iterator il = monitors.iterator();
		while (il.hasNext()) {
			SchedulingStatusUpdateListener l = (SchedulingStatusUpdateListener) il
					.next();
			try {
				l.candidateAdded(qId, group, metrics, score, rank);
			} catch (Exception e) {
				e.printStackTrace();
				il.remove();
				logger.create().extractCallInfo().warn().level(3)
						.msg("Removed unresponsive listener: " + l).send();
			}
		}
	}

	/**
	 * Notify listeners that a candidate was rejected from all queues.
	 * 
	 * @param group
	 *            The group that is being rejected.
	 * @param reason
	 *            Why it was rejected.
	 */
	private void notifyListenersCandidateRejected(GroupItem group, String reason) {

		// TODO this produces screeds of callbacks

		/*
		 * Iterator il = monitors.iterator(); while (il.hasNext()) {
		 * SchedulingStatusUpdateListener l = (SchedulingStatusUpdateListener)
		 * il.next(); try { l.candidateRejected("ALL", group, reason); } catch
		 * (Exception e) { e.printStackTrace(); il.remove();
		 * logger.create().extractCallInfo
		 * ().warn().level(3).msg("Removed unresponsive listener: " + l).send();
		 * } }
		 */
	}

	/**
	 * Notify listeners that a candidate has been selected for execution.
	 * 
	 * @param schedule
	 */
	private void notifyListenersCandidateSelected(long time,
			ScheduleItem schedule) {
		Iterator il = monitors.iterator();
		while (il.hasNext()) {
			SchedulingStatusUpdateListener l = (SchedulingStatusUpdateListener) il
					.next();
			try {
				l.candidateSelected(time, schedule);
			} catch (Exception e) {
				e.printStackTrace();
				il.remove();
				logger.create().extractCallInfo().warn().level(3)
						.msg("Removed unresponsive listener: " + l).send();
			}
		}
	}

	public ScheduleDespatcher getDespatcher() throws RemoteException {
		return this;
	}

	public VetoManager getVetoManager() {
		return vetoMgr;
	}

	public void ping() throws RemoteException {
		System.err.println("Sched: Ive just been pinged !");
	}

}
