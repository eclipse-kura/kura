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

public class GwtBSFirewallOpenPortEntry extends GwtBSBaseModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1153451329284913943L;

	public GwtBSFirewallOpenPortEntry() {}
	
	public int getPort() {
    	if (get("port") != null) {
    		return ((Integer) get("port")).intValue();
    	}
    	else {
    		return 0;
    	}
    }

    public void setPort(int port) {
        set("port", String.valueOf(port));
    }
    
    public String getProtocol() {
        return (String) get("protocol");
    }

    public void setProtocol(String protocol) {
        set("protocol", protocol);
    }
    
    public String getPermittedNetwork() {
        return (String) get("permittedNetwork");
    }

    public void setPermittedNetwork(String permittedNetwork) {
        set("permittedNetwork", permittedNetwork);
    }
    
    public String getPermittedInterfaceName() {
    	return (String) get("permittedInterfaceName");
    }
    
    public void setPermittedInterfaceName(String permittedInterfaceName) {
    	set("permittedInterfaceName", permittedInterfaceName);
    }
    
    public String getUnpermittedInterfaceName() {
    	return (String) get("unpermittedInterfaceName");
    }
    
    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
    	set("unpermittedInterfaceName", unpermittedInterfaceName);
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
