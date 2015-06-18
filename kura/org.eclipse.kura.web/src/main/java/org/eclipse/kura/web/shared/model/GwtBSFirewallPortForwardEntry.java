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

public class GwtBSFirewallPortForwardEntry extends GwtBSBaseModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5028849108840020090L;

	public GwtBSFirewallPortForwardEntry() {}
	
	public String getInboundInterface() {
        return (String) get("inboundInterface");
    }

    public void setInboundInterface(String interfaceName) {
        set("inboundInterface", interfaceName);
    }
    
    public String getOutboundInterface() {
        return (String) get("outboundInterface");
    }

    public void setOutboundInterface(String interfaceName) {
        set("outboundInterface", interfaceName);
    }
    
    public String getAddress() {
        return (String) get("address");
    }

    public void setAddress(String address) {
        set("address", address);
    }
    
    public String getProtocol() {
        return (String) get("protocol");
    }

    public void setProtocol(String protocol) {
        set("protocol", protocol);
    }
    
    public int getInPort() {
    	if (get("inPort") != null) {
    		return (Integer)((get("inPort")));
    	}
    	else {
    		return 0;
    	}
    }

    public void setInPort(int inPort) {
        set("inPort", String.valueOf(inPort));
    }
    
    public int getOutPort() {
    	if (get("outPort") != null) {
    		return (Integer)get("outPort");
    	}
    	else {
    		return 0;
    	}
    }

    public void setOutPort(int outPort) {
        set("outPort", String.valueOf(outPort));
    }
    
    public String getMasquerade() {
    	return (String) get("masquerade");
    }

    public void setMasquerade(String masquerade) {
        set("masquerade", masquerade);
    }
    
    public String getPermittedNetwork() {
        return (String) get("permittedNetwork");
    }

    public void setPermittedNetwork(String permittedNetwork) {
        set("permittedNetwork", permittedNetwork);
    }
    
    public String getPermittedMAC() {
        return (String) get("permittedMAC");
    }

    public void setPermittedMAC(String permittedMAC) {
        set("permittedMAC", permittedMAC);
    }
    
    public String getSourcePortRange() {
        return (String) get("sourcePortRange");
    }

    public void setSourcePortRange(String sourcePortRange) {
        set("sourcePortRange", sourcePortRange);
    }
}
