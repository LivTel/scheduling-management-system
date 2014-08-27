/**
 * 
 */
package ngat.sms.tlas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author eng
 * 
 */
public class GenomeFactory {

	private char[] caps = new char[] { 'B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T',
			'V', 'X' };

	private char[] low = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
			'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	// map genome to gid
	private Map<Long, String> gmap = new HashMap<Long, String>();
	private List<String> gstrings = new Vector<String>();

	public String genome(long gid) {
		String gstring = null;
		if (gmap.containsKey(gid)) {
			gstring = gmap.get(gid);
		} else {
			gstring = newGenome();
			gmap.put(gid, gstring);
		}
		return gstring;
	}

	private String newGenome() {

		String test = null;
		boolean done = false;
		while (!done) {

			int ci = (int) (Math.random() * 182828) % caps.length;
			int li = (int) (Math.random() * 847474) % low.length;
			int ui = (int) (Math.random() * 182828) % caps.length;

			char[] cc = new char[3];
			cc[0] = caps[ci];
			cc[1] = low[li];
			cc[2] = caps[ui];

			test = new String(cc);

			if (!gstrings.contains(test)) {
				gstrings.add(test);
				done = true;
			}

		}
		return test;

	}

}
