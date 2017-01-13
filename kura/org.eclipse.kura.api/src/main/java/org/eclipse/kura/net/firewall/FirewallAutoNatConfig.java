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
 * Represents an automatic NAT configuration
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class FirewallAutoNatConfig implements NetConfig {

    /** The source interface (LAN interface) for the NAT configuration **/
    private String m_sourceInterface;

    /** The destination interface (WAN interface) for the NAT configuration **/
    private String m_destinationInterface;

    /** Whether or not MASQUERADE should be enabled **/
    private boolean m_masquerade;

    /**
     * Creates a null NAT configuration
     */
    public FirewallAutoNatConfig() {
        super();
    }

    /**
     * Creates a complete auto NAT configuration
     *
     * @param sourceInterface
     *            The source interface (LAN interface) for the NAT configuration
     * @param destinationInterface
     *            The destination interface (WAN interface) for the NAT configuration
     * @param masquerade
     *            Whether or not MASQUERADE should be enabled
     */
    public FirewallAutoNatConfig(String sourceInterface, String destinationInterface, boolean masquerade) {
        super();
        this.m_sourceInterface = sourceInterface;
        this.m_destinationInterface = destinationInterface;
        this.m_masquerade = masquerade;
    }

    public String getSourceInterface() {
        return this.m_sourceInterface;
    }

    public void setSourceInterface(String sourceInterface) {
        this.m_sourceInterface = sourceInterface;
    }

    public String getDestinationInterface() {
        return this.m_destinationInterface;
    }

    public void setDestinationInterface(String destinationInterface) {
        this.m_destinationInterface = destinationInterface;
    }

    public boolean isMasquerade() {
        return this.m_masquerade;
    }

    public void setMasquerade(boolean masquerade) {
        this.m_masquerade = masquerade;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_destinationInterface == null ? 0 : this.m_destinationInterface.hashCode());
        result = prime * result + (this.m_masquerade ? 1231 : 1237);
        result = prime * result + (this.m_sourceInterface == null ? 0 : this.m_sourceInterface.hashCode());
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
        FirewallAutoNatConfig other = (FirewallAutoNatConfig) obj;
        /*
         * if (m_destinationInterface == null) {
         * if (other.m_destinationInterface != null)
         * return false;
         * } else if (!m_destinationInterface.equals(other.m_destinationInterface))
         * return false;
         */
        if (this.m_masquerade != other.m_masquerade) {
            return false;
        }
        if (this.m_sourceInterface == null) {
            if (other.m_sourceInterface != null) {
                return false;
            }
        } else if (!this.m_sourceInterface.equals(other.m_sourceInterface)) {
            return false;
        }

        return true;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FirewallNatConfig [m_sourceInterface=");
        builder.append(this.m_sourceInterface);
        builder.append(", m_destinationInterface=");
        builder.append(this.m_destinationInterface);
        builder.append(", m_masquerade=");
        builder.append(this.m_masquerade);
        builder.append("]");
        return builder.toString();
    }
}
