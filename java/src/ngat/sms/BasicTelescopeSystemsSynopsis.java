/**
 * 
 */
package ngat.sms;

import java.io.IOException;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.rmi.Naming;

import ngat.net.*;
import ngat.astrometry.ISite;
import ngat.autoguider.command.StatusTemperatureGetCommand;
import ngat.message.GUI_RCS.GET_STATUS;
import ngat.message.RCS_TCS.TCS_Status;
import ngat.message.base.COMMAND_DONE;
import ngat.net.SocketConnection;
import ngat.net.camp.CAMPResponseHandler;
import ngat.phase2.IBeamSteeringConfig;
import ngat.phase2.XOpticalSlideConfig;
import ngat.tcm.Telescope;
import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusUpdateListener;
import ngat.tcm.SciencePayload;
import ngat.util.*;

/**
 * @author eng
 * 
 */
public class BasicTelescopeSystemsSynopsis implements TelescopeSystemsSynopsis, TelescopeStatusUpdateListener,
		PropertiesConfigurable {


    public static final long LOADING_RETRY_DELAY = 30*1000L;

	/** Site details. */
	private ISite site;

	/** Dome low limit (rads). */
	private double domeLimit;

	/** ZAZ size (rads). */
	private double zazSize;

	/** High limit for AG temperature. */
	private double agTempMaxLimit;

	/** max slew rate (rad/sec). */
	private double maxSlewRate;

	/** Azimuth wrap limits. */
	private double azLimits;

	/** Altitude wrap limits. */
	private double altLimits;

	/** Rotator wrap limits. */
	private double rotLimits;

	/** Autoguider status. */
	private boolean autoguiderStatus;

	/** Autoguider temperature status. */
	private boolean autoguiderTempStatus;

	/** Rotator base offset (rads).*/
	private double rotatorBaseOffset;
	
	/** Store temperature data. */
	private List<Boolean> agTempData;

	private long autoguiderAcquireTime;

	private long axisSettleTime;

	private List lowerSlideElements;

	public BasicTelescopeSystemsSynopsis() {
		agTempData = new Vector<Boolean>();
		lowerSlideElements = new Vector();
	}

	/**
	 * @param site
	 *            the site to set
	 */
	public void setSite(ISite site) {
		this.site = site;
	}

	/**
	 * @param domeLimit
	 *            the domeLimit to set
	 */
	public void setDomeLimit(double domeLimit) {
		this.domeLimit = domeLimit;
	}

	/**
	 * @param zazSize
	 *            the zazSize to set
	 */
	public void setZazSize(double zazSize) {
		this.zazSize = zazSize;
	}

	/**
	 * @param maxSlewRate
	 *            the maxSlewRate to set
	 */
	public void setMaxSlewRate(double maxSlewRate) {
		this.maxSlewRate = maxSlewRate;
	}

	/**
	 * @param azLimits
	 *            the azLimits to set
	 */
	public void setAzLimits(double azLimits) {
		this.azLimits = azLimits;
	}

	/**
	 * @param altLimits
	 *            the altLimits to set
	 */
	public void setAltLimits(double altLimits) {
		this.altLimits = altLimits;
	}

	/**
	 * @param rotLimits
	 *            the rotLimits to set
	 */
	public void setRotLimits(double rotLimits) {
		this.rotLimits = rotLimits;
	}

	/**
	 * @param autoguiderStatus
	 *            the autoguiderStatus to set
	 */
	public void setAutoguiderStatus(boolean autoguiderStatus) {
		this.autoguiderStatus = autoguiderStatus;
	}

	public void setAutoguiderTempStatus(boolean autoguiderTempStatus) {
		this.autoguiderTempStatus = autoguiderTempStatus;
	}

	/**
	 * @return the rotatorBaseOffset
	 */
	public double getRotatorBaseOffset() {
		return rotatorBaseOffset;
	}

	/**
	 * @param rotatorBaseOffset the rotatorBaseOffset to set
	 */
	public void setRotatorBaseOffset(double rotatorBaseOffset) {
		this.rotatorBaseOffset = rotatorBaseOffset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getAltitudeWrapLimits()
	 */
	public double getAltitudeWrapLimits() {
		// TODO Auto-generated method stub
		return altLimits;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getAutoguiderStatus()
	 */
	// public boolean getAutoguiderStatus() {

	/*
	 * // count the number of ag temp items which are set true int nok = 0; for
	 * (int i = 0; i < agTempData.size(); i++) { boolean b = agTempData.get(i);
	 * if (b) nok++; }
	 * 
	 * // only allow if both the ag status AND the ag temp status are ok boolean
	 * agtok = (nok > 3); boolean overall = (autoguiderStatus && agtok);
	 * System.err
	 * .printf("BTS::Ag sw state: %b, Ag temp: %2d/10 %b, Overall %b\n",
	 * autoguiderStatus, nok, agtok, overall); return overall;
	 */

	// return autoguiderStatus && autoguiderTempStatus;

	// }

	public boolean isAutoguiderOperational() {
		return autoguiderStatus;
	}

	public boolean isAutoguiderFunctional() {
		return autoguiderTempStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getAzimuthWrapLimits()
	 */
	public double getAzimuthWrapLimits() {
		// TODO Auto-generated method stub
		return azLimits;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getDomeLimit()
	 */
	public double getDomeLimit() {
		// TODO Auto-generated method stub
		return domeLimit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getMaximumAxisSlewRate()
	 */
	public double getMaximumAxisSlewRate() {
		// TODO Auto-generated method stub
		return maxSlewRate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getRotatorWrapLimits()
	 */
	public double getRotatorWrapLimits() {
		// TODO Auto-generated method stub
		return rotLimits;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getSiteDetails()
	 */
	public ISite getSiteDetails() {
		// TODO Auto-generated method stub
		return site;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.sms.TelescopeSystemsSynopsis#getZenithAvoidanceZoneSize()
	 */
	public double getZenithAvoidanceZoneSize() {
		// TODO Auto-generated method stub
		return zazSize;
	}

	/**
	 * @return the autoguiderAcquireTime
	 */
	public long getAutoguiderAcquireTime() {
		return autoguiderAcquireTime;
	}

	/**
	 * @param autoguiderAcquireTime
	 *            the autoguiderAcquireTime to set
	 */
	public void setAutoguiderAcquireTime(long autoguiderAcquireTime) {
		this.autoguiderAcquireTime = autoguiderAcquireTime;
	}

	/**
	 * @return the axisSettleTime
	 */
	public long getAxisSettleTime() {
		return axisSettleTime;
	}

	/**
	 * @param axisSettleTime
	 *            the axisSettleTime to set
	 */
	public void setAxisSettleTime(long axisSettleTime) {
		this.axisSettleTime = axisSettleTime;
	}

   

    /**
     * Load the details and stuff from a remote registry.
     * 
     * @throws Exception
     *             If anything goes awry.
     */
    public void loadFromTelescope(String telescopeUrl) {
	
	Telescope telescope = null;

	// this loop is for when we loose our own binding and have to restart the whole process...
	//while (true) {

	    boolean loaded = false;
	    while (!loaded) {

		// keep trying to locate the Telescope, then load data and keep
		// re-registering for updates.

		//logger.create().info().level(1).extractCallInfo().msg(
		//					      "Looking for telescope using: " + telescopeUrl).send();
		try {
		    System.err.println("Search for: "+telescopeUrl);
		    telescope = (Telescope) Naming.lookup(telescopeUrl);
		    // iregFound = true;
		    //  logger.create().info().level(1).extractCallInfo().msg(
		    //						  "Located telescope using: " + telescope).send();
		} catch (Exception e) {
		    e.printStackTrace();
		    // continue;
		}

		//logger.create().info().level(1).extractCallInfo().msg("Loading telescope data from remote telescope")
		//  .send();

		try {

		    // Rotator base offset                                           
		    SciencePayload payload = telescope.getTelescopeSystem().getSciencePayload();
		    
		    rotatorBaseOffset = payload.getRotatorBaseOffset();
		    //  logger.create().info().level(1).extractCallInfo().msg("Loaded rotator base offset: "+ Math.toDegrees(rotatorBaseOffset)).send();
		    System.err.println("TELSYN::Loaded rotator base offset: "+ Math.toDegrees(rotatorBaseOffset));

		    site = telescope.getSiteInfo();

		    // azLimits =                                                                                                                                                                                                                     
		    // scope.getTelescopeSystem().getAzimuth().getAxisCapabilities().getLowLimit();                                                                                                                                                   

		    // altLimits =                                                                                                                                                                                                                    
		    // scope.getTelescopeSystem().getAltitude().getAxisCapabilities().getLowLimit();                                                                                                                                                  
		    // rotLimits =                                                                                                                                                                                                                    
		    // scope.getTelescopeSystem().getRotator().getAxisCapabilities().getLowLimit();                                                                                                                                                   
		    //                                                                                                                                                                                                                                
		    // domeLimit = scope.getTelescopeCapabilities();                                                                                                                                                                                  
		    // zazSize = scope.getZazSize();                               
		    
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
	    /*boolean registered = true;
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
	    */

	    //}

    }

    /**
     * Request the provider to load from registry asynchronously. This method
     * returns immediately.
     */
    public void asynchLoadFromTelescope(final String telescopeUrl) {
	Runnable r = new Runnable() {
		public void run() {
		    loadFromTelescope(telescopeUrl);
		}
	    };
	(new Thread(r)).start();
    }






	public void configure(ConfigurationProperties config) throws Exception {
		domeLimit = Math.toRadians(config.getDoubleValue("dome.limit", 25.0));
		zazSize = Math.toRadians(config.getDoubleValue("zaz.limit", 2.0));
		agTempMaxLimit = config.getDoubleValue("ag.temp.hi.limit", 0.0);

		// TODO replace ag temp limit with these values so we can signal an InstrumentStatus type reply
		
		//agTempHiWarnLimit  = config.getDoubleValue("ag.temp.hi.warn.limit", 0.0);
		//agTempHiFailLimit  = config.getDoubleValue("ag.temp.hi.fail.limit", 0.0);
		//agTempLowWarnLimit = config.getDoubleValue("ag.temp.lo.warn.limit", 0.0);
		//agTempLowFailLimit = config.getDoubleValue("ag.temp.lo.fail.limit", 0.0);
		
		// load the beam elements

		String strbeam = config.getProperty("beam.elements");
		if (strbeam == null || strbeam.equals(""))
			throw new IllegalArgumentException("No beam config element list specified for telescope");
		
		StringTokenizer st = new StringTokenizer(strbeam, ",");
		while (st.hasMoreTokens()) {
			String element = st.nextToken().trim();
			lowerSlideElements.add(element);
			System.err.println("BTS:config: Loading beam element: [" + element+"]");
		}

	}

	public void telescopeStatusUpdate(TelescopeStatus status) throws RemoteException {
		// TODO need to sort out TCM API properly now....
		// mostly interested in AG status and AgTemp status
	}

	public void telescopeNetworkFailure(long time, String arg0) throws RemoteException {
		// TODO Auto-generated method stub
		// cant rely on TCS sourced data if this is set
	}

	/** Start monitoring AG. */
	public void startMonitoring(String host, int port, String agHost, int agPort, long interval) {
		MonitorThread mtt = new MonitorThread(host, port, agHost, agPort, interval);
		mtt.start();
	}

	private void updateAgTemp(boolean value) {
		// agTempData.add(value);
		// trim to size
		// while (agTempData.size() > 10)
		// agTempData.remove(0);

		// getAutoguiderStatus();
		autoguiderTempStatus = value;
	}

	/**
	 * A thread to perform monitoring of status.
	 * 
	 * @author eng
	 * 
	 */
	private class MonitorThread extends Thread {

		/** Polling interval. */
		private long interval;

		/** RCS host. */
		private String host;

		/** AG host. */
		private String agHost;

		/** RCS command port. */
		private int port;

		/** AG command port. */
		private int agPort;

		CAMPResponseHandler handler;

		/** The RCS GET_STATUS command to extract Autoguider info from TCS. */
		GET_STATUS command;

		/** Get the AG temperature status. */
		StatusTemperatureGetCommand tempGetCommand;

		/** The autoguider direct command to get CCD temperature. */
		// StatusTemperatureGetCommand tempGetCommand;

		/**
		 * @param host
		 *            The RCS host.
		 * @param port
		 *            RCS command port.
		 * @param interval
		 *            Polling interval.
		 */
		public MonitorThread(String host, int port, String agHost, int agPort, long interval) {
			this.host = host;
			this.port = port;
			this.agHost = agHost;
			this.agPort = agPort;
			this.interval = interval;
			command = new GET_STATUS("bts", "AUTOGUIDER");		
			
			TCS_Status.mapCodes(); // needed to get code values back properly
		}

		/** Poll the RCS server with GET_STATUS requests. */
		public void run() {

			while (true) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException ix) {
				}

				// Try to get Temperature AG status directly from AG...
				try {
					tempGetCommand = new StatusTemperatureGetCommand(agHost, agPort);
					tempGetCommand.run();
					if (tempGetCommand.getRunException() != null) {
						System.err.println("BTS:AG_StatusCollator::StatusTemperatureGetCommand: Command failed.");
						tempGetCommand.getRunException().printStackTrace(System.err);
						updateAgTemp(false);
					} else {
						long time = System.currentTimeMillis();
						tempGetCommand.getCommandFinished();
						tempGetCommand.getParsedReplyOK();
						Date timeStamp = tempGetCommand.getTimestamp();
						double ccdTemp = tempGetCommand.getCCDTemperature();
						System.err
								.printf("BTS:AG_StatusCollator::StatusTemperatureGetCommand: %tF %tT %tZ, CCD Temperature: %4.2f from %tF %tT \n",
										time, time, time, ccdTemp, timeStamp, timeStamp);

						// check the temperature is below max value (-20C ?)
						if (ccdTemp < agTempMaxLimit) {
							updateAgTemp(true);
						} else {
							updateAgTemp(false);
						}
						
						// TODO Does the AG also have a minimum temperature if so
						// then we should change this field into a OKAY/WARN/FAIL type
						// Instrument.status value.
						
						
					}

				} catch (Exception ax) {
				
					System.err
							.println("BTS:AG_StatusCollator::StatusTemperatureGetCommand: Error getting ccd temperature: "
									+ ax);
					updateAgTemp(false);
				}

				// Try to get the AUTOGUIDER online status from RCS
				SocketConnection connection = new SocketConnection(host, port);

				try {
					connection.open();
				} catch (Exception cx) {
					System.err.println("BTS:AG_StatusCollator::RCSGetStatus: Error opening connection:" + cx);
					// TODO connection failure ? offline
					continue;
				}

				try {
					connection.send(command);
				} catch (Exception iox) {
					System.err.println("BTS:AG_StatusCollator::RCSGetStatus: Error sending request:" + iox);
					// TODO connection failure ? offline
					continue;
				}

				try {
					Object obj = connection.receive(20000L);
					System.err.println("BTS:AG_StatusCollator::RCSGetStatus:Object recvd: " + obj);
					COMMAND_DONE update = (COMMAND_DONE) obj;

					if (update instanceof ngat.message.GUI_RCS.GET_STATUS_DONE) {

						StatusCategory status = ((ngat.message.GUI_RCS.GET_STATUS_DONE) update).getStatus();
						TCS_Status.Autoguider ag = (TCS_Status.Autoguider) status;

						int agstatus = ag.agSwState;

						System.err.println("BTS:AG_StatusCollator::RCSGetStatus: AG SW State: " + agstatus);

						if (agstatus == TCS_Status.STATE_OKAY)
							autoguiderStatus = true;
						else
							autoguiderStatus = false;

						// TODO give this name and method a more logical name
						// ie. isAutoguiderOperational() or the likes

					}

				} catch (Exception iox) {
					System.err.println("BTS:AG_StatusCollator::RCSGetStatus: Error reading reply:" + iox);
					continue;
				} finally {
					if (connection != null) {
						System.err.println("BTS:AG_StatusCollator::RCSGetStatus:Closing connection: "+connection);
						connection.close();
					}
				}

			}
		}
	}

	/**
	 * @return true if the supplied config is valid.
	 */
	public boolean isValidBeamSteeringConfig(IBeamSteeringConfig beam) {
		if (beam == null)
			return false;

		XOpticalSlideConfig upper = beam.getUpperSlideConfig();
		XOpticalSlideConfig lower = beam.getLowerSlideConfig();

		String lowerElement = lower.getElementName();
		System.err.println("BTS:checkBeamCfg:checking lower element called: ["+lowerElement+"] against values stored");
		if (lowerSlideElements.contains(lowerElement))
			return true;
		return false;
	}

}
