package ngat.sms.test;

import ngat.sms.*;
import ngat.util.*;
import ngat.phase2.*;

import java.util.*;
import java.rmi.*;

public class CheckSeq {

    public static void main(String args[]) {

	try {
	
	    String smphost = args[0];

	    int gid = Integer.parseInt(args[1]);

	    SynopticModelProvider smp = (SynopticModelProvider) Naming.lookup("rmi://" + smphost
									      + "/SynopticModelProvider");

	    // Check all groups from cache model.
	    Phase2CompositeModel p2g = smp.getPhase2CompositeModel();

	    List<GroupItem> glist = p2g.listGroups();
	    System.err.println("There are "+glist.size()+" groups");

	    // load the  account and history caches
	    Iterator<GroupItem> igl = glist.iterator();
	    while (igl.hasNext()) {

		GroupItem group = igl.next();

		if (group.getID() == gid) {
		    System.err.println("Found group: "+group.getName());
		    System.err.println("Sequence is: "+group.getSequence());
		}

	    }
    

	} catch (Exception e) {
	    e.printStackTrace();	
	}
    
    }



}