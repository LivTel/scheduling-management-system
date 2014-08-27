/**
 * 
 */
package ngat.sms.test;

import java.rmi.Naming;

import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.phase2.XDetectorConfig;
import ngat.phase2.XFilterDef;
import ngat.phase2.XFilterSpec;
import ngat.phase2.XImagerInstrumentConfig;

/**
 * @author eng
 *
 */
public class TestCheckRatClear {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			XImagerInstrumentConfig rat = new XImagerInstrumentConfig("c-c-2x2");
			XFilterSpec spec = new XFilterSpec();
			spec.setFilterList(null);
			rat.setFilterSpec(spec);
			XDetectorConfig dc = new XDetectorConfig();
			dc.setXBin(2);
			dc.setYBin(2);
			rat.setDetectorConfig(dc);
			rat.setInstrumentName("RATCAM");
			
			InstrumentRegistry ireg = (InstrumentRegistry)Naming.lookup("InstrumentRegistry");
			System.err.println("Located IREG: "+ireg);
			
			InstrumentDescriptor rid = new InstrumentDescriptor("RATCAM");
			InstrumentCapabilitiesProvider rcp = ireg.getCapabilitiesProvider(rid);
			System.err.println("Located cap provider for RATCAM: "+rcp);
			
			InstrumentCapabilities rcap = rcp.getCapabilities();
			System.err.println("Located caps for RATCAM: "+rcap);
			
			System.err.println("Testing validity of: "+rat);
			boolean valid = rcap.isValidConfiguration(rat);
			System.err.println("Config "+(valid ? "IS VALID" : "IS NOT VALID"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
