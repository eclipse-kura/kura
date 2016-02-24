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
package org.eclipse.kura.net;

/**
 * Network interface for Ethernet cards. 
 */
public interface EthernetInterface<T extends NetInterfaceAddress> extends NetInterface<T> 
{	
	/**
	 * Indicates whether the physical carrier is found (e.g. whether a cable is plugged in or not).
	 * @return
	 */
	public boolean isLinkUp();
}
