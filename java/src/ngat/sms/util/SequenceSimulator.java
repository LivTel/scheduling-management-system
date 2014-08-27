/**
 * 
 */
package ngat.sms.util;

import java.util.List;
import java.util.Vector;

import ngat.phase2.IExecutiveAction;
import ngat.phase2.IIteratorCondition;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.sms.ExecutionResource;
import ngat.sms.ExecutionResourceUsageEstimationModel;

/**
 * Class to simulate the execution of an observation sequence.
 * 
 * @author eng
 * 
 */
public class SequenceSimulator {

	/** List of registered listeners. */
	private List<SequenceSimulationUpdateListener> listeners;

	/** The sequence root for which we will simulate execution. */
	private ISequenceComponent root;

	/** Execution timing model. */
	private ExecutionResourceUsageEstimationModel execModel;

	/**
	 * @param root
	 */
	public SequenceSimulator(ISequenceComponent root, ExecutionResourceUsageEstimationModel execModel) {
		this.root = root;
		this.execModel = execModel;
		listeners = new Vector<SequenceSimulationUpdateListener>();
	}

	/** Perform the execution simulation. */
	public void startSimulation() throws Exception {

		long start = System.currentTimeMillis();

		notifyListenersSimulationStarting(start);

		long end = execute(root, 0);

		notifyListenersSimulationFinished(start+end);

	}

	public void addSimulationListener(SequenceSimulationUpdateListener l) {
		if (listeners.contains(l))
			return;
		listeners.add(l);
	}

	/**
	 * Simulate execution of the supplied sequence.
	 * 
	 * @param c
	 *            A sequence component.
	 * @param time
	 *            The time we are starting the sequence.
	 * @return The time the sequence completes given the supplied start time..
	 */
	private long execute(ISequenceComponent c, long time) throws Exception {

		if (c == null)
			throw new IllegalArgumentException("SequenceSimulator;exec:Component was null");
		
		long mytime = time;

		if (c instanceof XExecutiveComponent) {
			XExecutiveComponent exec = (XExecutiveComponent) c;
			IExecutiveAction action = exec.getExecutiveAction();

			notifyListenersActionStarting(action, time);
			long delta = calculateExecTime(action);
			mytime += delta;
			notifyListenersActionFinished(action, mytime);
			return mytime;
		} else if (c instanceof XBranchComponent) {

			XBranchComponent branch = (XBranchComponent) c;
			List blist = branch.listChildComponents();

			long tmax = -999;
			// find the longest branch, thats the actual completion time
			for (int ib = 0; ib < blist.size(); ib++) {
				ISequenceComponent bc = (ISequenceComponent) blist.get(ib);
				mytime = execute(bc, time);
				if (mytime > tmax)
					tmax = mytime;
			}
			return tmax;
		} else if (c instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) c;

			// Execute_iterator (needs to repeat as many times as required)
			XIteratorRepeatCountCondition xrepeat = (XIteratorRepeatCountCondition) iter.getCondition();
			int nc = xrepeat.getCount();
			for (int ir = 0; ir < nc; ir++) {
				List ilist = iter.listChildComponents();
				for (int ii = 0; ii < ilist.size(); ii++) {
					ISequenceComponent bc = (ISequenceComponent) ilist.get(ii);
					mytime = execute(bc, mytime);
				}
			}
			return mytime;
		}

		// impossible there are no other types of component !!!
		throw new IllegalArgumentException("SequenceSimulator;exec:Unknown component type: "+c.getClass().getName());
	}

	private long calculateExecTime(IExecutiveAction action) {
		XExecutiveComponent c = new XExecutiveComponent("", action);
		return (long) execModel.getExecTime(c);
	}

	private void notifyListenersSimulationStarting(long t) {

		for (int i = 0; i < listeners.size(); i++) {
			SequenceSimulationUpdateListener l = listeners.get(i);
			try {
				l.simulationStarting(t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void notifyListenersSimulationFinished(long t) {

		for (int i = 0; i < listeners.size(); i++) {
			SequenceSimulationUpdateListener l = listeners.get(i);
			try {
				l.simulationFinished(t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void notifyListenersActionStarting(IExecutiveAction action, long t) {

		for (int i = 0; i < listeners.size(); i++) {
			SequenceSimulationUpdateListener l = listeners.get(i);
			try {
				l.startingAction(action, t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void notifyListenersActionFinished(IExecutiveAction action, long t) {

		for (int i = 0; i < listeners.size(); i++) {
			SequenceSimulationUpdateListener l = listeners.get(i);
			try {
				l.finishedAction(action, t);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
