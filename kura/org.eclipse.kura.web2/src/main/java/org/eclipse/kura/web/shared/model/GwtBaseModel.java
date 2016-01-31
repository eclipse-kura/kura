package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

@SuppressWarnings("unused")
public class GwtBaseModel implements IsSerializable, Serializable {

	private static final long serialVersionUID = -4890171188631895494L;
	
	// Unused members needed for GWT serialization
	private Date _date;
	
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
