/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

import ngat.phase2.*;

/** Provides an execution history synopsis.
 * @author eng
 *
 */
public interface ExecutionHistorySynopsisModel extends Remote {
    
    /**
     * @param gid The ID of the group for which we want an ExecutionHistorySynopsis.
     * @param time The time upto which we want the synopsis.
     * @return An ExecutionHistorySynopsis for the specified group upto time.
     */
    public ExecutionHistorySynopsis getExecutionHistorySynopsis(long gid, long time) throws RemoteException;
    
    /**
     * @param gid The ID of the group whose history is to be updated.
     * @param hid The ID of the specific history entry.
     * @param cstat Completion status.
     * @param ctime Completion time.
     * @param efc Execution failure information (or null).
     * @param qosStats QOS statistics.
     */
    public void updateHistory(long gid, long hid, int cstat, long ctime,
			      IExecutionFailureContext efc, Set qosStats) throws RemoteException;
     
    /** 
     * @param  gid The ID of the group for which a history entry is to be created.
     */
    public long addHistoryItem(long gid) throws RemoteException;

}
