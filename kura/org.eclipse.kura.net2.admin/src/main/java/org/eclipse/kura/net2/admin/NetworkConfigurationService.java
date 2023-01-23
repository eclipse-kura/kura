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
package org.eclipse.kura.net2.admin;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net2.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net2.admin.writer.DhcpServerConfigWriter;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationService implements SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationService.class);
    public static final String PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";

    private static final String PREFIX = "net.interface.";
    private static final String CONFIG_IP4_PREFIX = ".config.ip4.prefix";
    private static final String CONFIG_IP4_ADDRESS = ".config.ip4.address";
    private static final String CONFIG_DRIVER = ".config.driver";
    private static final String CONFIG_AUTOCONNECT = ".config.autoconnect";
    private static final String CONFIG_MTU = ".config.mtu";
    private static final String NET_INTERFACES = "net.interfaces";
    private static final String MODIFIED_INTERFACE_NAMES = "modified.interface.names";
    private static final String MODEM_PORT_REGEX = "^\\d+-\\d+";
    private static final Pattern PPP_INTERFACE = Pattern.compile("ppp[0-9]+");
    private static final Pattern COMMA = Pattern.compile(",");

    private NetworkService networkService;
    private EventAdmin eventAdmin;
    private UsbService usbService;
    private CommandExecutorService commandExecutorService;
    private CryptoService cryptoService;
    private ConfigurationService configurationService;

    private LinuxNetworkUtil linuxNetworkUtil;

    private NetworkProperties networkProperties;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void unsetNetworkService(NetworkService networkService) {
        if (this.networkService.equals(networkService)) {
            this.networkService = null;
        }
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        if (this.eventAdmin.equals(eventAdmin)) {
            this.eventAdmin = null;
        }
    }

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        if (this.usbService.equals(usbService)) {
            this.usbService = null;
        }
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.commandExecutorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        if (this.commandExecutorService.equals(executorService)) {
            this.commandExecutorService = null;
        }
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        if (this.cryptoService.equals(cryptoService)) {
            this.cryptoService = null;
        }
    }

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------
    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("Activate NetworkConfigurationService...");

        this.linuxNetworkUtil = new LinuxNetworkUtil(this.commandExecutorService);
        if (properties == null) {
            logger.debug("Received null properties...");
        } else {
            logger.debug("Properties... {}", properties);
            // for now apply the configuration in any case. Is the property the best
            // approach to manage the incremental updates?
            this.networkProperties = new NetworkProperties(discardModifiedNetworkInterfaces(new HashMap<>(properties)));
            update(this.networkProperties.getProperties());
        }
    }

    public void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivate NetworkConfigurationService...");
    }

    protected NetInterfaceType getNetworkTypeFromSystem(String interfaceName) throws KuraException {
        // Do be done with NM...
        if (isUsbPort(interfaceName)) {
            return this.linuxNetworkUtil.getType(this.networkService.getModemPppInterfaceName(interfaceName));
        } else {
            return this.linuxNetworkUtil.getType(interfaceName);
        }
    }

    private Optional<NetInterfaceType> getNetworkTypeFromProperties(final String interfaceName) {
        Optional<NetInterfaceType> type = Optional.empty();
        Optional<String> typeString = this.networkProperties.getOpt(String.class, "net.interface.%s.type",
                interfaceName);
        if (typeString.isPresent()) {
            type = Optional.of(NetInterfaceType.valueOf(typeString.get()));
        }
        return type;
    }

    public synchronized void update(Map<String, Object> receivedProperties) {
        logger.debug("Update NetworkConfigurationService...");
        if (receivedProperties == null) {
            logger.debug("Received null properties...");
            return;
        }

        final Map<String, Object> modifiedProps = migrateModemConfigs(receivedProperties); // for backward
                                                                                           // compatibility
        final Set<String> interfaces = getNetworkInterfaceNamesInConfig(modifiedProps);

        try {
            for (final String interfaceName : interfaces) {
                NetInterfaceType type = getNetworkTypeFromSystem(interfaceName);
                // at least only if the type is not in the properties
                setInterfaceType(modifiedProps, interfaceName, type); // do we need to retrieve the interface type from
                                                                      // the system?
                if (NetInterfaceType.MODEM.equals(type)) {
                    setModemPppNumber(modifiedProps, interfaceName);
                    setModemUsbDeviceProperties(modifiedProps, interfaceName);
                }
            }

            final boolean changed = checkWanInterfaces(this.networkProperties.getProperties(), modifiedProps);
            mergeNetworkConfigurationProperties(modifiedProps, this.networkProperties.getProperties());

            decryptAndConvertPasswordProperties(modifiedProps);
            this.networkProperties = new NetworkProperties(discardModifiedNetworkInterfaces(modifiedProps));

            // networkManager.applyConfiguration
            writeDhcpServerConfiguration(interfaces);

            this.eventAdmin.postEvent(new NetworkConfigurationChangeEvent(modifiedProps)); // not sure about the
            // management
            // of the modifiedprops...

            if (changed) {
                this.configurationService.snapshot();
            }
        } catch (

        KuraException e) {
            logger.error("Failed to apply network configuration", e);
        }
    }

    private boolean checkWanInterfaces(final Map<String, Object> oldProperties,
            final Map<String, Object> newProperties) {
        final Set<String> oldWanInterfaces = getWanInterfaces(oldProperties);
        final Set<String> newWanInterfaces = getWanInterfaces(newProperties);

        boolean changed = false;

        if (newWanInterfaces.stream().anyMatch(i -> !oldWanInterfaces.contains(i))) {
            Set<String> allNetworkInterfaces;
            try {
                allNetworkInterfaces = this.networkService.getNetworkInterfaces().stream()
                        .map(this::probeNetInterfaceConfigName)
                        .collect(Collectors.toSet());
            } catch (KuraException e) {
                logger.warn("failed to retrieve network interface names", e);
                return changed;
            }

            for (final String intf : newWanInterfaces) {
                if (!allNetworkInterfaces.contains(intf)) {
                    logger.info(
                            "A new interface has been enabled for WAN and interface {} is also enabled for WAN but it is not currently available."
                                    + " Disabling it to avoid potentially unwanted multiple interfaces enabled for WAN.",
                            intf);
                    newProperties.put(PREFIX + intf + ".config.ip4.status",
                            NetInterfaceStatus.netIPv4StatusDisabled.name());

                    changed = true;
                }
            }
        }

        return changed;
    }

    private Set<String> getWanInterfaces(final Map<String, Object> properties) {
        return getNetworkInterfaceNamesInConfig(properties).stream()
                .filter(p -> NetInterfaceStatus.netIPv4StatusEnabledWAN
                        .name().equals(properties.get(PREFIX + p + ".config.ip4.status")))
                .collect(Collectors.toSet());
    }

    protected void setModemPppNumber(Map<String, Object> modifiedProps, String interfaceName) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX).append(interfaceName).append(".config.pppNum");
        Integer pppNum = Integer.valueOf(this.networkService.getModemPppInterfaceName(interfaceName).substring(3));
        modifiedProps.put(sb.toString(), pppNum);
    }

    protected void setInterfaceType(Map<String, Object> modifiedProps, String interfaceName, NetInterfaceType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX).append(interfaceName).append(".type");
        modifiedProps.put(sb.toString(), type.toString());
    }

    protected void setModemUsbDeviceProperties(Map<String, Object> modifiedProps, String interfaceName) {
        // Can we use the NM?
        Optional<ModemDevice> modemOptional = this.networkService.getModemDevice(interfaceName);
        if (modemOptional.isPresent() && modemOptional.get() instanceof UsbModemDevice) {
            String prefix = PREFIX + interfaceName + ".";
            UsbModemDevice usbModemDevice = (UsbModemDevice) modemOptional.get();
            modifiedProps.put(prefix + "usb.vendor.id", usbModemDevice.getVendorId());
            modifiedProps.put(prefix + "usb.vendor.name", usbModemDevice.getManufacturerName());
            modifiedProps.put(prefix + "usb.product.id", usbModemDevice.getProductId());
            modifiedProps.put(prefix + "usb.product.name", usbModemDevice.getProductName());
            modifiedProps.put(prefix + "usb.busNumber", usbModemDevice.getUsbBusNumber());
            modifiedProps.put(prefix + "usb.devicePath", usbModemDevice.getUsbDevicePath());
        }
    }

    private boolean isEncrypted(String password) {
        try {
            this.cryptoService.decryptAes(password.toCharArray());
            return true;
        } catch (Exception unableToDecryptAes) {
            return false;
        }
    }

    private Password decryptPassword(String password) throws KuraException {
        String decryptedPassword = "";
        if (!password.isEmpty()) {
            if (isEncrypted(password)) {
                decryptedPassword = new String(this.cryptoService.decryptAes(password.toCharArray()));
            } else {
                decryptedPassword = password;
            }
        }
        return new Password(decryptedPassword);
    }

    private void decryptAndConvertPasswordProperties(Map<String, Object> modifiedProps) throws KuraException {
        for (Entry<String, Object> prop : modifiedProps.entrySet()) {
            if (prop.getKey().contains("passphrase") || prop.getKey().contains("password")) {

                Object value = prop.getValue();

                if (value instanceof Password) {
                    modifiedProps.put(prop.getKey(), decryptPassword(((Password) value).toString()));
                } else if (value instanceof String) {
                    modifiedProps.put(prop.getKey(), decryptPassword(value.toString()));
                } else {
                    modifiedProps.put(prop.getKey(), value);
                }
            }
        }
    }

    @Override
    public synchronized ComponentConfiguration getConfiguration() throws KuraException {

        return new ComponentConfigurationImpl(PID, getDefinition(),
                this.networkProperties.getProperties());
    }

    private void mergeNetworkConfigurationProperties(final Map<String, Object> source, final Map<String, Object> dest) {
        final Set<String> interfaces = getNetworkInterfaceNamesInConfig(source);
        interfaces.addAll(getNetworkInterfaceNamesInConfig(dest));

        dest.putAll(source);
        dest.put(NET_INTERFACES, interfaces.stream().collect(Collectors.joining(",")));
    }

    private String probeNetInterfaceConfigName(NetInterface<? extends NetInterfaceAddress> netInterface) {
        final Set<String> interfaceNamesInConfig = getNetworkInterfaceNamesInConfig(
                this.networkProperties.getProperties());

        final Optional<String> usbPort = Optional.ofNullable(netInterface.getUsbDevice()).map(UsbDevice::getUsbPort);

        if (usbPort.isPresent() && interfaceNamesInConfig.contains(usbPort.get())) {
            return usbPort.get();
        } else if (interfaceNamesInConfig.contains(netInterface.getName())) {
            return netInterface.getName();
        }
        return usbPort.orElse(netInterface.getName());
    }

    private Set<String> getNetworkInterfaceNamesInConfig(final Map<String, Object> properties) {
        return Optional.ofNullable(properties).map(p -> p.get(NET_INTERFACES))
                .map(s -> COMMA.splitAsStream((String) s).filter(p -> !p.trim().isEmpty())
                        .collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
    }

    private void writeDhcpServerConfiguration(Set<String> interfaceNames) { // do we
                                                                            // need the
                                                                            // modified
                                                                            // properties?
        interfaceNames.forEach(interfaceName -> {
            if (isDhcpServerValid(interfaceName)) {
                DhcpServerConfigWriter dhcpServerConfigWriter = new DhcpServerConfigWriter(interfaceName,
                        this.networkProperties);
                try {
                    dhcpServerConfigWriter.writeConfiguration();
                } catch (UnknownHostException | KuraException e) {
                    logger.error("Failed to write DHCP Server configuration", e);
                }
            }
        });
    }

    private boolean isDhcpServerValid(String interfaceName) {
        boolean isValid = false;
        Optional<NetInterfaceType> type = getNetworkTypeFromProperties(interfaceName);
        Optional<Boolean> isDhcpServerEnabled = this.networkProperties.getOpt(Boolean.class,
                "net.interface.%s.config.dhcpServer4.enabled", interfaceName);
        Optional<NetInterfaceStatus> status = getNetInterfaceStatus(interfaceName);

        if (type.isPresent() && (NetInterfaceType.ETHERNET.equals(type.get())
                || NetInterfaceType.WIFI.equals(type.get()))
                && Boolean.TRUE
                        .equals(isDhcpServerEnabled.isPresent() && isDhcpServerEnabled.get() && status.isPresent())
                && !status.get().equals(NetInterfaceStatus.netIPv4StatusL2Only)) {
            isValid = true;
        }
        return isValid;
    }

    private Optional<NetInterfaceStatus> getNetInterfaceStatus(String interfaceName) {
        Optional<String> interfaceStatus = this.networkProperties.getOpt(String.class,
                "net.interface.%s.config.ip4.status", interfaceName);
        if (interfaceStatus.isPresent()) {
            return Optional.of(NetInterfaceStatus.valueOf(interfaceStatus.get()));
        } else {
            return Optional.empty();
        }
    }

    private Tocd getDefinition() throws KuraException {
        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("NetworkConfigurationService");
        tocd.setId("org.eclipse.kura.net.admin.NetworkConfigurationService"); // for backward compatibility, let's keep
                                                                              // the old id
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.PLATFORM_INTERFACES));
        tocd.addAD(tad);

        // Get the network interfaces on the platform
        try {
            Set<String> networkInterfaceNames = getNetworkInterfaceNamesInConfig(
                    this.networkProperties.getProperties());
            for (String ifaceName : networkInterfaceNames) {
                // get the current configuration for this interface

                Optional<NetInterfaceType> type = getNetworkTypeFromProperties(ifaceName);

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

    private static Map<String, Object> discardModifiedNetworkInterfaces(final Map<String, Object> properties) {
        if (!properties.containsKey(MODIFIED_INTERFACE_NAMES)) {
            return properties;
        }

        final Map<String, Object> result = new HashMap<>(properties);
        result.remove(MODIFIED_INTERFACE_NAMES);
        return result;
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
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_SSID));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_BROADCAST_ENABLED));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_HARDWARE_MODE));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_HARDWARE_MODE));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_SECURITY_TYPE));
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
                NetworkConfigurationMessages
                        .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_PASSPHRASE));
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
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MASTER_CHANNEL));
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
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_SSID));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_HARDWARE_MODE));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_HARDWARE_MODE));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_SECURITY_TYPE));
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
                NetworkConfigurationMessages
                        .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PASSPHRASE));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_GROUP_CIPHERS));
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
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_INFRA_CHANNEL));
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.WIFI_CAPABILITIES));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.mode").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.wifi.mode").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_WIFI_MODE));
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_DNS_SERVERS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.winsServers").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.winsServers").toString());
        tad.setType(Tscalar.STRING);
        tad.setCardinality(10000);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_WINS_SERVERS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.enabled").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.enabled").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_ENABLED));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME));
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
                NetworkConfigurationMessages
                        .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_PREFIX));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_RANGE_START));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_RANGE_END));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.passDns").toString());
        tad.setName(
                new StringBuffer().append(PREFIX).append(ifaceName).append(".config.dhcpServer4.passDns").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_PASS_DNS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.nat.enabled").toString());
        tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".config.nat.enabled").toString());
        tad.setType(Tscalar.BOOLEAN);
        tad.setCardinality(0);
        tad.setRequired(false);
        tad.setDefault("");
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED));
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
        tad.setDescription(NetworkConfigurationMessages
                .getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_DHCP_CLIENT_ENABLED));
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_GATEWAY));
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
        tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_DRIVER));
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_AUTOCONNECT));
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
        tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_MTU));
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_PREFIX));
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
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.CONFIG_IPV4_ADDRESS));
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
                tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.USB_PORT));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.manufacturer").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.manfacturer").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(
                        NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.USB_MANUFACTURER));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(
                        NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.USB_PRODUCT));
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
                        NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.USB_MANUFACTURER_ID));
                tocd.addAD(tad);

                tad = objectFactory.createTad();
                tad.setId(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product.id").toString());
                tad.setName(new StringBuffer().append(PREFIX).append(ifaceName).append(".usb.product.id").toString());
                tad.setType(Tscalar.STRING);
                tad.setCardinality(0);
                tad.setRequired(false);
                tad.setDefault("");
                tad.setDescription(
                        NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.USB_PRODUCT_ID));
                tocd.addAD(tad);
            }
        }
    }

    private boolean isUsbPort(String interfaceName) {
        return interfaceName.split("\\.")[0].matches(MODEM_PORT_REGEX);
    }

    private Map<String, Object> migrateModemConfigs(final Map<String, Object> properties) {

        final Map<String, Object> result = new HashMap<>(properties);
        final Set<String> interfaceNames = getNetworkInterfaceNamesInConfig(properties);
        final Set<String> resultInterfaceNames = new HashSet<>();

        for (final String existingInterfaceName : interfaceNames) {
            if (!PPP_INTERFACE.matcher(existingInterfaceName).matches()) {
                resultInterfaceNames.add(existingInterfaceName);
                continue;
            }

            logger.info("migrating configuration for interface: {}...", existingInterfaceName);

            String prefix = PREFIX + existingInterfaceName + ".";

            final Optional<String> usbBusNumber = this.networkProperties.getOpt(String.class,
                    "net.interface.%s.usb.busNumber",
                    existingInterfaceName);
            final Optional<String> usbDevicePath = this.networkProperties.getOpt(String.class,
                    "net.interface.%s.usb.devicePath",
                    existingInterfaceName);

            if (!usbBusNumber.isPresent() || !usbDevicePath.isPresent()) {
                logger.warn("failed to determine usb port for {}, skipping", existingInterfaceName);
                continue;
            }

            final String migratedInterfaceName = usbBusNumber.get() + "-" + usbDevicePath.get();

            logger.info("renaming {} to {}", existingInterfaceName, migratedInterfaceName);
            fillProperties(migratedInterfaceName, prefix, result);
            resultInterfaceNames.add(migratedInterfaceName);

            logger.info("migrating configuration for interface: {}...done", existingInterfaceName);

        }

        result.put(NET_INTERFACES, resultInterfaceNames.stream().collect(Collectors.joining(",")));
        return result;
    }

    private void fillProperties(String migratedInterfaceName, String prefix, Map<String, Object> result) {
        final String migratedPrefix = PREFIX + migratedInterfaceName + ".";

        for (final Entry<String, Object> e : this.networkProperties.getProperties().entrySet()) {
            final String key = e.getKey();

            if (key.startsWith(prefix)) {
                final String suffix = key.substring(prefix.length());
                final String migratedPropertyKey = migratedPrefix + suffix;

                final Object existingProperty = this.networkProperties.getProperties().get(migratedPropertyKey);

                if (existingProperty != null) {
                    result.put(migratedPropertyKey, existingProperty);
                } else {
                    result.put(migratedPropertyKey, e.getValue());
                }
            }
        }
    }
}
