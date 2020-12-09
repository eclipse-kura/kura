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

    @SuppressWarnings("checkstyle:parameterNumber")
    public FirewallPortForwardConfigIP4(String inboundIface, String outboundIface, IP4Address address,
            NetProtocol protocol, int inPort, int outPort, boolean masquerade, NetworkPair<IP4Address> permittedNetwork,
            String permittedMac, String sourcePortRange) {
        super(inboundIface, outboundIface, address, protocol, inPort, outPort, masquerade, permittedNetwork,
                permittedMac, sourcePortRange);
    }
}
