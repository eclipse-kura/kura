/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;

import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.IpConfigurationInterpreter;
import org.eclipse.kura.core.net.LoopbackInterfaceConfigImpl;
import org.eclipse.kura.core.net.ModemConfigurationInterpreter;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiConfigurationInterpreter;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.EthernetInterface;
import org.eclipse.kura.net.LoopbackInterface;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.visitor.linux.LinuxWriteVisitor;
import org.eclipse.kura.net.configuration.NetworkConfigurationServiceCommon;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationServiceImpl implements NetworkConfigurationService, SelfConfiguringComponent {

    private static final String ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION = "Error fetching information for network interface: {}";

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServiceImpl.class);

    private static final String PREFIX = "net.interface.";
    private static final String NET_INTERFACES = "net.interfaces";
    private static final String MODIFIED_INTERFACE_NAMES = "modified.interface.names";
    private static final String MODEM_PORT_REGEX = "^\\d+-\\d+";
    private static final Pattern PPP_INTERFACE = Pattern.compile("ppp[0-9]+");

    private NetworkService networkService;
    private EventAdmin eventAdmin;
    private UsbService usbService;
    private ModemManagerService modemManagerService;
    private CommandExecutorService commandExecutorService;
    private CryptoService cryptoService;
    private ConfigurationService configurationService;

    private List<NetworkConfigurationVisitor> writeVisitors;

    private LinuxNetworkUtil linuxNetworkUtil;

    private Map<String, Object> properties = new HashMap<>();
    private Optional<NetworkConfiguration> currentNetworkConfiguration = Optional.empty();
    private List<UsbNetDevice> usbNetDevices;

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

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------
    public void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("activate(componentContext, properties)...");
        initVisitors();

        this.linuxNetworkUtil = new LinuxNetworkUtil(this.commandExecutorService);
        if (properties == null) {
            logger.debug("Got null properties...");
        } else {
            logger.debug("Props...{}", properties);
            this.properties = discardModifiedNetworkInterfaces(new HashMap<>(properties));
            updated(this.properties);
        }
    }

    protected void initVisitors() {
        this.writeVisitors = new ArrayList<>();
        this.writeVisitors.add(LinuxWriteVisitor.getInstance());
    }

    protected List<NetworkConfigurationVisitor> getVisitors() {
        return this.writeVisitors;
    }

    public void deactivate(ComponentContext componentContext) {
        logger.debug("deactivate()");
        this.writeVisitors = null;
    }

    protected List<String> getAllInterfaceNames() throws KuraException {
        return this.networkService.getAllNetworkInterfaceNames();
    }

    protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
        if (isUsbPort(interfaceName)) {
            return this.linuxNetworkUtil.getType(this.networkService.getModemPppInterfaceName(interfaceName));
        } else {
            return this.linuxNetworkUtil.getType(interfaceName);
        }
    }

    @Override
    public synchronized void setNetworkConfiguration(NetworkConfiguration networkConfiguration) throws KuraException {
        updated(networkConfiguration.getConfigurationProperties());
    }

    public synchronized void updated(Map<String, Object> receivedProperties) {
        if (receivedProperties == null) {
            logger.debug("properties are null");
            return;
        }

        // get the USB network interfaces (if any)
        this.usbNetDevices = this.usbService.getUsbNetDevices();

        try {
            final Map<String, Object> newProperties = migrateModemConfigs(receivedProperties);
            logger.debug("new properties - updating");
            logger.debug("modified.interface.names: {}", newProperties.get(MODIFIED_INTERFACE_NAMES));

            Map<String, Object> modifiedProps = new HashMap<>(newProperties);

            final Set<String> interfaces = NetworkConfigurationServiceCommon
                    .getNetworkInterfaceNamesInConfig(newProperties);

            for (final String interfaceName : interfaces) {
                NetInterfaceType type = getNetworkType(interfaceName);

                setInterfaceType(modifiedProps, interfaceName, type);
                if (NetInterfaceType.MODEM.equals(type)) {
                    setModemPppNumber(modifiedProps, interfaceName);
                    setModemUsbDeviceProperties(modifiedProps, interfaceName);
                }
            }

            final boolean changed = checkWanInterfaces(this.properties, modifiedProps);
            mergeNetworkConfigurationProperties(modifiedProps, this.properties);

            decryptPasswordProperties(modifiedProps);
            NetworkConfiguration networkConfiguration = new NetworkConfiguration(modifiedProps);

            executeVisitors(networkConfiguration);

            updateCurrentNetworkConfiguration();

            this.eventAdmin.postEvent(new NetworkConfigurationChangeEvent(modifiedProps));

            this.properties = discardModifiedNetworkInterfaces(this.properties);

            if (changed) {
                this.configurationService.snapshot();
            }

        } catch (Exception e) {
            logger.error("Error updating the configuration", e);
        }
    }

    private boolean checkWanInterfaces(final Map<String, Object> oldProperties,
            final Map<String, Object> newProperties) {
        final Set<String> oldWanInterfaces = NetworkConfigurationServiceCommon.getWanInterfacesInConfig(oldProperties);
        final Set<String> newWanInterfaces = NetworkConfigurationServiceCommon.getWanInterfacesInConfig(newProperties);

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

    private void executeVisitors(NetworkConfiguration networkConfiguration) throws KuraException {
        for (NetworkConfigurationVisitor visitor : getVisitors()) {
            visitor.setExecutorService(this.commandExecutorService);
            networkConfiguration.accept(visitor);
        }
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

    @Override
    @SuppressWarnings("restriction")
    public synchronized ComponentConfiguration getConfiguration() throws KuraException {

        return NetworkConfigurationServiceCommon.getConfiguration(PID, properties, Optional.of(this.usbNetDevices));
    }

    @Override
    public synchronized NetworkConfiguration getNetworkConfiguration() throws KuraException {
        if (!this.currentNetworkConfiguration.isPresent()) {
            throw new KuraRuntimeException(KuraErrorCode.CONFIGURATION_ERROR,
                    "The network configuration cannot be retrieved");
        }
        return this.currentNetworkConfiguration.get();

    }

    @Override
    public synchronized Optional<NetworkConfiguration> getNetworkConfiguration(boolean recompute) throws KuraException {
        if (recompute) {
            updateCurrentNetworkConfiguration();
        }
        return this.currentNetworkConfiguration;
    }

    @Override
    public synchronized void updateCurrentNetworkConfiguration() throws KuraException {
        NetworkConfiguration networkConfiguration = new NetworkConfiguration();

        // Get the current values
        List<NetInterface<? extends NetInterfaceAddress>> allNetworkInterfaces = this.networkService
                .getNetworkInterfaces();
        Map<String, NetInterface<? extends NetInterfaceAddress>> allNetworkInterfacesMap = new HashMap<>();

        for (NetInterface<? extends NetInterfaceAddress> netInterface : allNetworkInterfaces) {
            allNetworkInterfacesMap.put(netInterface.getName(), netInterface);
        }

        // Create the NetInterfaceConfig objects
        if (allNetworkInterfacesMap.keySet() != null) {
            for (NetInterface<? extends NetInterfaceAddress> netInterface : allNetworkInterfacesMap.values()) {

                String interfaceName = netInterface.getName();
                try {
                    // ignore mon interface
                    if (shouldSkipNetworkConfiguration(interfaceName)) {
                        continue;
                    }

                    NetInterfaceType type = netInterface.getType();

                    UsbDevice usbDevice = netInterface.getUsbDevice();

                    logger.debug("Getting config for {} type: {}", interfaceName, type);
                    switch (type) {
                        case LOOPBACK:
                            populateLoopbackConfiguration(networkConfiguration, netInterface, usbDevice);
                            break;

                        case ETHERNET:
                            populateEthernetConfiguration(networkConfiguration, netInterface, usbDevice);
                            break;

                        case WIFI:
                            populateWifiConfig(networkConfiguration, netInterface, usbDevice);
                            break;

                        case MODEM:
                            populateModemConfig(networkConfiguration, netInterface, usbDevice);
                            break;

                        case UNKNOWN:
                            logger.debug("Found interface of unknown type in current configuration: {}. Ignoring it.",
                                    interfaceName);
                            break;

                        default:
                            logger.debug("Unsupported type: {} - not adding to configuration. Ignoring it.", type);
                    }
                } catch (Exception e) {
                    logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
                }
            }
        }

        mergeNetworkConfigurationProperties(networkConfiguration.getConfigurationProperties(), this.properties);

        handleNewNetworkInterfaces(networkConfiguration);
        this.currentNetworkConfiguration = Optional.of(networkConfiguration);
    }

    private void mergeNetworkConfigurationProperties(final Map<String, Object> source, final Map<String, Object> dest) {
        final Set<String> interfaces = NetworkConfigurationServiceCommon.getNetworkInterfaceNamesInConfig(source);
        interfaces.addAll(NetworkConfigurationServiceCommon.getNetworkInterfaceNamesInConfig(dest));

        dest.putAll(source);
        dest.put(NET_INTERFACES, interfaces.stream().collect(Collectors.joining(",")));
    }

    private void handleNewNetworkInterfaces(NetworkConfiguration newNetworkConfiguration)
            throws KuraException {
        if (this.currentNetworkConfiguration.isPresent()) {
            final NetworkConfiguration currentConfiguration = this.currentNetworkConfiguration.get();

            final boolean hasNewInterfaceConfigs = newNetworkConfiguration.getNetInterfaceConfigs().stream()
                    .anyMatch(c -> currentConfiguration.getNetInterfaceConfig(c.getName()) == null);

            if (hasNewInterfaceConfigs) {
                logger.info("found new network interfaces, rewriting network configuration");
                executeVisitors(newNetworkConfiguration);
                this.eventAdmin.postEvent(
                        new NetworkConfigurationChangeEvent(newNetworkConfiguration.getConfigurationProperties()));
            }
        }
    }

    private void populateModemConfig(NetworkConfiguration networkConfiguration,
            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {

        String interfaceConfigName = probeNetInterfaceConfigName(netInterface);

        ModemInterfaceImpl<? extends NetInterfaceAddress> activeModemInterface = (ModemInterfaceImpl<? extends NetInterfaceAddress>) netInterface;
        addPropertiesInModemInterface(activeModemInterface);
        ModemInterfaceConfigImpl modemInterfaceConfig = new ModemInterfaceConfigImpl(activeModemInterface);

        boolean isVirtual = modemInterfaceConfig.isVirtual();

        modemInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
            try {
                List<NetConfig> modemNetConfigs = IpConfigurationInterpreter.populateConfiguration(this.properties,
                        interfaceConfigName, netInterfaceAddress.getAddress(), isVirtual);
                modemNetConfigs.addAll(ModemConfigurationInterpreter.populateConfiguration(netInterfaceAddress,
                        this.properties, interfaceConfigName, modemInterfaceConfig.getPppNum()));
                ((ModemInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(modemNetConfigs);
            } catch (UnknownHostException | KuraException e) {
                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceConfigName, e);
            }
        });

        modemInterfaceConfig.setUsbDevice(usbDevice);
        networkConfiguration.addNetInterfaceConfig(modemInterfaceConfig);
    }

    private String probeNetInterfaceConfigName(NetInterface<? extends NetInterfaceAddress> netInterface) {
        final Set<String> interfaceNamesInConfig = NetworkConfigurationServiceCommon
                .getNetworkInterfaceNamesInConfig(this.properties);

        final Optional<String> usbPort = Optional.ofNullable(netInterface.getUsbDevice()).map(UsbDevice::getUsbPort);

        if (usbPort.isPresent() && interfaceNamesInConfig.contains(usbPort.get())) {
            return usbPort.get();
        } else if (interfaceNamesInConfig.contains(netInterface.getName())) {
            return netInterface.getName();
        }
        return usbPort.orElse(netInterface.getName());
    }

    private void populateWifiConfig(NetworkConfiguration networkConfiguration,
            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
        String interfaceName = netInterface.getName();
        WifiInterfaceImpl<? extends NetInterfaceAddress> activeWifiInterface = (WifiInterfaceImpl<? extends NetInterfaceAddress>) netInterface;
        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(activeWifiInterface);

        boolean isVirtual = wifiInterfaceConfig.isVirtual();

        wifiInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
            try {
                List<NetConfig> wifiNetConfigs = IpConfigurationInterpreter.populateConfiguration(this.properties,
                        interfaceName, netInterfaceAddress.getAddress(), isVirtual);
                wifiNetConfigs
                        .addAll(WifiConfigurationInterpreter.populateConfiguration(this.properties, interfaceName));
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddress).setNetConfigs(wifiNetConfigs);
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddress)
                        .setMode(WifiConfigurationInterpreter.getWifiMode(this.properties, interfaceName));
            } catch (UnknownHostException | KuraException e) {
                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
            }
        });

        wifiInterfaceConfig.setUsbDevice(usbDevice);
        networkConfiguration.addNetInterfaceConfig(wifiInterfaceConfig);
    }

    private void populateEthernetConfiguration(NetworkConfiguration networkConfiguration,
            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
        String interfaceName = netInterface.getName();

        EthernetInterface<? extends NetInterfaceAddress> activeEthInterface = (EthernetInterface<? extends NetInterfaceAddress>) netInterface;
        EthernetInterfaceConfigImpl ethernetInterfaceConfig = new EthernetInterfaceConfigImpl(activeEthInterface);

        boolean isVirtual = ethernetInterfaceConfig.isVirtual();

        ethernetInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
            try {
                ((NetInterfaceAddressConfigImpl) netInterfaceAddress)
                        .setNetConfigs(IpConfigurationInterpreter.populateConfiguration(this.properties, interfaceName,
                                netInterfaceAddress.getAddress(), isVirtual));
            } catch (UnknownHostException e) {
                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
            }
        });

        ethernetInterfaceConfig.setUsbDevice(usbDevice);
        networkConfiguration.addNetInterfaceConfig(ethernetInterfaceConfig);
    }

    private void populateLoopbackConfiguration(NetworkConfiguration networkConfiguration,
            NetInterface<? extends NetInterfaceAddress> netInterface, UsbDevice usbDevice) {
        String interfaceName = netInterface.getName();

        LoopbackInterface<? extends NetInterfaceAddress> activeLoopInterface = (LoopbackInterface<? extends NetInterfaceAddress>) netInterface;
        LoopbackInterfaceConfigImpl loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(activeLoopInterface);

        boolean isVirtual = loopbackInterfaceConfig.isVirtual();

        loopbackInterfaceConfig.getNetInterfaceAddresses().forEach(netInterfaceAddress -> {
            try {
                ((NetInterfaceAddressConfigImpl) netInterfaceAddress)
                        .setNetConfigs(IpConfigurationInterpreter.populateConfiguration(this.properties, interfaceName,
                                netInterfaceAddress.getAddress(), isVirtual));
            } catch (UnknownHostException e) {
                logger.warn(ERROR_FETCHING_NETWORK_INTERFACE_INFORMATION, interfaceName, e);
            }
        });

        loopbackInterfaceConfig.setUsbDevice(usbDevice);
        networkConfiguration.addNetInterfaceConfig(loopbackInterfaceConfig);
    }

    private boolean shouldSkipNetworkConfiguration(String interfaceName) {
        boolean result = false;

        if (interfaceName.startsWith("mon.") || interfaceName.startsWith("rpine")) {
            result = true;
        }

        return result;
    }

    private void addPropertiesInModemInterface(ModemInterfaceImpl<? extends NetInterfaceAddress> modemInterface) {
        String interfaceName = modemInterface.getName();
        if (isNull(this.modemManagerService)) {
            return;
        }

        String modemPort = this.networkService.getModemUsbPort(interfaceName);
        if (modemPort == null) {
            modemPort = interfaceName;
        }
        try {
            this.modemManagerService.withModemService(modemPort, m -> {
                if (!m.isPresent()) {
                    return (Void) null;
                }

                final CellularModem modem = m.get();

                // set modem properties
                modemInterface.setSerialNumber(modem.getSerialNumber());
                modemInterface.setModel(modem.getModel());
                modemInterface.setFirmwareVersion(modem.getRevisionID());
                modemInterface.setGpsSupported(modem.isGpsSupported());

                // set modem driver
                UsbModemDevice usbModemDevice = (UsbModemDevice) modemInterface.getUsbDevice();
                if (usbModemDevice != null) {
                    List<? extends UsbModemDriver> drivers = SupportedUsbModemsFactoryInfo
                            .getDeviceDrivers(usbModemDevice.getVendorId(), usbModemDevice.getProductId());
                    if (drivers != null && !drivers.isEmpty()) {
                        UsbModemDriver driver = drivers.get(0);
                        modemInterface.setDriver(driver.getName());
                    }
                }

                return (Void) null;
            });
        } catch (KuraException e) {
            logger.warn("Error getting modem info");
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

    private boolean isUsbPort(String interfaceName) {
        return interfaceName.split("\\.")[0].matches(MODEM_PORT_REGEX);
    }

    private Map<String, Object> migrateModemConfigs(final Map<String, Object> properties) {

        final Map<String, Object> result = new HashMap<>(properties);
        final Set<String> interfaceNames = NetworkConfigurationServiceCommon
                .getNetworkInterfaceNamesInConfig(properties);
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

            resultInterfaceNames.add(migratedInterfaceName);

            logger.info("migrating configuration for interface: {}...done", existingInterfaceName);

        }

        result.put(NET_INTERFACES, resultInterfaceNames.stream().collect(Collectors.joining(",")));
        return result;
    }
}
