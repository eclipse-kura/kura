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

import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * DNS server configurations for IPv6 networks
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class DnsServerConfigIP6 extends DnsServerConfigIP<IP6Address>implements DnsServerConfig6 {

    public DnsServerConfigIP6(Set<IP6Address> forwarders, Set<NetworkPair<IP6Address>> allowedNetworks) {
        super(forwarders, allowedNetworks);
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }
}
