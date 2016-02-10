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
package org.eclipse.kura.net.dhcp;

import org.eclipse.kura.KuraException;

/**
 * Represents a DHCP server.
 * 
 * @author eurotech
 *
 */
public interface DhcpServer {

	/**
	 * Returns whether or not the DhcpServer is actively running or not
	 * 
	 * @return	a boolean denoting whether or not the DhcpServer is running or not
	 * @throws KuraException
	 */
	public boolean isRunning() throws KuraException;
}
