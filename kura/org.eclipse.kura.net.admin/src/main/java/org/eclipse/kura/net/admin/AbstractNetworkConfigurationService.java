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
package org.eclipse.kura.net.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNetworkConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNetworkConfigurationService.class);

    protected static final String PREFIX = "net.interface.";
    protected static final String CONFIG_IP4_PREFIX = ".config.ip4.prefix";
    protected static final String CONFIG_IP4_ADDRESS = ".config.ip4.address";
    protected static final String CONFIG_DRIVER = ".config.driver";
    protected static final String CONFIG_AUTOCONNECT = ".config.autoconnect";
    protected static final String CONFIG_MTU = ".config.mtu";
    protected static final String NET_INTERFACES = "net.interfaces";
    protected static final Pattern COMMA = Pattern.compile(",");

    private UsbService usbService;

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        if (this.usbService.equals(usbService)) {
            this.usbService = null;
        }
    }

    protected Tocd getDefinition(final Map<String, Object> properties) throws KuraException {
        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("NetworkConfigurationService");
        tocd.setId("org.eclipse.kura.net.admin.NetworkConfigurationService");
        tocd.setDescription("Network Configuration Service");

        // get the USB network interfaces (if any)
        List<UsbNetDevice> usbNetDevices = this.usbService.getUsbNetDevices();

        Tad tad = objectFactory.createTad();
        tad.setId(NET_INTERFACES);
        tad.setName(NET_INTERFACES);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(true);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.PLATFORM_INTERFACES));
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

                if (type.get() == NetInterfaceType.LOOPBACK) {
                    getLoopbackDefinition(objectFactory, tocd, ifaceName);
                } else if (type.get() == NetInterfaceType.ETHERNET || type.get() == NetInterfaceType.WIFI) {
                    getUsbDeviceDefinition(usbNetDevices, objectFactory, tocd, ifaceName);
                    getInterfaceCommonDefinition(objectFactory, tocd, ifaceName);
                    getDnsDefinition(objectFactory, tocd, ifaceName);
                    getWifiDefinition(type.get(), objectFactory, tocd, ifaceName);
                    // TODO - deal with USB devices (READ ONLY)
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }

        return tocd;
    }

    private void getWifiDefinition(NetInterfaceType type, ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        if (type == NetInterfaceType.WIFI) {
            getWifiCommonDefinition(objectFactory, tocd, ifaceName);
            getWifiInfraDefinition(objectFactory, tocd, ifaceName);
            getWifiMasterDefinition(objectFactory, tocd, ifaceName);
        }
    }

    private void getWifiMasterDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        // MASTER
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.ssid").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.ssid").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_SSID));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.broadcast").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.broadcast").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_BROADCAST_ENABLED));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.hardwareMode")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.hardwareMode")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_HARDWARE_MODE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.radioMode").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.radioMode").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_HARDWARE_MODE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.securityType")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.securityType")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_SECURITY_TYPE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.passphrase")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.passphrase")
                .toString());
        tad.setType(Tscalar.PASSWORD);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_PASSPHRASE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.channel").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.master.channel").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_CHANNEL));
        tocd.addAD(tad);
    }

    private void getWifiInfraDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        // INFRA
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.ssid").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.ssid").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_SSID));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.hardwareMode")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.hardwareMode")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_HARDWARE_MODE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.radioMode").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.radioMode").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_HARDWARE_MODE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.securityType")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.securityType")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_SECURITY_TYPE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.passphrase").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.passphrase").toString());
        tad.setType(Tscalar.PASSWORD);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_PASSPHRASE));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.pairwiseCiphers")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.pairwiseCiphers")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.groupCiphers")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.groupCiphers")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_GROUP_CIPHERS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.channel").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.infra.channel").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_CHANNEL));
        tocd.addAD(tad);
    }

    private void getWifiCommonDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        // Common
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".wifi.capabilities").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".wifi.capabilities").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.WIFI_CAPABILITIES));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.mode").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.mode").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MODE));
        tocd.addAD(tad);
    }

    private void getDnsDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        // DNS and WINS
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dnsServers").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dnsServers").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_DNS_SERVERS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.winsServers").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.winsServers").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_WINS_SERVERS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.enabled").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.enabled").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_ENABLED));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.defaultLeaseTime")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.defaultLeaseTime")
                .toString());
        tad.setType(Tscalar.INTEGER);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.maxLeaseTime")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.maxLeaseTime")
                .toString());
        tad.setType(Tscalar.INTEGER);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.prefix").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.prefix").toString());
        tad.setType(Tscalar.SHORT);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_PREFIX));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.rangeStart")
                .toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.rangeStart")
                .toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_RANGE_START));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.rangeEnd").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.rangeEnd").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_RANGE_END));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.passDns").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.passDns").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_PASS_DNS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.nat.enabled").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.nat.enabled").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED));
        tocd.addAD(tad);
    }

    private void getInterfaceCommonDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        addMtuDefinition(objectFactory, tocd, ifaceName);
        addAutoconnectDefinition(objectFactory, tocd, ifaceName);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpClient4.enabled").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpClient4.enabled").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages
                .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_CLIENT_ENABLED));
        tocd.addAD(tad);

        addIp4AddressDefinition(objectFactory, tocd, ifaceName);
        addIp4PrefixDefinition(objectFactory, tocd, ifaceName);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.ip4.gateway").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.ip4.gateway").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_GATEWAY));
        tocd.addAD(tad);
    }

    private void getLoopbackDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        addMtuDefinition(objectFactory, tocd, ifaceName);
        addAutoconnectDefinition(objectFactory, tocd, ifaceName);
        addDriverDefinition(objectFactory, tocd, ifaceName);
        addIp4AddressDefinition(objectFactory, tocd, ifaceName);
        addIp4PrefixDefinition(objectFactory, tocd, ifaceName);
    }

    private void addDriverDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_DRIVER).toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_DRIVER).toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_DRIVER));
        tocd.addAD(tad);
    }

    private void addAutoconnectDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_AUTOCONNECT).toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_AUTOCONNECT).toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_AUTOCONNECT));
        tocd.addAD(tad);
    }

    private void addMtuDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_MTU).toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_MTU).toString());
        tad.setType(Tscalar.INTEGER);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_MTU));
        tocd.addAD(tad);
    }

    private void addIp4PrefixDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_IP4_PREFIX).toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_IP4_PREFIX).toString());
        tad.setType(Tscalar.SHORT);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_PREFIX));
        tocd.addAD(tad);
    }

    private void addIp4AddressDefinition(ObjectFactory objectFactory, Tocd tocd, String ifaceName) {
        Tad tad;
        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_IP4_ADDRESS).toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(CONFIG_IP4_ADDRESS).toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_ADDRESS));
        tocd.addAD(tad);
    }

    private void getUsbDeviceDefinition(List<UsbNetDevice> usbNetDevices, ObjectFactory objectFactory, Tocd tocd,
            String ifaceName) {
        if (usbNetDevices != null) {
            Optional<UsbNetDevice> usbNetDeviceOptional = usbNetDevices.stream()
                    .filter(usbNetDevice -> usbNetDevice.getInterfaceName().equals(ifaceName)).findFirst();
            if (usbNetDeviceOptional.isPresent()) {
                // found a match - add the read only fields?
                Tad tad;
                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.port").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.port").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_PORT));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.manufacturer").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.manfacturer").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(
                        NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_MANUFACTURER));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_PRODUCT));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(
                        new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.manufacturer.id").toString());
                tad.setName(
                        new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.manfacturer.id").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(
                        NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_MANUFACTURER_ID));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product.id").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product.id").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(
                        NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.USB_PRODUCT_ID));
                tocd.addAD(tad);
            }
        }
    }

    protected Set<String> getNetworkInterfaceNamesInConfig(final Map<String, Object> properties) {
        return Optional.ofNullable(properties).map(p -> p.get(NET_INTERFACES))
                .map(s -> COMMA.splitAsStream((String) s).filter(p -> !p.trim().isEmpty())
                        .collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    protected Optional<NetInterfaceType> getNetworkTypeFromProperties(final String interfaceName,
            final Map<String, Object> properties) {
        return Optional.ofNullable(properties.get(PREFIX + interfaceName + ".type")).flatMap(p -> {
            try {
                return Optional.of(NetInterfaceType.valueOf((String) p));
            } catch (final Exception e) {
                return Optional.empty();
            }
        });
    }

    protected void setModemPppNumber(Map<String, Object> modifiedProps, String interfaceName, Integer pppNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX).append(interfaceName).append(".config.pppNum");
        modifiedProps.put(sb.toString(), pppNumber);
    }

    protected void setInterfaceType(Map<String, Object> modifiedProps, String interfaceName, NetInterfaceType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX).append(interfaceName).append(".type");
        modifiedProps.put(sb.toString(), type.toString());
    }

    protected void setModemUsbDeviceProperties(Map<String, Object> modifiedProps, String interfaceName,
            UsbModemDevice usbModemDevice) {
        String prefix = PREFIX + interfaceName + ".";
        modifiedProps.put(prefix + "usb.vendor.id", usbModemDevice.getVendorId());
        modifiedProps.put(prefix + "usb.vendor.name", usbModemDevice.getManufacturerName());
        modifiedProps.put(prefix + "usb.product.id", usbModemDevice.getProductId());
        modifiedProps.put(prefix + "usb.product.name", usbModemDevice.getProductName());
        modifiedProps.put(prefix + "usb.busNumber", usbModemDevice.getUsbBusNumber());
        modifiedProps.put(prefix + "usb.devicePath", usbModemDevice.getUsbDevicePath());
    }
//    }
}
