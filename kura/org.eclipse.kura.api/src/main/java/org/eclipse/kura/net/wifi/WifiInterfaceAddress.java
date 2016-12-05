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
