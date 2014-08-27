/**
 * 
 */
package ngat.sms.tlas;

import java.beans.FeatureDescriptor;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.BasicTargetCalculator;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCalculator;
import ngat.astrometry.TargetTrackCalculator;
import ngat.ems.SkyModel;
import ngat.oss.transport.RemotelyPingable;
import ngat.phase2.IAccount;
import ngat.phase2.IHistoryItem;
import ngat.phase2.IProposal;
import ngat.phase2.IQosMetric;
import ngat.phase2.ISemester;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITarget;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XEphemerisTimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.phase2.XPhotometricityConstraint;
import ngat.phase2.XSeeingConstraint;
import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.AsynchronousScheduleResponseHandler;
import ngat.sms.AsynchronousScheduler;
import ngat.sms.CachedAccountSynopsisModel;
import ngat.sms.CachedHistorySynopsisModel;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.ComponentSet;
import ngat.sms.DefaultMutableTimeModel;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentPredictionModel;
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
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.MutableTimeModel;
import ngat.sms.ObservingConstraintAdapter;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.ScheduleDespatcher;
import ngat.sms.ScheduleItem;
import ngat.sms.SchedulingStatusProvider;
import ngat.sms.SchedulingStatusUpdateListener;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TelescopeSystemsSynopsis;
import ngat.sms.TimeModel;
import ngat.sms.Veto;
import ngat.sms.VetoManager;
import ngat.sms.bds.TestScheduleItem;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.util.FeasibilityPrescan;
import ngat.sms.util.PrescanEntry;
import ngat.tcm.Telescope;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class FastLookAheadScheduler extends UnicastRemoteObject implements SchedulingLookaheadStatusProvider, 
									   ScheduleDespatcher, AsynchronousScheduler, VetoManager, RemotelyPingable {

    /** How long to sleep between slots if no group can be found. */
    public static long IDLE_SLEEP_INTERVAL = 2 * 60 * 1000L;

    /** Prescan resolution/interval. */
    private static final long PRESCAN_INTERVAL = 120000L; // 2 minutes

    /** Elevation angle for Zenith. */
    public static final double ELEVATION_ZENITH = Math.PI / 2.0;

    /** Azimuth angle for direction South. */
    public static final double AZIMUTH_SOUTH = 0.0;

    /** Determines the number of sweeps we can make. */
    private int numberSweeps;

    /** Determines how far we can look-ahead. */
    private long horizon;
	
    private HorizonSweep currentSweep;

    /** The currentSequence list. */
    private List<SweepItem> currentSequence;

    ///private Sweep currentSweep;
	
    /** True if we are in a slack period.*/
    private volatile boolean inSlackPeriod = false;
	
    /** When the current slack-period (if any) started.*/
    private volatile long slackPeriodStarted = 0L;
	
    private LogGenerator logger;

    private int sweep = 0;

    private ISite site;
    private TimeModel timeModel;
    private SynopticModelProvider smp;
    private AccountSynopsisModel asm;
    private ExecutionHistorySynopsisModel hsm;
    private Phase2CompositeModel gphase2;
    private ExecutionFeasibilityModel xfm;
    private ExecutionResourceUsageEstimationModel xrm;

    private ExecutionUpdateManager xm;
    private AstrometrySiteCalculator astro;

    private MutableTimeModel cTimeModel;
    private CachedAccountSynopsisModel casm;
    private CachedHistorySynopsisModel chsm;
    private ChargeAccountingModel cam;
    private InstrumentSynopsisModel ism;
    private TelescopeSystemsSynopsis tel;
    private FeasibilityPrescan prescan;

    List prelist;

    // TODO need abetter way of doing this
    String skyModelUrl;

    GenomeFactory genomeFactory;

    /** List of listeners.*/
    List<SchedulingLookaheadUpdateListener> listeners;
	
	
    /**
     * @throws RemoteException
     */
    public FastLookAheadScheduler(TimeModel timeModel, ISite site, ExecutionFeasibilityModel xfm,
				  ExecutionResourceUsageEstimationModel xrm, InstrumentSynopsisModel ism, TelescopeSystemsSynopsis tel,
				  SynopticModelProvider smp, AstrometrySiteCalculator astro, ExecutionUpdateManager xm)
	throws RemoteException {
	super();
	this.timeModel = timeModel;
	this.site = site;
	this.astro = astro;
	this.xfm = xfm;
	this.xrm = xrm;
	this.ism = ism;
	this.tel = tel;
	this.smp = smp;
	this.xm = xm;
	currentSequence = new Vector<SweepItem>();

	Logger alogger = LogManager.getLogger("SMS");
	logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider")
	    .srcCompClass(this.getClass().getSimpleName()).srcCompId("flas");

	// Create the cached models for internal use.
	gphase2 = smp.getPhase2CompositeModel();
	asm = smp.getProposalAccountSynopsisModel();
	hsm = smp.getHistorySynopsisModel();
	cTimeModel = new DefaultMutableTimeModel();
	casm = new CachedAccountSynopsisModel(asm);
	chsm = new CachedHistorySynopsisModel(hsm);

	cam = new StandardChargeAccountingModel();

	prescan = new FeasibilityPrescan(site, smp, xrm, cam, tel, ism);

	genomeFactory = new GenomeFactory();

	listeners = new Vector<SchedulingLookaheadUpdateListener>();
		
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

	logger.create().block("nextScheduledJob").info().level(1)
	    .msg("Schedule sweep [" + sweep + "] requested for model time: " + new Date(time)).send();

	int seeCat = EnvironmentSnapshot.SEEING_EXCELLENT;
	int extCat = EnvironmentSnapshot.EXTINCTION_PHOTOM;
	try {
	    SkyModel skyModel = (SkyModel) Naming.lookup(skyModelUrl);
	    double seeing = skyModel.getSeeing(700.0, ELEVATION_ZENITH, AZIMUTH_SOUTH, time);

	    seeCat = EnvironmentSnapshot.getSeeingCategory(seeing);

	    double extinction = skyModel.getExtinction(700.0, ELEVATION_ZENITH, AZIMUTH_SOUTH, time);

	    extCat = EnvironmentSnapshot.getExtinctionCategory(extinction);
	    logger.create()
		.block("nextScheduledJob")
		.info()
		.level(2)
		.msg("Current sky conditions from remote SkyModel: Seeing=" + seeing + "("
		     + EnvironmentSnapshot.getSeeingCategoryName(seeCat) + ")" + " Extinction=" + extinction
		     + "(" + extCat + ")").send();
	} catch (Exception e) {
	    e.printStackTrace();
	    logger.create().block("nextScheduledJob").error().level(4)
		.msg("Unable to obtain remote SkyModel data: Assuming GOOD and PHOTOM").send();
	}
	EnvironmentSnapshot env = new EnvironmentSnapshot(time, seeCat, extCat);

	// TODO need to generate this - will it be diffrnt for select and
	// lookahead ???
	List<Disruptor> disruptors = new Vector<Disruptor>();

	// see if there is anything still on the current list if it exists
				
	if (!currentSequence.isEmpty()) {
	    logger.create().block("nextScheduledJob").info().level(2)
		.msg("Current sequence contains " + currentSequence.size() + " unexecuted entries").send();
	} else {

	    // there may be a currentSweep with empty sequence or no sweep at all
	    logger.create().block("nextScheduledJob").info().level(2)
		.msg("Current sequence is empty, creating new sequence...").send();

	    // do several sweeps to yield a best sequence	
	    try {

		currentSweep = selectBestSweep(time, horizon, disruptors, env);

		if (currentSweep != null) {
		    // set the global currentSequence value...
		    currentSequence = currentSweep.getSequence();
		    long activeTime = currentSweep.calculateTotalTime();
		    long idleTime = currentSweep.getIdleTime(); 
		    logger.create()
			.block("nextScheduledJob")
			.info()
			.level(2)
			.msg("Best sequence generated for time: " + time + 
			     " contains: " + currentSequence.size()+ 
			     " entries, active: " + ( activeTime/ 60000) + "m"+
			     " idle: "+(idleTime/60000)+"m"+
			     " fill: "+(100.0*(double)activeTime/(double)horizon)).send();

		    // this should not happen
		    if (currentSequence == null)
			return null;

		    if (!currentSequence.isEmpty()) {
			// just logging this info
			logger.create().block("nextScheduledJob").info().level(2).msg("Winning sequence...").send();
			int ii = 0;
			Iterator<SweepItem> is = currentSequence.iterator();
			while (is.hasNext()) {
			    ii++;
			    SweepItem item = is.next();
			    GroupItem g = item.getGroup();
			    logger.create().block("nextScheduledJob").info().level(2)
				.msg("Sequence entry: [" + ii + "]" + g.getName()).send();
			}

			// TODO notifyListenersSequenceGenerated(time, currentSequence);
						
		    }

		}

		// TODO This would be a better place to do the greedy sweep and
		// then log its sequence

	    } catch (Exception e) {
		throw new RemoteException("Unable to determine best sequence", e);
	    }

	}

	// Let listeners know we have a new horizon currentSequence ready.
	// TODO notifyListenersHorizonSequenceCreated(startTimeOfHorizon,
	// horizon, currentSequence);
	// last param is seq or sweep which ? latter gives additional info like
	// tally and totaltime
	// also do we want to pass the env params assumed for the currentSequence

	// is there anything in the currentSequence? , if not there are no feasible
	// sequences atm.
	if (currentSequence.isEmpty())
	    return null;

	// check if first item is feasible, if not record the start of a slack period
	SweepItem item = currentSequence.get(0);
	GroupItem firstGroup = item.getGroup();

	hsm = smp.getHistorySynopsisModel();
	asm = smp.getProposalAccountSynopsisModel();

	// check here that group is still executable, really we only need to test env and window, the hist
	// and accounts should already be ok.
		
	ExecutionHistorySynopsis history = hsm.getExecutionHistorySynopsis(firstGroup.getID(), time);
	AccountSynopsis accounts = asm.getAccountSynopsis(firstGroup.getProposal().getID(), time);
	logger.create().block("nextScheduledJob").info().level(2).msg("Checking first group for feasibility...").send();

	if (xfm.isitFeasible(firstGroup, time, history, accounts, env, disruptors).isFeasible()) {
	    // create the initial history entry and set in GI
	    long hid = -1;
	    try {
		hid = hsm.addHistoryItem(firstGroup.getID());
	    } catch (Exception e) {
		throw new RemoteException("Unable to set initial history entry for selected group", e);
	    }
	    firstGroup.setHId(hid);
			
	    // now safe to remove from sequence list
	    currentSequence.remove(0);
			
	    // Create a new ExecutionUpdater to handle the reply
	    ExecutionUpdater xu = xm.getExecutionUpdater(firstGroup.getID());

	    ScheduleItem scheditem = new TestScheduleItem(firstGroup, xu);
	    return scheditem;
			
	} else {
			
	    // the first group was not feasible after all at this time, it may be too early?		
	    if (inSlackPeriod) {
		if ((time - slackPeriodStarted) > item.getSlack()) {
		    inSlackPeriod = false;
		    // abandon this group its out of time
		    // pull the item off the list
		    currentSequence.remove(0);
		}
		// we allow it more time, executor will likely be in BG mode for a bit
	    } else {
		// record the start of a slack period
		inSlackPeriod = true;
		slackPeriodStarted = time;
	    }
	}
		
	return null;
		
    }

    /**
     * Run the initial prescan to build the feasibility mapping. This should be
     * run during the afternoon as it takes a long time (30m) and must be done before
     * the evening scheduling runs starts.
     */
    public void asynchRunPrescan() {

	logger.create().extractCallInfo().info().level(1).msg("Called asynch prescan...").send();

	final FeasibilityPrescan fprescan = prescan;
	final long ftime = System.currentTimeMillis();

	Runnable r = new Runnable() {

		public void run() {
		    try {
			prelist = fprescan.prescan(ftime, PRESCAN_INTERVAL);
		    } catch (Exception e) {
			e.printStackTrace();
			// how do we let the caller know if we even can ???
		    }
		}
	    };

	(new Thread(r)).start();

    }

    /** Generate a series of sequences (sweeps) and pick the best (highest scoring).
     * @param time The start time of the sweep.
     * @param horizon The horizon length.
     * @param disruptors A list of potential disruptors (usually fixed-time groups).
     * @param env The current environmental conditions.
     * @return The best sweep or none if no groups are available over the entire period.
     * @throws Exception
     */
    private HorizonSweep selectBestSweep(long time, long horizon, List<Disruptor> disruptors, EnvironmentSnapshot env)
	throws Exception {
	logger.create().block("selectBestSweep").info().level(2).msg("Select best currentSequence from: " + numberSweeps)
	    .send();

	// these are just for convenience so we can log later
	double tallies[] = new double[numberSweeps];
	long total[] = new long[numberSweeps];
	HorizonSweep[] sweeps = new HorizonSweep[numberSweeps];

	HorizonSweep bestSweep = null;
	double bestTally = -999.9;
	for (int i = 0; i < numberSweeps; i++) {
	    try {
		logger.create().block("selectBestSweep").info().level(2)
		    .msg("Generate currentSequence: " + i + " of " + numberSweeps).send();
		HorizonSweep horizonSweep = makeSingleSweep(time, horizon, disruptors, env);
		List<SweepItem> sequence = horizonSweep.getSequence();
		logger.create().block("selectBestSweep").info().level(2)
		    .msg("Sequence contained: " + sequence.size() + " items, tally: " + horizonSweep.getTally()).send();

		logger.create().block("selectBestSweep").info().level(2).msg("Candidate currentSequence [" + i + "]...")
		    .send();
		int ii = 0;
		Iterator<SweepItem> is = sequence.iterator();
		while (is.hasNext()) {
		    ii++;
		    SweepItem item = is.next();
		    GroupItem g = item.getGroup();
		    logger.create().block("selectBestSweep").info().level(2)
			.msg("Sequence entry: [" + ii + "]" + g.getName()).send();
		}

		total[i] = horizonSweep.calculateTotalTime();
		sweeps[i] = horizonSweep;

		double tally = horizonSweep.getTally() / total[i];
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
	greedyTally = greedyTally / (double) greedyTotal;

	List<SweepItem> greedySequence = greedySweep.getSequence();

	logger.create().block("selectBestSweep").info().level(2).msg("Greedy despatch currentSequence ...").send();
	int ii = 0;
	Iterator<SweepItem> is = greedySequence.iterator();
	while (is.hasNext()) {
	    ii++;
	    SweepItem item = is.next();
	    GroupItem g = item.getGroup();
	    logger.create().block("selectBestSweep").info().level(2).msg("Sequence entry: [" + ii + "]" + g.getName())
		.send();
	}

	logger.create().block("selectBestSweep").info().level(2).msg("Completed all sweeps, tallies...").send();

	for (int i = 0; i < numberSweeps; i++) {
	    System.err.printf("Sweep[%d] Tally= %2.4f XF= %2.4f = %s \n", i, tallies[i], ((double) total[i] / 3600000.0),
			      sweeps[i].display());
	}
	System.err.printf("Sweep[D] Tally= %2.4f XF= %2.4f = %s", greedyTally, ((double) greedyTotal / 3600000.0),
			  greedySweep.display());

	if (greedyTally > bestTally) {
	    logger.create().block("selectBestSweep").info().level(2).msg("Depatcher wins").send();
	}

	return bestSweep;
    }

    /** Make a single sweep from t to t+h and generate a currentSequence and tally. */
	
	
	
	
    /** Make a single schedule sweep and generate a candidate sequence.
     * @param time The start time of the sweep.
     * @param horizon The horizon length.
     * @param disruptors A list of potential disruptors (usually fixed-time groups).
     * @param env The current environmental conditions.
     * @return A single candidate sequence.
     * @throws Exception
     */
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
	long idle = 0L; // record total idle time in sequence
	long slack = 0L; // record slack time at start of group
	while (t < time + horizon) {

	    SweepItem sweepItem = selectCandidate(t, disruptors, env);

	    if (sweepItem == null) {
		// no candidate so background sleep
		t += IDLE_SLEEP_INTERVAL;
		idle += IDLE_SLEEP_INTERVAL;
		slack += IDLE_SLEEP_INTERVAL;
	    } else {

		// found a group, add any slack then zero the counter
		if (slack > 0L) {
		    sweepItem.setSlack(slack);
		    slack = 0L;				
		}
				
		GroupItem selectedGroup = sweepItem.getGroup();
		// calculate expected time used.
		ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
		ExecutionResource xtime = xrb.getResource("TIME");
		double exec = xtime.getResourceUsage();

		// advance model time by exec time of selected group
		t += (long) exec;

		// update cached temporary accounts for selected group this sweep
		casm.chargeAccount2(selectedGroup.getProposal().getID(), exec, "charge", "tlas");

		// update cached temporary history for selected group this sweep
		Set<IQosMetric> qos = new HashSet<IQosMetric>();
		chsm.updateHistory2(selectedGroup.getID(), 0, IHistoryItem.EXECUTION_SUCCESSFUL, t, null, qos);

		sequence.add(sweepItem);
		tally += sweepItem.getScore() * exec;
	    }

	}

	return new HorizonSweep(sequence, tally, idle);

    }

    /**
     * Make a single <i>greedy</i> schedule sweep and generate a  sequence.
     * @param time The start time of the sweep.
     * @param horizon The horizon length.
     * @param disruptors A list of potential disruptors (usually fixed-time groups).
     * @param env The current environmental conditions.
     * @return A single candidate sequence.
     */
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
		tally += sweepItem.getScore() * exec;
	    }

	}

	return new HorizonSweep(sequence, tally, idle);

    }

    /** Find a single candidate group which is feasible at the specified time under the specified conditions.
     * The algorithm used to select ONE candidate is specified by SelectionModel.
     * @param time The time.
     * @param disruptors A list of potential disruptors.
     * @param env The current environmental conditions.
     * @return A single candidate group.
     * @throws Exception
     */
    private SweepItem selectCandidate(long time, List<Disruptor> disruptors, EnvironmentSnapshot env) throws Exception {

	List<GroupItem> candidates = new Vector<GroupItem>();

	int ng = 0;
	int nc = 0;
	
	// stuffed...
	if (prelist == null)
	    return null;

	for (int ip = 0; ip < prelist.size(); ip++) {

	    PrescanEntry entry = (PrescanEntry) prelist.get(ip);

	    GroupItem group = entry.group;
	    ng++;

	    logger.create().block("nextScheduledJob").info().level(3).msg("Checking group: " + group.getName()).send();

	    ExecutionHistorySynopsis hist = null;
	    try {
		hist = chsm.getExecutionHistorySynopsis(group.getID(), time);
	    } catch (Exception e) {
		logger.create().block("nextScheduledJob").error().level(3).msg("Error reading history: " + e).send();
		continue;
	    }

	    AccountSynopsis accounts = null;
	    try {
		accounts = casm.getAccountSynopsis(group.getProposal().getID(), time);
	    } catch (Exception e) {
		logger.create().block("nextScheduledJob").error().level(3).msg("Error reading accounts: " + e).send();
		continue;
	    }

	    // do a simple local test on seeing and photom and maybe timing
	    // incase its been done already...

	    if (checkFeasible(group, time, hist, accounts, env)) {
		nc++;
		candidates.add(group);
	    }

	}

	// pick one at random from the candidates. or use some sort of pruning
	// or bias or whatever...

	if (candidates.isEmpty())
	    return null;

	// NOTE choose ANY candidate for this sweep
	logger.create().block("nextScheduledJob").info().level(2)
	    .msg("Select any from " + candidates.size() + " potential candidates at: " + time).send();
	int sn = (int) Math.floor((Math.random() * (double) candidates.size()));
	GroupItem selectedGroup = (GroupItem) candidates.get(sn);

	double score = scoreGroup(selectedGroup, time);

	// TODO BETTER
	// GroupItem selectedGroup = randomSelectionModel.getSelection(candidates);
	// double score = scoringModel.getScore(selectedGroup);
		
	ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
	ExecutionResource xtime = xrb.getResource("TIME");
	double exec = xtime.getResourceUsage();

	SweepItem s = new SweepItem(selectedGroup, score, exec);
	s.setGenome(genomeFactory.genome(selectedGroup.getID()));
	return s;
    }
	
    /** Find the <b>best</b> candidate group which is feasible at the specified time under the specified conditions.
     * The algorithm used to determine the best candidate is specified by ScoringModel.
     * @param time The time.
     * @param disruptors A list of potential disruptors.
     * @param env The current environmental conditions.
     * @return The single <b>best</b> candidate group.
     * @throws Exception
     */
    private SweepItem selectGreedyCandidate(long time, List<Disruptor> disruptors, EnvironmentSnapshot env)
	throws Exception {

	// we are assuming certain values for Env here

	List candidates = new Vector();

	int ng = 0;
	int nc = 0;

	if (prelist == null)
	    return null;

	for (int ip = 0; ip < prelist.size(); ip++) {

	    PrescanEntry entry = (PrescanEntry) prelist.get(ip);

	    GroupItem group = entry.group;

	    ng++;

	    logger.create().block("nextScheduledJob").info().level(2).msg("Checking group: " + group.getName()).send();

	    ExecutionHistorySynopsis hist = null;
	    try {
		hist = chsm.getExecutionHistorySynopsis(group.getID(), time);
	    } catch (Exception e) {
		logger.create().block("nextScheduledJob").error().level(3).msg("Error reading history: " + e).send();
		continue;
	    }

	    AccountSynopsis accounts = null;
	    try {
		accounts = casm.getAccountSynopsis(group.getProposal().getID(), time);
	    } catch (Exception e) {
		logger.create().block("nextScheduledJob").error().level(3).msg("Error reading accounts: " + e).send();
		continue;
	    }

	    // complex stuff like disruptors etc - ie la disruptors!

	    // if (xfm.isitFeasible(group, time, hist, accounts, env,
	    // disruptors).isFeasible()) {
	    if (checkFeasible(group, time, hist, accounts, env)) {
		nc++;
		candidates.add(group);
	    }
	}

	// pick one at random from the candidates. or use some sort of pruning
	// or bias or whatever...

	if (candidates.isEmpty())
	    return null;

	// NOTE choose BEST candidate for this sweep
	logger.create().block("nextScheduledJob").info().level(2)
	    .msg("Select best from " + candidates.size() + " potential candidates at: " + time).send();

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
		
	// TODO BETTER
	// GroupItem selectedGroup = greedySelectionModel.getBest(candidates);
	// double score = scoringModel.getScore(selectedGroup);
				
	ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(selectedGroup);
	ExecutionResource xtime = xrb.getResource("TIME");
	double exec = xtime.getResourceUsage();

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

	/*
	 * switch (seeCat) { case XSeeingConstraint.GOOD_SEEING: smscore = 1.0;
	 * break; case XSeeingConstraint.AVERAGE_SEEING: smscore = 0.5; break;
	 * case XSeeingConstraint.POOR_SEEING: smscore = 0.333; break; case
	 * XSeeingConstraint.UNCONSTRAINED_SEEING: smscore = 0.25; break;
	 * default: }
	 */

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

    /** TEMP method to calculate elevation score.*/
    private double calculateElevationScore(GroupItem group, long time) throws Exception {

	ISequenceComponent seq = group.getSequence();
	ComponentSet cs = new ComponentSet(seq);

	// no targets to check
	if (cs.countTargets() == 0)
	    return 0.0;

	// AstrometryCalculator astro = new BasicAstrometryCalculator();
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

    /**
     * Perform simple local test on group feasibility.
     * 
     * @param group
     *            The group to test.
     * @param time
     *            The time of the test.
     * @param hist
     *            Group execution histrory synopsis.
     * @param accounts
     *            Proposal account synopsis.
     * @param env
     *            Environment snapshot at time.
     * @return
     */
    private boolean checkFeasible(GroupItem group, long time, ExecutionHistorySynopsis hist, AccountSynopsis accounts,
				  EnvironmentSnapshot env) {

	// check execution/timing
	if (!checkTiming(group, time, hist))
	    return false;

	// check accounts - watch for semester overlap period - this is complex.
	if (!checkAccounts(group, time, accounts))
	    return false;

	ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);

	// check seeing
	XSeeingConstraint see = oca.getSeeingConstraint();
	if (see == null || see.getSeeingValue() > env.getCorrectedSeeing())
	    return false;

	// check photom - this needs looking at as its quite awkward...
	// XPhotometricityConstraint phot = oca.getPhotometricityConstraint();
	// if (phot == null || phot.getPhotometricityCategory() !=
	// env.getExtinctionState())
	// return false;

	// passed all tests
	return true;
    }

    private boolean checkTiming(GroupItem group, long time, ExecutionHistorySynopsis hist) {

	ITimingConstraint tc = group.getTimingConstraint();

	if (tc == null)
	    return false;

	long lastExec = hist.getLastExecution();
	int countExec = hist.getCountExecutions();

	if (tc instanceof XFlexibleTimingConstraint) {
	    XFlexibleTimingConstraint xflex = (XFlexibleTimingConstraint) tc;

	    if (xflex.getActivationDate() > time)
		return false;
	    if (xflex.getExpiryDate() < time)
		return false;
	    if (lastExec > xflex.getActivationDate() && lastExec < xflex.getExpiryDate())
		return false;

	}

	if (tc instanceof XMonitorTimingConstraint) {

	    XMonitorTimingConstraint xmon = (XMonitorTimingConstraint) tc;

	    long startDate = xmon.getStartDate();
	    long endDate = xmon.getEndDate();
	    long period = xmon.getPeriod();
	    long window = xmon.getWindow();
	    double floatFraction = (double) window / (double) period;

	    double fPeriod = (double) (time - startDate) / (double) period;
	    double iPeriod = Math.rint(fPeriod);

	    if (startDate > time)
		return false;

	    if (endDate < time)
		return false;

	    long startFloat = startDate + (long) ((iPeriod - (double) floatFraction / 2.0) * (double) period);
	    long endFloat = startDate + (long) ((iPeriod + (double) floatFraction / 2.0) * (double) period);

	    if ((startFloat > time) || (endFloat < time))
		return false;
	    // see if its already done in window
	    if (lastExec > startFloat && lastExec < endFloat)
		return false;
	}

	if (tc instanceof XMinimumIntervalTimingConstraint) {

	    XMinimumIntervalTimingConstraint xmin = (XMinimumIntervalTimingConstraint) tc;

	    if (countExec >= xmin.getMaximumRepeats())
		return false;

	    if (time - lastExec < xmin.getMinimumInterval())
		return false;

	    if (xmin.getStart() > time)
		return false;
	    if (xmin.getEnd() < time)
		return false;

	}

	if (tc instanceof XEphemerisTimingConstraint) {

	    XEphemerisTimingConstraint xephem = (XEphemerisTimingConstraint) tc;
	    // TODO SOON we will allow this but need to just check in-window
	    // if (countExec >= 1)
	    // return fail(group, "EPHEM_ALREADY_DONE");

	    if (xephem.getStart() > time)
		return false;
	    if (xephem.getEnd() < time)
		return false;

	    // work out the window periods

	    long startDate = xephem.getStart();
	    long endDate = xephem.getEnd();
	    long period = xephem.getCyclePeriod();
	    double phase = (double) xephem.getPhase();
	    double window = (double) xephem.getWindow();

	    // double fperiod = Math.floor((time - startDate) / period);

	    double fperiod = Math.floor(((double) time - (double) startDate - phase * (double) period + window / 2.0)
					/ period);

	    long startWindow = startDate + (long) ((fperiod + phase) * period - window / 2.0);
	    long endWindow = startDate + (long) ((fperiod + phase) * period + window / 2.0);

	    if (startWindow > time || endWindow < time)
		return false;

	    // NEW 9-nov-09 was executed since start date
	    if (lastExec > startDate)
		return false;

	}

	// not sure we need to do these ?
	if (tc instanceof XFixedTimingConstraint) {

	    XFixedTimingConstraint fixed = (XFixedTimingConstraint) tc;

	    long start = fixed.getStartTime();
	    long end = fixed.getEndTime();
	    // long slack = fixed.getSlack() / 2;

	    // if we are in its start window, its doable
	    long startWindow = start;// - slack;
	    long endWindow = end;// + slack;
	    if ((startWindow > time) || (endWindow < time))
		return false;

	    if (lastExec > startWindow && lastExec < endWindow)
		return false;
	}

	return true;

    }

    private boolean checkAccounts(GroupItem group, long time, AccountSynopsis accounts) {

	ExecutionResourceBundle costs = new ExecutionResourceBundle();

	// total charge for execution
	double timeCost = cam.calculateCost(group.getSequence());

	ExecutionResource timeCostRes = new ExecutionResource("allocation", timeCost);
	costs.addResource(timeCostRes);

	Iterator<ExecutionResource> icost = costs.listResources();
	while (icost.hasNext()) {
	    ExecutionResource costRes = icost.next();

	    double costAmount = costRes.getResourceUsage() / 3600000.0;
	    String costName = costRes.getResourceName();

	    ISemester earlySemester = accounts.getEarlySemester();
	    IAccount costAccountEarly = accounts.getEarlySemesterAccount();
	    double balanceEarly = 0.0;
	    ISemester lateSemester = accounts.getLateSemester();
	    IAccount costAccountLate = accounts.getLateSemesterAccount();
	    double balanceLate = 0.0;

	    // if we have 2 accounts then always try the early then the late
	    // otherwise the single one that exists

	    if (costAccountEarly != null) {
		System.err.println("Found cost account for semester: " + earlySemester.getName());

		balanceEarly = costAccountEarly.getAllocated() - costAccountEarly.getConsumed();
	    }

	    if (costAccountLate != null) {
		System.err.println("Found cost account for: " + lateSemester.getName() + " : " + costAccountLate);

		balanceLate = costAccountLate.getAllocated() - costAccountLate.getConsumed();
	    }

	    // veto groups which are totally out of funds
	    if (balanceEarly <= 0.0 && balanceLate <= 0.0)
		return false;

	    if (balanceEarly + balanceLate < costAmount) {
		// insufficient funds but we let it run this once
	    }

	}

	// default for where a group passes this set of tests
	return true;
    }

    private boolean checkConstraints(GroupItem group, long time, EnvironmentSnapshot env) throws Exception {

	ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);
	ComponentSet cset = new ComponentSet(group.getSequence());

	// check seeing
	XSeeingConstraint xsee = oca.getSeeingConstraint();

	if (xsee != null) {
	    double requiredSeeing = xsee.getSeeingValue();

	    // if they want > 1.5 we need that seeing
	    // if they want < 1.5 we need better still

	    double rcsLimit = requiredSeeing;
	    if (requiredSeeing < 1.5)
		rcsLimit = 0.14472 * requiredSeeing * requiredSeeing * requiredSeeing - 0.96969 * requiredSeeing
		    * requiredSeeing + 3.10189 * requiredSeeing - 1.46731;

	    // decorrect seeing to target elevation for each target, if any fail
	    // we fail
	    Iterator<ITarget> targets = cset.listTargets();
	    while (targets.hasNext()) {
		ITarget target = targets.next();
		TargetTrackCalculator track = new BasicTargetCalculator(target, site);
		// better double targetMinElev = astro.getMinimumAltitude(track,
		// site, time, time + (long) execTime);
		Coordinates c = track.getCoordinates(time);
		double targetElev = astro.getAltitude(c, time);
		double targetZd = 0.5 * Math.PI - targetElev;
		double corrsee = env.getCorrectedSeeing();
		double targetSeeing = corrsee / Math.pow(Math.cos(targetZd), 0.5);
		int targetSeeingState = EnvironmentSnapshot.getSeeingCategory(targetSeeing);

		logger.create()
		    .extractCallInfo()
		    .info()
		    .level(4)
		    .msg("Checking seeing: S_zr= " + corrsee + ", Target elev: " + Math.toDegrees(targetElev)
			 + " S_tgt= " + targetSeeing + ", cat:"
			 + EnvironmentSnapshot.getSeeingCategoryName(targetSeeingState) + " S_lim= " + rcsLimit)
		    .send();

		if (Double.isNaN(targetSeeing))
		    return false;

		if (targetSeeing > rcsLimit)
		    return false;
	    } // next target

	}

	XPhotometricityConstraint xphot = oca.getPhotometricityConstraint();
	if (xphot != null) {

	    int photom = xphot.getPhotometricityCategory();

	    switch (photom) {
	    case XPhotometricityConstraint.PHOTOMETRIC:
		if (env.getExtinctionState() != EnvironmentSnapshot.EXTINCTION_PHOTOM) {
		    return false;
		}
		break;
	    case XPhotometricityConstraint.NON_PHOTOMETRIC:
		if (env.getExtinctionState() != EnvironmentSnapshot.EXTINCTION_SPECTRO) {

		    return false;
		}
		break;
	    }

	    return true;

	}

	return true;

    }

    private void notifyListenersLookaheadSweepStarted(long time) {
		
	for (int i = 0; i < listeners.size(); i++) {
			
	    try {
			
	    } catch (Exception e) {
		e.printStackTrace();
		// if we were using an iterator we could remove the offending listener
		// it may be better to add it to a kill list which we process first...
		// or use some sort of ListenerManagment where we record the number of
		// offences and then kill it if necessary after a specified number
	    }
			
	}
		
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
		logger.create().block("nextScheduledJob").info().level(3)
		    .msg("Unable to send progress message to handler: " + ee).send();
	    }

	    // make the schedule request...
	    try {
		logger.create().block("nextScheduledJob").info().level(3)
		    .msg("Calling nextSchedJob() for handler: " + asrh).send();
		sched = nextScheduledJob();
		logger.create().block("nextScheduledJob").info().level(3)
		    .msg("Schedule request completed, got: " + sched).send();
	    } catch (Exception e) {
		logger.create().block("nextScheduledJob").info().level(3).msg("Error obtaining schedule: " + e).send();
		e.printStackTrace();
		try {
		    String message = "Unable to generate schedule: " + e;
		    logger.create().block("nextScheduledJob").info().level(3)
			.msg("Sending error message to handler: [" + message + "]").send();
		    asrh.asynchronousScheduleFailure(5566, message);
		} catch (Exception e2) {
		    logger.create().block("nextScheduledJob").info().level(3)
			.msg("Unable to send error message to handler: " + e2).send();
		    e2.printStackTrace();
		}
		return;
	    }

	    // finally let the client know the result...
	    try {
		logger.create().block("nextScheduledJob").info().level(3)
		    .msg("Sending schedule reply to handler: " + asrh).send();
		asrh.asynchronousScheduleResponse(sched);
	    } catch (Exception e3) {
		logger.create().block("nextScheduledJob").info().level(3)
		    .msg("Unable to send schedule reply to handler: " + e3).send();
		e3.printStackTrace();
	    }
	}

    }

   

    public void ping() throws RemoteException {
	System.err.println("FLAS: Ive just been pinged !");
    }

    public void addSchedulingLookaheadUpdateListener(SchedulingLookaheadUpdateListener l) throws RemoteException {
	if (listeners.contains(l))
	    return;
	listeners.add(l);
		
    }

    public void removeSchedulingLookaheadUpdateListener(SchedulingLookaheadUpdateListener l) throws RemoteException {
	if (!listeners.contains(l))
	    return;
	listeners.remove(l);
		
    }
    
    public void vetoGroup(long gid, long time) throws RemoteException {
    	// TODO Auto-generated method stub

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
