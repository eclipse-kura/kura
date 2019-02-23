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

import java.util.Set;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * DNS server configurations for IPv4 networks
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class DnsServerConfigIP4 extends DnsServerConfigIP<IP4Address>implements DnsServerConfig4 {

    public DnsServerConfigIP4(Set<IP4Address> forwarders, Set<NetworkPair<IP4Address>> allowedNetworks) {
        super(forwarders, allowedNetworks);
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }
}
