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
package org.eclipse.kura.nm.status;

import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceStatus.NetworkInterfaceStatusBuilder;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus.EthernetInterfaceStatusBuilder;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus.LoopbackInterfaceStatusBuilder;
import org.eclipse.kura.nm.NMDeviceState;
import org.eclipse.kura.usb.UsbNetDevice;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusConverter.class);

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";

    private static final EnumMap<NMDeviceState, NetworkInterfaceState> DEVICE_STATE_CONVERTER = initDeviceStateConverter();

    private NMStatusConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static NetworkInterfaceStatus buildEthernetStatus(String interfaceName,
            Properties deviceProperties, Optional<Properties> ip4configProperties,
            Optional<UsbNetDevice> usbNetDevice) {
        EthernetInterfaceStatusBuilder builder = EthernetInterfaceStatus.builder();
        builder.withName(interfaceName).withVirtual(false);

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
        builder.withState(DEVICE_STATE_CONVERTER.get(deviceState));
        builder.withIsLinkUp(NMDeviceState.isConnected(deviceState));

        builder.withUsbNetDevice(usbNetDevice);

        setDeviceStatus(builder, deviceProperties);
        setIP4Status(builder, ip4configProperties);

        return builder.build();

    }

    public static NetworkInterfaceStatus buildLoopbackStatus(String interfaceName,
            Properties deviceProperties, Optional<Properties> ip4configProperties) {
        LoopbackInterfaceStatusBuilder builder = LoopbackInterfaceStatus.builder();
        builder.withName(interfaceName).withVirtual(true);

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
        builder.withState(DEVICE_STATE_CONVERTER.get(deviceState));

        setDeviceStatus(builder, deviceProperties);
        setIP4Status(builder, ip4configProperties);

        return builder.build();
    }

    private static void setDeviceStatus(NetworkInterfaceStatusBuilder<?> builder, Properties deviceProperties) {
        builder.withAutoConnect(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Autoconnect"));
        builder.withFirmwareVersion(deviceProperties.Get(NM_DEVICE_BUS_NAME, "FirmwareVersion"));
        builder.withDriver(deviceProperties.Get(NM_DEVICE_BUS_NAME, "Driver"));
        builder.withDriverVersion(deviceProperties.Get(NM_DEVICE_BUS_NAME, "DriverVersion"));

        UInt32 mtu = deviceProperties.Get(NM_DEVICE_BUS_NAME, "Mtu");
        builder.withMtu(mtu.intValue());

        String hwAddress = deviceProperties.Get(NM_DEVICE_BUS_NAME, "HwAddress");
        builder.withHardwareAddress(getMacAddressBytes(hwAddress));
    }

    private static void setIP4Status(NetworkInterfaceStatusBuilder<?> builder,
            Optional<Properties> ip4configProperties) {
        ip4configProperties.ifPresent(properties -> {
            try {
                NetworkInterfaceIpAddressStatus<IP4Address> ip4AddressStatus = new NetworkInterfaceIpAddressStatus<>();
                setIP4Gateway(properties, ip4AddressStatus);
                setIP4DnsServers(properties, ip4AddressStatus);
                setIP4Addresses(properties, ip4AddressStatus);
                builder.withInterfaceIp4Addresses(Optional.of(ip4AddressStatus));
            } catch (UnknownHostException e) {
                logger.error("Failed to set IP4 address.", e);
            }
        });
    }

    private static void setIP4Addresses(Properties ip4configProperties,
            NetworkInterfaceIpAddressStatus<IP4Address> ip4AddressStatus) throws UnknownHostException {
        List<Map<String, Variant<?>>> addressData = ip4configProperties.Get(NM_IP4CONFIG_BUS_NAME, "AddressData");
        for (Map<String, Variant<?>> data : addressData) {
            String addressStr = String.class.cast(data.get("address").getValue());
            UInt32 prefix = UInt32.class.cast(data.get("prefix").getValue());
            NetworkInterfaceIpAddress<IP4Address> address = new NetworkInterfaceIpAddress<>(
                    (IP4Address) IPAddress.parseHostAddress(addressStr), prefix.shortValue());
            ip4AddressStatus.addAddress(address);
        }
    }

    private static void setIP4DnsServers(Properties ip4configProperties,
            NetworkInterfaceIpAddressStatus<IP4Address> ip4AddressStatus) throws UnknownHostException {
        List<Map<String, Variant<?>>> nameserverData = ip4configProperties.Get(NM_IP4CONFIG_BUS_NAME,
                "NameserverData");
        for (Map<String, Variant<?>> dns : nameserverData) {
            ip4AddressStatus
                    .addDnsServerAddress(
                            (IP4Address) IPAddress.parseHostAddress(String.class.cast(dns.get("address").getValue())));
        }
    }

    private static void setIP4Gateway(Properties ip4configProperties,
            NetworkInterfaceIpAddressStatus<IP4Address> ip4AddressStatus) throws UnknownHostException {
        String gateway = ip4configProperties.Get(NM_IP4CONFIG_BUS_NAME, "Gateway");
        ip4AddressStatus.setGateway((IP4Address) IPAddress.parseHostAddress(gateway));
    }

    private static byte[] getMacAddressBytes(String macAddress) {
        String[] macAddressParts = macAddress.split(":");

        byte[] macAddressBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }

        return macAddressBytes;
    }

    private static EnumMap<NMDeviceState, NetworkInterfaceState> initDeviceStateConverter() {
        EnumMap<NMDeviceState, NetworkInterfaceState> map = new EnumMap<>(NMDeviceState.class);

        map.put(NMDeviceState.NM_DEVICE_STATE_UNKNOWN, NetworkInterfaceState.UNKNOWN);
        map.put(NMDeviceState.NM_DEVICE_STATE_UNMANAGED, NetworkInterfaceState.UNMANAGED);
        map.put(NMDeviceState.NM_DEVICE_STATE_UNAVAILABLE, NetworkInterfaceState.UNAVAILABLE);
        map.put(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, NetworkInterfaceState.DISCONNECTED);
        map.put(NMDeviceState.NM_DEVICE_STATE_PREPARE, NetworkInterfaceState.PREPARE);
        map.put(NMDeviceState.NM_DEVICE_STATE_CONFIG, NetworkInterfaceState.CONFIG);
        map.put(NMDeviceState.NM_DEVICE_STATE_NEED_AUTH, NetworkInterfaceState.NEED_AUTH);
        map.put(NMDeviceState.NM_DEVICE_STATE_IP_CONFIG, NetworkInterfaceState.IP_CONFIG);
        map.put(NMDeviceState.NM_DEVICE_STATE_IP_CHECK, NetworkInterfaceState.IP_CHECK);
        map.put(NMDeviceState.NM_DEVICE_STATE_SECONDARIES, NetworkInterfaceState.SECONDARIES);
        map.put(NMDeviceState.NM_DEVICE_STATE_ACTIVATED, NetworkInterfaceState.ACTIVATED);
        map.put(NMDeviceState.NM_DEVICE_STATE_DEACTIVATING, NetworkInterfaceState.DEACTIVATING);
        map.put(NMDeviceState.NM_DEVICE_STATE_FAILED, NetworkInterfaceState.FAILED);

        return map;
    }

}
