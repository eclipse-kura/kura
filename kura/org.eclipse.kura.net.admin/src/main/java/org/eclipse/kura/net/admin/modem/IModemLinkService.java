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
package org.eclipse.kura.net.admin.modem;

import org.eclipse.kura.KuraException;

public interface IModemLinkService {

	public static final String SERVICE_NAME = IModemLinkService.class.getName();

	/**
	* Connect request with default timeout 
	* 
	* @throws KuraException
	*/
	public void connect() throws KuraException;
	
	/**
	* Connect request
	* @param tout - timeout in seconds as {@link int}
	* 
	* @throws KuraException
	*/
	public void connect(int tout) throws KuraException;

	/**
	* Disconnect request
	*
	* @throws KuraException
	*/
	public void disconnect () throws KuraException;
	
	public void release();
	
	public PppState getPppState () throws KuraException;
	
	public PppState getPppState (int tout) throws KuraException;
	
	/**
	* Reports IP address of cellular interface
	*
	* @return IP address as <code>String</code>
	* @throws KuraException
	*/
	public String getIPaddress() throws KuraException;
	
	/**
	* Reports gateway IP address (IP address of the peer)
	*
	* @return gateway IP address as <code>String</code>
	*/
	public String getGatewayIpAddress();
	
	/**
	* Reports name of network interface
	*
	* @return network interface name as <code>String</code>
	*/
	public String getIfaceName();
}