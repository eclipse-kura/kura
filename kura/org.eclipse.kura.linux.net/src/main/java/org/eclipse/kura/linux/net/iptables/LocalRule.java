/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Fix build warnings
 *******************************************************************************/
package org.eclipse.kura.linux.net.iptables;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;

/**
 * Creates an iptables command for a Local Rule, allowing an incoming port connection.
 *
 */
public class LocalRule {

    // required vars
    private int port;
    private String portRange;
    private String protocol;

    // optional vars
    private String permittedNetworkString;
    private String permittedInterfaceName;
    private String unpermittedInterfaceName;
    private String permittedMAC;
    private String sourcePortRange;

    /**
     * Constructor of <code>LocalRule</code> object.
     *
     * @param port
     *            destination local IP port number to allow
     * @param protocol
     *            protocol of port (tcp, udp)
     * @param sourcePortRange
     *            range of source ports allowed on IP connection (sourcePort1:sourcePort2)
     * @param permittedNetwork
     *            source network or ip address from which connection is allowed (such as 192.168.1.0/24)
     * @param permittedInterfaceName
     *            only allow open port for this interface
     * @param unpermittedInterfaceName
     *            allow open port for all interfaces except this one
     * @param permittedMAC
     *            MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
     */
    public LocalRule(int port, String protocol, NetworkPair<IP4Address> permittedNetwork, String permittedInterfaceName,
            String unpermittedInterfaceName, String permittedMAC, String sourcePortRange) {
        this.port = port;
        this.portRange = null;
        this.protocol = protocol;
        this.sourcePortRange = sourcePortRange;

        if (permittedNetwork != null) {
            this.permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/"
                    + permittedNetwork.getPrefix();
        } else {
            this.permittedNetworkString = "0.0.0.0/0";
        }

        this.permittedInterfaceName = permittedInterfaceName;
        this.unpermittedInterfaceName = unpermittedInterfaceName;
        this.permittedMAC = permittedMAC;
    }

    /**
     * Constructor of <code>LocalRule</code> object.
     *
     * @param portRange
     *            destination local IP port range to allow of the form X:Y where X<Y and both are valid ports
     * @param protocol
     *            protocol of port (tcp, udp)
     * @param sourcePortRange
     *            range of source ports allowed on IP connection (sourcePort1:sourcePort2)
     * @param permittedNetwork
     *            source network or ip address from which connection is allowed (such as 192.168.1.0/24)
     * @param permittedInterfaceName
     *            only allow open port for this interface
     * @param unpermittedInterfaceName
     *            allow open port for all interfaces except this one
     * @param permittedMAC
     *            MAC address from which connection is allowed (such as AA:BB:CC:DD:EE:FF)
     */
    public LocalRule(String portRange, String protocol, NetworkPair<IP4Address> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC,
            String sourcePortRange) {
        this.port = -1;
        this.portRange = portRange;
        this.protocol = protocol;
        this.sourcePortRange = sourcePortRange;

        if (permittedNetwork != null) {
            this.permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/"
                    + permittedNetwork.getPrefix();
        } else {
            this.permittedNetworkString = "0.0.0.0/0";
        }

        this.permittedInterfaceName = permittedInterfaceName;
        this.unpermittedInterfaceName = unpermittedInterfaceName;
        this.permittedMAC = permittedMAC;
    }

    /**
     * Constructor of <code>LocalRule</code> object.
     */
    public LocalRule() {
        this.port = -1;
        this.portRange = null;
        this.protocol = null;
        this.permittedNetworkString = null;
        this.permittedInterfaceName = null;
        this.unpermittedInterfaceName = null;
        this.permittedMAC = null;
        this.sourcePortRange = null;
    }

    public LocalRule(String rule) throws KuraException {
        try {
            String[] aRuleTokens = rule.split(" ");
            for (int i = 0; i < aRuleTokens.length; i++) {
                if ("-i".equals(aRuleTokens[i])) {
                    if ("!".equals(aRuleTokens[i - 1])) {
                        this.unpermittedInterfaceName = aRuleTokens[++i];
                    } else {
                        this.permittedInterfaceName = aRuleTokens[++i];
                    }
                } else if ("-s".equals(aRuleTokens[i])) {
                    this.permittedNetworkString = aRuleTokens[++i];
                } else if ("-p".equals(aRuleTokens[i])) {
                    this.protocol = aRuleTokens[++i];
                } else if ("--dport".equals(aRuleTokens[i])) {
                    if (aRuleTokens[i + 1].indexOf(':') > 0) {
                        this.portRange = aRuleTokens[++i];
                        this.port = -1;
                    } else {
                        this.port = Integer.parseInt(aRuleTokens[++i]);
                        this.portRange = null;
                    }
                } else if ("--sport".equals(aRuleTokens[i])) {
                    this.sourcePortRange = aRuleTokens[++i];
                } else if ("--mac-source".equals(aRuleTokens[i])) {
                    this.permittedMAC = aRuleTokens[++i];
                }
            }
            if (this.permittedNetworkString == null) {
                this.permittedNetworkString = "0.0.0.0/0";
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    /**
     * Returns true if the required <code>LocalRule</code> parameters have all been set. Returns false otherwise.
     *
     * @return A boolean representing whether all parameters have been set.
     */
    public boolean isComplete() {
        if (this.protocol != null && this.port != -1) {
            return true;
        } else if (this.protocol != null && this.portRange != null) {
            return isPortRangeValid(this.portRange) ? true : false;
        } else {
            return false;
        }
    }

    /**
     * Setter for the protocol.
     *
     * @param protocol
     *            A String representing the protocol.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Setter for the permittedNetwork.
     *
     * @param permittedNetwork
     *            A String representing the permittedNetwork.
     */
    public void setPermittedNetwork(NetworkPair<IP4Address> permittedNetwork) {
        if (permittedNetwork != null) {
            this.permittedNetworkString = permittedNetwork.getIpAddress().getHostAddress() + "/"
                    + permittedNetwork.getPrefix();
        } else {
            this.permittedNetworkString = "0.0.0.0/0";
        }
    }

    /**
     * Setter for the permittedInterfaceName.
     *
     * @param permittedInterfaceName
     *            A String representing the only interface allowed on this open port
     */
    public void setPermittedInterfaceName(String permittedInterfaceName) {
        this.permittedInterfaceName = permittedInterfaceName;
    }

    /**
     * Setter for the unpermittedInterfaceName.
     *
     * @param unpermittedInterfaceName
     *            A String representing the only interface not allowed on this open port
     */
    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
        this.unpermittedInterfaceName = unpermittedInterfaceName;
    }

    /**
     * Setter for the permittedMAC.
     *
     * @param permittedMAC
     *            A String representing the permittedMAC.
     */
    public void setPermittedMAC(String permittedMAC) {
        this.permittedMAC = permittedMAC;
    }

    /**
     * Setter for the sourcePortRange.
     *
     * @param sourcePortRange
     *            A String representing the sourcePortRange.
     */
    public void setSourcePortRange(String sourcePortRange) {
        this.sourcePortRange = sourcePortRange;
    }

    /**
     * Setter for the port.
     *
     * @param port
     *            An int representing the port.
     */
    public void setPort(int port) {
        this.port = port;
        this.portRange = null;
    }

    /**
     * Setter for the portRange
     *
     * @param portRange
     *            A string representing the port range of the form X:Y where X < Y and both are valid ports
     */
    public void setPortRange(String portRange) {
        this.port = -1;
        this.portRange = portRange;
    }

    /**
     * Getter for the sourcePortRange.
     *
     * @return the sourcePortRange
     */
    public String getSourcePortRange() {
        return this.sourcePortRange;
    }

    /**
     * Getter for the permittedInterfaceName.
     *
     * @param permittedInterfaceName
     *            A String representing the only interface allowed on this open port
     */
    public String getPermittedInterfaceName() {
        return this.permittedInterfaceName;
    }

    /**
     * Getter for the unpermittedInterfaceName.
     *
     * @param unpermittedInterfaceName
     *            A String representing the only interface not allowed on this open port
     */
    public String getUnpermittedInterfaceName() {
        return this.unpermittedInterfaceName;
    }

    /**
     * Getter for port
     *
     * @return the port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Getter for portRange
     *
     * @return the portRange
     */
    public String getPortRange() {
        return this.portRange;
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
     * Getter for permittedNetwork
     *
     * @return the permittedNetwork
     */
    public NetworkPair<IP4Address> getPermittedNetwork() throws KuraException {
        try {
            if (this.permittedNetworkString != null) {
                String[] split = this.permittedNetworkString.split("/");
                return new NetworkPair<>((IP4Address) IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
            } else {
                return new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
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
     * Converts the <code>LocalRule</code> to a <code>String</code>.
     * Returns one of the following iptables strings depending on the <code>LocalRule</code> format:
     * <code>
     * <p>  -A INPUT -p {protocol} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} -m mac --mac-source {permittedMAC} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} -m mac --mac-source {permittedMAC} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} -s {permittedNetwork} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} -s {permittedNetwork} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --dport {port} -j ACCEPT
     * <p>  -A INPUT -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * </code>
     */
    @Override
    public String toString() {
        String interfaceString = null;
        if (this.permittedInterfaceName != null) {
            interfaceString = new StringBuilder().append(" -i ").append(this.permittedInterfaceName).toString();
        } else if (this.unpermittedInterfaceName != null) {
            interfaceString = new StringBuilder().append(" ! -i ").append(this.unpermittedInterfaceName).toString();
        }

        if (this.port != -1) {
            return getLocalRuleWithPort(interfaceString);
        } else {
            return getLocalRuleWithoutPort(interfaceString);
        }
    }

    private String getLocalRuleWithPort(String interfaceString) {
        String localRuleString = "";
        if (this.permittedMAC == null && this.sourcePortRange == null) {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " --dport " + this.port + " -j ACCEPT";
        } else if (this.permittedMAC == null) {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " --sport " + this.sourcePortRange
                    + " --dport " + this.port + " -j ACCEPT";
        } else if (this.sourcePortRange == null) {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC
                    + " --dport " + this.port + " -j ACCEPT";
        } else {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC
                    + " --sport " + this.sourcePortRange + " --dport " + this.port + " -j ACCEPT";
        }
        return localRuleString;
    }

    private String getLocalRuleWithoutPort(String interfaceString) {
        String localRuleString = "";
        if (this.permittedMAC == null && this.sourcePortRange == null) {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " --dport " + this.portRange + " -j ACCEPT";
        } else if (this.permittedMAC == null) {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " --sport " + this.sourcePortRange
                    + " --dport " + this.portRange + " -j ACCEPT";
        } else if (this.sourcePortRange == null) {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC
                    + " --dport " + this.portRange + " -j ACCEPT";
        } else {
            localRuleString = "-A INPUT -p " + this.protocol + " -s " + this.permittedNetworkString
                    + (interfaceString != null ? interfaceString : "") + " -m mac --mac-source " + this.permittedMAC
                    + " --sport " + this.sourcePortRange + " --dport " + this.portRange + " -j ACCEPT";
        }
        return localRuleString;
    }

    private boolean isPortRangeValid(String range) {
        try {
            String[] rangeParts = range.split(":");
            if (rangeParts.length == 2) {
                int portStart = Integer.parseInt(rangeParts[0]);
                int portEnd = Integer.parseInt(rangeParts[1]);
                return portStart > 0 && portStart < 65535 && portEnd > 0 && portEnd < 65535 && portStart < portEnd
                        ? true : false;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LocalRule)) {
            return false;
        }

        LocalRule other = (LocalRule) o;

        return this.port == other.port && compareObjects(this.portRange, other.portRange)
                && compareObjects(this.protocol, other.protocol)
                && compareObjects(this.permittedMAC, other.permittedMAC)
                && compareObjects(this.sourcePortRange, other.sourcePortRange)
                && compareObjects(this.permittedInterfaceName, other.permittedInterfaceName)
                && compareObjects(this.unpermittedInterfaceName, other.unpermittedInterfaceName)
                && compareObjects(this.permittedNetworkString, other.permittedNetworkString);
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
        final int prime = 73;
        int result = 1;
        result = prime * result + this.port;
        result = prime * result + (this.portRange == null ? 0 : this.portRange.hashCode());
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.sourcePortRange == null ? 0 : this.sourcePortRange.hashCode());
        result = prime * result + (this.permittedInterfaceName == null ? 0 : this.permittedInterfaceName.hashCode());
        result = prime * result
                + (this.unpermittedInterfaceName == null ? 0 : this.unpermittedInterfaceName.hashCode());
        result = prime * result + (this.permittedMAC == null ? 0 : this.permittedMAC.hashCode());
        result = prime * result + (this.permittedNetworkString == null ? 0 : this.permittedNetworkString.hashCode());

        return result;
    }
}
