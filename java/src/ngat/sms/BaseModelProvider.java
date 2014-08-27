/**
 * 
 */
package ngat.sms;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IAccountModel;
import ngat.oss.model.IHistoryModel;
import ngat.oss.model.IPhase2Model;

/**
 * @author eng
 *
 */
public interface BaseModelProvider {

	public IAccessModel getAccessModel() throws Exception;
	
	public IHistoryModel getHistoryModel() throws Exception;
	
	public IAccountModel getProposalAccountModel() throws Exception;
	
    public IAccountModel getTagAccountModel() throws Exception;

	public IPhase2Model getPhase2Model() throws Exception;
	
}
