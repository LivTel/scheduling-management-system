package ngat.sms.test;

import ngat.sms.*;
import ngat.sms.bds.MysqlHistoryModel;
import ngat.oss.model.*;
import ngat.phase2.*;
import ngat.util.*;
import java.util.*;
import java.rmi.*;

public class TestUpdateHistory {

    public static void main(String args[]) {

        try {

            ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

	    String host = cfg.getProperty("host", "localhost");

	    IHistoryModel hist = null;

            if (cfg.getProperty("local") != null)
                hist = new MysqlHistoryModel("jdbc:mysql://"+host+"/phase2odb?user=oss&password=ng@toss");
            else
                hist = (IHistoryModel)Naming.lookup("rmi://"+host+"/HistoryModel");

            long hid = cfg.getIntValue("hid");

	    int cstat = cfg.getIntValue("status", 1); // 1=success, 2=partial, 3=fail

	    int code = 0;
	    String msg = "";
	    if (cstat == 3) {
		code = cfg.getIntValue("code", 666);
		msg = cfg.getProperty("err", "An error occurred");
	    }

	    long ctime = System.currentTimeMillis();

	    IExecutionFailureContext efc = new XBasicExecutionFailureContext(code,msg);

	    hist.updateHistory(hid, cstat, ctime, efc, new HashSet<IQosMetric>());

	    System.err.println("Historu was updated successfully");
	    

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
