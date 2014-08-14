/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.net.firewall;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;

public class FirewallPortForwardConfigIP4 extends FirewallPortForwardConfigIP<IP4Address> implements FirewallPortForwardConfig4 {
	
	public FirewallPortForwardConfigIP4() {
		super();
	}
	
	public FirewallPortForwardConfigIP4(String interfaceName, IP4Address address,
			NetProtocol protocol, int inPort, int outPort,
			NetworkPair<IP4Address> permittedNetwork,
			String permittedMac, String sourcePortRange) {
		super(interfaceName, address, protocol, inPort, outPort, permittedNetwork, permittedMac, sourcePortRange);
	}
}
