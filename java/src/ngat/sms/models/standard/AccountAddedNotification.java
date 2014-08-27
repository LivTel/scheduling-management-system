/**
 * 
 */
package ngat.sms.models.standard;

import ngat.phase2.IAccount;

/**
 * @author eng
 *
 */
public class AccountAddedNotification {

	private IAccount account;

	private long ownerId;
	
	private long semesterId;

	/**
	 * @param account
	 * @param ownerId
	 * @param semesterId
	 */
	public AccountAddedNotification(IAccount account, long ownerId, long semesterId) {
		super();
		this.account = account;
		this.ownerId = ownerId;
		this.semesterId = semesterId;
	}

	/**
	 * @return the account
	 */
	public IAccount getAccount() {
		return account;
	}

	/**
	 * @return the ownerId
	 */
	public long getOwnerId() {
		return ownerId;
	}

	/**
	 * @return the semesterId
	 */
	public long getSemesterId() {
		return semesterId;
	}
	
	

	
	
	
}
