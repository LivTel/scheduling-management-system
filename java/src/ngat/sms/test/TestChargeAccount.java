package ngat.sms.test;

import ngat.sms.*;
import ngat.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class TestChargeAccount {

    public static void main(String args[]) {

	try {

	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
	    
	    String host = cfg.getProperty("host", "localhost");
	    long pid = (long)cfg.getIntValue("pid");
	    double amount = cfg.getDoubleValue("amt");
	    String comment = "test add";
	    String clientRef = "tester";

	    SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup("rmi://"+host+"/SynopticModelProvider");
	    AccountSynopsisModel basm = smp.getProposalAccountSynopsisModel();
	    
	    basm.chargeAccount(pid, amount, comment, clientRef); 

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}