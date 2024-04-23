/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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

        Tad tad = builder(NET_INTERFACES, NetworkConfigurationPropertyNames.PLATFORM_INTERFACES,
                Tscalar.STRING).withCardinality(10000).withRequired(true).build();
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
        tocd.addAD(builder(String.format(PREFIX + "%s.config.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_ENABLED, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_ENABLED_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.idle", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_IDLE, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_IDLE_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.username", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_USERNAME, Tscalar.STRING)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.password", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PASSWORD, Tscalar.PASSWORD)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.pdpType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PDP_TYPE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_MODEM_PDP_TYPE_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.maxFail", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_MAX_FAIL, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_MAXFAIL_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.authType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_AUTH_TYPE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_MODEM_AUTH_TYPE_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.lpcEchoInterval", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_LPC_ECHO_INTERVAL, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_LCP_ECHO_INTERVAL_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.activeFilter", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_ACTIVE_FILTER, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_MODEM_ACTIVE_FILTER_VALUE).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.lpcEchoFailure", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_LPC_ECHO_FAILURE, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_LCP_ECHO_FAILURE_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.diversityEnabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_DIVERSITY_ENABLED, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_DIVERSITY_ENABLED_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.resetTimeout", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_RESET_TIMEOUT, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_RESET_TIMEOUT_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.gpsEnabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_GPS_ENABLED, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_GPS_ENABLED_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.persist", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PERSIST, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_PERSIST_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.apn", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_APN, Tscalar.STRING)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dialString", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_DIAL_STRING, Tscalar.STRING)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.holdoff", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_HOLDOFF, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_HOLDOFF_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.pppNum", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MODEM_PPP_NUM, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_MODEM_PPP_NUMBER_VALUE)).build());
    }

    private static void getWifiDefinition(Tocd tocd, String ifaceName) {
        getWifiCommonDefinition(tocd, ifaceName);
        getWifiInfraDefinition(tocd, ifaceName);
        getWifiMasterDefinition(tocd, ifaceName);
    }

    private static void getWifiMasterDefinition(Tocd tocd, String ifaceName) {
        // MASTER
        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.ssid", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_SSID, Tscalar.STRING).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.broadcast", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_BROADCAST_ENABLED, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_WIFI_BROADCAST_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.radioMode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_RADIO_MODE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_RADIO_MODE_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.securityType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_SECURITY_TYPE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_SECURITY_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.passphrase", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_PASSPHRASE, Tscalar.PASSWORD)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.channel", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_CHANNEL, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_CHANNEL_VALUE).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.ignoreSSID", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_IGNORE_SSID, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_WIFI_IGNORE_SSID_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.pairwiseCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_PAIRWISE_CIPHERS, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_PAIRWISE_CIPHERS_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.master.groupCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_GROUP_CIPHERS, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_GROUP_CIPHERS_VALUE.name()).build());
    }

    private static void getWifiInfraDefinition(Tocd tocd, String ifaceName) {
        // INFRA
        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.ssid", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_SSID, Tscalar.STRING).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.radioMode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_RADIO_MODE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_RADIO_MODE_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.securityType", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_SECURITY_TYPE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_SECURITY_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.passphrase", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PASSPHRASE, Tscalar.PASSWORD)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.pairwiseCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_PAIRWISE_CIPHERS_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.groupCiphers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_GROUP_CIPHERS, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_GROUP_CIPHERS_VALUE.name()).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.channel", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_CHANNEL, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_CHANNEL_VALUE).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.bgscan", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_BGSCAN, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_BGSCAN_VALUE).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.pingAccessPoint", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PING_AP, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_WIFI_PING_AP_VALUE)).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.infra.ignoreSSID", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_IGNORE_SSID, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_WIFI_IGNORE_SSID_VALUE)).build());
    }

    private static void getWifiCommonDefinition(Tocd tocd, String ifaceName) {
        // Common
        tocd.addAD(builder(String.format(PREFIX + "%s.wifi.capabilities", ifaceName),
                NetworkConfigurationPropertyNames.WIFI_CAPABILITIES, Tscalar.STRING)
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.wifi.mode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_WIFI_MODE, Tscalar.STRING)
                .withDefault(NetworkConfigurationConstants.DEFAULT_WIFI_MODE.name()).build());
    }

    private static void getDnsDefinition(Tocd tocd, String ifaceName) {
        getIp4DnsDefinition(tocd, ifaceName);
        getIp6DnsDefinition(tocd, ifaceName);
    }

    private static void getIp4DnsDefinition(Tocd tocd, String ifaceName) {
        // DNS and WINS
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.dnsServers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DNS_SERVERS, Tscalar.STRING)
                .withCardinality(10000).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.winsServers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_WINS_SERVERS, Tscalar.STRING)
                .withCardinality(10000).build());
    }

    private static void getIp6DnsDefinition(Tocd tocd, String ifaceName) {
        // DNS
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.dnsServers", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_DNS_SERVERS, Tscalar.STRING)
                .withCardinality(10000).build());
    }

    private static void getDhcpServerDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_ENABLED, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_SERVER_ENABLED_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.defaultLeaseTime", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME, Tscalar.INTEGER)
                .withDefault(
                        String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.maxLeaseTime", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME, Tscalar.INTEGER)
                .withDefault(
                        String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_SERVER_MAX_LEASE_TIME_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.prefix", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_PREFIX, Tscalar.SHORT)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_SERVER_PREFIX_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.rangeStart", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_RANGE_START, Tscalar.STRING).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.rangeEnd", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_RANGE_END, Tscalar.STRING).build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpServer4.passDns", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_PASS_DNS, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_PASS_DNS_VALUE))
                .build());

        tocd.addAD(builder(String.format(PREFIX + "%s.config.nat.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED, Tscalar.BOOLEAN)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_SERVER_NAT_ENABLED_VALUE))
                .build());
    }

    private static void getInterfaceCommonDefinition(Tocd tocd, String ifaceName) {
        addTypeDefinition(tocd, ifaceName);
        addMtuDefinition(tocd, ifaceName);
        addAutoconnectDefinition(tocd, ifaceName);
        addPromiscDefinition(tocd, ifaceName);

        addIp4InterfaceCommonDefinition(tocd, ifaceName);
        addIp6InterfaceCommonDefinition(tocd, ifaceName);

        addIp6AddressGenerationModeDefinition(tocd, ifaceName);
        addIp6PrivacyDefinition(tocd, ifaceName);

    }

    private static void getVlanDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.vlan.parent", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_PARENT, Tscalar.STRING)
                .withRequired(true).build());
        tocd.addAD(builder(String.format(PREFIX + "%s.config.vlan.id", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_ID, Tscalar.INTEGER)
                .withRequired(true).build());
        tocd.addAD(builder(String.format(PREFIX + "%s.config.vlan.ingress", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_INGRESS_MAP, Tscalar.STRING)
                .build());
        tocd.addAD(builder(String.format(PREFIX + "%s.config.vlan.egress", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_EGRESS_MAP, Tscalar.STRING)
                .build());
        tocd.addAD(builder(String.format(PREFIX + "%s.config.vlan.flags", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_VLAN_FLAGS, Tscalar.INTEGER)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_VLAN_FLAGS_VALUE)).build());

    }

    private static void addIp4InterfaceCommonDefinition(Tocd tocd, String ifaceName) {

        tocd.addAD(builder(String.format(PREFIX + "%s.config.dhcpClient4.enabled", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_CLIENT_ENABLED, Tscalar.BOOLEAN)
                .withRequired(true)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_DHCP_CLIENT_ENABLED_VALUE))
                .build());

        addIp4AddressDefinition(tocd, ifaceName);
        addIp4PrefixDefinition(tocd, ifaceName);

        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.gateway", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_GATEWAY, Tscalar.STRING)
                .build());
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.mtu", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_MTU, Tscalar.INTEGER)
                .build());

        addIp4StatusDefinition(tocd, ifaceName);

    }

    private static void addIp6InterfaceCommonDefinition(Tocd tocd, String ifaceName) {

        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.address.method", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_ADDRESS_METHOD, Tscalar.STRING)
                .withRequired(true).withDefault(NetworkConfigurationConstants.DEFAULT_IPV6_ADDRESS_METHOD_VALUE)
                .build());

        addIp6AddressDefinition(tocd, ifaceName);
        addIp6PrefixDefinition(tocd, ifaceName);

        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.gateway", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_GATEWAY, Tscalar.STRING)
                .build());
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.mtu", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_MTU, Tscalar.INTEGER)
                .build());
        addIp4StatusDefinition(tocd, ifaceName);

    }

    private static void addIp6AddressGenerationModeDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.addr.gen.mode", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_ADDRESS_GENERATION_METHOD, Tscalar.STRING)
                .build());
    }

    private static void addIp6PrivacyDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.privacy", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_PRIVACY, Tscalar.STRING)
                .build());
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
        tocd.addAD(builder(String.format(PREFIX + "%s.type", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_TYPE, Tscalar.STRING)
                .withRequired(true)
                .withDefault(NetworkConfigurationConstants.DEFAULT_INTERFACE_TYPE_VALUE.name()).build());
    }

    private static void addIp4StatusDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.status", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_STATUS, Tscalar.STRING)
                .withRequired(true).withDefault(NetworkConfigurationConstants.DEFAULT_IPV4_STATUS_VALUE.name())
                .build());
    }

    private static void addIp6StatusDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.status", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_STATUS, Tscalar.STRING)
                .withRequired(true).withDefault(NetworkConfigurationConstants.DEFAULT_IPV6_STATUS_VALUE.name())
                .build());
    }

    private static void addAutoconnectDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.autoconnect", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_AUTOCONNECT, Tscalar.BOOLEAN)
                .withRequired(true).withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_AUTOCONNECT_VALUE))
                .build());
    }

    private static void addMtuDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.mtu", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_MTU, Tscalar.INTEGER)
                .build());
    }

    private static void addPromiscDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.promisc", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_PROMISC, Tscalar.INTEGER)
                .withRequired(true).withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_PROMISC_VALUE))
                .build());
    }

    private static void addIp4PrefixDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.prefix", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_PREFIX, Tscalar.SHORT)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV4_PREFIX_VALUE)).build());
    }

    private static void addIp6PrefixDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.prefix", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_PREFIX, Tscalar.SHORT)
                .withDefault(String.valueOf(NetworkConfigurationConstants.DEFAULT_IPV6_PREFIX_VALUE)).build());
    }

    private static void addIp4AddressDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip4.address", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV4_ADDRESS, Tscalar.STRING).build());
    }

    private static void addIp6AddressDefinition(Tocd tocd, String ifaceName) {
        tocd.addAD(builder(String.format(PREFIX + "%s.config.ip6.address", ifaceName),
                NetworkConfigurationPropertyNames.CONFIG_IPV6_ADDRESS, Tscalar.STRING).build());
    }

    private static void getUsbDeviceDefinition(List<UsbNetDevice> usbNetDevices, Tocd tocd, String ifaceName) {
        if (usbNetDevices != null) {
            Optional<UsbNetDevice> usbNetDeviceOptional = usbNetDevices.stream()
                    .filter(usbNetDevice -> usbNetDevice.getInterfaceName().equals(ifaceName)).findFirst();
            if (usbNetDeviceOptional.isPresent()) {
                tocd.addAD(builder(String.format(PREFIX + "%s.usb.port", ifaceName),
                        NetworkConfigurationPropertyNames.USB_PORT, Tscalar.STRING).build());

                tocd.addAD(builder(String.format(PREFIX + "%s.usb.manufacturer", ifaceName),
                        NetworkConfigurationPropertyNames.USB_MANUFACTURER, Tscalar.STRING).build());

                tocd.addAD(builder(String.format(PREFIX + "%s.usb.product", ifaceName),
                        NetworkConfigurationPropertyNames.USB_PRODUCT, Tscalar.STRING).build());

                tocd.addAD(builder(String.format(PREFIX + "%s.usb.manufacturer.id", ifaceName),
                        NetworkConfigurationPropertyNames.USB_MANUFACTURER_ID, Tscalar.STRING).build());

                tocd.addAD(builder(String.format(PREFIX + "%s.usb.product.id", ifaceName),
                        NetworkConfigurationPropertyNames.USB_PRODUCT_ID, Tscalar.STRING).build());
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

    private static TadBuilder builder(String propertyName, NetworkConfigurationPropertyNames messageId, Tscalar type) {
        return new TadBuilder(propertyName, messageId, type);
    }

    private static final class TadBuilder {
        private final String name;
        private final String description;
        private final Tscalar type;
        private Integer cardinality = 0;
        private String defaultValue = "";
        private Boolean required = false;

        public TadBuilder(String name, NetworkConfigurationPropertyNames messageId, Tscalar type) {
            this.name = name;
            this.description = NetworkConfigurationMessages.getMessage(messageId);
            this.type = type;
        }

        public TadBuilder withCardinality(Integer cardinality) {
            this.cardinality = cardinality;
            return this;
        }

        public TadBuilder withDefault(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public TadBuilder withRequired(Boolean required) {
            this.required = required;
            return this;
        }

        public Tad build() {
            Tad tad = objectFactory.createTad();
            tad.setId(this.name);
            tad.setName(this.name);
            tad.setType(this.type);
            tad.setCardinality(this.cardinality);
            tad.setRequired(this.required);
            tad.setDefault(this.defaultValue);
            tad.setDescription(this.description);
            return tad;
        }
    }

}