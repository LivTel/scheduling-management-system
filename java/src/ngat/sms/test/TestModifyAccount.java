package ngat.sms.test;

import ngat.phase2.*;
import ngat.sms.*;
import ngat.oss.model.*;
import ngat.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class TestModifyAccount {

    public static void main(String args[]) {

	try {

	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
	    
	    String host = cfg.getProperty("host", "localhost");
	    long pid = (long)cfg.getIntValue("pid");
	    long sid = (long)cfg.getIntValue("sid");
	    	    
	    double amt = cfg.getDoubleValue("amt", 1.0);
	    IAccountModel accModel = (IAccountModel)Naming.lookup("rmi://"+host+"/ProposalAccountModel");
	    
	    IAccount account = accModel.findAccount(pid, sid);
	    if (account == null) {
		System.err.println("no such account");
		return;
	    } 

	    System.err.println("Found: "+account);
	    
	    String mode = cfg.getProperty("mode", "a");
	    if (mode.equalsIgnoreCase("a"))
		accModel.modifyAllocation(account.getID(), amt, "test update", "TestModifyAccount");
	    else
		accModel.modifyConsumed(account.getID(), amt, "test update", "TestModifyAccount");

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}