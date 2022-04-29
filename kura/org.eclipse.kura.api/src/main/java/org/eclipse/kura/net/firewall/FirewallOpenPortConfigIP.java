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
 ******************************************************************************/
package org.eclipse.kura.net.firewall;

import java.util.Objects;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The base class for firewall open port configurations
 *
 * @param <T>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class FirewallOpenPortConfigIP<T extends IPAddress> implements FirewallOpenPortConfig {

    /** The port to open for inbound connections **/
    private int port;

    /** Range of ports to open for inbound connections **/
    private String portRange;

    /** The type of protocol to allow for inbound connections **/
    private NetProtocol protocol;

    /** The (optional) permitted network for inbound connections **/
    private NetworkPair<T> permittedNetwork;

    /** The (optional) permitted interface name for inbound connections **/
    private String permittedInterfaceName;

    /** The (optional) not permitted interface name for inbound connections **/
    private String unpermittedInterfaceName;

    /** The (optional) permitted MAC address for inbound connections **/
    private String permittedMac;

    /** The (options) permitted source port range for inbound connections **/
    private String sourcePortRange;

    /**
     * Creates and empty open port configuration
     */
    public FirewallOpenPortConfigIP() {
        super();
    }

    /**
     * Creates a complete Open Port configuration
     *
     * @param port
     *            The port to open for inbound connections
     * @param protocol
     *            The type of protocol to allow for inbound connections
     * @param permittedNetwork
     *            The (optional) permitted network for inbound connections
     * @param permittedInterfaceName
     *            The (optional) permitted interface name for inbound connections
     * @param unpermittedInterfaceName
     *            The (optional) not permitted interface name for inbound connections
     * @param permittedMac
     *            The (optional) permitted MAC address for inbound connections
     * @param sourcePortRange
     *            The (options) permitted source port range for inbound connections
     */
    public FirewallOpenPortConfigIP(int port, NetProtocol protocol, NetworkPair<T> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMac,
            String sourcePortRange) {
        super();
        this.port = port;
        this.portRange = null;
        this.protocol = protocol;
        this.permittedNetwork = permittedNetwork;
        this.permittedInterfaceName = permittedInterfaceName;
        this.unpermittedInterfaceName = unpermittedInterfaceName;
        this.permittedMac = permittedMac;
        this.sourcePortRange = sourcePortRange;
    }

    public FirewallOpenPortConfigIP(String portRange, NetProtocol protocol, NetworkPair<T> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMac,
            String sourcePortRange) {
        super();
        this.portRange = portRange;
        this.port = -1;
        this.protocol = protocol;
        this.permittedNetwork = permittedNetwork;
        this.permittedInterfaceName = permittedInterfaceName;
        this.unpermittedInterfaceName = unpermittedInterfaceName;
        this.permittedMac = permittedMac;
        this.sourcePortRange = sourcePortRange;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getPortRange() {
        return this.portRange;
    }

    public void setPortRange(String portRange) {
        this.portRange = portRange;
    }

    @Override
    public NetProtocol getProtocol() {
        return this.protocol;
    }

    public void setProtocol(NetProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public NetworkPair<T> getPermittedNetwork() {
        return this.permittedNetwork;
    }

    public void setPermittedNetwork(NetworkPair<T> permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
    }

    public String getPermittedInterfaceName() {
        return this.permittedInterfaceName;
    }

    public void setPermittedInterfaceName(String permittedInterfaceName) {
        this.permittedInterfaceName = permittedInterfaceName;
    }

    public String getUnpermittedInterfaceName() {
        return this.unpermittedInterfaceName;
    }

    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
        this.unpermittedInterfaceName = unpermittedInterfaceName;
    }

    @Override
    public String getPermittedMac() {
        return this.permittedMac;
    }

    public void setPermittedMac(String permittedMac) {
        this.permittedMac = permittedMac;
    }

    @Override
    public String getSourcePortRange() {
        return this.sourcePortRange;
    }

    public void setSourcePortRange(String sourcePortRange) {
        this.sourcePortRange = sourcePortRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(permittedInterfaceName, permittedMac, permittedNetwork, port, portRange, protocol,
                sourcePortRange, unpermittedInterfaceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FirewallOpenPortConfigIP other = (FirewallOpenPortConfigIP) obj;
        return Objects.equals(permittedInterfaceName, other.permittedInterfaceName)
                && Objects.equals(permittedMac, other.permittedMac)
                && Objects.equals(permittedNetwork, other.permittedNetwork) && port == other.port
                && Objects.equals(portRange, other.portRange) && protocol == other.protocol
                && Objects.equals(sourcePortRange, other.sourcePortRange)
                && Objects.equals(unpermittedInterfaceName, other.unpermittedInterfaceName);
    }

    @Override
    public boolean isValid() {
        if (this.port < 0 || this.port > 65535) {
            return false;
        }

        if (this.protocol == null || !this.protocol.equals(NetProtocol.tcp) || !this.protocol.equals(NetProtocol.udp)) {
            return false;
        }

        // TODO - add checks for optional parameters to make sure if they are not null they are valid

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FirewallOpenPortConfigIP [port=");
        builder.append(this.port);
        builder.append(", portRange=");
        builder.append(this.portRange);
        builder.append(", protocol=");
        builder.append(this.protocol);
        builder.append(", permittedNetwork=");
        builder.append(this.permittedNetwork);
        builder.append(", permittedMac=");
        builder.append(this.permittedMac);
        builder.append(", sourcePortRange=");
        builder.append(this.sourcePortRange);
        builder.append("]");
        return builder.toString();
    }
}
