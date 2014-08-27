/**
 * 
 */
package ngat.sms.bds;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.oss.model.IAccessModel;
import ngat.oss.model.IPhase2Model;
import ngat.oss.monitor.*;
import ngat.sms.Phase2CompositeModel;
import ngat.sms.Phase2GroupModelProvider;
import ngat.sms.models.standard.BasicPhase2CompositeModel;
import ngat.util.CommandTokenizer;
import ngat.util.ConfigurationProperties;

/**
 * @author eng
 * 
 */
public class TestPhase2ModelProvider extends UnicastRemoteObject implements Phase2GroupModelProvider {

	private IPhase2Model phase2;

	private Phase2CompositeModel gphase2;

	/**
	 * @param phase2
	 */
	public TestPhase2ModelProvider(Phase2CompositeModel gphase2) throws RemoteException {
		super();
		this.gphase2 = gphase2;
		System.err.println("TestPhase2ModelProvider.init(): Using: " + gphase2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.Phase2GroupModelProvider#getPhase2Model()
	 */
	public Phase2CompositeModel getPhase2Model() throws RemoteException {
		return gphase2;
	}

	public static void main(String args[]) {
		String host = null;
		int port = 1099;
		try {

			ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

			host = cfg.getProperty("rhost", "localhost");
			port = cfg.getIntValue("port", 1099);

		} catch (Exception e) {
			// if we fail here there's no way were going to bind
			e.printStackTrace();
			System.exit(1);
		}

		boolean bound = false;

		while (! bound) {

			try {
				System.err.println("GP2::Locating base models...");
				IPhase2Model phase2 = (IPhase2Model)Naming.lookup("rmi://" + host + ":" + port + "/Phase2Model");
				IAccessModel access = (IAccessModel)Naming.lookup("rmi://" + host + ":" + port + "/AccessModel");
								
				System.err.println("GP2::Creating synoptic model over base models...");
				BasicPhase2CompositeModel lgphase2 = new BasicPhase2CompositeModel();
				
				// lets get some updates when phase2 data changes.
				System.err.println("GP2::Registering for phase2 updates...");
				Phase2Monitor phase2Monitor = (Phase2Monitor) phase2;
				phase2Monitor.addPhase2UpdateListener(lgphase2);
				
				System.err.println("GP2::Loading phase2 synopsis...");
				try {Thread.sleep(5000L);} catch (InterruptedException ix) {}
				lgphase2.loadPhase2(access, phase2);
				System.err.println("GP2::Loaded all phase2 data from base model");
				
				System.err.println("GP2::Start update monitoring...");
				lgphase2.startUpdateMonitor(access, phase2);
			
				System.err.println("GP2::Creating a model provider using: " + lgphase2);
				TestPhase2ModelProvider test = new TestPhase2ModelProvider(lgphase2);

				Naming.rebind("rmi://localhost:" + port + "/Phase2GroupModelProvider", test);
				System.err.println("GP2::Bound P2CModel Provider: " + test);
								
				Naming.rebind("rmi://localhost:" + port + "/Phase2CompositeModel", lgphase2);
				System.err.println("GP2::Bound P2C Model: " + lgphase2);
				
				
				bound = true;

			} catch (Exception e) {
				e.printStackTrace();
				// failed to lock on
			}
			// if we didnt bind, try again in a minute		
				try {
					Thread.sleep(60000L);
				} catch (InterruptedException ic) {
				}

		}

		// keep the miserable thread going...
		while (true) {
			try {
				Thread.sleep(60000L);
			} catch (InterruptedException ic) {
			}
		}

	}

}
