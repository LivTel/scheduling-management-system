/**
 * 
 */
package ngat.sms.util;

import java.util.List;

import ngat.sms.ExecutionHistorySynopsis;
import ngat.sms.GroupItem;

/**
 * @author eng
 *
 */
public class GroupNodeDescriptor {

	public GroupItem group;
	
	public List<ExecutionHistorySynopsis> hist;
	
	public long execTime;
	
	public long startTime;
	
	public boolean std;
	
	public boolean bgstd;
	
	public int env;
	
	

	/**
	 * @param group
	 * @param startTime
	 */
	public GroupNodeDescriptor(GroupItem group) {
		super();
		this.group = group;	
	}
	
	
	
}
