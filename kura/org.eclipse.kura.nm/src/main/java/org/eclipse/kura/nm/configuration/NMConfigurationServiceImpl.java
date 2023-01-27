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
package org.eclipse.kura.nm.configuration;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.configuration.AbstractNetworkConfigurationService;
import org.eclipse.kura.nm.configuration.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.nm.configuration.writer.DhcpServerConfigWriter;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMConfigurationServiceImpl implements SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(NMConfigurationServiceImpl.class);

    private static final String PREFIX = "net.interface.";
    private static final String NET_INTERFACES = "net.interfaces";
    private static final String MODIFIED_INTERFACE_NAMES = "modified.interface.names";
    private static final String MODEM_PORT_REGEX = "^\\d+-\\d+";
    private static final Pattern PPP_INTERFACE = Pattern.compile("ppp[0-9]+");

    private NetworkService networkService;
    private EventAdmin eventAdmin;
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

    public synchronized void update(Map<String, Object> receivedProperties) {
        logger.debug("Update NetworkConfigurationService...");
        if (receivedProperties == null) {
            logger.debug("Received null properties...");
            return;
        }

        final Map<String, Object> modifiedProps = migrateModemConfigs(receivedProperties); // for backward
                                                                                           // compatibility
        final Set<String> interfaces = AbstractNetworkConfigurationService
                .getNetworkInterfaceNamesInConfig(modifiedProps);

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
        return AbstractNetworkConfigurationService.getNetworkInterfaceNamesInConfig(properties).stream()
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

        return new ComponentConfigurationImpl(AbstractNetworkConfigurationService.PID,
                AbstractNetworkConfigurationService.getDefinition(this.networkProperties.getProperties()),
                this.networkProperties.getProperties());
    }

    private void mergeNetworkConfigurationProperties(final Map<String, Object> source, final Map<String, Object> dest) {
        final Set<String> interfaces = AbstractNetworkConfigurationService.getNetworkInterfaceNamesInConfig(source);
        interfaces.addAll(AbstractNetworkConfigurationService.getNetworkInterfaceNamesInConfig(dest));

        dest.putAll(source);
        dest.put(NET_INTERFACES, interfaces.stream().collect(Collectors.joining(",")));
    }

    private String probeNetInterfaceConfigName(NetInterface<? extends NetInterfaceAddress> netInterface) {
        final Set<String> interfaceNamesInConfig = AbstractNetworkConfigurationService.getNetworkInterfaceNamesInConfig(
                this.networkProperties.getProperties());

        final Optional<String> usbPort = Optional.ofNullable(netInterface.getUsbDevice()).map(UsbDevice::getUsbPort);

        if (usbPort.isPresent() && interfaceNamesInConfig.contains(usbPort.get())) {
            return usbPort.get();
        } else if (interfaceNamesInConfig.contains(netInterface.getName())) {
            return netInterface.getName();
        }
        return usbPort.orElse(netInterface.getName());
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
        Optional<NetInterfaceType> type = AbstractNetworkConfigurationService
                .getNetworkTypeFromProperties(interfaceName, this.networkProperties.getProperties());
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

    private boolean isUsbPort(String interfaceName) {
        return interfaceName.split("\\.")[0].matches(MODEM_PORT_REGEX);
    }

    private Map<String, Object> migrateModemConfigs(final Map<String, Object> properties) {

        final Map<String, Object> result = new HashMap<>(properties);
        final Set<String> interfaceNames = AbstractNetworkConfigurationService
                .getNetworkInterfaceNamesInConfig(properties);
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

    private static Map<String, Object> discardModifiedNetworkInterfaces(final Map<String, Object> properties) {
        if (!properties.containsKey(MODIFIED_INTERFACE_NAMES)) {
            return properties;
        }

        final Map<String, Object> result = new HashMap<>(properties);
        result.remove(MODIFIED_INTERFACE_NAMES);
        return result;
    }
}
