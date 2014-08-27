/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.IProposal;

/**
 * @author eng
 *
 */
public class ProposalUpdatedNotification extends Phase2UpdateNotification {

	private IProposal proposal;

	/**
	 * @param proposal
	 */
	public ProposalUpdatedNotification(IProposal proposal) {		
		this.proposal = proposal;
	}

	/**
	 * @return the proposal
	 */
	public IProposal getProposal() {
		return proposal;
	}
	
	
}
