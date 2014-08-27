/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;
import java.util.Iterator;
import java.util.List;

import ngat.oss.model.IPhase2Model;
import ngat.phase2.IGroup;
import ngat.phase2.IProgram;
import ngat.phase2.IProposal;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;

/**
 * @author eng
 *
 */
public class ODBStatistics {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String p2host = args[0];
			
			IPhase2Model phase2 = (IPhase2Model)Naming.lookup("rmi://"+p2host+"/Phase2Model");
			
			int ig = 0;
			List lprog = phase2.listProgrammes();
			Iterator iprog = lprog.iterator(); 
			while (iprog.hasNext()) {
				IProgram prog = (IProgram)iprog.next();
				List lprop = phase2.listProposalsOfProgramme(prog.getID());
				Iterator iprop = lprop.iterator();
				while (iprop.hasNext()) {
					IProposal prop = (IProposal)iprop.next();
					int igp = 0;
					List lgrp = phase2.listGroups(prop.getID(), true);
					Iterator igrp = lgrp.iterator();
					while (igrp.hasNext()) {
						IGroup g = (IGroup)igrp.next();
						ITimingConstraint timing = g.getTimingConstraint();
						if (timing == null)
							continue;
						long tdiff = timing.getEndTime()-timing.getStartTime();
						long period = 0L;
						if (timing instanceof XMonitorTimingConstraint) {
							period = ((XMonitorTimingConstraint)timing).getPeriod();
						} else if
						(timing instanceof XMinimumIntervalTimingConstraint) {
							period = ((XMinimumIntervalTimingConstraint)timing).getMinimumInterval();
						}
						ig++;
						igp++;
						System.err.println("Group: "+ig+" "+g.getName()+", tdiff: "+(tdiff/3600000)+"h, period: "+(period/3600000)+"h");
					}
					System.err.println("Proposal "+prop.getName()+" contains: "+igp+" groups");
				}
				
	
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
