/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
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
    private final String m_sourceInterface;

    /** The destination interface (LAN interface) **/
    private final String m_destinationInterface;

    /** protocol (i.e. all, tcp, udp) */
    private final String m_protocol;

    /** source network/host in CIDR notation */
    private final String m_source;

    /** destination network/host in CIDR notation */
    private final String m_destination;

    /** Whether or not MASQUERADE should be enabled **/
    private final boolean m_masquerade;

    public FirewallNatConfig(String srcIface, String dstIface, String protocol, String src, String dst,
            boolean masquerade) {
        this.m_sourceInterface = srcIface;
        this.m_destinationInterface = dstIface;
        this.m_protocol = protocol;
        this.m_source = src;
        this.m_destination = dst;
        this.m_masquerade = masquerade;
    }

    public String getSourceInterface() {
        return this.m_sourceInterface;
    }

    public String getDestinationInterface() {
        return this.m_destinationInterface;
    }

    public String getProtocol() {
        return this.m_protocol;
    }

    public String getSource() {
        return this.m_source;
    }

    public String getDestination() {
        return this.m_destination;
    }

    public boolean isMasquerade() {
        return this.m_masquerade;
    }

    @Override
    public boolean isValid() {
        if (this.m_destinationInterface != null && !this.m_destinationInterface.trim().isEmpty()
                && this.m_sourceInterface != null && !this.m_sourceInterface.trim().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_destinationInterface == null ? 0 : this.m_destinationInterface.hashCode());

        result = prime * result + (this.m_sourceInterface == null ? 0 : this.m_sourceInterface.hashCode());

        result = prime * result + (this.m_protocol == null ? 0 : this.m_protocol.hashCode());

        result = prime * result + (this.m_source == null ? 0 : this.m_source.hashCode());

        result = prime * result + (this.m_destination == null ? 0 : this.m_destination.hashCode());

        result = prime * result + (this.m_masquerade ? 1231 : 1237);

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

        if (this.m_masquerade != other.m_masquerade) {
            return false;
        }

        if (this.m_sourceInterface == null) {
            if (other.m_sourceInterface != null) {
                return false;
            }
        } else if (!this.m_sourceInterface.equals(other.m_sourceInterface)) {
            return false;
        } else if (!this.m_protocol.equals(other.m_protocol)) {
            return false;
        }

        if (this.m_destinationInterface == null) {
            if (other.m_destinationInterface != null) {
                return false;
            }
        } else if (!this.m_destinationInterface.equals(other.m_destinationInterface)) {
            return false;
        }

        if (this.m_source == null) {
            if (other.m_source != null) {
                return false;
            }
        } else if (!this.m_source.equals(other.m_source)) {
            return false;
        }

        if (this.m_destination == null) {
            if (other.m_destination != null) {
                return false;
            }
        } else if (!this.m_destination.equals(other.m_destination)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FirewallNatConfig [m_sourceInterface=");
        builder.append(this.m_sourceInterface);
        builder.append(", m_destinationInterface=");
        builder.append(this.m_destinationInterface);
        builder.append(", m_source=");
        builder.append(this.m_source);
        builder.append(", m_destination=");
        builder.append(this.m_destination);
        builder.append("]");
        return builder.toString();
    }
}
