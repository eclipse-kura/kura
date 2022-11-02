/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
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
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.visitor.linux.LinuxWriteVisitor;
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
    private static final String CONFIG_IP4_PREFIX = ".config.ip4.prefix";
    private static final String CONFIG_IP4_ADDRESS = ".config.ip4.address";
    private static final String CONFIG_DRIVER = ".config.driver";
    private static final String CONFIG_AUTOCONNECT = ".config.autoconnect";
    private static final String CONFIG_MTU = ".config.mtu";
    private static final String NET_INTERFACES = "net.interfaces";
    private static final String MODEM_PORT_REGEX = "^\\d+-\\d+";
    private static final Pattern PPP_INTERFACE = Pattern.compile("ppp[0-9]+");
    private static final Pattern COMMA = Pattern.compile(",");

    private NetworkService networkService;
    private EventAdmin eventAdmin;
    private UsbService usbService;
    private ModemManagerService modemManagerService;
    private CommandExecutorService commandExecutorService;
    private CryptoService cryptoService;

    private List<NetworkConfigurationVisitor> writeVisitors;

    private LinuxNetworkUtil linuxNetworkUtil;

    private Map<String, Object> properties = new HashMap<>();
    private Optional<NetworkConfiguration> currentNetworkConfiguration = Optional.empty();

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
            this.properties = properties;
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

    private Optional<NetInterfaceType> getNetworkTypeFromProperties(final String interfaceName,
            final Map<String, Object> properties) {
        return Optional.ofNullable(properties.get(PREFIX + interfaceName + ".type")).flatMap(p -> {
            try {
                return Optional.of(NetInterfaceType.valueOf((String) p));
            } catch (final Exception e) {
                return Optional.empty();
            }
        });
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

        try {
            final Map<String, Object> newProperties = migrateModemConfigs(receivedProperties);
            logger.debug("new properties - updating");
            logger.debug("modified.interface.names: {}", newProperties.get("modified.interface.names"));
            this.properties.putAll(receivedProperties);

            Map<String, Object> modifiedProps = new HashMap<>();
            modifiedProps.putAll(newProperties);
            String interfaces = (String) newProperties.get(NET_INTERFACES);
            StringTokenizer st = new StringTokenizer(interfaces, ",");
            while (st.hasMoreTokens()) {
                String interfaceName = st.nextToken();
                NetInterfaceType type = getNetworkType(interfaceName);

                setInterfaceType(modifiedProps, interfaceName, type);
                if (NetInterfaceType.MODEM.equals(type)) {
                    setModemPppNumber(modifiedProps, interfaceName);
                    setModemUsbDeviceProperties(modifiedProps, interfaceName);
                }
            }

            decryptPasswordProperties(modifiedProps);

            NetworkConfiguration networkConfiguration = new NetworkConfiguration(modifiedProps);

            for (NetworkConfigurationVisitor visitor : getVisitors()) {
                visitor.setExecutorService(this.commandExecutorService);
                networkConfiguration.accept(visitor);
            }

            updateCurrentNetworkConfiguration();

            this.eventAdmin.postEvent(new NetworkConfigurationChangeEvent(modifiedProps));

        } catch (Exception e) {
            logger.error("Error updating the configuration", e);
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
    public synchronized ComponentConfiguration getConfiguration() throws KuraException {
        // This method returns the network configuration properties without the current
        // values.
        // i.e. the ip address that should be applied to the system, but not the actual
        // one.
        Optional<NetworkConfiguration> networkConfiguration = getNetworkConfiguration(false);
        if (!networkConfiguration.isPresent()) {
            throw new KuraRuntimeException(KuraErrorCode.CONFIGURATION_ERROR,
                    "The network component configuration cannot be retrieved");
        }

        return new ComponentConfigurationImpl(PID, getDefinition(this.properties),
                this.properties);
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

        this.properties.putAll(networkConfiguration.getConfigurationProperties());
        this.currentNetworkConfiguration = Optional.of(networkConfiguration);
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
        final Set<String> interfaceNamesInConfig = getNetworkInterfaceNamesInConfig(this.properties);

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
                .map(s -> COMMA.splitAsStream((String) s).collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);
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

    private Tocd getDefinition(final Map<String, Object> properties) throws KuraException {
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
