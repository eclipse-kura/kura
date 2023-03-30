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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiMode;

@SuppressWarnings("unused")
public class WifiInterfaceStatusDTO extends NetworkInterfaceStatusDTO {

    private final Set<WifiCapability> capabilities;
    private final List<WifiChannelDTO> channels;
    private final String countryCode;
    private final WifiMode mode;
    private final WifiAccessPointDTO activeWifiAccessPoint;
    private final List<WifiAccessPointDTO> availableWifiAccessPoints;

    public WifiInterfaceStatusDTO(final WifiInterfaceStatus status) {
        super(status);

        this.capabilities = status.getCapabilities();
        this.channels = status.getChannels().stream().map(WifiChannelDTO::new).collect(Collectors.toList());
        this.countryCode = status.getCountryCode();
        this.mode = status.getMode();
        this.activeWifiAccessPoint = status.getActiveWifiAccessPoint().map(WifiAccessPointDTO::new).orElse(null);
        this.availableWifiAccessPoints = status.getAvailableWifiAccessPoints().stream().map(WifiAccessPointDTO::new)
                .collect(Collectors.toList());

    }
}
