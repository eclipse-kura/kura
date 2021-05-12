/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.net.NetConfig;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a NAT configuration
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class FirewallNatConfig implements NetConfig {

    /** The source interface (WAN interface) **/
    private final String sourceInterface;

    /** The destination interface (LAN interface) **/
    private final String destinationInterface;

    /** protocol (i.e. all, tcp, udp) */
    private final String protocol;

    /** source network/host in CIDR notation */
    private final String source;

    /** destination network/host in CIDR notation */
    private final String destination;

    /** Whether or not MASQUERADE should be enabled **/
    private final boolean masquerade;

    /**
     * Represent the type of the rule
     * 
     * @since 3.0
     */
    private final RuleType type;

    /**
     * 
     * @deprecated since version 3.0. It will be removed in the next major release. Use instead
     *             {@link #FirewallNatConfig(String, String, String, String, String, boolean, RuleType)}
     */
    @Deprecated
    public FirewallNatConfig(String srcIface, String dstIface, String protocol, String src, String dst,
            boolean masquerade) {
        this.sourceInterface = srcIface;
        this.destinationInterface = dstIface;
        this.protocol = protocol;
        this.source = src;
        this.destination = dst;
        this.masquerade = masquerade;
        this.type = RuleType.GENERIC;
    }

    /**
     * 
     * Create a configuration for a NAT rule
     * 
     * @param srcIface
     *            the source network interface (WAN interface)
     * @param dstIface
     *            the destination network interface (LAN interface)
     * @param protocol
     *            the network protocol (i.e. tcp, udp)
     * @param src
     *            the source network/host address in CIDR notation
     * @param dst
     *            the destination network/host address in CIDR notation
     * @param masquerade
     *            whether or not MASQUERADE should be enabled
     * @param type
     *            the type of the rule (IP forwarding, Port forwarding or generic)
     * 
     * @since 3.0
     */
    public FirewallNatConfig(String srcIface, String dstIface, String protocol, String src, String dst,
            boolean masquerade, RuleType type) {
        this.sourceInterface = srcIface;
        this.destinationInterface = dstIface;
        this.protocol = protocol;
        this.source = src;
        this.destination = dst;
        this.masquerade = masquerade;
        this.type = type;
    }

    public String getSourceInterface() {
        return this.sourceInterface;
    }

    public String getDestinationInterface() {
        return this.destinationInterface;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getSource() {
        return this.source;
    }

    public String getDestination() {
        return this.destination;
    }

    public boolean isMasquerade() {
        return this.masquerade;
    }

    /**
     * 
     * @since 3.0
     */
    public RuleType getRuleType() {
        return this.type;
    }

    @Override
    public boolean isValid() {
        boolean result = false;
        if (this.destinationInterface != null && !this.destinationInterface.trim().isEmpty()
                && this.sourceInterface != null && !this.sourceInterface.trim().isEmpty()) {
            result = true;
        }

        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.destinationInterface == null ? 0 : this.destinationInterface.hashCode());
        result = prime * result + (this.sourceInterface == null ? 0 : this.sourceInterface.hashCode());
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.source == null ? 0 : this.source.hashCode());
        result = prime * result + (this.destination == null ? 0 : this.destination.hashCode());
        result = prime * result + (this.masquerade ? 1231 : 1237);
        result = prime * result + (this.type == null ? 0 : this.type.hashCode());

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
        FirewallNatConfig other = (FirewallNatConfig) obj;

        if (this.masquerade != other.masquerade) {
            return false;
        }

        if (this.sourceInterface == null) {
            if (other.sourceInterface != null) {
                return false;
            }
        } else if (!this.sourceInterface.equals(other.sourceInterface)) {
            return false;
        } else if (!this.protocol.equals(other.protocol)) {
            return false;
        }

        if (this.destinationInterface == null) {
            if (other.destinationInterface != null) {
                return false;
            }
        } else if (!this.destinationInterface.equals(other.destinationInterface)) {
            return false;
        }

        if (this.source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!this.source.equals(other.source)) {
            return false;
        }

        if (this.destination == null) {
            if (other.destination != null) {
                return false;
            }
        } else if (!this.destination.equals(other.destination)) {
            return false;
        }

        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FirewallNatConfig [m_sourceInterface=");
        builder.append(this.sourceInterface);
        builder.append(", destinationInterface=");
        builder.append(this.destinationInterface);
        builder.append(", source=");
        builder.append(this.source);
        builder.append(", destination=");
        builder.append(this.destination);
        builder.append(", type=");
        builder.append(this.type);
        builder.append("]");
        return builder.toString();
    }
}
