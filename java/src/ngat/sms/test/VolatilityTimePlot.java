/**
 * 
 */
package ngat.sms.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;

/**
 * @author eng
 *
 */
public class VolatilityTimePlot {

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static SimpleTimeZone UTC=  new SimpleTimeZone(0, "UTC");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			BufferedReader bin = new BufferedReader(new FileReader(args[0]));
			
			sdf.setTimeZone(UTC);
			
			long start = (sdf.parse(args[1])).getTime();
			long end = (sdf.parse(args[2])).getTime();
			
			int nday = (int)Math.floor((double)(end-start)/86400000.0)+1;
			System.err.println("Time plot for ndays: "+nday);
			
			int[] count = new int[nday];
			int[] cdx   = new int[nday];
			for (int i = 0; i < nday; i++) {
				count[i] = 0;
			}
			
			// DATA 2010-05-21 03:20:17    c=0 7080000   dm=0.00 0.05   x=0.00 396996.75    pi=61    p=118  118  118

			String line = null;
			while ((line = bin.readLine()) != null) {

				System.err.println("Check line: "+line);
				StringTokenizer st = new StringTokenizer(line);
				
				st.nextToken(); // DATA
				String strdate = st.nextToken(); // date
				String strtime = st.nextToken(); // time

				st.nextToken(); // cb
				st.nextToken(); // ca

				st.nextToken(); // db
                                st.nextToken(); // da

				long time = (sdf.parse(strdate+" "+strtime)).getTime();
				double xb = Double.parseDouble(st.nextToken()); // xb
				double xa = Double.parseDouble(st.nextToken()); // xa
							
				int dx = (int)((xa - xb)/60000.0); // how much exec have we added (mins)?
				
				int it = (int)Math.floor((double)(time-start)/86400000.0);
				count[it]++;
				cdx[it] += dx;
			}
			
			// Print em
			for (int i = 0; i < nday; i++) {				
				long time = start + 86400000*(long)i;
				System.err.printf("PLOT %tF %6d %6d \n", time, count[i], cdx[i]);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
