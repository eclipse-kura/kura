/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.configuration.KuraNetConfigReadyEvent;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.EthernetInterface;
import org.eclipse.kura.net.LoopbackInterface;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.visitor.linux.LinuxReadVisitor;
import org.eclipse.kura.net.admin.visitor.linux.LinuxWriteVisitor;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.EventProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationServiceImpl
        implements NetworkConfigurationService, SelfConfiguringComponent, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServiceImpl.class);

    private static final String[] EVENT_TOPICS = { KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC };
    private static final String NET_INTERFACES = "net.interfaces";
    public static final String UNCONFIGURED_MODEM_REGEX = "^\\d+-\\d+(\\.\\d+)*$";

    private NetworkService networkService;
    private EventAdmin eventAdmin;
    private UsbService usbService;
    private ModemManagerService modemManagerService;
    private CommandExecutorService executorService;

    private List<NetworkConfigurationVisitor> readVisitors;
    private List<NetworkConfigurationVisitor> writeVisitors;

    private ScheduledExecutorService executorUtil;
    private boolean firstConfig = true;
    private LinuxNetworkUtil linuxNetworkUtil;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void unsetNetworkService(NetworkService networkService) {
        this.networkService = null;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        this.usbService = null;
    }

    public void setModemManagerService(ModemManagerService modemManagerService) {
        logger.debug("Set the modem manager service");
        this.modemManagerService = modemManagerService;
    }

    public void unsetModemManagerService(ModemManagerService modemManagerService) {
        logger.debug("Unset the modem manager service");
        this.modemManagerService = null;
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------
    /*
     * Do not have a default activate for this self configuring component because we are not using it at startup
     * protected void activate(ComponentContext componentContext) {}
     */

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.debug("activate(componentContext, properties)...");

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);

        this.executorUtil = Executors.newSingleThreadScheduledExecutor();

        this.executorUtil.schedule(() -> {
            // make sure we don't miss the setting of firstConfig
            NetworkConfigurationServiceImpl.this.firstConfig = false;
        }, 3, TimeUnit.MINUTES);

        initVisitors();

        this.linuxNetworkUtil = new LinuxNetworkUtil(this.executorService);
        // we are intentionally ignoring the properties from ConfigAdmin at startup
        if (properties == null) {
            logger.debug("Got null properties...");
        } else {
            logger.debug("Props...{}", properties);
        }
    }

    protected void initVisitors() {
        this.readVisitors = new ArrayList<>();
        this.readVisitors.add(LinuxReadVisitor.getInstance());

        this.writeVisitors = new ArrayList<>();
        this.writeVisitors.add(LinuxWriteVisitor.getInstance());
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.debug("deactivate()");
        this.writeVisitors = null;
        this.readVisitors = null;
        this.executorUtil.shutdownNow();
    }

    protected List<String> getAllInterfaceNames() throws KuraException {
        return this.linuxNetworkUtil.getAllInterfaceNames();
    }

    protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
        return this.linuxNetworkUtil.getType(interfaceName);
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("handleEvent - topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC)) {
            this.firstConfig = false;
            this.executorUtil.schedule(() -> {
                Map<String, Object> props = new HashMap<>();
                EventProperties eventProps = new EventProperties(props);
                logger.info("postInstalledEvent() :: posting KuraNetConfigReadyEvent");
                NetworkConfigurationServiceImpl.this.eventAdmin
                        .postEvent(new Event(KuraNetConfigReadyEvent.KURA_NET_CONFIG_EVENT_READY_TOPIC, eventProps));
            }, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) throws KuraException {
        updated(networkConfiguration.getConfigurationProperties());
    }

    public synchronized void updated(Map<String, Object> properties) {
        // skip the first config
        if (this.firstConfig) {
            logger.debug("Ignoring first configuration");
            this.firstConfig = false;
            return;
        }

        try {
            if (properties != null) {
                logger.debug("new properties - updating");
                logger.debug("modified.interface.names: {}", properties.get("modified.interface.names"));

                // dynamically insert the type properties..
                Map<String, Object> modifiedProps = new HashMap<>();
                modifiedProps.putAll(properties);
                String interfaces = (String) properties.get(NET_INTERFACES);
                StringTokenizer st = new StringTokenizer(interfaces, ",");
                while (st.hasMoreTokens()) {
                    String interfaceName = st.nextToken();
                    StringBuilder sb = new StringBuilder();
                    sb.append("net.interface.").append(interfaceName).append(".type");

                    NetInterfaceType type = getNetworkType(interfaceName);
                    type = updateUnknownType(interfaceName, type);

                    modifiedProps.put(sb.toString(), type.toString());
                }

                NetworkConfiguration networkConfig = new NetworkConfiguration(modifiedProps);

                for (NetworkConfigurationVisitor visitor : this.writeVisitors) {
                    visitor.setExecutorService(this.executorService);
                    networkConfig.accept(visitor);
                }

                // raise the event because there was a change
                this.eventAdmin.postEvent(new NetworkConfigurationChangeEvent(modifiedProps));
            } else {
                logger.debug("properties are null");
            }
        } catch (Exception e) {
            // TODO - would still want an event if partially successful?
            logger.error("Error updating the configuration", e);
        }
    }

    private NetInterfaceType updateUnknownType(String interfaceName, NetInterfaceType type) {
        NetInterfaceType result = type;
        if (type == NetInterfaceType.UNKNOWN && interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
            // If the interface name is in a form such as "1-3.4" (USB address), assume it is a modem
            result = NetInterfaceType.MODEM;
        }

        return result;
    }

    @Override
    public synchronized ComponentConfiguration getConfiguration() throws KuraException {
        logger.debug("getConfiguration()");
        try {
            NetworkConfiguration networkConfiguration = getNetworkConfiguration();
            return new ComponentConfigurationImpl(PID, getDefinition(),
                    networkConfiguration.getConfigurationProperties());
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    // @Override
    // FIXME:MC Introducing a short lived cache will make startup much faster.
    @Override
    public synchronized NetworkConfiguration getNetworkConfiguration() throws KuraException {
        NetworkConfiguration networkConfiguration = new NetworkConfiguration();

        // Get the current values
        List<NetInterface<? extends NetInterfaceAddress>> allNetworkInterfaces = this.networkService
                .getNetworkInterfaces();
        Map<String, NetInterface<? extends NetInterfaceAddress>> allNetworkInterfacesMap = new HashMap<>();
        Map<String, NetInterface<? extends NetInterfaceAddress>> activeNetworkInterfacesMap = new HashMap<>();
        for (NetInterface<? extends NetInterfaceAddress> netInterface : allNetworkInterfaces) {
            allNetworkInterfacesMap.put(netInterface.getName(), netInterface);
            if (netInterface.isUp()) {
                activeNetworkInterfacesMap.put(netInterface.getName(), netInterface);
            }
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
                    type = updateUnknownType(interfaceName, type);

                    logger.debug("Getting config for {} type: {}", interfaceName, type);
                    switch (type) {
                    case LOOPBACK:
                        LoopbackInterface<? extends NetInterfaceAddress> activeLoopInterface = (LoopbackInterface<? extends NetInterfaceAddress>) netInterface;
                        LoopbackInterfaceConfigImpl loopbackInterfaceConfig = new LoopbackInterfaceConfigImpl(
                                activeLoopInterface);
                        networkConfiguration.addNetInterfaceConfig(loopbackInterfaceConfig);
                        break;

                    case ETHERNET:
                        EthernetInterface<? extends NetInterfaceAddress> activeEthInterface = (EthernetInterface<? extends NetInterfaceAddress>) netInterface;
                        EthernetInterfaceConfigImpl ethernetInterfaceConfig = new EthernetInterfaceConfigImpl(
                                activeEthInterface);
                        networkConfiguration.addNetInterfaceConfig(ethernetInterfaceConfig);
                        break;

                    case WIFI:
                        WifiInterfaceImpl<? extends NetInterfaceAddress> activeWifiInterface = (WifiInterfaceImpl<? extends NetInterfaceAddress>) netInterface;
                        WifiInterfaceConfigImpl wifiInterfaceConfig = new WifiInterfaceConfigImpl(activeWifiInterface);
                        networkConfiguration.addNetInterfaceConfig(wifiInterfaceConfig);
                        break;

                    case MODEM:
                        ModemInterfaceImpl<? extends NetInterfaceAddress> activeModemInterface = (ModemInterfaceImpl<? extends NetInterfaceAddress>) netInterface;
                        addPropertiesInModemInterface(activeModemInterface);
                        ModemInterfaceConfigImpl modemInterfaceConfig = new ModemInterfaceConfigImpl(
                                activeModemInterface);
                        networkConfiguration.addNetInterfaceConfig(modemInterfaceConfig);
                        break;

                    case UNKNOWN:
                        logger.debug("Found interface of unknown type in current configuration: {}. Ignoring it.",
                                interfaceName);
                        break;

                    default:
                        logger.debug("Unsupported type: {} - not adding to configuration. Ignoring it.", type);
                    }
                } catch (Exception e) {
                    logger.warn("Error fetching information for network interface: {}", interfaceName, e);
                }
            }
        }

        // populate the NetInterfaceConfigs
        for (NetworkConfigurationVisitor visitor : this.readVisitors) {
            visitor.setExecutorService(this.executorService);
            networkConfiguration.accept(visitor);
        }

        return networkConfiguration;
    }

    private boolean shouldSkipNetworkConfiguration(String interfaceName) {
        boolean result = false;

        // ignore mon interface, redpine vlan interface and usb0 for beaglebone
        if (interfaceName.startsWith("mon.") || interfaceName.startsWith("rpine")
                || interfaceName.startsWith("usb0") && "beaglebone".equals(System.getProperty("target.device"))) {
            result = true;
        }

        return result;
    }

    private void addPropertiesInModemInterface(ModemInterfaceImpl<? extends NetInterfaceAddress> modemInterface)
            throws KuraException {
        String interfaceName = modemInterface.getName();
        if (this.modemManagerService != null) {
            String modemPort = this.networkService.getModemUsbPort(interfaceName);
            if (modemPort == null) {
                modemPort = interfaceName;
            }
            CellularModem modem = this.modemManagerService.getModemService(modemPort);
            if (modem != null) {

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
            }
        }
    }

    private Tocd getDefinition() throws KuraException {
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
            List<String> networkInterfaceNames = getAllInterfaceNames();
            for (String ifaceName : networkInterfaceNames) {
                // get the current configuration for this interface
                NetInterfaceType type = getNetworkType(ifaceName);

                String prefix = "net.interface.";

                if (type == NetInterfaceType.LOOPBACK) {
                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu").toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu").toString());
                    tad.setType(Tscalar.INTEGER);
                    tad.setCardinality(0);
                    tad.setRequired(true);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_MTU));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")
                            .toString());
                    tad.setType(Tscalar.BOOLEAN);
                    tad.setCardinality(0);
                    tad.setRequired(true);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_AUTOCONNECT));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.driver").toString());
                    tad.setName(
                            new StringBuffer().append(prefix).append(ifaceName).append(".config.driver").toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_DRIVER));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")
                            .toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_ADDRESS));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")
                            .toString());
                    tad.setType(Tscalar.SHORT);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_PREFIX));
                    tocd.addAD(tad);
                } else if (type == NetInterfaceType.ETHERNET || type == NetInterfaceType.WIFI) {
                    if (usbNetDevices != null) {
                        for (UsbNetDevice usbNetDevice : usbNetDevices) {
                            if (usbNetDevice.getInterfaceName().equals(ifaceName)) {
                                // found a match - add the read only fields?
                                tad = objectFactory.createTad();
                                tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".usb.port")
                                        .toString());
                                tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".usb.port")
                                        .toString());
                                tad.setType(Tscalar.STRING);
                                tad.setCardinality(0);
                                tad.setRequired(false);
                                tad.setDefault("");
                                tad.setDescription(NetworkAdminConfigurationMessages
                                        .getMessage(NetworkAdminConfiguration.USB_PORT));
                                tocd.addAD(tad);

                                tad = objectFactory.createTad();
                                tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                        .append(".usb.manufacturer").toString());
                                tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                        .append(".usb.manfacturer").toString());
                                tad.setType(Tscalar.STRING);
                                tad.setCardinality(0);
                                tad.setRequired(false);
                                tad.setDefault("");
                                tad.setDescription(NetworkAdminConfigurationMessages
                                        .getMessage(NetworkAdminConfiguration.USB_MANUFACTURER));
                                tocd.addAD(tad);

                                tad = objectFactory.createTad();
                                tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".usb.product")
                                        .toString());
                                tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".usb.product")
                                        .toString());
                                tad.setType(Tscalar.STRING);
                                tad.setCardinality(0);
                                tad.setRequired(false);
                                tad.setDefault("");
                                tad.setDescription(NetworkAdminConfigurationMessages
                                        .getMessage(NetworkAdminConfiguration.USB_PRODUCT));
                                tocd.addAD(tad);

                                tad = objectFactory.createTad();
                                tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                        .append(".usb.manufacturer.id").toString());
                                tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                        .append(".usb.manfacturer.id").toString());
                                tad.setType(Tscalar.STRING);
                                tad.setCardinality(0);
                                tad.setRequired(false);
                                tad.setDefault("");
                                tad.setDescription(NetworkAdminConfigurationMessages
                                        .getMessage(NetworkAdminConfiguration.USB_MANUFACTURER_ID));
                                tocd.addAD(tad);

                                tad = objectFactory.createTad();
                                tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".usb.product.id")
                                        .toString());
                                tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                        .append(".usb.product.id").toString());
                                tad.setType(Tscalar.STRING);
                                tad.setCardinality(0);
                                tad.setRequired(false);
                                tad.setDefault("");
                                tad.setDescription(NetworkAdminConfigurationMessages
                                        .getMessage(NetworkAdminConfiguration.USB_PRODUCT_ID));
                                tocd.addAD(tad);

                                // no need to continue
                                break;
                            }
                        }
                    }

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu").toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.mtu").toString());
                    tad.setType(Tscalar.INTEGER);
                    tad.setCardinality(0);
                    tad.setRequired(true);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_MTU));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.autoconnect")
                            .toString());
                    tad.setType(Tscalar.BOOLEAN);
                    tad.setCardinality(0);
                    tad.setRequired(true);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_AUTOCONNECT));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpClient4.enabled")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpClient4.enabled").toString());
                    tad.setType(Tscalar.BOOLEAN);
                    tad.setCardinality(0);
                    tad.setRequired(true);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_CLIENT_ENABLED));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.address")
                            .toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_ADDRESS));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.prefix")
                            .toString());
                    tad.setType(Tscalar.SHORT);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_IPV4_PREFIX));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.gateway")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.ip4.gateway")
                            .toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_GATEWAY));
                    tocd.addAD(tad);

                    // DNS and WINS
                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.dnsServers")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.dnsServers")
                            .toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(10000);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(
                            NetworkAdminConfigurationMessages.getMessage(NetworkAdminConfiguration.CONFIG_DNS_SERVERS));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.winsServers")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.winsServers")
                            .toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(10000);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_WINS_SERVERS));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.enabled")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.enabled").toString());
                    tad.setType(Tscalar.BOOLEAN);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_ENABLED));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.defaultLeaseTime").toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.defaultLeaseTime").toString());
                    tad.setType(Tscalar.INTEGER);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_DEFAULT_LEASE_TIME));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.maxLeaseTime").toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.maxLeaseTime").toString());
                    tad.setType(Tscalar.INTEGER);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_MAX_LEASE_TIME));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.prefix")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.prefix")
                            .toString());
                    tad.setType(Tscalar.SHORT);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_PREFIX));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.rangeStart").toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.rangeStart").toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_RANGE_START));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.rangeEnd")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.rangeEnd").toString());
                    tad.setType(Tscalar.STRING);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_RANGE_END));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.dhcpServer4.passDns")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                            .append(".config.dhcpServer4.passDns").toString());
                    tad.setType(Tscalar.BOOLEAN);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_PASS_DNS));
                    tocd.addAD(tad);

                    tad = objectFactory.createTad();
                    tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.nat.enabled")
                            .toString());
                    tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.nat.enabled")
                            .toString());
                    tad.setType(Tscalar.BOOLEAN);
                    tad.setCardinality(0);
                    tad.setRequired(false);
                    tad.setDefault("");
                    tad.setDescription(NetworkAdminConfigurationMessages
                            .getMessage(NetworkAdminConfiguration.CONFIG_IPV4_DHCP_SERVER_NAT_ENABLED));
                    tocd.addAD(tad);

                    if (type == NetInterfaceType.WIFI) {
                        // Common
                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".wifi.capabilities")
                                .toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".wifi.capabilities")
                                .toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.WIFI_CAPABILITIES));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.mode")
                                .toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.mode")
                                .toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MODE));
                        tocd.addAD(tad);

                        // INFRA
                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.infra.ssid")
                                .toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.ssid").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_SSID));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.hardwareMode").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.hardwareMode").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_HARDWARE_MODE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.radioMode").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.radioMode").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_HARDWARE_MODE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.securityType").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.securityType").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_SECURITY_TYPE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.passphrase").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.passphrase").toString());
                        tad.setType(Tscalar.PASSWORD);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_PASSPHRASE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.pairwiseCiphers").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.pairwiseCiphers").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.groupCiphers").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.groupCiphers").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_GROUP_CIPHERS));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.channel").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.infra.channel").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_INFRA_CHANNEL));
                        tocd.addAD(tad);

                        // MASTER
                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName).append(".config.wifi.master.ssid")
                                .toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.ssid").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_SSID));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.broadcast").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.broadcast").toString());
                        tad.setType(Tscalar.BOOLEAN);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_BROADCAST_ENABLED));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.hardwareMode").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.hardwareMode").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_HARDWARE_MODE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.radioMode").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.radioMode").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_HARDWARE_MODE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.securityType").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.securityType").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_SECURITY_TYPE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.passphrase").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.passphrase").toString());
                        tad.setType(Tscalar.PASSWORD);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_PASSPHRASE));
                        tocd.addAD(tad);

                        tad = objectFactory.createTad();
                        tad.setId(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.channel").toString());
                        tad.setName(new StringBuffer().append(prefix).append(ifaceName)
                                .append(".config.wifi.master.channel").toString());
                        tad.setType(Tscalar.STRING);
                        tad.setCardinality(0);
                        tad.setRequired(false);
                        tad.setDefault("");
                        tad.setDescription(NetworkAdminConfigurationMessages
                                .getMessage(NetworkAdminConfiguration.CONFIG_WIFI_MASTER_CHANNEL));
                        tocd.addAD(tad);
                    }

                    // TODO - deal with USB devices (READ ONLY)
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, e);
        }

        return tocd;
    }
}
