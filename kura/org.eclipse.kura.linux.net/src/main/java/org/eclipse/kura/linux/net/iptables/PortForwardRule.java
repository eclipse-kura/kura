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
package org.eclipse.kura.linux.net.iptables;

import java.util.List;

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
    private String sourcePortRange;

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
    @SuppressWarnings("checkstyle:parameterNumber")
    public PortForwardRule(String inboundIface, String outboundIface, String address, String protocol, int inPort,
            int outPort, boolean masquerade, String permittedNetwork, int permittedNetworkMask, String permittedMAC,
            String sourcePortRange) {
        this.inboundIface = inboundIface;
        this.outboundIface = outboundIface;
        this.inPort = inPort;
        this.protocol = protocol;
        this.address = address;
        this.outPort = outPort;
        this.masquerade = masquerade;

        this.permittedNetwork = permittedNetwork;
        this.permittedNetworkMask = permittedNetworkMask;
        this.permittedMAC = permittedMAC;
        this.sourcePortRange = sourcePortRange;
    }

    /**
     * Constructor of <code>PortForwardRule</code> object.
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
        this.sourcePortRange = null;
    }

    /**
     * Returns true if the required <code>LocalRule</code> parameters have all been set. Returns false otherwise.
     *
     * @return A boolean representing whether all parameters have been set.
     */
    public boolean isComplete() {
        if (this.protocol != null && this.inboundIface != null && this.outboundIface != null && this.address != null
                && this.inPort != 0 && this.outPort != 0) {
            return true;
        }
        return false;
    }

    public NatPreroutingChainRule getNatPreroutingChainRule() {
        int srcPortFirst = 0;
        int srcPortLast = 0;
        if (this.sourcePortRange != null) {
            srcPortFirst = Integer.parseInt(this.sourcePortRange.split(":")[0]);
            srcPortLast = Integer.parseInt(this.sourcePortRange.split(":")[1]);
        }
        return new NatPreroutingChainRule(this.inboundIface, this.protocol, this.inPort, this.outPort, srcPortFirst,
                srcPortLast, this.address, this.permittedNetwork, this.permittedNetworkMask, this.permittedMAC);
    }

    public NatPostroutingChainRule getNatPostroutingChainRule() {
        return new NatPostroutingChainRule(this.address, (short) 32, this.permittedNetwork,
                (short) this.permittedNetworkMask, this.outboundIface, this.protocol, this.masquerade);
    }

    public FilterForwardChainRule getFilterForwardChainRule() {
        int srcPortFirst = 0;
        int srcPortLast = 0;
        if (this.sourcePortRange != null) {
            srcPortFirst = Integer.parseInt(this.sourcePortRange.split(":")[0]);
            srcPortLast = Integer.parseInt(this.sourcePortRange.split(":")[1]);
        }
        return new FilterForwardChainRule(this.inboundIface, this.outboundIface, this.permittedNetwork,
                (short) this.permittedNetworkMask, this.address, (short) 32, this.protocol, this.permittedMAC,
                srcPortFirst, srcPortLast);
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
        return this.sourcePortRange;
    }

    /**
     * Setter for sourcePortRange
     *
     * @param sourcePortRange
     *            the sourcePortRange to set
     */
    public void setSourcePortRange(String sourcePortRange) {
        this.sourcePortRange = sourcePortRange;
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
                && compareObjects(this.sourcePortRange, other.sourcePortRange);
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
        result = prime * result + (this.sourcePortRange == null ? 0 : this.sourcePortRange.hashCode());

        return result;
    }
}
