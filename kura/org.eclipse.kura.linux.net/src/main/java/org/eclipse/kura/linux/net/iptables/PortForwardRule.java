/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.iptables;

import java.util.List;

import org.eclipse.kura.net.firewall.RuleType;

/**
 * Creates an iptables command for a Port Forward Rule, allowing an incoming port to be forwarded to destinationIP/port.
 *
 */
public class PortForwardRule {

    // required
    private String inboundIface;
    private String outboundIface;
    private String address;
    private String protocol;
    private int inPort;
    private int outPort;
    private boolean masquerade;

    // optional
    private String permittedNetwork;
    private int permittedNetworkMask;
    private String permittedMAC;
    private int sourcePortStart;
    private int sourcePortEnd;

    /**
     * Constructor of <code>PortForwardRule</code> object.
     *
     * @param inboundIface
     *            interface name on which inbound connection is allowed (such as ppp0)
     * @param outboundIface
     *            interface name on which outbound connection is allowed (such as eth0)
     * @param inPort
     *            inbound port on which to listen for port forward
     * @param protocol
     *            protocol of port connection (tcp, udp)
     * @param address
     *            destination IP address to forward IP traffic
     * @param outPort
     *            destination port to forward IP traffic
     * @param masquerade
     *            use masquerading
     * @param permittedNetwork
     *            source network or ip address from which connection is allowed (such as 192.168.1.0)
     * @param permittedNetworkMask
     *            source network mask from which connection is allowed (e.g. 24 for 255.255.255.0)
     * @param permittedMAC
     *            MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
     * @param sourcePortRange
     *            range of source ports allowed on IP connection (sourcePort1:sourcePort2)
     */
    public PortForwardRule() {
        this.inboundIface = null;
        this.outboundIface = null;
        this.inPort = 0;
        this.protocol = null;
        this.address = null;
        this.outPort = 0;
        this.masquerade = false;
        this.permittedNetworkMask = 0;
        this.permittedNetwork = null;
        this.permittedMAC = null;
        this.sourcePortStart = 0;
        this.sourcePortEnd = 0;
    }

    public PortForwardRule inboundIface(String inboundIface) {
        this.inboundIface = inboundIface;
        return this;
    }

    public PortForwardRule outboundIface(String outboundIface) {
        this.outboundIface = outboundIface;
        return this;
    }

    public PortForwardRule address(String address) {
        this.address = address;
        return this;
    }

    public PortForwardRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public PortForwardRule inPort(int inPort) {
        this.inPort = inPort;
        return this;
    }

    public PortForwardRule outPort(int outPort) {
        this.outPort = outPort;
        return this;
    }

    public PortForwardRule masquerade(boolean masquerade) {
        this.masquerade = masquerade;
        return this;
    }

    public PortForwardRule permittedNetwork(String permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
        return this;
    }

    public PortForwardRule permittedNetworkMask(int permittedNetworkMask) {
        this.permittedNetworkMask = permittedNetworkMask;
        return this;
    }

    public PortForwardRule permittedMAC(String permittedMAC) {
        this.permittedMAC = permittedMAC;
        return this;
    }

    public PortForwardRule sourcePortRange(String sourcePortRange) {
        parsePortRange(sourcePortRange);
        return this;
    }

    private void parsePortRange(String sourcePortRange) {
        if (sourcePortRange != null) {
            int start;
            int end;
            if (sourcePortRange.contains(":")) {
                String[] ports = sourcePortRange.split(":");
                start = Integer.parseInt(ports[0]);
                end = Integer.parseInt(ports[1]);
            } else {
                start = Integer.parseInt(sourcePortRange);
                end = start;
            }
            this.sourcePortStart = start;
            this.sourcePortEnd = end;
        }
    }

    /**
     * Returns true if the required <code>PortForwardRule</code> parameters have all been set. Returns false otherwise.
     *
     * @return A boolean representing whether all parameters have been set.
     */
    public boolean isComplete() {
        return this.protocol != null && this.inboundIface != null && this.outboundIface != null && this.address != null
                && this.inPort != 0 && this.outPort != 0;
    }

    public NatPreroutingChainRule getNatPreroutingChainRule() {
        return new NatPreroutingChainRule().inputInterface(this.inboundIface).protocol(this.protocol)
                .externalPort(this.inPort).internalPort(this.outPort).srcPortFirst(this.sourcePortStart)
                .srcPortLast(this.sourcePortEnd).dstIpAddress(this.address).permittedNetwork(this.permittedNetwork)
                .permittedNetworkMask(this.permittedNetworkMask).permittedMacAddress(this.permittedMAC)
                .type(RuleType.PORT_FORWARDING);
    }

    public NatPostroutingChainRule getNatPostroutingChainRule() {
        return new NatPostroutingChainRule().dstNetwork(this.address).dstMask((short) 32)
                .srcNetwork(this.permittedNetwork).srcMask((short) this.permittedNetworkMask)
                .dstInterface(this.outboundIface).protocol(this.protocol).masquerade(this.masquerade)
                .type(RuleType.PORT_FORWARDING);
    }

    public FilterForwardChainRule getFilterForwardChainRule() {
        return new FilterForwardChainRule().inputInterface(this.inboundIface).outputInterface(this.outboundIface)
                .srcNetwork(this.permittedNetwork).srcMask((short) this.permittedNetworkMask).dstNetwork(this.address)
                .dstMask((short) 32).protocol(this.protocol).permittedMacAddress(this.permittedMAC)
                .srcPortFirst(this.sourcePortStart).srcPortLast(this.sourcePortEnd).type(RuleType.PORT_FORWARDING);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<String> forwardRules = getFilterForwardChainRule().toStrings();
        for (String forwardRule : forwardRules) {
            sb.append(forwardRule).append("; ");
        }
        sb.append(getNatPreroutingChainRule().toString()).append("; ");
        sb.append(getNatPostroutingChainRule().toString());
        return sb.toString();
    }

    /**
     * Getter for inbound iface
     *
     * @return the iface
     */
    public String getInboundIface() {
        return this.inboundIface;
    }

    /**
     * Setter for iface
     *
     * @param iface
     *            the iface to set
     */
    public void setInboundIface(String iface) {
        this.inboundIface = iface;
    }

    /**
     * Getter for inbound iface
     *
     * @return the iface
     */
    public String getOutboundIface() {
        return this.outboundIface;
    }

    /**
     * Setter for iface
     *
     * @param iface
     *            the iface to set
     */
    public void setOutboundIface(String iface) {
        this.outboundIface = iface;
    }

    /**
     * Getter for address
     *
     * @return the address
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Setter for address
     *
     * @param address
     *            the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Getter for protocol
     *
     * @return the protocol
     */
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * Setter for protocol
     *
     * @param protocol
     *            the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Getter for inPort
     *
     * @return the inPort
     */
    public int getInPort() {
        return this.inPort;
    }

    /**
     * Setter for inPort
     *
     * @param inPort
     *            the inPort to set
     */
    public void setInPort(int inPort) {
        this.inPort = inPort;
    }

    /**
     * Getter for outPort
     *
     * @return the outPort
     */
    public int getOutPort() {
        return this.outPort;
    }

    /**
     * Setter for outPort
     *
     * @param outPort
     *            the outPort to set
     */
    public void setOutPort(int outPort) {
        this.outPort = outPort;
    }

    /**
     * Getter for masquerade
     *
     * @return the 'masquerade' flag
     */
    public boolean isMasquerade() {
        return this.masquerade;
    }

    /**
     * Setter for masquerade
     *
     * @param masquerade
     *            - 'masquerade' flag
     */
    public void setMasquerade(boolean masquerade) {
        this.masquerade = masquerade;
    }

    /**
     * Getter for permittedNetwork
     *
     * @return the permittedNetwork
     */
    public String getPermittedNetwork() {
        return this.permittedNetwork;
    }

    /**
     * Setter for permittedNetwork
     *
     * @param permittedNetwork
     *            the permittedNetwork to set
     */
    public void setPermittedNetwork(String permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
    }

    /**
     * Getter for permittedNetworkMask
     *
     * @return the permittedNetworkMask
     */
    public int getPermittedNetworkMask() {
        return this.permittedNetworkMask;
    }

    /**
     * Setter for permittedNetworkMask
     *
     * @param permittedNetworkMask
     *            of the permittedNetwork to set
     */
    public void setPermittedNetworkMask(int permittedNetworkMask) {
        this.permittedNetworkMask = permittedNetworkMask;
    }

    /**
     * Getter for permittedMAC
     *
     * @return the permittedMAC
     */
    public String getPermittedMAC() {
        return this.permittedMAC;
    }

    /**
     * Setter for permittedMAC
     *
     * @param permittedMAC
     *            the permittedMAC to set
     */
    public void setPermittedMAC(String permittedMAC) {
        this.permittedMAC = permittedMAC;
    }

    /**
     * Getter for sourcePortRange
     *
     * @return the sourcePortRange
     */
    public String getSourcePortRange() {
        if (this.sourcePortStart == this.sourcePortEnd && this.sourcePortStart == 0) {
            return null;
        }
        return this.sourcePortStart + ":" + this.sourcePortEnd;
    }

    /**
     * Setter for sourcePortRange
     *
     * @param sourcePortRange
     *            the sourcePortRange to set
     */
    public void setSourcePortRange(String sourcePortRange) {
        parsePortRange(sourcePortRange);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PortForwardRule)) {
            return false;
        }

        PortForwardRule other = (PortForwardRule) o;

        return compareObjects(this.inboundIface, other.inboundIface)
                && compareObjects(this.outboundIface, other.outboundIface)
                && compareObjects(this.address, other.address) && compareObjects(this.protocol, other.protocol)
                && this.inPort == other.inPort && this.outPort == other.outPort && this.masquerade == other.masquerade
                && compareObjects(this.permittedNetwork, other.permittedNetwork)
                && this.permittedNetworkMask == other.permittedNetworkMask
                && compareObjects(this.permittedMAC, other.permittedMAC)
                && compareObjects(this.sourcePortStart, other.sourcePortStart)
                && compareObjects(this.sourcePortEnd, other.sourcePortEnd);
    }

    private boolean compareObjects(Object obj1, Object obj2) {
        if (obj1 != null) {
            return obj1.equals(obj2);
        } else if (obj2 != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 79;
        int result = 1;
        result = prime * result + this.inPort;
        result = prime * result + this.outPort;
        result = prime * result + (this.inboundIface == null ? 0 : this.inboundIface.hashCode());
        result = prime * result + (this.outboundIface == null ? 0 : this.outboundIface.hashCode());
        result = prime * result + (this.masquerade ? 1231 : 1237);
        result = prime * result + (this.address == null ? 0 : this.address.hashCode());
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.permittedNetwork == null ? 0 : this.permittedNetwork.hashCode());
        result = prime * result + this.permittedNetworkMask;
        result = prime * result + (this.permittedMAC == null ? 0 : this.permittedMAC.hashCode());
        result = prime * result + this.sourcePortStart;
        result = prime * result + this.sourcePortEnd;

        return result;
    }
}
