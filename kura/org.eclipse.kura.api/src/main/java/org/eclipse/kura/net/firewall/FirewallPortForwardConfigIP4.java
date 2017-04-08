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

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class FirewallPortForwardConfigIP4 extends FirewallPortForwardConfigIP<IP4Address>
        implements FirewallPortForwardConfig4 {

    public FirewallPortForwardConfigIP4() {
        super();
    }

    public FirewallPortForwardConfigIP4(String inboundIface, String outboundIface, IP4Address address,
            NetProtocol protocol, int inPort, int outPort, boolean masquerade, NetworkPair<IP4Address> permittedNetwork,
            String permittedMac, String sourcePortRange) {
        super(inboundIface, outboundIface, address, protocol, inPort, outPort, masquerade, permittedNetwork,
                permittedMac, sourcePortRange);
    }
}
