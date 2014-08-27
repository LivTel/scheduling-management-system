/**
 * 
 */
package ngat.sms;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class DefaultPhase2LoadContoller extends UnicastRemoteObject implements Phase2LoadController {

	private static final long BINDING_RETRY_DELAY = 60000L;

	private BasicPhase2Cache cache;

	private BaseModelLoader loader;

	private BasicAccountSynopsisModel pasm;

	private BasicHistorySynopsisModel hsm;

	LogGenerator logger;

	public DefaultPhase2LoadContoller(BasicPhase2Cache cache, BaseModelLoader loader, BasicAccountSynopsisModel pasm,
			BasicHistorySynopsisModel hsm) throws RemoteException {
		super();
		this.cache = cache;
		this.loader = loader;
		this.pasm = pasm;
		this.hsm = hsm;

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate().system("SMS").subSystem("Synoptics").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("dlc");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.Phase2LoadController#loadProposal(long)
	 */
	public void loadProposal(long pid) throws RemoteException {
		try {
			loader.loadProposal(pid, cache);
			logger.create().block("loadProposal").info().level(1)
				.msg("Phase2 cache loaded successfully").send();
			
			AccountSynopsis acc = pasm.getAccountSynopsis(pid, System.currentTimeMillis());
			logger.create().block("loadProposal").info().level(1)
				.msg("Proposal accounts were loaded: "+acc).send();
			
			hsm.loadProposalHistory(cache, pid, System.currentTimeMillis());
			logger.create().block("loadProposal").info().level(1)
				.msg("Group histories loaded successfully").send();
		} catch (Exception e) {
			throw new RemoteException("Failed to load proposal: "+pid+" : "+e);
		}
	
	}

	/** Bind to local registry. */
	private void bind() {
		// Bind locally
		while (true) {
			try {
				// .msg("").send();
				logger.create().block("bind").info().level(1).msg("Attempting to bind...").send();
				Naming.rebind("rmi://localhost/Phase2LoadController", this);

				logger.create().block("bind").info().level(1).msg("Bound to local registry").send();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(BINDING_RETRY_DELAY);
			} catch (InterruptedException ix) {
			}
		}
	}

	/** Request the provider to bind, repeatedly. */
	public void asynchBind() {
		Runnable r = new Runnable() {
			public void run() {
				bind();
			}
		};
		(new Thread(r)).start();
	}

}
