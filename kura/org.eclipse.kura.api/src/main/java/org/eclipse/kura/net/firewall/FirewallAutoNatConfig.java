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
    private String sourceInterface;

    /** The destination interface (WAN interface) for the NAT configuration **/
    private String destinationInterface;

    /** Whether or not MASQUERADE should be enabled **/
    private boolean masquerade;

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
        this.sourceInterface = sourceInterface;
        this.destinationInterface = destinationInterface;
        this.masquerade = masquerade;
    }

    public String getSourceInterface() {
        return this.sourceInterface;
    }

    public void setSourceInterface(String sourceInterface) {
        this.sourceInterface = sourceInterface;
    }

    public String getDestinationInterface() {
        return this.destinationInterface;
    }

    public void setDestinationInterface(String destinationInterface) {
        this.destinationInterface = destinationInterface;
    }

    public boolean isMasquerade() {
        return this.masquerade;
    }

    public void setMasquerade(boolean masquerade) {
        this.masquerade = masquerade;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.destinationInterface == null ? 0 : this.destinationInterface.hashCode());
        result = prime * result + (this.masquerade ? 1231 : 1237);
        result = prime * result + (this.sourceInterface == null ? 0 : this.sourceInterface.hashCode());
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

        if (this.masquerade != other.masquerade) {
            return false;
        }
        if (this.sourceInterface == null) {
            if (other.sourceInterface != null) {
                return false;
            }
        } else if (!this.sourceInterface.equals(other.sourceInterface)) {
            return false;
        }

        return true;
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
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FirewallNatConfig [m_sourceInterface=");
        builder.append(this.sourceInterface);
        builder.append(", m_destinationInterface=");
        builder.append(this.destinationInterface);
        builder.append(", m_masquerade=");
        builder.append(this.masquerade);
        builder.append("]");
        return builder.toString();
    }
}
