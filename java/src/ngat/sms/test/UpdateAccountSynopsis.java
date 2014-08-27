/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;

import ngat.sms.AccountSynopsisModel;
import ngat.sms.SynopticModelProvider;

/**
 * @author eng
 *
 */
public class UpdateAccountSynopsis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		try {
		
			long pid = Long.parseLong(args[0]);
			
			String comment = args[1];
			
			String client = args[2];
			
			double amt = Double.parseDouble(args[3]);
			
			SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup("SynopticModelProvider");
			AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
			
			pasm.chargeAccount(pid, amt, comment, client);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
