/**
 * 
 */
package ngat.sms;

import java.rmi.*;

import ngat.phase2.ISemester;

/**
 * @author eng
 *
 */
public interface AccountSynopsisModel extends Remote {

	/** Retrieve the synopsis for a specified proposal for the given time.
	 * @param pid The ID of the proposal for which we want an account synopsis.
	 * @param time The time upto which we want the synopsis.
	 * @return The account-synopsis for the specified proposal.
	 */
	public AccountSynopsis getAccountSynopsis(long pid, long time) throws RemoteException;

	
	/** Update the consumed value for the specified account (by name).
	 * @param pid The id of the proposal whose account is to be charged.
	 * @param amount The amount to charge.
	 * @param comment Client comment - (what/why).
	 * @param clientRef A client identifier reference.
	 */
	public void chargeAccount(long pid, double amount, String comment, String clientRef) throws RemoteException;
	
}