/**
 * 
 */
package ngat.sms.util;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.SimpleTimeZone;

import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.AstrometryException;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCalculator;
import ngat.astrometry.test.GetCalculator;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XEphemerisTimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.phase2.XSeeingConstraint;
import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.CachedAccountSynopsisModel;
import ngat.sms.CachedHistorySynopsisModel;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.FeasibilityPrescanController;
import ngat.sms.FeasibilityPrescanMonitor;
import ngat.sms.FeasibilityPrescanUpdateListener;
import ngat.sms.GroupItem;
import ngat.sms.InstrumentSynopsisModel;
import ngat.sms.ObservingConstraintAdapter;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TelescopeSystemsSynopsis;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.genetic.test.GenomeMapper;
import ngat.sms.genetic.test.OptimisticInstrumentSynopsisModel;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class FeasibilityPrescan extends UnicastRemoteObject implements FeasibilityPrescanMonitor,
		FeasibilityPrescanController {

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	private SynopticModelProvider smp;
	private ExecutionResourceUsageEstimationModel xrm;
	private ChargeAccountingModel cam;
	private TelescopeSystemsSynopsis tel;
	private InstrumentSynopsisModel ism;
	private ISite site;

	private List<FeasibilityPrescanUpdateListener> listeners;

	private LogGenerator logger;

	public FeasibilityPrescan(ISite site, SynopticModelProvider smp, ExecutionResourceUsageEstimationModel xrm,
			ChargeAccountingModel cam, TelescopeSystemsSynopsis tel, InstrumentSynopsisModel ism) throws RemoteException {
		super();
		this.site = site;
		this.smp = smp;
		this.xrm = xrm;
		this.cam = cam;
		this.tel = tel;
		this.ism = ism;
		sdf.setTimeZone(UTC);

		Logger alogger = LogManager.getLogger("GSS");
		logger = new LogGenerator(alogger);
		logger.system("SMS").subSystem("GSS").srcCompClass("Prescan");

		listeners = new Vector<FeasibilityPrescanUpdateListener>();

	}

	public List prescan(long time, long interval) throws RemoteException {

		Logger smslogger = LogManager.getLogger("SMS");
		int oldlevel = smslogger.getLogLevel();
		smslogger.setLogLevel(1);

		long startScan = System.currentTimeMillis();

		List seq = new Vector();

		List prescan = new Vector();

		AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);

		SolarCalculator sun = null;
		Coordinates sunTrack = null;
		double sunlev;
		try {
			sun = new SolarCalculator();
			sunTrack = sun.getCoordinates(time);
			sunlev = astro.getAltitude(sunTrack, time);
		} catch (AstrometryException ax) {
			throw new RemoteException("Error determining solar position", ax);
		}

		logger.create().extractCallInfo().level(2).msg("Sun elevation is: " + Math.toDegrees(sunlev)).send();
		boolean daytime = (sunlev > 0.0);

		long start = 0L;
		long end = 0L;
		if (daytime) {
			logger.create().extractCallInfo().level(2).msg("It is currently daytime").send();
			try {
				start = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
				end = start + astro.getTimeUntilNextRise(sunTrack, 0.0, start);
			} catch (AstrometryException ax) {
				throw new RemoteException("Error determining time limits", ax);
			}
		} else {
			logger.create().extractCallInfo().level(2).msg("It is currently nighttime").send();
			try {
				start = time;
				end = time + astro.getTimeUntilNextRise(sunTrack, 0.0, time);
			} catch (AstrometryException ax) {
				throw new RemoteException("Error determining time limits", ax);
			}
		}
		logger.create().extractCallInfo().level(2).msg(
				"Start prescan at: " + sdf.format(new Date(start)) + ", End at: " + sdf.format(new Date(end))
						+ " Night duration: " + ((end - start) / 3600000) + "H").send();

		// need the following astrocalc, site, exresmodl, cam, telsyn,
		// imstsynmodl
		AstrometryCalculator astrocalc = new BasicAstrometryCalculator();
		
		
		InstrumentSynopsisModel oism = new OptimisticInstrumentSynopsisModel(ism);

		StandardExecutionFeasibilityModel xfm = new StandardExecutionFeasibilityModel(astrocalc, xrm, cam, site, tel,
				oism);

		// Check all groups from cache model.
		Phase2CompositeModel p2g = smp.getPhase2CompositeModel();
		logger.create().extractCallInfo().level(2).msg("Located external phase2 composite model").send();

		AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
		logger.create().extractCallInfo().level(2).msg("Located external proposal account synopsis model").send();
		//CachedAccountSynopsisModel casm  =new CachedAccountSynopsisModel(pasm);
		
		
		ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();
		logger.create().extractCallInfo().level(2).msg("Located external history synopsis model").send();
		//CachedHistorySynopsisModel chsm = new CachedHistorySynopsisModel(hsm);
		
		
		// Env snapshot
		EnvironmentSnapshot env = new EnvironmentSnapshot(time, 0.3, EnvironmentSnapshot.EXTINCTION_PHOTOM);

		// Disruptor list
		List<Disruptor> disruptors = new Vector<Disruptor>();
		// TODO find any fixed groups in the coming night.
		
		
		int ng = 0;
		List<GroupItem> glist = p2g.listGroups();
		int ngcheck = glist.size();
		// tell the listeners we are starting and how many groups we will test.
		notifyListenersPrescanClear(ngcheck);
		System.err.printf("Starting prescan with %4d groups\n", glist.size());
		Iterator<GroupItem> ig = glist.iterator();
		while (ig.hasNext()) {

			GroupItem group = ig.next();
			// logger.create().extractCallInfo().level(2)
			// .msg("Checking group: "+group.getName()).send();
			ng++;
			// test group for feasibility over the current or coming night
			// run over s-e at 1 minute intervals

			try {

				AccountSynopsis accounts = pasm.getAccountSynopsis(group.getProposal().getID(), time);

				ExecutionHistorySynopsis history = hsm.getExecutionHistorySynopsis(group.getID(), time);

				ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(group);
				ExecutionResource timeUsage = xrb.getResource("TIME");
				double execTime = timeUsage.getResourceUsage();

				System.err.printf("Processing group %4d of %4d %s xt %4.2fm \n",ng, glist.size(), group.getName(), (execTime/60000.0));
				PrescanEntry pse = new PrescanEntry(start, end, interval);
				pse.group = group;
				pse.execTime = execTime;

				long t = start;
				int nft = 0; // total feasible time slots
				int ic = 0; // feasible slots in block, upto 10
				int it = 0; // slot number
				StringBuffer buff = new StringBuffer();
				while (t < end) {
					// make a fake snapshot for night, ignore disruptors for now
					if (xfm.isitFeasible(group, t, history, accounts, env, disruptors).isFeasible()) {
						nft++;
						ic++;
						pse.setFeasible(t);
					}
					if (it % 10 == 9) {
						if (ic == 0) {
							buff.append("-");
						} else {
							if (group.getTimingConstraint() instanceof XFixedTimingConstraint)
								buff.append("F");
							else
								buff.append("" + (ic - 1));
						}
						ic = 0;
					}
					it++;
					t += interval;
				} // next slot

				// if there is any feasible time we want a mapping entry here...
				if (nft == 0) {
					// logger.create().extractCallInfo().level(2)
					// .msg("Group is NOT feasible tonight").send();
					// TODO maybe we want listeners to know this one is not on the list so it
					// can keep track of progress. It will have nx =0 (and not add to prescan list)
					//notifyListenersPrescanUpdate(pse);
				} else {
					logger.create().extractCallInfo().level(2)
							.msg(
									"Group " + group.getName() + " is feasible for: " + (nft*interval/60000)
											+ " minutes during remaining night").send();

					int execmins = (int) (execTime / 60000.0);

					String gname = GenomeMapper.newName();

					// how many execs?
					int nx = 1;
					ITimingConstraint timing = group.getTimingConstraint();
					if (timing instanceof XMonitorTimingConstraint) {
						long period = ((XMonitorTimingConstraint) timing).getPeriod();
						nx = (int) ((double) (nft * 60000) / (double) period) + 1;
					} else if (timing instanceof XMinimumIntervalTimingConstraint) {
						long period = ((XMinimumIntervalTimingConstraint) timing).getMinimumInterval();
						nx = (int) ((double) (nft * 60000) / (double) period) + 1;
					}

					String tc = "U";
					if (timing instanceof XMonitorTimingConstraint)
						tc = "M";
					else if (timing instanceof XMinimumIntervalTimingConstraint)
						tc = "I";
					else if (timing instanceof XFlexibleTimingConstraint)
						tc = "G";
					else if (timing instanceof XFixedTimingConstraint)
						tc = "F";
					else if (timing instanceof XEphemerisTimingConstraint)
						tc = "P";

					// feasibility plot line
					logger.create().extractCallInfo().level(2).msg(gname + " : " + buff.toString()).send();

					String ssee = "";
					ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);
					XSeeingConstraint see = oca.getSeeingConstraint();
					double maxsee = see.getSeeingValue();
					int seecat = EnvironmentSnapshot.getSeeingCategory(maxsee);
					
					switch (seecat) {
					case EnvironmentSnapshot.SEEING_POOR:
						ssee = "P";
						break;
					case EnvironmentSnapshot.SEEING_AVERAGE:
						ssee = "A";
						break;
					case EnvironmentSnapshot.SEEING_EXCELLENT:
						ssee = "X";
						break;
					case EnvironmentSnapshot.SEEING_USABLE:
						ssee = "U";
						break;
					default:
						ssee = "?";
						break;
					}

					if (nx == 1)
						seq.add(String.format("%3.3s %3d       %s ( %20.20s ) : %s [%s]", gname, execmins, tc, group
								.getName(), buff.toString(), ssee));
					else
						seq.add(String.format("%3.3s %3d [x%-2d] %s ( %20.20s ) : %s [%s]", gname, execmins, nx, tc,
								group.getName(), buff.toString(), ssee));

					pse.nx = nx;
					pse.gname = gname;
					prescan.add(pse);

					logger.create().extractCallInfo().level(2).msg(gname + " = " + pse.display()).send();

					System.err.printf("%s = %s", gname, pse.display());
					notifyListenersPrescanUpdate(pse);

				}
			} catch (NullPointerException nx) {

			} catch (Exception e) {
				// logger.create().extractCallInfo().error().level(3)
				// .msg("Error processing group: "+ e).send();
			}

		} // next group

		long endScan = System.currentTimeMillis();
		logger.create().extractCallInfo().level(2).msg("Checked " + ng + " groups in " + (endScan - startScan) + " ms")
				.send();

		notifyListenersPrescanCompleted();
		
		Iterator it = seq.iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			System.err.println(s);
		}

		smslogger.setLogLevel(oldlevel);

		return prescan;

	}

	/** Execute a prescan for the specified group based on the specified time. */
	public PrescanEntry prescan(GroupItem group, long time, long interval) throws RemoteException {

		AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);

		SolarCalculator sun = null;
		Coordinates sunTrack = null;
		double sunlev;
		try {
			sun = new SolarCalculator();
			sunTrack = sun.getCoordinates(time);
			sunlev = astro.getAltitude(sunTrack, time);
		} catch (AstrometryException ax) {
			throw new RemoteException("Error determining solar position", ax);
		}

		logger.create().extractCallInfo().level(2).msg("Sun elevation is: " + Math.toDegrees(sunlev)).send();
		boolean daytime = (sunlev > 0.0);

		long start = 0L;
		long end = 0L;
		if (daytime) {
			logger.create().extractCallInfo().level(2).msg("It is currently daytime").send();
			try {
				start = time + astro.getTimeUntilNextSet(sunTrack, 0.0, time);
				end = start + astro.getTimeUntilNextRise(sunTrack, 0.0, start);
			} catch (AstrometryException ax) {
				throw new RemoteException("Error determining time limits", ax);
			}
		} else {
			logger.create().extractCallInfo().level(2).msg("It is currently nighttime").send();
			try {
				start = time;
				end = time + astro.getTimeUntilNextRise(sunTrack, 0.0, time);
			} catch (AstrometryException ax) {
				throw new RemoteException("Error determining time limits", ax);
			}
		}
		logger.create().extractCallInfo().level(2).msg(
				"Start prescan at: " + sdf.format(new Date(start)) + ", End at: " + sdf.format(new Date(end))
						+ " Night duration: " + ((end - start) / 3600000) + "H").send();

		// need the following astrocalc, site, exresmodl, cam, telsyn,
		// imstsynmodl
		AstrometryCalculator astrocalc = new BasicAstrometryCalculator();

		InstrumentSynopsisModel oism = new OptimisticInstrumentSynopsisModel(ism);

		StandardExecutionFeasibilityModel xfm = new StandardExecutionFeasibilityModel(astrocalc, xrm, cam, site, tel,
				oism);

		// Check all groups from cache model.
		Phase2CompositeModel p2g = smp.getPhase2CompositeModel();
		logger.create().extractCallInfo().level(2).msg("Located external phase2 composite model").send();

		AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
		logger.create().extractCallInfo().level(2).msg("Located external proposal account synopsis model").send();

		ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();
		logger.create().extractCallInfo().level(2).msg("Located external history synopsis model").send();

		// Env snapshot
		EnvironmentSnapshot env = new EnvironmentSnapshot(time, 0.3, 
				EnvironmentSnapshot.EXTINCTION_PHOTOM);

		// Disruptor list
		List<Disruptor> disruptors = new Vector<Disruptor>();

		AccountSynopsis accounts = pasm.getAccountSynopsis(group.getProposal().getID(), time);

		ExecutionHistorySynopsis history = hsm.getExecutionHistorySynopsis(group.getID(), time);

		ExecutionResourceBundle xrb = xrm.getEstimatedResourceUsage(group);
		ExecutionResource timeUsage = xrb.getResource("TIME");
		double execTime = timeUsage.getResourceUsage();

		PrescanEntry pse = new PrescanEntry(start, end, interval);

		pse.group = group;
		pse.execTime = execTime;

		long t = start;
				
		int nft = 0; // total feasible time slots
		int ic = 0; // feasible slots in block, upto 10
		int it = 0; // slot number
		
		StringBuffer buff = new StringBuffer();
		while (t < end) {
			// make a fake snapshot for night, ignore disruptors for now
			if (xfm.isitFeasible(group, t, history, accounts, env, disruptors).isFeasible()) {
				nft++;
				ic++;
				pse.setFeasible(t);
			}
			if (it % 10 == 9) {
				if (ic == 0) {
					buff.append("-");
				} else {
					if (group.getTimingConstraint() instanceof XFixedTimingConstraint)
						buff.append("F");
					else
						buff.append("" + (ic - 1));
				}
				ic = 0;
			}
			it++;
			t += interval;
		} // next minute

		// if there is any feasible time we want a mapping entry here...
		if (nft == 0) {
			// logger.create().extractCallInfo().level(2)
			// .msg("Group is NOT feasible tonight").send();
		} else {
			logger.create().extractCallInfo().level(2).msg(
					"Group " + group.getName() + " is feasible for: " + (nft*interval/60000) + " minutes during remaining night").send();

			int execmins = (int) (execTime / 60000.0);

			String gname = GenomeMapper.newName();

			// how many execs?
			int nx = 1;
			ITimingConstraint timing = group.getTimingConstraint();
			if (timing instanceof XMonitorTimingConstraint) {
				long period = ((XMonitorTimingConstraint) timing).getPeriod();
				nx = (int) ((double) (nft * 60000) / (double) period) + 1;
			} else if (timing instanceof XMinimumIntervalTimingConstraint) {
				long period = ((XMinimumIntervalTimingConstraint) timing).getMinimumInterval();
				nx = (int) ((double) (nft * 60000) / (double) period) + 1;
			}

			String tc = "U";
			if (timing instanceof XMonitorTimingConstraint)
				tc = "M";
			else if (timing instanceof XMinimumIntervalTimingConstraint)
				tc = "I";
			else if (timing instanceof XFlexibleTimingConstraint)
				tc = "G";
			else if (timing instanceof XFixedTimingConstraint)
				tc = "F";
			else if (timing instanceof XEphemerisTimingConstraint)
				tc = "P";

			// feasibility plot line
			logger.create().extractCallInfo().level(2).msg(gname + " : " + buff.toString()).send();

			String ssee = "";
			ObservingConstraintAdapter oca = new ObservingConstraintAdapter(group);
			XSeeingConstraint see = oca.getSeeingConstraint();
			double maxsee = see.getSeeingValue();
			int seecat = EnvironmentSnapshot.getSeeingCategory(maxsee);
			
			switch (seecat) {
			case EnvironmentSnapshot.SEEING_POOR:
				ssee = "P";
				break;
			case EnvironmentSnapshot.SEEING_AVERAGE:
				ssee = "A";
				break;
			case EnvironmentSnapshot.SEEING_EXCELLENT:
				ssee = "X";
				break;
			case EnvironmentSnapshot.SEEING_USABLE:
				ssee = "U";
				break;
			default:
				ssee = "?";
				break;
			}
			
			pse.nx = nx;
			pse.gname = gname;

			logger.create().extractCallInfo().level(2).msg(gname + " = " + pse.display()).send();

			notifyListenersPrescanUpdate(pse);

		}

		long endScan = System.currentTimeMillis();

		return pse;

	}

	public void addFeasibilityPrescanUpdateListener(FeasibilityPrescanUpdateListener l) throws RemoteException {
		logger.create().extractCallInfo().level(2).msg(
				"List size: " + listeners.size() + ", requested to add listener: " + l).send();
		if (listeners.contains(l))
			return;
		logger.create().extractCallInfo().level(2).msg("Adding listener: " + l).send();
		listeners.add(l);

	}

	public void removeFeasibilityPrescanUpdateListener(FeasibilityPrescanUpdateListener l) throws RemoteException {
		logger.create().extractCallInfo().level(2).msg(
				"List size: " + listeners.size() + ", requested to remove listener: " + l).send();
		if (!listeners.contains(l))
			return;
		logger.create().extractCallInfo().level(2).msg("Removing listener: " + l).send();
		listeners.remove(l);
	}

	private void notifyListenersPrescanClear(int numberGroups) {
		logger.create().extractCallInfo().level(2).msg("List size: " + listeners.size() + " Notifying prescan starting")
				.send();
		Iterator<FeasibilityPrescanUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			try {
				FeasibilityPrescanUpdateListener f = il.next();
				f.prescanStarting(numberGroups);
			} catch (Exception e) {
				il.remove();
				logger.create().extractCallInfo().level(2).msg("Removed unresponsive listener").send();
			}
		}
	}

	private void notifyListenersPrescanUpdate(PrescanEntry pse) {
		logger.create().extractCallInfo().level(2).msg("List size: " + listeners.size() + " Notifying PSE update")
				.send();

		Iterator<FeasibilityPrescanUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			try {
				FeasibilityPrescanUpdateListener f = il.next();
				f.prescanUpdate(pse);
			} catch (Exception e) {
				il.remove();
				logger.create().extractCallInfo().level(2).msg("Removed unresponsive listener").send();
			}
		}
	}
	
	private void notifyListenersPrescanCompleted() {
		logger.create().extractCallInfo().level(2).msg("List size: " + listeners.size() + " Notifying prescan completed")
				.send();

		Iterator<FeasibilityPrescanUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			try {
				FeasibilityPrescanUpdateListener f = il.next();
				f.prescanCompleted();
			} catch (Exception e) {
				il.remove();
				logger.create().extractCallInfo().level(2).msg("Removed unresponsive listener").send();
			}
		}
	}
}
