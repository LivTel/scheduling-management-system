package ngat.sms.test;

import ngat.sms.*;
import ngat.sms.bds.MysqlHistoryModel;
import ngat.oss.model.*;
import ngat.phase2.*;
import ngat.util.*;
import java.util.*;
import java.rmi.*;

public class TestListHistory {

    public static void main(String args[]) {

        try {

            ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
	    
	    String host = cfg.getProperty("host", "localhost");

	    IHistoryModel hist = null;

            if (cfg.getProperty("local") != null)
                hist = new MysqlHistoryModel("jdbc:mysql://"+host+"/phase2odb?user=oss&password=ng@toss");
            else
                hist = (IHistoryModel)Naming.lookup("rmi://"+host+"/HistoryModel");

            long gid = cfg.getIntValue("gid");

            List hlist = hist.listHistoryItems(gid);

	    Iterator ih = hlist.iterator();
	    while (ih.hasNext()) {
		IHistoryItem h = (IHistoryItem)ih.next();
		System.err.println("Group: "+gid+" HistItem="+h);
	    }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
