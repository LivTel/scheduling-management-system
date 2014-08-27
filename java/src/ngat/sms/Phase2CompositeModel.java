/**
 * 
 */
package ngat.sms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** A collective interface to the Phase2Model... maye rename as Phase2CollectiveModel ...
 * @author eng
 *
 */
public interface Phase2CompositeModel extends Remote {

	public List<GroupItem> listGroups() throws RemoteException;
	
}
