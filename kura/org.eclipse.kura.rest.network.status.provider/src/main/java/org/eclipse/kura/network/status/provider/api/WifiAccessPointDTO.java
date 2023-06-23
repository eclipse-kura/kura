/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.network.status.provider.api;

import java.util.Set;

import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;

@SuppressWarnings("unused")
public class WifiAccessPointDTO {

    private final String ssid;
    private final String hardwareAddress;
    private final WifiChannelDTO channel;
    private final WifiMode mode;
    private final long maxBitrate;
    private final int signalQuality;
    private final int signalStrength;
    private final Set<WifiSecurity> wpaSecurity;
    private final Set<WifiSecurity> rsnSecurity;

    public WifiAccessPointDTO(final WifiAccessPoint accessPoint) {
        this.ssid = accessPoint.getSsid();
        this.channel = new WifiChannelDTO(accessPoint.getChannel());
        this.hardwareAddress = AddressUtil.formatHardwareAddress(accessPoint.getHardwareAddress());
        this.mode = accessPoint.getMode();
        this.maxBitrate = accessPoint.getMaxBitrate();
        this.signalQuality = accessPoint.getSignalQuality();
        this.signalStrength = accessPoint.getSignalStrength();
        this.wpaSecurity = accessPoint.getWpaSecurity();
        this.rsnSecurity = accessPoint.getRsnSecurity();
    }
}
