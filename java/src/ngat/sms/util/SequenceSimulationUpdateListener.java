/**
 * 
 */
package ngat.sms.util;

import ngat.phase2.IExecutiveAction;

/**
 * @author eng
 *
 */
public interface SequenceSimulationUpdateListener {

	public void simulationStarting(long time) throws Exception;
	
	public void simulationFinished(long time) throws Exception;
	
	public void startingAction(IExecutiveAction action, long time) throws Exception;
	
	public void finishedAction(IExecutiveAction action, long time) throws Exception;
	
}
