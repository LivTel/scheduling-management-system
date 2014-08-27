/**
 *
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.util.*;

import ngat.oss.model.*;
import ngat.phase2.*;

/**
 * @author eng
 * 
 */
public class CachedHistorySynopsisModel implements ExecutionHistorySynopsisModel {

	/** A base model to extract data from. */
	private ExecutionHistorySynopsisModel hsm;
	
	/** Primary cache, contains copy of remote synoptic model.*/
	Map<Long, ExecutionHistorySynopsis> cache1;
	
	/** Secondary cache - contains temporary (test) entries.*/
	Map<Long, ExecutionHistorySynopsis> cache2;
	
	/**
	 * @param historyModel
	 */
	public CachedHistorySynopsisModel(ExecutionHistorySynopsisModel hsm) {
		this.hsm = hsm;
		cache1 = new HashMap<Long, ExecutionHistorySynopsis>();
		cache2 = new HashMap<Long, ExecutionHistorySynopsis>();
	}

	/** Either return the value in the cache or devolve to the lower model. We do NOT ever modify the base model here.*/
	public ExecutionHistorySynopsis getExecutionHistorySynopsis(long gid, long time) throws RemoteException {
		
		// see if its in cache2
		if (cache2.containsKey(gid))
			return cache2.get(gid);
		
		// see if its in cache1
		if (cache1.containsKey(gid))
			return cache1.get(gid);
		
		// devolve to base model
		System.err.println("CHSM::No entries in cache #1 or #2, getting base value");
		ExecutionHistorySynopsis history = hsm.getExecutionHistorySynopsis(gid, time);
		
		System.err.println("CHSM::Adding entry to cache #1");
		cache1.put(gid, history);
		
		return history;

	}
	
	/** Update the cached model (#2).*/
	public void updateHistory2(long gid, long hid, int cstat, long ctime, IExecutionFailureContext efc, Set qosStats) throws RemoteException {

		// update cache if this was successful execution
		if (cstat == IHistoryItem.EXECUTION_SUCCESSFUL) {
			ExecutionHistorySynopsis exec = null;
			// update local cache
			if (cache2.containsKey(gid)) {
				// this might happen if a short period group was executed more than once in a candidate sequence.
				exec = cache2.get(gid);
			} else {
				// this would happen if a group was executed in a candidate sequence, normally once.
				exec = new ExecutionHistorySynopsis();
				cache2.put(new Long(gid), exec);
			}
			
			exec.setLastExecution(ctime);
			exec.setCountExecutions(exec.getCountExecutions() + 1);
			System.err.println("CHSM::Update locally cached (#2) history: Ctime=" + ctime + ", Count = "
					+ exec.getCountExecutions());
		}
			
	}
	
	/** Update the cached model (#1).*/
	public void updateHistory(long gid, long hid, int cstat, long ctime, IExecutionFailureContext efc, Set qosStats) throws RemoteException {

		// update cache if this was successful execution
		if (cstat == IHistoryItem.EXECUTION_SUCCESSFUL) {
			ExecutionHistorySynopsis exec = null;
			// update local cache
			if (cache1.containsKey(gid)) {
				// this might happen if a short period group was executed more than once in a candidate sequence.
				exec = cache1.get(gid);
			} else {
				// this would happen if a group was executed in a candidate sequence, normally once.
				exec = new ExecutionHistorySynopsis();
				cache1.put(new Long(gid), exec);
			}
			
			exec.setLastExecution(ctime);
			exec.setCountExecutions(exec.getCountExecutions() + 1);
			System.err.println("CHSM::Update locally cached history: Ctime=" + ctime + ", Count = "
					+ exec.getCountExecutions());
			
			
		}

	}

	/**
	 * @param gid
	 *            The ID of the group for which a history entry is to be
	 *            created.
	 */
	public long addHistoryItem(long gid) throws RemoteException  {
		// check we have this group, if not setup an entry for it with t=0, c=0
		if (!cache2.containsKey(gid))
			cache2.put(new Long(gid), new ExecutionHistorySynopsis());
		return 0; // irrelevant as we will never use it...
	}

	/** Clear the cache #2 of all temporary data. */
	public void clearCache() {
		cache2.clear();
		System.err.println("CHSM::Cache #2 size now: " + cache2.size());
	}

}