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

import com.extjs.gxt.ui.client.data.BaseModelData;

public class GwtFirewallPortForwardEntry extends BaseModelData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2257728265961620948L;

	public GwtFirewallPortForwardEntry() {}
	
	public String getInterfaceName() {
        return get("interfaceName");
    }

    public void setInterfaceName(String interfaceName) {
        set("interfaceName", interfaceName);
    }
    
    public String getAddress() {
        return get("address");
    }

    public void setAddress(String address) {
        set("address", address);
    }
    
    public String getProtocol() {
        return get("protocol");
    }

    public void setProtocol(String protocol) {
        set("protocol", protocol);
    }
    
    public Integer getInPort() {
    	if (get("inPort") != null) {
    		return (Integer) get("inPort");
    	}
    	else {
    		return 0;
    	}
    }

    public void setInPort(int inPort) {
        set("inPort", inPort);
    }
    
    public Integer getOutPort() {
    	if (get("outPort") != null) {
    		return (Integer) get("outPort");
    	}
    	else {
    		return 0;
    	}
    }

    public void setOutPort(int outPort) {
        set("outPort", outPort);
    }
    
    public String getPermittedNetwork() {
        return get("permittedNetwork");
    }

    public void setPermittedNetwork(String permittedNetwork) {
        set("permittedNetwork", permittedNetwork);
    }
    
    public String getPermittedMAC() {
        return get("permittedMAC");
    }

    public void setPermittedMAC(String permittedMAC) {
        set("permittedMAC", permittedMAC);
    }
    
    public String getSourcePortRange() {
        return get("sourcePortRange");
    }

    public void setSourcePortRange(String sourcePortRange) {
        set("sourcePortRange", sourcePortRange);
    }
}
