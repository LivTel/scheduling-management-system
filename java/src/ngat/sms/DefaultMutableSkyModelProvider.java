/**
 * 
 */
package ngat.sms;

import ngat.ems.SkyModel;

/**
 * @author eng
 *
 */
public class DefaultMutableSkyModelProvider implements SkyModelProvider {

	/** A sky model.*/
	private SkyModel skyModel;
	
	
	
	/**
	 * @param skyModel
	 */
	public DefaultMutableSkyModelProvider(SkyModel skyModel) {	
		this.skyModel = skyModel;
	}

	/* (non-Javadoc)
	 * @see ngat.sms.SkyModelProvider#getSkyModel()
	 */
	public SkyModel getSkyModel() throws Exception {
		return skyModel;
	}

}
