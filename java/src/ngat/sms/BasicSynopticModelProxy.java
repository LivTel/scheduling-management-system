package ngat.sms;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ngat.oss.transport.RemotelyPingable;
import ngat.sms.util.FeasibilityPrescan;
import ngat.sms.util.PrescanEntry;

/**
 * 
 */

/**
 * A proxy for a synoptic model prvider within local JVM. This SMP may invoke a
 * pruning action.
 * 
 * @author eng
 * 
 */
public class BasicSynopticModelProxy implements SynopticModelProvider {

	/** Interval at which to retry lookup on external SMP. */
	private static final long LOOKUP_RETRY_DELAY = 30000L;

	/** Interval at which to retry pruning. */
	private static final long PRUNE_RETRY_DELAY = 30000L;

	
	
	/** External SMP host. */
	private String rhost;

	/** External Composite model. */
	Phase2CompositeModel p2g;
	
	/** Local pruned Compisite model.*/
	Phase2CompositeModel p2gLocal;

	/** External execution history synopsis model. */
	ExecutionHistorySynopsisModel hsm;

	/** External proposal account synopsis model. */
	AccountSynopsisModel pasm;

	/** External TAG account synopsis model. */
	AccountSynopsisModel tasm;

	/** External synoptic model provider. */
	SynopticModelProvider smp;

	/** Is the external SMP bound ?. */
	private boolean smpBound = false;
	
	/** Set true if a pruned model has been successfully loaded.*/
	private boolean havePrunedModel = false;

	/** Contains the pruned list.*/
	private List<GroupItem> pruneList;
	
	/**
	 * @param rhost
	 */
	public BasicSynopticModelProxy(String rhost) {
		this.rhost = rhost;
		pruneList = new Vector<GroupItem>();
	}

	public ExecutionHistorySynopsisModel getHistorySynopsisModel() throws RemoteException {
		if (smpBound)
			return smp.getHistorySynopsisModel();
		throw new RemoteException("External synopsis model provider is not bound");
	}

	public Phase2CompositeModel getPhase2CompositeModel() throws RemoteException {
		if (havePrunedModel)
			return p2gLocal;
		if (smpBound)
			return smp.getPhase2CompositeModel();
		throw new RemoteException("External synopsis model provider is not bound");
	}

	public AccountSynopsisModel getProposalAccountSynopsisModel() throws RemoteException {
		if (smpBound)
			return smp.getProposalAccountSynopsisModel();
		throw new RemoteException("External synopsis model provider is not bound");
	}

	public AccountSynopsisModel getTagAccountSynopsisModel() throws RemoteException {
		if (smpBound)
			return smp.getTagAccountSynopsisModel();
		throw new RemoteException("External synopsis model provider is not bound");
	}

	/** Keep trying to locate the external SMP. */
	private void loadSynopticModel() {

		// locate external SMP.
		while (true) {
			try {
				System.err.println("SMProxy:Attempting to lookup external SMP at: "+rhost);
				smp = (SynopticModelProvider) Naming.lookup("rmi://" + rhost + "/SynopticModelProvider");
				System.err.println("SMProxy:Located external SMP");
				((RemotelyPingable) smp).ping();
				System.err.println("SMProxy:External SMP is pingable");
				
				// now locate the various models.
				p2g = smp.getPhase2CompositeModel();
				
				smpBound = true;
			} catch (Exception e) {
				e.printStackTrace();
				smpBound = false;
			}
			try {
				Thread.sleep(LOOKUP_RETRY_DELAY);
			} catch (InterruptedException ix) {
			}
		}

	}

	/** Asynchronously locate the external SMP. Returns immediately. */
	public void asynchLoadSynopticModel() {
		Runnable r = new Runnable() {
			public void run() {
				loadSynopticModel();
			}
		};
		(new Thread(r)).start();
	}

	/** Try to prune the p2c model.*/
	private void pruneWhenReady(FeasibilityPrescan fp) {
				
		havePrunedModel = false; 
		// clear this flag so we get the remote smp reference not the model we are trying to create
	
		while (!havePrunedModel) {
			
			try {
				Thread.sleep(PRUNE_RETRY_DELAY);
			} catch (InterruptedException ix) {
			}
			
			// we may not have an external reference anyway...
			if (p2g == null)
				continue;
			
			try {
				
				// we have a p2g probably
				List candidates = fp.prescan(System.currentTimeMillis(), 60*1000L);
				// we now have a set of prescan entries
				Iterator ic = candidates.iterator();
				while (ic.hasNext()) {
					PrescanEntry pse = (PrescanEntry)ic.next();
					// add the group item to the prune list
					GroupItem group = pse.group;
					pruneList.add(group);
					// TODO what we should really do here is cache all the relevant linkages
					// TODO namely - group, proposal, and very importantly targets 
					// add prop if not in prop table
					
					System.err.println("SMProxy::prune(): Adding feasible group: "+group.getName());
				}
				
				p2gLocal = new BasicPrunedPhase2CompositeModel(pruneList);
				havePrunedModel = true;
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}
			
	}
	
	/** Asynchronously locate the external SMP. Returns immediately. */
	public void asynchPruneWhenReady(final FeasibilityPrescan fp) {
		Runnable r = new Runnable() {
			public void run() {
				pruneWhenReady(fp);
			}
		};
		(new Thread(r)).start();
	}

	public Phase2LoadController getLoadController() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
}
