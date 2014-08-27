package ngat.sms.genetic.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ngat.icm.InstrumentDescriptor;
import ngat.sms.*;
import ngat.sms.bds.TestInstrumentSynopsis;

public class OptimisticInstrumentSynopsisModel implements InstrumentSynopsisModel {

	/** The base model.*/
	private InstrumentSynopsisModel ism;
    
	private Map<String, InstrumentSynopsis> synopses;

    public OptimisticInstrumentSynopsisModel(InstrumentSynopsisModel ism) {
    	this.ism = ism;
    	 synopses = new HashMap<String, InstrumentSynopsis>();	
    }

   
    /** Return the tester for the named instrument if available.*/
    public InstrumentSynopsis getInstrumentSynopsis(String instID) throws Exception {
    	if (synopses.containsKey(instID)) {
    		return synopses.get(instID);
    	}
    	
    	// lookup in base model and store.
    	InstrumentSynopsis isyn = ism.getInstrumentSynopsis(instID);
    	OptimisticInstrumentSynopsis opt = new OptimisticInstrumentSynopsis(isyn.getInstrumentCapabilities());
    	synopses.put(instID, opt);
    	return opt;
    }


	public List<InstrumentDescriptor> listAcquisitionInstruments() throws Exception {
		return ism.listAcquisitionInstruments();
	}

}
