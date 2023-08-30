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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.linux.net.iptables;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;

/**
 * Creates an iptables command for a Local Rule, allowing an incoming port connection.
 *
 */
public class LocalRule {

    private static final String M_MAC_MAC_SOURCE = " -m mac --mac-source ";
    private static final String J_ACCEPT = " -j ACCEPT";
    private static final String SPORT = " --sport ";
    private static final String DPORT = " --dport ";
    private static final String A_INPUT_KURA_P = "-A input-kura -p ";

    private int port;
    private Optional<String> portRange = Optional.empty();
    private String protocol;
    private Optional<String> permittedNetworkString = Optional.empty();
    private Optional<String> permittedInterfaceName = Optional.empty();
    private Optional<String> unpermittedInterfaceName = Optional.empty();
    private Optional<String> permittedMAC = Optional.empty();
    private Optional<String> sourcePortRange = Optional.empty();

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
    public LocalRule(int port, String protocol, NetworkPair<? extends IPAddress> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC,
            String sourcePortRange) {
        this.port = port;
        this.portRange = Optional.empty();
        this.protocol = protocol;
        if (permittedNetwork != null) {
            this.permittedNetworkString = Optional
                    .of(permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix());
        }
        if (permittedInterfaceName != null && !permittedInterfaceName.trim().isEmpty()) {
            this.permittedInterfaceName = Optional.of(permittedInterfaceName);
        }
        if (unpermittedInterfaceName != null && !unpermittedInterfaceName.trim().isEmpty()) {
            this.unpermittedInterfaceName = Optional.of(unpermittedInterfaceName);
        }
        if (permittedMAC != null && !permittedMAC.trim().isEmpty()) {
            this.permittedMAC = Optional.of(permittedMAC);
        }
        if (sourcePortRange != null && !sourcePortRange.trim().isEmpty()) {
            this.sourcePortRange = Optional.of(sourcePortRange);
        }
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
    public LocalRule(String portRange, String protocol, NetworkPair<? extends IPAddress> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC,
            String sourcePortRange) {
        this.port = -1;
        if (portRange != null && !portRange.trim().isEmpty()) {
            this.portRange = Optional.of(portRange);
        }
        this.protocol = protocol;
        if (permittedNetwork != null) {
            this.permittedNetworkString = Optional
                    .of(permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix());
        }
        if (permittedInterfaceName != null && !permittedInterfaceName.trim().isEmpty()) {
            this.permittedInterfaceName = Optional.of(permittedInterfaceName);
        }
        if (unpermittedInterfaceName != null && !unpermittedInterfaceName.trim().isEmpty()) {
            this.unpermittedInterfaceName = Optional.of(unpermittedInterfaceName);
        }
        if (permittedMAC != null && !permittedMAC.trim().isEmpty()) {
            this.permittedMAC = Optional.of(permittedMAC);
        }
        if (sourcePortRange != null && !sourcePortRange.trim().isEmpty()) {
            this.sourcePortRange = Optional.of(sourcePortRange);
        }
    }

    /**
     * Constructor of <code>LocalRule</code> object.
     */
    public LocalRule() {
        this.port = -1;
        this.protocol = null;
    }

    public LocalRule(String rule) throws KuraException {
        try {
            String[] aRuleTokens = rule.split(" ");
            for (int i = 0; i < aRuleTokens.length; i++) {
                if ("-i".equals(aRuleTokens[i])) {
                    if ("!".equals(aRuleTokens[i - 1])) {
                        this.unpermittedInterfaceName = Optional.of(aRuleTokens[++i]);
                    } else {
                        this.permittedInterfaceName = Optional.of(aRuleTokens[++i]);
                    }
                } else if ("-s".equals(aRuleTokens[i])) {
                    this.permittedNetworkString = Optional.of(aRuleTokens[++i]);
                } else if ("-p".equals(aRuleTokens[i])) {
                    this.protocol = aRuleTokens[++i];
                } else if ("--dport".equals(aRuleTokens[i])) {
                    if (aRuleTokens[i + 1].indexOf(':') > 0) {
                        this.portRange = Optional.of(aRuleTokens[++i]);
                        this.port = -1;
                    } else {
                        this.port = Integer.parseInt(aRuleTokens[++i]);
                        this.portRange = Optional.empty();
                    }
                } else if ("--sport".equals(aRuleTokens[i])) {
                    this.sourcePortRange = Optional.of(aRuleTokens[++i]);
                } else if ("--mac-source".equals(aRuleTokens[i])) {
                    this.permittedMAC = Optional.of(aRuleTokens[++i]);
                }
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
        } else if (this.protocol != null && this.portRange.isPresent()) {
            return isPortRangeValid(this.portRange.get());
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
    public void setPermittedNetwork(NetworkPair<? extends IPAddress> permittedNetwork) {
        if (permittedNetwork != null) {
            this.permittedNetworkString = Optional
                    .of(permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix());
        }
    }

    /**
     * Setter for the permittedInterfaceName.
     *
     * @param permittedInterfaceName
     *            A String representing the only interface allowed on this open port
     */
    public void setPermittedInterfaceName(String permittedInterfaceName) {
        if (permittedInterfaceName != null && !permittedInterfaceName.trim().isEmpty()) {
            this.permittedInterfaceName = Optional.of(permittedInterfaceName);
        }
    }

    /**
     * Setter for the unpermittedInterfaceName.
     *
     * @param unpermittedInterfaceName
     *            A String representing the only interface not allowed on this open port
     */
    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
        if (unpermittedInterfaceName != null && !unpermittedInterfaceName.trim().isEmpty()) {
            this.unpermittedInterfaceName = Optional.of(unpermittedInterfaceName);
        }
    }

    /**
     * Setter for the permittedMAC.
     *
     * @param permittedMAC
     *            A String representing the permittedMAC.
     */
    public void setPermittedMAC(String permittedMAC) {
        if (permittedMAC != null && !permittedMAC.trim().isEmpty()) {
            this.permittedMAC = Optional.of(permittedMAC);
        }
    }

    /**
     * Setter for the sourcePortRange.
     *
     * @param sourcePortRange
     *            A String representing the sourcePortRange.
     */
    public void setSourcePortRange(String sourcePortRange) {
        if (sourcePortRange != null && !sourcePortRange.trim().isEmpty()) {
            this.sourcePortRange = Optional.of(sourcePortRange);
        }
    }

    /**
     * Setter for the port.
     *
     * @param port
     *            An int representing the port.
     */
    public void setPort(int port) {
        this.port = port;
        this.portRange = Optional.empty();
    }

    /**
     * Setter for the portRange
     *
     * @param portRange
     *            A string representing the port range of the form X:Y where X < Y and both are valid ports
     */
    public void setPortRange(String portRange) {
        this.port = -1;
        if (portRange != null && !portRange.trim().isEmpty()) {
            this.portRange = Optional.of(portRange);
        }
    }

    /**
     * Getter for the sourcePortRange.
     *
     * @return the sourcePortRange
     */
    public String getSourcePortRange() {
        if (this.sourcePortRange.isPresent()) {
            return this.sourcePortRange.get();
        } else {
            return null;
        }
    }

    /**
     * Getter for the permittedInterfaceName.
     *
     * @param permittedInterfaceName
     *            A String representing the only interface allowed on this open port
     */
    public String getPermittedInterfaceName() {
        if (this.permittedInterfaceName.isPresent()) {
            return this.permittedInterfaceName.get();
        } else {
            return null;
        }
    }

    /**
     * Getter for the unpermittedInterfaceName.
     *
     * @param unpermittedInterfaceName
     *            A String representing the only interface not allowed on this open port
     */
    public String getUnpermittedInterfaceName() {
        if (this.unpermittedInterfaceName.isPresent()) {
            return this.unpermittedInterfaceName.get();
        } else {
            return null;
        }
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
        if (this.portRange.isPresent()) {
            return this.portRange.get();
        } else {
            return null;
        }
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
    public NetworkPair<? extends IPAddress> getPermittedNetwork() throws KuraException {
        NetworkPair<? extends IPAddress> permittedNetwork = null;
        try {
            if (this.permittedNetworkString.isPresent()) {
                String[] split = this.permittedNetworkString.get().split("/");
                permittedNetwork = new NetworkPair<>(IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return permittedNetwork;
    }

    public String getPermittedNetworkString() {
        if (this.permittedNetworkString.isPresent()) {
            return this.permittedNetworkString.get();
        } else {
            return null;
        }
    }

    /**
     * Getter for permittedMAC
     *
     * @return the permittedMAC
     */
    public String getPermittedMAC() {
        if (this.permittedMAC.isPresent()) {
            return this.permittedMAC.get();
        } else {
            return null;
        }
    }

    /**
     * Converts the <code>LocalRule</code> to a <code>String</code>.
     * Returns one of the following iptables strings depending on the <code>LocalRule</code> format:
     * <code>
     * <p>  -A input-kura -p {protocol} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} -m mac --mac-source {permittedMAC} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} -m mac --mac-source {permittedMAC} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} -s {permittedNetwork} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} -s {permittedNetwork} --sport {sourcePort1:sourcePort2} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --dport {port} -j ACCEPT
     * <p>  -A input-kura -p {protocol} -s {permittedNetwork} -m mac --mac-source {permittedMAC} --sport {sourcePort1:sourcePort2} 
     *       --dport {port} -j ACCEPT
     * </code>
     */
    @Override
    public String toString() {
        String interfaceString = null;
        if (this.permittedInterfaceName.isPresent()) {
            interfaceString = new StringBuilder().append(" -i ").append(this.permittedInterfaceName.get()).toString();
        } else if (this.unpermittedInterfaceName.isPresent()) {
            interfaceString = new StringBuilder().append(" ! -i ").append(this.unpermittedInterfaceName.get())
                    .toString();
        }

        if (this.port != -1) {
            return getLocalRuleWithPort(interfaceString);
        } else {
            return getLocalRuleWithoutPort(interfaceString);
        }
    }

    private String getLocalRuleWithPort(String interfaceString) {
        String localRuleString = "";
        if (!this.permittedMAC.isPresent() && !this.sourcePortRange.isPresent()) {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + DPORT + this.port + J_ACCEPT;
        } else if (!this.permittedMAC.isPresent()) {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + SPORT + this.sourcePortRange.get() + DPORT
                    + this.port + J_ACCEPT;
        } else if (!this.sourcePortRange.isPresent()) {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + M_MAC_MAC_SOURCE + this.permittedMAC.get()
                    + DPORT + this.port + J_ACCEPT;
        } else {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + M_MAC_MAC_SOURCE + this.permittedMAC.get()
                    + SPORT + this.sourcePortRange.get() + DPORT + this.port + J_ACCEPT;
        }
        return localRuleString;
    }

    private String getLocalRuleWithoutPort(String interfaceString) {
        String localRuleString = "";
        if (!this.permittedMAC.isPresent() && !this.sourcePortRange.isPresent()) {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + DPORT + this.portRange.get() + J_ACCEPT;
        } else if (!this.permittedMAC.isPresent()) {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + SPORT + this.sourcePortRange.get() + DPORT
                    + this.portRange.get() + J_ACCEPT;
        } else if (!this.sourcePortRange.isPresent()) {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + M_MAC_MAC_SOURCE + this.permittedMAC.get()
                    + DPORT + this.portRange.get() + J_ACCEPT;
        } else {
            localRuleString = A_INPUT_KURA_P + this.protocol + " -s " + this.permittedNetworkString.get()
                    + (interfaceString != null ? interfaceString : "") + M_MAC_MAC_SOURCE + this.permittedMAC.get()
                    + SPORT + this.sourcePortRange.get() + DPORT + this.portRange.get() + J_ACCEPT;
        }
        return localRuleString;
    }

    private boolean isPortRangeValid(String range) {
        try {
            String[] rangeParts = range.split(":");
            if (rangeParts.length == 2) {
                int portStart = Integer.parseInt(rangeParts[0]);
                int portEnd = Integer.parseInt(rangeParts[1]);
                return portStart > 0 && portStart < 65535 && portEnd > 0 && portEnd < 65535 && portStart < portEnd;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LocalRule other = (LocalRule) obj;
        return this.port == other.port && Objects.equals(this.portRange, other.portRange)
                && Objects.equals(this.protocol, other.protocol)
                && Objects.equals(this.permittedNetworkString, other.permittedNetworkString)
                && Objects.equals(this.permittedInterfaceName, other.permittedInterfaceName)
                && Objects.equals(this.unpermittedInterfaceName, other.unpermittedInterfaceName)
                && Objects.equals(this.permittedMAC, other.permittedMAC)
                && Objects.equals(this.sourcePortRange, other.sourcePortRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.port, this.portRange, this.protocol, this.permittedNetworkString,
                this.permittedInterfaceName, this.unpermittedInterfaceName, this.permittedMAC, this.sourcePortRange);
    }

}
