/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import org.eclipse.kura.web.client.util.KuraBaseModel;

public class GwtFirewallOpenPortEntry extends KuraBaseModel implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1153451329284913943L;

    public GwtFirewallOpenPortEntry() {
    }

    public String getPortRange() {
        return get("portRange");
    }

    public void setPortRange(String portRange) {
        set("portRange", portRange);
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
