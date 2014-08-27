/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;
import java.util.HashSet;

import ngat.phase2.IQosMetric;
import ngat.phase2.XGroup;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.phase2.XProgram;
import ngat.phase2.XProposal;
import ngat.phase2.XTag;
import ngat.sms.DefaultExecutionUpdater;
import ngat.sms.ExecutionResourceBundle;
import ngat.sms.GroupItem;
import ngat.sms.SynopticModelProvider;
import ngat.sms.models.standard.StandardChargeAccountingModel;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;

/**
 * @author eng
 *
 */
public class UpdateGroupExecHistory {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
			
			long gid = cfg.getLongValue("gid"); // group
			long pid = cfg.getLongValue("pid"); // prop
			long tid = cfg.getLongValue("tid"); // tag
			
			SynopticModelProvider smp = (SynopticModelProvider)Naming.lookup("SynopticModelProvider");
			DefaultExecutionUpdater dxu = new DefaultExecutionUpdater(smp);
			dxu.setChargeModel(new StandardChargeAccountingModel());
			
			ExecutionResourceBundle erb = new ExecutionResourceBundle();
			long now = System.currentTimeMillis();
			XGroup g = new XGroup();
			g.setName("Test");		
			g.setID(gid);
			GroupItem group = new GroupItem(g, new XIteratorComponent("root", new XIteratorRepeatCountCondition(1)));
			group.setProgram(new XProgram("PROG"));
			XProposal proposal = new XProposal("JL010101");
			proposal.setID(pid);
			group.setProposal(proposal);
			XTag tag = new XTag();
			tag.setName("TACGAGACCATA");
			tag.setID(tid);
			group.setTag(tag);
			
			dxu.groupExecutionCompleted(group, now, erb, new HashSet<IQosMetric>());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
