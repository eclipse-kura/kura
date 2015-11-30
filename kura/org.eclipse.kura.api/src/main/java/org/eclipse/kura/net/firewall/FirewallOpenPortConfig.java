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

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;

/**
 * Marker interface for firewall open port configurations
 * 
 * @author eurotech
 *
 */
public interface FirewallOpenPortConfig extends NetConfig {

	/**
	 * Gets the port that is open for inbound connections
	 * 
	 * @return		The port number representing the inbound network port
	 */
	public int getPort();
	
	/**
	 * Gets range of ports that are open for inbound connections
	 * 
	 * @return		The port range representing the inbound network port
	 */
	public String getPortRange();

	/**
	 * Gets the type of network protocol (TCP or UDP) that is open for inbound connections
	 * 
	 * @return		The NetProtocol type associated with this interface
	 */
	public NetProtocol getProtocol();

	/**
	 * Gets the (optional) permitted remote network that can make inbound connections
	 * 
	 * @return		The NetworkPair representing the permitted network
	 */
	public NetworkPair<? extends IPAddress> getPermittedNetwork();

	/**
	 * Gets the (optional) permitted MAC address that is allowed to make inbound connections
	 * 
	 * @return		The MAC address that is allowed to make inbound connections
	 */
	public String getPermittedMac();

	/** Gets the (optional) permitted source port range that is allowed to make inbound connections
	 * 
	 * @return		The source port range that is allowed to make inbound connections
	 */
	public String getSourcePortRange();
}
