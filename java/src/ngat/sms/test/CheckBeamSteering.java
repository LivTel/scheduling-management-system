/**
 * 
 */
package ngat.sms.test;

import java.io.File;
import java.util.Iterator;

import ngat.phase2.IBeamSteeringConfig;
import ngat.phase2.IOpticalSlideConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XBeamSteeringConfig;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.phase2.XOpticalSlideConfig;
import ngat.sms.BasicTelescopeSystemsSynopsis;
import ngat.sms.ComponentSet;
import ngat.util.PropertiesConfigurator;

/**
 * @author eng
 *
 */
public class CheckBeamSteering {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	try {
		
		BasicTelescopeSystemsSynopsis scope = new BasicTelescopeSystemsSynopsis();
		PropertiesConfigurator.use(new File(args[0])).configure(scope);
		
		XIteratorComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));
		
		XOpticalSlideConfig up = new XOpticalSlideConfig(IOpticalSlideConfig.SLIDE_UPPER);
		up.setElementName(args[1]);
		
		XOpticalSlideConfig low = new XOpticalSlideConfig(IOpticalSlideConfig.SLIDE_LOWER);
		low.setElementName(args[2]);
		
		XBeamSteeringConfig beam = new XBeamSteeringConfig(up, low);
		XExecutiveComponent xbeam = new XExecutiveComponent("xbeam", beam);
		root.addElement(xbeam);
		
		// now test it
		
		ComponentSet cset = new ComponentSet(root);
		
		System.err.println("Cset contains: "+cset.countBeamSteeringConfigs()+" beamconfigs");
		
		Iterator bconfigs = cset.listBeamSteeringConfigs();
		while (bconfigs.hasNext()) {
			IBeamSteeringConfig tbeam = (IBeamSteeringConfig)bconfigs.next();
			System.err.println("Testing feasibility of beam config: " + tbeam);
			if (! scope.isValidBeamSteeringConfig(tbeam)) {
				System.err.println("This config is NOT valid");
			} else {
				System.err.println("This config IS valid");
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}

	}

}
