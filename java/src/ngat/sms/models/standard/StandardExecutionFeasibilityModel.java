package ngat.sms.models.standard;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.Instrument;

import ngat.astrometry.BasicAstrometryCalculator;
import ngat.astrometry.AstrometryCalculator;
import ngat.astrometry.AstrometryException;
import ngat.astrometry.Coordinates;
import ngat.astrometry.ISite;
import ngat.astrometry.LunarCalculator;
import ngat.astrometry.SkyBrightnessCalculator;
import ngat.astrometry.SolarCalculator;
import ngat.astrometry.AstrometryTargetCalculator;
import ngat.astrometry.BasicTargetCalculator;
import ngat.astrometry.TargetTrackCalculator;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.phase2.*;
import ngat.sms.*;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

public class StandardExecutionFeasibilityModel implements ExecutionFeasibilityModel, Serializable {

	// TODO - Not clear where these items should come from but should be
	// configurable.
	// public static final double DOME_LOW_LIMIT = Math.toRadians(25.0);

	/** Zenith exclusion zone size (rads). */
	// public static final double ZAZ_LIMIT = Math.toRadians(2.0);

	/**
	 * How long before a fixed groups start-of-window do we want other groups to
	 * have finished (ms).
	 */
	public static final long FG_BUFFER = 3 * 60 * 1000L;

	private LogGenerator logger;

	private AstrometryCalculator astro;
	private ExecutionResourceUsageEstimationModel execModel;
	private ISite site;

	private TelescopeSystemsSynopsis telescope;

	private InstrumentSynopsisModel ism;

	private ChargeAccountingModel chargeModel;

	private ConfigChecker checker;

	private SkyBrightnessCalculator skycalc;

	private boolean ignoreAccounts = false;

	private boolean ignoreBeamSteering = false;

	private boolean ignoreRotator = false;

	private boolean ignoreInstruments = false;

	/**
     * 
     */
	public StandardExecutionFeasibilityModel(AstrometryCalculator astro,
			ExecutionResourceUsageEstimationModel execModel, ChargeAccountingModel chargeModel, ISite site,
			TelescopeSystemsSynopsis telescope, InstrumentSynopsisModel ism) {
		this.astro = astro;
		this.execModel = execModel;
		this.chargeModel = chargeModel;
		this.site = site;
		this.telescope = telescope;
		this.ism = ism;

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("FCL").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("xfm");

		logger.create().info().level(1).msg("Creating XFM").send();
		checker = new ConfigChecker(execModel, ism, telescope, site);
		skycalc = new SkyBrightnessCalculator(site);
	}

	/**
	 * @return the ignoreAccounts
	 */
	public boolean isIgnoreAccounts() {
		return ignoreAccounts;
	}

	/**
	 * @param ignoreAccounts
	 *            the ignoreAccounts to set
	 */
	public void setIgnoreAccounts(boolean ignoreAccounts) {
		this.ignoreAccounts = ignoreAccounts;
	}

	/**
	 * @return the ignoreBeamSteering
	 */
	public boolean isIgnoreBeamSteering() {
		return ignoreBeamSteering;
	}

	/**
	 * @param ignoreBeamSteering
	 *            the ignoreBeamSteering to set
	 */
	public void setIgnoreBeamSteering(boolean ignoreBeamSteering) {
		this.ignoreBeamSteering = ignoreBeamSteering;
	}

	/**
	 * @return the ignoreRotator
	 */
	public boolean isIgnoreRotator() {
		return ignoreRotator;
	}

	/**
	 * @param ignoreRotator
	 *            the ignoreRotator to set
	 */
	public void setIgnoreRotator(boolean ignoreRotator) {
		this.ignoreRotator = ignoreRotator;
	}

	/**
	 * @return the ignoreInstruments
	 */
	public boolean isIgnoreInstruments() {
		return ignoreInstruments;
	}

	/**
	 * @param ignoreInstruments
	 *            the ignoreInstruments to set
	 */
	public void setIgnoreInstruments(boolean ignoreInstruments) {
		this.ignoreInstruments = ignoreInstruments;
	}

	/**
	 * Determines whether a group can be executed subject to the supplied
	 * conditions and with the given history.
	 * 
	 * @param group
	 *            The group item to consider for execution.
	 * @param time
	 *            The time for which we want the determination.
	 * @param exec
	 *            The group's execution history.
	 * @param accounts
	 *            The group's accounts.
	 * @param env
	 *            Environmental conditions/prediction at time.
	 */
	public CandidateFeasibilitySummary isitFeasible(GroupItem group, long time, ExecutionHistorySynopsis exec,
			AccountSynopsis accounts, EnvironmentSnapshot env, List<Disruptor> disruptors) {

		CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary();
		cfs.setGroup(group);

		// terminal case
		if (group == null)
			return fail(group, "NULL_GROUP");

		if (!group.isActive())
			return fail(group, "GROUP_NOT_ACTIVE");

		// is the proposal enabled ?
		IProposal proposal = group.getProposal();
		if (proposal == null)
			return fail(group, "GROUP_HAS_NO_PROPOSAL");

		if (!proposal.isEnabled())
			return fail(group, "GROUP_PROPOSAL_DISABLED");

		// Proposal not active in next 24h 
		// (24h is over-generous it should be between now and sunrise)
		if (proposal.getActivationDate() > time + 24*3600*1000L)
			return fail(group, "PROPOSAL_NOT_YET_ACTIVE");
		
		// Proposal expired 
		if (proposal.getExpiryDate() < time)
			return fail(group, "PROPOSAL_HAS_EXPIRED");
		
		
		// TODO extract sequence: target and instrument refs -watch out for
		// ag,acq etc
		ISequenceComponent seq = group.getSequence();

		if (seq == null)
			return fail(group, "NO_SEQUENCE_DEFINED");

		ComponentSet cset = null;
		try {
			cset = new ComponentSet(seq);
		} catch (Exception e) {
			e.printStackTrace();
			return fail(group, "UNABLE_EXTRACT_SEQUENCE");
		}

		// check acquisitions
		logger.create().extractCallInfo().info().level(4)
				.msg("Sequence contains: " + cset.countAcquistionConfigs() + " acquisition configs").send();
		Iterator acqs = cset.listAcquisitionConfigs();
		while (acqs.hasNext()) {
			IAcquisitionConfig acq = (IAcquisitionConfig) acqs.next();
			if (acq.getTargetInstrumentName() == null)
				return fail(group, "NO_TARGET_FOR_ACQ");

			String instId = acq.getTargetInstrumentName().toUpperCase(); // UPPERCASE
			// NAMES
			InstrumentDescriptor id = new InstrumentDescriptor(instId);
			
			logger.create().extractCallInfo().info().level(4).msg("Testing feasibility of acquisition config: " + acq)
					.send();

			logger.create().extractCallInfo().info().level(4).msg("Acquisition for target instrument: " + instId)
					.send();

			// see if the instrument itself is enabled, online and
			// operational...

			// we dont care if its just an Aperture change
			switch (acq.getMode()) {
			case IAcquisitionConfig.INSTRUMENT_CHANGE:
				logger.create().extractCallInfo().info().level(4).msg("Aperture offsets for: "+instId)
				.send();
				break;
			case IAcquisitionConfig.WCS_FIT:
			case IAcquisitionConfig.BRIGHTEST:

				try {
					InstrumentSynopsis instr = ism.getInstrumentSynopsis(instId);
					if (instr == null)
						return fail(group, "ACQUIRE_TARGET_" + instId + "_UNKNOWN_INST");

					// is it enabled ?
					if (!instr.isEnabled()) {
						return fail(group, "ACQUIRE_TARGET_" + instId + "_DISABLED");

					}

					// is it online ?
					if (!instr.isOnline())
						return fail(group, "ACQUIRE_TARGET_" + instId + "_OFFLINE");

					// is it working ?
					if (!instr.isOperational())
						return fail(group, "ACQUIRE_TARGET_" + instId + "_FAULTY");

				} catch (Exception cx) {
					logger.create().extractCallInfo().error().level(3)
							.msg("Exception while checking acquire configs for group: " + group.getName() + ":" + cx)
							.send();
					return fail(group, "ACQUIRE_TARGET_CHECK_ERROR");
				}

				if (acq.getAcquisitionInstrumentName() == null)
					return fail(group, "No acquisition instrument specified for acquisition config");

				String acqInstId = acq.getAcquisitionInstrumentName().toUpperCase();
				InstrumentDescriptor aid = new InstrumentDescriptor(acqInstId);

				String targetInstId = acq.getTargetInstrumentName().toUpperCase();
				InstrumentDescriptor tid = new InstrumentDescriptor(targetInstId);

				logger.create().extractCallInfo().info().level(4)
						.msg("Acquisition using instrument: " + acqInstId + " -> " + targetInstId).send();

				boolean primaryAcqInstOk = true;
				try {
					InstrumentSynopsis itest = ism.getInstrumentSynopsis(acqInstId);
					if (itest == null) {
						// return fail(group, "ACQUIRE_ACQ_" + acqInstId +
						// "_UNKNOWN_INST");
						logger.create().extractCallInfo().error().level(3)
								.msg("Primary acquisition instrument " + acqInstId + " not known").send();

						primaryAcqInstOk = false;
					}

					// is it enabled ?
					if (!itest.isEnabled()) {
						// return fail(group, "ACQUIRE_ACQ_" + acqInstId +
						// "_DISABLED");
						logger.create().extractCallInfo().error().level(3)
								.msg("Primary acquisition instrument " + acqInstId + "disabled").send();
						primaryAcqInstOk = false;
					}

					// is it online ?
					if (!itest.isOnline()) {
						// return fail(group, "ACQUIRE_ACQ_" + acqInstId +
						// "_OFFLINE");
						logger.create().extractCallInfo().error().level(3)
								.msg("Primary acquisition instrument " + acqInstId + " offline").send();
						primaryAcqInstOk = false;
					}

					// is it working ?
					if (!itest.isOperational()) {
						// return fail(group, "ACQUIRE_ACQ_" + acqInstId +
						// "_FAULTY");
						logger.create().extractCallInfo().error().level(3)
								.msg("Primary acquisition instrument " + acqInstId + " faulty").send();
						primaryAcqInstOk = false;
					}

				} catch (Exception cx) {
					logger.create().extractCallInfo().error().level(3)
							.msg("Exception while checking status of primary acqusition instrument: " + cx).send();
					primaryAcqInstOk = false;
				}

				// primary ok then use it, were done now, else try alts
				if (!primaryAcqInstOk) {

					// no alts allowed , were done
					if (!acq.getAllowAlternative())
						return fail(group, "ACQUIRE_INST_FAIL_NO_ALTS_ALLOWED");

					// try the list of alternatives
					boolean anyAltInstOk = false;
					List<InstrumentDescriptor> alts = null;

					try {
						alts = ism.listAcquisitionInstruments();
						logger.create().extractCallInfo().info().level(3)
						.msg("List of alternative instruments contains: "+alts.size()+" entries")
						.send();
					} catch (Exception cx) {
						logger.create().extractCallInfo().error().level(3)
								.msg("Exception getting list of alternative acquisition instruments: " + cx).send();
						return fail(group, "ACQUIRE_ALT_LIST_ERROR");
					}

					for (int ia = 0; ia < alts.size(); ia++) {

						InstrumentDescriptor altInst = alts.get(ia);
						String altInstId = altInst.getInstrumentName();

						logger.create().extractCallInfo().info().level(3)
								.msg("Checking for alt acquisition using instrument: " + altInstId).send();

						boolean thisAltInstOk = true;
						try {
							InstrumentSynopsis atest = ism.getInstrumentSynopsis(altInstId);
							if (atest == null) {
								logger.create().extractCallInfo().error().level(3)
										.msg("Alternative acquisition instrument " + altInstId + " not known").send();

								thisAltInstOk = false;
							}

							// can we even use if ?
							if (!atest.getInstrumentCapabilities().canAcquire(tid)) {
								logger.create()
										.extractCallInfo()
										.error()
										.level(3)
										.msg("Alternative acquisition instrument " + altInstId + " cannot acquire for "
												+ targetInstId).send();
								thisAltInstOk = false;
							}

							// is it enabled ?
							if (!atest.isEnabled()) {
								logger.create().extractCallInfo().error().level(3)
										.msg("Alternative acquisition instrument " + altInstId + "disabled").send();
								thisAltInstOk = false;
							}

							// is it online ?
							if (!atest.isOnline()) {
								logger.create().extractCallInfo().error().level(3)
										.msg("Alternative acquisition instrument " + altInstId + " offline").send();
								thisAltInstOk = false;
							}

							// is it working ?
							if (!atest.isOperational()) {
								logger.create().extractCallInfo().error().level(3)
										.msg("Alternative acquisition instrument " + altInstId + " faulty").send();
								thisAltInstOk = false;
							}

						} catch (Exception cx) {
							logger.create()
									.extractCallInfo()
									.error()
									.level(3)
									.msg("Exception while checking status of alternative acquisition instrument: "
											+ altInstId + ":" + cx).send();
							thisAltInstOk = false;
						}

						if (thisAltInstOk) {
							// weve found a good un
							logger.create().extractCallInfo().error().level(3)
									.msg("Found usable alternative acquisition instrument: " + altInstId).send();
							anyAltInstOk = true;
							break;
						}

					} // next alt inst

					// at this point we have an acquire inst of some sort or not
					if (!anyAltInstOk) {
						logger.create().extractCallInfo().error().level(3)
								.msg("No valid alternative acquisition instruments were available").send();
						return fail(group, "ACQUIRE_NO_ALTS");
					}

				}

			} // end of cases

		} // next acquisition

		// calculate execution resource requirements
		logger.create().extractCallInfo().error().level(3).msg("Checking resource requirements").send();

		double execTime = 0L;
		try {
			ExecutionResourceBundle xrb = execModel.getEstimatedResourceUsage(group);
			ExecutionResource timeUsage = xrb.getResource("TIME");
			execTime = timeUsage.getResourceUsage();
		} catch (Exception e) {
			e.printStackTrace();
			return fail(group, "UNABLE_ESTIMATE_RESOURCE_USAGE");
		}

		// calculate execution resource costs and check accounts
		logger.create().extractCallInfo().error().level(3).msg("Checking costs").send();
		ExecutionResourceBundle costs = null;
		try {
			costs = determineCosts(group, env);
		} catch (Exception dcx) {
			dcx.printStackTrace();
			return fail(group, "UNABLE_ESTIMATE_COSTS");
		}

		if (ignoreAccounts) {
			logger.create().extractCallInfo().error().level(3).msg("IGNORE checking accounts").send();
		} else {
			logger.create().extractCallInfo().error().level(3).msg("Checking accounts").send();
			try {
				cfs = checkValidAccounts(group, accounts, costs);
				logger.create().extractCallInfo().error().level(3).msg("CheckValidAccounts returned: " + cfs).send();

				if (!cfs.isFeasible())
					return fail(group, cfs.getRejectionReason());
			} catch (Exception cax) {
				cax.printStackTrace();
				return fail(group, "UNABLE_CHECK_ACCOUNTS");
			}
		}

		// check mandatory autoguider
		logger.create().extractCallInfo().error().level(3).msg("Checking autoguider requirements").send();
		if (cset.isAutoguiderRequired()) {
			if (!telescope.isAutoguiderOperational() || !telescope.isAutoguiderFunctional()) {
				return fail(group, "MANDATORY_AUTOGUIDER_NON_OPER");
			}
		} else if (cset.isAutoguiderOptional()) {
			if (!telescope.isAutoguiderFunctional()) {
				return fail(group, "OPTIONAL_AUTOGUIDER_OVERHEAT");
			}
		}

		// TODO - better
		/*
		 * InstrumentStatus agState = telescope.getAutoguiderStatus(); if
		 * (cset.isAutoguiderRequired()) { if (!agState.isOnline() && !
		 * agState.isFunctional()) { return fail(group,
		 * "MANDATORY_AUTOGUIDER_NON_OPER"); } } else if
		 * (cset.isAutoguiderOptional()) { if (!agState.isFunctional()) { return
		 * fail(group, "OPTIONAL_AUTOGUIDER_OVERHEAT"); } }
		 */

		// check beamconfigs
		logger.create().extractCallInfo().error().level(3).msg("Checking beam steering").send();
		logger.create().extractCallInfo().info().level(4)
				.msg("Sequence contains: " + cset.countBeamSteeringConfigs() + " beamconfigs").send();
		logger.create().extractCallInfo().info().level(4).msg("NOT testing beam configs for now...").send();

		/*
		 * Iterator bconfigs = cset.listBeamSteeringConfigs(); while
		 * (bconfigs.hasNext()) { IBeamSteeringConfig beam =
		 * (IBeamSteeringConfig) bconfigs.next();
		 * logger.create().extractCallInfo
		 * ().info().level(4).msg("Testing feasibility of beam config: " +
		 * beam).send(); if (!telescope.isValidBeamSteeringConfig(beam)) {
		 * return fail(group, "INVALID_BEAM_CONFIG"); } }
		 */
		// check instruments
		logger.create().extractCallInfo().info().level(4).msg("Sequence contains: " + cset.countConfigs() + " configs")
				.send();
		// check for configs with exposures... TEMP we just check there are
		// configs
		if (cset.countConfigs() == 0) {
			logger.create().extractCallInfo().info().level(4).msg("Sequence has no configs defined").send();
			if (!ignoreInstruments)
				return fail(group, "INVALID_SEQUENCE");
		}

		Iterator configs = cset.listInstrumentConfigs();
		while (configs.hasNext()) {
			IInstrumentConfig config = (IInstrumentConfig) configs.next();

			// find an instrument or instrument cap-provider or s-u for the
			// instrument associated with this here config.
			String instId = config.getInstrumentName().toUpperCase(); // UPPERCASE
			// NAMES
			InstrumentDescriptor id = new InstrumentDescriptor(instId);
			logger.create().extractCallInfo().info().level(4).msg("Testing feasibility of config: " + config).send();

			logger.create().extractCallInfo().info().level(4)
					.msg("Config uses instrument: " + config.getInstrumentName()).send();

			// see if the instrument itself is online and operational...
			try {
				InstrumentSynopsis itest = ism.getInstrumentSynopsis(instId);
				if (itest == null)
					return fail(group, "UNKNOWN_INST_" + instId);

				// is it enabled ?
				if (!itest.isEnabled())
					return fail(group, instId + "_DISABLED");

				// is it online ?
				if (!itest.isOnline())
					return fail(group, instId + "_OFFLINE");

				// is it working ?
				if (!itest.isOperational())
					return fail(group, instId + "_FAULTY");

				if (!itest.isValidConfig(config)) {
					logger.create().extractCallInfo().error().level(3).msg("Illegal config: " + config);
					return fail(group, instId + "_ILLEGAL_CONFIG");
				}
			} catch (Exception cx) {
				logger.create().extractCallInfo().error().level(3)
						.msg("Exception while checking configs for group: " + group.getName() + ":" + cx).send();

				cx.printStackTrace();
				return fail(group, "CONFIG_CHECKING_ERROR");
			}

		}

		// check targets against basic constraints
		logger.create().extractCallInfo().info().level(4).msg("Sequence contains: " + cset.countTargets() + " targets")
				.send();
		Iterator targets = cset.listTargets();
		while (targets.hasNext()) {
			ITarget target = (ITarget) targets.next();
			TargetTrackCalculator track = new BasicTargetCalculator(target, site);
			logger.create()
					.extractCallInfo()
					.info()
					.level(4)
					.msg("Testing feasibility of " + (target.getClass().getSimpleName()) + " target: "
							+ target.getName()).send();
			try {
				Coordinates c = track.getCoordinates(time);
				double alt = astro.getAltitude(c, site, time);
				double minalt = astro.getMinimumAltitude(track, site, time, time + (long) execTime);
				logger.create()
						.extractCallInfo()
						.info()
						.level(4)
						.msg(String.format("Target: %s Alt: %3.2f, MinAlt: %3.2f over duration: %6.2f m",
								target.getName(), Math.toDegrees(alt), Math.toDegrees(minalt), (execTime / 60000.0)))
						.send();

				// Check its above Dome Low Limit now.
				if (alt < telescope.getDomeLimit())
					return fail(group, "TARGET_HORIZON_INFRINGEMENT");

				// Check target wont set before end-of-obs (unless
				// circum-polar).
				if (astro.canSet(c, site, telescope.getDomeLimit(), time)) {
					long ttset = astro.getTimeUntilNextSet(c, site, telescope.getDomeLimit(), time);
					if (ttset < execTime)
						return fail(group, "TARGET_WILL_SET");
				}

				// Check if target can enter Zenith Avoidance Zone (ZAZ).
				double zazSize = telescope.getZenithAvoidanceZoneSize();
				if (astro.canRise(c, site, 0.5 * Math.PI - zazSize, time)) {
					double maxElev = astro.getMaximumAltitude(track, site, time, time + (long) execTime);
					if (maxElev > 0.5 * Math.PI - zazSize)
						return fail(group, "ZAZ_INCURSION");
				}

			} catch (Exception e) {
				// TODO Need to throw a wobbly here rather than returning false.
				e.printStackTrace();
				logger.create().extractCallInfo().error().level(3)
						.msg("Exception while processing targets for group: " + group.getName() + ":" + e).send();
				return fail(group, "TARGET_PROCESSING");
			}
		}

		// check if any rotator configs are achievable - not simple we must walk
		// the seq tree
		// looking for rot configs and determine what target in use and how long
		// this will need
		// to track for - also in theory instrument rotator alignment if varies.
		logger.create().extractCallInfo().info().level(4).msg("Testing feasibility of rotator configs...").send();
		cfs = checkRotatorConfigs(ism, group, time);
		if (!cfs.isFeasible())
			return fail(group, "ROTATOR_SETTING_NOT_FEASIBLE");

		// check timing constraints
		cfs = checkValidTimingConstraints(group, exec, time);
		if (!cfs.isFeasible())
			return cfs;

		// check observing constraints
		List oclist = group.listObservingConstraints();

		// A missing list (rather than an empty one)
		// is a sign of DB or extraction problem, group is thus suspect
		if (oclist == null)
			return fail(group, "GROUP_NO_OBSERVING_CONSTRAINT_LIST");

		// - or maybe we ahould just assume none in this case ?
		// oclist = new Vector() = empty

		Iterator ocs = oclist.iterator();
		while (ocs.hasNext()) {

			IObservingConstraint oc = (IObservingConstraint) ocs.next();

			try {
				cfs = checkValidConstraint(group, cset, oc, env, time, (long) execTime);
				if (!cfs.isFeasible())
					return cfs;
			} catch (Exception cx) {
				logger.create().extractCallInfo().error().level(3)
						.msg("Exception while processing OC for group: " + group.getName() + ":" + cx).send();
				return fail(group, "CONSTRAINT_ERROR");
			}

		}

		// if its Fixed-time then all the necessary tests are done and
		// we are now home and dry !
		if (group.getTimingConstraint() instanceof XFixedTimingConstraint) {
			cfs.setFeasible(true);
			cfs.setRejectionReason(null);
			return cfs;
		}

		// check for disruptors
		ITimePeriod execPeriod = new XTimePeriod(time, time + (long) execTime);
		Iterator<Disruptor> idis = disruptors.iterator();
		while (idis.hasNext()) {

			Disruptor disruptor = idis.next();
			ITimePeriod disruption = disruptor.getPeriod();

			// found a disruptor in the next 24 h

			long overlapStart = Math.max(disruption.getStart(), execPeriod.getStart());
			long overlapEnd = Math.min(disruption.getEnd(), execPeriod.getEnd());

			// see if our group (exec.start, exec.end) overlaps (d.start, d.end)
			// ie if e starts before eod AND e ends after sod

			if (overlapStart < overlapEnd) {
				logger.create()
						.extractCallInfo()
						.error()
						.level(3)
						.msg("Group exec window t=[0, +" + ((long) execTime / 60000) + "]m overlaps with disruptor: "
								+ disruptor.getDisruptorClass() + ":" + disruptor.getDisruptorName()
								+ " exclusion window t=[+" + (disruptor.getPeriod().getStart() - time) / 60000 + ", +"
								+ (disruptor.getPeriod().getEnd() - time) / 60000 + "]m").send();

				// Group exec window t=[0, +35]m overlaps with
				// disruptor:FixedGroup:RISE-098709
				// exclusion window t=[+23,+43]m

				return fail(group, "DISRUPTOR_INTERSECTION");
			}

		}

		// Check the sun isnt about to come up...
		try {
			TargetTrackCalculator sunCalc = new SolarCalculator();
			Coordinates sun = sunCalc.getCoordinates(time);
			Coordinates sunPlus = sunCalc.getCoordinates(time + (long) execTime);

			if (astro.getAltitude(sun, site, time) > Math.toRadians(-2.0))
				return fail(group, "SUN_IS_UP");

			if (astro.getAltitude(sunPlus, site, time + (long) execTime) > Math.toRadians(-2.0))
				return fail(group, "SUN_WILL_RISE");
		} catch (AstrometryException aex) {
			logger.create().extractCallInfo().error().level(3)
					.msg("Exception while processing sun target for group: " + group.getName() + ":" + aex).send();
			return fail(group, "ASTROMETRY_ERROR");
		}

		// check external imposed constraints
		// TODO there are currently none of these in force.

		// all done, the group is feasible
		cfs.setFeasible(true);
		cfs.setRejectionReason(null);
		return cfs;

	}

	/**
	 * Determine the costs associated with executing this group (under specified
	 * conditions ?).
	 */
	private ExecutionResourceBundle determineCosts(GroupItem group, EnvironmentSnapshot env) {

		ExecutionResourceBundle costs = new ExecutionResourceBundle();

		// we need to decide what seeing and lunar conditions are requested.
		// SNF - 6-nov-09 Condition costs are removed as we no longer use them.

		// ObservingConstraintAdapter oca = new
		// ObservingConstraintAdapter(group);

		// int seeCat = oca.getSeeingConstraint().getSeeingCategory();
		// String seeCatName = getSeeCatName(seeCat);

		// int lunCat =
		// oca.getLunarElevationConstraint().getLunarElevationCategory();
		// String lunCatName = getLunCatName(lunCat);

		// String lunSeeName = "allocation." + lunCatName + "." + seeCatName;
		// e.g. allocation.dark.poor

		// total charge for execution
		double timeCost = chargeModel.calculateCost(group.getSequence());

		// TODO Set lenient soas to allow groups to execute with no condition
		// allocation for testing purposes
		// we dont bother checking if they asked for BU or DU
		/*
		 * if (seeCat != IObservingConstraint.UNCONSTRAINED_SEEING) {
		 * ExecutionResource lunseeRes = new ExecutionResource(lunSeeName,
		 * timeCost); costs.addResource(lunseeRes); }
		 */

		ExecutionResource timeCostRes = new ExecutionResource("allocation", timeCost);
		costs.addResource(timeCostRes);

		// Add in cost for fixed groups if group is fixed time
		/*
		 * ITimingConstraint timing = group.getTimingConstraint(); if (timing
		 * instanceof XFixedTimingConstraint) { ExecutionResource fixedTimeRes =
		 * new ExecutionResource("allocation.fixed", timeCost);
		 * costs.addResource(fixedTimeRes); }
		 */

		return costs;

	}

	/**
	 * Check the group has sufficient funds to complete (based on charge-model
	 * NOT timing model).
	 * 
	 * @param group
	 *            The group to test.
	 * @param accounts
	 *            The group's proposal's account synopsis.
	 * @param env
	 *            Current/predicted environment snapshot.
	 * @param costs
	 *            Cost of executing the group's observing sequence (according to
	 *            charging model).
	 * */
	private CandidateFeasibilitySummary checkValidAccounts(GroupItem group, AccountSynopsis accounts,
			ExecutionResourceBundle costs) throws Exception {

		logger.create().extractCallInfo().info().level(1).msg("Checking accounts using: " + accounts).send();

		// currently we only test for time-used against the current and next
		// semester allocations
		// the ERB should only contain 1 entry
		Iterator<ExecutionResource> icost = costs.listResources();
		while (icost.hasNext()) {
			ExecutionResource costRes = icost.next();
			// TODO Note that accounts have been allocated in hours but cost is
			// in millis
			double costAmount = costRes.getResourceUsage() / 3600000.0;
			String costName = costRes.getResourceName();

			// 2SEM - this all needs re-writing......

			logger.create().extractCallInfo().info().level(4).msg("Searching for account named: " + costName).send();

			// 2SEM FUDGED - we find the early and late semester acccounts, 1 or
			// other may not exist.
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
				logger.create()
						.extractCallInfo()
						.info()
						.level(4)
						.msg("Account located: (" + costAccountEarly.getName() + ") ID=[" + costAccountEarly.getID()
								+ "]" + " Alloc=" + costAccountEarly.getAllocated() + ", Used="
								+ costAccountEarly.getConsumed() + ", Cost=" + costAmount).send();
				balanceEarly = costAccountEarly.getAllocated() - costAccountEarly.getConsumed();
			}

			if (costAccountLate != null) {
				System.err.println("Found cost account for: " + lateSemester.getName() + " : " + costAccountLate);
				logger.create()
						.extractCallInfo()
						.info()
						.level(4)
						.msg("Account located: (" + costAccountLate.getName() + ") ID=[" + costAccountLate.getID()
								+ "]" + " Alloc=" + costAccountLate.getAllocated() + ", Used="
								+ costAccountLate.getConsumed() + ", Cost=" + costAmount).send();
				balanceLate = costAccountLate.getAllocated() - costAccountLate.getConsumed();
			}
			// e.g Account located: (allocation.dark.poor) ID=[565] Alloc=20.0,
			// Used=6.45, Cost=1.22

			// veto groups which are totally out of funds
			if (balanceEarly <= 0.0 && balanceLate <= 0.0)
				return fail(group, "NO_" + costName.toUpperCase() + "_FUNDS");

			// allow groups which have some funds left even if it will result in
			// an OD or we can push into next semester account ... needs
			// clarification
			/*
			 * double od = costAmount - costAccountEarly.getAllocated() +
			 * costAccountEarly.getConsumed(); double odp = 100.0 * od /
			 * costAccountEarly.getAllocated(); double odb = 100.0 * od /
			 * (costAccountEarly.getAllocated() -
			 * costAccountEarly.getConsumed()); logger.create()
			 * .extractCallInfo() .info() .level(4)
			 * .msg("Semester "+earlySemester.getName()+
			 * " account has insufficent funds, expected overdraft will be: " +
			 * od + "H, " + odp + "% of initial allocation, " + odb +
			 * "% of remaining balance").send();
			 */
			if (balanceEarly + balanceLate < costAmount) {
				logger.create()
						.extractCallInfo()
						.info()
						.level(4)
						.msg("Total balance between semester accounts is insufficent to cover cost, one or other will be overdrawn")
						.send();

			}

		}

		// default for where a group passes this set of tests
		return new CandidateFeasibilitySummary(group, true, null);
	}

	private CandidateFeasibilitySummary checkValidTimingConstraints(GroupItem group, ExecutionHistorySynopsis exec,
			long time) {

		ITimingConstraint tc = group.getTimingConstraint();

		if (tc == null)
			return fail(group, "NO_TIMING");

		long lastExec = exec.getLastExecution();
		int countExec = exec.getCountExecutions();

		if (tc instanceof XFlexibleTimingConstraint) {
			XFlexibleTimingConstraint xflex = (XFlexibleTimingConstraint) tc;

			if (xflex.getActivationDate() > time)
				return fail(group, "FLEX_PRE_START");

			if (xflex.getExpiryDate() < time)
				return fail(group, "FLEX_POST_END");

			if (lastExec > xflex.getActivationDate() && lastExec < xflex.getExpiryDate())
				return fail(group, "FLEX_DONE_IN_WINDOW");

			CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary(group, true, null);
			return cfs;
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
				return fail(group, "MONITORING_PRE_START");

			if (endDate < time)
				return fail(group, "MONITORING_POST_END");

			long startFloat = startDate + (long) ((iPeriod - (double) floatFraction / 2.0) * (double) period);
			long endFloat = startDate + (long) ((iPeriod + (double) floatFraction / 2.0) * (double) period);

			if ((startFloat > time) || (endFloat < time))
				return fail(group, "MONITOR_NOT_IN_WINDOW");

			// see if its already done in window
			if (lastExec > startFloat && lastExec < endFloat)
				return fail(group, "MONITOR_DONE_IN_WINDOW");

		}

		if (tc instanceof XMinimumIntervalTimingConstraint) {

			XMinimumIntervalTimingConstraint xmin = (XMinimumIntervalTimingConstraint) tc;

			if (countExec >= xmin.getMaximumRepeats())
				return fail(group, "MININT_MAX_REPEATS");

			if (time - lastExec < xmin.getMinimumInterval())
				return fail(group, "MININT_TOO_SOON");

			if (xmin.getStart() > time)
				return fail(group, "MININT_PRE_START");

			if (xmin.getEnd() < time)
				return fail(group, "MININT_POST_END");

		}

		if (tc instanceof XEphemerisTimingConstraint) {

			XEphemerisTimingConstraint xephem = (XEphemerisTimingConstraint) tc;
			// TODO SOON we will allow this but need to just check in-window
			// if (countExec >= 1)
			// return fail(group, "EPHEM_ALREADY_DONE");

			if (xephem.getStart() > time)
				return fail(group, "EPHEM_PRE_START");

			if (xephem.getEnd() < time)
				return fail(group, "EPHEM_POST_END");

			// work out the window periods

			long startDate = xephem.getStart();
			long endDate = xephem.getEnd();
			long period = xephem.getCyclePeriod();
			double phase = (double) xephem.getPhase();
			double window = (double) xephem.getWindow();

			// double fperiod = Math.floor((time - startDate) / period);

			// LOG DETAILS
			logger.create().extractCallInfo().info().level(4)
			    .msg(String.format("EPHEM: Group timing: ST: %tF %tT , PE: %8d , PH: %8.2f , W: %8.2f \n",
					       startDate, startDate, period, phase, window)).send(); 
			
			double fperiod = Math.floor(((double) time - (double) startDate - phase * (double) period + window / 2.0)
					/ period);

			long startWindow = startDate + (long) ((fperiod + phase) * period - window / 2.0);
			long endWindow = startDate + (long) ((fperiod + phase) * period + window / 2.0);

			logger.create().extractCallInfo().info().level(4)
                            .msg(String.format("EPHEM: Now: %tF %tT FPNo: %6.2f  ST: %tF %tT  ET: %tF %tT \n", 
					       time, time, fperiod, startWindow, startWindow, endWindow, endWindow)).send();
			
			// LOG CALCS

			if (startWindow > time || endWindow < time) 
			    return fail(group, "EPHEM_NOT_IN_WINDOW");
						
			// NEW 9-nov-09 was executed since start date
			if (lastExec > startDate)
				return fail(group, "EPHEM_ALREADY_DONE");

		}

		if (tc instanceof XFixedTimingConstraint) {

			XFixedTimingConstraint fixed = (XFixedTimingConstraint) tc;

			long start = fixed.getStartTime();
			long end = fixed.getEndTime();
			// long slack = fixed.getSlack() / 2;

			// if we are in its start window, its doable
			long startWindow = start;// - slack;
			long endWindow = end;// + slack;
			if ((startWindow > time) || (endWindow < time))
				return fail(group, "FIXED_NOT_IN_WINDOW");

			if (lastExec > startWindow && lastExec < endWindow)
				return fail(group, "FIXED_DONE_IN_WINDOW");

		}

		// all tests passed
		CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary(group, true, null);
		return cfs;

	}

	/**
	 * Check whether the group has any rotator configs and that these are valid
	 * at the specified time.
	 * 
	 * @param group
	 *            The group to test.
	 * @param time
	 *            When we want it tested.
	 * @return True if the group's rotator settings are valid at time.
	 */
	private CandidateFeasibilitySummary checkRotatorConfigs(InstrumentSynopsisModel ism, GroupItem group, long time) {

		ISequenceComponent sequence = group.getSequence();

		// no sequence so no problem
		if (sequence == null) {
			CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary(group, true, null);
			return cfs;
		}

		// need to recursively test any rot configs or slews within the groups
		// sequence
		boolean ok = false;
		try {
			ok = checker.checkRotator(sequence, time);
		} catch (Exception e) {
			e.printStackTrace();
			return fail(group, "ROTATOR_CHECKING_ERROR");
		}
		CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary(group, ok, null);
		return cfs;
	}

	private CandidateFeasibilitySummary checkValidConstraint(GroupItem group, ComponentSet cset,
			IObservingConstraint oc, EnvironmentSnapshot env, long time, long execTime) throws Exception {

		// XPhotometricityConstraint.java
		// XLunarPhaseConstraint.java
		// XAirmassConstraint.java
		// XHourAngleConstraint.java
		// XSkyBrightnessConstraint.java

		logger.create().extractCallInfo().info().level(4)
				.msg("Checking constraint for " + group.getName() + " type=" + oc).send();

		if (group.getTimingConstraint() instanceof XFixedTimingConstraint) {
			if (!(oc instanceof XSeeingConstraint) && !(oc instanceof XPhotometricityConstraint)) {
				logger.create().extractCallInfo().info().level(4).msg("Fixed group has illegal constraint type: " + oc)
						.send();
				return fail(group, "FIXED_GROUP_ILLEGAL_CONSTRAINT");
			}
		}

		/*
		 * if (oc instanceof XLunarPhaseConstraint) { XLunarPhaseConstraint xlp
		 * = (XLunarPhaseConstraint) oc; double maxp =
		 * xlp.getMaximumLunarPhase();
		 * 
		 * // calculate lunar phase angle in (0,1) TargetTrackCalculator sunCalc
		 * = new SolarCalculator(); Coordinates sun =
		 * sunCalc.getCoordinates(time); double sra = sun.getRa(); double sdec =
		 * sun.getDec(); TargetTrackCalculator moonTrack = new
		 * LunarCalculator(site); Coordinates moon =
		 * moonTrack.getCoordinates(time); double mra = moon.getRa(); double
		 * mdec = moon.getDec();
		 * 
		 * double angle = Math.acos(Math.cos(mdec) * Math.cos(sdec) *
		 * Math.cos(mra - sra) + Math.sin(mdec) Math.sin(sdec)); double fraction
		 * = 0.5 * (1.0 + Math.cos(Math.PI - angle)); double phase = (angle <
		 * Math.PI ? angle / Math.PI : (2.0 * Math.PI - angle) / Math.PI);
		 * 
		 * logger.create() .extractCallInfo() .info() .level(4)
		 * .msg("Lunar phase constraint: S-M:" + Math.toDegrees(angle) +
		 * " Illum-frac: " + fraction + ", phase: " + phase + " max-phase: " +
		 * maxp);
		 * 
		 * if (phase > maxp) return fail(group, "LUNAR_PHASE");
		 * 
		 * } else if (oc instanceof XLunarElevationConstraint) {
		 * XLunarElevationConstraint xlev = (XLunarElevationConstraint) oc;
		 * switch (xlev.getLunarElevationCategory()) { case
		 * XLunarElevationConstraint.MOON_DARK:
		 * 
		 * TargetTrackCalculator moonTrack = new LunarCalculator(site);
		 * Coordinates moon = moonTrack.getCoordinates(time); double moonElev =
		 * astro.getAltitude(moon, site, time);
		 * 
		 * logger.create().extractCallInfo().info().level(4)
		 * .msg("Lunar elevation constraint: Moon Elev=" +
		 * Math.toDegrees(moonElev)).send();
		 * 
		 * if (moonElev > 0.0) return fail(group, "LUNAR_ELEVATION"); break;
		 * case XLunarElevationConstraint.MOON_BRIGHT: // not a problem... }
		 */
		if (oc instanceof XSeeingConstraint) {

			boolean seeok = true;
			XSeeingConstraint xsee = (XSeeingConstraint) oc;

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
				double targetElev = astro.getAltitude(c, site, time);
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
					return fail(group, "SEEING_NAN");

				if (targetSeeing > rcsLimit)
					return fail(group, "SEEING_CONSTRAINT");
			} // next target

		} else if (oc instanceof XSkyBrightnessConstraint) {

			XSkyBrightnessConstraint xsky = (XSkyBrightnessConstraint) oc;
			int requiredSkycat = xsky.getSkyBrightnessCategory();
			double requiredSky = SkyBrightnessCalculator.getSkyBrightness(requiredSkycat);

			Iterator<ITarget> targets = cset.listTargets();
			while (targets.hasNext()) {
				ITarget target = targets.next();
				TargetTrackCalculator track = new BasicTargetCalculator(target, site);

				// test at start, end and middle of exec
				long startExec = time;
				long midExec = time + (long) (0.5 * (double) execTime);
				long endExec = time + execTime;
				int actualSkyCat1 = skycalc.getSkyBrightnessCriterion(track, startExec);
				int actualSkyCat2 = skycalc.getSkyBrightnessCriterion(track, midExec);
				int actualSkyCat3 = skycalc.getSkyBrightnessCriterion(track, endExec);

				// find worst value...
				int actualSkyCat = Math.max(actualSkyCat1, actualSkyCat2);
				actualSkyCat = Math.max(actualSkyCat, actualSkyCat3);

				double actualSky1 = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat1);
				double actualSky2 = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat2);
				double actualSky3 = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat3);

				double actualSky = SkyBrightnessCalculator.getSkyBrightness(actualSkyCat);
				logger.create()
						.extractCallInfo()
						.info()
						.level(4)
						.msg("Checking skyb: actual:" + " (s)=" + actualSky1 + " (m)=" + actualSky2 + " (e)="
								+ actualSky3 + " (u)=" + actualSky + "] " + " requested: " + requiredSky + " ["
								+ requiredSkycat + "]").send();

				if (actualSky > requiredSky)
					return fail(group, "SKYB_CONSTRAINT");
			} // next target

		} else if (oc instanceof XAirmassConstraint) {
			XAirmassConstraint xair = (XAirmassConstraint) oc;
			double airmax = xair.getMaximumAirmass();
			// check all targets are above this altitude for "all" exec time...
			Iterator<ITarget> targets = cset.listTargets();
			while (targets.hasNext()) {
				ITarget target = targets.next();
				TargetTrackCalculator track = new BasicTargetCalculator(target, site);
				double targetMinElev = astro.getMinimumAltitude(track, site, time, time + (long) execTime);

				// calculate airmass.
				double maxZenDist = 0.5 * Math.PI - targetMinElev;
				double maxTargetAirMass = 1.0 / Math.cos(maxZenDist);

				if (maxTargetAirMass > airmax)
					return fail(group, "AIRMASS_VIOLATION");

			}

			// }
			/*
			 * else if (oc instanceof XSolarElevationConstraint) {
			 * XSolarElevationConstraint xsol = (XSolarElevationConstraint) oc;
			 * 
			 * // require sun NOT to exceed the specified max over exec time.
			 * TargetTrackCalculator sunCalc = new SolarCalculator();
			 * Coordinates sun = sunCalc.getCoordinates(time); double sunElev =
			 * astro.getAltitude(sun, site, time); double sunMaxElev =
			 * astro.getMaximumAltitude(sunCalc, site, time, time + (long)
			 * execTime); logger.create() .extractCallInfo() .info() .level(4)
			 * .msg("Solar elevation constraint: Sun Elev: Now:" +
			 * Math.toDegrees(sunElev) + ", Max: " +
			 * Math.toDegrees(sunMaxElev)).send(); switch
			 * (xsol.getMaximumSolarElevationCategory()) { case
			 * XSolarElevationConstraint.CIVIL_TWILIGHT: if (sunMaxElev >
			 * Math.toRadians(-1.0)) return fail(group, "SUN_ELEV_CIVIL");
			 * break; case XSolarElevationConstraint.NAUTICAL_TWILIGHT: if
			 * (sunMaxElev > Math.toRadians(-6.0)) return fail(group,
			 * "SUN_ELEV_NAUTICAL"); break; case
			 * XSolarElevationConstraint.ASTRONOMICAL_TWILIGHT: if (sunMaxElev >
			 * Math.toRadians(-12.0)) return fail(group, "SUN_ELEV_ASTRO");
			 * break; case XSolarElevationConstraint.NIGHT_TIME: if (sunMaxElev
			 * > Math.toRadians(-18.0)) return fail(group, "SUN_ELEV_NIGHT");
			 * break; } } else if (oc instanceof XLunarDistanceConstraint) {
			 * XLunarDistanceConstraint xld = (XLunarDistanceConstraint) oc;
			 * 
			 * double mld = xld.getMinimumLunarDistance(); // rads
			 * 
			 * TargetTrackCalculator moonTrack = new LunarCalculator(site);
			 * 
			 * // check all targets are beyond this MLD for "all" exec time...
			 * Iterator<ITarget> targets = cset.listTargets(); while
			 * (targets.hasNext()) { ITarget target = targets.next();
			 * TargetTrackCalculator tgtTrack = new
			 * BasicTargetCalculator(target, site); // calculate CPA for Target
			 * and Moon tracks double cpaMoon =
			 * astro.getClosestPointOfApproach(tgtTrack, moonTrack, time, time +
			 * (long) execTime);
			 * 
			 * if (cpaMoon < mld) { logger.create() .extractCallInfo() .info()
			 * .level(4) .msg("Target: " + target.getName() + " passes within "
			 * + Math.toDegrees(cpaMoon) + " of the moon").send(); return
			 * fail(group, "LUNAR_CPA_INFRINGEMENT");// or // ENCROACHMENT } }
			 */
		} else if (oc instanceof XPhotometricityConstraint) {
			XPhotometricityConstraint xphot = (XPhotometricityConstraint) oc;

			int photom = xphot.getPhotometricityCategory();

			switch (photom) {
			case XPhotometricityConstraint.PHOTOMETRIC:
				if (env.getExtinctionState() != EnvironmentSnapshot.EXTINCTION_PHOTOM) {
					logger.create().extractCallInfo().info().level(4)
							.msg("Required extinction is: " + xphot + " Actual is: " + env).send();
					return fail(group, "NON_PHOTOMETRIC");
				}
				break;
			case XPhotometricityConstraint.NON_PHOTOMETRIC:
				if (env.getExtinctionState() != EnvironmentSnapshot.EXTINCTION_SPECTRO) {
					logger.create().extractCallInfo().info().level(4)
							.msg("Required extinction is: " + xphot + " Actual is: " + env).send();
					return fail(group, "TOO_PHOTOMETRIC");
				}
				break;
			}

		} else if (oc instanceof XHourAngleConstraint) {
			XHourAngleConstraint xha = (XHourAngleConstraint) oc;

			double lo = xha.getMinimumHourAngle();
			double hi = xha.getMaximumHourAngle();
			if (lo < 0.0)
				lo += 2.0 * Math.PI;
			if (hi < 0.0)
				hi += 2.0 * Math.PI;

			Iterator<ITarget> targets = cset.listTargets();
			while (targets.hasNext()) {
				ITarget target = targets.next();
				TargetTrackCalculator tgtTrack = new BasicTargetCalculator(target, site);
				Coordinates c = tgtTrack.getCoordinates(time);
				double ha = astro.getHourAngle(c, site, time);

				if (lo < hi) {
					if (ha < lo || ha > hi)
						return fail(group, "HA_CONSTRAINT");
				} else {
					if (ha < lo && ha > hi)
						return fail(group, "HA_CONSTRAINT");
				}
			}
		}

		CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary(group, true, null);
		return cfs;

	}

	/*
	 * private String getSeeCatName(int seeCat) {
	 * 
	 * switch (seeCat) { case IObservingConstraint.GOOD_SEEING: return "good";
	 * case IObservingConstraint.AVERAGE_SEEING: return "aver"; case
	 * IObservingConstraint.POOR_SEEING: return "poor"; } return "poor"; //
	 * unknown !
	 * 
	 * }
	 * 
	 * private String getLunCatName(int lunCat) { switch (lunCat) { case
	 * IObservingConstraint.MOON_BRIGHT: return "bright"; case
	 * IObservingConstraint.MOON_DARK: return "dark"; } return "unk"; }
	 */
	private CandidateFeasibilitySummary fail(GroupItem group, String reason) {

		CandidateFeasibilitySummary cfs = new CandidateFeasibilitySummary();
		cfs.setGroup(group);
		cfs.setFeasible(false);
		cfs.setRejectionReason(reason);

		String path = (group.getTag() != null ? group.getTag().getName() : "UNK_TAG") + "/"
				+ (group.getUser() != null ? group.getUser().getName() : "UNK_USR") + "/"
				+ (group.getProposal() != null ? group.getProposal().getName() : "UNK_PRP");

		String message = String.format("Group: [%6d] [%35.35s..] [%20.20s..] failed due to: %s", group.getID(), path,
				group.getName(), reason);

		logger.create().block("isFeasible").info().level(3).msg(message).send();

		return cfs;
	}

}
