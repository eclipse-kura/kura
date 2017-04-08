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
package org.eclipse.kura.net.modem;

import org.eclipse.kura.net.NetInterfaceAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Modem Interface Address represents the modem state
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ModemInterfaceAddress extends NetInterfaceAddress {

    /**
     * Reports signal strength in dBm
     *
     * @return signal strength
     */
    public int getSignalStrength();

    /**
     * Reports roaming status
     *
     * @return
     *         true - modem is roaming
     *         false - modem is not roaming
     */
    public boolean isRoaming();

    /**
     * Reports connection status
     *
     * @return connection status
     */
    public ModemConnectionStatus getConnectionStatus();

    /**
     * Reports number of bytes transmitted during a call
     *
     * @return number of bytes transmitted
     */
    public long getBytesTransmitted();

    /**
     * Reports number of bytes received during a call
     *
     * @return number of bytes received
     */
    public long getBytesReceived();

    /**
     * Reports Connection Type
     *
     * @return connection type
     */
    public ModemConnectionType getConnectionType();
}
