package ngat.sms;
import java.rmi.RemoteException;


/**
 * 
 */

/**
 * @author eng
 *
 */
public class CachedSynopticModelProvider implements SynopticModelProvider {
	
	/** Phase2 composite model.*/
	private Phase2CompositeModel phase2CompositeModel;
	
	/** Proposal account model.*/
	private CachedAccountSynopsisModel proposalAccountSynopsisModel;
	
	/** TAG account model.*/
	private CachedAccountSynopsisModel tagAccountSynopsisModel;
	
	/** History synopsis.*/
	private CachedHistorySynopsisModel historySynopsisModel;

	/**
	 * @return the phase2CompositeModel
	 */
	public Phase2CompositeModel getPhase2CompositeModel() throws RemoteException {
		return phase2CompositeModel;
	}

	/**
	 * @param phase2CompositeModel the phase2CompositeModel to set
	 */
	public void setPhase2CompositeModel(Phase2CompositeModel phase2CompositeModel) {
		this.phase2CompositeModel = phase2CompositeModel;
	}

	/**
	 * @return the proposalAccountSynopsisModel
	 */
	public AccountSynopsisModel getProposalAccountSynopsisModel() throws RemoteException {
		return proposalAccountSynopsisModel;
	}

	/**
	 * @param proposalAccountSynopsisModel the proposalAccountSynopsisModel to set
	 */
	public void setProposalAccountSynopsisModel(CachedAccountSynopsisModel proposalAccountSynopsisModel) {
		this.proposalAccountSynopsisModel = proposalAccountSynopsisModel;
	}

	/**
	 * @return the tagAccountSynopsisModel
	 */
	public AccountSynopsisModel getTagAccountSynopsisModel() throws RemoteException {
		return tagAccountSynopsisModel;
	}

	/**
	 * @param tagAccountSynopsisModel the tagAccountSynopsisModel to set
	 */
	public void setTagAccountSynopsisModel(CachedAccountSynopsisModel tagAccountSynopsisModel) {
		this.tagAccountSynopsisModel = tagAccountSynopsisModel;
	}

	/**
	 * @return the historySynopsisModel
	 */
	public ExecutionHistorySynopsisModel getHistorySynopsisModel() throws RemoteException {
		return historySynopsisModel;
	}

	/**
	 * @param historySynopsisModel the historySynopsisModel to set
	 */
	public void setHistorySynopsisModel(CachedHistorySynopsisModel historySynopsisModel) {
		this.historySynopsisModel = historySynopsisModel;
	}

	
	/**
	 * Clear out any cached data, returning enclosed models to orginal states.
	 */
	public void clearCaches() {
		proposalAccountSynopsisModel.clearCache();
		tagAccountSynopsisModel.clearCache();
		historySynopsisModel.clearCache();
	}

	public Phase2LoadController getLoadController() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
