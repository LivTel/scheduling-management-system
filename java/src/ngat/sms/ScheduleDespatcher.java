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
public interface ScheduleDespatcher extends Remote {

	public ScheduleItem nextScheduledJob() throws RemoteException;
	
}
