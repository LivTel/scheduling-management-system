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
public interface Phase2Loader {

	/** Load external data into the specified cache.*/
	public void loadPhase2Data(BasicPhase2Cache cache) throws Exception;

	public void loadProposal(long pid, BasicPhase2Cache cache) throws Exception;
}
