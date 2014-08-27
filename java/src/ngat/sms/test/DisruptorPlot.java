/**
 * 
 */
package ngat.sms.test;

import javax.swing.JFrame;

import ngat.phase2.XTimePeriod;
import ngat.sms.Disruptor;

/** Display a disruptor plot.
 * @author eng
 *
 */
public class DisruptorPlot {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		long start = System.currentTimeMillis();
		long end = start + 10*86400*1000L;
		
		DisruptorPanel dp = new DisruptorPanel();
		dp.setTimeLimits(start, end);
		
		for (int i = 0; i< 50; i++) {
			// create a disruptor
			
			long ts = start + (long)(Math.random()*(double)(end-start));
			long te = ts + (long)(Math.random()*4*3600*1000.0);
			
			Disruptor d = new Disruptor("T"+i, "Generic", new XTimePeriod(ts, te));
			dp.addDisruptor(d);
			System.err.println("Add disruptor: "+d);
		}
		
		JFrame f = new JFrame("Disruptors");
		f.getContentPane().add(dp);
		f.pack();
		f.setBounds(50,50,400,400);
		f.setVisible(true);
				
	}

}
