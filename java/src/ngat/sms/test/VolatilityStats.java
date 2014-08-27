/**
 * 
 */
package ngat.sms.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;
import java.util.*;
import java.text.*;

/**
 * @author eng
 *
 */
public class VolatilityStats {
	

    // DATA 2010-05-21 03:20:17    c=0 7080000   dm=0.00 0.05   x=0.00 396996.75    pi=61    p=118  118  118

    static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void main(String [] args) {
		
	    sdf.setTimeZone(UTC);

		int[] pibin = new int[20];
		double piav = 0.0;
		
		int[] sbin  = new int[30];
		double spav = 0.0;

		int[] ubin = new int[24];

		int[] xbin = new int[40];
		double xav = 0.0;
		

		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(UTC);
		try {
			
			BufferedReader bin = new BufferedReader(new FileReader(args[0]));
			
			int n = 0;
			String line = null;
			while ((line = bin.readLine()) != null) {
				
				StringTokenizer st = new StringTokenizer(line);
				
				st.nextToken(); // DATA

				String strdate = st.nextToken(); // date
				String strtime = st.nextToken(); // time
					
				Date date = sdf.parse(strdate+" "+strtime);
				cal.setTime(date);
				int iut = cal.get(Calendar.HOUR_OF_DAY);

				ubin[iut]++; // count arrivals in hourly bins
				
				st.nextToken(); // cb
				st.nextToken(); // ca
				st.nextToken(); // db
				st.nextToken(); // da
				double xb = Double.parseDouble(st.nextToken()); // xb
                                double xa = Double.parseDouble(st.nextToken()); // xa
				xa /= 60000.0; // to mins

				xav += xa; 
				int ix = (int)Math.floor((double)xa/5.0);
				if (ix < 40)
				    xbin[ix] += 1.0;

				int pi = Integer.parseInt(st.nextToken()); // PI
				int span = Integer.parseInt(st.nextToken()); // SPAN
				
				piav += (double)pi;
				int ipi = (int)Math.floor((double)pi/25.0);
				if (ipi < 20)
					pibin[ipi]+=1.0;
				
				spav += (double)span;
				int isp = (int)Math.floor((double)span/25.0);
				if (isp < 30)
					sbin[isp]+=1.0;
				
				n++;
			
			}
			
			System.err.printf("UT histogram");
			for (int i = 0; i < 24; i++) {
			    System.err.printf("UT: %4d %8d\n",i, ubin[i]);
                        }

			System.err.printf("XT histogram, av: %4.2f\n", xav/(double)n);
                        for (int i = 0; i < 20; i++) {
			    System.err.printf("XT: %4d %4d %8d\n",(i*40), (i*40+40), xbin[i]);
                        }

			System.err.printf("PI histogram, av: %4.2f\n", piav/(double)n);
			for (int i = 0; i < 20; i++) {
				System.err.printf("PI: %4d %4d %8d\n",(i*25), (i*25+25), pibin[i]);
			}
			
			System.err.printf("SPAN histogram, av: %4.2f\n", spav/(double)n);
			for (int i = 0; i < 30; i++) {
				System.err.printf("SPAN: %4d %4d %8d\n",(i*25), (i*25+25), sbin[i]);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
}
