/**
 * 
 */
package ngat.sms.test;

import java.util.Iterator;
import java.util.List;

import ngat.phase2.IExposureInfo;
import ngat.phase2.IHistoryItem;
import ngat.sms.bds.MysqlHistoryModel;

/**
 * @author eng
 *
 */
public class TestListExposures {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
		
			int gid = Integer.parseInt(args[0]);
			
			MysqlHistoryModel hm = new MysqlHistoryModel("jdbc:mysql://ltdev1/phase2odb?user=oss&password=ng@toss");
			
			List lhist = hm.listHistoryItems(gid);
			Iterator ih = lhist.iterator();
			while (ih.hasNext()) {
				IHistoryItem hist = (IHistoryItem)ih.next();
				System.err.println("HIST: "+hist);
				
				List lexp = hm.listExposureItems(hist.getID());
				Iterator ie = lexp.iterator();
				while (ie.hasNext()) {
					IExposureInfo info = (IExposureInfo)ie.next();
					System.err.println("  EXP: "+info);
				}
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}

}
