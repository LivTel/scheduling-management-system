package ngat.sms.genetic.test;

import ngat.sms.*;
import ngat.icm.BasicInstrumentCapabilities;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.Wavelength;
import ngat.phase2.*;

public class OptimisticInstrumentSynopsis implements InstrumentSynopsis {
 
	private InstrumentCapabilities icap;
	
    /**
	 * 
	 */
	public OptimisticInstrumentSynopsis(InstrumentCapabilities icap) {
		super();	
		this.icap = icap;
	}

	/** 
     * @return true if the associated instrument is ONLINE.
     */
    public boolean isOnline() {return true;}
    
    /**
     * @return true if the associated instrument is OPERATIONAL.
     */
    public boolean isOperational() {return true;}
    
    /**
     * @param config The config to test for validity
     * @return true if the specified config is valid for the associated instrument.
     */
    public boolean isValidConfig(IInstrumentConfig config) {return true;}

	public InstrumentCapabilities getInstrumentCapabilities() {
		return icap;
	}

	public boolean isEnabled() {return true;}

    
}