/**
 * 
 */
package ngat.sms.test;

import ngat.sms.BasicTelescopeSystemsSynopsis;

/**
 * @author eng
 *
 */
public class TestGetAgStatus {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			BasicTelescopeSystemsSynopsis bts = new BasicTelescopeSystemsSynopsis();
			bts.startMonitoring("ltsim1", 9110, "autoguider1", 6571, 10000L);
			
			while(true) {
				try {
					Thread.sleep(Integer.MAX_VALUE);
				} catch (InterruptedException ie) {}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
