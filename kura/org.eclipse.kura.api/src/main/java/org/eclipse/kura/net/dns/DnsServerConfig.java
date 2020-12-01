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
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The placeholder for all DNS proxy servers
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface DnsServerConfig extends NetConfig {

    /**
     * returns the DNS forwarders associated with this DnsServerConfig
     *
     * @return a {@link java.util.List } of DNS forwarders
     */
    public Set<? extends IPAddress> getForwarders();

    /**
     * returns the allowed networks for resolving DNS queries
     *
     * @return a {@link java.util.List } of {@link NetworkPair } representing the networks that are allowed to
     *         perform DNS queries
     */
    public Set<? extends NetworkPair<? extends IPAddress>> getAllowedNetworks();
}
