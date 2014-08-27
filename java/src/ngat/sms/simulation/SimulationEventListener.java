package ngat.sms.simulation;

import ngat.sms.ScheduleItem;

/** Receives event notifications during the progress of a simulation.*/
public interface SimulationEventListener {

    /** Notification that a group represented by the specified Metric was selected at time.*/
    public void groupSelected(ScheduleItem item);

    /** Notification of contention info at time*/
    public void contentionResults(int contention);

    /** Notification that simulation has completed.*/
    public void simulationCompleted();

    /** Handle failure of group.*/
	public void groupFailed(ScheduleItem selected, long time, String execFailReason);

	/** Handle completion of group.*/
	public void groupCompleted(ScheduleItem selected, long time);

}
