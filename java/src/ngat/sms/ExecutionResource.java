/**
 * 
 */
package ngat.sms;

import java.io.Serializable;

/**
 * @author eng
 *
 */
public class ExecutionResource implements Serializable {

	private String resourceName;
	
	private double resourceUsage;
	
	private String resourceDescription;

	/**
	 * @param resourceName
	 * @param resourceUsage
	 */
	public ExecutionResource(String resourceName, double resourceUsage) {
		this.resourceName = resourceName;
		this.resourceUsage = resourceUsage;
	}

	/**
	 * @return the resourceName
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * @param resourceName the resourceName to set
	 */
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	/**
	 * @return the resourceUsage
	 */
	public double getResourceUsage() {
		return resourceUsage;
	}

	/**
	 * @param resourceUsage the resourceUsage to set
	 */
	public void setResourceUsage(double resourceUsage) {
		this.resourceUsage = resourceUsage;
	}
	
	
	
	/**
	 * @return the resourceDescription
	 */
	public String getResourceDescription() {
		return resourceDescription;
	}

	/**
	 * @param resourceDescription the resourceDescription to set
	 */
	public void setResourceDescription(String resourceDescription) {
		this.resourceDescription = resourceDescription;
	}

	public String toString(){
		return "["+resourceName+"::"+resourceUsage+"]";
	}
	
}
