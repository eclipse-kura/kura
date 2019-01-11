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
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraConnectionStatus {

	/**
	 * initial state for any connection
	 */
	public static final int NEVERCONNECTED=-1;
	/**
	 * attempts have been made to connect to the device, but currently 
	 * no connection exists
	 */
	public static final int DISCONNECTED=0;
	/**
	 * attempting to connect to field device, this is a transient state
	 */
	public static final int CONNECTING=1;
	/**
	 * a connection to the field device currently exists.  A status of
	 * CONNECTED does not assure that requests from or commands to this
	 * field device will succeed.
	 */
	public static final int CONNECTED=2;
	/**
	 * attempting to disconnect from the field device.  Disconnection from
	 * a field device may be delayed while outstanding commands are either
	 * completed or terminated.
	 */
	public static final int DISCONNECTING=3;
}
