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

public class GwtFirewallOpenPortEntry extends BaseModelData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1153451329284913943L;

	public GwtFirewallOpenPortEntry() {}
	
	public Integer getPort() {
    	if (get("port") != null) {
    		return (Integer) get("port");
    	}
    	else {
    		return 0;
    	}
    }

    public void setPort(int port) {
        set("port", port);
    }
    
    public String getProtocol() {
        return get("protocol");
    }

    public void setProtocol(String protocol) {
        set("protocol", protocol);
    }
    
    public String getPermittedNetwork() {
        return get("permittedNetwork");
    }

    public void setPermittedNetwork(String permittedNetwork) {
        set("permittedNetwork", permittedNetwork);
    }
    
    public String getPermittedInterfaceName() {
    	return get("permittedInterfaceName");
    }
    
    public void setPermittedInterfaceName(String permittedInterfaceName) {
    	set("permittedInterfaceName", permittedInterfaceName);
    }
    
    public String getUnpermittedInterfaceName() {
    	return get("unpermittedInterfaceName");
    }
    
    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
    	set("unpermittedInterfaceName", unpermittedInterfaceName);
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
