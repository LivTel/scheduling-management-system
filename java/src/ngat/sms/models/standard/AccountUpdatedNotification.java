package ngat.sms.models.standard;

import ngat.phase2.IAccount;

public class AccountUpdatedNotification {

	IAccount account;

	/**
	 * @param account
	 */
	public AccountUpdatedNotification(IAccount account) {
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
