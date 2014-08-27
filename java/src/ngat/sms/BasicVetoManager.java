/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 * @author eng
 *
 */
public class BasicVetoManager extends UnicastRemoteObject implements VetoManager {

	Map<Long, Veto> vetoMap;// 
	
	/**
	 * @param vetoMap
	 * @throws RemoteException
	 */
	public BasicVetoManager() throws RemoteException {
		super();
		vetoMap = new HashMap<Long, Veto>();
	}
	
	public void vetoGroup(long gid, long untilTime) throws RemoteException {
		vetoMap.put(new Long(gid), new Veto(gid, untilTime));
	}

	public void removeVeto(long gid) throws RemoteException {
		if (vetoMap.containsKey(gid))
			vetoMap.remove(gid);		
	}

	public long getVetoTime(long gid) throws RemoteException {
		if (vetoMap.containsKey(gid))
			return (vetoMap.get(gid)).untilTime;
		return -1L;
	}

	

	public List<Veto> listActiveVetos() throws RemoteException {
		List<Veto> vetos = new Vector<Veto>();
		Iterator<Long> ig = vetoMap.keySet().iterator();
		while (ig.hasNext()) {
			long gid = ig.next();
			Veto veto = vetoMap.get(gid);
			vetos.add(veto);
		}
		return vetos;
	}
	

}
