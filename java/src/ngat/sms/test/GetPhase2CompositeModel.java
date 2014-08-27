/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;
import java.util.List;

import ngat.sms.AccountSynopsis;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.GroupItem;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.SynopticModelProvider;

/**
 * @author eng
 *
 */
public class GetPhase2CompositeModel {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		try {
		
			String remoteHost = args[0];				
			
			SynopticModelProvider smp  = (SynopticModelProvider)Naming.lookup("rmi://"+remoteHost+"/SynopticModelProvider");
			System.err.println("Found synoptic model provider");
			
			Phase2CompositeModel gp2 = smp.getPhase2CompositeModel();
			System.err.println("Grabbed phase2 composite model: "+gp2);
			
			List<GroupItem> glist = gp2.listGroups();
			System.err.println("Group list contains: "+glist.size()+" entries");
			
			// pick one from list
			int ig = (int)(Math.random()*glist.size());
			GroupItem g = glist.get(ig);
			long gid = g.getID();
			long pid = g.getProposal().getID();
			
			long now = System.currentTimeMillis();
			
		AccountSynopsisModel pasm = smp.getProposalAccountSynopsisModel();
		System.err.println("Grabbed Proposal account synopsis model: "+pasm);
		
		AccountSynopsis pac = pasm.getAccountSynopsis(pid, now);
		System.err.println("Proposal account: ["+pid+"] = "+pac);
		
		ExecutionHistorySynopsisModel hsm = smp.getHistorySynopsisModel();
		System.err.println("Grabbed history synopsis model: "+hsm);
		
		ExecutionHistorySynopsis hist = hsm.getExecutionHistorySynopsis(gid, now);
		System.err.println("Group exec history synopsis: "+hist);
		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
