/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;
import java.util.Iterator;
import java.util.List;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IAccountModel;
import ngat.phase2.IAccount;
import ngat.phase2.ISemester;
import ngat.phase2.ISemesterPeriod;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;

/**
 * @author eng
 * 
 */
public class TestGetAccounts {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {

			IAccountModel acc = (IAccountModel) Naming.lookup("rmi://localhost/ProposalAccountModel");

			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

			int pid = cfg.getIntValue("pid");

			ISemesterPeriod semlist = acc.getSemesterPeriodOfDate(System.currentTimeMillis());
			ISemester s1 = semlist.getFirstSemester();
			ISemester s2 = semlist.getSecondSemester();
			
			System.err.println("Found: S1: "+s1);
			System.err.println("Found: S2: "+s2);
		
			if (s1 != null) {
				IAccount acc1 = acc.findAccount(pid, s1.getID());
				System.err.println("Acc: S1: "+acc1);
			}
			
			if (s2 != null) {
				IAccount acc2 = acc.findAccount(pid, s2.getID());
				System.err.println("Acc: S2: "+acc2);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
