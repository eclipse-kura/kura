/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.route;

import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.route.RouteConfig;

/**
 * The IRouteService is used to add, remove, and obtain information on entries
 * in the system's route table
 *
 * CONFIGURATION
 *
 * Configuration will be accepted in the form of key/value pairs. The key/value
 * pairs are strictly defined here:
 *
 * CONFIG_ENTRY -> KEY + "=" + VALUE KEY -> PARAM + "_" + INDEX INDEX -> "1" |
 * "2" | ... | "N" PARAM -> "destination" | "netmask" | "iface" | "gateway"
 * VALUE -> (value of the specified parameter)
 *
 * EXAMPLE:
 *
 * destination_1=192.168.2.0 gateway_1=192.168.1.56 netmask_1=255.255.255.0
 * iface_1=eth0 destination_2=10.11.0.0 gateway_2=10.11.10.1
 * netmask_2=255.255.0.0 iface_2=eth1
 */

public interface RouteService {

	/**
	 * Returns the default route for a specified interface.
	 *
	 * @param interfaceName
	 *            A String object specifying the interface name.
	 * @return A Route object representing the default route or null if none
	 *         exists.
	 */
	public RouteConfig getDefaultRoute(String interfaceName);

	/**
	 * Returns the default route's interface for the specified destination
	 * address, or null if no route exists.
	 *
	 * @param destination
	 *            An IPAddress object representing the destination IP address.
	 * @return A String object specifying the interface name, null if no route
	 *         exists.
	 */
	public String getDefaultInterface(IPAddress destination);

	/**
	 * Adds a static route to the system's route table.
	 *
	 * @param destination
	 *            An IPAddress object representing the destination IP address.
	 * @param gateway
	 *            An IPAddress object representing the gateway IP address.
	 * @param netmask
	 *            An IPAddress object representing the netmask.
	 * @param iface
	 *            A String object specifying the interface name.
	 * @param metric
	 *            An int representing the metric (priority) of the route.
	 * @throws Exception
	 *             for invalid supplied parameters, or failure to add.
	 */
	public void addStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface, int metric)
			throws Exception;

	/**
	 * Removes a static route in the system's route table.
	 *
	 * @param destination
	 *            An InetAddress object representing the destination IP address.
	 * @param gateway
	 *            An InetAddress object representing the gateway IP address.
	 * @param netmask
	 *            An InetAddress object representing the netmask.
	 * @param iface
	 *            A String object specifying the interface name.
	 * @throws Exception
	 *             for invalid supplied parameters, or failure to remove.
	 */
	public void removeStaticRoute(IPAddress destination, IPAddress gateway, IPAddress netmask, String iface)
			throws Exception;

	/**
	 * Returns the routes contained in the system's route table.
	 *
	 * @return An array of Route objects representing the system's route table,
	 *         null if no routes exist.
	 */
	public RouteConfig[] getRoutes();
}