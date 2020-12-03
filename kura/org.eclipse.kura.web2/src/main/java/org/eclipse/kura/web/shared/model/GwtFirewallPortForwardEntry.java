/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtFirewallPortForwardEntry extends KuraBaseModel implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5028849108840020090L;

    public GwtFirewallPortForwardEntry() {
    }

    public String getInboundInterface() {
        return get("inboundInterface");
    }

    public void setInboundInterface(String interfaceName) {
        set("inboundInterface", interfaceName);
    }

    public String getOutboundInterface() {
        return get("outboundInterface");
    }

    public void setOutboundInterface(String interfaceName) {
        set("outboundInterface", interfaceName);
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
        } else {
            return 0;
        }
    }

    public void setInPort(int inPort) {
        set("inPort", inPort);
    }

    public Integer getOutPort() {
        if (get("outPort") != null) {
            return (Integer) get("outPort");
        } else {
            return 0;
        }
    }

    public void setOutPort(int outPort) {
        set("outPort", outPort);
    }

    public String getMasquerade() {
        return get("masquerade");
    }

    public void setMasquerade(String masquerade) {
        set("masquerade", masquerade);
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

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("inboudInterface : ").append(getInboundInterface());
        output.append(" ");
        output.append("outboudInterface : ").append(getOutboundInterface());
        output.append(" ");
        output.append("addess : ").append(getAddress());
        output.append(" ");
        output.append("protocol : ").append(getProtocol());
        output.append(" ");
        output.append("inputPort : ").append(getInPort());
        output.append(" ");
        output.append("outputPort : ").append(getOutPort());
        output.append(" ");
        output.append("network : ").append(getPermittedNetwork());
        output.append(" ");
        output.append("permittedMAC : ").append(getPermittedMAC());
        output.append(" ");
        output.append("sourcePortRange : ").append(getSourcePortRange());

        return output.toString();
    }
}
