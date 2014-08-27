/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ngat.ems.SkyModel;

/**
 * @author eng
 *
 */
public interface SkyModelProvider extends Remote {

	
	/** Returna reference to the sky model.
	 * @return The skymodel.
	 * @throws RemoteException
	 */
	public SkyModel getSkyModel() throws Exception;
	
}
