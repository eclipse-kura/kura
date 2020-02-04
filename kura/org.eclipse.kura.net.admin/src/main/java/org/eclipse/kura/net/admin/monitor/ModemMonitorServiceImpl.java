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
package org.eclipse.kura.net.admin.monitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedUsbModemsInfo;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.ConnectionInfo;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.admin.modem.EvdoCellularModem;
import org.eclipse.kura.net.admin.modem.HspaCellularModem;
import org.eclipse.kura.net.admin.modem.IModemLinkService;
import org.eclipse.kura.net.admin.modem.PppFactory;
import org.eclipse.kura.net.admin.modem.PppState;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedUsbModemsFactoryInfo.UsbModemFactoryInfo;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemAddedEvent;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.modem.ModemGpsDisabledEvent;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.eclipse.kura.net.modem.ModemInterface;
import org.eclipse.kura.net.modem.ModemManagerService;
import org.eclipse.kura.net.modem.ModemMonitorListener;
import org.eclipse.kura.net.modem.ModemMonitorService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.net.modem.ModemRemovedEvent;
import org.eclipse.kura.net.modem.ModemTechnologyType;
import org.eclipse.kura.net.modem.SerialModemDevice;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbModemDevice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemMonitorServiceImpl implements ModemMonitorService, ModemManagerService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModemMonitorServiceImpl.class);

    private static final long THREAD_INTERVAL = 30000;
    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds

    private Future<?> task;

    private SystemService systemService;
    private NetworkService networkService;
    private NetworkConfigurationService netConfigService;
    private EventAdmin eventAdmin;
    private CommandExecutorService executorService;

    private final Set<ModemMonitorListener> listeners = Collections.synchronizedSet(new HashSet<>());

    private ScheduledExecutorService executor = createExecutor();

    private final Map<String, MonitoredModem> modems = new ConcurrentHashMap<>();
    private Map<String, InterfaceState> interfaceStatuses = new HashMap<>();

    private NetworkConfiguration networkConfig;

    private final AtomicBoolean monitorRequestPending = new AtomicBoolean();
    private volatile boolean serviceActivated;
    private LinuxNetworkUtil linuxNetworkUtil;

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

    public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = netConfigService;
    }

    public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = null;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    protected void activate() {

        this.linuxNetworkUtil = new LinuxNetworkUtil(this.executorService);
        executor.execute(() -> {
            // track currently installed modems
            try {
                this.networkConfig = this.netConfigService.getNetworkConfiguration();
                for (NetInterface<? extends NetInterfaceAddress> netInterface : this.networkService
                        .getNetworkInterfaces()) {
                    if (netInterface instanceof ModemInterface) {
                        ModemDevice modemDevice = ((ModemInterface<?>) netInterface).getModemDevice();
                        trackModem(modemDevice);
                    }
                }
            } catch (Exception e) {
                logger.error("Error getting installed modems", e);
            }

            startMonitorTask();

            this.serviceActivated = true;
            logger.debug("ModemMonitor activated and ready to receive events");
        });
    }

    private Future<?> startMonitorTask() {
        if (task != null) {
            return task;
        }
        task = this.executor.scheduleWithFixedDelay(this::monitor, 0, THREAD_INTERVAL, TimeUnit.MILLISECONDS);
        return task;
    }

    private void stopMonitorTask() {
        if (this.task != null) {
            logger.debug("Cancelling ModemMonitor task ...");
            this.task.cancel(false);
            logger.info("ModemMonitor task cancelled? = {}", task.isDone());
            this.task = null;
        }
    }

    private void requestMonitor() {
        if (!monitorRequestPending.getAndSet(true)) {
            executor.execute(this::monitor);
        }
    }

    protected void deactivate() {

        stopMonitorTask();

        logger.debug("Terminating ModemMonitor Thread ...");
        this.executor.shutdownNow();
        try {
            this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Interrupted", e);
        }
        logger.info("ModemMonitor Thread terminated? - {}", this.executor.isTerminated());

        this.serviceActivated = false;

        this.networkConfig = null;
    }

    @Override
    public void handleEvent(final Event event) {
        logger.debug("handleEvent - topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {

            executor.execute(() -> {
                NetworkConfigurationChangeEvent netConfigChangedEvent = (NetworkConfigurationChangeEvent) event;
                String[] propNames = netConfigChangedEvent.getPropertyNames();
                if (propNames != null && propNames.length > 0) {
                    Map<String, Object> props = new HashMap<>();
                    for (String propName : propNames) {
                        Object prop = netConfigChangedEvent.getProperty(propName);
                        if (prop != null) {
                            props.put(propName, prop);
                        }
                    }

                    try {
                        final NetworkConfiguration newNetworkConfig = new NetworkConfiguration(props);
                        processNetworkConfigurationChangeEvent(newNetworkConfig);
                        requestMonitor();
                    } catch (Exception e) {
                        logger.error("Failed to handle the NetworkConfigurationChangeEvent ", e);
                    }
                }
            });
        } else if (topic.equals(ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC)) {
            ModemAddedEvent modemAddedEvent = (ModemAddedEvent) event;
            final ModemDevice modemDevice = modemAddedEvent.getModemDevice();
            if (this.serviceActivated) {
                trackModem(modemDevice);
            }
            requestMonitor();
        } else if (topic.equals(ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC)) {
            ModemRemovedEvent modemRemovedEvent = (ModemRemovedEvent) event;
            String usbPort = (String) modemRemovedEvent.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY);
            final MonitoredModem modem = this.modems.remove(usbPort);
            if (modem != null) {
                modem.release();
            }
        }
    }

    @Override
    public CellularModem getModemService(String usbPort) {
        final MonitoredModem modem = this.modems.get(usbPort);

        if (modem == null) {
            return null;
        }

        return modem.modem;
    }

    @Override
    public Collection<CellularModem> getAllModemServices() {
        return this.modems.values().stream().map(MonitoredModem::getModem).collect(Collectors.toList());
    }

    private NetInterfaceStatus getNetInterfaceStatus(List<NetConfig> netConfigs) {

        NetInterfaceStatus interfaceStatus = NetInterfaceStatus.netIPv4StatusUnknown;
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof NetConfigIP4) {
                    interfaceStatus = ((NetConfigIP4) netConfig).getStatus();
                    break;
                }
            }
        }
        return interfaceStatus;
    }

    private void setNetInterfaceStatus(NetInterfaceStatus netInterfaceStatus, List<NetConfig> netConfigs) {
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof NetConfigIP4) {
                    ((NetConfigIP4) netConfig).setStatus(netInterfaceStatus);
                    break;
                }
            }
        }
    }

    @Override
    public void registerListener(ModemMonitorListener newListener) {
        this.listeners.add(newListener);
    }

    @Override
    public void unregisterListener(ModemMonitorListener listenerToUnregister) {
        this.listeners.remove(listenerToUnregister);
    }

    private void processNetworkConfigurationChangeEvent(NetworkConfiguration newNetworkConfig) {
        if (this.modems == null || this.modems.isEmpty()) {
            return;
        }
        for (Map.Entry<String, MonitoredModem> modemEntry : this.modems.entrySet()) {
            String usbPort = modemEntry.getKey();
            final MonitoredModem monitoredModem = modemEntry.getValue();
            CellularModem modem = monitoredModem.getModem();
            try {
                String ifaceName = null;
                if (this.networkService != null) {
                    ifaceName = this.networkService.getModemPppPort(modem.getModemDevice());
                }
                if (ifaceName != null) {
                    List<NetConfig> oldNetConfigs = modem.getConfiguration();
                    NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = newNetworkConfig
                            .getNetInterfaceConfig(ifaceName);
                    if (netInterfaceConfig == null) {
                        netInterfaceConfig = newNetworkConfig.getNetInterfaceConfig(usbPort);
                    }
                    List<NetConfig> newNetConfigs = null;
                    IModemLinkService pppService = null;
                    int ifaceNo = getInterfaceNumber(oldNetConfigs);
                    if (ifaceNo >= 0) {
                        pppService = getPppService("ppp" + ifaceNo, modem.getDataPort());
                    }

                    if (netInterfaceConfig != null) {
                        newNetConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
                    } else {
                        if (oldNetConfigs != null && pppService != null
                                && !ifaceName.equals(pppService.getIfaceName())) {
                            StringBuilder key = new StringBuilder().append("net.interface.").append(ifaceName)
                                    .append(".config.ip4.status");
                            String statusString = KuranetConfig.getProperty(key.toString());
                            NetInterfaceStatus netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
                            if (statusString != null && !statusString.isEmpty()) {
                                netInterfaceStatus = NetInterfaceStatus.valueOf(statusString);
                            }

                            newNetConfigs = oldNetConfigs;
                            oldNetConfigs = null;
                            try {
                                setInterfaceNumber(ifaceName, newNetConfigs);
                                setNetInterfaceStatus(netInterfaceStatus, newNetConfigs);
                            } catch (NumberFormatException e) {
                                logger.error("failed to set new interface number ", e);
                            }
                        }
                    }

                    if (oldNetConfigs == null || !isConfigsEqual(oldNetConfigs, newNetConfigs)) {
                        logger.info("new configuration for cellular modem on usb port {} netinterface {}", usbPort,
                                ifaceName);
                        this.networkConfig = newNetworkConfig;
                        NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(newNetConfigs);
                        if (pppService != null && netInterfaceStatus != NetInterfaceStatus.netIPv4StatusUnmanaged) {
                            PppState pppSt = pppService.getPppState();
                            if (pppSt == PppState.CONNECTED || pppSt == PppState.IN_PROGRESS) {
                                logger.info("disconnecting " + pppService.getIfaceName());
                                pppService.disconnect();
                            }
                        }

                        if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                            logger.error("processNetworkConfigurationChangeEvent() :: Failed to disable modem GPS");
                            monitoredModem.forceReset();
                        }

                        modem.setConfiguration(newNetConfigs);

                        if (modem.hasDiversityAntenna()) {
                            if (isDiversityEnabledInConfig(newNetConfigs)) {
                                if (!modem.isDiversityEnabled()) {
                                    modem.enableDiversity();
                                }
                            } else {
                                if (modem.isDiversityEnabled()) {
                                    modem.disableDiversity();
                                }
                            }
                        }

                        if (modem instanceof EvdoCellularModem) {
                            NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(newNetConfigs);
                            if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {

                                if (!((EvdoCellularModem) modem).isProvisioned()) {
                                    logger.info(
                                            "NetworkConfigurationChangeEvent :: The {} is not provisioned, will try to provision it ...",
                                            modem.getModel());

                                    ((EvdoCellularModem) modem).provision();

                                } else {
                                    logger.info("NetworkConfigurationChangeEvent :: The {} is provisioned",
                                            modem.getModel());
                                }
                            }

                            if (modem.isGpsSupported() && isGpsEnabledInConfig(newNetConfigs)
                                    && !modem.isGpsEnabled()) {
                                modem.enableGps();
                                postModemGpsEvent(modem, true);
                            }
                        }
                    }
                }
            } catch (KuraException e) {
                logger.error("NetworkConfigurationChangeEvent :: Failed to process ", e);
            }
        }
    }

    private boolean isConfigsEqual(List<NetConfig> oldConfig, List<NetConfig> newConfig) {
        if (oldConfig == null || newConfig == null) {
            return false;
        }
        boolean ret = false;
        ModemConfig oldModemConfig = getModemConfig(oldConfig);
        ModemConfig newModemConfig = getModemConfig(newConfig);
        NetConfigIP4 oldNetConfigIP4 = getNetConfigIp4(oldConfig);
        NetConfigIP4 newNetConfigIP4 = getNetConfigIp4(newConfig);
        if (oldNetConfigIP4.equals(newNetConfigIP4) && oldModemConfig.equals(newModemConfig)) {
            ret = true;
        }
        logger.info("old diversity = {},  new diversity = {}", oldModemConfig.isDiversityEnabled(),
                newModemConfig.isDiversityEnabled());
        if (oldModemConfig.isDiversityEnabled() != newModemConfig.isDiversityEnabled())
            ret = false;
        return ret;
    }

    private ModemConfig getModemConfig(List<NetConfig> netConfigs) {
        ModemConfig modemConfig = new ModemConfig();
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof ModemConfig) {
                modemConfig = (ModemConfig) netConfig;
                break;
            }
        }
        return modemConfig;
    }

    private NetConfigIP4 getNetConfigIp4(List<NetConfig> netConfigs) {
        NetConfigIP4 netConfigIP4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusUnknown, false);
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof NetConfigIP4) {
                netConfigIP4 = (NetConfigIP4) netConfig;
                break;
            }
        }
        return netConfigIP4;
    }

    private int getInterfaceNumber(List<NetConfig> netConfigs) {
        int ifaceNo = -1;
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    ifaceNo = ((ModemConfig) netConfig).getPppNumber();
                    break;
                }
            }
        }
        return ifaceNo;
    }

    private int getInterfaceNumber(String ifaceName) {
        return Integer.parseInt(ifaceName.replaceAll("[^0-9]", ""));
    }

    private void setInterfaceNumber(String ifaceName, List<NetConfig> netConfigs) {
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    ((ModemConfig) netConfig).setPppNumber(getInterfaceNumber(ifaceName));
                    break;
                }
            }
        }
    }

    long getModemResetTimeoutMsec(String ifaceName, List<NetConfig> netConfigs) {
        long resetToutMsec = 0L;

        if (ifaceName != null && netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    resetToutMsec = ((ModemConfig) netConfig).getResetTimeout() * 60000;
                    break;
                }
            }
        }
        return resetToutMsec;
    }

    private boolean isGpsEnabledInConfig(List<NetConfig> netConfigs) {
        boolean isGpsEnabled = false;
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    isGpsEnabled = ((ModemConfig) netConfig).isGpsEnabled();
                    break;
                }
            }
        }
        return isGpsEnabled;
    }

    private boolean isDiversityEnabledInConfig(List<NetConfig> netConfigs) {
        boolean isDiversityEnabled = false;
        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    isDiversityEnabled = ((ModemConfig) netConfig).isDiversityEnabled();
                    break;
                }
            }
        }
        return isDiversityEnabled;
    }

    private void monitor() {
        try {
            monitorRequestPending.set(false);

            if (this.modems.isEmpty()) {
                return;
            }

            final HashMap<String, InterfaceState> newInterfaceStatuses = new HashMap<>();

            logger.debug("tracked modems: {}", modems.keySet());

            for (final Entry<String, MonitoredModem> e : this.modems.entrySet()) {

                logger.debug("processing modem {}", e.getKey());
                processMonitor(newInterfaceStatuses, e.getKey(), e.getValue());
            }

            // post event for any status changes
            checkStatusChange(this.interfaceStatuses, newInterfaceStatuses);
            this.interfaceStatuses = newInterfaceStatuses;
        } catch (Exception ex) {
            logger.error("Unexpected error during monitoring", ex);
        }
    }

    private void processMonitor(final HashMap<String, InterfaceState> newInterfaceStatuses, final String modemName,
            final MonitoredModem modem) {
        try {
            modem.monitor(newInterfaceStatuses);
        } catch (Exception ex) {
            logger.error("Failed to monitor {}", modemName, ex);
        }
    }

    private void checkStatusChange(Map<String, InterfaceState> oldStatuses, Map<String, InterfaceState> newStatuses) {

        if (newStatuses != null) {
            // post NetworkStatusChangeEvent on current and new interfaces
            for (Map.Entry<String, InterfaceState> newStatus : newStatuses.entrySet()) {
                String interfaceName = newStatus.getKey();
                InterfaceState interfaceState = newStatus.getValue();
                if (oldStatuses != null && oldStatuses.containsKey(interfaceName)) {
                    if (!interfaceState.equals(oldStatuses.get(interfaceName))) {
                        logger.debug("Posting NetworkStatusChangeEvent on interface: {}", interfaceName);
                        this.eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, interfaceState, null));
                    }
                } else {
                    logger.debug("Posting NetworkStatusChangeEvent on enabled interface: {}", interfaceName);
                    this.eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, interfaceState, null));
                }
            }

            // post NetworkStatusChangeEvent on interfaces that are no longer there
            if (oldStatuses != null) {
                for (Map.Entry<String, InterfaceState> oldStatus : oldStatuses.entrySet()) {
                    String interfaceName = oldStatus.getKey();
                    InterfaceState interfaceState = oldStatus.getValue();
                    if (!newStatuses.containsKey(interfaceName)) {
                        logger.debug("Posting NetworkStatusChangeEvent on disabled interface: {}", interfaceName);
                        this.eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, interfaceState, null));
                    }
                }
            }
        }
    }

    private void trackModem(ModemDevice modemDevice) {
        try {
            final CellularModemFactory modemFactoryService = getCellularModemFactory(modemDevice);

            String platform = null;
            if (this.systemService != null) {
                platform = this.systemService.getPlatform();
            }
            CellularModem modem = modemFactoryService.obtainCellularModemService(modemDevice, platform);
            try {
                HashMap<String, String> modemInfoMap = new HashMap<>();
                modemInfoMap.put(ModemReadyEvent.IMEI, modem.getSerialNumber());
                modemInfoMap.put(ModemReadyEvent.IMSI, modem.getMobileSubscriberIdentity());
                modemInfoMap.put(ModemReadyEvent.ICCID, modem.getIntegratedCirquitCardId());
                modemInfoMap.put(ModemReadyEvent.RSSI, Integer.toString(modem.getSignalStrength()));
                logger.info("posting ModemReadyEvent on topic {}", ModemReadyEvent.MODEM_EVENT_READY_TOPIC);
                this.eventAdmin.postEvent(new ModemReadyEvent(modemInfoMap));
            } catch (Exception e) {
                logger.error("Failed to post the ModemReadyEvent", e);
            }

            if (modemDevice instanceof UsbModemDevice) {
                this.modems.put(((UsbModemDevice) modemDevice).getUsbPort(),
                        new MonitoredModem(modem, this.linuxNetworkUtil));
            } else if (modemDevice instanceof SerialModemDevice) {
                this.modems.put(modemDevice.getProductName(), new MonitoredModem(modem, this.linuxNetworkUtil));
            }
        } catch (Exception e) {
            logger.error("trackModem() :: {}", e.getMessage(), e);
        }
    }

    protected CellularModemFactory getCellularModemFactory(final ModemDevice modemDevice)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Class<? extends CellularModemFactory> modemFactoryClass = getModemFactoryClass(modemDevice);

        if (modemFactoryClass == null) {
            throw new IllegalArgumentException("No ModemFactory associated with specified modemDevice");
        }

        CellularModemFactory modemFactoryService;

        Method getInstanceMethod = modemFactoryClass.getDeclaredMethod("getInstance", (Class<?>[]) null);
        getInstanceMethod.setAccessible(true);
        modemFactoryService = (CellularModemFactory) getInstanceMethod.invoke(null, (Object[]) null);

        // if unsuccessful in calling getInstance()
        if (modemFactoryService == null) {
            modemFactoryService = modemFactoryClass.newInstance();
        }

        return modemFactoryService;
    }

    protected Class<? extends CellularModemFactory> getModemFactoryClass(ModemDevice modemDevice) {
        Class<? extends CellularModemFactory> modemFactoryClass = null;

        if (modemDevice instanceof UsbModemDevice) {
            SupportedUsbModemInfo supportedUsbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) modemDevice);
            UsbModemFactoryInfo usbModemFactoryInfo = SupportedUsbModemsFactoryInfo.getModem(supportedUsbModemInfo);
            modemFactoryClass = usbModemFactoryInfo.getModemFactoryClass();
        }

        return modemFactoryClass;

    }

    ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    void addModem(final String intf, final CellularModem modem) {
        this.modems.put(intf, new MonitoredModem(modem, this.linuxNetworkUtil));
    }

    void sync() throws InterruptedException, ExecutionException {
        this.executor.submit(() -> {
        }).get();
    }

    IModemLinkService getPppService(final String interfaceName, final String port) {
        return PppFactory.getPppService(interfaceName, port, this.executorService);
    }

    private boolean disableModemGps(CellularModem modem) {

        try {
            postModemGpsEvent(modem, false);

            long startTimer = System.currentTimeMillis();
            do {
                try {
                    Thread.sleep(3000);
                    if (modem.isPortReachable(modem.getAtPort())) {
                        logger.debug("disableModemGps() modem is now reachable ...");

                        modem.disableGps();
                        final boolean isGpsEnabled = modem.isGpsEnabled();

                        logger.debug("disableModemGps() modem.isGpsEnabled()={} ...", isGpsEnabled);
                        return !isGpsEnabled;
                    } else {
                        logger.debug("disableModemGps() waiting for PositionService to release serial port ...");
                    }
                } catch (Exception e) {
                    logger.debug("disableModemGps() waiting for PositionService to release serial port ", e);
                }
            } while (System.currentTimeMillis() - startTimer < 20000L);

            logger.error("disableModemGps() :: portIsReachable=false");
            return false;

        } catch (Exception e) {
            logger.error("disableModemGps() :: failed due to excetpion", e);
            return false;
        }
    }

    private void postModemGpsEvent(CellularModem modem, boolean enabled) throws KuraException {

        if (enabled) {
            CommURI commUri = modem.getSerialConnectionProperties(CellularModem.SerialPortType.GPSPORT);
            if (commUri != null) {
                logger.trace("postModemGpsEvent() :: Modem SeralConnectionProperties: {}", commUri.toString());

                HashMap<String, Object> modemInfoMap = new HashMap<>();
                modemInfoMap.put(ModemGpsEnabledEvent.PORT, modem.getGpsPort());
                modemInfoMap.put(ModemGpsEnabledEvent.BAUD_RATE, Integer.valueOf(commUri.getBaudRate()));
                modemInfoMap.put(ModemGpsEnabledEvent.DATA_BITS, Integer.valueOf(commUri.getDataBits()));
                modemInfoMap.put(ModemGpsEnabledEvent.STOP_BITS, Integer.valueOf(commUri.getStopBits()));
                modemInfoMap.put(ModemGpsEnabledEvent.PARITY, Integer.valueOf(commUri.getParity()));

                logger.debug("postModemGpsEvent() :: posting ModemGpsEnabledEvent on topic {}",
                        ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC);
                this.eventAdmin.postEvent(new ModemGpsEnabledEvent(modemInfoMap));
            }
        } else {
            logger.debug("postModemGpsEvent() :: posting ModemGpsDisableEvent on topic {}",
                    ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC);
            HashMap<String, Object> modemInfoMap = new HashMap<>();
            this.eventAdmin.postEvent(new ModemGpsDisabledEvent(modemInfoMap));
        }
    }

    class ModemResetTimer {

        private long startTime = -1;

        void restart() {
            startTime = -1;
        }

        boolean shouldResetModem(final long modemResetTimeout) {
            
            if (modemResetTimeout == 0) {
                return false;
            }

            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            }

            final long timeTillReset = modemResetTimeout - (System.currentTimeMillis() - startTime);
            final boolean shouldReset = timeTillReset <= 0;

            if (!shouldReset) {
                logger.info("monitor() :: Modem will be reset in {} sec if not connected", timeTillReset / 1000);
            }

            return shouldReset;
        }
    }

    class MonitoredModem {

        private final CellularModem modem;
        private final ModemResetTimer resetTimer;

        private AtomicBoolean isValid = new AtomicBoolean(true);
        private boolean isInitialized;
        private PppState pppState = PppState.NOT_CONNECTED;
        private LinuxNetworkUtil linuxNetworkUtil;

        public MonitoredModem(final CellularModem modem, LinuxNetworkUtil linuxNetworkUtil) {
            this.modem = modem;
            this.resetTimer = new ModemResetTimer();
            this.linuxNetworkUtil = linuxNetworkUtil;
        }

        CellularModem getModem() {
            return modem;
        }

        boolean shouldCheckSimCard() throws KuraException {
            return modem.getTechnologyTypes().stream()
                    .anyMatch(t -> t == ModemTechnologyType.GSM_GPRS || t == ModemTechnologyType.UMTS
                            || t == ModemTechnologyType.HSDPA || t == ModemTechnologyType.HSPA);
        }

        boolean isSimCardReady() throws KuraException {
            if (!shouldCheckSimCard()) {
                return true;
            }

            final boolean isSimCardReady = ((HspaCellularModem) modem).isSimCardReady();

            if (isSimCardReady) {
                logger.info("monitor() :: !!! SIM CARD IS READY !!!");
            } else {
                logger.warn("monitor() :: ! SIM CARD IS NOT READY !");
            }

            return isSimCardReady;
        }

        void release() {
            try {
                logger.debug("Releasing modem device from factory...");
                this.isValid.set(false);
                final CellularModemFactory factory = getCellularModemFactory(modem.getModemDevice());
                factory.releaseModemService(modem);
                logger.debug("Releasing modem device from factory...done");
            } catch (Exception e) {
                logger.warn("Failed to release modem device from factory", e);
            }
        }

        void reportSignalStrength(String interfaceName) {

            if (listeners.isEmpty()) {
                return;
            }

            int rssi = 0;

            try {
                rssi = modem.getSignalStrength();
            } catch (KuraException e) {
                logger.error("monitor() :: Failed to obtain signal strength - {}", e);
            }

            for (final ModemMonitorListener listener : listeners) {
                listener.setCellularSignalLevel(interfaceName, rssi);
            }
        }

        void cleanupAndReset(final IModemLinkService pppService, final PppState pppSt) {

            try {
                if (pppService != null && pppSt != null) {
                    logger.info("monitor() :: PPPD disconnect");
                    pppService.disconnect();
                }
            } catch (final Exception e) {
                logger.error("monitor() :: Exception while disconnect", e);
            }

            try {
                if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                    logger.error("monitor() :: Failed to disable modem GPS");
                }
            } catch (final Exception e) {
                logger.error("monitor() :: Exception while disabling modem GPS", e);
            }

            forceReset();
        }

        void forceReset() {
            this.resetTimer.restart();
            this.pppState = PppState.NOT_CONNECTED;

            try {
                logger.info("monitor() :: modem reset");
                modem.reset();
            } catch (Exception e) {
                logger.error("monitor() :: Exception during modem reset", e);
            }
        }

        void initialize() throws KuraException {

            String ifaceName = networkService.getModemPppPort(modem.getModemDevice());
            List<NetConfig> netConfigs = null;
            if (ifaceName != null) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = networkConfig
                        .getNetInterfaceConfig(ifaceName);

                if (netInterfaceConfig == null) {
                    networkConfig = netConfigService.getNetworkConfiguration();
                    netInterfaceConfig = networkConfig.getNetInterfaceConfig(ifaceName);
                }

                if (netInterfaceConfig != null) {
                    netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
                    if (netConfigs != null && !netConfigs.isEmpty()) {
                        modem.setConfiguration(netConfigs);
                    }
                }
            }

            if (modem instanceof EvdoCellularModem) {
                NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(netConfigs);
                if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                    // TODO check if EVDO-specific gps handling is necessary

                    if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                        logger.error("initialize() :: Failed to disable modem GPS, resetting modem ...");
                        throw new KuraException(KuraErrorCode.CLOSED_DEVICE);
                    }

                    if (!((EvdoCellularModem) modem).isProvisioned()) {
                        logger.info("trackModem() :: The {} is not provisioned, will try to provision it ...",
                                modem.getModel());
                        ((EvdoCellularModem) modem).provision();
                    } else {
                        logger.info("trackModem() :: The {} is provisioned", modem.getModel());
                    }
                }

                if (modem.isGpsSupported() && isGpsEnabledInConfig(netConfigs) && !modem.isGpsEnabled()) {
                    modem.enableGps();
                    postModemGpsEvent(modem, true);
                }
            }

            isInitialized = true;
        }

        void monitor(final HashMap<String, InterfaceState> newInterfaceStatuses) {

            if (!isValid.get()) {
                return;
            }

            IModemLinkService pppService = null;
            PppState pppSt = null;

            try {
                if (!isInitialized) {
                    initialize();
                }

                boolean modemReset = false;

                NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(modem.getConfiguration());

                final String ifaceName = networkService.getModemPppPort(modem.getModemDevice());

                if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusUnmanaged) {
                    logger.warn("The {} interface is configured not to be managed by Kura and will not be monitored.",
                            ifaceName);
                    return;
                }
                if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN && ifaceName != null) {

                    reportSignalStrength(ifaceName);

                    pppService = getPppService(ifaceName, modem.getDataPort());
                    pppSt = pppService.getPppState();

                    if (this.pppState != pppSt) {
                        logger.info("monitor() :: previous PppState={}", this.pppState);
                        logger.info("monitor() :: current PppState={}", pppSt);
                    }

                    boolean isSimCardReady = true;

                    if (pppSt == PppState.NOT_CONNECTED) {
                        isSimCardReady = isSimCardReady();

                        if (isSimCardReady) {
                            logger.info("monitor() :: connecting ...");
                            pppService.connect();
                        }
                    }

                    if (pppSt == PppState.CONNECTED) {
                        resetTimer.restart();
                    } else if (isSimCardReady) {
                        final long modemResetTimeout = getModemResetTimeoutMsec(ifaceName, modem.getConfiguration());

                        if (resetTimer.shouldResetModem(modemResetTimeout)) {
                            logger.info("monitor() :: Modem Reset TIMEOUT !!!");
                            cleanupAndReset(pppService, pppSt);
                            modemReset = true;
                        }
                    }

                    this.pppState = pppSt;
                    ConnectionInfo connInfo = new ConnectionInfoImpl(ifaceName);
                    InterfaceState interfaceState = new InterfaceState(ifaceName,
                            this.linuxNetworkUtil.hasAddress(ifaceName), pppSt == PppState.CONNECTED,
                            connInfo.getIpAddress(), this.linuxNetworkUtil.getCarrierChanges(ifaceName));
                    newInterfaceStatuses.put(ifaceName, interfaceState);

                } else {
                    resetTimer.restart();
                }

                // If the modem has been reset in this iteration of the monitor,
                // do not immediately enable GPS to avoid concurrency issues due to asynchronous
                // events
                // (possible serial port contention between the PositionService and the
                // trackModem() method),
                // GPS will be eventually enabled in the next iteration of the monitor.
                if (!modemReset && modem.isGpsSupported() && isGpsEnabledInConfig(modem.getConfiguration())) {
                    if (modem instanceof HspaCellularModem && !modem.isGpsEnabled()) {
                        modem.enableGps();
                    }
                    postModemGpsEvent(modem, true);
                }

                // Same process for eventual DIV antenna.
                if (!modemReset && modem.hasDiversityAntenna()) {
                    if (isDiversityEnabledInConfig(modem.getConfiguration())) {
                        if (!modem.isDiversityEnabled()) {
                            modem.enableDiversity();
                        }
                    } else if (modem.isDiversityEnabled()) {
                        modem.disableDiversity();
                    }
                }

            } catch (Exception e) {
                logger.error("monitor() :: Exception, resetting modem", e);
                cleanupAndReset(pppService, pppSt);
            }
        }
    }
}
