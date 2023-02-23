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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint.WifiAccessPointBuilder;
import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus.WifiInterfaceStatusBuilder;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.eclipse.kura.nm.NM80211ApSecurityFlags;
import org.eclipse.kura.nm.NM80211Mode;
import org.eclipse.kura.nm.NMDeviceState;
import org.eclipse.kura.nm.NMDeviceWifiCapabilities;
import org.eclipse.kura.usb.UsbNetDevice;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusConverter.class);

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_DEVICE_WIRELESS_BUS_NAME = "org.freedesktop.NetworkManager.Device.Wireless";
    private static final String NM_ACCESSPOINT_BUS_NAME = "org.freedesktop.NetworkManager.AccessPoint";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";

    private NMStatusConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static NetworkInterfaceStatus buildEthernetStatus(String interfaceName,
            Properties deviceProperties, Optional<Properties> ip4configProperties,
            Optional<UsbNetDevice> usbNetDevice) {
        EthernetInterfaceStatusBuilder builder = EthernetInterfaceStatus.builder();
        builder.withName(interfaceName).withVirtual(false);

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
        builder.withState(deviceStateConvert(deviceState));
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
        builder.withState(deviceStateConvert(deviceState));

        setDeviceStatus(builder, deviceProperties);
        setIP4Status(builder, ip4configProperties);

        return builder.build();
    }

    public static NetworkInterfaceStatus buildWirelessStatus(String interfaceName, Properties deviceProperties,
            Optional<Properties> ip4configProperties, Properties wirelessDeviceProperties,
            Optional<Properties> activeAccessPoint, List<Properties> accessPoints,
            Optional<UsbNetDevice> usbNetDevice) {
        WifiInterfaceStatusBuilder builder = WifiInterfaceStatus.builder();
        builder.withName(interfaceName).withVirtual(false);

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, "State"));
        builder.withState(deviceStateConvert(deviceState));

        builder.withUsbNetDevice(usbNetDevice);

        setDeviceStatus(builder, deviceProperties);
        setIP4Status(builder, ip4configProperties);
        setWifiStatus(builder, wirelessDeviceProperties, activeAccessPoint, accessPoints);

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

    private static void setWifiStatus(WifiInterfaceStatusBuilder builder, Properties wirelessDeviceProperties,
            Optional<Properties> activeAccessPoint, List<Properties> accessPoints) {
        NM80211Mode mode = NM80211Mode.fromUInt32(wirelessDeviceProperties.Get(NM_DEVICE_WIRELESS_BUS_NAME, "Mode"));
        builder.withMode(wifiModeConvert(mode));

        List<NMDeviceWifiCapabilities> capabilities = NMDeviceWifiCapabilities
                .fromUInt32(wirelessDeviceProperties.Get(NM_DEVICE_WIRELESS_BUS_NAME, "WirelessCapabilities"));
        builder.withCapabilities(wifiCapabilitiesConvert(capabilities));

        // builder.withSupportedBitrates

        // builder.withSupportedRadioModes
        // builder.withSupportedChannels
        // builder.withSupportedFrequencies
        // builder.withCountryCode

        if (mode == NM80211Mode.NM_802_11_MODE_AP) {
            if (!activeAccessPoint.isPresent()) {
                logger.warn("No access point found for interface in MASTER mode.");
                return;
            }
            // Only one AP should be available in MASTER mode
            WifiAccessPoint ap = wifiAccessPointConvert(accessPoints.get(0));
            builder.withActiveWifiAccessPoint(Optional.of(ap));
        } else {
            if (activeAccessPoint.isPresent()) {
                WifiAccessPoint ap = wifiAccessPointConvert(activeAccessPoint.get());
                builder.withActiveWifiAccessPoint(Optional.of(ap));
            }
        }

        builder.withAvailableWifiAccessPoints(wifiAccessPointConvert(accessPoints));
    }

    private static List<WifiAccessPoint> wifiAccessPointConvert(List<Properties> nmAccessPoints) {
        List<WifiAccessPoint> kuraAccessPoints = new ArrayList<>();

        for (Properties prop : nmAccessPoints) {
            kuraAccessPoints.add(wifiAccessPointConvert(prop));
        }

        return kuraAccessPoints;
    }

    private static WifiAccessPoint wifiAccessPointConvert(Properties nmAccessPoint) {
        WifiAccessPointBuilder builder = WifiAccessPoint.builder();

        byte[] rawSsid = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "Ssid");
        String ssid = new String(rawSsid, StandardCharsets.UTF_8);
        builder.withSsid(ssid);

        NM80211Mode mode = NM80211Mode.fromUInt32(nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "Mode"));
        builder.withMode(wifiModeConvert(mode));

        String rawHwAddress = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "HwAddress");
        builder.withHardwareAddress(getMacAddressBytes(rawHwAddress));

        UInt32 frequency = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "Frequency");
        builder.withFrequency(frequency.longValue());
        builder.withChannel(channelFrequencyConvert(frequency));

        UInt32 maxBitrate = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "MaxBitrate");
        builder.withMaxBitrate(maxBitrate.longValue());

        Byte strength = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "Strength");
        builder.withSignalQuality(strength.intValue());

        List<NM80211ApSecurityFlags> wpaSecurityFlags = NM80211ApSecurityFlags
                .fromUInt32(nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "WpaFlags"));
        builder.withWpaSecurity(wifiSecurityFlagConvert(wpaSecurityFlags));

        List<NM80211ApSecurityFlags> rsnSecurityFlags = NM80211ApSecurityFlags
                .fromUInt32(nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "RsnFlags"));
        builder.withRsnSecurity(wifiSecurityFlagConvert(rsnSecurityFlags));

        return builder.build();
    }

    private static int channelFrequencyConvert(UInt32 frequency) {
        int fMHz = frequency.intValue();

        /* see 802.11 17.3.8.3.2 and Annex J */
        if (fMHz == 2484) {
            return 14;
        } else if (fMHz < 2484) {
            return (fMHz - 2407) / 5;
        } else if (fMHz >= 4910 && fMHz <= 4980) {
            return (fMHz - 4000) / 5;
        } else if (fMHz < 5925) {
            return (fMHz - 5000) / 5;
        } else if (fMHz == 5935) {
            return 2;
        } else if (fMHz <= 45000) { /* DMG band lower limit */
            /* see 802.11ax D6.1 27.3.22.2 */
            return (fMHz - 5950) / 5;
        } else if (fMHz >= 58320 && fMHz <= 70200) {
            return (fMHz - 56160) / 2160;
        } else {
            return 0;
        }
    }

    private static Set<WifiSecurity> wifiSecurityFlagConvert(List<NM80211ApSecurityFlags> nmSecurityFlags) {
        List<WifiSecurity> kuraSecurityFlags = new ArrayList<>();

        for (NM80211ApSecurityFlags nmFlag : nmSecurityFlags) {
            kuraSecurityFlags.add(wifiSecurityFlagConvert(nmFlag));
        }

        return new HashSet<>(kuraSecurityFlags);
    }

    private static WifiSecurity wifiSecurityFlagConvert(NM80211ApSecurityFlags nmFlag) {
        switch (nmFlag) {
        case NM_802_11_AP_SEC_NONE:
            return WifiSecurity.NONE;
        case NM_802_11_AP_SEC_PAIR_WEP40:
            return WifiSecurity.PAIR_WEP40;
        case NM_802_11_AP_SEC_PAIR_WEP104:
            return WifiSecurity.PAIR_WEP104;
        case NM_802_11_AP_SEC_PAIR_TKIP:
            return WifiSecurity.PAIR_TKIP;
        case NM_802_11_AP_SEC_PAIR_CCMP:
            return WifiSecurity.PAIR_CCMP;
        case NM_802_11_AP_SEC_GROUP_WEP40:
            return WifiSecurity.GROUP_WEP40;
        case NM_802_11_AP_SEC_GROUP_WEP104:
            return WifiSecurity.GROUP_WEP104;
        case NM_802_11_AP_SEC_GROUP_TKIP:
            return WifiSecurity.GROUP_TKIP;
        case NM_802_11_AP_SEC_GROUP_CCMP:
            return WifiSecurity.GROUP_CCMP;
        case NM_802_11_AP_SEC_KEY_MGMT_PSK:
            return WifiSecurity.KEY_MGMT_PSK;
        case NM_802_11_AP_SEC_KEY_MGMT_802_1X:
            return WifiSecurity.KEY_MGMT_802_1X;
        case NM_802_11_AP_SEC_KEY_MGMT_SAE:
            return WifiSecurity.KEY_MGMT_SAE;
        case NM_802_11_AP_SEC_KEY_MGMT_OWE:
            return WifiSecurity.KEY_MGMT_OWE;
        case NM_802_11_AP_SEC_KEY_MGMT_OWE_TM:
            return WifiSecurity.KEY_MGMT_OWE_TM;
        case NM_802_11_AP_SEC_KEY_MGMT_EAP_SUITE_B_192:
            return WifiSecurity.KEY_MGMT_EAP_SUITE_B_192;
        default:
            throw new IllegalArgumentException(String.format("Non convertible NM80211ApSecurityFlag \"%s\"", nmFlag));
        }
    }

    private static Set<WifiCapability> wifiCapabilitiesConvert(List<NMDeviceWifiCapabilities> nmCapabilities) {
        List<WifiCapability> kuraCapabilities = new ArrayList<>();

        for (NMDeviceWifiCapabilities nmCapability : nmCapabilities) {
            kuraCapabilities.add(wifiCapabilitiesConvert(nmCapability));
        }

        return new HashSet<>(kuraCapabilities);

    }

    private static WifiCapability wifiCapabilitiesConvert(NMDeviceWifiCapabilities nmCapability) {
        switch (nmCapability) {
        case NM_WIFI_DEVICE_CAP_NONE:
            return WifiCapability.NONE;
        case NM_WIFI_DEVICE_CAP_CIPHER_WEP40:
            return WifiCapability.CIPHER_WEP40;
        case NM_WIFI_DEVICE_CAP_CIPHER_WEP104:
            return WifiCapability.CIPHER_WEP104;
        case NM_WIFI_DEVICE_CAP_CIPHER_TKIP:
            return WifiCapability.CIPHER_TKIP;
        case NM_WIFI_DEVICE_CAP_CIPHER_CCMP:
            return WifiCapability.CIPHER_CCMP;
        case NM_WIFI_DEVICE_CAP_WPA:
            return WifiCapability.WPA;
        case NM_WIFI_DEVICE_CAP_RSN:
            return WifiCapability.RSN;
        case NM_WIFI_DEVICE_CAP_AP:
            return WifiCapability.AP;
        case NM_WIFI_DEVICE_CAP_ADHOC:
            return WifiCapability.ADHOC;
        case NM_WIFI_DEVICE_CAP_FREQ_VALID:
            return WifiCapability.FREQ_VALID;
        case NM_WIFI_DEVICE_CAP_FREQ_2GHZ:
            return WifiCapability.FREQ_2GHZ;
        case NM_WIFI_DEVICE_CAP_FREQ_5GHZ:
            return WifiCapability.FREQ_5GHZ;
        default:
            throw new IllegalArgumentException(
                    String.format("Non convertible NMDeviceWifiCapabilities \"%s\"", nmCapability));
        }
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

    private static WifiMode wifiModeConvert(NM80211Mode mode) {
        switch (mode) {
        case NM_802_11_MODE_ADHOC:
            return WifiMode.ADHOC;
        case NM_802_11_MODE_INFRA:
            return WifiMode.INFRA;
        case NM_802_11_MODE_AP:
            return WifiMode.MASTER;
        case NM_802_11_MODE_MESH:
            return WifiMode.MESH;
        case NM_802_11_MODE_UNKNOWN:
        default:
            return WifiMode.UNKNOWN;
        }
    }

    private static NetworkInterfaceState deviceStateConvert(NMDeviceState state) {
        switch (state) {
        case NM_DEVICE_STATE_UNMANAGED:
            return NetworkInterfaceState.UNMANAGED;
        case NM_DEVICE_STATE_UNAVAILABLE:
            return NetworkInterfaceState.UNAVAILABLE;
        case NM_DEVICE_STATE_DISCONNECTED:
            return NetworkInterfaceState.DISCONNECTED;
        case NM_DEVICE_STATE_PREPARE:
            return NetworkInterfaceState.PREPARE;
        case NM_DEVICE_STATE_CONFIG:
            return NetworkInterfaceState.CONFIG;
        case NM_DEVICE_STATE_NEED_AUTH:
            return NetworkInterfaceState.NEED_AUTH;
        case NM_DEVICE_STATE_IP_CONFIG:
            return NetworkInterfaceState.IP_CONFIG;
        case NM_DEVICE_STATE_IP_CHECK:
            return NetworkInterfaceState.IP_CHECK;
        case NM_DEVICE_STATE_SECONDARIES:
            return NetworkInterfaceState.SECONDARIES;
        case NM_DEVICE_STATE_ACTIVATED:
            return NetworkInterfaceState.ACTIVATED;
        case NM_DEVICE_STATE_DEACTIVATING:
            return NetworkInterfaceState.DEACTIVATING;
        case NM_DEVICE_STATE_FAILED:
            return NetworkInterfaceState.FAILED;
        case NM_DEVICE_STATE_UNKNOWN:
        default:
            return NetworkInterfaceState.UNKNOWN;
        }
    }

}
