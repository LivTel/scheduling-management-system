package ngat.sms.simulation;

import java.util.*;
import java.text.*;

import javax.security.auth.login.FailedLoginException;

import ngat.astrometry.*;
import ngat.phase2.XProposal;
import ngat.sms.Disruptor;
import ngat.sms.EnvironmentPredictionModel;
import ngat.sms.EnvironmentSnapshot;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.sms.ScheduleDespatcher;
import ngat.sms.ScheduleItem;
import ngat.sms.WeatherPredictionModel;
import ngat.sms.WeatherSnapshot;
import ngat.util.*;
import ngat.util.logging.*;

/**
 * Performs a simulation over one or more nights using a cached ODB for a
 * specified site.
 */
public class ScheduleSimulator {

    /** Standard date formatter. */
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Standard UTC timezone. */
    public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    /** Horizon level (for sunset/sunrise). */
    public static final double HORIZON = Math.toRadians(-1.0);

    static {
	sdf.setTimeZone(UTC);
	Calendar.getInstance().setTimeZone(UTC);
    }

    /** Site location. */
    private ISite site;

    /** Logger. */
    private LogGenerator logger;

    /** Schedule dispatcher. */
    ScheduleDespatcher despatcher;

    /** Execution timing. */
    private ExecutionResourceUsageEstimationModel execTimingModel;

    /** Environment prediction. */
    private EnvironmentPredictionModel envPredictor;

    /** Weather prediction. */
    private WeatherPredictionModel weatherPredictor;

    /** Generate disruptor events. */
    private DisruptionGenerator dgen;

    /** Generate volatility events. */
    private VolatilityGenerator volgen;

    /** Works out the actual exec-time of groups. */
    private StochasticExecutionTimingModel stm;

    /** Site-centred astro calculator. */
    private AstrometrySiteCalculator astro;

    /** Receives time signal callbacks. */
    private DefaultTimeSignalReceiver tsr;

    /**
     * Create a ScheduleSimulator.
     * 
     * @param site
     *            The location where the observatory is sited.
     * @param despatcher
     *            A scheduler.
     * @param execModel
     *            Execution resource model.
     * @param envPredictor
     *            Sky-conditions prediction.
     * @param weatherPredictor
     *            Weather prediction.
     */
    public ScheduleSimulator(ISite site, ScheduleDespatcher despatcher,
			     ExecutionResourceUsageEstimationModel execModel, EnvironmentPredictionModel envPredictor,
			     WeatherPredictionModel weatherPredictor, DisruptionGenerator dgen, VolatilityGenerator volgen,
			     StochasticExecutionTimingModel stm) {

	this.site = site;
	this.despatcher = despatcher;
	this.execTimingModel = execModel;
	this.envPredictor = envPredictor;
	this.weatherPredictor = weatherPredictor;
	this.dgen = dgen;
	this.volgen = volgen;
	this.stm = stm;
	astro = new BasicAstrometrySiteCalculator(site);

	Logger alogger = LogManager.getLogger("SIM");
	logger = new LogGenerator(alogger);
	logger.system("SMS").subSystem("Simulator").srcCompClass("SchedSim(" + site.getSiteName() + ")").srcCompId(
														   this.getClass().getSimpleName());

    }

    /**
     * Start a simulation run from start to end times.
     * 
     * @param tsg
     *            The TimeSignalGenerator to synchronize with.
     * @param start
     *            simulation start time.
     * @param end
     *            simulation end time.
     * @param sel
     *            A SimulationEventListener to receive progress events.
     */
    public void runSimulation(TimeSignalGenerator tsg, long start, long end, SimulationEventListener sel)
	throws Exception {

	tsr = new DefaultTimeSignalReceiver(tsg);

	int ig = 0;

	SolarCalculator sunTrack = new SolarCalculator();

	long time = start;
	long timeStep = 0L;

	ScheduleItem selected = null;
	boolean execFail = false;
	String execFailReason = null;

	// run over sim period
	while (time < end) {

	    selected = null;

	    EnvironmentSnapshot env = envPredictor.predictEnvironment(time);

	    // If its daytime we should jump to next sunset...
	    Coordinates sun = sunTrack.getCoordinates(time);
	    if (astro.getAltitude(sun, time) > HORIZON) {

		// Calculate next sunset time after current time.
		// Use -1 degs to make sure the bugger has really set
		// as we haven't considered site elevation or refraction.
		long timeTillSunset = astro.getTimeUntilNextSet(sun, HORIZON, time);
		long sunset = time + timeTillSunset;
		timeStep = timeTillSunset;

		// Wait for sunset...
		logger.create().extractCallInfo().info().level(3).msg(
								      String.format("Awaiting SUNSET at %s in %4d m at %tF %tT ", site.getSiteName(),
										    (timeStep / 60000), sunset, sunset)).send();

	    } else {

		// check the weather and other disruptors
		Disruptor d = dgen.hasDisruptor(time);
		if (d != null) {
		    // WeatherSnapshot weather =
		    // weatherPredictor.predictWeather(time);
		    // if (!weather.isGood()) {

		    long timeWhenDisruptionEnds = d.getPeriod().getEnd() + 30000L; // add buffer on
		    timeStep = timeWhenDisruptionEnds - time;

		    // Wait for weather event to complete...
		    logger.create().extractCallInfo().info().level(3).
			msg(String.format("Awaiting end of event %s at %s in %4d m at %tF %tT ", 
					  d.toString(),
					  site.getSiteName(), (timeStep / 60000), 
					  timeWhenDisruptionEnds,
					  timeWhenDisruptionEnds)).send();

		} else {

		    try {

			selected = despatcher.nextScheduledJob();

		    } catch (Exception e) {
			e.printStackTrace();
		    }

		    if (selected == null) {

			timeStep = 150 * 1000L;// 2.5 minutes
			long bgCompletion = time + timeStep;

			// Wait for BG to complete...
			logger
			    .create()
			    .extractCallInfo()
			    .info()
			    .level(3)
			    .msg(
				 String
				 .format(
					 "No groups available, awaiting completion of BG_OBS at %s in %4d m at %tF %tT ",
					 site.getSiteName(), (timeStep / 60000), bgCompletion,
					 bgCompletion)).send();

		    } else {

			// at last weve actually got something to do...
			GroupItem group = selected.getGroup();

			ExecutionResourceBundle xrb = execTimingModel.getEstimatedResourceUsage(group);
			ExecutionResource timeResource = xrb.getResource("TIME");
			long exec = (long) timeResource.getResourceUsage();

			ig++;

			logger.create().extractCallInfo().info().level(2).msg(
									      "Selected Group estimated completion at " + sdf.format(new Date(time + exec))).send();

			sel.groupSelected(selected);

			// now need to decide if it will fail..

			// Disruptor d = disruptorGenerator.firstDisruptor(time,
			// time+exec);
			Disruptor dg = dgen.nextDisruptor(time, time + exec);
			if (dg == null) {
			    execFail = false;
			    timeStep = exec;
			    long groupCompletionTime = time + exec;

			    logger.create().extractCallInfo().info().level(2).msg(
										  String.format("Selected group: %s estimated completion in %6d s at %tF %tT", group
												.getName(), (exec / 1000), groupCompletionTime, groupCompletionTime))
				.send();
			} else {
			    execFail = true;
			    execFailReason = dg.getDisruptorClass()+":"+dg.getDisruptorName();
			    long groupFailureTime = dg.getPeriod().getStart();
			    timeStep = groupFailureTime - time;
						
			    logger.create().extractCallInfo().info().level(2).
				msg(String.format("Selected group: %s will fail in %6d s at %tF %tT due to %s", 
						  group.getName(), 
						  (timeStep / 1000), 
						  groupFailureTime, groupFailureTime, 
						  dg.toString()))
				.send();
			    
			}

		    } // group was selected

		} // weather is good

	    } // it is night-time

	    // fire any volatility events in [t, t+tau]
	    volgen.fireEvents(time, time + timeStep);

	    // find out what time the SimApp has decided to jump onto,
	    // this should be reflected in the TimeModel
	    // used by the despatcher and its xfm.
	    tsr.waitTimeSignal(time + timeStep);
	    time = tsg.getTime();

	    // TODO if we are running a group, update group info in history 
	    // - controller will do this --- hopefully,
			
	    if (selected != null) {
		if (execFail) { 
		    sel.groupFailed(selected, time, execFailReason); 
		} else { 
		    sel.groupCompleted(selected, time); 
		}
	    }

	} // next time step

	// end of simulation
	sel.simulationCompleted();

    }
} // [ScheduleSimulator]
