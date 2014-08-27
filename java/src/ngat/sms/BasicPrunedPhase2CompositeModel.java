/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * @author eng
 *
 */
public class BasicPrunedPhase2CompositeModel extends UnicastRemoteObject implements Phase2CompositeModel {

	 /** Contains the pruned list of feasible groups.*/
	private List<GroupItem> prunedList;
		
	/**
	 * @param prunedList
	 * @throws RemoteException
	 */
	public BasicPrunedPhase2CompositeModel(List<GroupItem> prunedList) throws RemoteException {
		super();
		this.prunedList = prunedList;
	}



	/* (non-Javadoc)
	 * @see ngat.sms.Phase2CompositeModel#listGroups()
	 */
	public List<GroupItem> listGroups() throws RemoteException {		
		return prunedList;
	}

}
