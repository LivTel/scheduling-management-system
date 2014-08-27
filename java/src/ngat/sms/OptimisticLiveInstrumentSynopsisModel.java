/**
 * 
 */
package ngat.sms;

import java.rmi.Naming;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatus;
import ngat.sms.bds.TestInstrumentSynopsis;
import ngat.util.logging.BogstanLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class OptimisticLiveInstrumentSynopsisModel implements InstrumentSynopsisModel {

	private Map<String, InstrumentSynopsis> instruments;

	private LogGenerator logger;

	/**
	 * 
	 */
	public OptimisticLiveInstrumentSynopsisModel() {
		instruments = new HashMap<String, InstrumentSynopsis>();	
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate()
			.system("SMS")
			.subSystem("FCL")
			.srcCompClass(this.getClass().getSimpleName())
			.srcCompId("oism");

		logger.create().info().level(1).msg("Creating OISM").send();
	}
	/**
	 * Load the details and stuff from a remote registry.
	 * @param ireg The remote registry.
	 * @throws Exception If anything goes awry.
	 */
	public void loadFromRegistry(InstrumentRegistry ireg) throws Exception {
		logger.create().info().level(1)
			.extractCallInfo().msg("Loading instrument data from remote registry")
			.send();
		
		List<InstrumentDescriptor> instList= ireg.listInstruments();
		Iterator<InstrumentDescriptor> itInst = instList.iterator();
		while (itInst.hasNext()) {
			InstrumentDescriptor id = (InstrumentDescriptor)itInst.next();
			InstrumentCapabilitiesProvider iprov = ireg.getCapabilitiesProvider(id);		
			InstrumentCapabilities icap = iprov.getCapabilities();
			
			TestInstrumentSynopsis tit = new TestInstrumentSynopsis(id.getInstrumentName(),icap);
			instruments.put(id.getInstrumentName(), tit);
			logger.create().info().level(1).extractCallInfo()
				.msg("Added instrument provider for: Instrument:"+id+", using name:"+id.getInstrumentName())
				.send();
			// now make up an Optimistic status report...
			InstrumentStatus status = new InstrumentStatus();
			status.setEnabled(true);
			status.setFunctional(true);
			status.setOnline(true);
			tit.updateStatus(status);			
			
		}
		
	}

	/* (non-Javadoc)
	 * @see ngat.sms.InstrumentSynopsisModel#getInstrumentSynopsis(java.lang.String)
	 */
	public InstrumentSynopsis getInstrumentSynopsis(String instID) throws Exception {
		if (instruments.containsKey(instID))
			return instruments.get(instID);
		throw new Exception("getInstrumentTester(): Unknown instrument: "+instID);
	}

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
			
			InstrumentRegistry ireg = (InstrumentRegistry)Naming.lookup("InstrumentRegistry");			
			OptimisticLiveInstrumentSynopsisModel oism = new OptimisticLiveInstrumentSynopsisModel();			
			oism.loadFromRegistry(ireg);
			
			System.err.println("Registry loaded...");
			
			while(true) {try {Thread.sleep(60000);} catch (InterruptedException ix) {}}	
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public List<InstrumentDescriptor> listAcquisitionInstruments() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
