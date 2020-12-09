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
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.permittedInterfaceName == null ? 0 : this.permittedInterfaceName.hashCode());
        result = prime * result + (this.permittedMac == null ? 0 : this.permittedMac.hashCode());
        result = prime * result + (this.permittedNetwork == null ? 0 : this.permittedNetwork.hashCode());
        result = prime * result + this.port;
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.sourcePortRange == null ? 0 : this.sourcePortRange.hashCode());
        result = prime * result
                + (this.unpermittedInterfaceName == null ? 0 : this.unpermittedInterfaceName.hashCode());
        return result;
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
        if (this.permittedInterfaceName == null) {
            if (other.permittedInterfaceName != null) {
                return false;
            }
        } else if (!this.permittedInterfaceName.equals(other.permittedInterfaceName)) {
            return false;
        }
        if (this.permittedMac == null) {
            if (other.permittedMac != null) {
                return false;
            }
        } else if (!this.permittedMac.equals(other.permittedMac)) {
            return false;
        }
        if (this.permittedNetwork == null) {
            if (other.permittedNetwork != null) {
                return false;
            }
        } else if (!this.permittedNetwork.equals(other.permittedNetwork)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if (this.protocol != other.protocol) {
            return false;
        }
        if (this.sourcePortRange == null) {
            if (other.sourcePortRange != null) {
                return false;
            }
        } else if (!this.sourcePortRange.equals(other.sourcePortRange)) {
            return false;
        }
        if (this.unpermittedInterfaceName == null) {
            if (other.unpermittedInterfaceName != null) {
                return false;
            }
        } else if (!this.unpermittedInterfaceName.equals(other.unpermittedInterfaceName)) {
            return false;
        }
        return true;
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
