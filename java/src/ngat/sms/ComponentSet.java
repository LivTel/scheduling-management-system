/**
 * 
 */
package ngat.sms;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ngat.oss.impl.mysql.accessors.InstrumentConfigAccessor;
import ngat.phase2.IAcquisitionConfig;
import ngat.phase2.IAutoguiderConfig;
import ngat.phase2.IBeamSteeringConfig;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.IInstrumentConfigSelector;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ISlew;
import ngat.phase2.ITarget;
import ngat.phase2.ITargetSelector;
import ngat.phase2.XAcquisitionConfig;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XInstrumentConfig;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XRotatorConfig;

/**
 * A set of sets of components involved in a observation sequence. This is
 * effectively a way of mapping a sequence into something like an old-style
 * observation soas to extract target and instrument information without having
 * to repeatedly walk the sequence tree.
 * 
 * @author eng
 * 
 */
public class ComponentSet {

    public static final String AUTO_INST = "RATCAM";    

	private Set<ITarget> targets;

	private Set<IInstrumentConfig> instrumentConfigs;
	
	private Set<IBeamSteeringConfig> beamConfigs;

	private List acquisitionConfigs;

	private boolean autoguiderRequired;
	
	private boolean autoguiderOptional;
	
    /** Set true to auto fix any rotconfigs where the focal plane instrument is not specified.
     * NEVER EVER USE THIS EVER EVER EVER */
   public static boolean autofix = false;

	/** 
     * 
     */
	public ComponentSet() {
		targets = new HashSet();
		instrumentConfigs = new HashSet();
		acquisitionConfigs = new Vector();
		beamConfigs = new HashSet();
	}

	/**
	 * Create a ComponentSet by walking the tree of the given sequence.
	 * 
	 * @param seq
	 *            The observation sequence to extract components from.
	 * @throws Exception
	 *             If anything goes awry.
	 */
	public ComponentSet(ISequenceComponent seq) throws Exception {
		this();
		extractComponents(seq);
	}


	/**
	 * Add an {@link ITarget} to the set.
	 * 
	 * @param target
	 *            The target to add.
	 */
	private void addTarget(ITarget target) {
		targets.add(target);
	}

	/**
	 * @return An {@link Iterator} over the set of targets.
	 */
	public Iterator<ITarget> listTargets() {
		return targets.iterator();
	}

	public int countTargets() {
		return targets.size();
	}

	/**
	 * Add an {@link IInstrumentConfig} to the set.
	 * 
	 * @param cfg
	 *            The config to add.
	 */
	private void addAcquistionConfig(IAcquisitionConfig cfg) {
		acquisitionConfigs.add(cfg);
	}

	/**
	 * @return An {@link Iterator} over the set of configs.
	 */
	public Iterator<IAcquisitionConfig> listAcquisitionConfigs() {
		return acquisitionConfigs.iterator();
	}

	public int countAcquistionConfigs() {
		return acquisitionConfigs.size();
	}

	/**
	 * Add an {@link IInstrumentConfig} to the set.
	 * 
	 * @param cfg
	 *            The config to add.
	 */
	private void addInstrumentConfig(IInstrumentConfig cfg) {
		instrumentConfigs.add(cfg);
	}

	/**
	 * @return An {@link Iterator} over the set of configs.
	 */
	public Iterator<IInstrumentConfig> listInstrumentConfigs() {
		return instrumentConfigs.iterator();
	}

	public int countConfigs() {
		return instrumentConfigs.size();
	}
	
	/**
	 * Add an {@link IBeamSteeringConfig} to the set.
	 * 
	 * @param cfg
	 *            The config to add.
	 */
	private void addBeamSteeringConfig(IBeamSteeringConfig beam) {
		beamConfigs.add(beam);
	}

	/**
	 * @return An {@link Iterator} over the set of configs.
	 */
	public Iterator<IBeamSteeringConfig> listBeamSteeringConfigs() {
		return beamConfigs.iterator();
	}

	public int countBeamSteeringConfigs() {
		return beamConfigs.size();
	}

	public boolean isAutoguiderRequired() {
		return autoguiderRequired;
	}

	/**
	 * Extract the components from the supplied sequence (or subsequence).
	 * 
	 * @param seq
	 *            The sequence root from which to extract components.
	 * @throws Exception
	 *             If anything goes awry.
	 */
	private void extractComponents(ISequenceComponent seq) throws Exception {
		if (seq == null)
			throw new Exception("No sequence defined");

		if (seq instanceof XExecutiveComponent) {
			XExecutiveComponent xec = (XExecutiveComponent) seq;
			IExecutiveAction action = xec.getExecutiveAction();
			
			if (action instanceof ITargetSelector) {
				ITarget target = ((ITargetSelector) action).getTarget();
				if (target == null)
					throw new Exception("TargetSelector had null target");
				addTarget(((ITargetSelector) action).getTarget());
			} else if (action instanceof ISlew) {
				ITarget target = ((ISlew) action).getTarget();
				if (target == null)
					throw new Exception("Slew had null target");
				addTarget(((ISlew) action).getTarget());
				
				IRotatorConfig rotConfig = ((ISlew)action).getRotatorConfig();
				// can this be null?
				if (rotConfig == null)
					throw new Exception("Slew had null rotator onfig");
				// capitalize the instrument name to prevent problems later
				if (rotConfig.getRotatorMode() == IRotatorConfig.CARDINAL ||
					rotConfig.getRotatorMode() == IRotatorConfig.SKY) {
					String instrumentName = rotConfig.getInstrumentName();
					if (instrumentName == null) {
					    if (autofix)
						instrumentName = AUTO_INST;
					    else
						throw new Exception("Rotator config has null instrument name");
					}		
					((XRotatorConfig)rotConfig).setInstrumentName(instrumentName.toUpperCase());
				}
			} else if (action instanceof IInstrumentConfigSelector) {
				IInstrumentConfig config = ((IInstrumentConfigSelector) action).getInstrumentConfig();
				if (config == null)
					throw new Exception("InstConfigSelector had null config");
				String instrumentName = config.getInstrumentName();
				if (instrumentName != null)
					// capitalize the instrument name to prevent problems later
					((XInstrumentConfig) config).setInstrumentName(instrumentName.toUpperCase());
				addInstrumentConfig(((IInstrumentConfigSelector) action).getInstrumentConfig());
				
			} else if (action instanceof IBeamSteeringConfig) {
				
				IBeamSteeringConfig beam = (IBeamSteeringConfig)action;
				addBeamSteeringConfig(beam);
				
			} else if (action instanceof IAutoguiderConfig) {
				
				// If AG MAND for any obs, set required
				if (((IAutoguiderConfig) action).getAutoguiderCommand() == IAutoguiderConfig.ON)
					autoguiderRequired = true;
				
				// If AG OPT for any obs (and not already MAND), set optional
				if (((IAutoguiderConfig) action).getAutoguiderCommand() == IAutoguiderConfig.ON_IF_AVAILABLE) {
					if (!autoguiderRequired)
					autoguiderOptional = true;
				}
				
			} else if (action instanceof IAcquisitionConfig) {
				IAcquisitionConfig acq = (IAcquisitionConfig) action;
				String targetInstrumentName = acq.getTargetInstrumentName();
				if (targetInstrumentName != null)
					// capitalize the instrument name to prevent problems later
					((XAcquisitionConfig) acq).setTargetInstrumentName(targetInstrumentName.toUpperCase());
				String acqInstrumentName = acq.getAcquisitionInstrumentName();
				if (acqInstrumentName != null)
					// capitalize the instrument name to prevent problems later
					((XAcquisitionConfig) acq).setAcquisitionInstrumentName(acqInstrumentName.toUpperCase());
				addAcquistionConfig(acq);
			}

		} else if (seq instanceof XIteratorComponent) {
			// extract from each sub-element

			XIteratorComponent xit = (XIteratorComponent) seq;
			List list = xit.listChildComponents();
			Iterator ic = list.iterator();
			while (ic.hasNext()) {
				ISequenceComponent cseq = (ISequenceComponent) ic.next();
				extractComponents(cseq);
			}

		} else if (seq instanceof XBranchComponent) {
			// extract from both branches

			XBranchComponent xbran = (XBranchComponent) seq;

			List components = xbran.listChildComponents();
			ISequenceComponent red = (ISequenceComponent) components.get(0);
			extractComponents(red);

			// TODO WATCH THIS do we really mean 1, previously was 0 but why ?????
			ISequenceComponent blue = (ISequenceComponent) components.get(1);
			extractComponents(blue);

		}

	}

	/**
	 * @return A readable description of the set.
	 */
	public String toString() {
		return "Components: Targets=" + targets.size() + " Configs=" + instrumentConfigs.size();
	}

	public boolean isAutoguiderOptional() {
		// TODO Auto-generated method stub
		return false;
	}

}
