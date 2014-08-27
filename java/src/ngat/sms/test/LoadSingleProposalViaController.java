/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;

import ngat.oss.model.IPhase2Model;
import ngat.phase2.IProposal;
import ngat.sms.AccountSynopsisModel;
import ngat.sms.BaseModelLoader;
import ngat.sms.BaseModelProvider;
import ngat.sms.BasicAccountSynopsisModel;
import ngat.sms.BasicHistorySynopsisModel;
import ngat.sms.BasicPhase2Cache;
import ngat.sms.DefaultBaseModelProvider;
import ngat.sms.DefaultPhase2LoadContoller;
import ngat.sms.DefaultSynopticModelProvider;
import ngat.sms.ExecutionHistorySynopsisModel;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.Phase2LoadController;
import ngat.sms.SynopticModelProvider;

/** Load a single proposal into cache via P2LoadController
 * @author eng
 *
 */
public class LoadSingleProposalViaController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			
		
			String dlchost = args[0];
			String p2host  = args[1];
					
			DefaultBaseModelProvider bmp = new DefaultBaseModelProvider(p2host);
			System.err.println("Built BaseModelProvider for: "+p2host);
			
			System.err.println("Looking for SynopticModelProvider on: "+dlchost+"...");
			SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup("rmi://"+dlchost+"/SynopticModelProvider");
			System.err.println("Found: "+smp);
					
			System.err.println("Looking for Phase2LoadController on: "+dlchost+"...");
			Phase2LoadController dlc = smp.getLoadController();
			System.err.println("Found: "+dlc);
			
			String pname = args[2];
			
			System.err.println("Looking for Phase2Model on: "+p2host+"...");
			IPhase2Model phase2 = (IPhase2Model)Naming.lookup("rmi://"+p2host+"/Phase2Model");
			System.err.println("Found: "+phase2);
			
			System.err.println("Looking up proposal: "+pname+"...");
			IProposal proposal = phase2.findProposal(pname);
			System.err.println("Found: "+proposal);
			
			long pid = proposal.getID();
			
			System.err.println("(Re)-loading proposal: "+pid+"...");
			dlc.loadProposal(pid);
			System.err.println("Done");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
