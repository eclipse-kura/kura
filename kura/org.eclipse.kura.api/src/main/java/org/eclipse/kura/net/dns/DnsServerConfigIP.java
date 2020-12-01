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
package org.eclipse.kura.net.dns;

import java.util.Set;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Base class for DNS proxy configurations
 *
 * @param <T>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public abstract class DnsServerConfigIP<T extends IPAddress> implements DnsServerConfig {

    private Set<T> forwarders;
    private Set<NetworkPair<T>> allowedNetworks;

    /**
     * Creates a DNS configuration with a default set of forwarders and a set of allowed networks
     *
     * @param forwarders
     *            The recursive DNS servers to use
     * @param allowedNetworks
     *            The LAN networks that are allowed to make queries
     */
    public DnsServerConfigIP(Set<T> forwarders, Set<NetworkPair<T>> allowedNetworks) {
        super();

        this.forwarders = forwarders;
        this.allowedNetworks = allowedNetworks;
    }

    /**
     * Gets the current recursive domain name servers to use to resolve queries
     */
    @Override
    public Set<T> getForwarders() {
        return this.forwarders;
    }

    /**
     * Sets the current recursive domain name servers to use to resolve queries
     *
     * @param forwarders
     *            The recursive DNS servers to use
     */
    public void setForwarders(Set<T> forwarders) {
        this.forwarders = forwarders;
    }

    /**
     * Gets a List of networks that are allowed to make DNS queries
     */
    @Override
    public Set<NetworkPair<T>> getAllowedNetworks() {
        return this.allowedNetworks;
    }

    /**
     * Sets a List of networks that are allowed to make DNS queries
     *
     * @param allowedNetworks
     *            The LAN networks that are allowed to make queries
     */
    public void setAllowedNetworks(Set<NetworkPair<T>> allowedNetworks) {
        this.allowedNetworks = allowedNetworks;
    }

    @Override
    public String toString() {
        return "DnsServerConfigIP [m_forwarders=" + this.forwarders + ", m_allowedNetworks=" + this.allowedNetworks
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.allowedNetworks == null ? 0 : this.allowedNetworks.hashCode());
        result = prime * result + (this.forwarders == null ? 0 : this.forwarders.hashCode());
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
        DnsServerConfigIP<?> other = (DnsServerConfigIP<?>) obj;
        if (this.allowedNetworks == null) {
            if (other.allowedNetworks != null) {
                return false;
            }
        } else if (!this.allowedNetworks.equals(other.allowedNetworks)) {
            return false;
        }
        if (this.forwarders == null) {
            if (other.forwarders != null) {
                return false;
            }
        } else if (!this.forwarders.equals(other.forwarders)) {
            return false;
        }
        return true;
    }
}
