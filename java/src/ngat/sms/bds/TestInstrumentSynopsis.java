/**
 * 
 */
package ngat.sms.bds;

import java.rmi.RemoteException;

import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusUpdateListener;
import ngat.phase2.IInstrumentConfig;
import ngat.sms.InstrumentSynopsis;

/**
 * @author eng
 *
 */
public class TestInstrumentSynopsis implements InstrumentSynopsis {

    private boolean enabled;

	private boolean online;
	
	private boolean operational;
	
	private String instName;
	private InstrumentCapabilities instCap;
	
	
	/**
	 * @param instCap
	 */
	public TestInstrumentSynopsis(String instName, InstrumentCapabilities instCap) {
		this.instName = instName;
		this.instCap = instCap;
	}

    /* (non-Javadoc)
     * @see ngat.sms.InstrumentTester#isEnabled()
     */
    public boolean isEnabled() {
	return enabled;
    }


	/* (non-Javadoc)
	 * @see ngat.sms.InstrumentTester#isOnline()
	 */
	public boolean isOnline() {
		return online;
	}

	/* (non-Javadoc)
	 * @see ngat.sms.InstrumentTester#isOperational()
	 */
	public boolean isOperational() {
		return operational;
	}

	/* (non-Javadoc)
	 * @see ngat.sms.InstrumentTester#isValidConfig(ngat.phase2.IInstrumentConfig)
	 */
	public boolean isValidConfig(IInstrumentConfig config) {
		return instCap.isValidConfiguration(config);
	}
	
	/** Process a status update.*/
	public void updateStatus(InstrumentStatus status) {
	    enabled = status.isEnabled();
		online = status.isOnline();
		operational = status.isFunctional();		
	}

	public InstrumentCapabilities getInstrumentCapabilities() {
		return instCap;
	}
	
	
	
}
