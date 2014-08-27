package ngat.sms.test;

import ngat.phase2.*;
import ngat.sms.*;
import ngat.oss.model.*;
import ngat.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class TestAddAccount {

    public static void main(String args[]) {

	try {

	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
	    
	    String host = cfg.getProperty("host", "localhost");
	    long pid = (long)cfg.getIntValue("pid");
	    long sid = (long)cfg.getIntValue("sid");
	    	    
	    IAccountModel accModel = (IAccountModel)Naming.lookup("rmi://"+host+"/ProposalAccountModel");
	    XAccount acc = new XAccount("alloc");
	    acc.setAllocated(34.0);
	    acc.setConsumed(1.1);
	    acc.setChargeable(true);
	    accModel.addAccount(pid, sid, acc);

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}