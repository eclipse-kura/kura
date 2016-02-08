/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

/**
 * Used to track interface configuration status
 * 
 * @author eurotech
 *
 */
public enum NetInterfaceStatus {
	/** IPv4 configuration is disabled **/
	netIPv4StatusDisabled,
	
	/** IPv4 configuration is enabled as a LAN interface **/
	netIPv4StatusEnabledLAN,
	
	/** IPv4 configuration is enabled as a WAN interface **/
	netIPv4StatusEnabledWAN,
	
	/** IPv4 configuration is unknown **/
	netIPv4StatusUnknown,
	
	/** IPv6 configuration is disabled **/
	netIPv6StatusDisabled,
	
	/** IPv6 configuration is enabled as a LAN interface **/
	netIPv6StatusEnabledLAN,
	
	/** IPv6 configuration is enabled as a WAN interface **/
	netIPv6StatusEnabledWAN,
	
	/** IPv6 configuration is unknown **/
	netIPv6StatusUnknown;
}
