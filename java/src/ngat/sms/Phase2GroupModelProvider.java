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
public interface Phase2GroupModelProvider extends Remote {

	public Phase2CompositeModel getPhase2Model() throws RemoteException;
	
}
