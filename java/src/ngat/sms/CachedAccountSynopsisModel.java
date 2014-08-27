/**
 * 
 */
package ngat.sms;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ngat.oss.model.IAccountModel;
import ngat.phase2.IAccount;
import ngat.phase2.ISemester;
import ngat.phase2.XAccount;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class CachedAccountSynopsisModel implements AccountSynopsisModel {

	private AccountSynopsisModel asm;

	/** Primary cache, contains copy of remote synoptic model.*/
	Map<Long, AccountSynopsis> cache1;
	
	/** Secondary cache - contains temporary (test) entries.*/
	Map<Long, AccountSynopsis> cache2;

	
	LogGenerator logger;

	/**
	 * @param accModel
	 * @param cache
	 */
	public CachedAccountSynopsisModel(AccountSynopsisModel asm) {
		this.asm = asm;
		cache1 = new HashMap<Long, AccountSynopsis>();
		cache2 = new HashMap<Long, AccountSynopsis>();

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("SchedulingStatusProvider").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("casm");

	}

	/*
	 * Returns the cached value or the lower level account
	 * 
	 * @see ngat.sms.AccountSynopsisModel#getAccountSynopsis(long, long)
	 */
	public AccountSynopsis getAccountSynopsis(long pid, long time) throws RemoteException {
		
		// see if its in cache2
		if (cache2.containsKey(pid))
			return cache2.get(pid);

		// see if its in cache1
		if (cache1.containsKey(pid))
			return cache1.get(pid);

		
		// devolve to base model
		System.err.println("CASM::No entries in cache #1 or #2, getting base value");
		AccountSynopsis acsyn = asm.getAccountSynopsis(pid, time);
		
		System.err.println("CASM::Adding entry to cache #1");
		cache1.put(pid, acsyn);
		
		return acsyn;
	}

	/** Clear the cache #2 of all temporary data. */
	public void clearCache() {
		cache2.clear();
		System.err.println("CASM::Cache #2 size now: " + cache2.size());
	}

	public void chargeAccount(long pid, double amount, String comment, String clientRef)
			throws RemoteException {
		
		// 2SEM Needs re-write and how do we deal with forwarding ??
		// this one gets passed down to lower level (A1)
		asm.chargeAccount(pid, amount, comment, clientRef);
		
	}
	public void chargeAccount2(long pid, double amount, String comment, String clientRef)
			throws RemoteException {
		
		// 2SEM Needs re-write and how do we deal with forwarding ??
		// this is only a local update but we may have to create the AS here
		
	}
}
