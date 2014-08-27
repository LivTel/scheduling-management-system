/**
 * 
 */
package ngat.sms;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusProvider;
import ngat.icm.InstrumentStatusUpdateListener;
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
public class BasicInstrumentSynopsisModel extends UnicastRemoteObject implements InstrumentSynopsisModel,
		InstrumentStatusUpdateListener {

	private static final long LOADING_RETRY_DELAY = 30000L;

	private static final long REGISTRATION_RETRY_DELAY = 180000L;

	private String iregUrl;

	private Map<String, InstrumentSynopsis> instruments;

	private List<InstrumentDescriptor> acquisitionInstruments;
	
	private LogGenerator logger;

	/**
	 * Create a new BasicInstrumentSynopsisModel.
	 */
	public BasicInstrumentSynopsisModel(String iregUrl) throws RemoteException {
		super();
		this.iregUrl = iregUrl;
		instruments = new HashMap<String, InstrumentSynopsis>();
		acquisitionInstruments = new Vector<InstrumentDescriptor>();
		
		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("FCL").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("bism");

		logger.create().info().level(1).msg("Creating BISM").send();

	}

	/**
	 * Load the details and stuff from a remote registry.
	 * 
	 * @param ireg
	 *            The remote registry.
	 * @throws Exception
	 *             If anything goes awry.
	 */
	public void loadFromRegistry() {

		InstrumentRegistry ireg = null;
		
		while (true) {

			boolean loaded = false;
			while (!loaded) {

				// keep trying to locate the IREG, then load data and keep
				// re-registering for updates.

				// boolean iregFound = false;
				// while (!iregFound) {
				logger.create().info().level(1).extractCallInfo().msg(
						"Looking for instrument registry using: " + iregUrl).send();
				try {
					System.err.println("Search for: "+iregUrl);
					ireg = (InstrumentRegistry) Naming.lookup(iregUrl);
					// iregFound = true;
					logger.create().info().level(1).extractCallInfo().msg(
							"Located intrument registry using: " + iregUrl).send();
				} catch (Exception e) {
					e.printStackTrace();
					// continue;
				}

				// keep trying to load instrument data
				// boolean loaded = false;
				// while (!loaded) {
				logger.create().info().level(1).extractCallInfo().msg("Loading instrument data from remote registry")
						.send();
				instruments.clear();
				try {
					List<InstrumentDescriptor> instList = ireg.listInstruments();
					Iterator<InstrumentDescriptor> itInst = instList.iterator();
					while (itInst.hasNext()) {
						InstrumentDescriptor id = (InstrumentDescriptor) itInst.next();
						InstrumentCapabilitiesProvider iprov = ireg.getCapabilitiesProvider(id);
						InstrumentCapabilities icap = iprov.getCapabilities();

						TestInstrumentSynopsis tit = new TestInstrumentSynopsis(id.getInstrumentName(), icap);
						instruments.put(id.getInstrumentName(), tit);
						logger.create().info().level(1).extractCallInfo().msg(
								"Added instrument synopsis for: Instrument:" + id + ", using name:"
										+ id.getInstrumentName()).send();
						
						List<InstrumentDescriptor> sublist = id.listSubcomponents();
						Iterator<InstrumentDescriptor> is = sublist.iterator();
						while (is.hasNext()) {
							InstrumentDescriptor sid = is.next();
							String subName = id.getInstrumentName()+"_"+sid.getInstrumentName();
							sid.setInstrumentName(subName);
							instruments.put(sid.getInstrumentName(), tit);
							logger.create().info().level(1).extractCallInfo().msg(
									"Added instrument synopsis for: Instrument:" + sid + ", using name:"
											+ sid.getInstrumentName()).send();
						}
						
					}
					
					logger.create().info().level(1).extractCallInfo().msg("Loading instrument acquisition list from remote registry")
					.send();
					
					List<InstrumentDescriptor> acqInstList = ireg.listAcquisitionInstruments();
					logger.create().info().level(1).extractCallInfo().msg("Acquisition list: "+acqInstList).send();
					//logger.create().info().level(1).extractCallInfo().msg("Acquisition list contains: "+acqInstList.size()+" entries")
					//.send();
					Iterator<InstrumentDescriptor> aInst = acqInstList.iterator();
					while (aInst.hasNext()) {
						InstrumentDescriptor id = (InstrumentDescriptor) aInst.next();
						acquisitionInstruments.add(id);
						logger.create().info().level(1).extractCallInfo().msg(
								"Added acquisition instrument: "+id).send(); 
					}
					
					loaded = true;
					continue;
				} catch (Exception e) {
					e.printStackTrace();				
				}

				try {
					Thread.sleep(LOADING_RETRY_DELAY);
				} catch (InterruptedException ix) {
				}

			} // retry load registry

			// now keep on registering for updates
			boolean registered = true;
			while (registered) {
				try {
					logger.create().info().level(1).extractCallInfo().msg("Registering for status updates...").send();
					List<InstrumentDescriptor> instList2 = ireg.listInstruments();
					Iterator<InstrumentDescriptor> itInst2 = instList2.iterator();
					while (itInst2.hasNext()) {
						InstrumentDescriptor id = (InstrumentDescriptor) itInst2.next();
						InstrumentStatusProvider isp = ireg.getStatusProvider(id);
						isp.addInstrumentStatusUpdateListener(this);
						logger.create().info().level(1).extractCallInfo().msg("Registered self as ISU for: " + id)
								.send();
					}
				} catch (Exception e) {
					e.printStackTrace();
					registered = false;
					continue;
					// fall out and restart the whole process
				}
				try {
					Thread.sleep(REGISTRATION_RETRY_DELAY);
				} catch (InterruptedException ix) {
				}
			} // retry register for updates

		}

	}

	/**
	 * Request the provider to load from registry asynchronously. This method
	 * returns immediately.
	 */
	public void asynchLoadFromRegistry() {
		Runnable r = new Runnable() {
			public void run() {
				loadFromRegistry();
			}
		};
		(new Thread(r)).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.icm.InstrumentStatusUpdateListener#instrumentStatusUpdated(ngat.
	 * icm.InstrumentDescriptor, ngat.icm.InstrumentStatus)
	 */
	public void instrumentStatusUpdated(InstrumentStatus status) throws RemoteException {

		String instId = status.getInstrument().getInstrumentName();
		logger.create().info().level(1).extractCallInfo()
		    .msg("Recieved status update for instrument: " + 
			 instId +" : "+status).send();
		
		if (instId != null) {
		    TestInstrumentSynopsis tester = (TestInstrumentSynopsis) instruments.get(instId);
		    tester.updateStatus(status);
		}
		
	}

	public InstrumentSynopsis getInstrumentSynopsis(String instID) throws Exception {
		if (instruments.containsKey(instID))
			return instruments.get(instID);
		throw new Exception("getInstrumentSynopsis(): Unknown instrument: " + instID);
	}

	public List<InstrumentDescriptor> listAcquisitionInstruments() throws Exception {
		return acquisitionInstruments;
	}

}
