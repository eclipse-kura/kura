/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.LoopbackInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.WifiInterfaceAddressImpl;
import org.eclipse.kura.core.net.WifiInterfaceImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
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
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiInterfaceAddress;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
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

    public static final String PPP_PEERS_DIR = "/etc/ppp/peers/";

    private static final Logger logger = LoggerFactory.getLogger(NetworkServiceImpl.class);

    private static final String UNCONFIGURED_MODEM_REGEX = "^\\d+-\\d+(\\.\\d+)?$";

    private static final String[] EVENT_TOPICS = new String[] { UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC,
            UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC };

    private static final String TOOGLE_MODEM_THREAD_NAME = "ToggleModem";
    private static final long TOOGLE_MODEM_THREAD_INTERVAL = 10000; // in msec
    private static final long TOOGLE_MODEM_THREAD_TERMINATION_TOUT = 1; // in sec
    private static final long TOOGLE_MODEM_THREAD_EXECUTION_DELAY = 2; // in min

    private ComponentContext ctx;

    private EventAdmin eventAdmin;
    private UsbService usbService;

    private Map<String, UsbModemDevice> usbModems;
    private SerialModemDevice serialModem;

    private List<String> addedModems;

    private ScheduledExecutorService executor;

    private ScheduledFuture<?> task;
    private static AtomicBoolean stopThread;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
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

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        // save the bundle context
        this.ctx = componentContext;

        stopThread = new AtomicBoolean();
        this.usbModems = new HashMap<>();
        this.addedModems = new ArrayList<>();

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        this.ctx.getBundleContext().registerService(EventHandler.class.getName(), this, d);

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
        Iterator<Entry<String, UsbModemDevice>> it = this.usbModems.entrySet().iterator();
        while (it.hasNext()) {
            final UsbModemDevice usbModem = it.next().getValue();
            final SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbModem.getVendorId(),
                    usbModem.getProductId(), usbModem.getProductName());

            logger.debug("activate() :: Found modem: {}", usbModem);

            // Check for correct number of resources
            if (modemInfo != null) {
                logger.debug("activate() :: usbModem.getTtyDevs().size()={}, modemInfo.getNumTtyDevs()={}",
                        usbModem.getTtyDevs().size(), modemInfo.getNumTtyDevs());
                logger.debug("activate() :: usbModem.getBlockDevs().size()={}, modemInfo.getNumBlockDevs()={}",
                        usbModem.getBlockDevs().size(), modemInfo.getNumBlockDevs());

                if (usbModem.getTtyDevs().size() == modemInfo.getNumTtyDevs()
                        && usbModem.getBlockDevs().size() == modemInfo.getNumBlockDevs()) {
                    logger.info("activate () :: posting ModemAddedEvent ... {}", usbModem);
                    this.eventAdmin.postEvent(new ModemAddedEvent(usbModem));
                    this.addedModems.add(usbModem.getUsbPort());
                } else {
                    logger.warn(
                            "activate() :: modem doesn't have correct number of resources, will try to toggle it ...");
                    this.executor = Executors.newSingleThreadScheduledExecutor();
                    logger.info("activate() :: scheduling {} thread in {} minutes ..", TOOGLE_MODEM_THREAD_NAME,
                            TOOGLE_MODEM_THREAD_EXECUTION_DELAY);
                    stopThread.set(false);
                    this.task = this.executor.schedule(() -> {
                        Thread.currentThread().setName(TOOGLE_MODEM_THREAD_NAME);
                        try {
                            toggleModem(modemInfo);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            logger.debug("activate() :: modem monitor interrupted - {}", interruptedException);
                        } catch (Throwable t) {
                            logger.error("activate() :: Exception while monitoring cellular connection ", t);
                        }

                    }, TOOGLE_MODEM_THREAD_EXECUTION_DELAY, TimeUnit.MINUTES);
                }
            }
        }
    }

    private void addUsbDevices(List<? extends AbstractUsbDevice> usbDevices) {
        for (AbstractUsbDevice device : usbDevices) {
            if (SupportedUsbModemsInfo.isSupported(device.getVendorId(), device.getProductId(),
                    device.getProductName())) {
                String usbPort = device.getUsbPort();
                UsbModemDevice usbModem;
                if (this.usbModems.get(usbPort) == null) {
                    usbModem = new UsbModemDevice(device);
                } else {
                    usbModem = this.usbModems.get(usbPort);
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
                this.usbModems.put(device.getUsbPort(), usbModem);
            }
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        if (this.task != null && !this.task.isDone()) {
            stopThread.set(true);
            toggleModemNotity();
            logger.debug("deactivate() :: Cancelling {} task ...", TOOGLE_MODEM_THREAD_NAME);
            this.task.cancel(true);
            logger.info("deactivate() :: {} task cancelled? = {}", TOOGLE_MODEM_THREAD_NAME, this.task.isDone());
            this.task = null;
        }

        if (this.executor != null) {
            logger.debug("deactivate() :: Terminating {} Thread ...", TOOGLE_MODEM_THREAD_NAME);
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(TOOGLE_MODEM_THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted", e);
            }
            logger.info("deactivate() :: {} Thread terminated? - {}", TOOGLE_MODEM_THREAD_NAME,
                    this.executor.isTerminated());
            this.executor = null;
        }
        this.usbModems = null;
        this.ctx = null;
    }

    @Override
    public NetworkState getState() throws KuraException {
        // FIXME - this method needs some work

        // see if we have global access by trying to ping - maybe there is a better way?
        if (LinuxNetworkUtil.canPing("8.8.8.8", 1)) {
            return NetworkState.CONNECTED_GLOBAL;
        }
        if (LinuxNetworkUtil.canPing("8.8.4.4", 1)) {
            return NetworkState.CONNECTED_GLOBAL;
        }

        // if we have a link we at least of network local access
        List<NetInterface<? extends NetInterfaceAddress>> netInterfaces = getNetworkInterfaces();
        for (NetInterface<? extends NetInterfaceAddress> netInterface : netInterfaces) {
            if (netInterface.getType() == NetInterfaceType.ETHERNET) {
                if (((EthernetInterfaceImpl<? extends NetInterfaceAddress>) netInterface).isLinkUp()) {
                    return NetworkState.CONNECTED_SITE;
                }
            }
        }

        // TODO - should be know if we are CONNECTED_SITE for wifi?

        LoopbackInterfaceImpl<? extends NetInterfaceAddress> netInterface = (LoopbackInterfaceImpl<? extends NetInterfaceAddress>) getNetworkInterface(
                "lo");
        if (netInterface.isUp()) {
            return NetworkState.CONNECTED_LOCAL;
        }

        // not sure what we're doing...
        return NetworkState.UNKNOWN;
    }

    @Override
    public NetInterfaceState getState(String interfaceName) throws KuraException {
        NetInterface<? extends NetInterfaceAddress> netInterface = getNetworkInterface(interfaceName);
        if (netInterface == null) {
            logger.error("There is no status available for network interface {}", interfaceName);
            return NetInterfaceState.UNKNOWN;
        } else {
            return netInterface.getState();
        }
    }

    @Override
    public List<String> getAllNetworkInterfaceNames() throws KuraException {
        ArrayList<String> interfaceNames = new ArrayList<>();
        List<String> allInterfaceNames = LinuxNetworkUtil.getAllInterfaceNames();
        if (allInterfaceNames != null) {
            interfaceNames.addAll(allInterfaceNames);
        }

        // include non-connected ppp interfaces and usb port numbers for non-configured modems
        Iterator<String> it = this.addedModems.iterator();
        while (it.hasNext()) {
            String modemId = it.next();
            UsbModemDevice usbModem = this.usbModems.get(modemId);
            String pppPort = null;
            if (usbModem != null) {
                pppPort = getModemPppPort(usbModem);
            } else {
                // for Serial modem
                if (this.serialModem != null) {
                    pppPort = getModemPppPort(this.serialModem);
                }
            }

            if (pppPort != null) {
                if (!interfaceNames.contains(pppPort)) {
                    interfaceNames.add(pppPort);
                }
            } else {
                // add the usb port as an interface if there isn't already a ppp interface associated with this port
                interfaceNames.add(modemId);
            }
        }

        return interfaceNames;
    }

    @Override
    public List<NetInterface<? extends NetInterfaceAddress>> getNetworkInterfaces() throws KuraException {
        logger.trace("getNetworkInterfaces()");
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

        // Return an entry for non-connected modems (those w/o a ppp interface)
        Iterator<String> it = this.addedModems.iterator();
        while (it.hasNext()) {
            String modemId = it.next();
            UsbModemDevice usbModem = this.usbModems.get(modemId);
            if (usbModem != null) {
                // only add if there is not already a ppp interface for this modem
                boolean addModem = true;
                for (NetInterface<?> netInterface : getInterfacesForUsbDevice(netInterfaces, usbModem.getUsbPort())) {
                    if (netInterface.getType() == NetInterfaceType.MODEM) {
                        // we already have a ppp interface associated to the usb modem, do not add
                        addModem = false;
                    } else {
                        // there is a network interface associated with the modem that is not managed by the ppp driver
                        // this interface probably cannot be managed by Kura (e.g. a cdc_ncm interface)
                        // completely ignore this interface
                        netInterfaces.remove(netInterface);
                    }
                }

                if (addModem) {
                    netInterfaces.add(getModemInterface(usbModem.getUsbPort(), false, usbModem));
                }
            } else {
                // for Serial modem
                if (this.serialModem != null) {
                    // only add if there is not already a ppp interface for this modem
                    boolean addModem = true;
                    for (NetInterface<?> netInterface : netInterfaces) {
                        String iface = netInterface.getName();
                        if (iface != null && iface.startsWith("ppp")) {
                            ModemInterface<ModemInterfaceAddress> pppModemInterface = getModemInterface(iface, false,
                                    this.serialModem);
                            ModemInterface<ModemInterfaceAddress> serialModemInterface = getModemInterface(
                                    this.serialModem.getProductName(), false, this.serialModem);
                            if (pppModemInterface != null && serialModemInterface != null) {
                                String pppModel = pppModemInterface.getModel();
                                String serialModel = serialModemInterface.getModel();
                                if (pppModel != null && pppModel.equals(serialModel)) {
                                    addModem = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (addModem) {
                        netInterfaces
                                .add(getModemInterface(this.serialModem.getProductName(), false, this.serialModem));
                    }
                }
            }
        }
        return netInterfaces;
    }

    private List<NetInterface<?>> getInterfacesForUsbDevice(List<NetInterface<?>> netInterfaces, String usbPort) {
        List<NetInterface<?>> result = new ArrayList<>();
        for (NetInterface<?> netInterface : netInterfaces) {
            final UsbDevice usbDevice = netInterface.getUsbDevice();
            if (usbDevice == null) {
                continue;
            }
            final String interfaceUsbPort = usbDevice.getUsbPort();
            if (interfaceUsbPort == null) {
                continue;
            }
            if (interfaceUsbPort.equals(usbPort)) {
                result.add(netInterface);
            }
        }
        return result;
    }

    @Override
    public List<WifiAccessPoint> getAllWifiAccessPoints() throws KuraException {
        List<WifiAccessPoint> accessPoints = new ArrayList<>();
        List<String> interfaceNames = getAllNetworkInterfaceNames();
        if (!interfaceNames.isEmpty()) {
            for (String interfaceName : interfaceNames) {
                if (LinuxNetworkUtil.getType(interfaceName) == NetInterfaceType.WIFI) {
                    accessPoints.addAll(getWifiAccessPoints(interfaceName));
                }
            }
        }
        return accessPoints;
    }

    @Override
    public List<WifiAccessPoint> getWifiAccessPoints(String wifiInterfaceName) throws KuraException {
        List<WifiAccessPoint> wifAccessPoints = null;
        IScanTool scanTool = ScanTool.get(wifiInterfaceName);
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
            if (LinuxNetworkUtil.hasAddress(iface.getName())) {
                activeInterfaces.add(iface);
            }
        }
        return activeInterfaces;
    }

    public NetInterface<? extends NetInterfaceAddress> getNetworkInterface(String interfaceName) throws KuraException {
        // ignore redpine vlan interface
        if (interfaceName.startsWith("rpine")) {
            logger.debug("Ignoring redpine vlan interface.");
            return null;
        }
        // ignore usb0 for beaglebone
        if (interfaceName.startsWith("usb0") && "beaglebone".equals(System.getProperty("target.device"))) {
            logger.debug("Ignoring usb0 for beaglebone.");
            return null;
        }
        // ignore unconfigured modem interface
        if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
            logger.debug("Ignoring unconfigured modem interface {}", interfaceName);
            return null;
        }

        LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
        if (ifconfig == null) {
            logger.debug("Ignoring {} interface.", interfaceName);
            return null;
        }

        NetInterfaceType type = ifconfig.getType();
        boolean isUp = ifconfig.isUp();
        if (type == NetInterfaceType.UNKNOWN && this.serialModem != null
                && interfaceName.equals(this.serialModem.getProductName())) {
            // If the interface name is in a form such as "1-3.4", assume it is a modem
            type = NetInterfaceType.MODEM;
        }

        if (type == NetInterfaceType.ETHERNET) {
            EthernetInterfaceImpl<NetInterfaceAddress> netInterface = new EthernetInterfaceImpl<>(interfaceName);

            Map<String, String> driver = LinuxNetworkUtil.getEthernetDriver(interfaceName);
            netInterface.setDriver(driver.get("name"));
            netInterface.setDriverVersion(driver.get("version"));
            netInterface.setFirmwareVersion(driver.get("firmware"));
            netInterface.setAutoConnect(LinuxNetworkUtil.isAutoConnect(interfaceName));
            netInterface.setHardwareAddress(ifconfig.getMacAddressBytes());
            netInterface.setMTU(ifconfig.getMtu());
            netInterface.setSupportsMulticast(ifconfig.isMulticast());
            netInterface.setLinkUp(LinuxNetworkUtil.isLinkUp(type, interfaceName));
            netInterface.setLoopback(false);
            netInterface.setPointToPoint(false);
            netInterface.setUp(isUp);
            netInterface.setVirtual(isVirtual());
            netInterface.setUsbDevice(getUsbDevice(interfaceName));
            netInterface.setState(getState(interfaceName, isUp));
            netInterface.setNetInterfaceAddresses(getNetInterfaceAddresses(interfaceName, type, isUp));

            return netInterface;
        } else if (type == NetInterfaceType.LOOPBACK) {
            LoopbackInterfaceImpl<NetInterfaceAddress> netInterface = new LoopbackInterfaceImpl<>(interfaceName);

            netInterface.setDriver(getDriver());
            netInterface.setDriverVersion(getDriverVersion());
            netInterface.setFirmwareVersion(getFirmwareVersion());
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
            netInterface.setNetInterfaceAddresses(getNetInterfaceAddresses(interfaceName, type, isUp));

            return netInterface;
        } else if (type == NetInterfaceType.WIFI) {
            WifiInterfaceImpl<WifiInterfaceAddress> wifiInterface = new WifiInterfaceImpl<>(interfaceName);

            Map<String, String> driver = LinuxNetworkUtil.getEthernetDriver(interfaceName);
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
            wifiInterface.setVirtual(isVirtual());
            wifiInterface.setUsbDevice(getUsbDevice(interfaceName));
            wifiInterface.setState(getState(interfaceName, isUp));
            wifiInterface.setNetInterfaceAddresses(getWifiInterfaceAddresses(interfaceName, isUp));
            wifiInterface.setCapabilities(LinuxNetworkUtil.getWifiCapabilities(interfaceName));

            return wifiInterface;
        } else if (type == NetInterfaceType.MODEM) {
            ModemDevice modemDevice = null;
            if (interfaceName.startsWith("ppp")) {
                // already connected - find the corresponding usb device
                modemDevice = this.usbModems.get(getModemUsbPort(interfaceName));
                if (modemDevice == null && this.serialModem != null) {
                    modemDevice = this.serialModem;
                }
            } else if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
                // the interface name is in the form of a usb port i.e. "1-3.4"
                modemDevice = this.usbModems.get(interfaceName);
            } else if (this.serialModem != null && interfaceName.equals(this.serialModem.getProductName())) {
                modemDevice = this.serialModem;
            }
            return modemDevice != null ? getModemInterface(interfaceName, isUp, modemDevice) : null;
        } else {
            if (interfaceName.startsWith("can")) {
                logger.trace("Ignoring CAN interface: {}", interfaceName);
            } else if (interfaceName.startsWith("ppp")) {
                logger.debug("Ignoring unconfigured ppp interface: {}", interfaceName);
            } else {
                logger.debug("Unsupported network type - not adding to network devices: {} of type: ", interfaceName,
                        type.toString());
            }
            return null;
        }
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("handleEvent() :: topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC)) {
            // validate mandatory properties
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY) == null) {
                return;
            }
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY) == null) {
                return;
            }
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY) == null) {
                return;
            }
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY) == null
                    || ((String) event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY)).startsWith("usb")) {
                return;
            }
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY) == null
                    || ((UsbDeviceType) event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY))
                            .equals(UsbDeviceType.USB_NET_DEVICE)) {
                return;
            }

            // do we care?
            final SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(
                    (String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
                    (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY),
                    (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY));
            if (modemInfo != null) {
                // Found one - see if we have some info for it.
                // Also check if we are getting more devices than expected.
                // This can happen if all the modem resources cannot be removed from the OS or from Kura.
                // In this case we did not receive an UsbDeviceRemovedEvent and we did not post
                // an ModemRemovedEvent. Should we do it here?
                List<? extends UsbModemDriver> drivers = modemInfo.getDeviceDrivers();
                for (UsbModemDriver driver : drivers) {
                    try {
                        driver.install();
                    } catch (Exception e) {
                        logger.error("handleEvent() :: Failed to install modem device driver {} ", driver.getName(), e);
                    }
                }

                UsbModemDevice usbModem = this.usbModems
                        .get(event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));

                boolean createNewUsbModemDevice = false;
                if (usbModem == null) {
                    logger.debug("handleEvent() :: Modem not found. Create one");
                    createNewUsbModemDevice = true;
                } else if (modemInfo.getNumTtyDevs() > 0 && modemInfo.getNumBlockDevs() > 0) {
                    if (usbModem.getTtyDevs().size() >= modemInfo.getNumTtyDevs()
                            && usbModem.getBlockDevs().size() >= modemInfo.getNumBlockDevs()) {
                        logger.debug("handleEvent() :: Found modem with too many resources: {}. Create a new one",
                                usbModem);
                        createNewUsbModemDevice = true;
                    }
                } else if (modemInfo.getNumTtyDevs() > 0 && usbModem.getTtyDevs().size() >= modemInfo.getNumTtyDevs()
                        || modemInfo.getNumBlockDevs() > 0
                                && usbModem.getBlockDevs().size() >= modemInfo.getNumBlockDevs()) {
                    logger.debug("handleEvent() :: Found modem with too many resources: {}. Create a new one",
                            usbModem);
                    createNewUsbModemDevice = true;
                }

                if (createNewUsbModemDevice) {
                    usbModem = new UsbModemDevice(
                            (String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
                            (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY),
                            (String) event.getProperty(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY),
                            (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY),
                            (String) event.getProperty(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY),
                            (String) event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY));
                }

                String resource = (String) event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY);
                Integer interfaceNumber = (Integer) event.getProperty(UsbDeviceEvent.USB_EVENT_USB_INTERFACE_NUMBER);
                UsbDeviceType usbDeviceType = (UsbDeviceType) event
                        .getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY);
                logger.debug("handleEvent() :: Found resource: {} of type {} for: {}", resource, usbDeviceType,
                        usbModem.getUsbPort());
                if (usbDeviceType.equals(UsbDeviceType.USB_TTY_DEVICE)) {
                    usbModem.addTtyDev(resource, interfaceNumber);
                } else if (usbDeviceType.equals(UsbDeviceType.USB_BLOCK_DEVICE)) {
                    usbModem.addBlockDev(resource);
                }

                this.usbModems.put(usbModem.getUsbPort(), usbModem);

                // At this point, we should have some modems - display them
                logger.info("handleEvent() :: Modified modem (Added resource): {}", usbModem);

                logger.debug("handleEvent() :: usbModem.getTtyDevs().size()={}, modemInfo.getNumTtyDevs()={}",
                        usbModem.getTtyDevs().size(), modemInfo.getNumTtyDevs());
                logger.debug("handleEvent() :: usbModem.getBlockDevs().size()={}, modemInfo.getNumBlockDevs()={}",
                        usbModem.getBlockDevs().size(), modemInfo.getNumBlockDevs());

                // Check for correct number of resources
                if (usbModem.getTtyDevs().size() == modemInfo.getNumTtyDevs()
                        && usbModem.getBlockDevs().size() == modemInfo.getNumBlockDevs()) {
                    logger.info("handleEvent() :: posting ModemAddedEvent -- USB_EVENT_DEVICE_ADDED_TOPIC: {}",
                            usbModem);
                    this.eventAdmin.postEvent(new ModemAddedEvent(usbModem));
                    this.addedModems.add(usbModem.getUsbPort());
                }
            }
        } else if (topic.equals(UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC)) {
            // validate mandatory properties
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY) == null) {
                return;
            }
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY) == null) {
                return;
            }
            if (event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY) == null) {
                return;
            }

            // do we care?
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(
                    (String) event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY),
                    (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY),
                    (String) event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY));
            if (modemInfo != null) {
                // found one - remove if it exists
                UsbModemDevice usbModem = this.usbModems
                        .remove(event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));
                if (usbModem != null) {
                    logger.info("handleEvent() :: Removing modem: {}", usbModem);
                    this.addedModems.remove(usbModem.getUsbPort());

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
        } else {
            logger.error("handleEvent() :: Unexpected event topic: {}", topic);
        }
    }

    private String getDriver() {
        // FIXME - hard coded
        return "unknown";
    }

    private String getDriverVersion() {
        // FIXME - hard coded
        return "unknown";
    }

    private String getFirmwareVersion() {
        // FIXME - hard coded
        return "unknown";
    }

    private ModemInterface<ModemInterfaceAddress> getModemInterface(String interfaceName, boolean isUp,
            ModemDevice modemDevice) throws KuraException {

        ModemInterfaceImpl<ModemInterfaceAddress> modemInterface = new ModemInterfaceImpl<>(interfaceName);

        modemInterface.setModemDevice(modemDevice);
        if (modemDevice instanceof UsbModemDevice) {

            UsbModemDevice usbModemDevice = (UsbModemDevice) modemDevice;
            SupportedUsbModemInfo supportedUsbModemInfo = SupportedUsbModemsInfo.getModem(usbModemDevice.getVendorId(),
                    usbModemDevice.getProductId(), usbModemDevice.getProductName());
            modemInterface.setTechnologyTypes(supportedUsbModemInfo.getTechnologyTypes());
            modemInterface.setUsbDevice((UsbModemDevice) modemDevice);
        }

        int pppNum = 0;
        if (interfaceName.startsWith("ppp")) {
            pppNum = Integer.parseInt(interfaceName.substring(3));
        }
        modemInterface.setPppNum(pppNum);
        modemInterface.setManufacturer(modemDevice.getManufacturerName());
        modemInterface.setModel(modemDevice.getProductName());
        modemInterface.setModemIdentifier(modemDevice.getProductName());

        // these properties required net.admin packages
        modemInterface.setDriver(getDriver());
        modemInterface.setDriverVersion(getDriverVersion());
        modemInterface.setFirmwareVersion(getFirmwareVersion());
        modemInterface.setSerialNumber("unknown");

        modemInterface.setLoopback(false);
        modemInterface.setPointToPoint(true);
        modemInterface.setState(getState(interfaceName, isUp));
        modemInterface.setHardwareAddress(new byte[] { 0, 0, 0, 0, 0, 0 });
        if (!interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
            LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
            if (ifconfig != null) {
                modemInterface.setMTU(ifconfig.getMtu());
                modemInterface.setSupportsMulticast(ifconfig.isMulticast());
            }
        }

        modemInterface.setUp(isUp);
        modemInterface.setVirtual(isVirtual());
        modemInterface.setNetInterfaceAddresses(getModemInterfaceAddresses(interfaceName, isUp));

        return modemInterface;

    }

    private List<NetInterfaceAddress> getNetInterfaceAddresses(String interfaceName, NetInterfaceType type,
            boolean isUp) throws KuraException {
        List<NetInterfaceAddress> netInterfaceAddresses = new ArrayList<>();
        if (isUp) {
            ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);
            NetInterfaceAddressImpl netInterfaceAddress = new NetInterfaceAddressImpl();
            try {
                LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
                if (ifconfig != null) {
                    String currentNetmask = ifconfig.getInetMask();
                    if (currentNetmask != null) {
                        netInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
                        netInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));

                        netInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
                        netInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
                        netInterfaceAddress.setGateway(conInfo.getGateway());
                        if (type == NetInterfaceType.MODEM) {
                            netInterfaceAddress.setDnsServers(LinuxDns.getInstance().getPppDnServers());
                        } else {
                            netInterfaceAddress.setDnsServers(conInfo.getDnsServers());
                        }
                        netInterfaceAddresses.add(netInterfaceAddress);
                    }
                }
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }
        return netInterfaceAddresses;
    }

    private List<WifiInterfaceAddress> getWifiInterfaceAddresses(String interfaceName, boolean isUp)
            throws KuraException {
        List<WifiInterfaceAddress> wifiInterfaceAddresses = new ArrayList<>();
        if (isUp) {
            ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);
            WifiInterfaceAddressImpl wifiInterfaceAddress = new WifiInterfaceAddressImpl();
            wifiInterfaceAddresses.add(wifiInterfaceAddress);
            try {
                LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
                if (ifconfig != null) {
                    String currentNetmask = ifconfig.getInetMask();
                    if (currentNetmask != null) {
                        wifiInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
                        wifiInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
                        wifiInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
                        wifiInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
                        wifiInterfaceAddress.setGateway(conInfo.getGateway());
                        wifiInterfaceAddress.setDnsServers(conInfo.getDnsServers());

                        WifiMode wifiMode = LinuxNetworkUtil.getWifiMode(interfaceName);
                        wifiInterfaceAddress.setBitrate(LinuxNetworkUtil.getWifiBitrate(interfaceName));
                        wifiInterfaceAddress.setMode(wifiMode);

                        // TODO - should this only be the AP we are connected to in client mode?
                        if (wifiMode == WifiMode.INFRA) {
                            String currentSSID = LinuxNetworkUtil.getSSID(interfaceName);

                            if (currentSSID != null) {
                                logger.debug("Adding access point SSID: {}", currentSSID);

                                WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(currentSSID);

                                // FIXME: fill in other info
                                wifiAccessPoint.setMode(WifiMode.INFRA);
                                List<Long> bitrate = new ArrayList<>();
                                bitrate.add(54000000L);
                                wifiAccessPoint.setBitrate(bitrate);
                                wifiAccessPoint.setFrequency(12345);
                                wifiAccessPoint.setHardwareAddress("20AA4B8A6442".getBytes());
                                wifiAccessPoint.setRsnSecurity(EnumSet.allOf(WifiSecurity.class));
                                wifiAccessPoint.setStrength(1234);
                                wifiAccessPoint.setWpaSecurity(EnumSet.allOf(WifiSecurity.class));

                                wifiInterfaceAddress.setWifiAccessPoint(wifiAccessPoint);
                            }
                        }
                    }
                }
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }

        return wifiInterfaceAddresses;
    }

    private List<ModemInterfaceAddress> getModemInterfaceAddresses(String interfaceName, boolean isUp)
            throws KuraException {
        List<ModemInterfaceAddress> modemInterfaceAddresses = new ArrayList<>();
        if (isUp) {
            ConnectionInfo conInfo = new ConnectionInfoImpl(interfaceName);
            ModemInterfaceAddressImpl modemInterfaceAddress = new ModemInterfaceAddressImpl();
            modemInterfaceAddresses.add(modemInterfaceAddress);
            try {
                LinuxIfconfig ifconfig = LinuxNetworkUtil.getInterfaceConfiguration(interfaceName);
                if (ifconfig != null) {
                    String currentNetmask = ifconfig.getInetMask();
                    if (currentNetmask != null) {
                        modemInterfaceAddress.setAddress(IPAddress.parseHostAddress(ifconfig.getInetAddress()));
                        modemInterfaceAddress.setBroadcast(IPAddress.parseHostAddress(ifconfig.getInetBcast()));
                        modemInterfaceAddress.setNetmask(IPAddress.parseHostAddress(currentNetmask));
                        modemInterfaceAddress.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(currentNetmask));
                        modemInterfaceAddress.setGateway(conInfo.getGateway());
                        modemInterfaceAddress.setDnsServers(conInfo.getDnsServers());
                        ModemConnectionStatus connectionStatus = ModemConnectionStatus.CONNECTED;
                        modemInterfaceAddress.setConnectionStatus(connectionStatus);
                        // TODO - other attributes
                    }
                }
            } catch (UnknownHostException e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
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

    /**
     * Given an interface name (e.g. 'ppp0'), look up the associated usb port
     * using the ppp peers config files
     */
    @Override
    public String getModemUsbPort(String interfaceName) {
        if (interfaceName != null) {
            File peersDir = new File(PPP_PEERS_DIR);
            if (peersDir.isDirectory()) {
                File[] peerFiles = peersDir.listFiles();
                for (File peerFile : peerFiles) {
                    if (peerFile.getName().equals(interfaceName)) {
                        // should be a symlink...find the file it links to
                        try {
                            String peerFilename = peerFile.getCanonicalFile().getName();
                            String[] filenameParts = peerFilename.split("_");
                            return filenameParts[filenameParts.length - 1];
                        } catch (IOException e) {
                            logger.error("Error splitting peer filename!", e);
                        }
                    }
                }
            }

            // Return the interface name if it looks like a usb port
            if (interfaceName.matches(UNCONFIGURED_MODEM_REGEX)) {
                return interfaceName;
            }
        }

        return null;
    }

    /**
     * Given a usb port address, look up the associated ppp interface name
     *
     * @throws KuraException
     */
    @Override
    public String getModemPppPort(ModemDevice modemDevice) throws KuraException {

        String deviceName = null;
        String modemId = null;

        if (modemDevice instanceof UsbModemDevice) {
            UsbModemDevice usbModem = (UsbModemDevice) modemDevice;
            SupportedUsbModemInfo modemInfo = SupportedUsbModemsInfo.getModem(usbModem.getVendorId(),
                    usbModem.getProductId(), usbModem.getProductName());
            deviceName = modemInfo.getDeviceName();
            modemId = usbModem.getUsbPort();
        } else if (modemDevice instanceof SerialModemDevice) {
            SerialModemDevice serialModemDevice = (SerialModemDevice) modemDevice;
            deviceName = serialModemDevice.getProductName();
            modemId = serialModemDevice.getProductName();
        }

        // find a matching config file in the ppp peers directory
        File peersDir = new File(PPP_PEERS_DIR);
        if (peersDir.isDirectory()) {
            File[] peerFiles = peersDir.listFiles();
            for (File peerFile : peerFiles) {
                String peerFilename = peerFile.getName();
                if (peerFilename.startsWith(deviceName) && peerFilename.endsWith(modemId)) {
                    try (FileReader fr = new FileReader(peerFile); BufferedReader br = new BufferedReader(fr)) {
                        String line = null;
                        StringBuilder sbIfaceName = null;
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("unit")) {
                                sbIfaceName = new StringBuilder("ppp");
                                sbIfaceName.append(line.substring("unit".length()).trim());
                                break;
                            }
                        }
                        return sbIfaceName != null ? sbIfaceName.toString() : null;
                    } catch (Exception e) {
                        logger.error("failed to parse peers file ", e);
                    }
                    break;
                }
            }
        }
        return null;
    }

    private boolean isVirtual() {
        // FIXME - assuming only one to one relationship for network interfaces today
        return false;
    }

    private void toggleModem(SupportedUsbModemInfo modemInfo) throws Exception {
        while (!stopThread.get()) {
            UsbModemDriver modemDriver = null;
            List<? extends UsbModemDriver> usbDeviceDrivers = modemInfo.getDeviceDrivers();
            if (usbDeviceDrivers != null && !usbDeviceDrivers.isEmpty()) {
                modemDriver = usbDeviceDrivers.get(0);
            }
            if (modemDriver != null) {
                boolean status = false;
                try {
                    logger.info("toggleModem() :: turning modem off ...");
                    modemDriver.disable();
                    sleep(3000);
                    logger.info("toggleModem() :: turning modem on ...");
                    modemDriver.enable();

                    logger.info("toggleModem() :: modem has been toggled successfully ...");
                    stopThread.set(status);
                    toggleModemNotity();

                } catch (Exception e) {
                    logger.error("toggleModem() :: failed to toggle modem ", e);
                }
            }
            if (!stopThread.get()) {
                toggleModemWait();
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void toggleModemNotity() {
        if (stopThread != null) {
            synchronized (stopThread) {
                stopThread.notifyAll();
            }
        }
    }

    private void toggleModemWait() throws InterruptedException {
        if (stopThread != null) {
            synchronized (stopThread) {
                stopThread.wait(TOOGLE_MODEM_THREAD_INTERVAL);
            }
        }
    }
}
