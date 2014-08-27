/**
 * 
 */
package ngat.sms.tlas;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicTargetCalculator;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCalculator;
import ngat.astrometry.TargetTrackCalculator;
import ngat.ems.SkyModel;
import ngat.oss.transport.RemotelyPingable;
import ngat.phase2.IHistoryItem;
import ngat.phase2.IProposal;
import ngat.phase2.IQosMetric;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITarget;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.AsynchronousScheduleResponseHandler;
import ngat.sms.AsynchronousScheduler;
import ngat.sms.CachedAccountSynopsisModel;
import ngat.sms.CachedHistorySynopsisModel;
import ngat.sms.ComponentSet;
import ngat.sms.DefaultMutableTimeModel;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionFeasibilityModel;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.ExecutionUpdateManager;
import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;
import ngat.sms.MutableTimeModel;
import ngat.sms.ObservingConstraintAdapter;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.ScheduleDespatcher;
import ngat.sms.ScheduleItem;
import ngat.sms.SchedulingStatusProvider;
import ngat.sms.SchedulingStatusUpdateListener;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TimeModel;
import ngat.sms.Veto;
import ngat.sms.VetoManager;
import ngat.sms.bds.TestScheduleItem;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class TestLookAheadScheduler extends UnicastRemoteObject implements SchedulingStatusProvider, ScheduleDespatcher, AsynchronousScheduler, VetoManager, RemotelyPingable {

	/** How long to sleep between slots if no group can be found. */
	public static long IDLE_SLEEP_INTERVAL = 2 * 60 * 1000L;

	/** Elevation angle for Zenith. */
	public static final double ELEVATION_ZENITH = Math.PI / 2.0;

	/** Azimuth angle for direction South. */
	public static final double AZIMUTH_SOUTH = 0.0;

	/** Determines the number of sweeps we can make. */
	private int numberSweeps;

	/** Determines how far we can look-ahead. */
	private long horizon;

	/** The sequence list. */
	private List<SweepItem> sequence;

	private LogGenerator logger;

	private int sweep = 0;

	private TimeModel timeModel;
	private SynopticModelProvider smp;
	private AccountSynopsisModel asm;
	private ExecutionHistorySynopsisModel hsm;
	private Phase2CompositeModel gphase2;
	private ExecutionFeasibilityModel xfm;
	private ExecutionResourceUsageEstimationModel xrm;
	//private ISite site;
	private ExecutionUpdateManager xm;
	private AstrometrySiteCalculator astro;
	
	private MutableTimeModel cTimeModel;
	private CachedAccountSynopsisModel casm;
	private CachedHistorySynopsisModel chsm;

	// TODO need abetter way of doing this
	String skyModelUrl;

    GenomeFactory genomeFactory;

	/**
	 * @throws RemoteException
	 */
	public TestLookAheadScheduler(TimeModel timeModel, ExecutionFeasibilityModel xfm,
			ExecutionResourceUsageEstimationModel xrm,
			SynopticModelProvider smp,
			AstrometrySiteCalculator astro,
			ExecutionUpdateManager xm)
			throws RemoteException {
		super();
		this.timeModel = timeModel;
		this.astro = astro;
		this.xfm = xfm;
		this.xrm = xrm;
		this.smp = smp;
		this.xm = xm;
		
		sequence = new Vector<SweepItem>();

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("tlas");

		// Create the cached models for internal use.
		gphase2 = smp.getPhase2CompositeModel();
		asm = smp.getProposalAccountSynopsisModel();
		hsm = smp.getHistorySynopsisModel();
		cTimeModel = new DefaultMutableTimeModel();
		casm = new CachedAccountSynopsisModel(asm);
		chsm = new CachedHistorySynopsisModel(hsm);

		genomeFactory = new GenomeFactory();

	}

	// TODO need better way of doing this...
	public void setSkyModelUrl(String url) {
		this.skyModelUrl = url;
	}

	/**
	 * @return the numberSweeps
	 */
	public int getNumberSweeps() {
		return numberSweeps;
	}

	/**
	 * @param numberSweeps
	 *            the numberSweeps to set
	 */
	public void setNumberSweeps(int numberSweeps) {
		this.numberSweeps = numberSweeps;
	}

	/**
	 * @return the horizon
	 */
	public long getHorizon() {
		return horizon;
	}

	/**
	 * @param horizon
	 *            the horizon to set
	 */
	public void setHorizon(long horizon) {
		this.horizon = horizon;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seengat.sms.Scheduler#addSchedulingUpdateListener(ngat.sms.
	 * SchedulingStatusUpdateListener)
	 */
	public void addSchedulingUpdateListener(SchedulingStatusUpdateListener l) throws RemoteException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SchedulingStatusProvider#getDespatcher()
	 */
	public ScheduleDespatcher getDespatcher() throws RemoteException {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SchedulingStatusProvider#listCandidateQueues()
	 */
	public List<String> listCandidateQueues() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seengat.sms.Scheduler#removeSchedulingUpdateListener(ngat.sms.
	 * SchedulingStatusUpdateListener)
	 */
	public void removeSchedulingUpdateListener(SchedulingStatusUpdateListener l) throws RemoteException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seengat.sms.AsynchronousScheduler#requestSchedule(ngat.sms.
	 * AsynchronousScheduleResponseHandler)
	 */
	public void requestSchedule(AsynchronousScheduleResponseHandler asrh) throws RemoteException {		
		AsynchResponder ar = new AsynchResponder(asrh);
		(new Thread(ar)).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.ScheduleDespatcher#nextScheduledJob()
	 */
	public ScheduleItem nextScheduledJob() throws RemoteException {

		long time = timeModel.getTime();
		sweep++;

		logger.create().block("nextScheduledJob").info().level(1).msg(
				"Schedule sweep [" + sweep + "] requested for model time: " + new Date(time)).send();

		int seeCat = EnvironmentSnapshot.SEEING_EXCELLENT;
		int extCat = EnvironmentSnapshot.EXTINCTION_PHOTOM;
		try {
			SkyModel skyModel = (SkyModel) Naming.lookup(skyModelUrl);
			double seeing = skyModel.getSeeing(700.0, ELEVATION_ZENITH, AZIMUTH_SOUTH, time);

			seeCat = EnvironmentSnapshot.getSeeingCategory(seeing);

			double extinction = skyModel.getExtinction(700.0, ELEVATION_ZENITH, AZIMUTH_SOUTH, time);

			extCat = getExtinctionCategory(extinction);
			logger.create().block("nextScheduledJob").info().level(2).msg(
					"Current sky conditions from remote SkyModel: Seeing=" + seeing + "("
							+ EnvironmentSnapshot.getSeeingCategoryName(seeCat) + ")" + " Extinction=" + extinction + "("
							+ extCat + ")").send();
		} catch (Exception e) {
			e.printStackTrace();
			logger.create().block("nextScheduledJob").error().level(4).msg(
					"Unable to obtain remote SkyModel data: Assuming BAD and NON_PHOTOM").send();
		}
		EnvironmentSnapshot env = new EnvironmentSnapshot(time, seeCat, extCat);

		// TODO need to generate this - will it be diffrnt for select and
		// lookahead ???
		List<Disruptor> disruptors = new Vector<Disruptor>();

		// see if there is anything on the list
		if (!sequence.isEmpty()) {
			logger.create().block("nextScheduledJob").info().level(2)
			.msg("Current sequence contains "+sequence.size()+" unexecuted entries").send();
		} else {
			
			logger.create().block("nextScheduledJob").info().level(2)
			.msg("Current sequence is empty, creating new sequence...").send();
			
			// do several sweeps to yield a best sequence 
			// WHY is horizon a param but not NSweep ???
			try {
			
				HorizonSweep horizonSweep = selectBestSweep(time, horizon, disruptors, env);
				
				if (horizonSweep != null) {
					// set the global sequence value...
					sequence = horizonSweep.getSequence();
					logger.create().block("nextScheduledJob").info().level(2)
					    .msg("Best sequence generated for time: "+time+
						 " contains: "+sequence.size()+
						 " entries, duration "+(horizonSweep.calculateTotalTime()/60000)+"m")
					    .send(); 
					
					// this should not happen
					if (sequence == null)
						return null;
					
					if (!sequence.isEmpty()) {

					    logger.create().block("nextScheduledJob").info().level(2)
						.msg("Winning sequence...").send();
						int ii = 0;
						Iterator<SweepItem> is = sequence.iterator();
						while (is.hasNext()) {
							ii++;
							SweepItem s = is.next();
							GroupItem g = s.getGroup();
							logger.create().block("nextScheduledJob").info().level(2).msg(
									"Sequence entry: [" + ii + "]" + g.getName()).send();
						}

					}

				}
				
				
				// TODO This would be a better place to do the greedy sweep and then log its sequence
				
			} catch (Exception e) {
				throw new RemoteException("Unable to determine best sequence", e);
			}

		}
		

		// Let listeners know we have a new horizon sequence ready.
		// TODO notifyListenersHorizonSequenceCreated(startTimeOfHorizon, horizon, sequence);
		// last param is seq or sweep which ? latter gives additional info like tally and totaltime
		// also do we want to pass the env params assumed for the sequence
		
		// is there anything in the sequence? , if not there are no feasible
		// sequences atm.
		if (sequence.isEmpty())
			return null;

		// check if first item is feasible.
		SweepItem fs = sequence.remove(0);
		GroupItem firstGroup = fs.getGroup();
	
		hsm = smp.getHistorySynopsisModel();
		asm = smp.getProposalAccountSynopsisModel();
	
		ExecutionHistorySynopsis history = hsm.getExecutionHistorySynopsis(firstGroup.getID(), time);
		AccountSynopsis accounts = asm.getAccountSynopsis(firstGroup.getProposal().getID(), time);
		logger.create().block("nextScheduledJob").info().level(2)
			.msg("Checking first group for feasibility...").send();
		
		if (xfm.isitFeasible(firstGroup, time, history, accounts, env, disruptors).isFeasible()) {
			// create the initial history entry and set in GI
			long hid = -1;
			try {
				hid = hsm.addHistoryItem(firstGroup.getID());
			} catch (Exception e) {
				throw new RemoteException("Unable to set initial history entry for selected group", e);
			}
			firstGroup.setHId(hid);

			// Create a new ExecutionUpdater to handle the reply
			ExecutionUpdater xu = xm.getExecutionUpdater(firstGroup.getID());

			ScheduleItem item = new TestScheduleItem(firstGroup, xu);
			return item;
		} else {
			// the first group was not feasible after all
			return null;
		}
	}

	private HorizonSweep selectBestSweep(long time, long horizon, List<Disruptor> disruptors,
			EnvironmentSnapshot env) throws Exception {
		logger.create().block("selectBestSweep").info().level(2).msg("Select best sequence from: " + numberSweeps)
				.send();

		double tallies[] = new double[numberSweeps];
		long total[] = new long[numberSweeps];
		HorizonSweep[] sweeps = new HorizonSweep[numberSweeps];

		HorizonSweep bestSweep = null;
		double bestTally = -999.9;
		for (int i = 0; i < numberSweeps; i++) {
			try {
				logger.create().block("selectBestSweep").info().level(2).msg(
						"Generate sequence: " + i + " of " + numberSweeps).send();
				HorizonSweep horizonSweep = makeSingleSweep(time, horizon, disruptors, env);
				List<SweepItem> sequence = horizonSweep.getSequence();
				logger.create().block("selectBestSweep").info().level(2).msg(
						"Sequence contained: " + sequence.size() + 
						" items, tally: " + horizonSweep.getTally()).send();
				
				logger.create().block("selectBestSweep").info().level(2)
				    .msg("Candidate sequence ["+i+"]...").send();
				int ii = 0;
				Iterator<SweepItem> is = sequence.iterator();
				while (is.hasNext()) {
				    ii++;
				    SweepItem s = is.next();
				    GroupItem g = s.getGroup();
				    logger.create().block("selectBestSweep").info().level(2)
					.msg("Sequence entry: [" + ii + "]" + g.getName()).send();
				}
		
				total[i] = horizonSweep.calculateTotalTime();
				sweeps[i] = horizonSweep;

				double tally = horizonSweep.getTally()/total[i];
				tallies[i] = tally;
				if (tally > bestTally) {
					bestSweep = horizonSweep;
					bestTally = tally;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// do a Greedy sweep (despatcher)
		HorizonSweep greedySweep = makeGreedySweep(time, horizon, disruptors, env);
		double greedyTally = greedySweep.getTally();
		long greedyTotal = greedySweep.calculateTotalTime();
		greedyTally = greedyTally/(double)greedyTotal;
		
		List<SweepItem> greedySequence = greedySweep.getSequence();

		logger.create().block("selectBestSweep").info().level(2)
		    .msg("Greedy despatch sequence ...").send();
		int ii = 0;
		Iterator<SweepItem> is = greedySequence.iterator();
		while (is.hasNext()) {
		    ii++;
		    SweepItem s = is.next();
		    GroupItem g =s.getGroup();
		    logger.create().block("selectBestSweep").info().level(2)
			.msg("Sequence entry: [" + ii + "]" + g.getName()).send();
		}
		
		logger.create().block("selectBestSweep").info().level(2)
		    .msg("Completed all sweeps, tallies...").send();

		for (int i = 0; i < numberSweeps; i++) {
		    System.err.printf("Sweep[%d] Tally= %2.4f XF= %2.4f = %s", 
		    		i, tallies[i], ((double)total[i]/3600000.0), sweeps[i].display());
		}
		System.err.printf("Sweep[D] Tally= %2.4f XF= %2.4f = %s",
				greedyTally, ((double)greedyTotal/3600000.0), greedySweep.display());
		
		if (greedyTally > bestTally) {
			logger.create().block("selectBestSweep").info().level(2).msg("Depatcher wins").send();
		}
		
		return bestSweep;
	}

	/** Make a single sweep from t to t+h and generate a sequence and tally. */
	private HorizonSweep makeSingleSweep(long time, long horizon, List<Disruptor> disruptors, EnvironmentSnapshot env)
			throws Exception {

		List<SweepItem> sequence = new Vector<SweepItem>();
		double tally = 0.0;

		// create/clear cached models for H and A
		cTimeModel.setTime(time);
		casm.clearCache();
		chsm.clearCache();
		// run through the night from t to t+h
		long t = time;
		long idle = 0L;
		while (t < time + horizon) {

			// TODO sweepItem here.....
			SweepItem sweepItem = selectCandidate(t, disruptors, env);

			if (sweepItem == null) {
				// no candidate
				t += IDLE_SLEEP_INTERVAL;
				idle += IDLE_SLEEP_INTERVAL;
			} else {

				GroupItem selectedGroup = sweepItem.getGroup();
				// calculate expected time used.
				ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
				ExecutionResource xtime = xrb.getResource("TIME");
				double exec = xtime.getResourceUsage();

				// advance model time by exec time of selected group
				t += (long) exec;

				// update cached accounts for selected group this sweep
				casm.chargeAccount(selectedGroup.getProposal().getID(), exec, "charge", "tlas");

				// update cached history for selected group this sweep
				Set<IQosMetric> qos = new HashSet<IQosMetric>();
				chsm.updateHistory(selectedGroup.getID(), 0, IHistoryItem.EXECUTION_SUCCESSFUL, t, null, qos);

				sequence.add(sweepItem);
				tally += sweepItem.getScore()*exec;
			}

		}

		return new HorizonSweep(sequence, tally, idle);

	}

	
	/** Make a single sweep from t to t+h and generate a sequence and tally. */
	private HorizonSweep makeGreedySweep(long time, long horizon, List<Disruptor> disruptors, EnvironmentSnapshot env)
			throws Exception {

		List<SweepItem> sequence = new Vector<SweepItem>();
		double tally = 0.0;

		// create/clear cached models for H and A
		cTimeModel.setTime(time);
		casm.clearCache();
		chsm.clearCache();
		// run through the night from t to t+h
		long t = time;
		long idle = 0L;
		while (t < time + horizon) {

			// TODO sweepItem here.....
			SweepItem sweepItem = selectGreedyCandidate(t, disruptors, env);

			if (sweepItem == null) {
				// no candidate
				t += IDLE_SLEEP_INTERVAL;
				idle += IDLE_SLEEP_INTERVAL;
			} else {

				GroupItem selectedGroup = sweepItem.getGroup();
				// calculate expected time used.
				ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
				ExecutionResource xtime = xrb.getResource("TIME");
				double exec = xtime.getResourceUsage();

				// advance model time by exec time of selected group
				t += (long) exec;

				// update cached accounts for selected group this sweep
				casm.chargeAccount(selectedGroup.getProposal().getID(), exec, "charge", "tlas");

				// update cached history for selected group this sweep
				Set<IQosMetric> qos = new HashSet<IQosMetric>();
				chsm.updateHistory(selectedGroup.getID(), 0, IHistoryItem.EXECUTION_SUCCESSFUL, t, null, qos);

				sequence.add(sweepItem);
				tally += sweepItem.getScore()*exec;
			}

		}

		return new HorizonSweep(sequence, tally, idle);

	}

	
	private SweepItem selectCandidate(long time, List<Disruptor> disruptors, EnvironmentSnapshot env) throws Exception {

		// we are assuming certain values for Env here

		List candidates = new Vector();

		int ng = 0;
		int nc = 0;
		List<GroupItem> lgroups = gphase2.listGroups();
		Iterator<GroupItem> groups = lgroups.iterator();
		while (groups.hasNext()) {
			GroupItem group = groups.next();
			ng++;
			
			logger.create().block("nextScheduledJob").info().level(3).msg("Checking group: " + group.getName()).send();

			ExecutionHistorySynopsis hist = null;
			try {
				hist = chsm.getExecutionHistorySynopsis(group.getID(), time);
			} catch (Exception e) {
				logger.create().block("nextScheduledJob").error().level(3)
				.msg("Error reading history: " + e).send();
				continue;
			}

			AccountSynopsis accounts = null;
			try {
				accounts = casm.getAccountSynopsis(group.getProposal().getID(), time);
			} catch (Exception e) {
				logger.create().block("nextScheduledJob").error().level(3)
				.msg("Error reading accounts: " + e).send();
				continue;
			}
			
			// complex stuff like disruptors etc - ie la disruptors!

			if (xfm.isitFeasible(group, time, hist, accounts, env, disruptors).isFeasible()) {
				nc++;
				candidates.add(group);
			}
		}

		// pick one at random from the candidates. or use some sort of pruning
		// or bias or whatever...

		if (candidates.isEmpty())
			return null;

		// NOTE choose ANY candidate for this sweep
		logger.create().block("nextScheduledJob").info().level(2).msg(
				"Select any from " + candidates.size() + " potential candidates at: " + time).send();
		int sn = (int) Math.floor((Math.random() * (double) candidates.size()));
		GroupItem selectedGroup = (GroupItem) candidates.get(sn);

		double score = scoreGroup(selectedGroup, time);

		ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
		ExecutionResource xrTime = xrb.getResource("TIME");
		double exec = xrTime.getResourceUsage();
		
		// create a sweep item for the group
		SweepItem s = new SweepItem(selectedGroup, score, exec);
		s.setGenome(genomeFactory.genome(selectedGroup.getID()));
		
		return s;
	}

	private SweepItem selectGreedyCandidate(long time, List<Disruptor> disruptors, EnvironmentSnapshot env) throws Exception {

		// we are assuming certain values for Env here

		List candidates = new Vector();
		
		int ng = 0;
		int nc = 0;
		List<GroupItem> lgroups = gphase2.listGroups();
		Iterator<GroupItem> groups = lgroups.iterator();
		while (groups.hasNext()) {
			GroupItem group = groups.next();
			ng++;
			
			logger.create().block("nextScheduledJob").info().level(2).msg("Checking group: " + group.getName()).send();

			ExecutionHistorySynopsis hist = null;
			try {
				hist = chsm.getExecutionHistorySynopsis(group.getID(), time);
			} catch (Exception e) {
				logger.create().block("nextScheduledJob").error().level(3)
				.msg("Error reading history: " + e).send();
				continue;
			}

			AccountSynopsis accounts = null;
			try {
				accounts = casm.getAccountSynopsis(group.getProposal().getID(), time);
			} catch (Exception e) {
				logger.create().block("nextScheduledJob").error().level(3)
				.msg("Error reading accounts: " + e).send();
				continue;
			}
			
			// complex stuff like disruptors etc - ie la disruptors!

			if (xfm.isitFeasible(group, time, hist, accounts, env, disruptors).isFeasible()) {
				nc++;
				candidates.add(group);
			}
		}

		// pick one at random from the candidates. or use some sort of pruning
		// or bias or whatever...

		if (candidates.isEmpty())
			return null;

		// NOTE choose BEST candidate for this sweep
		logger.create().block("nextScheduledJob").info().level(2).msg(
				"Select best from " + candidates.size() + " potential candidates at: " + time).send();
		
		GroupItem selectedGroup = null;
		double bestScore = -999.999;
		
		Iterator icand = candidates.iterator();
		while (icand.hasNext()) {
			GroupItem group = (GroupItem) icand.next();
			double score = scoreGroup(group, time);
			if (score > bestScore) {
				selectedGroup = group;
				bestScore = score;
			}
		}
		
		ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
		ExecutionResource xrTime = xrb.getResource("TIME");
		double exec = xrTime.getResourceUsage();
		
		// create a sweep item for the group
		SweepItem s = new SweepItem(selectedGroup, bestScore, exec);
		s.setGenome(genomeFactory.genome(selectedGroup.getID()));
		return s;
		
	}

	
	
	/** TEMP method to calculate group score. */
	private double scoreGroup(GroupItem group, long time) throws Exception {

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

		ITimingConstraint timing = group.getTimingConstraint();
		if (timing instanceof XMonitorTimingConstraint || timing instanceof XMinimumIntervalTimingConstraint) {
			pscore += 1.0;
		}

		pscore /= 7.0;

		// now add some seeing matching stuff..
		double smscore = 0.0;
		ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);
		double maxSeeing = oca.getSeeingConstraint().getSeeingValue();
		
		
		/*switch (seeCat) {
		case XSeeingConstraint.GOOD_SEEING:
			smscore = 1.0;
			break;
		case XSeeingConstraint.AVERAGE_SEEING:
			smscore = 0.5;
			break;
		case XSeeingConstraint.POOR_SEEING:
			smscore = 0.333;
			break;
		case XSeeingConstraint.UNCONSTRAINED_SEEING:
			smscore = 0.25;
			break;
		default:
		}*/

		// elevation of target re max in night
		double escore = 0.0;
		try {
			escore = calculateElevationScore(group, time);
		} catch (Exception e) {
			e.printStackTrace();
			// escore will be zero now
		}

		double random = Math.random();

		double score = 0.5 * escore + 0.5 * pscore + 0.15 * smscore + 0.05 * random;

		String path = (group.getTag() != null ? group.getTag().getName() : "UNK_TAG") + "/"
				+ (group.getUser() != null ? group.getUser().getName() : "UNK_USR") + "/"
				+ (group.getProposal() != null ? group.getProposal().getName() : "UNK_PRP");

		String result = String.format("Scoring group: [%35.35s..] [%20.20s..] %2.2f %2.2f %2.2f %2.2f -> %2.4f ", path,
				group.getName(), escore, pscore, smscore, random, score);

		// ... Scoring group: [JMU/Bloggs.Fred/JL09B007..] [RS_oph_big_bonus..]
		// 0.3 0.25 0.54 0.02 -> 1.2453

		logger.create().block("nextScheduledJob").info().level(3).msg(result).send();

		return score;

	}

	private double calculateElevationScore(GroupItem group, long time) throws Exception {

		ISequenceComponent seq = group.getSequence();
		ComponentSet cs = new ComponentSet(seq);

		// no targets to check
		if (cs.countTargets() == 0)
			return 0.0;

		//AstrometryCalculator astro = new BasicAstrometryCalculator();
		ISite site = astro.getSite();
				
		SolarCalculator sun = new SolarCalculator();
		long sunrise = 0L;

		// use rise horizon -2.0 degrees
		double horizon = Math.toRadians(-2.0);
		// check the suns not already up...
		double sunelev = astro.getAltitude(sun.getCoordinates(time), time);
		if (sunelev > horizon)
			sunrise = time; // bizarre but let it through
		else
			sunrise = time + (long) astro.getTimeUntilNextRise(sun.getCoordinates(time), horizon, time);

		// long lastSunset = time - (long)
		// astro.getTimeSinceLastSet(sun.getCoordinates(time), site, horizon,
		// time);

		// getTransitAltitude(Coordinates c, ISite site, long time);

		double escore = 0.0;

		// loop over targets, calculate averge value of score
		Iterator<ITarget> targets = cs.listTargets();
		while (targets.hasNext()) {

			ITarget target = targets.next();
			TargetTrackCalculator track = new BasicTargetCalculator(target, site);

			// coordinates and elevation at time
			Coordinates c = track.getCoordinates(time);
			double elev = astro.getAltitude(c, time);

			// highest elevation in night ahead until sunrise@-2 OR do we want
			// highest since sunset also ?
			double maxelev = astro.getMaximumAltitude(track, time, sunrise);

			double tscore = elev / maxelev;
			if (Double.isNaN(tscore) || Double.isInfinite(tscore)) {
				System.err.println("Elevation score for: " + group.getName() + "/" + target.getName() + " " + tscore);
				tscore = 0.0;
			}

			escore += tscore;

		}

		return escore / cs.countTargets();

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
				logger.create().block("nextScheduledJob").info().level(3).msg(
						"Unable to send progress message to handler: " + ee).send();
			}

			// make the schedule request...
			try {
				logger.create().block("nextScheduledJob").info().level(3).msg(
						"Calling nextSchedJob() for handler: " + asrh).send();
				sched = nextScheduledJob();
				logger.create().block("nextScheduledJob").info().level(3).msg(
						"Schedule request completed, got: " + sched).send();
			} catch (Exception e) {
				logger.create().block("nextScheduledJob").info().level(3).msg("Error obtaining schedule: " + e).send();
				e.printStackTrace();
				try {
					String message = "Unable to generate schedule: " + e;
					logger.create().block("nextScheduledJob").info().level(3).msg(
							"Sending error message to handler: [" + message + "]").send();
					asrh.asynchronousScheduleFailure(5566, message);
				} catch (Exception e2) {
					logger.create().block("nextScheduledJob").info().level(3).msg(
							"Unable to send error message to handler: " + e2).send();
					e2.printStackTrace();
				}
				return;
			}

			// finally let the client know the result...
			try {
				logger.create().block("nextScheduledJob").info().level(3).msg(
						"Sending schedule reply to handler: " + asrh).send();
				asrh.asynchronousScheduleResponse(sched);
			} catch (Exception e3) {
				logger.create().block("nextScheduledJob").info().level(3).msg(
						"Unable to send schedule reply to handler: " + e3).send();
				e3.printStackTrace();
			}
		}

	}


	/** Calculates which seeing band the specified seeing is in. */
	private int getSeeingCategory(double seeing) {

		if (seeing < 0.8)
			return EnvironmentSnapshot.SEEING_EXCELLENT;
		else if (seeing < 1.3)
			return EnvironmentSnapshot.SEEING_AVERAGE;
		else if (seeing < 3.0)
			return EnvironmentSnapshot.SEEING_POOR;
		else if (seeing < 5.0)
			return EnvironmentSnapshot.SEEING_USABLE;
		else
			return EnvironmentSnapshot.SEEING_BAD;

	}

	/** Calculates which extinction band the specified extinction is in. */
	private int getExtinctionCategory(double extinction) {

		if (extinction < 0.5)
			return EnvironmentSnapshot.EXTINCTION_PHOTOM;
		else
			return EnvironmentSnapshot.EXTINCTION_SPECTRO;
	}

	public void vetoGroup(long gid, long time) throws RemoteException {
		// TODO Auto-generated method stub

	}
	
	public void ping() throws RemoteException {
		System.err.println("TLAS: Ive just been pinged !");
	}

	public void removeVeto(long gid) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	public long getVetoTime(long gid) throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Veto> listActiveVetos() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	
}
