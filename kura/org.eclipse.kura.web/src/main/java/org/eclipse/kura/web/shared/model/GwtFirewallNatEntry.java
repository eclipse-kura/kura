/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.data.BaseModelData;

public class GwtFirewallNatEntry extends BaseModelData implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6603318099742645871L;

	public String getInInterface() {
        return get("inInterface");
    }

    public void setInInterface(String inInterface) {
        set("inInterface", inInterface);
    }

	public String getOutInterface() {
        return get("outInterface");
    }

    public void setOutInterface(String outInterface) {
        set("outInterface", outInterface);
    }
    
    public String getProtocol() {
        return get("protocol");
    }

    public void setProtocol(String protocol) {
        set("protocol", protocol);
    }
    
    public String getSourceNetwork() {
        return get("sourceNetwork");
    }

    public void setSourceNetwork(String sourceNetwork) {
        set("sourceNetwork", sourceNetwork);
    }
    
    public String getDestinationNetwork() {
        return get("destinationNetwork");
    }

    public void setDestinationNetwork(String destinationNetwork) {
        set("destinationNetwork", destinationNetwork);
    }
    
    public String getMasquerade() {
    	return get("masquerade");
    }

    public void setMasquerade(String masquerade) {
        set("masquerade", masquerade);
    }
    
    public boolean equals(Object o) {
    	if (!(o instanceof GwtFirewallNatEntry)) {
    		return false;
    	}
    	
    	Map<String, Object> properties = this.getProperties();
        Map<String, Object> otherProps = ((GwtFirewallNatEntry)o).getProperties();
        
        if(properties != null) {
            if(otherProps == null) {
                return false;
            }            
            if(properties.size() != otherProps.size()) {
                Log.debug("Sizes differ");
                return false;
            }
            
            Object oldVal, newVal;
            for(String key : properties.keySet()) {
                oldVal = properties.get(key);
                newVal = otherProps.get(key);                
                if(oldVal != null) {
                    if(!oldVal.equals(newVal)) {
                        Log.debug("Values differ - Key: " + key + " oldVal: " + oldVal + ", newVal: " + newVal);
                        return false;
                    }
                } else if(newVal != null) {
                    return false;
                }
            }
        } else if(otherProps != null) {
            return false;
        }
    			
    	return true;
    }
}
