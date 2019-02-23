/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
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

    private Set<T> m_forwarders;
    private Set<NetworkPair<T>> m_allowedNetworks;

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

        this.m_forwarders = forwarders;
        this.m_allowedNetworks = allowedNetworks;
    }

    /**
     * Gets the current recursive domain name servers to use to resolve queries
     */
    @Override
    public Set<T> getForwarders() {
        return this.m_forwarders;
    }

    /**
     * Sets the current recursive domain name servers to use to resolve queries
     *
     * @param forwarders
     *            The recursive DNS servers to use
     */
    public void setForwarders(Set<T> forwarders) {
        this.m_forwarders = forwarders;
    }

    /**
     * Gets a List of networks that are allowed to make DNS queries
     */
    @Override
    public Set<NetworkPair<T>> getAllowedNetworks() {
        return this.m_allowedNetworks;
    }

    /**
     * Sets a List of networks that are allowed to make DNS queries
     *
     * @param allowedNetworks
     *            The LAN networks that are allowed to make queries
     */
    public void setAllowedNetworks(Set<NetworkPair<T>> allowedNetworks) {
        this.m_allowedNetworks = allowedNetworks;
    }

    @Override
    public String toString() {
        return "DnsServerConfigIP [m_forwarders=" + this.m_forwarders + ", m_allowedNetworks=" + this.m_allowedNetworks
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.m_allowedNetworks == null ? 0 : this.m_allowedNetworks.hashCode());
        result = prime * result + (this.m_forwarders == null ? 0 : this.m_forwarders.hashCode());
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
        if (this.m_allowedNetworks == null) {
            if (other.m_allowedNetworks != null) {
                return false;
            }
        } else if (!this.m_allowedNetworks.equals(other.m_allowedNetworks)) {
            return false;
        }
        if (this.m_forwarders == null) {
            if (other.m_forwarders != null) {
                return false;
            }
        } else if (!this.m_forwarders.equals(other.m_forwarders)) {
            return false;
        }
        return true;
    }
}
