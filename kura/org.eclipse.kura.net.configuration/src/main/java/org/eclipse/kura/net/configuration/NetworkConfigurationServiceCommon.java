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
package org.eclipse.kura.net.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.usb.UsbNetDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationServiceCommon {

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServiceCommon.class);
    public static final String PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";

    private static final String PREFIX = "net.interface.";
    private static final String NET_INTERFACES = "net.interfaces";
    private static final Pattern COMMA = Pattern.compile(",");
    private static ObjectFactory objectFactory = new ObjectFactory();

    private NetworkConfigurationServiceCommon() {
        // Do nothing...
    }

    public static Tocd getDefinition(Map<String, Object> properties, Optional<List<UsbNetDevice>> usbNetDevices)
            throws KuraException {
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("NetworkConfigurationService");
        tocd.setId(PID);
        tocd.setDescription("Network Configuration Service");

        Tad tad = buildAttributeDefinition(NET_INTERFACES, NetworkConfigurationPropertyNames.PLATFORM_INTERFACES,
                Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(true);
        tocd.addAD(tad);

        // Get the network interfaces on the platform
        try {
            Set<String> networkInterfaceNames = getNetworkInterfaceNamesInConfig(properties);
            for (String ifaceName : networkInterfaceNames) {
                // get the current configuration for this interface

                Optional<NetInterfaceType> type = getNetworkTypeFromProperties(ifaceName, properties);

                if (!type.isPresent()) {
                    logger.warn("failed to compute the interface type for {}", ifaceName);
                    continue;
                }

                switch (type.get()) {
                case LOOPBACK:
                    getLoopbackDefinition(tocd, ifaceName);
                    break;
                case ETHERNET:
                    usbNetDevices.ifPresent(usbNetDevice -> getUsbDeviceDefinition(usbNetDevice,
                            tocd, ifaceName));
                    getInterfaceCommonDefinition(tocd, ifaceName);
                    getDnsDefinition(tocd, ifaceName);
                    getDhcpServerDefinition(tocd, ifaceName);
                    break;
                case WIFI:
                    usbNetDevices.ifPresent(usbNetDevice -> getUsbDeviceDefinition(usbNetDevice,
                            tocd, ifaceName));
                    getInterfaceCommonDefinition(tocd, ifaceName);
                    getDnsDefinition(tocd, ifaceName);
                    getDhcpServerDefinition(tocd, ifaceName);
                    getWifiDefinition(tocd, ifaceName);
                    break;
                case MODEM:
                    usbNetDevices.ifPresent(usbNetDevice -> getUsbDeviceDefinition(usbNetDevice,
                            tocd, ifaceName));
                    getInterfaceCommonDefinition(tocd, ifaceName);
                    getDnsDefinition(tocd, ifaceName);
                    getModemDefinition(tocd, ifaceName);
                    break;
                case VLAN:
                    usbNetDevices.ifPresent(usbNetDevice -> getUsbDeviceDefinition(usbNetDevice,
                            tocd, ifaceName));
                    getInterfaceCommonDefinition(tocd, ifaceName);
                    getDnsDefinition(tocd, ifaceName);
                    getDhcpServerDefinition(tocd, ifaceName);
                    getVlanDefinition(tocd, ifaceName);
                    break;
                default:
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }

        return tocd;
    }

    public static ComponentConfiguration getConfiguration(final String pid, final Map<String, Object> properties,
            final Optional<List<UsbNetDevice>> usbNetDevices) throws KuraException {
        final Tocd definition = getDefinition(properties, usbNetDevices);

        return new ComponentConfigurationImpl(pid, definition, convertStringsToPasswords(definition, properties));
    }

    private static Map<String, Object> convertStringsToPasswords(final Tocd ocd, final Map<String, Object> properties) {
        Optional<Map<String, Object>> result = Optional.empty();

        for (final AD ad : ocd.getAD()) {
            final String key = ad.getId();
            final Object value = result.orElse(properties).get(ad.getId());

            if (ad.getType() == Scalar.PASSWORD && value instanceof String) {
                if (!result.isPresent()) {
                    result = Optional.of(new HashMap<>(properties));
                }
                result.get().put(key, new Password(((String) value).toCharArray()));
            }
        }

        return result.orElse(properties);
    }

    private static void getModemDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_ENABLED, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.idle", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_IDLE, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.username", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_USERNAME, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.password", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PASSWORD, Tscalar.PASSWORD));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.pdpType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PDP_TYPE, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.maxFail", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_MAX_FAIL, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.authType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_AUTH_TYPE, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.lpcEchoInterval", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_LPC_ECHO_INTERVAL, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.activeFilter", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_ACTIVE_FILTER, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.lpcEchoFailure", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_LPC_ECHO_FAILURE, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.diversityEnabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_DIVERSITY_ENABLED, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.resetTimeout", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_RESET_TIMEOUT, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.gpsEnabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_GPS_ENABLED, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.persist", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PERSIST, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.apn", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_APN, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dialString", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_DIAL_STRING, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.holdoff", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_HOLDOFF, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.pppNum", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PPP_NUM, Tscalar.INTEGER));
    }

    private static void getWifiDefinition(Tocd tocd, String ifaceName) {
        getWifiCommonDefinition(tocd, ifaceName);
        getWifiInfraDefinition(tocd, ifaceName);
        getWifiMasterDefinition(tocd, ifaceName);
    }

    private static void getWifiMasterDefinition(Tocd tocd, String ifaceName) {
        // MASTER
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.ssid", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_SSID, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.broadcast", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_BROADCAST_ENABLED, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.radioMode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_RADIO_MODE, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.securityType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_SECURITY_TYPE, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.passphrase", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_PASSPHRASE, Tscalar.PASSWORD));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.channel", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_CHANNEL, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.ignoreSSID", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_IGNORE_SSID, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.pairwiseCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_PAIRWISE_CIPHERS, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.master.groupCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_GROUP_CIPHERS, Tscalar.STRING));
    }

    private static void getWifiInfraDefinition(Tocd tocd, String ifaceName) {
        // INFRA
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.ssid", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_SSID, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.radioMode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_RADIO_MODE, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.securityType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_SECURITY_TYPE, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.passphrase", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PASSPHRASE, Tscalar.PASSWORD));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.pairwiseCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.groupCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_GROUP_CIPHERS, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.channel", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_CHANNEL, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.bgscan", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_BGSCAN, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.pingAccessPoint", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PING_AP, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.infra.ignoreSSID", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_IGNORE_SSID, Tscalar.BOOLEAN));
    }

    private static void getWifiCommonDefinition(Tocd tocd, String ifaceName) {
        // Common
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.wifi.capabilities", ifaceName),
                NetworkConfigurationPropertyNames.WIFI_CAPABILITIES, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.wifi.mode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MODE, Tscalar.STRING));
    }

    private static void getDnsDefinition(Tocd tocd, String ifaceName) {
        getIp4DnsDefinition(tocd, ifaceName);
        getIp6DnsDefinition(tocd, ifaceName);
    }

    private static void getIp4DnsDefinition(Tocd tocd, String ifaceName) {
        Tad tad;
        // DNS and WINS
        tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.ip4.dnsServers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DNS_SERVERS, Tscalar.STRING);
        tad.setCardinality(10000);
        tocd.addAD(tad);

        tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.ip4.winsServers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_WINS_SERVERS, Tscalar.STRING);
        tad.setCardinality(10000);
        tocd.addAD(tad);

    }

    private static void getIp6DnsDefinition(Tocd tocd, String ifaceName) {
        Tad tad;
        // DNS
        tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.dnsServers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_DNS_SERVERS, Tscalar.STRING);
        tad.setCardinality(10000);
        tocd.addAD(tad);

    }

    private static void getDhcpServerDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_ENABLED, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.defaultLeaseTime", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.maxLeaseTime", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME, Tscalar.INTEGER));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.prefix", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_PREFIX, Tscalar.SHORT));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.rangeStart", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_RANGE_START, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.rangeEnd", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_RANGE_END, Tscalar.STRING));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpServer4.passDns", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_PASS_DNS, Tscalar.BOOLEAN));

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.nat.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED, Tscalar.BOOLEAN));
    }

    private static void getInterfaceCommonDefinition(Tocd tocd, String ifaceName) {
        addTypeDefinition(tocd, ifaceName);
        addMtuDefinition(tocd, ifaceName);
        addAutoconnectDefinition(tocd, ifaceName);

        addIp4InterfaceCommonDefinition(tocd, ifaceName);
        addIp6InterfaceCommonDefinition(tocd, ifaceName);

        addIp6AddressGenerationModeDefinition(tocd, ifaceName);
        addIp6PrivacyDefinition(tocd, ifaceName);

    }
    
    private static void getVlanDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.vlan.parent", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_PARENT, Tscalar.STRING));
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.vlan.id", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_ID, Tscalar.INTEGER));
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.vlan.ingress", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_INGRESS_MAP, Tscalar.STRING));
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.vlan.egress", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_EGRESS_MAP, Tscalar.STRING));
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.vlan.flags", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_FLAGS, Tscalar.INTEGER));
        
    }

    private static void addIp4InterfaceCommonDefinition(Tocd tocd, String ifaceName) {

        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.dhcpClient4.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_CLIENT_ENABLED, Tscalar.BOOLEAN);
        tad.setRequired(true);
        tocd.addAD(tad);

        addIp4AddressDefinition(tocd, ifaceName);
        addIp4PrefixDefinition(tocd, ifaceName);

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip4.gateway", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_GATEWAY, Tscalar.STRING));
        addIp4StatusDefinition(tocd, ifaceName);

    }

    private static void addIp6InterfaceCommonDefinition(Tocd tocd, String ifaceName) {

        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.address.method", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_ADDRESS_METHOD, Tscalar.STRING);
        tad.setRequired(true);
        tocd.addAD(tad);

        addIp6AddressDefinition(tocd, ifaceName);
        addIp6PrefixDefinition(tocd, ifaceName);

        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.gateway", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_GATEWAY, Tscalar.STRING));
        addIp4StatusDefinition(tocd, ifaceName);

    }

    private static void addIp6AddressGenerationModeDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.addr.gen.mode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_ADDRESS_GENERATION_METHOD, Tscalar.STRING));
    }

    private static void addIp6PrivacyDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.privacy", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_PRIVACY, Tscalar.STRING));
    }

    private static void getLoopbackDefinition(Tocd tocd, String ifaceName) {
        addTypeDefinition(tocd, ifaceName);
        addMtuDefinition(tocd, ifaceName);
        addAutoconnectDefinition(tocd, ifaceName);
        addIp4AddressDefinition(tocd, ifaceName);
        addIp4PrefixDefinition(tocd, ifaceName);
        addIp4StatusDefinition(tocd, ifaceName);
        addIp6AddressDefinition(tocd, ifaceName);
        addIp6PrefixDefinition(tocd, ifaceName);
        addIp6StatusDefinition(tocd, ifaceName);
    }

    private static void addTypeDefinition(Tocd tocd, String ifaceName) {
        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.type", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_TYPE, Tscalar.STRING);
        tad.setRequired(true);
        tocd.addAD(tad);
    }

    private static void addIp4StatusDefinition(Tocd tocd, String ifaceName) {
        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.ip4.status", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_STATUS, Tscalar.STRING);
        tad.setRequired(true);
        tocd.addAD(tad);
    }

    private static void addIp6StatusDefinition(Tocd tocd, String ifaceName) {
        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.status", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_STATUS, Tscalar.STRING);
        tad.setRequired(true);
        tocd.addAD(tad);
    }

    private static void addAutoconnectDefinition(Tocd tocd, String ifaceName) {
        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.autoconnect", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_AUTOCONNECT, Tscalar.BOOLEAN);
        tad.setRequired(true);
        tocd.addAD(tad);
    }

    private static void addMtuDefinition(Tocd tocd, String ifaceName) {
        Tad tad = buildAttributeDefinition(String.format(PREFIX + "%s.config.mtu", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MTU, Tscalar.INTEGER);
        tad.setRequired(true);
        tocd.addAD(tad);
    }

    private static void addIp4PrefixDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip4.prefix", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_PREFIX, Tscalar.SHORT));
    }

    private static void addIp6PrefixDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.prefix", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_PREFIX, Tscalar.SHORT));
    }

    private static void addIp4AddressDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip4.address", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_ADDRESS, Tscalar.STRING));
    }

    private static void addIp6AddressDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.config.ip6.address", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_ADDRESS, Tscalar.STRING));
    }

    private static void getUsbDeviceDefinition(List<UsbNetDevice> usbNetDevices, Tocd tocd, String ifaceName) {
        if (usbNetDevices != null) {
            Optional<UsbNetDevice> usbNetDeviceOptional = usbNetDevices.stream()
                    .filter(usbNetDevice -> usbNetDevice.getInterfaceName().equals(ifaceName)).findFirst();
            if (usbNetDeviceOptional.isPresent()) {
                // found a match - add the read only fields?
                tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.usb.port", ifaceName),
                        NetworkConfigurationPropertyNames.USB_PORT, Tscalar.STRING));

                tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.usb.manufacturer", ifaceName),
                        NetworkConfigurationPropertyNames.USB_MANUFACTURER, Tscalar.STRING));

                tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.usb.product", ifaceName),
                        NetworkConfigurationPropertyNames.USB_PRODUCT, Tscalar.STRING));

                tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.usb.manufacturer.id", ifaceName),
                        NetworkConfigurationPropertyNames.USB_MANUFACTURER_ID, Tscalar.STRING));

                tocd.addAD(buildAttributeDefinition(String.format(PREFIX + "%s.usb.product.id", ifaceName),
                        NetworkConfigurationPropertyNames.USB_PRODUCT_ID, Tscalar.STRING));
            }
        }
    }

    public static Set<String> getNetworkInterfaceNamesInConfig(final Map<String, Object> properties) {
        return Optional
                .ofNullable(properties).map(p -> p.get(NET_INTERFACES)).map(s -> COMMA.splitAsStream((String) s)
                        .filter(p -> !p.trim().isEmpty()).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    public static Optional<NetInterfaceType> getNetworkTypeFromProperties(final String interfaceName,
            final Map<String, Object> properties) {
        Optional<NetInterfaceType> interfaceType = Optional.empty();
        Object interfaceTypeString = properties.get(String.format("net.interface.%s.type", interfaceName));
        if (interfaceTypeString != null) {
            interfaceType = Optional.of(NetInterfaceType.valueOf((String) interfaceTypeString));
        }
        return interfaceType;
    }

    public static Set<String> getWanInterfacesInConfig(final Map<String, Object> properties) {

        Set<String> wanInterfaceResult = new HashSet<>();

        wanInterfaceResult.addAll(getIp4WanInterfacesInConfig(properties));
        wanInterfaceResult.addAll(getIp6WanInterfacesInConfig(properties));

        return wanInterfaceResult;

    }

    public static Set<String> getIp4WanInterfacesInConfig(final Map<String, Object> properties) {
        return getNetworkInterfaceNamesInConfig(properties).stream()
                .filter(p -> NetInterfaceStatus.netIPv4StatusEnabledWAN.name()
                        .equals(properties.get(PREFIX + p + ".config.ip4.status")))
                .collect(Collectors.toSet());
    }

    public static Set<String> getIp6WanInterfacesInConfig(final Map<String, Object> properties) {
        return getNetworkInterfaceNamesInConfig(properties).stream()
                .filter(p -> NetInterfaceStatus.netIPv6StatusEnabledWAN.name()
                        .equals(properties.get(PREFIX + p + ".config.ip6.status")))
                .collect(Collectors.toSet());
    }

    private static Tad buildAttributeDefinition(String propertyName, NetworkConfigurationPropertyNames messageId,
            Tscalar type) {
        Tad tad = objectFactory.createTad();
        tad.setId(propertyName);
        tad.setName(propertyName);
        tad.setType(type);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkConfigurationMessages.getMessage(messageId));
        return tad;
    }

}
