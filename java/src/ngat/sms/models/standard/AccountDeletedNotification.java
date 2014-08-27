/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.IAccount;

/**
 * @author eng
 *
 */
public class AccountDeletedNotification {
	
	IAccount account;

	/**
	 * @param account
	 */
	public AccountDeletedNotification(IAccount account) {
		super();
		this.account = account;
	}

	/**
	 * @return the account
	 */
	public IAccount getAccount() {
		return account;
	}
	
	
	
}
