/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author eng
 *
 */
public interface Phase2LoadController extends Remote {

	public void loadProposal(long pid) throws RemoteException;
	
	
}
