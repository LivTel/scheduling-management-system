/**
 * 
 */
package ngat.sms;

import java.rmi.Naming;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IAccountModel;
import ngat.oss.model.IHistoryModel;
import ngat.oss.model.IPhase2Model;

/**
 * @author eng
 *
 */
public class DefaultBaseModelProvider implements BaseModelProvider {

	private String remoteHost;
	
	/**
	 * @param remoteHost
	 */
	public DefaultBaseModelProvider(String remoteHost) {
		this.remoteHost = remoteHost;
	}
	
	public IAccessModel getAccessModel() throws Exception {
		return (IAccessModel)Naming.lookup("rmi://"+remoteHost+"/AccessModel");
	}

	public IAccountModel getProposalAccountModel() throws Exception {
		return (IAccountModel)Naming.lookup("rmi://"+remoteHost+"/ProposalAccountModel");
	}

    public IAccountModel getTagAccountModel() throws Exception {
	return (IAccountModel)Naming.lookup("rmi://"+remoteHost+"/TagAccountModel");
    }


	public IHistoryModel getHistoryModel() throws Exception {
		return (IHistoryModel)Naming.lookup("rmi://"+remoteHost+"/HistoryModel");
	}

	public IPhase2Model getPhase2Model() throws Exception {
		return (IPhase2Model)Naming.lookup("rmi://"+remoteHost+"/Phase2Model");
	}


}
