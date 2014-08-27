/**
 * 
 */
package ngat.sms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.oss.impl.mysql.model.Phase2Model;
import ngat.oss.listeners.AccountingModelUpdateListener;
import ngat.oss.model.IAccountModel;
import ngat.phase2.IAccount;
import ngat.phase2.IProposal;
import ngat.phase2.ISemester;
import ngat.phase2.ITag;
import ngat.phase2.ITransaction;
import ngat.phase2.XAccount;
import ngat.phase2.ISemesterPeriod;
import ngat.sms.models.standard.AccountAddedNotification;
import ngat.sms.models.standard.AccountDeletedNotification;
import ngat.sms.models.standard.AccountUpdatedNotification;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

import java.rmi.*;
import java.rmi.server.*;

/**
 * @author eng
 * 
 */
public class BasicAccountSynopsisModel extends UnicastRemoteObject implements AccountSynopsisModel,
									      AccountingModelUpdateListener {

    public static final long UPDATE_LIST_POLLING_INTERVAL = 60000L;

    BaseModelProvider bmp;

    // IAccountModel accModel;

    Map<Long, AccountSynopsis> cache;

    LogGenerator logger;

    private String modelName;

    private String modelClientType;

    /** Records updates. */
    private List updateList;

    /**
     * @param accModel
     * @param cache
     */
    public BasicAccountSynopsisModel(BaseModelProvider bmp) throws RemoteException {
	super();
	this.bmp = bmp;
	cache = new HashMap<Long, AccountSynopsis>();
	updateList = new Vector();

	Logger alogger = LogManager.getLogger("SMS");
	logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
	    .srcCompId("basm");

    }

    public void loadProposalAccounts(Phase2CompositeModel p2g, long time) throws Exception {
	modelName = "PASM";
	modelClientType = "Proposal";
	// IAccountModel accModel = bmp.getProposalAccountModel();

	List<GroupItem> lgroups = p2g.listGroups();
	Iterator<GroupItem> groups = lgroups.iterator();
	while (groups.hasNext()) {
	    GroupItem group = groups.next();
	    IProposal prop = group.getProposal();
	    long pid = prop.getID();
	    if (!cache.containsKey(pid)) {
		// TODO acsyn = getRemoteAccountSynopsis(group.getID(), SemID);/
		AccountSynopsis acsyn = getRemoteAccountSynopsis(pid, time);
		cache.put(new Long(pid), acsyn);
		logger.create().extractCallInfo().info().level(3)
		    .msg("Loaded Proposal account synopsis for: " + group.getName()).send();
	    } else {
		logger.create().extractCallInfo().info().level(3)
		    .msg("Already got Proposal account synopsis for: " + group.getName()).send();
	    }

	}
	logger.create().extractCallInfo().info().level(2).msg("Proposal Accounts loaded").send();

    }

    public void loadTagAccounts(Phase2CompositeModel p2g, long time) throws Exception {
	modelName = "TASM";
	modelClientType = "TAG";
	// IAccountModel accModel = bmp.getTagAccountModel();

	List<GroupItem> lgroups = p2g.listGroups();
	Iterator<GroupItem> groups = lgroups.iterator();
	while (groups.hasNext()) {
	    GroupItem group = groups.next();
	    ITag tag = group.getTag();
	    long tid = tag.getID();
	    if (!cache.containsKey(tid)) {
		// TODO acsyn = getRemoteAccountSynopsis(group.getID(), SemID);/
		AccountSynopsis acsyn = getRemoteAccountSynopsis(tid, time);
		cache.put(new Long(tid), acsyn);
		logger.create().extractCallInfo().info().level(3)
		    .msg("Loaded TAG account synopsis for: " + group.getName()).send();
	    } else {
		logger.create().extractCallInfo().info().level(3)
		    .msg("Already got TAG account synopsis for: " + group.getName()).send();
	    }
	}
	logger.create().extractCallInfo().info().level(2).msg("Tag Acounts loaded").send();

    }

    public void startUpdateMonitor() {
	UpdateMonitor monitor = new UpdateMonitor();
	monitor.start();
	System.err.println("Started account update monitor " + modelName);
    }

    private AccountSynopsis getRemoteAccountSynopsis(long pid, long time) throws Exception {

	AccountSynopsis acsyn = null;
	IAccountModel accModel = null;

	// decide which remote account we need to access
	if (modelName.equals("PASM")) {
	    accModel = bmp.getProposalAccountModel();
	} else {
	    accModel = bmp.getTagAccountModel();
	}

	try {

	    ISemesterPeriod semList = accModel.getSemesterPeriodOfDate(time);

	    boolean overlap = semList.isOverlap();
	    ISemester early = semList.getFirstSemester();
	    ISemester later = semList.getSecondSemester();

	    logger.create()
		.extractCallInfo()
		.info()
		.level(3)
		.msg("Creating account synopsis for: " + modelClientType + ": " + pid + " Found semesters: "
		     + early + ", " + later).send();

	    IAccount earlyAccount = null;
	    IAccount laterAccount = null;

	    double bal1 = 0.0;
	    double alloc1 = 0.0;
	    double cons1 = 0.0;

	    double bal2 = 0.0;
	    double alloc2 = 0.0;
	    double cons2 = 0.0;

	    if (early != null) {
		earlyAccount = accModel.findAccount(pid, early.getID());
		if (earlyAccount != null) {
		    alloc1 = earlyAccount.getAllocated();
		    cons1 = earlyAccount.getConsumed();
		    bal1 = alloc1 - cons1;
		}
	    }
	    if (later != null) {
		laterAccount = accModel.findAccount(pid, later.getID());
		if (laterAccount != null) {
		    alloc2 = laterAccount.getAllocated();
		    cons2 = laterAccount.getConsumed();
		    bal2 = alloc2 - cons2;
		}
	    }

	    // TODO 2SEM at this point we should have either 2 accounts or 1

	    logger.create()
		.extractCallInfo()
		.info()
		.level(3)
		.msg("Creating account synopsis for: "
		     + modelClientType
		     + ": "
		     + pid
		     + " Sem:"
		     + (early != null ? "(" + early.getName() + " " + alloc1 + " - " + cons1 + " = " + bal1
			+ ")" : "NONE")
		     + " Sem:"
		     + (later != null ? "(" + later.getName() + " " + alloc2 + " - " + cons2 + " = " + bal2
			+ ")" : "NONE")).send();

	    // Creating account synopsis for: Proposal: 343 (Sem:13A 12.0 - 8.4
	    // = 2.6) (Sem:13B 10.0 - 0.0 = 10.0)
	    acsyn = new AccountSynopsis(early, earlyAccount, later, laterAccount);

	} catch (Exception e) {
	    e.printStackTrace();
	}
	return acsyn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ngat.sms.AccountSynopsisModel#getAccountSynopsis(long, long)
     */
    public AccountSynopsis getAccountSynopsis(long pid, long time) throws RemoteException {
	try {
	    if (cache.containsKey(pid)) {
		logger.create().extractCallInfo().info().level(3)
		    .msg("Located account synopsis for: " + modelClientType + ": " + pid + " in cache").send();
		return cache.get(pid);
	    }
	    logger.create().extractCallInfo().info().level(3)
		.msg("Account synopsis for: " + modelClientType + ": " + pid + " is not currently in cache").send();
	    AccountSynopsis acsyn = getRemoteAccountSynopsis(pid, time);
	    cache.put(new Long(pid), acsyn);

	    return acsyn;
	} catch (Exception e) {
	    throw new RemoteException(this.getClass().getName() + "(" + modelName + ").getAccountSynopsis()", e);
	}
    }

    public void chargeAccount(long pid, double amount, String comment, String clientRef) throws RemoteException {
	try {
	    IAccountModel accModel = null;

	    // decide which remote account we need to access
	    if (modelName.equals("PASM")) {
		accModel = bmp.getProposalAccountModel();
	    } else {
		accModel = bmp.getTagAccountModel();
	    }

	    AccountSynopsis acsyn = getAccountSynopsis(pid, System.currentTimeMillis());

	    ISemester earlySemester = acsyn.getEarlySemester();
	    IAccount earlySemesterAccount = acsyn.getEarlySemesterAccount();
	    ISemester lateSemester = acsyn.getLateSemester();
	    IAccount lateSemesterAccount = acsyn.getLateSemesterAccount();

	    logger.create().extractCallInfo().info().level(3)
		.msg("Found synopsis:  " + modelClientType + ": " + pid + acsyn).send();

	    // one or other account may be null as for the semesters

	    // record balance for the semesters
	    double earlySemesterBalance = 0.0;
	    double lateSemesterBalance = 0.0;

	    // see if we can charge the early semester
	    if (earlySemesterAccount != null) {
		double allocated = earlySemesterAccount.getAllocated();
		double consumed = earlySemesterAccount.getConsumed();
		earlySemesterBalance = allocated - consumed;

		logger.create()
		    .extractCallInfo()
		    .info()
		    .level(3)
		    .msg("Checking account for " + modelClientType + ": " + pid + " Sem:" + earlySemester.getName()
			 + " A: " + allocated + " - C: " + consumed + " Bal: " + (earlySemesterBalance)).send();

		// if we have <<anything at all>> in S1 go ahead, and were done
		if (earlySemesterBalance <= 0.0) {
		    logger.create().extractCallInfo().info().level(3)
			.msg("Account is overdrawn for: " + earlySemesterAccount.getID()).send();
		} else {
		    accModel.modifyConsumed(earlySemesterAccount.getID(), amount, comment, clientRef);
		    logger.create()
			.extractCallInfo()
			.info()
			.level(3)
			.msg("Update locally cached account for " + modelClientType + ": " + pid + ", Acc: "
			     + earlySemester.getName() + ", amount: " + amount + ", " + comment + ", by "
			     + clientRef).send();
		    return;
		}
	    }

	    // see if we can charge the late semester
	    if (lateSemesterAccount != null) {
		double allocated = lateSemesterAccount.getAllocated();
		double consumed = lateSemesterAccount.getConsumed();
		lateSemesterBalance = allocated - consumed;

		logger.create()
		    .extractCallInfo()
		    .info()
		    .level(3)
		    .msg("Checking account for " + modelClientType + ": " + pid + " Sem:" + lateSemester.getName()
			 + " A: " + allocated + " - C: " + consumed + " Bal: " + (lateSemesterBalance)).send();

		// Early semester was not charged so MUST try to charge late
		// semester
		accModel.modifyConsumed(lateSemesterAccount.getID(), amount, comment, clientRef);
		logger.create()
		    .extractCallInfo()
		    .info()
		    .level(3)
		    .msg("Update locally cached account for " + modelClientType + ": " + pid + ", Acc: "
			 + lateSemester.getName() + ", amount: " + amount + ", " + comment + ", by " + clientRef)
		    .send();
		return;
	    }

	    // TODO do we actually ever modify the cached value here ???
	    logger.create().extractCallInfo().info().level(3).msg("No accounts could be updated").send();
	} catch (Exception e) {
	    throw new RemoteException(this.getClass().getName() + "(" + modelName + ").chargeAccount()", e);
	}
    }

    public void accountAdded(IAccount account, long ownerId, long semesterId) throws RemoteException {
	logger.create().block("accountAdded").info().level(1)
	    .msg("Adding accountAdded request: "+modelClientType+": "+ownerId+" sem: "+semesterId+" to update queue for account: " + account).send();

	// an account (or semester of an account has been added)
	AccountAddedNotification aan = new AccountAddedNotification(account, ownerId, semesterId);
	addEvent(aan);
    }

    public void accountDeleted(long arg0) throws RemoteException {
	// TODO Auto-generated method stub

    }

    public void accountUpdated(IAccount account) throws RemoteException {
	logger.create()
	    .block("accountUpdated")
	    .info()
	    .level(1)
	    .msg("Adding accountUpdated request to update queue for account: " + account).send();

	AccountUpdatedNotification aun = new AccountUpdatedNotification(account);
	addEvent(aun);
    }

    private void addEvent(Object o) {
	updateList.add(o);
	logger.create()
            .block("addEvent")
            .info()
            .level(1)
            .msg("Adding event class: "+(o != null ? o.getClass().getSimpleName() : "null")+ ", list size: "+updateList.size())
	    .send();
    }

    private class UpdateMonitor extends Thread {

	int nc = 0;

	/**
	 * @param access
	 * @param phase2
	 */
	public UpdateMonitor() {
	    super();

	}

	public void run() {

	    IAccountModel accModel = null;

	    while (true) {
		nc++;

		try {
		    // we only log the test every 5 cycles, but actual updates are
		    // all recorded
		    //		if (nc % 5 == 0)
		    logger.create().block(modelName + ".monitor.run").info().level(1).msg("Checking updatelist: size: "+updateList.size()).send();
		    // check for updates..
		    while (!updateList.isEmpty()) {
			
			logger.create().block(modelName + ".monitor.run").info().level(1)
			    .msg("Update list contains " + updateList.size() + " items for processing").send();
			
			Object update = updateList.remove(0);
			
			if (update != null) {
			    logger.create().block(modelName + ".monitor.run").info().level(1)
				.msg("Processing update class: " + update.getClass().getSimpleName()).send();
			}
			
			if (update instanceof AccountAddedNotification) {
			    
			    try {
				AccountAddedNotification aan = (AccountAddedNotification) update;
				IAccount account = aan.getAccount();
				
				XAccount xacc = (XAccount) account;
				xacc.setName("allocation");
				
				// decide which remote account we need to access
				if (modelName.equals("PASM"))
				    accModel = bmp.getProposalAccountModel();
				else
				    accModel = bmp.getTagAccountModel();
				
				long pid = aan.getOwnerId();
				long sid = aan.getSemesterId();
				
				ISemester semester = accModel.getSemester(sid);
				
				// what semesters are current ? - we are assuming
				// its for now !
				ISemesterPeriod semlist = accModel.getSemesterPeriodOfDate(System.currentTimeMillis());
				
				ISemester s1 = semlist.getFirstSemester();
				ISemester s2 = semlist.getSecondSemester();
				
				if ( (s1 == null || s1.getID() != sid) && (s2 == null || s2.getID() != sid)) {
				    logger.create()
					.block(modelName + ".monitor.run")
					.info()
					.level(1)
					.msg("Account was not added for " + modelClientType + ": " + pid
					     + " specified semester " + semester.getName() + " is not current")
					.send();
				    continue;
				}
				
				// Semester is current either retrieve the accsyn or create one here.
				AccountSynopsis acsyn = getAccountSynopsis(pid, System.currentTimeMillis());
				
				// NOTE we dont actually use the account info passed in, we have just retrieved it from accmodel !
				
			    } catch (Exception e) {
				e.printStackTrace();
			    }
			    
			} else if (update instanceof AccountUpdatedNotification) {
			    try {
				AccountUpdatedNotification aun = (AccountUpdatedNotification) update;
				IAccount account = aun.getAccount();

				XAccount xacc = (XAccount) account;
				xacc.setName("allocation");

				// decide which remote account we need to access
				if (modelName.equals("PASM"))
				    accModel = bmp.getProposalAccountModel();
				else
				    accModel = bmp.getTagAccountModel();

				long pid = accModel.getAccountOwnerID(account.getID());

				// retrieve or create new one
				AccountSynopsis acsyn = getAccountSynopsis(pid, System.currentTimeMillis());

				// we may need to create this account but we cant not enough info							
				if (acsyn.getAccount(account.getID()) == null) {
								
				    logger.create()
					.block(modelName + ".monitor.run")
					.info()
					.level(1)
					.msg("Account could not be added for " + modelClientType +": " + pid
					     + " no such semester account").send();
				    continue;
				}

				// known account, change its content
				((XAccount) (acsyn.getAccount(account.getID()))).setAllocated(account.getAllocated());
				((XAccount) (acsyn.getAccount(account.getID()))).setConsumed(account.getConsumed());
				logger.create().block(modelName + ".monitor.run").info().level(1)
				    .msg("Existing account has been updated for " + pid).send();

			    } catch (Exception e) {
				e.printStackTrace();
			    }
			} else if (update instanceof AccountDeletedNotification) {
			    try {
				AccountDeletedNotification adn = (AccountDeletedNotification) update;

				IAccount account = adn.getAccount();

				XAccount xacc = (XAccount) account;
				xacc.setName("allocation");

				// decide which remote account we need to access
				if (modelName.equals("PASM"))
				    accModel = bmp.getProposalAccountModel();
				else
				    accModel = bmp.getTagAccountModel();

				long pid = accModel.getAccountOwnerID(account.getID());

				// retrieve or create new one
				AccountSynopsis acsyn = getAccountSynopsis(pid, System.currentTimeMillis());

				// remove the account
				acsyn.deleteAccount(account.getID());

				logger.create().block(modelName + ".monitor.run").info().level(1)
				    .msg("Removed: " + account.getName() + " account for " + pid).send();

			    } catch (Exception e) {
				e.printStackTrace();
			    }

			}
		    
		    }
		    try {
			Thread.sleep(UPDATE_LIST_POLLING_INTERVAL);
		    } catch (InterruptedException ix) {
		    }
		
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
	    } // while
	    
	    
	} // run
    }
}
