/**
 * 
 */
package ngat.sms;

import java.rmi.*;

/**
 * @author eng
 *
 */
public interface SynopticModelProvider extends Remote{

	public ExecutionHistorySynopsisModel getHistorySynopsisModel() throws RemoteException;
	
	public Phase2CompositeModel getPhase2CompositeModel() throws RemoteException;
	
	public AccountSynopsisModel getProposalAccountSynopsisModel() throws RemoteException;
	
	public AccountSynopsisModel getTagAccountSynopsisModel() throws RemoteException;
	
	public Phase2LoadController getLoadController() throws RemoteException;
	
}
