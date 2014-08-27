/**
 * 
 */
package ngat.sms;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

import ngat.astrometry.AstroCatalog;
import ngat.astrometry.AstrometrySiteCalculator;
import ngat.astrometry.BasicAstrometrySiteCalculator;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCalculator;
import ngat.oss.listeners.AccountingModelUpdateListener;
import ngat.oss.listeners.Phase2ModelUpdateListener;
import ngat.oss.model.IAccessModel;
import ngat.oss.model.IAccountModel;
import ngat.oss.model.IHistoryModel;
import ngat.oss.model.IPhase2Model;
import ngat.oss.monitor.AccountMonitor;
import ngat.oss.monitor.Phase2Monitor;
import ngat.oss.transport.RemotelyPingable;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.sms.models.standard.BasicPhase2CompositeModel;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class DefaultSynopticModelProvider extends UnicastRemoteObject implements SynopticModelProvider,
		RemotelyPingable {

	private static final long LOADING_RETRY_DELAY = 60000L;

	private static final long REGISTRATION_RETRY_DELAY = 600000L;

	private static final long BINDING_RETRY_DELAY = 30000L;

	/** Provides acc to Base models. */
	private BaseModelProvider bmp;

	private DefaultPhase2LoadContoller dlc;
	
	private BasicHistorySynopsisModel hsm;

	private BasicAccountSynopsisModel pasm;

	private BasicAccountSynopsisModel tasm;
	
	private BaseModelLoader loader;
	
	private BasicPhase2Cache cache;
	
	private ISite site;

	LogGenerator logger;

	/**
	 * Create a DefaultSynopticModelProvider using specified BaseModelProvider.
	 * 
	 * @param bmp
	 *            The BaseModelProvider to use.
	 */
	public DefaultSynopticModelProvider(BaseModelProvider bmp, BaseModelLoader loader, BasicPhase2Cache cache , ISite site) throws RemoteException {
		super();
		this.bmp = bmp;
		this.loader = loader;
		this.cache = cache;
		this.site = site;
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("smp");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SynopticModelProvider#getHistorySynopsisModel()
	 */
	public ExecutionHistorySynopsisModel getHistorySynopsisModel() throws RemoteException {
		if (hsm == null)
			throw new RemoteException("HistorySynopsisModel is not available");
		return hsm;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SynopticModelProvider#getPhase2CompositeModel()
	 */
	public Phase2CompositeModel getPhase2CompositeModel() throws RemoteException {
		if (cache == null)
			throw new RemoteException("Phase2CompositeModel L1 is not available");
		return cache;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SynopticModelProvider#getProposalAccountSynopsisModel()
	 */
	public AccountSynopsisModel getProposalAccountSynopsisModel() throws RemoteException {
		if (pasm == null)
			throw new RemoteException("ProposalAccountSynopsisModel is not available");
		return pasm;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.SynopticModelProvider#getTagAccountSynopsisModel()
	 */
	public AccountSynopsisModel getTagAccountSynopsisModel() throws RemoteException {
		if (tasm == null)
			throw new RemoteException("TagAccountSynopsisModel is not available");
		return tasm;
	}

	/** Bind to local registry. */
	private void bind() {
		// Bind locally
		while (true) {
			try {
				// .msg("").send();
				logger.create().block("bind").info().level(1).msg("Attempting to bind...").send();
				Naming.rebind("rmi://localhost/SynopticModelProvider", this);

				logger.create().block("bind").info().level(1).msg("Bound to local registry as "+this.getClass().getName()).send();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(BINDING_RETRY_DELAY);
			} catch (InterruptedException ix) {
			}
		}
	}

	/** Request the provider to bind, repeatedly. */
	public void asynchBind() {
		Runnable r = new Runnable() {
			public void run() {
				bind();
			}
		};
		(new Thread(r)).start();
	}

	/**
	 * Keep trying to load the base models, then keep on registering for
	 * callbacks.
	 */
	private void loadModels() {

		IPhase2Model phase2Model = null;
		boolean allLoaded = false;
		while (!allLoaded) {

			try {
				Thread.sleep(20000L);
			} catch (InterruptedException ix) {
			}

			// Find access model
			IAccessModel accessModel = null;
			try {
				System.err.println("SMP:Looking for access model...");
				accessModel = bmp.getAccessModel();
				System.err.println("SMP:Found access model");
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			// Load Phase2
			phase2Model = null;
			try {

				logger.create().block("loadModels").info().level(1).msg("Looking for base phase2 model...").send();
				phase2Model = bmp.getPhase2Model();

				//BaseModelLoader loader = new BaseModelLoader(bmp);
				logger.create().block("loadModels").info().level(1).msg("Base model loader ready").send();

				logger.create().block("loadModels").info().level(1).msg("Creating composite wrapper model L1...")
						.send();
				//p2c = new BasicPhase2Cache("L1");

				Phase2Monitor phase2 = (Phase2Monitor) bmp.getPhase2Model();
				phase2.addPhase2UpdateListener(cache);
				logger.create().extractCallInfo().level(1).msg("Registered L1 for phase2 updates from base model")
						.send();

				// logger.create().block("loadModels").info().level(1).msg("Registering p2c for phase2 updates...").send();
				// ((Phase2Monitor) phase2Model).addPhase2UpdateListener(p2c);

				logger.create().block("loadModels").info().level(1).msg("Completed registration").send();

				logger.create().block("loadModels").info().level(1).msg("Loading phase2 cache into L1...").send();
				cache.loadCache(loader);
				// p2c.loadPhase2(accessModel, phase2Model);
				logger.create().block("loadModels").info().level(1).msg("Phase2 cache loaded into L1").send();

				Phase2UpdateProcessor processor = new BaseModelPhase2UpdaterPlugin(cache, bmp);
				cache.startUpdateMonitor(processor);
				logger.create().block("loadModels").info().level(1).msg("Phase2 update monitor started by L1").send();

				// p2c.startUpdateMonitor(accessModel, phase2Model);

			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			// Load the HSM
			boolean hsmLoaded = false;
			while (!hsmLoaded) {
				long time = System.currentTimeMillis();
				try {

					// logger.create().block("loadModels").info().level(1).msg("Looking for history model...").send();
					// IHistoryModel historyModel = bmp.getHistoryModel();

					logger.create().block("loadModels").info().level(1).msg("Found history model").send();
					hsm = new BasicHistorySynopsisModel(bmp);

					logger.create().block("loadModels").info().level(1).msg("Loading history cache...").send();
					hsm.loadHistory(cache, time);

					logger.create().block("loadModels").info().level(1).msg("History cache loaded").send();
					hsmLoaded = true;
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					Thread.sleep(LOADING_RETRY_DELAY);
				} catch (InterruptedException ix) {
				}
			}

			// Load the PASM
			boolean pasmLoaded = false;
			while (!pasmLoaded) {
				long time = System.currentTimeMillis();
				try {

					logger.create().block("loadModels").info().level(1).msg("Looking for proposal account model...")
							.send();
					IAccountModel pam = bmp.getProposalAccountModel();
					logger.create().block("loadModels").info().level(1)
							.msg("Found proposal account model: " + pam.getClass().getName()).send();

					logger.create().block("loadModels").info().level(1).msg("Creating synoptic wrapper model...")
							.send();
					pasm = new BasicAccountSynopsisModel(bmp);

					logger.create().block("loadModels").info().level(1).msg("Registering pasm for acc updates...")
							.send();
					AccountMonitor acm = (AccountMonitor) pam;

					acm.addAccountUpdateListener(pasm);

					logger.create().block("loadModels").info().level(1).msg("Loading proposal account cache...").send();
					pasm.loadProposalAccounts(cache, time);

					logger.create().block("loadModels").info().level(1).msg("Proposal account cache loaded").send();

					pasm.startUpdateMonitor();
					logger.create().block("loadModels").info().level(1).msg("Account update monitor started by pasm")
							.send();

					pasmLoaded = true;
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					Thread.sleep(LOADING_RETRY_DELAY);
				} catch (InterruptedException ix) {
				}
			}

			// Load the TASM
			boolean tasmLoaded = false;
			while (!tasmLoaded) {
				long time = System.currentTimeMillis();
				try {

					logger.create().block("loadModels").info().level(1).msg("Looking for TAG account model...").send();
					IAccountModel tam = bmp.getTagAccountModel();

					logger.create().block("loadModels").info().level(1).msg("Found TAG account model").send();
					tasm = new BasicAccountSynopsisModel(bmp);

					logger.create().block("loadModels").info().level(1).msg("Registering tasm for acc updates...")
							.send();
					((AccountMonitor) tam).addAccountUpdateListener(tasm);

					logger.create().block("loadModels").info().level(1).msg("Loading TAG account cache...").send();
					tasm.loadTagAccounts(cache, time);

					logger.create().block("loadModels").info().level(1).msg("TAG account cache loaded").send();

					tasm.startUpdateMonitor();
					logger.create().block("loadModels").info().level(1).msg("Account update monitor started by tasm")
							.send();

					tasmLoaded = true;
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					Thread.sleep(LOADING_RETRY_DELAY);
				} catch (InterruptedException ix) {
				}
			}

			allLoaded = true;
		}

		// at this stage all models are hopefully loaded lets test the groups
		// now

		try {
			logger.create().block("loadModels:prune").info().level(1).msg("Start cache pruning...").send();

			int nfg = 0;
			int ng = 0;
			long now = System.currentTimeMillis();

			SolarCalculator sun = new SolarCalculator();
			Coordinates sunnow = sun.getCoordinates(now);
			AstrometrySiteCalculator astro = new BasicAstrometrySiteCalculator(site);

			long swindow = 0L;
			long ewindow = 0L;
			long delta = 10 * 60 * 1000L;
			if (astro.getAltitude(sunnow, now) > 0.0) {
				// sun is up get time of next sunset and following sunrise
				long ttsunset = astro.getTimeUntilNextSet(sunnow, 0.0, now);
				long sunset = now + ttsunset;
				Coordinates sunatset = sun.getCoordinates(sunset);
				long ttsunrise = astro.getTimeUntilNextRise(sunatset, 0.0, sunset + delta);
				swindow = sunset;
				ewindow = sunset + ttsunrise - delta;

				logger.create()
						.block("loadModels:prune")
						.info()
						.level(1)
						.msg(String.format("Next night window: %tF %tT  - %tF %tT", swindow, swindow, ewindow, ewindow))
						.send();
			} else {
				// sun is down get next sunrise
				swindow = now;
				long ttsunrise = astro.getTimeUntilNextRise(sunnow, 0.0, now);
				ewindow = now + ttsunrise;

				logger.create()
						.block("loadModels:prune")
						.info()
						.level(1)
						.msg(String.format("Remaining night window: %tF %tT  - %tF %tT", swindow, swindow, ewindow,
								ewindow)).send();
			}

			List<GroupItem> groups = cache.listGroups();
			Iterator<GroupItem> ig = groups.iterator();
			while (ig.hasNext()) {
				ng++;
				GroupItem group = ig.next();
				ITimingConstraint timing = group.getTimingConstraint();
				ExecutionHistorySynopsis history = hsm.getExecutionHistorySynopsis(group.getID(), now);
				long last = history.getLastExecution();
				int count = history.getCountExecutions();

				// lets see if were done now...
				if (timing instanceof XFlexibleTimingConstraint) {
					XFlexibleTimingConstraint xflex = (XFlexibleTimingConstraint) timing;
					if (count > 0 && last > xflex.getStartTime()) {
						nfg++;
						logger.create().block("loadModels:prune").info().level(1)
								.msg("Reject completed flexible group: " + group.getName()).send();
					}
				} else if (timing instanceof XMinimumIntervalTimingConstraint) {
					XMinimumIntervalTimingConstraint xmin = (XMinimumIntervalTimingConstraint) timing;
					if (count >= xmin.getMaximumRepeats()) {
						nfg++;
						logger.create().block("loadModels:prune").info().level(1)
								.msg("Reject completed interval group: " + group.getName()).send();
					}
				} else if (timing instanceof XFixedTimingConstraint) {
					XFixedTimingConstraint xfixed = (XFixedTimingConstraint) timing;
					if (count > 0 && last > xfixed.getEndTime()) {
						nfg++;
						logger.create().block("loadModels:prune").info().level(1)
								.msg("Reject completed fixed group: " + group.getName()).send();
					}
				} else if (timing instanceof XMonitorTimingConstraint) {
					XMonitorTimingConstraint xmon = (XMonitorTimingConstraint) timing;
					long startDate = xmon.getStartDate();
					long endDate = xmon.getEndDate();
					long period = xmon.getPeriod();
					long window = xmon.getWindow();

					// how many periods to consider ?
					int nperiod = (int) ((double) (ewindow - swindow) / (double) period) + 1;

					// what period are we in or near (before).
					double floatFraction = (double) window / (double) period;

					double fPeriod = (double) (now - startDate) / (double) period;
					double iPeriod = Math.floor(fPeriod);
					int ipstart = (int) iPeriod;
					int nhit = 0;
					for (int ip = ipstart; ip < ipstart + nperiod; ip++) {
						// check period number ip
						long startFloat = startDate + (long) ((ip - (double) floatFraction / 2.0) * (double) period);
						long endFloat = startDate + (long) ((ip + (double) floatFraction / 2.0) * (double) period);
						if (startFloat < ewindow && endFloat > swindow)
							nhit++;
					}

					if (nhit == 0) {
						logger.create().block("loadModels:prune").info().level(1)
								.msg("Reject monitor group: " + group.getName()).send();
						nfg++;
					}

				}
			}
			logger.create().block("loadModels:prune").info().level(1)
					.msg("Rejected a total of: " + nfg + " groups out of " + ng + " candidates").send();

			dlc = new DefaultPhase2LoadContoller(cache, loader, pasm, hsm);
			logger.create().block("loadModels").info().level(1)
			.msg("Created load controller: "+dlc).send();
			dlc.asynchBind();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}
	
	
	/**
	 * Keep on re-registering for callbacks from external base models.
	 */
	private void reregister() {

		while (true) {
			try {

				logger.create().block("loadModels").info().level(1).msg("Re-registering L1 for phase2 updates...")
						.send();

				Phase2Monitor phase2Monitor = (Phase2Monitor) bmp.getPhase2Model();
				phase2Monitor.addPhase2UpdateListener(cache);
				logger.create().block("reregister").info().level(1).msg("Re-registered L1 with remote provider").send();

				IAccountModel pam = bmp.getProposalAccountModel();
				AccountMonitor pacm = (AccountMonitor) pam;
				pacm.addAccountUpdateListener(pasm);
				logger.create().block("reregister").info().level(1).msg("Re-registered PASM with remote provider")
						.send();

			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(REGISTRATION_RETRY_DELAY);
			} catch (InterruptedException ix) {
			}
		}

	}

	/**
	 * Request the provider to load its models asynchronously.
	 * 
	 * @param timeout
	 *            How long to wait for load to complete. 0 = forever.
	 */
	public void asynchLoadModels(long timeout) {
		Runnable r = new Runnable() {
			public void run() {
				loadModels();
			}
		};
		Thread t = new Thread(r);
		t.run();
		if (timeout < 0)
			return;
		try {
			t.join(timeout);
		} catch (InterruptedException ix) {
		}
	}

	/**
	 * Request the provider to load its models asynchronously. This method
	 * returns immediately.
	 */
	public void asynchLoadModels() {
		asynchLoadModels(-1);
	}

	public void asynchReregister() {
		Runnable r = new Runnable() {
			public void run() {
				reregister();
			}
		};
		Thread t = new Thread(r);
		t.run();
	}

	/** Delegate rebinding to DLC's own asynch rebind.*/
	public void asynchBindLoadController() {
		dlc.asynchBind();		
	}
	
	
	public void ping() throws RemoteException {
		logger.create().block("ping").info().level(1).msg("Ping received").send();
	}

	public Phase2LoadController getLoadController() throws RemoteException {
		return dlc;
	}

}
