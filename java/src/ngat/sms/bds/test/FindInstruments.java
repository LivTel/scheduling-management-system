/**
 * 
 */
package ngat.sms.bds.test;

import java.rmi.Naming;

import ngat.icm.InstrumentRegistry;
import ngat.sms.BasicInstrumentSynopsisModel;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.DatagramLogHandler;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/** Find all the available instruments from the registry.
 * @author eng
 *
 */
public class FindInstruments {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Logger alogger = LogManager.getLogger("SMS");
			alogger.setLogLevel(5);
			ConsoleLogHandler console = new ConsoleLogHandler(new BogstanLogFormatter());
			console.setLogLevel(5);
			alogger.addExtendedHandler(console);
		
			
			BasicInstrumentSynopsisModel bit = new BasicInstrumentSynopsisModel("rmi://ltsim1/InstrumentRegistry");			
			bit.asynchLoadFromRegistry();
			
			System.err.println("Registry loaded...");
					
			while(true) {try {Thread.sleep(60000);} catch (InterruptedException ix) {}}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

