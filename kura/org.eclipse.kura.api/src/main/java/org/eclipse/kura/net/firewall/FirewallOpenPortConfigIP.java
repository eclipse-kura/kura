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
 ******************************************************************************/
package org.eclipse.kura.net.firewall;

import java.net.UnknownHostException;
import java.util.Objects;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The base class for firewall open port configurations
 *
 * @param <T>
 *            the type of IPAddess
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
     * 
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public FirewallOpenPortConfigIP() {
        super();
    }

    /**
     * Creates a complete Open Port configuration
     *
     * @param portRange
     *            The range of ports to open for inbound connections
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
     *            The (optional) permitted source port range for inbound connections
     * 
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
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
     * 
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
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

    protected FirewallOpenPortConfigIP(FirewallOpenPortConfigIPBuilder<T, ?> builder) {
        this.portRange = builder.portRange;
        this.port = builder.port;
        this.protocol = builder.protocol;
        this.permittedNetwork = builder.permittedNetwork;
        this.permittedInterfaceName = builder.permittedInterfaceName;
        this.unpermittedInterfaceName = builder.unpermittedInterfaceName;
        this.permittedMac = builder.permittedMac;
        this.sourcePortRange = builder.sourcePortRange;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getPortRange() {
        return this.portRange;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setPortRange(String portRange) {
        this.portRange = portRange;
    }

    @Override
    public NetProtocol getProtocol() {
        return this.protocol;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setProtocol(NetProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public NetworkPair<T> getPermittedNetwork() {
        return this.permittedNetwork;
    }

    /**
     * @since 2.6
     */
    @Override
    public String getPermittedNetworkString() {
        return this.permittedNetwork.getIpAddress().getHostAddress() + "/" + this.permittedNetwork.getPrefix();
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setPermittedNetwork(NetworkPair<T> permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
    }

    public String getPermittedInterfaceName() {
        return this.permittedInterfaceName;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setPermittedInterfaceName(String permittedInterfaceName) {
        this.permittedInterfaceName = permittedInterfaceName;
    }

    public String getUnpermittedInterfaceName() {
        return this.unpermittedInterfaceName;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
        this.unpermittedInterfaceName = unpermittedInterfaceName;
    }

    @Override
    public String getPermittedMac() {
        return this.permittedMac;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setPermittedMac(String permittedMac) {
        this.permittedMac = permittedMac;
    }

    @Override
    public String getSourcePortRange() {
        return this.sourcePortRange;
    }

    /**
     * @deprecated since 2.6. Use the FirewallOpenPortConfigIP builder
     */
    @Deprecated
    public void setSourcePortRange(String sourcePortRange) {
        this.sourcePortRange = sourcePortRange;
    }

    /**
     * The base builder class for firewall open port configurations
     * 
     * @since 2.6
     */
    @ProviderType
    public abstract static class FirewallOpenPortConfigIPBuilder<U extends IPAddress, T extends FirewallOpenPortConfigIPBuilder<U, T>> {

        protected int port = -1;
        protected String portRange;
        protected NetProtocol protocol;
        protected NetworkPair<U> permittedNetwork;
        protected String permittedInterfaceName;
        protected String unpermittedInterfaceName;
        protected String permittedMac;
        protected String sourcePortRange;

        public T withPort(int port) {
            this.port = port;
            return getThis();
        }

        public T withPortRange(String portRange) {
            this.portRange = portRange;
            return getThis();
        }

        public T withProtocol(NetProtocol protocol) {
            this.protocol = protocol;
            return getThis();
        }

        public T withPermittedNetwork(NetworkPair<U> permittedNetwork) {
            this.permittedNetwork = permittedNetwork;
            return getThis();
        }

        public T withPermittedInterfaceName(String permittedInterfaceName) {
            this.permittedInterfaceName = permittedInterfaceName;
            return getThis();
        }

        public T withUnpermittedInterfaceName(String unpermittedInterfaceName) {
            this.unpermittedInterfaceName = unpermittedInterfaceName;
            return getThis();
        }

        public T withPermittedMac(String permittedMac) {
            this.permittedMac = permittedMac;
            return getThis();
        }

        public T withSourcePortRange(String sourcePortRange) {
            this.sourcePortRange = sourcePortRange;
            return getThis();
        }

        public abstract T getThis();

        public abstract FirewallOpenPortConfigIP<U> build() throws UnknownHostException;
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
        FirewallOpenPortConfigIP<?> other = (FirewallOpenPortConfigIP<?>) obj;
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
