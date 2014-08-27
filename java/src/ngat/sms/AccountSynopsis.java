/**
 * 
 */
package ngat.sms;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.phase2.IAccount;
import ngat.phase2.ISemester;
import ngat.phase2.XGroup;

/** Synopsis of a set of accountsBySemester identified by name, up to a given date.
 * @author eng
 *
 */
public class AccountSynopsis implements Serializable {

	private ISemester earlySemester;
	
	private ISemester lateSemester;
	
	private IAccount earlySemesterAccount;
	
	private IAccount lateSemesterAccount;
	
	
	/** Maps the account semester to an account.*/
	//private Map<ISemester, IAccount> accountsBySemester;

	/** Maps the account ID to an account.*/
	private Map<Long, IAccount> accountsById;

	
	/**
	 * Create an AccountSynopsis.
	 * @param accountsBySemester The accountsBySemester map.
	 */
	public AccountSynopsis(ISemester earlySemester, 
						   IAccount earlySemesterAccount, 
						   ISemester lateSemester,
						   IAccount lateSemesterAccount) {
		this.earlySemester = earlySemester;
		this.earlySemesterAccount = earlySemesterAccount;
		this.lateSemester = lateSemester;
		this.lateSemesterAccount = lateSemesterAccount;
		
		accountsById = new HashMap<Long, IAccount>();
		// now map the 1 or 2 IDs.
		if (earlySemesterAccount != null)
			accountsById.put(earlySemesterAccount.getID(), earlySemesterAccount);
		if (lateSemesterAccount != null)
			accountsById.put(lateSemesterAccount.getID(), lateSemesterAccount);
		
	}
	
	
	
	/**
	 * @return An ordered (by semester) list of the set of accounts.
	 *//*
	public List<AccountWithSemester> listAccountsBySemester(){
		
		// ideally we want these sorted into semester order
		
		List<AccountWithSemester> list = new Vector<AccountWithSemester>();
		Iterator ia = accountsBySemester.keySet().iterator();
		while (ia.hasNext()) {
			ISemester semester = (ISemester)ia.next();
			IAccount account = accountsBySemester.get(semester);
			list.add(new AccountWithSemester(account, semester));
		}
		
		Collections.sort(list);
		return list;
	}*/
	
	
	
	/**
	 * @return An iterator over the set of accountsBySemester.
	 */
	//public Iterator<IAccount> listAccountsById(){
	//	return accountsById.values().iterator();
	//}
	
	/**
	 * @return the earlySemester
	 */
	public ISemester getEarlySemester() {
		return earlySemester;
	}



	/**
	 * @return the lateSemester
	 */
	public ISemester getLateSemester() {
		return lateSemester;
	}



	/**
	 * @return the earlySemesterAccount
	 */
	public IAccount getEarlySemesterAccount() {
		return earlySemesterAccount;
	}



	/**
	 * @return the lateSemesterAccount
	 */
	public IAccount getLateSemesterAccount() {
		return lateSemesterAccount;
	}



	/** Returns the Id'd account if available, otherwise...
	 * @param id The id of the account.
	 * @return The named account or ...
	 */
	public IAccount getAccount(long id) {
		return accountsById.get(id);
	}
	
	/** Standard method of adding an account.*//*
	public void addAccountBySemester(ISemester semester, IAccount account) {
		accountsBySemester.put(semester, account);
		accountsById.put(account.getID(), account);
	}*/
	
	public void deleteAccount(long id) {
		accountsById.remove(id);
	}
	
	public String toString() { return "Acc: "+earlySemesterAccount+", "+lateSemesterAccount; }
	
}
