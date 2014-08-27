/**
 * 
 */
package ngat.sms;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.ems.SkyModel;

/**
 * @author eng
 *
 */
public class BasicSkyModelProvider extends UnicastRemoteObject implements SkyModelProvider {

	
	/** A sky model URL.*/
	private String skyModelUrl;
	
	
	
	/**
	 * @param skyModelUrl
	 * @throws RemoteException
	 */
	public BasicSkyModelProvider(String skyModelUrl) throws RemoteException {
		super();
		this.skyModelUrl = skyModelUrl;
	}

	/**
	 * @see ngat.sms.SkyModelProvider#getSkyModel()
	 */
	public SkyModel getSkyModel() throws Exception {		
		return (SkyModel)Naming.lookup(skyModelUrl);		
	}
	
}
