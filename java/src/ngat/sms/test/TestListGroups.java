/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;
import java.util.*;

import ngat.sms.GroupItem;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.Phase2GroupModelProvider;

/**
 * @author eng
 *
 */
public class TestListGroups {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			Phase2GroupModelProvider pmg = (Phase2GroupModelProvider)Naming.
			lookup("rmi://localhost/Phase2GroupModelProvider");
			
			Phase2CompositeModel p2g = pmg.getPhase2Model();
			
			int ng = 0;
			List<GroupItem> lgroups = p2g.listGroups();
			Iterator<GroupItem> groups = lgroups.iterator();
			while (groups.hasNext()) {
				GroupItem group = groups.next();
				System.err.println(""+(++ng)+" Group: "+group);
			}
			System.err.println("There were "+ng+" groups");
			
		} catch (Exception e) {
			e.printStackTrace(); 
		}
		
	}

}
