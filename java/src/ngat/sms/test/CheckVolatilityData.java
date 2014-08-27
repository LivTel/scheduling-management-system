/**
 * 
 */
package ngat.sms.test;

import ngat.sms.*;
import ngat.phase2.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * @author eng
 * 
 */
public class CheckVolatilityData {
    
    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	try {
	    File base = new File(args[0]);
	    System.err.println("Extracting volatility info from: " + base);

	    FilenameFilter f = new FilenameFilter() {
		    public boolean accept(File dir, String name) {
			if (name.endsWith(".dat"))
			    return true;
			return false;
		    }
		};

	    System.err.println("Using filter: "+f);
	    
	    int iok = 0;
	    File[] flist = base.listFiles(f);
	    for (int i = 0; i < flist.length; i++) {
		File file = flist[i];
		
		System.err.println("Checking file: "+file+"...");
		
		try {
		    GroupItem g1 = (GroupItem) readGroup(file);
		    System.err.println("Read: "+g1.getName());
		    iok++;
		} catch (Exception re) {
		    System.err.println("Error reading file: "+re.getMessage());
		}
	    } // next file
	    
	    System.err.println("Checked "+flist.length+" files, "+iok+" were ok, "+(flist.length-iok)+" were duff");

	    
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static Object readGroup(File file) throws Exception {
	
	ObjectInputStream oin = new ObjectInputStream(new FileInputStream(file));
	Object obj = oin.readObject();
	oin.close();
	return obj;
	
    }
}

