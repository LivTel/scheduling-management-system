/**
 * 
 */
package ngat.sms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author eng
 *
 */
public class ExecutionResourceBundle implements Serializable {

	private Map<String, ExecutionResource> resources;
	
	
	/**
	 * 
	 */
	public ExecutionResourceBundle() {
		resources = new HashMap<String, ExecutionResource>();
	}

	/* (non-Javadoc)
	 * @see ngat.sms.ExecutionResourceBundle#addResource(ngat.sms.ExecutionResource)
	 */
	public void addResource(ExecutionResource resource) {
		// TODO Auto-generated method stub
		if (resource == null)
			return;
		resources.put(resource.getResourceName(), resource);				
	}

	/* (non-Javadoc)
	 * @see ngat.sms.ExecutionResourceBundle#getResource(java.lang.String)
	 */
	public ExecutionResource getResource(String resName) throws IllegalArgumentException {
		
		if (! resources.containsKey(resName))
			throw new IllegalArgumentException("Unknown resource: "+resName);
		
		return resources.get(resName);
	
	}

	/* (non-Javadoc)
	 * @see ngat.sms.ExecutionResourceBundle#listResources()
	 */
	public Iterator<ExecutionResource> listResources() {
		return resources.values().iterator();
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		Iterator<ExecutionResource> ers = listResources();
		while (ers.hasNext()) {
			ExecutionResource res = ers.next();
			buffer.append("["+res.getResourceName()+":"+res.getResourceUsage()+"]");
			if (ers.hasNext())
				buffer.append(",");
		}
		return buffer.toString();
	}

}
