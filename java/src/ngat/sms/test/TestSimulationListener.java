/**
 * 
 */
package ngat.sms.test;

import ngat.phase2.IExecutiveAction;
import ngat.sms.util.SequenceSimulationUpdateListener;

/**
 * @author eng
 *
 */
public class TestSimulationListener implements SequenceSimulationUpdateListener {

	/* (non-Javadoc)
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#simulationStarting(long)
	 */
	public void simulationStarting(long time) throws Exception {
		System.err.println("Start sim: at: "+time);
	}

	/* (non-Javadoc)
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#simulationFinished(long)
	 */
	public void simulationFinished(long time) throws Exception {
		System.err.println("End sim: at: "+time);
	}

	/* (non-Javadoc)
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#startingAction(ngat.phase2.IExecutiveAction, long)
	 */
	public void startingAction(IExecutiveAction action, long time) throws Exception {
		System.err.println("Start action: "+action.getClass().getName()+" at: "+time/1000);
	}

	/* (non-Javadoc)
	 * @see ngat.sms.util.SequenceSimulationUpdateListener#finishedAction(ngat.phase2.IExecutiveAction, long)
	 */
	public void finishedAction(IExecutiveAction action, long time) throws Exception {
		System.err.println("End action: "+action.getClass().getName()+" at: "+time/1000);
		System.err.println();
	}

}
