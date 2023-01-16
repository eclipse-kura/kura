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
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkManagerImpl extends AbstractNetworkConfigurationService implements SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(NetworkManagerImpl.class);
    public static final String PID = "org.eclipse.kura.net.admin.NetworkConfigurationService"; // ????
    private static final String MODIFIED_INTERFACE_NAMES = "modified.interface.names";
    private static final Pattern PPP_INTERFACE = Pattern.compile("ppp[0-9]+");
    private static final String MODEM_PORT_REGEX = "^\\d+-\\d+";

    private LinuxNetworkUtil linuxNetworkUtil; // remove
    private NetworkService networkService;
    private EventAdmin eventAdmin;
    private ModemManagerService modemManagerService;
    private CommandExecutorService commandExecutorService;
    private CryptoService cryptoService;
    private ConfigurationService configurationService;
    private Map<String, Object> properties = new HashMap<>();

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

    public void setModemManagerService(ModemManagerService modemManagerService) {
        logger.debug("Set the modem manager service");
        this.modemManagerService = modemManagerService;
    }

    public void unsetModemManagerService(ModemManagerService modemManagerService) {
        if (this.modemManagerService.equals(modemManagerService)) {
            logger.debug("Unset the modem manager service");
            this.modemManagerService = null;
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

    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        // initialize resources if needed
        // launch update

        logger.debug("Activating NetworkManagerImpl...");
//        initVisitors();
//
        this.linuxNetworkUtil = new LinuxNetworkUtil(this.commandExecutorService); // remove
        if (properties == null) {
            logger.debug("Got null properties...");
        } else {
            logger.debug("Properties...{}", properties);
            this.properties = discardModifiedNetworkInterfaces(new HashMap<>(properties)); // remove
            updated(this.properties);
        }
    }

    public void deactivate(ComponentContext componentContext) {
        // deactivate resources if needed

        logger.debug("Deactivating NetworkManagerImpl...");
//        this.writeVisitors = null;
    }

    public synchronized void updated(Map<String, Object> receivedProperties) {
        // check if the new properties differ from the current ones
        // select the interface that differs and apply the configuration
        // save the properties in NetworkConfiguration

        logger.debug("Updating NetworkManagerImpl...");

        if (receivedProperties == null) {
            logger.debug("Properties are null");
            return;
        }
//
        try {
            // replace pppX with usbNumber-usbPath in property name
            // do we really need it? What happen if is a DirectIP?
            final Map<String, Object> newProperties = migrateModemConfigs(receivedProperties);
//            logger.debug("new properties - updating");
//            logger.debug("modified.interface.names: {}", newProperties.get(MODIFIED_INTERFACE_NAMES));
//
            Map<String, Object> modifiedProps = new HashMap<>(newProperties);
//
            final Set<String> interfaces = getNetworkInterfaceNamesInConfig(newProperties);
//
            for (final String interfaceName : interfaces) {
                NetInterfaceType type = getNetworkType(interfaceName);
//
                // Set the type of the inteface in the properties, why?
                setInterfaceType(modifiedProps, interfaceName, type);
                if (NetInterfaceType.MODEM.equals(type)) {
                    setModemProperties(interfaceName, modifiedProps);
                }
            }
//
            // check if multiple interfaces are configured as WAN. Should it be removed,
            // since we want to support it?
            final boolean changed = checkWanInterfaces(this.properties, modifiedProps);
            // merge the old and new configurations. The new ones overwrite the old, but
            // interfaces that are not in the new are kept in the properties.
            mergeNetworkConfigurationProperties(modifiedProps, this.properties);

//            decryptPasswordProperties(modifiedProps);
            NetworkConfiguration networkConfiguration = new NetworkConfiguration(modifiedProps);

//            executeVisitors(networkConfiguration);

//            updateCurrentNetworkConfiguration();

//            this.eventAdmin.postEvent(new NetworkConfigurationChangeEvent(modifiedProps));

            this.properties = discardModifiedNetworkInterfaces(this.properties);

            if (changed) {
                this.configurationService.snapshot();
            }

        } catch (KuraException | UnknownHostException e) {
            logger.error("Error updating the configuration", e);
        }
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        return new ComponentConfigurationImpl(PID, getDefinition(this.properties),
                this.properties);
    }

    protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
        // To be done with NetworkManager?
//        return NetInterfaceType.ETHERNET;
        if (isUsbPort(interfaceName)) {
            return this.linuxNetworkUtil.getType(this.networkService.getModemPppInterfaceName(interfaceName));
        } else {
            return this.linuxNetworkUtil.getType(interfaceName);
        }
    }

    private void setModemProperties(String interfaceName, Map<String, Object> props) {
        // Set the ppp number for the given madem
        // To be done with NetworkManager?
        Integer pppNumber = Integer
                .valueOf(this.networkService.getModemPppInterfaceName(interfaceName).substring(3));
        setModemPppNumber(props, interfaceName, pppNumber);

        // To be done with NetworkManager?
        // Set some usb properties for the modems (they aren't in the snapshot
        Optional<ModemDevice> modemOptional = this.networkService.getModemDevice(interfaceName);
        if (modemOptional.isPresent() && modemOptional.get() instanceof UsbModemDevice) {
            UsbModemDevice usbModemDevice = (UsbModemDevice) modemOptional.get();
            setModemUsbDeviceProperties(props, interfaceName, usbModemDevice);
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

//    private void executeVisitors(NetworkConfiguration networkConfiguration) throws KuraException {
//        for (NetworkConfigurationVisitor visitor : getVisitors()) {
//            visitor.setExecutorService(this.commandExecutorService);
//            networkConfiguration.accept(visitor);
//        }
//    }

    private boolean isEncrypted(String password) {
        try {
            this.cryptoService.decryptAes(password.toCharArray());
            return true;
        } catch (Exception unableToDecryptAes) {
            return false;
        }
    }

    private String decryptPassword(String password) throws KuraException {
        if (password.isEmpty()) {
            return "";
        }

        if (isEncrypted(password)) {
            return new String(this.cryptoService.decryptAes(password.toCharArray()));
        } else {
            return password;
        }
    }

    private void decryptPasswordProperties(Map<String, Object> modifiedProps) throws KuraException {
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

//    @Override
//    public synchronized void updateCurrentNetworkConfiguration() throws KuraException {
//        NetworkConfiguration networkConfiguration = new NetworkConfiguration();
//
//        // Get the current values
//        List<NetInterface<? extends NetInterfaceAddress>> allNetworkInterfaces = this.networkService
//                .getNetworkInterfaces();
//        Map<String, NetInterface<? extends NetInterfaceAddress>> allNetworkInterfacesMap = new HashMap<>();
//
//        for (NetInterface<? extends NetInterfaceAddress> netInterface : allNetworkInterfaces) {
//            allNetworkInterfacesMap.put(netInterface.getName(), netInterface);
//        }
//
//        // Create the NetInterfaceConfig objects
//        if (allNetworkInterfacesMap.keySet() != null) {
//            for (NetInterface<? extends NetInterfaceAddress> netInterface : allNetworkInterfacesMap.values()) {
//
//                String interfaceName = netInterface.getName();
//                try {
//                    // ignore mon interface
//                    if (shouldSkipNetworkConfiguration(interfaceName)) {
//                        continue;
//                    }
//
//                    NetInterfaceType type = netInterface.getType();
//
//                    UsbDevice usbDevice = netInterface.getUsbDevice();
//
//                    logger.debug("Getting config for {} type: {}", interfaceName, type);
//                    switch (type) {
//                        case LOOPBACK:
//                            populateLoopbackConfiguration(networkConfiguration, netInterface, usbDevice);
//                            break;
//
//                        case ETHERNET:
//                            populateEthernetConfiguration(networkConfiguration, netInterface, usbDevice);
//                            break;
//
//                        case WIFI:
//                            populateWifiConfig(networkConfiguration, netInterface, usbDevice);
//                            break;
//
//                        case MODEM:
//                            populateModemConfig(networkConfiguration, netInterface, usbDevice);
//                            break;
//
//                        case UNKNOWN:
//                            logger.debug("Found interface of unknown type in current configuration: {}. Ignoring it.",
//                                    interfaceName);
//                            break;
//
//                        default:
//                            logger.debug("Unsupported type: {} - not adding to configuration. Ignoring it.", type);
//                    }
//                } catch (Exception e) {
//                    logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
//                }
//            }
//        }
//
//        mergeNetworkConfigurationProperties(networkConfiguration.getConfigurationProperties(), this.properties);
//
//        handleNewNetworkInterfaces(networkConfiguration);
//        this.currentNetworkConfiguration = Optional.of(networkConfiguration);
//    }

    private void mergeNetworkConfigurationProperties(final Map<String, Object> source, final Map<String, Object> dest) {
        final Set<String> interfaces = getNetworkInterfaceNamesInConfig(source);
        interfaces.addAll(getNetworkInterfaceNamesInConfig(dest));

        dest.putAll(source);
        dest.put(NET_INTERFACES, interfaces.stream().collect(Collectors.joining(",")));
    }

//    private void handleNewNetworkInterfaces(NetworkConfiguration newNetworkConfiguration)
//            throws KuraException {
//        if (this.currentNetworkConfiguration.isPresent()) {
//            final NetworkConfiguration currentConfiguration = this.currentNetworkConfiguration.get();
//
//            final boolean hasNewInterfaceConfigs = newNetworkConfiguration.getNetInterfaceConfigs().stream()
//                    .anyMatch(c -> currentConfiguration.getNetInterfaceConfig(c.getName()) == null);
//
//            if (hasNewInterfaceConfigs) {
//                logger.info("found new network interfaces, rewriting network configuration");
//                executeVisitors(newNetworkConfiguration);
//                this.eventAdmin.postEvent(
//                        new NetworkConfigurationChangeEvent(newNetworkConfiguration.getConfigurationProperties()));
//            }
//        }
//    }

//    private void populateModemConfig(NetworkConfiguration networkConfiguration,
//            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
//
//        String interfaceConfigName = probeNetInterfaceConfigName(netInterface);
//
//        ModemInterfaceImpl<? extends NetInterfaceAddress> activeModemInterface = (ModemInterfaceImpl<? extends NetInterfaceAddress>) netInterface;
//        addPropertiesInModemInterface(activeModemInterface);
//        ModemInterfaceConfigImpl modemInterfaceConfig = new ModemInterfaceConfigImpl(activeModemInterface);
//
//        boolean isVirtual = modemInterfaceConfig.isVirtual();
//
//        modemInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
//            try {
//                List<NetConfig> modemNetConfigs = IpConfigurationInterpreter.populateConfiguration(this.properties,
//                        interfaceConfigName, netInterfaceAddress.getAddress(), isVirtual);
//                modemNetConfigs.addAll(ModemConfigurationInterpreter.populateConfiguration(netInterfaceAddress,
//                        this.properties, interfaceConfigName, modemInterfaceConfig.getPppNum()));
//                ((ModemInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(modemNetConfigs);
//            } catch (UnknownHostException | KuraException e) {
//                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceConfigName, e);
//            }
//        });
//
//        modemInterfaceConfig.setUsbDevice(usbDevice);
//        networkConfiguration.addNetInterfaceConfig(modemInterfaceConfig);
//    }

    private String probeNetInterfaceConfigName(NetInterface<? extends NetInterfaceAddress> netInterface) {
        final Set<String> interfaceNamesInConfig = getNetworkInterfaceNamesInConfig(this.properties);

        final Optional<String> usbPort = Optional.ofNullable(netInterface.getUsbDevice()).map(UsbDevice::getUsbPort);

        if (usbPort.isPresent() && interfaceNamesInConfig.contains(usbPort.get())) {
            return usbPort.get();
        } else if (interfaceNamesInConfig.contains(netInterface.getName())) {
            return netInterface.getName();
        }
        return usbPort.orElse(netInterface.getName());
    }

//    private void populateWifiConfig(NetworkConfiguration networkConfiguration,
//            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
//        String interfaceName = netInterface.getName();
//        WifiInterfaceImpl<? extends NetInterfaceAddress> activeWifiInterface = (WifiInterfaceImpl<? extends NetInterfaceAddress>) netInterface;
//        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(activeWifiInterface);
//
//        boolean isVirtual = wifiInterfaceConfig.isVirtual();
//
//        wifiInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
//            try {
//                List<NetConfig> wifiNetConfigs = IpConfigurationInterpreter.populateConfiguration(this.properties,
//                        interfaceName, netInterfaceAddress.getAddress(), isVirtual);
//                wifiNetConfigs
//                        .addAll(WifiConfigurationInterpreter.populateConfiguration(this.properties, interfaceName));
//                ((WifiInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(wifiNetConfigs);
//                ((WifiInterfaceAddressConfigImpl) netInterfaceAddress)
//                        .setMode(WifiConfigurationInterpreter.getWifiMode(this.properties, interfaceName));
//            } catch (UnknownHostException | KuraException e) {
//                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
//            }
//        });
//
//        wifiInterfaceConfig.setUsbDevice(usbDevice);
//        networkConfiguration.addNetInterfaceConfig(wifiInterfaceConfig);
//    }

//    private void populateEthernetConfiguration(NetworkConfiguration networkConfiguration,
//            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
//        String interfaceName = netInterface.getName();
//
//        EthernetInterface<? extends NetInterfaceAddress> activeEthInterface = (EthernetInterface<? extends NetInterfaceAddress>) netInterface;
//        EthernetInterfaceConfigImpl ethernetInterfaceConfig = new EthernetInterfaceConfigImpl(activeEthInterface);
//
//        boolean isVirtual = ethernetInterfaceConfig.isVirtual();
//
//        ethernetInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
//            try {
//                ((NetInterfaceAddressConfigImpl) netInterfaceAddress)
//                        .setNetConfigs(IpConfigurationInterpreter.populateConfiguration(this.properties, interfaceName,
//                                netInterfaceAddress.getAddress(), isVirtual));
//            } catch (UnknownHostException e) {
//                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
//            }
//        });
//
//        ethernetInterfaceConfig.setUsbDevice(usbDevice);
//        networkConfiguration.addNetInterfaceConfig(ethernetInterfaceConfig);
//    }

//    private void populateLoopbackConfiguration(NetworkConfiguration networkConfiguration,
//            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
//        String interfaceName = netInterface.getName();
//
//        LoopbackInterface<? extends NetInterfaceAddress> activeLoopInterface = (LoopbackInterface<? extends NetInterfaceAddress>) netInterface;
//        LoopbackInterfaceConfigImpl loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(activeLoopInterface);
//
//        boolean isVirtual = loopbackInterfaceConfig.isVirtual();
//
//        loopbackInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
//            try {
//                ((NetInterfaceAddressConfigImpl) netInterfaceAddress)
//                        .setNetConfigs(IpConfigurationInterpreter.populateConfiguration(this.properties, interfaceName,
//                                netInterfaceAddress.getAddress(), isVirtual));
//            } catch (UnknownHostException e) {
//                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
//            }
//        });
//
//        loopbackInterfaceConfig.setUsbDevice(usbDevice);
//        networkConfiguration.addNetInterfaceConfig(loopbackInterfaceConfig);
//    }

//    private boolean shouldSkipNetworkConfiguration(String interfaceName) {
//        boolean result = false;
//
//        if (interfaceName.startsWith("mon.") || interfaceName.startsWith("rpine")) {
//            result = true;
//        }
//
//        return result;
//    }
//
//    private void addPropertiesInModemInterface(ModemInterfaceImpl<? extends NetInterfaceAddress> modemInterface) {
//        String interfaceName = modemInterface.getName();
//        if (isNull(this.modemManagerService)) {
//            return;
//        }
//
//        String modemPort = this.networkService.getModemUsbPort(interfaceName);
//        if (modemPort == null) {
//            modemPort = interfaceName;
//        }
//        try {
//            this.modemManagerService.withModemService(modemPort, m -> {
//                if (!m.isPresent()) {
//                    return (Void) null;
//                }
//
//                final CellularModem modem = m.get();
//
//                // set modem properties
//                modemInterface.setSerialNumber(modem.getSerialNumber());
//                modemInterface.setModel(modem.getModel());
//                modemInterface.setFirmwareVersion(modem.getRevisionID());
//                modemInterface.setGpsSupported(modem.isGpsSupported());
//
//                // set modem driver
//                UsbModemDevice usbModemDevice = (UsbModemDevice) modemInterface.getUsbDevice();
//                if (usbModemDevice != null) {
//                    List<? extends UsbModemDriver> drivers = SupportedUsbModemsFactoryInfo
//                            .getDeviceDrivers(usbModemDevice.getVendorId(), usbModemDevice.getProductId());
//                    if (drivers != null && !drivers.isEmpty()) {
//                        UsbModemDriver driver = drivers.get(0);
//                        modemInterface.setDriver(driver.getName());
//                    }
//                }
//
//                return (Void) null;
//            });
//        } catch (KuraException e) {
//            logger.warn("Error getting modem info");
//        }
//    }

    private static Map<String, Object> discardModifiedNetworkInterfaces(final Map<String, Object> properties) {
        if (!properties.containsKey(MODIFIED_INTERFACE_NAMES)) {
            return properties;
        }

        final Map<String, Object> result = new HashMap<>(properties);
        result.remove(MODIFIED_INTERFACE_NAMES);
        return result;
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

            final Object usbBusNumber = this.properties.get(prefix + "usb.busNumber");
            final Object usbDevicePath = this.properties.get(prefix + "usb.devicePath");

            if (!(usbBusNumber instanceof String) || !(usbDevicePath instanceof String)) {
                logger.warn("failed to determine usb port for {}, skipping", existingInterfaceName);
            }

            final String migratedInterfaceName = usbBusNumber + "-" + usbDevicePath;

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

        for (final Entry<String, Object> e : this.properties.entrySet()) {
            final String key = e.getKey();

            if (key.startsWith(prefix)) {
                final String suffix = key.substring(prefix.length());
                final String migratedPropertyKey = migratedPrefix + suffix;

                final Object existingProperty = this.properties.get(migratedPropertyKey);

                if (existingProperty != null) {
                    result.put(migratedPropertyKey, existingProperty);
                } else {
                    result.put(migratedPropertyKey, e.getValue());
                }
            }
        }
    }

}
