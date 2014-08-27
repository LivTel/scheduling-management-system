/**
 * 
 */
package ngat.sms;

import ngat.icm.InstrumentCapabilities;
import ngat.phase2.IInstrumentConfig;

/**
 * @author eng
 *
 */
public interface InstrumentSynopsis {

    /**
     * @return true if the associated instrument is ENABLED.
     */
    public boolean isEnabled();


	/**
	 * @return true if the associated instrument is ONLINE.
	 */
	public boolean isOnline();
	
	/**
	 * @return true if the associated instrument is OPERATIONAL.
	 */
	public boolean isOperational();
	
	/**
	 * @param config The config to test for validity
	 * @return true if the specified config is valid for the associated instrument.
	 */
	public boolean isValidConfig(IInstrumentConfig config);
	
	/**
	 * @return the capabilities of the instrument in some detail.
	 */
	public InstrumentCapabilities getInstrumentCapabilities();
		
}
