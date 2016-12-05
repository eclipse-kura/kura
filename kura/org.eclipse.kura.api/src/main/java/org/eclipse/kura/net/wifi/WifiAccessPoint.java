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

import java.util.EnumSet;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The WifiAccessPoint models an access point for a Wifi network.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WifiAccessPoint {

    /**
     * The Service Set Identifier identifying the access point.
     *
     * @return
     */
    public String getSSID();

    /**
     * The hardware address (BSSID) of the access point.
     *
     * @return
     */
    public byte[] getHardwareAddress();

    /**
     * The radio channel frequency in use by the access point, in MHz.
     *
     * @return
     */
    public long getFrequency();

    /**
     * Describes the operating mode of the access point.
     *
     * @return
     */
    public WifiMode getMode();

    /**
     * The bitrates this access point is capable of, in kilobits/second (Kb/s).
     *
     * @return
     */
    public List<Long> getBitrate();

    /**
     * The current signal quality of the access point, in percent.
     *
     * @return
     */
    public int getStrength();

    /**
     * Describes the access point's capabilities according to WPA (Wifi Protected Access).
     *
     * @return
     */
    public EnumSet<WifiSecurity> getWpaSecurity();

    /**
     * Describes the access point's capabilities according to the RSN (Robust Secure Network) protocol.
     *
     * @return
     */
    public EnumSet<WifiSecurity> getRsnSecurity();

    /**
     * Describes the access point's capabilities
     *
     * @return
     */
    public List<String> getCapabilities();
}
