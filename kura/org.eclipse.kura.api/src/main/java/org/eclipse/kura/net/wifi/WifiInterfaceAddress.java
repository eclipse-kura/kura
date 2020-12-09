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
package org.eclipse.kura.net.wifi;

import org.eclipse.kura.net.NetInterfaceAddress;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for wifi status information
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WifiInterfaceAddress extends NetInterfaceAddress {

    /**
     * The operating mode of the wireless device.
     *
     * @return
     */
    public WifiMode getMode();

    /**
     * The bit rate currently used by the wireless device, in kilobits/second (Kb/s).
     *
     * @return
     */
    public long getBitrate();

    /**
     * Returns the WifiAccessPoint that this InterfaceAddress was acquired from when in managed mode.
     * Returns a WifiAccessPoint representing itself when in master mode.
     *
     * @return the WifiAccessPoint
     */
    public WifiAccessPoint getWifiAccessPoint();

}
