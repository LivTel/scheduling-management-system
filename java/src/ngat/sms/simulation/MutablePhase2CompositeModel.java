/**
 * 
 */
package ngat.sms.simulation;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import com.sun.management.UnixOperatingSystemMXBean;

import ngat.sms.GroupItem;
import ngat.sms.Phase2CompositeModel;

/** A mutable composite model allows additional groups to be added to a special proposal.
 *
 * @author eng
 *
 */
public class MutablePhase2CompositeModel extends UnicastRemoteObject implements Phase2CompositeModel {

    private Phase2CompositeModel p2base;

    private List<GroupItem> extraGroups;

    public MutablePhase2CompositeModel(Phase2CompositeModel p2base) throws RemoteException {
	super();
	extraGroups = new Vector<GroupItem>();
    }

    public List<GroupItem> listGroups() throws RemoteException {
	List<GroupItem> baseListGroups = p2base.listGroups();
	List<GroupItem> allGroups = new Vector<GroupItem>(baseListGroups);
	allGroups.addAll(extraGroups);
	return allGroups;
    }

    public void clearExtraGroups() {
	extraGroups.clear();
    }
	
}
