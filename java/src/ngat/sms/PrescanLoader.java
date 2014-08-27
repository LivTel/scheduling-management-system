package ngat.sms;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

import ngat.phase2.IProposal;
import ngat.phase2.ISequenceComponent;
import ngat.sms.util.*;
import ngat.util.logging.*;

public class PrescanLoader implements Phase2Loader {

    /** Base model provider to use.*/
    private FeasibilityPrescan prescan;

    private LogGenerator logger;

    /**
     * @param bmp
     */
    public PrescanLoader(FeasibilityPrescan prescan) {
        super();
        this.prescan = prescan;
        Logger alogger = LogManager.getLogger("SMS");
        logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
            .srcCompId("fpl");
    }

    /** Load Phase2 data into specified cache.
     * @see ngat.sms.Phase2Loader#loadPhase2Data(ngat.sms.BasicPhase2Cache)
     */
    public void loadPhase2Data(BasicPhase2Cache cache) throws Exception {
    	
    	long now = System.currentTimeMillis();
    	
    	List<PrescanEntry> candidates;
    	
    	candidates = prescan.prescan(System.currentTimeMillis(), 60*1000L);
    	
    	int ngrp = 0;
    	int nprp = 0;
    	Iterator<PrescanEntry> ic = candidates.iterator();
    	while (ic.hasNext()) {
    		PrescanEntry pse = ic.next();
    		GroupItem group = pse.group;
    		
    		// add the group to the cache
    		cache.addGroup(group);
    		ngrp++;
    		logger.create().block("loadPhase2").info().level(1).msg(
					"Load F group: [" + group.getID() + "] " + group.getName()).send();

    		// weve added the group but what if we dont have its proposal  which we wont have !
    		IProposal proposal = group.getProposal();
    		logger.create().block("loadPhase2").info().level(1).msg("Group proposal is: "+proposal).send();
    	
    		if (proposal != null && (cache.findProposal(proposal.getID()) == null)) {
    			cache.addProposal(proposal);
    			logger.create().block("loadPhase2").info().level(1).msg("Proposal has been cached").send();
    			nprp++;
    		}
    		
    		// load the group's targets if any 
    		ISequenceComponent sequence = group.getSequence();
    		if (sequence != null)
    			cache.linkTargets(sequence);
    		
    	}	
    	long end = System.currentTimeMillis();
    	System.err.println("Loaded: " + nprp + " Proposals, "+ngrp + " Groups in " + (end - now) + "ms");
    	
    }

	public void loadProposal(long pid, BasicPhase2Cache cache) throws Exception {
		// TODO Auto-generated method stub
		
	}

}