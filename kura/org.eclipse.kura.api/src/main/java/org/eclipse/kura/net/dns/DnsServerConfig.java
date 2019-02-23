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
package org.eclipse.kura.net.dns;

import java.util.List;
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
     * @return a {@link List } of DNS forwarders
     */
    public Set<? extends IPAddress> getForwarders();

    /**
     * returns the allowed networks for resolving DNS queries
     *
     * @return a {@link List } of {@link NetworkPair } representing the networks that are allowed to
     *         perform DNS queries
     */
    public Set<? extends NetworkPair<? extends IPAddress>> getAllowedNetworks();
}
