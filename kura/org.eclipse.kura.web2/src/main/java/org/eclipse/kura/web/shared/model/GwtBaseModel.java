package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class GwtBaseModel implements Serializable {

	private static final long serialVersionUID = -4890171188631895494L;

	protected HashMap<String, Object> data;

	public GwtBaseModel() {
		data = new HashMap<String, Object>();
	}
	
	@SuppressWarnings("unchecked")
	public <X> X get(String key){
		return (X) data.get(key);
	}
	
	public void set(String key, Object value){
		data.put(key, value);
	}
	
	public void setProperties(Map<String, Object> properties) {
		for (String property : properties.keySet()) {
			set(property, properties.get(property));
		}
	}
	  
	public Map<String, Object> getProperties() {
		return data;
	}

}
