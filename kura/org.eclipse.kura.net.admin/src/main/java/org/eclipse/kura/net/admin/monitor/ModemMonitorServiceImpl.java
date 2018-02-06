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
package org.eclipse.kura.net.admin.monitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.linux.net.ConnectionInfoImpl;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemInfo;
import org.eclipse.kura.linux.net.modem.SupportedSerialModemsInfo;
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
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo;
import org.eclipse.kura.net.admin.modem.SupportedSerialModemsFactoryInfo.SerialModemFactoryInfo;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemMonitorServiceImpl implements ModemMonitorService, ModemManagerService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModemMonitorServiceImpl.class);

    private static final String[] EVENT_TOPICS = new String[] {
            NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC, ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC,
            ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC, };

    private static final long THREAD_INTERVAL = 30000;
    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds

    private static Object lock = new Object();

    private Future<?> task;
    private static AtomicBoolean stopThread;
    private SystemService systemService;
    private NetworkService networkService;
    private NetworkConfigurationService netConfigService;
    private EventAdmin eventAdmin;

    private List<ModemMonitorListener> listeners;

    private ExecutorService executor;

    private Map<String, CellularModem> modems;
    private Map<String, InterfaceState> interfaceStatuses;

    private NetworkConfiguration networkConfig;

    private boolean serviceActivated;

    private PppState pppState;
    private long resetTimerStart;

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

    protected void activate(ComponentContext componentContext) {

        this.pppState = PppState.NOT_CONNECTED;
        this.resetTimerStart = 0L;

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);

        this.modems = new HashMap<>();
        this.interfaceStatuses = new HashMap<>();
        this.listeners = new ArrayList<>();

        stopThread = new AtomicBoolean();

        // must be initialized before trackModem() is called; risk of NPE otherwise
        this.executor = Executors.newSingleThreadExecutor();

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

        submitMonitorTask();

        this.serviceActivated = true;
        logger.debug("ModemMonitor activated and ready to receive events");
    }

    private Future<?> submitMonitorTask() {
        stopThread.set(false);

        // is task already prepared?
        if (task != null) {
            return task;
        }

        // is executor ready?
        if (this.executor == null) {
            return null;
        }

        task = this.executor.submit(() -> {
            while (!stopThread.get()) {
                Thread.currentThread().setName("ModemMonitor");
                try {
                    monitor();
                    monitorWait();
                } catch (InterruptedException interruptedException) {
                    Thread.interrupted();
                    logger.debug("modem monitor interrupted", interruptedException);
                } catch (Throwable t) {
                    logger.error("Exception while monitoring cellular connection", t);
                }
            }
        });
        return task;
    }

    protected void deactivate(ComponentContext componentContext) {
        this.listeners = null;
        PppFactory.releaseAllPppServices();
        if (task != null && !task.isDone()) {
            stopThread.set(true);
            monitorNotify();
            logger.debug("Cancelling ModemMonitor task ...");
            task.cancel(true);
            logger.info("ModemMonitor task cancelled? = {}", task.isDone());
            task = null;
        }

        if (this.executor != null) {
            logger.debug("Terminating ModemMonitor Thread ...");
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
            }
            logger.info("ModemMonitor Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }
        this.serviceActivated = false;

        this.networkConfig = null;
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("handleEvent - topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {

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
                    ExecutorService ex = Executors.newSingleThreadExecutor();
                    ex.submit(() -> processNetworkConfigurationChangeEvent(newNetworkConfig));
                } catch (Exception e) {
                    logger.error("Failed to handle the NetworkConfigurationChangeEvent ", e);
                }
            }
        } else if (topic.equals(ModemAddedEvent.MODEM_EVENT_ADDED_TOPIC)) {
            ModemAddedEvent modemAddedEvent = (ModemAddedEvent) event;
            final ModemDevice modemDevice = modemAddedEvent.getModemDevice();
            if (this.serviceActivated) {
                ExecutorService ex = Executors.newSingleThreadExecutor();
                ex.submit(() -> trackModem(modemDevice));
            }
        } else if (topic.equals(ModemRemovedEvent.MODEM_EVENT_REMOVED_TOPIC)) {
            ModemRemovedEvent modemRemovedEvent = (ModemRemovedEvent) event;
            String usbPort = (String) modemRemovedEvent.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY);
            this.modems.remove(usbPort);
        }
    }

    @Override
    public CellularModem getModemService(String usbPort) {
        return this.modems.get(usbPort);
    }

    @Override
    public Collection<CellularModem> getAllModemServices() {
        return this.modems.values();
    }

    private NetInterfaceStatus getNetInterfaceStatus(List<NetConfig> netConfigs) {

        NetInterfaceStatus interfaceStatus = NetInterfaceStatus.netIPv4StatusUnknown;
        if (netConfigs != null && !netConfigs.isEmpty()) {
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
        if (netConfigs != null && !netConfigs.isEmpty()) {
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
        boolean found = false;
        if (this.listeners == null) {
            this.listeners = new ArrayList<>();
        }
        if (!this.listeners.isEmpty()) {
            for (ModemMonitorListener listener : this.listeners) {
                if (listener.equals(newListener)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            this.listeners.add(newListener);
        }
    }

    @Override
    public void unregisterListener(ModemMonitorListener listenerToUnregister) {
        if (this.listeners != null && !this.listeners.isEmpty()) {

            for (int i = 0; i < this.listeners.size(); i++) {
                if (this.listeners.get(i).equals(listenerToUnregister)) {
                    this.listeners.remove(i);
                }
            }
        }
    }

    private void processNetworkConfigurationChangeEvent(NetworkConfiguration newNetworkConfig) {
        synchronized (lock) {
            if (this.modems == null || this.modems.isEmpty()) {
                return;
            }
            for (Map.Entry<String, CellularModem> modemEntry : this.modems.entrySet()) {
                String usbPort = modemEntry.getKey();
                CellularModem modem = modemEntry.getValue();
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
                            pppService = PppFactory.obtainPppService(ifaceNo, modem.getDataPort());
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
                                PppFactory.releasePppService(pppService.getIfaceName());
                            }

                            if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                                logger.error("processNetworkConfigurationChangeEvent() :: Failed to disable modem GPS");
                                modem.reset();
                                this.resetTimerStart = System.currentTimeMillis();
                            }

                            modem.setConfiguration(newNetConfigs);

                            if (modem instanceof EvdoCellularModem) {
                                NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(newNetConfigs);
                                if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {

                                    if (!((EvdoCellularModem) modem).isProvisioned()) {
                                        logger.info(
                                                "NetworkConfigurationChangeEvent :: The {} is not provisioned, will try to provision it ...",
                                                modem.getModel());

                                        if (task != null && !task.isCancelled()) {
                                            logger.info("NetworkConfigurationChangeEvent :: Cancelling monitor task");
                                            stopThread.set(true);
                                            monitorNotify();
                                            task.cancel(true);
                                            task = null;
                                        }

                                        ((EvdoCellularModem) modem).provision();
                                        if (task == null) {
                                            logger.info("NetworkConfigurationChangeEvent :: Restarting monitor task");

                                            submitMonitorTask();
                                        } else {
                                            monitorNotify();
                                        }
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
        if (netConfigs != null && !netConfigs.isEmpty()) {
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
        if (netConfigs != null && !netConfigs.isEmpty()) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    ((ModemConfig) netConfig).setPppNumber(getInterfaceNumber(ifaceName));
                    break;
                }
            }
        }
    }

    private long getModemResetTimeoutMsec(String ifaceName, List<NetConfig> netConfigs) {
        long resetToutMsec = 0L;

        if (ifaceName != null && netConfigs != null && !netConfigs.isEmpty()) {
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
        if (netConfigs != null && !netConfigs.isEmpty()) {
            for (NetConfig netConfig : netConfigs) {
                if (netConfig instanceof ModemConfig) {
                    isGpsEnabled = ((ModemConfig) netConfig).isGpsEnabled();
                    break;
                }
            }
        }
        return isGpsEnabled;
    }

    private void monitor() {
        synchronized (lock) {
            HashMap<String, InterfaceState> newInterfaceStatuses = new HashMap<>();
            if (this.modems == null || this.modems.isEmpty()) {
                return;
            }
            for (Map.Entry<String, CellularModem> modemEntry : this.modems.entrySet()) {
                CellularModem modem = modemEntry.getValue();
                // get signal strength only if somebody needs it
                if (this.listeners != null && !this.listeners.isEmpty()) {
                    for (ModemMonitorListener listener : this.listeners) {
                        try {
                            int rssi = modem.getSignalStrength();
                            listener.setCellularSignalLevel(rssi);
                        } catch (KuraException e) {
                            listener.setCellularSignalLevel(0);
                            logger.error("monitor() :: Failed to obtain signal strength - {}", e);
                        }
                    }
                }

                IModemLinkService pppService = null;
                PppState pppSt = null;
                NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(modem.getConfiguration());
                try {
                    String ifaceName = this.networkService.getModemPppPort(modem.getModemDevice());
                    if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusUnmanaged) {
                        logger.warn(
                                "The {} interface is configured not to be managed by Kura and will not be monitored.",
                                ifaceName);
                        continue;
                    }
                    if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN && ifaceName != null) {
                        pppService = PppFactory.obtainPppService(ifaceName, modem.getDataPort());
                        pppSt = pppService.getPppState();
                        if (this.pppState != pppSt) {
                            logger.info("monitor() :: previous PppState={}", this.pppState);
                            logger.info("monitor() :: current PppState={}", pppSt);
                        }

                        if (pppSt == PppState.NOT_CONNECTED) {
                            boolean checkIfSimCardReady = false;
                            List<ModemTechnologyType> modemTechnologyTypes = modem.getTechnologyTypes();
                            for (ModemTechnologyType modemTechnologyType : modemTechnologyTypes) {
                                if (modemTechnologyType == ModemTechnologyType.GSM_GPRS
                                        || modemTechnologyType == ModemTechnologyType.UMTS
                                        || modemTechnologyType == ModemTechnologyType.HSDPA
                                        || modemTechnologyType == ModemTechnologyType.HSPA) {
                                    checkIfSimCardReady = true;
                                    break;
                                }
                            }
                            if (checkIfSimCardReady) {
                                if (((HspaCellularModem) modem).isSimCardReady()) {
                                    logger.info("monitor() :: !!! SIM CARD IS READY !!! connecting ...");
                                    pppService.connect();
                                    if (this.pppState == PppState.NOT_CONNECTED) {
                                        this.resetTimerStart = System.currentTimeMillis();
                                    }
                                } else {
                                    logger.warn("monitor() :: ! SIM CARD IS NOT READY !");
                                }
                            } else {
                                logger.info("monitor() :: connecting ...");
                                pppService.connect();
                                if (this.pppState == PppState.NOT_CONNECTED) {
                                    this.resetTimerStart = System.currentTimeMillis();
                                }
                            }
                        } else if (pppSt == PppState.IN_PROGRESS) {
                            long modemResetTout = getModemResetTimeoutMsec(ifaceName, modem.getConfiguration());
                            if (modemResetTout > 0) {
                                long timeElapsed = System.currentTimeMillis() - this.resetTimerStart;
                                if (timeElapsed > modemResetTout) {
                                    // reset modem
                                    logger.info("monitor() :: Modem Reset TIMEOUT !!!");
                                    pppService.disconnect();
                                    if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                                        logger.error("monitor() :: Failed to disable modem GPS");
                                    }
                                    modem.reset();
                                    pppSt = PppState.NOT_CONNECTED;
                                    this.resetTimerStart = System.currentTimeMillis();
                                } else {
                                    int timeTillReset = (int) (modemResetTout - timeElapsed) / 1000;
                                    logger.info(
                                            "monitor() :: PPP connection in progress. Modem will be reset in {} sec if not connected",
                                            timeTillReset);
                                }
                            }
                        } else if (pppSt == PppState.CONNECTED) {
                            this.resetTimerStart = System.currentTimeMillis();
                        }

                        this.pppState = pppSt;
                        ConnectionInfo connInfo = new ConnectionInfoImpl(ifaceName);
                        InterfaceState interfaceState = new InterfaceState(ifaceName,
                                LinuxNetworkUtil.hasAddress(ifaceName), pppSt == PppState.CONNECTED,
                                connInfo.getIpAddress());
                        newInterfaceStatuses.put(ifaceName, interfaceState);
                    }

                    if (modem.isGpsSupported() && isGpsEnabledInConfig(modem.getConfiguration())) {
                        if (modem instanceof HspaCellularModem && !modem.isGpsEnabled()) {
                            modem.enableGps();
                        }
                        postModemGpsEvent(modem, true);
                    }

                } catch (Exception e) {
                    logger.error("monitor() :: Exception", e);
                    if (pppService != null && pppSt != null) {
                        try {
                            logger.info("monitor() :: Exception :: PPPD disconnect");
                            pppService.disconnect();
                        } catch (KuraException e1) {
                            logger.error("monitor() :: Exception while disconnect", e1);
                        }
                        this.pppState = pppSt;
                    }

                    try {
                        if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                            logger.error("monitor() :: Failed to disable modem GPS");
                        }
                    } catch (KuraException e1) {
                        logger.error("monitor() :: Exception disableModemGps", e1);
                    }

                    try {
                        logger.info("monitor() :: Exception :: modem reset");
                        modem.reset();
                        this.resetTimerStart = System.currentTimeMillis();
                    } catch (KuraException e1) {
                        logger.error("monitor() :: Exception modem.reset", e1);
                    }
                }
            }

            // post event for any status changes
            checkStatusChange(this.interfaceStatuses, newInterfaceStatuses);
            this.interfaceStatuses = newInterfaceStatuses;
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
        Class<? extends CellularModemFactory> modemFactoryClass = getModemFactoryClass(modemDevice);

        if (modemFactoryClass != null) {
            CellularModemFactory modemFactoryService = null;
            try {
                try {
                    Method getInstanceMethod = modemFactoryClass.getDeclaredMethod("getInstance", (Class<?>[]) null);
                    getInstanceMethod.setAccessible(true);
                    modemFactoryService = (CellularModemFactory) getInstanceMethod.invoke(null, (Object[]) null);
                } catch (Exception e) {
                    logger.error("Error calling getInstance() method on {}", modemFactoryClass.getName(), e);
                }

                // if unsuccessful in calling getInstance()
                if (modemFactoryService == null) {
                    modemFactoryService = modemFactoryClass.newInstance();
                }

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

                String ifaceName = this.networkService.getModemPppPort(modemDevice);
                List<NetConfig> netConfigs = null;
                if (ifaceName != null) {
                    NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.networkConfig
                            .getNetInterfaceConfig(ifaceName);

                    if (netInterfaceConfig == null) {
                        this.networkConfig = this.netConfigService.getNetworkConfiguration();
                        netInterfaceConfig = this.networkConfig.getNetInterfaceConfig(ifaceName);
                    }

                    if (netInterfaceConfig != null) {
                        netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
                        if (netConfigs != null && !netConfigs.isEmpty()) {
                            modem.setConfiguration(netConfigs);
                        }
                    }
                }

                if (modemDevice instanceof UsbModemDevice) {
                    this.modems.put(((UsbModemDevice) modemDevice).getUsbPort(), modem);
                } else if (modemDevice instanceof SerialModemDevice) {
                    this.modems.put(modemDevice.getProductName(), modem);
                }

                if (modem instanceof EvdoCellularModem) {
                    NetInterfaceStatus netIfaceStatus = getNetInterfaceStatus(netConfigs);
                    if (netIfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                        if (modem.isGpsEnabled() && !disableModemGps(modem)) {
                            logger.error("trackModem() :: Failed to disable modem GPS, resetting modem ...");
                            modem.reset();
                            this.resetTimerStart = System.currentTimeMillis();
                        }

                        if (!((EvdoCellularModem) modem).isProvisioned()) {
                            logger.info("trackModem() :: The {} is not provisioned, will try to provision it ...",
                                    modem.getModel());
                            if (task != null && !task.isCancelled()) {
                                logger.info("trackModem() :: Cancelling monitor task");
                                stopThread.set(true);
                                monitorNotify();
                                task.cancel(true);
                                task = null;
                            }
                            ((EvdoCellularModem) modem).provision();
                            if (task == null) {
                                logger.info("trackModem() :: Restarting monitor task");

                                submitMonitorTask();
                            } else {
                                monitorNotify();
                            }
                        } else {
                            logger.info("trackModem() :: The {} is provisioned", modem.getModel());
                        }
                    }

                    if (modem.isGpsSupported() && isGpsEnabledInConfig(netConfigs) && !modem.isGpsEnabled()) {
                        modem.enableGps();
                        postModemGpsEvent(modem, true);
                    }
                }
            } catch (Exception e) {
                logger.error("trackModem() :: {}", e.getMessage(), e);
            }
        }
    }

    protected Class<? extends CellularModemFactory> getModemFactoryClass(ModemDevice modemDevice) {
        Class<? extends CellularModemFactory> modemFactoryClass = null;

        if (modemDevice instanceof UsbModemDevice) {
            SupportedUsbModemInfo supportedUsbModemInfo = SupportedUsbModemsInfo.getModem((UsbModemDevice) modemDevice);
            UsbModemFactoryInfo usbModemFactoryInfo = SupportedUsbModemsFactoryInfo.getModem(supportedUsbModemInfo);
            modemFactoryClass = usbModemFactoryInfo.getModemFactoryClass();
        } else if (modemDevice instanceof SerialModemDevice) {
            SupportedSerialModemInfo supportedSerialModemInfo = SupportedSerialModemsInfo.getModem();
            SerialModemFactoryInfo serialModemFactoryInfo = SupportedSerialModemsFactoryInfo
                    .getModem(supportedSerialModemInfo);
            modemFactoryClass = serialModemFactoryInfo.getModemFactoryClass();
        }

        return modemFactoryClass;

    }

    private boolean disableModemGps(CellularModem modem) throws KuraException {

        postModemGpsEvent(modem, false);

        boolean portIsReachable = false;
        long startTimer = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(3000);
                if (modem.isPortReachable(modem.getAtPort())) {
                    logger.debug("disableModemGps() modem is now reachable ...");
                    portIsReachable = true;
                    break;
                } else {
                    logger.debug("disableModemGps() waiting for PositionService to release serial port ...");
                }
            } catch (Exception e) {
                logger.debug("disableModemGps() waiting for PositionService to release serial port ", e);
            }
        } while (System.currentTimeMillis() - startTimer < 20000L);

        modem.disableGps();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        boolean ret = false;
        if (portIsReachable && !modem.isGpsEnabled()) {
            logger.error("disableModemGps() :: Modem GPS is disabled :: portIsReachable={}, modem.isGpsEnabled()={}",
                    portIsReachable, modem.isGpsEnabled());
            ret = true;
        }
        return ret;
    }

    private void postModemGpsEvent(CellularModem modem, boolean enabled) throws KuraException {

        if (enabled) {
            CommURI commUri = modem.getSerialConnectionProperties(CellularModem.SerialPortType.GPSPORT);
            if (commUri != null) {
                logger.trace("postModemGpsEvent() :: Modem SeralConnectionProperties: {}", commUri.toString());

                HashMap<String, Object> modemInfoMap = new HashMap<>();
                modemInfoMap.put(ModemGpsEnabledEvent.Port, modem.getGpsPort());
                modemInfoMap.put(ModemGpsEnabledEvent.BaudRate, Integer.valueOf(commUri.getBaudRate()));
                modemInfoMap.put(ModemGpsEnabledEvent.DataBits, Integer.valueOf(commUri.getDataBits()));
                modemInfoMap.put(ModemGpsEnabledEvent.StopBits, Integer.valueOf(commUri.getStopBits()));
                modemInfoMap.put(ModemGpsEnabledEvent.Parity, Integer.valueOf(commUri.getParity()));

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

    private void monitorNotify() {
        if (stopThread != null) {
            synchronized (stopThread) {
                stopThread.notifyAll();
            }
        }
    }

    private void monitorWait() throws InterruptedException {
        if (stopThread != null) {
            synchronized (stopThread) {
                stopThread.wait(THREAD_INTERVAL);
            }
        }
    }
}
