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

import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;

@SuppressWarnings("unused")
public class NetworkInterfaceStatusDTO {

    private final String id;
    private final String interfaceName;
    private final String hardwareAddress;
    private final NetworkInterfaceType type;
    private final String driver;
    private final String driverVersion;
    private final String firmwareVersion;
    private final boolean virtual;
    private final NetworkInterfaceState state;
    private final boolean autoConnect;
    private final int mtu;
    private final NetworkInterfaceIpAddressStatusDTO interfaceIp4Addresses;
    private final NetworkInterfaceIpAddressStatusDTO interfaceIp6Addresses;

    protected NetworkInterfaceStatusDTO(final NetworkInterfaceStatus status) {
        this.id = status.getInterfaceId();
        this.interfaceName = status.getInterfaceName();
        this.hardwareAddress = AddressUtil.formatHardwareAddress(status.getHardwareAddress());
        this.type = status.getType();
        this.driver = status.getDriver();
        this.driverVersion = status.getDriverVersion();
        this.firmwareVersion = status.getFirmwareVersion();
        this.virtual = status.isVirtual();
        this.state = status.getState();
        this.autoConnect = status.isAutoConnect();
        this.mtu = status.getMtu();
        this.interfaceIp4Addresses = status.getInterfaceIp4Addresses().map(NetworkInterfaceIpAddressStatusDTO::new)
                .orElse(null);
        this.interfaceIp6Addresses = status.getInterfaceIp6Addresses().map(NetworkInterfaceIpAddressStatusDTO::new)
                .orElse(null);
    }

    public static NetworkInterfaceStatusDTO fromNetworkInterfaceStatus(final NetworkInterfaceStatus status) {
        if (status instanceof ModemInterfaceStatus) {
            return new ModemInterfaceStatusDTO((ModemInterfaceStatus) status);
        } else if (status instanceof WifiInterfaceStatus) {
            return new WifiInterfaceStatusDTO((WifiInterfaceStatus) status);
        } else if (status instanceof EthernetInterfaceStatus) {
            return new EthernetInterfaceStatusDTO((EthernetInterfaceStatus) status);
        } else {
            return new NetworkInterfaceStatusDTO(status);
        }
    }
}
