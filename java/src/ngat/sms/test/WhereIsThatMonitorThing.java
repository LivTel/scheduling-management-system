/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;

import ngat.oss.model.IAccountModel;
import ngat.oss.monitor.AccountMonitor;

/**
 * @author eng
 *
 */
public class WhereIsThatMonitorThing {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			IAccountModel pam = (IAccountModel)Naming.lookup("rmi://localhost/ProposalAccountModel");
			System.err.println("Lookup found: "+pam.getClass().getName());
			
			Class pc = pam.getClass();
			Class[] ic = pc.getInterfaces();
			for (int i = 0; i < ic.length; i++) {
				System.err.println("It isa ["+ic[i].getName()+"]");
			}
			
			AccountMonitor mon = (AccountMonitor)pam;
			System.err.println("It has worked !");
			
		} catch (Exception e) {
			System.err.println("It has NOT worked !");
			e.printStackTrace();			
		}
	}

}
