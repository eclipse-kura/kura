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
package org.eclipse.kura.linux.net;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressImpl;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModems;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.modem.UsbModemDriver;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.LinuxIfconfig;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.util.ScanTool;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.NetworkState;
import org.eclipse.kura.net.modem.ModemAddedEvent;
import org.eclipse.kura.net.modem.ModemConnectionStatus;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemInterfaceAddress;
import org.eclipse.kura.net.modem.ModemRemovedEvent;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.usb.AbstractUsbDevice;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbDeviceType;
import org.eclipse.kura.usb.UsbModemDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkServiceImpl implements NetworkService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceImpl.class);

    private static final String IP_ADDRESS_PARSING_FAILED = "IP address parsing failed";
    public static final String MODEM_PORT_REGEX = "^\\d+-\\d+(\\.\\d+)?$";
    private static final String UNKNOWN = "unknown";
    private static final String NA = "N/A";
    public static final String PPP_PEERS_DIR = "/etc/ppp/peers/";
    private static final String PPP = "ppp";
    private static final Integer MAX_PPP_NUMBER = 100;

    private static final String[] EVENT_TOPICS = new String[] { UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC,
            UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC };

    private static final String TOGGLE_MODEM_TASK_NAME = "ToggleModem";
    private static final long TOGGLE_MODEM_TASK_INTERVAL = 40; // in sec
    private static final long TOGGLE_MODEM_TASK_TERMINATION_TOUT = 1; // in sec
    private static final long TOGGLE_MODEM_TASK_EXECUTION_DELAY = 2; // in min

    private final Map<String, UsbModemDevice> detectedUsbModems = new ConcurrentHashMap<>();
    private final Map<String, Integer> validUsbModemsPppNumbers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean activated = new AtomicBoolean(false);

    private ComponentContext ctx;

    private EventAdmin eventAdmin;
    private UsbService usbService;
    private CommandExecutorService executorService;

    private LinuxNetworkUtil linuxNetworkUtil;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        if (eventAdmin.equals(this.eventAdmin)) {
            this.eventAdmin = null;
        }
    }

    public void setUsbService(UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(UsbService usbService) {
        if (usbService.equals(this.usbService)) {
            this.usbService = null;
        }
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        if (executorService.equals(this.executorService)) {
            this.executorService = null;
        }
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        // save the bundle context
        this.ctx = componentContext;
        setLinuxNetworkUtil(new LinuxNetworkUtil(this.executorService));

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        this.ctx.getBundleContext().registerService(EventHandler.class.getName(), this, d);

        this.executor.execute(() -> {
            try {
                SupportedUsbModems.installModemDrivers(this.executorService);

                // Add tty devices
                List<? extends AbstractUsbDevice> ttyDevices = this.usbService.getUsbTtyDevices();
                if (ttyDevices != null && !ttyDevices.isEmpty()) {
                    logger.debug("activate() :: Total tty devices reported by UsbService: {}", ttyDevices.size());
                    addUsbDevices(ttyDevices);
                }

                // Add block devices
                List<? extends AbstractUsbDevice> blockDevices = this.usbService.getUsbBlockDevices();
                if (blockDevices != null && !blockDevices.isEmpty()) {
                    logger.debug("activate() :: Total block devices reported by UsbService: {}", blockDevices.size());
                    addUsbDevices(blockDevices);
                }

                // At this point, we should have some modems - display them
                Iterator<Entry<String, UsbModemDevice>> it = this.detectedUsbModems.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, UsbModemDevice> e = it.next();

                    final String usbPort = e.getKey();
                    final UsbModemDevice usbModem = e.getValue();

                    final SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbModem);

                    logger.debug("activate() :: Found modem: {}", usbModem);

                    if (modemInfo == null) {
                        continue;
                    }

                    // Check for correct number of resources

                    logger.debug("activate() :: usbModem.getTtyDevs().size()={}, modemInfo.getNumTtyDevs()={}",
                            usbModem.getTtyDevs().size(), modemInfo.getNumTtyDevs());
                    logger.debug("activate() :: usbModem.getBlockDevs().size()={}, modemInfo.getNumBlockDevs()={}",
                            usbModem.getBlockDevs().size(), modemInfo.getNumBlockDevs());

                    if (hasCorrectNumberOfResources(modemInfo, usbModem)) {
                        logger.info("activate () :: posting ModemAddedEvent ... {}", usbModem);
                        this.eventAdmin.postEvent(new ModemAddedEvent(usbModem));
                        this.validUsbModemsPppNumbers.put(usbModem.getUsbPort(), generatePppNumber());
                    } else {
                        logger.warn(
                                "activate() :: modem doesn't have correct number of resources, will try to toggle it ...");
                        logger.info("activate() :: scheduling {} thread in {} minutes ..", TOGGLE_MODEM_TASK_NAME,
                                TOGGLE_MODEM_TASK_EXECUTION_DELAY);
                        this.executor.schedule(new ToggleModemTask(modemInfo, usbPort),
                                TOGGLE_MODEM_TASK_EXECUTION_DELAY, TimeUnit.MINUTES);
                    }
                }
            } finally {
                this.activated.set(true);
            }
        });
    }

    protected void setLinuxNetworkUtil(LinuxNetworkUtil linuxNetworkUtil) {
        this.linuxNetworkUtil = linuxNetworkUtil;
    }

    private int generatePppNumber() {
        OptionalInt pppNumber = IntStream.range(0, MAX_PPP_NUMBER)
                .filter(i -> !this.validUsbModemsPppNumbers.containsValue(i)).findFirst();
        if (pppNumber.isPresent()) {
            return pppNumber.getAsInt();
        } else {
            throw new IllegalArgumentException("Failed to generate ppp number");
        }
    }

    private String generatePppName(int pppNumber) {
        return PPP + pppNumber;
    }

    private void addUsbDevices(List<? extends AbstractUsbDevice> usbDevices) {
        for (AbstractUsbDevice device : usbDevices) {
            if (SupportedUsbModemsInfo.isSupported(device)) {
                String usbPort = device.getUsbPort();
                UsbModemDevice usbModem;
                if (this.detectedUsbModems.get(usbPort) == null) {
                    usbModem = new UsbModemDevice(device);
                } else {
                    usbModem = this.detectedUsbModems.get(usbPort);
                }
                if (device instanceof UsbTtyDevice) {
                    String deviceNode = ((UsbTtyDevice) device).getDeviceNode();
                    Integer interfaceNumber = ((UsbTtyDevice) device).getInterfaceNumber();
                    usbModem.addTtyDev(deviceNode, interfaceNumber);
                    logger.debug("activate() :: Adding tty resource: {} for {}", deviceNode, device.getUsbPort());
                } else if (device instanceof UsbBlockDevice) {
                    String deviceNode = ((UsbBlockDevice) device).getDeviceNode();
                    usbModem.addBlockDev(deviceNode);
                    logger.debug("activate() :: Adding block resource: {} for {}", deviceNode, device.getUsbPort());
                }
                this.detectedUsbModems.put(device.getUsbPort(), usbModem);
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {

        if (this.executor != null) {
            logger.debug("deactivate() :: Terminating {} Thread ...", TOGGLE_MODEM_TASK_NAME);
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(TOGGLE_MODEM_TASK_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted", e);
            }
            logger.info("deactivate() :: {} Thread terminated? - {}", TOGGLE_MODEM_TASK_NAME,
                    this.executor.isTerminated());
        }
        this.ctx = null;
    }

    @Override
    public NetworkState getState() throws KuraException {
        return NetworkState.UNKNOWN;
    }

    @Override
    public NetInterfaceState getState(String interfaceName) throws KuraException {
        return NetInterfaceState.UNKNOWN;
    }

    @Override
    public List<String> getAllNetworkInterfaceNames() throws KuraException {
        waitActivated();

        List<String> interfaceNames = new ArrayList<>();
        List<String> allInterfaceNames = this.linuxNetworkUtil.getAllInterfaceNames();
        if (allInterfaceNames != null) {
            interfaceNames.addAll(allInterfaceNames);
        }

        for (Integer pppNumber : this.validUsbModemsPppNumbers.values()) {
            if (!interfaceNames.contains(generatePppName(pppNumber))) {
                interfaceNames.add(generatePppName(pppNumber));
            }
        }

        // remove virtual interface for Access Point
        interfaceNames = interfaceNames.stream()
                .filter(name -> !name.endsWith(LinuxNetworkUtil.ACCESS_POINT_INTERFACE_SUFFIX))
                .collect(Collectors.toList());

        return interfaceNames;
    }

    @Override
    public List<NetInterface<? extends NetInterfaceAddress>> getNetworkInterfaces() throws KuraException {
        logger.trace("getNetworkInterfaces()");

        waitActivated();

        List<NetInterface<? extends NetInterfaceAddress>> netInterfaces = new ArrayList<>();

        List<String> interfaceNames = getAllNetworkInterfaceNames();
        for (String interfaceName : interfaceNames) {
            try {
                NetInterface<? extends NetInterfaceAddress> netInterface = getNetworkInterface(interfaceName);
                if (netInterface != null) {
                    netInterfaces.add(netInterface);
                }
            } catch (KuraException e) {
                logger.error("Can't get network interface info for {} ", interfaceName, e);
            }
        }

        for (Entry<String, Integer> modemEntry : this.validUsbModemsPppNumbers.entrySet()) {
            UsbModemDevice usbModem = this.detectedUsbModems.get(modemEntry.getKey());
            if (usbModem != null) {
                for (NetInterface<?> netInterface : getInterfacesForUsbDevice(netInterfaces, usbModem.getUsbPort())) {
                    if (netInterface.getType() != NetInterfaceType.MODEM) {
                        // there is a network interface associated with the modem that is not managed by the ppp driver
                        // this interface probably cannot be managed by Kura (e.g. a cdc_ncm interface)
                        // completely ignore this interface
                        netInterfaces.remove(netInterface);
                    }
                }
            }
        }
        return netInterfaces;
    }

    private List<NetInterface<?>> getInterfacesForUsbDevice(List<NetInterface<?>> netInterfaces, String usbPort) {
        List<NetInterface<?>> result = new ArrayList<>();
        netInterfaces.forEach(netInterface -> {
            final UsbDevice usbDevice = netInterface.getUsbDevice();
            if (usbDevice != null) {
                final String interfaceUsbPort = usbDevice.getUsbPort();
                if (interfaceUsbPort != null && interfaceUsbPort.equals(usbPort)) {
                    result.add(netInterface);
                }
            }
        });
        return result;
    }

    @Override
    public List<WifiAccessPoint> getAllWifiAccessPoints() throws KuraException {
        List<WifiAccessPoint> accessPoints = new ArrayList<>();
        List<String> interfaceNames = getAllNetworkInterfaceNames();
        if (!interfaceNames.isEmpty()) {
            for (String interfaceName : interfaceNames) {
                if (this.linuxNetworkUtil.getType(interfaceName) == NetInterfaceType.WIFI) {
                    accessPoints.addAll(getWifiAccessPoints(interfaceName));
                }
            }
        }
        return accessPoints;
    }

    @Override
    public List<WifiAccessPoint> getWifiAccessPoints(String wifiInterfaceName) throws KuraException {
        List<WifiAccessPoint> wifAccessPoints = null;
        IScanTool scanTool = ScanTool.get(wifiInterfaceName, this.executorService);
        if (scanTool != null) {
            wifAccessPoints = scanTool.scan();
        }
        return wifAccessPoints;
    }

    @Override
    public List<NetInterface<? extends NetInterfaceAddress>> getActiveNetworkInterfaces() throws KuraException {
        List<NetInterface<? extends NetInterfaceAddress>> activeInterfaces = new ArrayList<>();
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = getNetworkInterfaces();
        for (NetInterface<? extends NetInterfaceAddress> iface : interfaces) {
            if (this.linuxNetworkUtil.hasAddress(iface.getName())) {
                activeInterfaces.add(iface);
            }
        }
        return activeInterfaces;
    }

    public NetInterface<? extends NetInterfaceAddress> getNetworkInterface(String interfaceName) throws KuraException {

        waitActivated();

        // ignore redpine vlan interface
        if (interfaceName.startsWith("rpine")) {
            logger.debug("Ignoring redpine vlan interface.");
            return null;
        }

        LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(interfaceName);
        if (ifconfig == null) {
            logger.debug("Ignoring {} interface.", interfaceName);
            return null;
        }

        NetInterfaceType type = ifconfig.getType();

        if (type == NetInterfaceType.ETHERNET) {
            return getEthernetInterface(interfaceName, ifconfig);
        } else if (type == NetInterfaceType.LOOPBACK) {
            return getLoopbackInterface(interfaceName, ifconfig);
        } else if (type == NetInterfaceType.WIFI) {
            return getWifiInterface(interfaceName, ifconfig);
        } else if (type == NetInterfaceType.MODEM) {
            return getModemInterface(interfaceName, ifconfig);
        } else {
            if (interfaceName.startsWith("can")) {
                logger.trace("Ignoring CAN interface: {}", interfaceName);
            } else if (interfaceName.startsWith("ppp")) {
                logger.debug("Ignoring unconfigured ppp interface: {}", interfaceName);
            } else {
                logger.debug("Unsupported network type - not adding to network devices: {} of type: {}", interfaceName,
                        type);
            }
            return null;
        }
    }

    protected NetInterface<? extends NetInterfaceAddress> getModemInterface(String interfaceName,
            LinuxIfconfig ifconfig) throws KuraException {
        if (interfaceName.startsWith("ppp")) {
            String modemUsbPort = getModemUsbPort(interfaceName);
            if (modemUsbPort == null || modemUsbPort.isEmpty()) {
                logger.debug("Usb port for {} modem not found", interfaceName);
                return null;
            }

            ModemDevice modemDevice = this.detectedUsbModems.get(modemUsbPort);
            return modemDevice != null ? getModemNetInterfaceByPppName(interfaceName, ifconfig.isUp(), modemDevice)
                    : null;
        } else {
            return null;
        }
    }

    protected NetInterface<? extends NetInterfaceAddress> getWifiInterface(String interfaceName, LinuxIfconfig ifconfig)
            throws KuraException {
        WifiInterfaceImpl<WifiInterfaceAddress> wifiInterface = new WifiInterfaceImpl<>(interfaceName);
        boolean isUp = ifconfig.isUp();

        Map<String, String> driver = this.linuxNetworkUtil.getEthernetDriver(interfaceName);
        wifiInterface.setDriver(driver.get("name"));
        wifiInterface.setDriverVersion(driver.get("version"));
        wifiInterface.setFirmwareVersion(driver.get("firmware"));
        wifiInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
        wifiInterface.setHardwareAddress(ifconfig.getMacAddressBytes());
        wifiInterface.setMTU(ifconfig.getMtu());
        wifiInterface.setSupportsMulticast(ifconfig.isMulticast());
        // FIXME:MS Add linkUp in the AbstractNetInterface and populate accordingly
        // wifiInterface.setLinkUp(LinuxNetworkUtil.isLinkUp(type, interfaceName));
        wifiInterface.setLoopback(false);
        wifiInterface.setPointToPoint(false);
        wifiInterface.setUp(isUp);
        wifiInterface.setVirtual(isVirtual(interfaceName));
        wifiInterface.setUsbDevice(getUsbDevice(interfaceName));
        wifiInterface.setState(getState(interfaceName, isUp));
        wifiInterface.setNetInterfaceAddresses(getWifiNetInterfaceAddresses(interfaceName, isUp));

        try {
            wifiInterface.setCapabilities(this.linuxNetworkUtil.getWifiCapabilities(interfaceName));
        } catch (final Exception e) {
            logger.warn("failed to get capabilities for {}", interfaceName);
            logger.debug("exception", e);
            wifiInterface.setCapabilities(EnumSet.noneOf(Capability.class));
        }

        return wifiInterface;
    }

    protected NetInterface<? extends NetInterfaceAddress> getLoopbackInterface(String interfaceName,
            LinuxIfconfig ifconfig) throws KuraException {
        LoopbackInterfaceImpl<NetInterfaceAddress> netInterface = new LoopbackInterfaceImpl<>(interfaceName);
        boolean isUp = ifconfig.isUp();

        netInterface.setDriver(NA);
        netInterface.setDriverVersion(NA);
        netInterface.setFirmwareVersion(NA);
        netInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
        netInterface.setHardwareAddress(new byte[] { 0, 0, 0, 0, 0, 0 });
        netInterface.setLoopback(true);
        netInterface.setMTU(ifconfig.getMtu());
        netInterface.setSupportsMulticast(ifconfig.isMulticast());
        netInterface.setPointToPoint(false);
        netInterface.setUp(isUp);
        netInterface.setVirtual(false);
        netInterface.setUsbDevice(null);
        netInterface.setState(getState(interfaceName, isUp));
        netInterface.setNetInterfaceAddresses(getEthernetOrLoopbackNetInterfaceAddresses(interfaceName, isUp));

        return netInterface;
    }

    protected NetInterface<? extends NetInterfaceAddress> getEthernetInterface(String interfaceName,
            LinuxIfconfig ifconfig) throws KuraException {
        EthernetInterfaceImpl<NetInterfaceAddress> netInterface = new EthernetInterfaceImpl<>(interfaceName);
        NetInterfaceType type = ifconfig.getType();
        boolean isUp = ifconfig.isUp();

        Map<String, String> driver = this.linuxNetworkUtil.getEthernetDriver(interfaceName);
        netInterface.setDriver(driver.get("name"));
        netInterface.setDriverVersion(driver.get("version"));
        netInterface.setFirmwareVersion(driver.get("firmware"));
        netInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
        netInterface.setHardwareAddress(ifconfig.getMacAddressBytes());
        netInterface.setMTU(ifconfig.getMtu());
        netInterface.setSupportsMulticast(ifconfig.isMulticast());
        netInterface.setLinkUp(this.linuxNetworkUtil.isLinkUp(type, interfaceName));
        netInterface.setLoopback(false);
        netInterface.setPointToPoint(false);
        netInterface.setUp(isUp);
        netInterface.setVirtual(isVirtual(interfaceName));
        netInterface.setUsbDevice(getUsbDevice(interfaceName));
        netInterface.setState(getState(interfaceName, isUp));
        netInterface.setNetInterfaceAddresses(getEthernetOrLoopbackNetInterfaceAddresses(interfaceName, isUp));

        return netInterface;
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("handleEvent() :: topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC)) {
            if (validateDeviceAddedEvent(event)) {
                manageDeviceAddedEvent(event);
            }
        } else if (topic.equals(UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC)) {
            if (validateDeviceRemovedEvent(event)) {
                manageDeviceRemovedEvent(event);
            }
        } else {
            logger.error("handleEvent() :: Unexpected event topic: {}", topic);
        }
    }

    private boolean validateDeviceAddedEvent(Event event) {
        if (event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY) == null
                || event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY) == null
                || event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY) == null
                || event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY) == null) {
            return false;
        }
        if (event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY) == null
                || ((String) event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY)).startsWith("usb")) {
            return false;
        }
        return event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY) != null
                && !((UsbDeviceType) event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY))
                        .equals(UsbDeviceType.USB_NET_DEVICE);
    }

    private boolean validateDeviceRemovedEvent(Event event) {
        if (event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY) == null
                || event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY) == null) {
            return false;
        }
        return event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY) != null;
    }

    private void manageDeviceRemovedEvent(Event event) {
        UsbModemDevice usbModem = this.detectedUsbModems
                .remove(event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));
        if (usbModem != null) {
            logger.info("handleEvent() :: Removing modem: {}", usbModem);
            this.validUsbModemsPppNumbers.remove(usbModem.getUsbPort());

            Map<String, String> properties = new HashMap<>();
            properties.put(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY, usbModem.getUsbBusNumber());
            properties.put(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY, usbModem.getUsbDevicePath());
            properties.put(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY, usbModem.getUsbPort());
            properties.put(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY, usbModem.getVendorId());
            properties.put(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY, usbModem.getProductId());
            properties.put(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, usbModem.getManufacturerName());
            properties.put(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, usbModem.getProductName());
            this.eventAdmin.postEvent(new ModemRemovedEvent(properties));
        }
    }

    private void manageDeviceAddedEvent(Event event) {
        UsbModemDevice temporaryUsbModem = new UsbModemDevice(
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY), null,
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY), null, null);
        final SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(temporaryUsbModem);

        if (modemInfo != null) {
            // Found one - see if we have some info for it.
            // Also check if we are getting more devices than expected.
            // This can happen if all the modem resources cannot be removed from the OS or from Kura.
            // In this case we did not receive an UsbDeviceRemovedEvent and we did not post
            // an ModemRemovedEvent. Should we do it here?
            installModemDriver(modemInfo);

            UsbModemDevice usbModem = getUsbModemDevice(event, modemInfo);
            configureUsbModemDevice(event, usbModem);
            this.detectedUsbModems.put(usbModem.getUsbPort(), usbModem);

            // At this point, we should have some modems - display them
            logger.info("handleEvent() :: Modified modem (Added resource): {}", usbModem);

            logger.debug("handleEvent() :: usbModem.getTtyDevs().size()={}, modemInfo.getNumTtyDevs()={}",
                    usbModem.getTtyDevs().size(), modemInfo.getNumTtyDevs());
            logger.debug("handleEvent() :: usbModem.getBlockDevs().size()={}, modemInfo.getNumBlockDevs()={}",
                    usbModem.getBlockDevs().size(), modemInfo.getNumBlockDevs());

            // Check for correct number of resources
            if (usbModem.getTtyDevs().size() == modemInfo.getNumTtyDevs()
                    && usbModem.getBlockDevs().size() == modemInfo.getNumBlockDevs()) {
                logger.info("handleEvent() :: posting ModemAddedEvent -- USB_EVENT_DEVICE_ADDED_TOPIC: {}", usbModem);
                this.eventAdmin.postEvent(new ModemAddedEvent(usbModem));
                this.validUsbModemsPppNumbers.put(usbModem.getUsbPort(), generatePppNumber());
            }
        }
    }

    private void configureUsbModemDevice(Event event, UsbModemDevice usbModem) {
        String resource = (String) event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY);
        Integer interfaceNumber = (Integer) event.getProperty(UsbDeviceEvent.USB_EVENT_USB_INTERFACE_NUMBER);
        UsbDeviceType usbDeviceType = (UsbDeviceType) event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY);
        logger.debug("handleEvent() :: Found resource: {} of type {} for: {}", resource, usbDeviceType,
                usbModem.getUsbPort());
        if (usbDeviceType.equals(UsbDeviceType.USB_TTY_DEVICE)) {
            usbModem.addTtyDev(resource, interfaceNumber);
        } else if (usbDeviceType.equals(UsbDeviceType.USB_BLOCK_DEVICE)) {
            usbModem.addBlockDev(resource);
        }
    }

    private UsbModemDevice getUsbModemDevice(Event event, final SupportedUsbModemInfo modemInfo) {
        UsbModemDevice usbModem = this.detectedUsbModems
                .get(event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));

        if (usbModem == null) {
            logger.debug("handleEvent() :: Modem not found. Create one");
            usbModem = createNewUsbModemDevice(event);
        } else if (modemInfo.getNumTtyDevs() > 0 && modemInfo.getNumBlockDevs() > 0) {
            if (usbModem.getTtyDevs().size() >= modemInfo.getNumTtyDevs()
                    && usbModem.getBlockDevs().size() >= modemInfo.getNumBlockDevs()) {
                logger.debug("handleEvent() :: Found modem with too many resources: {}. Create a new one", usbModem);
                usbModem = createNewUsbModemDevice(event);
            }
        } else if (modemInfo.getNumTtyDevs() > 0 && usbModem.getTtyDevs().size() >= modemInfo.getNumTtyDevs()
                || modemInfo.getNumBlockDevs() > 0 && usbModem.getBlockDevs().size() >= modemInfo.getNumBlockDevs()) {
            logger.debug("handleEvent() :: Found modem with too many resources: {}. Create a new one", usbModem);
            usbModem = createNewUsbModemDevice(event);
        }
        return usbModem;
    }

    private UsbModemDevice createNewUsbModemDevice(Event event) {
        UsbModemDevice usbModem;
        usbModem = new UsbModemDevice((String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY),
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY),
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY),
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY),
                (String) event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY));
        return usbModem;
    }

    protected void installModemDriver(final SupportedUsbModemInfo modemInfo) {
        List<? extends UsbModemDriver> drivers = modemInfo.getDeviceDrivers();
        for (UsbModemDriver driver : drivers) {
            try {
                driver.install(this.executorService);
            } catch (Exception e) {
                logger.error("handleEvent() :: Failed to install modem device driver {} ", driver.getName(), e);
            }
        }
    }

    private ModemInterface<ModemInterfaceAddress> getModemNetInterfaceByPppName(String pppInterfaceName, boolean isUp,
            ModemDevice modemDevice) throws KuraException {

        ModemInterfaceImpl<ModemInterfaceAddress> modemInterface = new ModemInterfaceImpl<>(pppInterfaceName);

        modemInterface.setModemDevice(modemDevice);
        if (modemDevice instanceof UsbModemDevice) {

            UsbModemDevice usbModemDevice = (UsbModemDevice) modemDevice;
            SupportedUsbModemInfo supportedUsbModemInfo = SupportedUsbModemsInfo.getModem(usbModemDevice);
            modemInterface.setTechnologyTypes(supportedUsbModemInfo.getTechnologyTypes());
            modemInterface.setUsbDevice((UsbModemDevice) modemDevice);
        }

        modemInterface.setPppNum(Integer.parseInt(pppInterfaceName.substring(3)));
        modemInterface.setManufacturer(modemDevice.getManufacturerName());
        modemInterface.setModel(modemDevice.getProductName());
        modemInterface.setModemIdentifier(modemDevice.getProductName());

        // these properties required net.admin packages
        modemInterface.setDriver(UNKNOWN);
        modemInterface.setDriverVersion(UNKNOWN);
        modemInterface.setFirmwareVersion(UNKNOWN);
        modemInterface.setSerialNumber(UNKNOWN);

        modemInterface.setLoopback(false);
        modemInterface.setPointToPoint(true);
        modemInterface.setState(getState(pppInterfaceName, isUp));
        modemInterface.setHardwareAddress(new byte[] { 0, 0, 0, 0, 0, 0 });
        LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(pppInterfaceName);
        if (ifconfig != null) {
            modemInterface.setMTU(ifconfig.getMtu());
            modemInterface.setSupportsMulticast(ifconfig.isMulticast());
        }

        modemInterface.setUp(isUp);
        modemInterface.setVirtual(isVirtual(pppInterfaceName));
        modemInterface.setNetInterfaceAddresses(getModemInterfaceAddresses(pppInterfaceName, isUp));

        return modemInterface;

    }

    private List<NetInterfaceAddress> getEthernetOrLoopbackNetInterfaceAddresses(String interfaceName, boolean isUp)
            throws KuraException {
        List<NetInterfaceAddress> netInterfaceAddresses = new ArrayList<>();
        if (isUp) {
            ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);
            NetInterfaceAddressImpl netInterfaceAddress = new NetInterfaceAddressImpl();
            try {
                LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(interfaceName);
                if (ifconfig != null) {
                    String currentNetmask = ifconfig.getInetMask();
                    if (currentNetmask != null) {
                        netInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
                        netInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));

                        netInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
                        netInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
                        Optional<IPAddress> gatewayAddress = this.linuxNetworkUtil.getGatewayIpAddress(interfaceName);
                        if (gatewayAddress.isPresent()) {
                            netInterfaceAddress.setGateway(gatewayAddress.get());
                        } else {
                            netInterfaceAddress.setGateway(conInfo.getGateway());
                        }
                        netInterfaceAddress.setDnsServers(new ArrayList<>(LinuxDns.getInstance().getDnServers()));
                        netInterfaceAddresses.add(netInterfaceAddress);
                    }
                }
            } catch (UnknownHostException e) {
                throw new KuraIOException(e, IP_ADDRESS_PARSING_FAILED);
            }
        }
        return netInterfaceAddresses;
    }

    private List<WifiInterfaceAddress> getWifiNetInterfaceAddresses(String interfaceName, boolean isUp)
            throws KuraException {
        List<WifiInterfaceAddress> wifiInterfaceAddresses = new ArrayList<>();
        if (isUp) {
            ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);
            WifiInterfaceAddressImpl wifiInterfaceAddress = new WifiInterfaceAddressImpl();
            wifiInterfaceAddresses.add(wifiInterfaceAddress);
            try {
                LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(interfaceName);
                if (ifconfig != null && ifconfig.getInetMask() != null) {
                    String currentNetmask = ifconfig.getInetMask();
                    wifiInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
                    wifiInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
                    wifiInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
                    wifiInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
                    Optional<IPAddress> gatewayAddress = this.linuxNetworkUtil.getGatewayIpAddress(interfaceName);
                    if (gatewayAddress.isPresent()) {
                        wifiInterfaceAddress.setGateway(gatewayAddress.get());
                    } else {
                        wifiInterfaceAddress.setGateway(conInfo.getGateway());
                    }
                    wifiInterfaceAddress.setDnsServers(new ArrayList<>(LinuxDns.getInstance().getDnServers()));

                    WifiMode wifiMode = this.linuxNetworkUtil.getWifiMode(interfaceName);
                    wifiInterfaceAddress.setBitrate(this.linuxNetworkUtil.getWifiBitrate(interfaceName));
                    wifiInterfaceAddress.setMode(wifiMode);

                    if (wifiMode == WifiMode.INFRA) {
                        addMinimalWifiInfraConfiguration(interfaceName, wifiInterfaceAddress);
                    }
                }
            } catch (UnknownHostException e) {
                throw new KuraIOException(e, IP_ADDRESS_PARSING_FAILED);
            }
        }

        return wifiInterfaceAddresses;
    }

    // It seems that this configuration is ignored, so add only a minimal configuration
    private void addMinimalWifiInfraConfiguration(String interfaceName, WifiInterfaceAddressImpl wifiInterfaceAddress)
            throws KuraException {
        String currentSSID = this.linuxNetworkUtil.getSSID(interfaceName);

        if (currentSSID != null) {
            logger.debug("Adding access point SSID: {}", currentSSID);
            WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(currentSSID);
            wifiAccessPoint.setMode(WifiMode.INFRA);
            wifiInterfaceAddress.setWifiAccessPoint(wifiAccessPoint);
        }
    }

    private List<ModemInterfaceAddress> getModemInterfaceAddresses(String interfaceName, boolean isUp)
            throws KuraException {
        List<ModemInterfaceAddress> modemInterfaceAddresses = new ArrayList<>();
        if (isUp) {
            ModemInterfaceAddressImpl modemInterfaceAddress = new ModemInterfaceAddressImpl();
            modemInterfaceAddresses.add(modemInterfaceAddress);
            try {
                LinuxIfconfig ifconfig = this.linuxNetworkUtil.getInterfaceConfiguration(interfaceName);
                if (ifconfig != null) {
                    String currentNetmask = ifconfig.getInetMask();
                    if (currentNetmask != null) {
                        modemInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
                        modemInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
                        modemInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
                        modemInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
                        modemInterfaceAddress.setDnsServers(LinuxDns.getInstance().getPppDnServers());
                        ModemConnectionStatus connectionStatus = ModemConnectionStatus.CONNECTED;
                        modemInterfaceAddress.setConnectionStatus(connectionStatus);
                    }
                }
            } catch (UnknownHostException e) {
                throw new KuraIOException(e, IP_ADDRESS_PARSING_FAILED);
            }
        }
        return modemInterfaceAddresses;
    }

    private NetInterfaceState getState(String interfaceName, boolean isUp) {
        /** The device is in an unknown state. */
        // UNKNOWN(0),
        /** The device is recognized but not managed by NetworkManager. */
        // UNMANAGED(10),
        /** The device cannot be used (carrier off, rfkill, etc). */
        // UNAVAILABLE(20),
        /** The device is not connected. */
        // DISCONNECTED(30),
        /** The device is preparing to connect. */
        // PREPARE(40),
        /** The device is being configured. */
        // CONFIG(50),
        /** The device is awaiting secrets necessary to continue connection. */
        // NEED_AUTH(60),
        /** The IP settings of the device are being requested and configured. */
        // IP_CONFIG(70),
        /** The device's IP connectivity ability is being determined. */
        // IP_CHECK(80),
        /** The device is waiting for secondary connections to be activated. */
        // SECONDARIES(90),
        /** The device is active. */
        // ACTIVATED(100),
        /** The device's network connection is being torn down. */
        // DEACTIVATING(110),
        /** The device is in a failure state following an attempt to activate it. */
        // FAILED(120);

        // FIXME - expand to support other States
        if (isUp) {
            return NetInterfaceState.ACTIVATED;
        } else {
            return NetInterfaceState.DISCONNECTED;
        }
    }

    private UsbNetDevice getUsbDevice(String interfaceName) {
        List<UsbNetDevice> usbNetDevices = this.usbService.getUsbNetDevices();
        if (usbNetDevices != null && !usbNetDevices.isEmpty()) {
            for (UsbNetDevice usbNetDevice : usbNetDevices) {
                if (usbNetDevice.getInterfaceName().equals(interfaceName)) {
                    return usbNetDevice;
                }
            }
        }

        return null;
    }

    @Override
    public String getModemUsbPort(String pppInterfaceName) {
        for (Entry<String, Integer> entry : this.validUsbModemsPppNumbers.entrySet()) {
            if (generatePppName(entry.getValue()).equals(pppInterfaceName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public String getModemPppPort(ModemDevice modemDevice) throws KuraException {
        if (modemDevice instanceof UsbModemDevice) {
            UsbModemDevice usbModem = (UsbModemDevice) modemDevice;
            String modemId = usbModem.getUsbPort();
            return getModemPppInterfaceName(modemId);
        }
        return null;
    }

    @Override
    public String getModemPppInterfaceName(String usbPath) {
        return generatePppName(this.validUsbModemsPppNumbers.get(usbPath));
    }

    @Override
    public Optional<ModemDevice> getModemDevice(String usbPath) {
        Optional<ModemDevice> modem = Optional.empty();
        if (this.validUsbModemsPppNumbers.containsKey(usbPath)) {
            modem = Optional.of(this.detectedUsbModems.get(usbPath));
        }
        return modem;
    }

    private boolean isVirtual(String interfaceName) {
        return this.linuxNetworkUtil.isVirtual(interfaceName);
    }

    private boolean hasCorrectNumberOfResources(final SupportedUsbModemInfo modemInfo,
            final UsbModemDevice modemDevice) {
        return modemDevice.getTtyDevs().size() == modemInfo.getNumTtyDevs()
                && modemDevice.getBlockDevs().size() == modemInfo.getNumBlockDevs();
    }

    private void waitActivated() {
        if (!this.activated.get()) {
            try {
                this.executor.submit(() -> {
                }).get();
            } catch (ExecutionException e) {
                logger.warn("Exception while waiting for activation", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Exception while waiting for activation", e);
            }
        }
    }

    private final class ToggleModemTask implements Runnable {

        private final SupportedUsbModemInfo modemInfo;
        private final String usbPort;

        ToggleModemTask(final SupportedUsbModemInfo modemInfo, final String usbPort) {
            this.modemInfo = modemInfo;
            this.usbPort = usbPort;
        }

        @Override
        public void run() {

            final String threadName = Thread.currentThread().getName();
            Thread.currentThread().setName(TOGGLE_MODEM_TASK_NAME);

            try {
                final UsbModemDevice modemDevice = NetworkServiceImpl.this.detectedUsbModems.get(this.usbPort);

                if (modemDevice == null) {
                    logger.info("ToggleModemTask :: modem is no longer attached, exiting");
                    return;
                }

                if (hasCorrectNumberOfResources(this.modemInfo, modemDevice)) {
                    logger.info("ToggleModemTask :: modem is ready, exiting");
                    return;
                }

                UsbModemDriver modemDriver = null;
                List<? extends UsbModemDriver> usbDeviceDrivers = this.modemInfo.getDeviceDrivers();
                if (usbDeviceDrivers != null && !usbDeviceDrivers.isEmpty()) {
                    modemDriver = usbDeviceDrivers.get(0);
                }

                if (modemDriver == null) {
                    return;
                }

                logger.info("ToggleModemTask :: turning modem off ...");

                modemDriver.disable(modemDevice);
                sleep(3000);
                logger.info("ToggleModemTask :: turning modem on ...");
                modemDriver.enable(modemDevice);

                logger.info("ToggleModemTask :: modem has been toggled ...");

                // will check if the modem is ready at next iteration and toggles again if needed
                NetworkServiceImpl.this.executor.schedule(this, TOGGLE_MODEM_TASK_INTERVAL, TimeUnit.SECONDS);

            } catch (Exception e) {
                logger.error("toggleModem() :: failed to toggle modem ", e);
                NetworkServiceImpl.this.executor.schedule(this, TOGGLE_MODEM_TASK_INTERVAL, TimeUnit.SECONDS);
            } finally {
                Thread.currentThread().setName(threadName);
            }
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
