/**
 * 
 */
package ngat.sms;

/** Records veto information.
 * @author eng
 *
 */
public class Veto {

	public long gid;
	
	public long untilTime;

	/**
	 * @param gid
	 * @param untilTime
	 */
	public Veto(long gid, long untilTime) {
		super();
		this.gid = gid;
		this.untilTime = untilTime;
	}
	
	
}
