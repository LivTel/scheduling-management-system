/**
 * 
 */
package ngat.sms.simulation.test;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.ems.DefaultMutableSkyModel;
import ngat.ems.SkyModel;
import ngat.icm.InstrumentRegistry;
import ngat.phase2.IExecutionFailureContext;
import ngat.phase2.IQosMetric;
import ngat.phase2.XBasicExecutionFailureContext;
import ngat.phase2.XTimePeriod;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BaseModelLoader;
import ngat.sms.BaseModelProvider;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.sms.BasicPhase2Cache;
import ngat.sms.BasicSkyModelProvider;
import ngat.sms.BasicSynopticModelProxy;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.CachedAccountSynopsisModel;
import ngat.sms.CachedHistorySynopsisModel;
import ngat.sms.CachedSynopticModelProvider;
import ngat.sms.ChargeAccountingModel;
import ngat.sms.DefaultBaseModelProvider;
import ngat.sms.DefaultDisruptionGenerator;
import ngat.sms.DefaultExecutionUpdateManager;
import ngat.sms.DefaultMutableSkyModelProvider;
import ngat.sms.DefaultMutableTimeModel;
import ngat.sms.DefaultSynopticModelProvider;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentPredictionModel;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.ExecutionUpdateManager;
import ngat.sms.ExecutionUpdater;
import ngat.sms.GroupItem;
import ngat.sms.OptimisticLiveInstrumentSynopsisModel;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.ScheduleDespatcher;
import ngat.sms.ScheduleItem;
import ngat.sms.SchedulingStatusUpdateListener;
import ngat.sms.ScoreMetricsSet;
import ngat.sms.SkyModelProvider;
import ngat.sms.SynopticModelProvider;
import ngat.sms.TimeModel;
import ngat.sms.VetoManager;
import ngat.sms.WeatherPredictionModel;
import ngat.sms.WeatherSnapshot;
import ngat.sms.bds.TestResourceUsageEstimator;
import ngat.sms.bds.BasicDespatchScheduler;
import ngat.sms.legacy.GroupRef;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.sms.models.standard.StandardExecutionFeasibilityModel;
import ngat.sms.simulation.BasicStochasticExecutionTimingModel;
import ngat.sms.simulation.DisruptionGenerator;
import ngat.sms.simulation.ScheduleSimulator;
import ngat.sms.simulation.SimulationEventListener;
import ngat.sms.simulation.StochasticExecutionTimingModel;
import ngat.sms.simulation.TimeSignalGenerator;
import ngat.sms.simulation.TimeSignalListener;
import ngat.sms.simulation.VolatilityGenerator;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class TestSimApp1 implements TimeSignalGenerator, WeatherPredictionModel, EnvironmentPredictionModel,
				    SimulationEventListener {

    /** stochastic timing model.*/
    private StochasticExecutionTimingModel xtm;
	
    private ExecutionUpdateManager xm;
	
    private SynopticModelProvider smp;
	
    private DefaultMutableTimeModel timeModel;
	
    List<String> sequence;
	
    long xt = 0L;
	
    /**
     * @param xrm
     */
    public TestSimApp1(StochasticExecutionTimingModel xtm, ExecutionUpdateManager xm, SynopticModelProvider smp, DefaultMutableTimeModel timeModel) {
	super();	
	this.xtm = xtm;
	this.xm = xm;
	this.smp = smp;
	this.timeModel = timeModel;
	sequence = new Vector<String>();
    }

    /**
     * @see ngat.sms.simulation.TimeSignalGenerator#awaitTimingSignal(ngat.sms.simulation.TimeSignalListener,
     *      long)
     */
    public void awaitTimingSignal(TimeSignalListener tsl, long time) {
	final long ftime = time;
	final TimeSignalListener ftsl = tsl;
	Runnable r = new Runnable() {
		public void run() {
		    timeModel.setTime(ftime);
		    ftsl.timingSignal(ftime);
		}
	    };
	(new Thread(r)).start();
    }

    /**
     * @see ngat.sms.TimeModel#getTime()
     */
    public long getTime() {
	return timeModel.getTime();
    }

    /**
     * @see ngat.sms.WeatherPredictionModel#predictWeather(long)
     */
    public WeatherSnapshot predictWeather(long time) throws Exception {
	WeatherSnapshot weather = new WeatherSnapshot(time, true);
	return weather;
    }

    /**
     * @see ngat.sms.EnvironmentPredictionModel#predictEnvironment(long)
     */
    public EnvironmentSnapshot predictEnvironment(long time) throws Exception {
	EnvironmentSnapshot env = new EnvironmentSnapshot(time, EnvironmentSnapshot.SEEING_AVERAGE,
							  EnvironmentSnapshot.EXTINCTION_PHOTOM);
	return env;
    }

    /**
     * @see ngat.sms.simulation.SimulationEventListener#contentionResults(int)
     */
    public void contentionResults(int contention) {		
    }

    /**
     * @see ngat.sms.simulation.SimulationEventListener#groupSelected(ngat.sms.ScheduleItem)
     */
    public void groupSelected(ScheduleItem item) {
	
	GroupItem group = item.getGroup();
		
	if (group != null) {
	    long time = getTime();
	    System.err.println("T1::Group selected at: " + ScheduleSimulator.sdf.format(new Date(time)) + ""
			       + group.getName()+" using HID: "+group.getHId());

	    long exec = xtm.getExecutionTime(group);
		
	    // TODO Update History synopsis
	    // TODO Update account synopsis
	    // TODO Maybe update sky and weather prediction models 
	    // TODO Maybe update instrument and scope synopses
			
	    //  Advance time seen by scheduler/xfm and ss.
		
	    sequence.add(String.format("%tF %tT %s",time, time, group.getName()));
	    xt += exec;
	}
    }
	
    /**
     * @see ngat.sms.simulation.SimulationEventListener#simulationCompleted()
     */
    public void simulationCompleted() {
	System.err.println("T1:: Sim completed at: " + ScheduleSimulator.sdf.format(new Date(getTime())));
	System.err.println("T1:: Sequence was: "+sequence);
	System.err.println("T1:: Total exec time: "+(xt/60000)+"m");
	sequence.clear();
	xt = 0L;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

	try {

	    Logger alogger = LogManager.getLogger("SIM");
	    ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
	    console.setLogLevel(3);
	    alogger.setLogLevel(3);
	    alogger.addExtendedHandler(console);

	    Logger blogger = LogManager.getLogger("SMS");
	    blogger.setLogLevel(2);			
	    blogger.addExtendedHandler(console);

	    LogGenerator logger = blogger.generate()
		.system("SMS")
		.subSystem("Simulation")
		.srcCompClass("TestSimApp1");
			
	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

	    double lat = Math.toRadians(cfg.getDoubleValue("lat", 28.0));
	    double lon = Math.toRadians(cfg.getDoubleValue("long", -17.0));

	    ISite site = new BasicSite("obs", lat, lon);

	    String bmpHost = cfg.getProperty("bmp", "localhost");
			
	    /// START

	    DefaultMutableTimeModel timeModel = new DefaultMutableTimeModel();

	    // Base models.
	    BaseModelProvider bmp = new DefaultBaseModelProvider(bmpHost);
			
	    // Base synoptic is loaded remotely	
	    
	    BasicPhase2Cache cache = new BasicPhase2Cache("L1");
		BaseModelLoader loader = new BaseModelLoader(bmp);
		
	    DefaultSynopticModelProvider bsmp = new DefaultSynopticModelProvider(bmp, loader, cache, site);
	    bsmp.asynchLoadModels();
			
	    // NOTE: we will actually use cached versions of all these except p2c.
	    Phase2CompositeModel p2c = bsmp.getPhase2CompositeModel();
			
	    AccountSynopsisModel pasm = bsmp.getProposalAccountSynopsisModel();
	    CachedAccountSynopsisModel cpasm = new CachedAccountSynopsisModel(pasm);
			
	    AccountSynopsisModel tasm = bsmp.getTagAccountSynopsisModel();
	    CachedAccountSynopsisModel ctasm = new CachedAccountSynopsisModel(tasm);
			
	    ExecutionHistorySynopsisModel hsm = bsmp.getHistorySynopsisModel();
	    CachedHistorySynopsisModel chsm = new CachedHistorySynopsisModel(hsm);
			
	    CachedSynopticModelProvider csmp = new CachedSynopticModelProvider();
	    csmp.setPhase2CompositeModel(p2c);
	    csmp.setProposalAccountSynopsisModel(cpasm);
	    csmp.setTagAccountSynopsisModel(ctasm);
	    csmp.setHistorySynopsisModel(chsm);
			
	    // NOTE: Clear out any cached data, need to do this for each sim run...
	    csmp.clearCaches();
			
	    // Charging
	    ChargeAccountingModel cam = new StandardChargeAccountingModel();
			
	    // Create execution update manager
	    DefaultExecutionUpdateManager xm = new DefaultExecutionUpdateManager(csmp, cam);
		
	    // Astrometry
	    BasicAstrometryCalculator calc = new BasicAstrometryCalculator();

	    // Exec resources
	    ExecutionResourceUsageEstimationModel xrm = new TestResourceUsageEstimator();
			
	    // Stochastic timing
	    StochasticExecutionTimingModel xtm = new BasicStochasticExecutionTimingModel(xrm);

	    // Instrumentals, we need some of the inst caps
	    String iHost = cfg.getProperty("inst", "ltsim1");
	    String iregUrl = "rmi://" + iHost + "/InstrumentRegistry";
	    InstrumentRegistry ireg = (InstrumentRegistry)Naming.lookup(iregUrl);
			
	    OptimisticLiveInstrumentSynopsisModel olism = new OptimisticLiveInstrumentSynopsisModel();
	    olism.loadFromRegistry(ireg);

	    // Telescope - grab the AG status
	    BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();			
	    scope.setDomeLimit(Math.toRadians(25.0));
	    // autoguider is always functional
	    scope.setAutoguiderStatus(true);
	    scope.setAutoguiderTempStatus(true);
		
	    StandardExecutionFeasibilityModel bxm = new StandardExecutionFeasibilityModel(calc, xrm, cam,
											  site, scope, olism);
		
	    // Skymodel - this should be defined locally
	    DefaultMutableSkyModel skyModel = new DefaultMutableSkyModel(1, 15*60*1000L);
	    // extinction photometric
	    skyModel.updateExtinction(0.0, 700.0, 0.5 * Math.PI, 0.0, System.currentTimeMillis(), true);
	    // seeing excellent
	    skyModel.updateSeeing(0.5, 700.0, 0.5*Math.PI, 0.0, System.currentTimeMillis(), true, "test", "star");
			
	    SkyModelProvider skyp = new DefaultMutableSkyModelProvider(skyModel);
			
	    // SchedulingStatusProvider
	    BasicDespatchScheduler tsd = new BasicDespatchScheduler(timeModel, xm, bxm, csmp, skyp, site);
			
	    SchedulingStatusUpdateListener l =new SchedulingStatusUpdateListener() {
				
		    int nc = 0;
		 
		    public void scheduleSweepStarted(long time, int sweepId) throws RemoteException {
			System.err.printf("SSU: Sweep started: %tF %tT\n",time,time);	
			nc = 0;
		    }
				
		    public void candidateSelected(long time, ScheduleItem schedule) throws RemoteException {
			System.err.printf("SSU: Candidate selected from %4d: %s\n", nc, schedule != null ?schedule.getGroup():"null");
		    }
				
		    public void candidateAdded(String qId, GroupItem group, ScoreMetricsSet metrics, double score, int rank)
			throws RemoteException {
			String gname = group != null ? group.getName():"null";
			nc++;
			System.err.printf("SSU: Candidate %4d added: %s %s %4.2f %4d\n",nc, qId, gname, score, rank);					
		    }

			public void candidateRejected(String qId, GroupItem group, String reason) throws RemoteException {
				// TODO Auto-generated method stub
				
			}

			public void schedulerMessage(int messageType, String message)
					throws RemoteException {
				// TODO Auto-generated method stub
				
			}
		};
			
	    tsd.addSchedulingUpdateListener(l);
			
	    Map<Long, GroupRef> gmap = new HashMap<Long, GroupRef>();
	    VetoManager vetoManager = (VetoManager) tsd;
			
	    long start = ScheduleSimulator.sdf.parse(cfg.getProperty("start")).getTime();
	    long end = ScheduleSimulator.sdf.parse(cfg.getProperty("end")).getTime();
	    long period = end - start;
			
	    // TODO temporary fake model, does nothing
	    VolatilityGenerator volgen = new VolatilityGenerator() {				
		    public void reset() {
			// TODO Auto-generated method stub					
		    }
				
		    public void fireEvents(long time1, long time2) {
			// TODO Auto-generated method stub					
		    }
		};;;
			
	    // make up some disruption events.
	    DefaultDisruptionGenerator dgen = new DefaultDisruptionGenerator();
	    for (int i = 0; i < 2; i++) {
				
		long ds = start + (long)(Math.random()*(double)period);
		long de = ds + (long)(0.05*Math.random()*(double)period);
		Disruptor dn = new Disruptor("test", "ALT_REBOOT:"+i, new XTimePeriod(ds, de));
		dgen.addDisruptor(dn);
		System.err.println("Add: "+dn);
		//ds = start + (long)(Math.random()*(double)period);
		//de = ds + (long)(0.05*Math.random()*(double)period);
		//Disruptor dw = new Disruptor("test", "BAD_WEATHER:"+i, new XTimePeriod(ds, de));
		//d//gen.addDisruptor(dw);
		//System.err.println("Add: "+dw);
	    }
			
	    timeModel.setTime(start);
	    TestSimApp1 test = new TestSimApp1(xtm, xm, csmp, timeModel);
			
	    // NOTE: site, sched, xrm, wpm, epm, vgen, stm
	    ScheduleSimulator sim = new ScheduleSimulator(site, tsd, xrm, test, test, dgen, volgen, xtm);

	    // extinction photometric
	    skyModel.updateExtinction(0.0, 700.0, 0.5 * Math.PI, 0.0, start, true);
	    // seeing excellent
	    skyModel.updateSeeing(0.5, 700.0, 0.5*Math.PI, 0.0, start, true, "test", "star");

	    for (int i = 0; i < 20; i++) {
		System.err.println("T1:main: Run: "+i);
		sim.runSimulation(test, start, end, test);		
		csmp.clearCaches();
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }
    /** Handle failure of group.*/
    public void groupFailed(ScheduleItem selected, long time, String execFailReason) {
	try {
	    GroupItem g = selected.getGroup();
		
	    ExecutionUpdater xu = xm.getExecutionUpdater(g.getID());
	    IExecutionFailureContext efc = new XBasicExecutionFailureContext(1, execFailReason);
	    xu.groupExecutionAbandoned(g, time, new ExecutionResourceBundle(), efc, new HashSet<IQosMetric>());

	    System.err.printf("T1:: Group: %s failed at %tF %tT due to: %s\n", g.getName(), time, time, execFailReason); 
		
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /** Handle completion of group.*/
    public void groupCompleted(ScheduleItem selected, long time) {
	try {
	    GroupItem g = selected.getGroup();
		
	    ExecutionUpdater xu = xm.getExecutionUpdater(g.getID());
			
	    xu.groupExecutionCompleted(g, time, new ExecutionResourceBundle(), new HashSet<IQosMetric>());
		
	    System.err.printf("T1:: Group: %s completed at  %tF %tT \n", g.getName(), time, time);
			
	} catch (Exception e) {
	    e.printStackTrace();
	}
		
    }

}
