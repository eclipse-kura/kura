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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceStatus.NetworkInterfaceStatusBuilder;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus.EthernetInterfaceStatusBuilder;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus.LoopbackInterfaceStatusBuilder;
import org.eclipse.kura.net.status.modem.Bearer;
import org.eclipse.kura.net.status.modem.BearerIpType;
import org.eclipse.kura.net.status.modem.ESimStatus;
import org.eclipse.kura.net.status.modem.ModemBand;
import org.eclipse.kura.net.status.modem.ModemCapability;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus.ModemInterfaceStatusBuilder;
import org.eclipse.kura.net.status.modem.ModemMode;
import org.eclipse.kura.net.status.modem.ModemModePair;
import org.eclipse.kura.net.status.modem.ModemPortType;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.modem.SimType;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint.WifiAccessPointBuilder;
import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus.WifiInterfaceStatusBuilder;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.nm.MMBearerIpFamily;
import org.eclipse.kura.nm.MMModem3gppRegistrationState;
import org.eclipse.kura.nm.MMModemAccessTechnology;
import org.eclipse.kura.nm.MMModemBand;
import org.eclipse.kura.nm.MMModemCapability;
import org.eclipse.kura.nm.MMModemLocationSource;
import org.eclipse.kura.nm.MMModemMode;
import org.eclipse.kura.nm.MMModemPortType;
import org.eclipse.kura.nm.MMModemPowerState;
import org.eclipse.kura.nm.MMModemState;
import org.eclipse.kura.nm.MMSimEsimStatus;
import org.eclipse.kura.nm.MMSimType;
import org.eclipse.kura.nm.NM80211ApSecurityFlags;
import org.eclipse.kura.nm.NM80211Mode;
import org.eclipse.kura.nm.NMDeviceState;
import org.eclipse.kura.nm.NMDeviceWifiCapabilities;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusConverter {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusConverter.class);

    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_DEVICE_WIRELESS_BUS_NAME = "org.freedesktop.NetworkManager.Device.Wireless";
    private static final String NM_DEVICE_WIRED_BUS_NAME = "org.freedesktop.NetworkManager.Device.Wired";
    private static final String NM_ACCESSPOINT_BUS_NAME = "org.freedesktop.NetworkManager.AccessPoint";
    private static final String NM_IP4CONFIG_BUS_NAME = "org.freedesktop.NetworkManager.IP4Config";
    private static final String NM_DEVICE_PROPERTY_HW_ADDRESS = "HwAddress";
    private static final String MM_MODEM_BUS_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MM_SIM_BUS_NAME = "org.freedesktop.ModemManager1.Sim";
    private static final String MM_BEARER_BUS_NAME = "org.freedesktop.ModemManager1.Bearer";
    private static final String MM_MODEM_LOCATION_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Location";
    private static final String MM_MODEM_3GPP_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Modem3gpp";
    private static final String EMPTY_MAC_ADDRESS = "00:00:00:00:00:00";
    private static final byte[] EMPTY_MAC_ADDRESS_BYTES = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    private static final String STATE = "State";

    private NMStatusConverter() {
        throw new IllegalStateException("Utility class");
    }

    public static NetworkInterfaceStatus buildEthernetStatus(String interfaceId,
            DevicePropertiesWrapper devicePropertiesWrapper, Optional<Properties> ip4configProperties) {

        EthernetInterfaceStatusBuilder builder = EthernetInterfaceStatus.builder();
        builder.withInterfaceId(interfaceId).withInterfaceName(interfaceId).withVirtual(false);

        NMDeviceState deviceState = NMDeviceState
                .fromUInt32(devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, STATE));
        builder.withState(deviceStateConvert(deviceState));
        builder.withIsLinkUp(NMDeviceState.isConnected(deviceState));

        setDeviceStatus(builder, devicePropertiesWrapper);
        setIP4Status(builder, ip4configProperties);

        return builder.build();

    }

    public static NetworkInterfaceStatus buildLoopbackStatus(String interfaceId,
            DevicePropertiesWrapper devicePropertiesWrapper, Optional<Properties> ip4configProperties) {
        LoopbackInterfaceStatusBuilder builder = LoopbackInterfaceStatus.builder();
        builder.withInterfaceId(interfaceId).withInterfaceName(interfaceId).withVirtual(true);

        NMDeviceState deviceState = NMDeviceState
                .fromUInt32(devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, STATE));
        builder.withState(deviceStateConvert(deviceState));

        setDeviceStatus(builder, devicePropertiesWrapper);
        setIP4Status(builder, ip4configProperties);

        return builder.build();
    }

    public static NetworkInterfaceStatus buildWirelessStatus(String interfaceId,
            DevicePropertiesWrapper devicePropertiesWrapper, Optional<Properties> ip4configProperties,
            AccessPointsProperties accessPointsProperties, SupportedChannelsProperties supportedChannelsProperties) {
        WifiInterfaceStatusBuilder builder = WifiInterfaceStatus.builder();
        builder.withInterfaceId(interfaceId).withInterfaceName(interfaceId).withVirtual(false);

        NMDeviceState deviceState = NMDeviceState
                .fromUInt32(devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, STATE));
        builder.withState(deviceStateConvert(deviceState));

        setDeviceStatus(builder, devicePropertiesWrapper);
        setIP4Status(builder, ip4configProperties);
        setWifiStatus(builder, devicePropertiesWrapper.getDeviceSpecificProperties(),
                accessPointsProperties.getActiveAccessPoint(), accessPointsProperties.getAvailableAccessPoints(),
                supportedChannelsProperties.getCountryCode(), supportedChannelsProperties.getSupportedChannels());

        return builder.build();
    }

    private static void setDeviceStatus(NetworkInterfaceStatusBuilder<?> builder,
            DevicePropertiesWrapper devicePropertiesWrapper) {
        builder.withAutoConnect(devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, "Autoconnect"));
        builder.withFirmwareVersion(
                devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, "FirmwareVersion"));
        builder.withDriver(devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, "Driver"));
        builder.withDriverVersion(
                devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, "DriverVersion"));
        UInt32 mtu = devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, "Mtu");
        builder.withMtu(mtu.intValue());
        String hwAddress = getHwAddressFrom(devicePropertiesWrapper);
        builder.withHardwareAddress(getMacAddressBytes(hwAddress));
    }

    public static NetworkInterfaceStatus buildModemStatus(String interfaceId,
            DevicePropertiesWrapper devicePropertiesWrapper, Optional<Properties> ip4configProperties,
            List<SimProperties> simProperties, List<Properties> bearerProperties) {
        ModemInterfaceStatusBuilder builder = ModemInterfaceStatus.builder();
        Properties deviceProperties = devicePropertiesWrapper.getDeviceProperties();
        Optional<Properties> modemProperties = devicePropertiesWrapper.getDeviceSpecificProperties();
        builder.withInterfaceId(interfaceId).withVirtual(false);
        setModemInterfaceName(interfaceId, deviceProperties, builder);

        NMDeviceState deviceState = NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, STATE));
        builder.withState(deviceStateConvert(deviceState));

        setDeviceStatus(builder, devicePropertiesWrapper);
        setIP4Status(builder, ip4configProperties);
        setModemStatus(builder, modemProperties, simProperties, bearerProperties);

        String driver = deviceProperties.Get(NM_DEVICE_BUS_NAME, "Driver");
        if (driver.contains("cdc_acm")) {
            builder.withConnectionType(ModemConnectionType.PPP);
        } else {
            builder.withConnectionType(ModemConnectionType.DirectIP);
        }

        return builder.build();
    }

    // Set the interface name as the interface used for the connection if available
    private static void setModemInterfaceName(String interfaceId, Properties deviceProperties,
            ModemInterfaceStatusBuilder builder) {
        try {
            String ipInterface = deviceProperties.Get(NM_DEVICE_BUS_NAME, "IpInterface");
            if (Objects.nonNull(ipInterface) && !ipInterface.isEmpty()) {
                builder.withInterfaceName(ipInterface);
            } else {
                builder.withInterfaceName("");
            }
        } catch (DBusExecutionException e) {
            logger.debug("Cannot retrieve IpInterface for {} interface Id", interfaceId, e);
            builder.withInterfaceName("");
        }
    }

    public static String getModemDeviceHwPath(Properties modemProperties) {
        String modemDeviceProperty = (String) modemProperties.Get(MM_MODEM_BUS_NAME, "Device");
        return modemDeviceProperty.substring(modemDeviceProperty.lastIndexOf("/") + 1);
    }

    private static String getHwAddressFrom(DevicePropertiesWrapper devicePropertiesWrapper) {
        try {
            return devicePropertiesWrapper.getDeviceProperties().Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_HW_ADDRESS);
        } catch (DBusExecutionException e) {
            logger.debug("NetworkManager version lower then 1.24 detected.");
        }

        Optional<Properties> specificProperties = devicePropertiesWrapper.getDeviceSpecificProperties();
        if (!specificProperties.isPresent()) {
            logger.debug("No device specific properties provided. Assuming Loopback. Setting HW Address to blank.");
            return EMPTY_MAC_ADDRESS;
        }

        switch (devicePropertiesWrapper.getDeviceType()) {
        case NM_DEVICE_TYPE_ETHERNET:
            return specificProperties.get().Get(NM_DEVICE_WIRED_BUS_NAME, NM_DEVICE_PROPERTY_HW_ADDRESS);
        case NM_DEVICE_TYPE_WIFI:
            return specificProperties.get().Get(NM_DEVICE_WIRELESS_BUS_NAME, NM_DEVICE_PROPERTY_HW_ADDRESS);
        case NM_DEVICE_TYPE_MODEM:
        case NM_DEVICE_TYPE_GENERIC:
        case NM_DEVICE_TYPE_LOOPBACK:
        default:
            logger.debug("Setting HW Address to blank.");
            return EMPTY_MAC_ADDRESS;
        }
    }

    private static void setIP4Status(NetworkInterfaceStatusBuilder<?> builder,
            Optional<Properties> ip4configProperties) {
        ip4configProperties.ifPresent(properties -> {
            try {
                NetworkInterfaceIpAddressStatus.Builder<IP4Address> ip4AddressStatusBuilder = NetworkInterfaceIpAddressStatus
                        .builder();
                setIP4Gateway(properties, ip4AddressStatusBuilder);
                setIP4DnsServers(properties, ip4AddressStatusBuilder);
                setIP4Addresses(properties, ip4AddressStatusBuilder);
                builder.withInterfaceIp4Addresses(Optional.of(ip4AddressStatusBuilder.build()));
            } catch (UnknownHostException e) {
                logger.error("Failed to set IP4 address.", e);
            }
        });
    }

    private static void setWifiStatus(WifiInterfaceStatusBuilder builder, Optional<Properties> wirelessDeviceProperties,
            Optional<Properties> activeAccessPoint, List<Properties> accessPoints, String countryCode,
            List<WifiChannel> supportedChannels) {
        if (wirelessDeviceProperties.isPresent()) {
            NM80211Mode mode = NM80211Mode
                    .fromUInt32(wirelessDeviceProperties.get().Get(NM_DEVICE_WIRELESS_BUS_NAME, "Mode"));
            builder.withMode(wifiModeConvert(mode));

            List<NMDeviceWifiCapabilities> capabilities = NMDeviceWifiCapabilities.fromUInt32(
                    wirelessDeviceProperties.get().Get(NM_DEVICE_WIRELESS_BUS_NAME, "WirelessCapabilities"));
            builder.withCapabilities(wifiCapabilitiesConvert(capabilities));
        }
        List<org.eclipse.kura.net.status.wifi.WifiChannel> kuraSupportedChannels = wifiChannelsConvert(
                supportedChannels);
        builder.withWifiChannels(kuraSupportedChannels);

        builder.withCountryCode(countryCode);

        if (activeAccessPoint.isPresent()) {
            // ActiveAccessPoint returns itself if in MASTER mode
            // returns the one we're connected to in INFRA mode
            WifiAccessPoint ap = wifiAccessPointConvert(activeAccessPoint.get());
            builder.withActiveWifiAccessPoint(Optional.of(ap));
        }

        builder.withAvailableWifiAccessPoints(wifiAccessPointConvert(accessPoints));
    }

    private static List<org.eclipse.kura.net.status.wifi.WifiChannel> wifiChannelsConvert(
            List<WifiChannel> supportedChannels) {
        List<org.eclipse.kura.net.status.wifi.WifiChannel> kuraChannels = new ArrayList<>();

        for (WifiChannel channel : supportedChannels) {
            org.eclipse.kura.net.status.wifi.WifiChannel.Builder kuraChannel = org.eclipse.kura.net.status.wifi.WifiChannel
                    .builder(channel.getChannel(), channel.getFrequency());
            if (channel.getAttenuation() != null) {
                kuraChannel.withAttenuation(channel.getAttenuation());
            }
            if (channel.isDisabled() != null) {
                kuraChannel.withDisabled(channel.isDisabled());
            }

            if (channel.isNoInitiatingRadiation() != null) {
                kuraChannel.withNoInitiatingRadiation(channel.isNoInitiatingRadiation());
            }

            if (channel.isRadarDetection() != null) {
                kuraChannel.withRadarDetection(channel.isRadarDetection());
            }

            kuraChannels.add(kuraChannel.build());
        }

        return kuraChannels;
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

        String rawHwAddress = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, NM_DEVICE_PROPERTY_HW_ADDRESS);
        builder.withHardwareAddress(getMacAddressBytes(rawHwAddress));

        UInt32 uintFrequency = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "Frequency");
        int frequency = uintFrequency.intValue();
        int channel = channelFrequencyConvert(frequency);
        builder.withChannel(org.eclipse.kura.net.status.wifi.WifiChannel.builder(channel, frequency).build());

        UInt32 maxBitrate = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "MaxBitrate");
        builder.withMaxBitrate(maxBitrate.longValue());

        Byte signalQuality = nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "Strength");
        builder.withSignalQuality(signalQuality.intValue());
        builder.withSignalStrength(convertToWifiSignalStrength(signalQuality.intValue()));

        List<NM80211ApSecurityFlags> wpaSecurityFlags = NM80211ApSecurityFlags
                .fromUInt32(nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "WpaFlags"));
        builder.withWpaSecurity(wifiSecurityFlagConvert(wpaSecurityFlags));

        List<NM80211ApSecurityFlags> rsnSecurityFlags = NM80211ApSecurityFlags
                .fromUInt32(nmAccessPoint.Get(NM_ACCESSPOINT_BUS_NAME, "RsnFlags"));
        builder.withRsnSecurity(wifiSecurityFlagConvert(rsnSecurityFlags));

        return builder.build();
    }

    private static int channelFrequencyConvert(int fMHz) {
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
        case NM_WIFI_DEVICE_CAP_MESH:
            return WifiCapability.MESH;
        case NM_WIFI_DEVICE_CAP_IBSS_RSN:
            return WifiCapability.IBSS_RSN;
        default:
            throw new IllegalArgumentException(
                    String.format("Non convertible NMDeviceWifiCapabilities \"%s\"", nmCapability));
        }
    }

    private static void setIP4Addresses(Properties ip4configProperties,
            NetworkInterfaceIpAddressStatus.Builder<IP4Address> builder) throws UnknownHostException {
        List<Map<String, Variant<?>>> addressData = ip4configProperties.Get(NM_IP4CONFIG_BUS_NAME, "AddressData");
        final List<NetworkInterfaceIpAddress<IP4Address>> addresses = new ArrayList<>();
        for (Map<String, Variant<?>> data : addressData) {
            String addressStr = String.class.cast(data.get("address").getValue());
            UInt32 prefix = UInt32.class.cast(data.get("prefix").getValue());
            NetworkInterfaceIpAddress<IP4Address> address = new NetworkInterfaceIpAddress<>(
                    (IP4Address) IPAddress.parseHostAddress(addressStr), prefix.shortValue());
            addresses.add(address);
        }
        builder.withAddresses(addresses);
    }

    private static void setIP4DnsServers(Properties ip4configProperties,
            NetworkInterfaceIpAddressStatus.Builder<IP4Address> builder) throws UnknownHostException {
        List<Map<String, Variant<?>>> nameserverData = ip4configProperties.Get(NM_IP4CONFIG_BUS_NAME, "NameserverData");
        final List<IP4Address> dnsAddresses = new ArrayList<>();
        for (Map<String, Variant<?>> dns : nameserverData) {
            dnsAddresses.add((IP4Address) IPAddress.parseHostAddress(String.class.cast(dns.get("address").getValue())));
        }
        builder.withDnsServerAddresses(dnsAddresses);
    }

    private static void setIP4Gateway(Properties ip4configProperties,
            NetworkInterfaceIpAddressStatus.Builder<IP4Address> ip4AddressStatus) throws UnknownHostException {
        String gateway = ip4configProperties.Get(NM_IP4CONFIG_BUS_NAME, "Gateway");
        if (Objects.nonNull(gateway) && !gateway.isEmpty()) {
            final IP4Address address = (IP4Address) IPAddress.parseHostAddress(gateway);
            ip4AddressStatus.withGateway(Optional.of(address));
        }
    }

    private static void setModemStatus(ModemInterfaceStatusBuilder builder, Optional<Properties> modemProperties,
            List<SimProperties> simProperties, List<Properties> bearerProperties) {
        modemProperties.ifPresent(properties -> {
            builder.withModel(properties.Get(MM_MODEM_BUS_NAME, "Model"));
            builder.withManufacturer(properties.Get(MM_MODEM_BUS_NAME, "Manufacturer"));
            builder.withSerialNumber(properties.Get(MM_MODEM_BUS_NAME, "EquipmentIdentifier"));
            builder.withSoftwareRevision(properties.Get(MM_MODEM_BUS_NAME, "Revision"));
            builder.withHardwareRevision(properties.Get(MM_MODEM_BUS_NAME, "HardwareRevision"));
            builder.withPrimaryPort(properties.Get(MM_MODEM_BUS_NAME, "PrimaryPort"));
            builder.withPorts(getPorts(properties));
            builder.withSupportedModemCapabilities(getModemSupportedCapabilities(properties));
            builder.withCurrentModemCapabilities(getModemCurrentCapabilities(properties));
            builder.withPowerState(
                    MMModemPowerState.toModemPowerState(properties.Get(MM_MODEM_BUS_NAME, "PowerState")));
            builder.withSupportedModes(getSupportedModemModes(properties));
            builder.withCurrentModes(getCurrentModemMode(properties));
            builder.withSupportedBands(getModemBands(properties, "SupportedBands"));
            builder.withCurrentBands(getModemBands(properties, "CurrentBands"));
            builder.withGpsSupported(isGpsSupported(properties));
            builder.withSimLocked(isSimLocked(properties));
            builder.withConnectionStatus(MMModemState.toModemState(properties.Get(MM_MODEM_BUS_NAME, STATE)));
            builder.withAccessTechnologies(MMModemAccessTechnology
                    .toAccessTechnologyFromBitMask(properties.Get(MM_MODEM_BUS_NAME, "AccessTechnologies")));
            int signalQuality = getSignalQuality(properties);
            builder.withSignalQuality(signalQuality);
            builder.withSignalStrength(convertToModemSignalStrength(signalQuality));
            fill3gppProperties(builder, properties);
        });
        if (Objects.nonNull(simProperties) && !simProperties.isEmpty()) {
            builder.withAvailableSims(getAvailableSims(simProperties));
        }
        if (Objects.nonNull(bearerProperties) && !bearerProperties.isEmpty()) {
            builder.withBearers(getBearers(bearerProperties));
        }
    }

    private static Map<String, ModemPortType> getPorts(Properties properties) {
        Map<String, ModemPortType> ports = new HashMap<>();
        List<Object[]> rawPorts = properties.Get(MM_MODEM_BUS_NAME, "Ports");
        rawPorts.forEach(portArray -> {
            if (portArray.length >= 2) {
                ports.put(String.class.cast(portArray[0]), MMModemPortType.toModemPortType((UInt32) portArray[1]));
            }
        });
        return ports;
    }

    private static Set<ModemCapability> getModemSupportedCapabilities(Properties properties) {
        EnumSet<ModemCapability> modemCapabilities = EnumSet.noneOf(ModemCapability.class);
        List<UInt32> capabilities = properties.Get(MM_MODEM_BUS_NAME, "SupportedCapabilities");
        capabilities.forEach(capability -> modemCapabilities.add(MMModemCapability.toModemCapability(capability)));
        return modemCapabilities;
    }

    private static Set<ModemCapability> getModemCurrentCapabilities(Properties properties) {
        UInt32 capabilityBitMask = properties.Get(MM_MODEM_BUS_NAME, "CurrentCapabilities");
        return MMModemCapability.toModemCapabilitiesFromBitMask(capabilityBitMask);
    }

    private static Set<ModemModePair> getSupportedModemModes(Properties properties) {
        Set<ModemModePair> modes = new HashSet<>();
        List<Object[]> rawModes = properties.Get(MM_MODEM_BUS_NAME, "SupportedModes");
        rawModes.forEach(mode -> {
            if (mode.length >= 2) {
                Set<ModemMode> modemModes = MMModemMode.toModemModeFromBitMask((UInt32) mode[0]);
                ModemMode preferredMode = MMModemMode.toModemMode((UInt32) mode[1]);
                modes.add(new ModemModePair(modemModes, preferredMode));
            }
        });
        return modes;
    }

    private static ModemModePair getCurrentModemMode(Properties properties) {
        ModemModePair mode = new ModemModePair(Collections.emptySet(), ModemMode.NONE);
        Object[] rawMode = properties.Get(MM_MODEM_BUS_NAME, "CurrentModes");
        if (rawMode.length >= 2) {
            Set<ModemMode> modemModes = MMModemMode.toModemModeFromBitMask((UInt32) rawMode[0]);
            ModemMode preferredMode = MMModemMode.toModemMode((UInt32) rawMode[1]);
            mode = new ModemModePair(modemModes, preferredMode);
        }
        return mode;
    }

    private static Set<ModemBand> getModemBands(Properties properties, String name) {
        Set<ModemBand> bands = new HashSet<>();
        List<UInt32> rawBands = properties.Get(MM_MODEM_BUS_NAME, name);
        rawBands.forEach(band -> bands.add(MMModemBand.toModemBands(band)));
        return bands;
    }

    private static boolean isGpsSupported(Properties properties) {
        boolean isSupported = false;
        try {
            UInt32 locationSources = properties.Get(MM_MODEM_LOCATION_BUS_NAME, "Capabilities");
            // Check if the location capability is MM_MODEM_LOCATION_SOURCE_GPS_RAW or
            // MM_MODEM_LOCATION_SOURCE_GPS_NMEA
            Set<MMModemLocationSource> modemLocationSources = MMModemLocationSource
                    .toMMModemLocationSourceFromBitMask(locationSources);
            if (modemLocationSources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW)
                    || modemLocationSources.contains(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA)) {
                isSupported = true;
            }
        } catch (DBusExecutionException e) {
            logger.debug("Cannot retrieve location object", e);
        }
        return isSupported;
    }

    private static boolean isSimLocked(Properties properties) {
        boolean simLocked = true;
        UInt32 simStatus = properties.Get(MM_MODEM_BUS_NAME, "UnlockRequired");
        // Check if the sim lock status is MM_MODEM_LOCK_UNKNOWN or MM_MODEM_LOCK_NONE
        if (simStatus.intValue() == 0 || simStatus.intValue() == 1) {
            simLocked = false;
        }
        return simLocked;
    }

    private static List<Sim> getAvailableSims(List<SimProperties> properties) {
        List<Sim> sims = new ArrayList<>();
        for (SimProperties property : properties) {
            Sim sim = getAvailableSim(property.getProperties(), property.isActive(), property.isPrimary());
            sims.add(sim);
        }

        return sims;
    }

    private static Sim getAvailableSim(Properties simProperties, boolean isActive, boolean isPrimary) {
        String iccid = simProperties.Get(MM_SIM_BUS_NAME, "SimIdentifier");
        String imsi = simProperties.Get(MM_SIM_BUS_NAME, "Imsi");
        String eid = "";
        try {
            eid = simProperties.Get(MM_SIM_BUS_NAME, "Eid");
        } catch (DBusExecutionException e) {
            logger.warn("Eid property not found.");
        }
        String operatorName = simProperties.Get(MM_SIM_BUS_NAME, "OperatorName");
        SimType simType = SimType.PHYSICAL;
        ESimStatus eSimStatus = ESimStatus.UNKNOWN;
        try {
            simType = MMSimType.toSimType(simProperties.Get(MM_SIM_BUS_NAME, "SimType"));
            if (simType.equals(SimType.ESIM)) {
                eSimStatus = MMSimEsimStatus.toESimStatus(simProperties.Get(MM_SIM_BUS_NAME, "EsimStatus"));
            }
        } catch (DBusExecutionException e) {
            logger.warn("SimType property not found. Only physical sims are supported.");
        }

        return Sim.builder().withActive(isActive).withPrimary(isPrimary).withIccid(iccid).withImsi(imsi).withEid(eid)
                .withOperatorName(operatorName).withSimType(simType).withESimStatus(eSimStatus).build();
    }

    private static List<Bearer> getBearers(List<Properties> properties) {
        List<Bearer> bearers = new ArrayList<>();
        for (Properties bearerProperties : properties) {
            String name = bearerProperties.Get(MM_BEARER_BUS_NAME, "Interface");
            boolean connected = bearerProperties.Get(MM_BEARER_BUS_NAME, "Connected");
            // MM unwraps the Variant<?> object in the corresponding Java types.
            // So the best option is to convert them to an Object and convert the value
            // manually.
            Map<String, Object> settings = bearerProperties.Get(MM_BEARER_BUS_NAME, "Properties");
            String apn = String.class.cast(settings.get("apn"));
            Set<BearerIpType> bearerTypes = MMBearerIpFamily
                    .toBearerIpTypeFromBitMask(UInt32.class.cast(settings.get("ip-type")));
            long bytesTransmitted = 0L;
            long bytesReceived = 0L;
            try {
                Map<String, Object> stats = bearerProperties.Get(MM_BEARER_BUS_NAME, "Stats");
                bytesTransmitted = UInt64.class.cast(stats.get("tx-bytes")).longValue();
                bytesReceived = UInt64.class.cast(stats.get("rx-bytes")).longValue();
            } catch (DBusExecutionException e) {
                logger.warn("Bearer statistics not found.");
            }
            bearers.add(new Bearer(name, connected, apn, bearerTypes, bytesTransmitted, bytesReceived));
        }
        return bearers;
    }

    private static int getSignalQuality(Properties properties) {
        Object[] signalQuality = properties.Get(MM_MODEM_BUS_NAME, "SignalQuality");
        if (signalQuality.length >= 1) {
            return UInt32.class.cast(signalQuality[0]).intValue();
        }
        return 0;
    }

    private static void fill3gppProperties(ModemInterfaceStatusBuilder builder, Properties properties) {
        try {
            builder.withRegistrationStatus(MMModem3gppRegistrationState
                    .toRegistrationStatus(properties.Get(MM_MODEM_3GPP_BUS_NAME, "RegistrationState")));
            builder.withOperatorName(properties.Get(MM_MODEM_3GPP_BUS_NAME, "OperatorName"));
        } catch (DBusExecutionException e) {
            logger.warn("3gpp properties not found.");
        }
    }

    private static byte[] getMacAddressBytes(String macAddress) {
        if (Objects.isNull(macAddress) || macAddress.isEmpty()) {
            return EMPTY_MAC_ADDRESS_BYTES;
        }
        String[] macAddressParts = macAddress.split(":");
        byte[] macAddressBytes = new byte[6];
        try {
            if (macAddressParts.length == 6) {
                for (int i = 0; i < 6; i++) {
                    Integer hex = Integer.parseInt(macAddressParts[i], 16);
                    macAddressBytes[i] = hex.byteValue();
                }
            } else {
                return EMPTY_MAC_ADDRESS_BYTES;
            }
        } catch (NumberFormatException e) {
            logger.warn("Cannot parse Hardware address", e);
            return EMPTY_MAC_ADDRESS_BYTES;
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

    /**
     * The conversion from signal quality [%] and signal strength [dBm]
     * is performed using the algorithm presented in
     * https://github.com/torvalds/linux/blob/c9c3395d5e3dcc6daee66c6908354d47bf98cb0c/drivers/net/wireless/intel/ipw2x00/ipw2200.c#L4305
     * and
     * https://github.com/torvalds/linux/blob/c9c3395d5e3dcc6daee66c6908354d47bf98cb0c/drivers/net/wireless/intel/ipw2x00/ipw2200.c#L11664
     * 
     * signalQuality = (100 * DeltaRSSI^2 - (RSSIMax - RSSI)*(15*DeltaRSSI + 62*DeltaRSSI))/DeltaRSSI^2
     */
    protected static int convertToWifiSignalStrength(int signalQuality) {
        int rssiMax = -20;
        int rssiMin = -85;
        int deltaRssi = rssiMax - rssiMin;
        return Math.round(rssiMax + (signalQuality - 100) * deltaRssi / 77F);
    }

    /**
     * Since it seems that the modem signal quality [%] is derived by the output of
     * the command at+csq (https://m2msupport.net/m2msupport/atcsq-signal-quality/),
     * the following method converts the signalQuality to the csq value and finally
     * convert this to the signal strength [dBm]:
     * 
     * signalQuality = 100/30 * csq
     * signalStrength = -113 + 2 * csq
     */
    protected static int convertToModemSignalStrength(int signalQuality) {
        float csqValue = signalQuality * 30F / 100F;
        return Math.round(-113F + 2F * csqValue);
    }
}
