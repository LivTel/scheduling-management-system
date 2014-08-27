/**
 * 
 */
package ngat.sms;

import java.util.List;

import ngat.icm.InstrumentDescriptor;

/**
 * @author eng
 *
 */
public interface InstrumentSynopsisModel {

	/** Return the tester for the named instrument if available.*/
	public InstrumentSynopsis getInstrumentSynopsis(String instID) throws Exception;
	
	public List<InstrumentDescriptor> listAcquisitionInstruments() throws Exception;; 
}
