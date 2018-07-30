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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.linux.net.route.RouteServiceImpl;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.IwLinkTool;
import org.eclipse.kura.linux.net.util.LinkTool;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.util.ScanTool;
import org.eclipse.kura.linux.net.util.iwconfigLinkTool;
import org.eclipse.kura.linux.net.wifi.WifiOptions;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.admin.NetworkAdminServiceImpl;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.route.RouteConfig;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiClientMonitorListener;
import org.eclipse.kura.net.wifi.WifiClientMonitorService;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiMonitorServiceImpl implements WifiClientMonitorService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(WifiMonitorServiceImpl.class);

    private static final String[] EVENT_TOPICS = new String[] {
            NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC };

    private static Object lock = new Object();
    private static final long THREAD_INTERVAL = 10000;
    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds
    private static Future<?> monitorTask;
    private static AtomicBoolean stopThread;
    private boolean first;
    private NetworkService networkService;
    private EventAdmin eventAdmin;
    private NetworkAdminService netAdminService;
    private NetworkConfigurationService netConfigService;
    private List<WifiClientMonitorListener> listeners;
    private Set<String> enabledInterfaces;
    private Set<String> disabledInterfaces;
    private Set<String> unmanagedInterfaces;
    private Map<String, InterfaceState> interfaceStatuses;
    private ExecutorService executor;
    private NetworkConfiguration currentNetworkConfiguration;
    private NetworkConfiguration newNetConfiguration;

    private WifiUtils wifiUtils;

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

    public void setNetworkAdminService(NetworkAdminService netAdminService) {
        this.netAdminService = netAdminService;
        if (this.netAdminService instanceof NetworkAdminServiceImpl) {
            ((NetworkAdminServiceImpl) this.netAdminService).setWifiClientMonitorServiceLock(lock);
        }
    }

    public void unsetNetworkAdminService(NetworkAdminService netAdminService) {
        if (this.netAdminService instanceof NetworkAdminServiceImpl) {
            ((NetworkAdminServiceImpl) this.netAdminService).unsetWifiClientMonitorServiceLock();
        }
        this.netAdminService = null;
    }

    public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = netConfigService;
    }

    public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = null;
    }

    public void setWifiUtilsService(WifiUtils wifiUtils) {
        this.wifiUtils = wifiUtils;
    }

    public void unsetWifiUtilsService(WifiUtils wifiUtils) {
        this.wifiUtils = null;
    }

    protected void activate(ComponentContext componentContext) {
        logger.debug("Activating WifiMonitor Service...");
        this.first = true;
        this.enabledInterfaces = new HashSet<>();
        this.disabledInterfaces = new HashSet<>();
        this.unmanagedInterfaces = new HashSet<>();
        this.interfaceStatuses = new HashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
        stopThread = new AtomicBoolean();
        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        this.listeners = new ArrayList<>();
        try {
            this.currentNetworkConfiguration = this.netConfigService.getNetworkConfiguration();
            initializeMonitoredInterfaces(this.currentNetworkConfiguration);
        } catch (KuraException e) {
            logger.error("Could not update list of interfaces", e);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        this.listeners = null;
        if (monitorTask != null && !monitorTask.isDone()) {
            stopThread.set(true);
            monitorNotify();
            logger.debug("Cancelling WifiMonitor task ...");
            monitorTask.cancel(true);
            logger.info("WifiMonitor task cancelled? = {}", monitorTask.isDone());
        }
        if (this.executor != null) {
            logger.debug("Terminating WifiMonitor Thread ...");
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
                Thread.currentThread().interrupt();
            }
            logger.info("WifiMonitor Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }
    }

    protected LinkTool getLinkTool(String interfaceName) throws KuraException {
        Collection<String> supportedWifiOptions = WifiOptions.getSupportedOptions(interfaceName);
        LinkTool linkTool = null;
        if (!supportedWifiOptions.isEmpty()) {
            if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_NL80211)) {
                linkTool = new IwLinkTool(interfaceName);
            } else if (supportedWifiOptions.contains(WifiOptions.WIFI_MANAGED_DRIVER_WEXT)) {
                linkTool = new iwconfigLinkTool(interfaceName);
            }
        }
        return linkTool;
    }

    protected NetInterfaceType getNetworkType(String interfaceName) throws KuraException {
        return LinuxNetworkUtil.getType(interfaceName);
    }

    protected RouteService getRouteService() {
        return RouteServiceImpl.getInstance();
    }

    protected IScanTool getScanTool(String interfaceName) throws KuraException {
        return ScanTool.get(interfaceName);
    }

    protected boolean isWifiDeviceOn(String interfaceName) {
        if (this.wifiUtils != null) {
            return this.wifiUtils.isWifiDeviceOn(interfaceName);
        }
        return false;
    }

    private void monitor() {
        synchronized (lock) {
            NetworkConfiguration newNetConfig = this.newNetConfiguration;
            try {
                // Track the interfaces being reconfigured
                List<String> interfacesToReconfigure = new ArrayList<>();

                logger.debug("monitor() :: wifi has started another run ...");
                // Find and disable interfaces affected by the configuration change
                if (newNetConfig != null && !newNetConfig.equals(this.currentNetworkConfiguration)) {
                    logger.info(
                            "monitor() :: Found a new network configuration, will check if wifi has been reconfigured ...");

                    // Note that the call to getReconfiguredWifiInterfaces() may also update
                    // m_enabledInterfaces or m_disabledInterfaces
                    interfacesToReconfigure.addAll(getReconfiguredWifiInterfaces());

                    this.currentNetworkConfiguration = newNetConfig;

                    // The interface being reconfigured is first disabled calling disableInterface().
                    // Note that calling disableInterface() does not update
                    // m_enabledInterfaces or m_disabledInterfaces.
                    // After calling disableInterface() and refreshing the list of
                    // interface statuses, a call to WifiState.isUp() should return false.
                    for (String interfaceName : interfacesToReconfigure) {
                        if (!this.unmanagedInterfaces.contains(interfaceName)) {
                            logger.debug("monitor() :: configuration has changed for {} , disabling...", interfaceName);
                            disableInterface(interfaceName);
                        }
                    }
                }

                // Check all interfaces configured to be enabled.
                // This includes the interfaces that might have been enabled by the above configuration change.
                // Get fresh interface statuses and post status change events.
                Map<String, InterfaceState> newStatuses = getInterfaceStatuses(this.enabledInterfaces);
                checkStatusChange(this.interfaceStatuses, newStatuses);
                this.interfaceStatuses = newStatuses;

                for (String interfaceName : this.enabledInterfaces) {
                    // Get current configuration
                    WifiInterfaceConfigImpl wifiInterfaceConfig = (WifiInterfaceConfigImpl) this.currentNetworkConfiguration
                            .getNetInterfaceConfig(interfaceName);
                    WifiConfig wifiConfig = getWifiConfig(wifiInterfaceConfig);

                    // Make sure we have enough information
                    if (wifiInterfaceConfig == null) {
                        logger.warn("monitor() :: missing WifiInterfaceConfigImpl for {}", interfaceName);
                        continue;
                    }
                    if (wifiConfig == null) {
                        logger.warn("monitor() :: missing WifiConfig for {}", interfaceName);
                        continue;
                    }

                    // There are interfaces for which we need to initially check if
                    // the right kernel module is loaded for the desired mode.
                    // If not we treat the interface as if needing to be reconfigured.
                    if (this.first
                            && wifiUtils != null && !wifiUtils.isKernelModuleLoadedForMode(interfaceName, wifiConfig.getMode())) {
                        logger.info("monitor() :: {} kernel module not suitable for WiFi mode {}", interfaceName,
                                wifiConfig.getMode());
                        this.first = false;
                        interfacesToReconfigure.add(interfaceName);
                        disableInterface(interfaceName);
                        // Update the current wifi state
                        this.interfaceStatuses.remove(interfaceName);
                        NetConfigIP4 netConfig = ((AbstractNetInterface<?>) wifiInterfaceConfig).getIP4config();
                        boolean isL2Only = netConfig.getStatus() == NetInterfaceStatus.netIPv4StatusL2Only ? true
                                : false;
                        this.interfaceStatuses.put(interfaceName,
                                new InterfaceState(NetInterfaceType.WIFI, interfaceName, isL2Only));
                    }

                    // Get current state
                    InterfaceState wifiState = this.interfaceStatuses.get(interfaceName);
                    if (wifiState == null) {
                        logger.warn("monitor() :: missing InterfaceState for {}", interfaceName);
                        continue;
                    }

                    // This flag is changed if the interface is disabled intentionally by the code below
                    boolean up = wifiState.isUp();
                    boolean linkUp = wifiState.isLinkUp();
                    logger.debug("monitor() :: interfaceName={}, wifiState.isLinkUp()={}", interfaceName,
                            wifiState.isLinkUp());
                    logger.debug("monitor() :: interfaceName={}, wifiState.isUp()={}", interfaceName, wifiState.isUp());
                    if (!up || !linkUp) {
                        // Either initially down or initially up and we disabled it explicitly
                        // * Check if the interface is being reconfigured and
                        // reload the kernel module (this may be ignored by the platform)
                        // * Infrastructure (Station) mode:
                        // * Configured to ignore SSID: just enable interface. Otherwise:
                        // * enable interface only if Access Point is available
                        //
                        // * Master (Access Point) mode:
                        // * just enable interface
                        // Some interfaces may require reloading the kernel module
                        // accordingly to the desired WifiMode.
                        // FIXME ideally we only need to this if the WifiMode changes.
                        // FIXME if reloading fails it won't be retried.
                        if (interfacesToReconfigure.contains(interfaceName)) {
                            try {
                                logger.info("monitor() :: reload {} kernel module for WiFi mode {}", interfaceName,
                                        wifiConfig.getMode());
                                reloadKernelModule(interfaceName, wifiConfig.getMode());
                            } catch (KuraException e) {
                                logger.warn("monitor() :: failed to reload {} kernel module.", interfaceName, e);
                                continue;
                            }
                        }
                        try {
                            if (WifiMode.MASTER.equals(wifiConfig.getMode())) {
                                logger.debug("monitor() :: enable {} in master mode", interfaceName);
                                enableInterface(wifiInterfaceConfig);
                            } else if (WifiMode.INFRA.equals(wifiConfig.getMode())) {
                                if (wifiConfig.ignoreSSID()) {
                                    logger.info("monitor() :: enable {} in infra mode", interfaceName);
                                    enableInterface(wifiInterfaceConfig);
                                } else {
                                    if (isAccessPointAvailable(interfaceName, wifiConfig.getSSID())) {
                                        logger.info("monitor() :: found access point - enable {} in infra mode",
                                                interfaceName);
                                        enableInterface(wifiInterfaceConfig);
                                    } else {
                                        logger.warn("monitor() :: {} - access point is not available",
                                                wifiConfig.getSSID());
                                    }
                                }
                            }
                        } catch (KuraException e) {
                            logger.error("monitor() :: Error enabling {} interface, will try to reset wifi",
                                    interfaceName, e);
                            resetWifiDevice(interfaceName);
                        }
                    } else {
                        // Infrastructure (Station) mode:
                        // * Notify RSSI to listeners
                        // * Detect interface link down: disable the interface
                        // * Cannot ping Access Point: renew DHCP lease
                        // * Check if enabled for LAN in DHCP mode: remove default gateway from route table
                        //
                        // Master (Access Point) mode:
                        // * Detect interface link down: enable interface.
                        // FIXME should we just disable it like in the Infrastructure case above?
                        if (WifiMode.INFRA.equals(wifiConfig.getMode())) {
                            // get signal strength only if somebody needs it
                            if (this.listeners != null && !this.listeners.isEmpty()) {
                                int rssi = 0;
                                try {
                                    logger.debug("monitor() :: Getting Signal Level for {} -> {}", interfaceName,
                                            wifiConfig.getSSID());
                                    rssi = getSignalLevel(interfaceName, wifiConfig.getSSID());
                                    logger.debug("monitor() :: Wifi RSSI is {}", rssi);
                                } catch (KuraException e) {
                                    logger.error("monitor() :: Failed to get Signal Level for {} -> {}", interfaceName,
                                            wifiConfig.getSSID());
                                    logger.error("monitor() :: Failed to get Signal Level ", e);
                                    rssi = 0;
                                }
                                for (WifiClientMonitorListener listener : this.listeners) {
                                    listener.setWifiSignalLevel(rssi);
                                }
                            }

                            if (!wifiState.isLinkUp()) {
                                logger.debug("monitor() :: link is down - disabling {}", interfaceName);
                                disableInterface(interfaceName);
                                up = false;
                            }

                            logger.debug("monitor() :: pingAccessPoint()? {}", wifiConfig.pingAccessPoint());
                            if (wifiConfig.pingAccessPoint()) {
                                NetConfigIP4 netConfigIP4 = ((AbstractNetInterface<?>) wifiInterfaceConfig)
                                        .getIP4config();
                                if (netConfigIP4 != null && netConfigIP4.isDhcp()) {
                                    boolean isApReachable = false;
                                    for (int i = 0; i < 3; i++) {
                                        isApReachable = isAccessPointReachable(interfaceName, 1000);
                                        if (isApReachable) {
                                            break;
                                        }
                                        sleep(1000);
                                    }
                                    if (!isApReachable) {
                                        this.netAdminService.renewDhcpLease(interfaceName);
                                    }
                                }
                            }

                            NetConfigIP4 netConfigIP4 = ((AbstractNetInterface<?>) wifiInterfaceConfig).getIP4config();
                            if (netConfigIP4.getStatus().equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)
                                    && netConfigIP4.isDhcp()) {
                                RouteService rs = RouteServiceImpl.getInstance();
                                RouteConfig rconf = rs.getDefaultRoute(interfaceName);
                                if (rconf != null) {
                                    logger.debug(
                                            "monitor() :: {} is configured for LAN/DHCP - removing GATEWAY route ...",
                                            rconf.getInterfaceName());
                                    rs.removeStaticRoute(rconf.getDestination(), rconf.getGateway(), rconf.getNetmask(),
                                            rconf.getInterfaceName());
                                }
                            }
                        } else if (WifiMode.MASTER.equals(wifiConfig.getMode()) && !wifiState.isLinkUp()) {
                            // disabling interface is probably needed to handle potential driver issues.
                            logger.warn(
                                    "monitor() :: !! Link is down for the {} in AP mode, while IP address is assigned. Will disable and reenable interface ...",
                                    interfaceName);
                            disableInterface(interfaceName);
                            enableInterface(wifiInterfaceConfig);
                        }
                    }
                }

                // Check all interfaces configured to be disabled
                for (String interfaceName : this.disabledInterfaces) {
                    InterfaceState wifiState = this.interfaceStatuses.get(interfaceName);
                    if (wifiState != null && wifiState.isUp()) {
                        logger.debug("monitor() :: {} is currently up - disable interface", interfaceName);
                        disableInterface(interfaceName);
                    }
                }

                // Shut down the monitor if no interface is configured to be enabled
                if (this.enabledInterfaces.isEmpty() && monitorTask != null) {
                    logger.info("monitor() :: No enabled wifi interfaces - shutting down monitor thread");
                    stopThread.set(true);
                    monitorTask.cancel(true);
                    monitorTask = null;
                }
            } catch (Exception e) {
                logger.warn("Error during WiFi Monitor handle event", e);
            }
        }
    }

    private void checkStatusChange(Map<String, InterfaceState> oldStatuses, Map<String, InterfaceState> newStatuses) {

        if (newStatuses != null) {
            // post NetworkStatusChangeEvent on current and new interfaces
            Iterator<String> it = newStatuses.keySet().iterator();
            while (it.hasNext()) {
                String interfaceName = it.next();
                if (oldStatuses != null && oldStatuses.containsKey(interfaceName)) {
                    if (!newStatuses.get(interfaceName).equals(oldStatuses.get(interfaceName))) {
                        logger.debug("Posting NetworkStatusChangeEvent on interface: {}", interfaceName);
                        this.eventAdmin.postEvent(
                                new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
                    }
                } else {
                    logger.debug("Posting NetworkStatusChangeEvent on enabled interface: {}", interfaceName);
                    this.eventAdmin.postEvent(
                            new NetworkStatusChangeEvent(interfaceName, newStatuses.get(interfaceName), null));
                }
            }

            // post NetworkStatusChangeEvent on interfaces that are no longer there
            if (oldStatuses != null) {
                it = oldStatuses.keySet().iterator();
                while (it.hasNext()) {
                    String interfaceName = it.next();
                    if (!newStatuses.containsKey(interfaceName)) {
                        logger.debug("Posting NetworkStatusChangeEvent on disabled interface: {}", interfaceName);
                        this.eventAdmin.postEvent(
                                new NetworkStatusChangeEvent(interfaceName, oldStatuses.get(interfaceName), null));
                    }
                }
            }
        }
    }

    @Override
    public void registerListener(WifiClientMonitorListener newListener) {
        boolean found = false;
        if (this.listeners == null) {
            this.listeners = new ArrayList<>();
        }
        if (!this.listeners.isEmpty()) {
            for (WifiClientMonitorListener listener : this.listeners) {
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
    public void unregisterListener(WifiClientMonitorListener listenerToUnregister) {
        if (this.listeners != null && !this.listeners.isEmpty()) {

            for (int i = 0; i < this.listeners.size(); i++) {
                if (this.listeners.get(i).equals(listenerToUnregister)) {
                    this.listeners.remove(i);
                }
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("handleEvent - topic: {}", event.getTopic());
        String topic = event.getTopic();

        if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {
            logger.debug("handleEvent - received network change event");
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
                    this.newNetConfiguration = new NetworkConfiguration(props);

                    // Initialize the monitor thread if needed
                    if (monitorTask == null) {
                        initializeMonitoredInterfaces(this.newNetConfiguration);
                    } else {
                        monitorNotify();
                    }
                } catch (Exception e) {
                    logger.warn("Error during WiFi Monitor handle event", e);
                }
            }
        }
    }

    private boolean isWifiEnabled(WifiInterfaceConfigImpl wifiInterfaceConfig) {
        if (wifiInterfaceConfig == null) {
            return false;
        }
        WifiMode wifiMode = getWifiInterfaceMode(wifiInterfaceConfig);
        NetInterfaceStatus status = ((AbstractNetInterface<?>) wifiInterfaceConfig).getInterfaceStatus();

        boolean statusEnabled = status == NetInterfaceStatus.netIPv4StatusL2Only
                || status == NetInterfaceStatus.netIPv4StatusEnabledLAN
                || status == NetInterfaceStatus.netIPv4StatusEnabledWAN ? true : false;
        boolean wifiEnabled = wifiMode == WifiMode.INFRA || wifiMode == WifiMode.MASTER ? true : false;

        logger.debug("isWifiEnabled() :: {} interface - status: {}", wifiInterfaceConfig.getName(), statusEnabled);
        logger.debug("isWifiEnabled() :: {} interface - WiFi Mode: {}", wifiInterfaceConfig.getName(), wifiEnabled);

        return statusEnabled && wifiEnabled;
    }

    private WifiMode getWifiInterfaceMode(WifiInterfaceConfigImpl wifiInterfaceConfig) {
        WifiMode wifiMode = WifiMode.UNKNOWN;
        WifiInterfaceAddressConfig wifiInterfaceAddressConfig;
        try {
            wifiInterfaceAddressConfig = (WifiInterfaceAddressConfig) ((AbstractNetInterface<?>) wifiInterfaceConfig)
                    .getNetInterfaceAddressConfig();
            wifiMode = wifiInterfaceAddressConfig.getMode();
        } catch (KuraException e) {
            logger.error("Failed to obtain WifiInterfaceMode", e);
        }
        return wifiMode;
    }

    private WifiConfig getWifiConfig(WifiInterfaceConfigImpl wifiInterfaceConfig) {
        if (wifiInterfaceConfig == null) {
            return null;
        }
        WifiConfig selectedWifiConfig = null;
        WifiInterfaceAddressConfig wifiInterfaceAddressConfig;
        try {
            wifiInterfaceAddressConfig = (WifiInterfaceAddressConfig) ((AbstractNetInterface<?>) wifiInterfaceConfig)
                    .getNetInterfaceAddressConfig();
            WifiMode wifiMode = wifiInterfaceAddressConfig.getMode();
            for (NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
                if (netConfig instanceof WifiConfig) {
                    WifiConfig wifiConfig = (WifiConfig) netConfig;
                    if (wifiMode.equals(wifiConfig.getMode())) {
                        selectedWifiConfig = wifiConfig;
                        break;
                    }
                }
            }
        } catch (KuraException e) {
            logger.error("Failed to obtain WifiConfig", e);
        }

        return selectedWifiConfig;
    }

    private void disableInterface(String interfaceName) throws KuraException {
        logger.debug("Disabling {}", interfaceName);
        this.netAdminService.disableInterface(interfaceName);
        this.netAdminService.manageDhcpServer(interfaceName, false);
    }

    private void enableInterface(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {
        logger.debug("enableInterface: {}", netInterfaceConfig);
        WifiInterfaceConfigImpl wifiInterfaceConfig;

        if (!(netInterfaceConfig instanceof WifiInterfaceConfigImpl)) {
            return;
        }
        wifiInterfaceConfig = (WifiInterfaceConfigImpl) netInterfaceConfig;
        String interfaceName = wifiInterfaceConfig.getName();
        WifiMode wifiMode;
        NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusUnknown;
        boolean isDhcpClient = false;
        boolean enableDhcpServer = false;

        WifiInterfaceAddressConfig wifiInterfaceAddressConfig = (WifiInterfaceAddressConfig) ((AbstractNetInterface<?>) wifiInterfaceConfig)
                .getNetInterfaceAddressConfig();

        wifiMode = wifiInterfaceAddressConfig.getMode();
        for (NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
            if (netConfig instanceof NetConfigIP4) {
                status = ((NetConfigIP4) netConfig).getStatus();
                isDhcpClient = ((NetConfigIP4) netConfig).isDhcp();
            } else if (netConfig instanceof DhcpServerConfig4) {
                enableDhcpServer = ((DhcpServerConfig4) netConfig).isEnabled();
            }
        }
        if (status == NetInterfaceStatus.netIPv4StatusL2Only) {
            isDhcpClient = false;
        }
        boolean enStatus = status == NetInterfaceStatus.netIPv4StatusL2Only
                || status == NetInterfaceStatus.netIPv4StatusEnabledLAN
                || status == NetInterfaceStatus.netIPv4StatusEnabledWAN ? true : false;
        boolean enWifiMode = wifiMode == WifiMode.INFRA || wifiMode == WifiMode.MASTER ? true : false;
        if (enStatus && enWifiMode) {
            this.netAdminService.enableInterface(interfaceName, isDhcpClient);
            if (enableDhcpServer) {
                this.netAdminService.manageDhcpServer(interfaceName, true);
            }
        }
    }

    private void reloadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException {
        logger.info("monitor() :: reload {} using kernel module for WiFi mode {}", interfaceName, wifiMode);
        if (wifiUtils == null) {
            return;
        }
        if (wifiUtils.isKernelModuleLoaded(interfaceName)) {
            wifiUtils.unloadKernelModule(interfaceName);
        }
        wifiUtils.loadKernelModule(interfaceName, wifiMode);
    }

    private void initializeMonitoredInterfaces(NetworkConfiguration networkConfiguration) throws KuraException {
        synchronized (lock) {
            logger.info("initializing monitor");
            this.enabledInterfaces.clear();
            this.disabledInterfaces.clear();
            this.unmanagedInterfaces.clear();

            if (networkConfiguration != null) {
                for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : networkConfiguration
                        .getNetInterfaceConfigs()) {
                    String interfaceName = netInterfaceConfig.getName();
                    if (netInterfaceConfig.getType() != NetInterfaceType.WIFI || interfaceName.startsWith("mon")) {
                        // ignore non-wifi or "mon" interfaces
                        continue;
                    }
                    if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                        if (isWifiEnabled((WifiInterfaceConfigImpl) netInterfaceConfig)) {
                            logger.debug("Adding {} to enabledInterfaces", interfaceName);
                            this.enabledInterfaces.add(interfaceName);
                        } else if (!((AbstractNetInterface<?>) netInterfaceConfig).isInterfaceManaged()) {
                            logger.debug("Adding {} to unmanagedInterfaces", interfaceName);
                            this.unmanagedInterfaces.add(interfaceName);
                        } else {
                            logger.debug("Adding {} to disabledInterfaces", interfaceName);
                            this.disabledInterfaces.add(interfaceName);
                        }
                    }
                }
            } else {
                logger.info("networkConfiguration is null");
            }

            if (!this.enabledInterfaces.isEmpty()) {
                this.interfaceStatuses = getInterfaceStatuses(this.enabledInterfaces);

                if (monitorTask == null) {
                    logger.info("Starting WifiMonitor thread...");
                    stopThread.set(false);
                    monitorTask = this.executor.submit(() -> {
                        while (!stopThread.get()) {
                            Thread.currentThread().setName("WifiMonitor Thread");
                            try {
                                monitor();
                                monitorWait();
                            } catch (InterruptedException interruptedException) {
                                logger.debug("WiFi monitor interrupted - {}", interruptedException);
                                Thread.currentThread().interrupt();
                            } catch (Throwable t) {
                                logger.error("Exception while monitoring WiFi connection - {}", t);
                            }
                        }
                    });
                } else {
                    monitorNotify();
                }
            }
        }
    }

    private Collection<String> getReconfiguredWifiInterfaces() throws KuraException {
        Set<String> reconfiguredInterfaces = new HashSet<>();
        for (String interfaceName : this.networkService.getAllNetworkInterfaceNames()) {
            // skip non-wifi interfaces
            if (getNetworkType(interfaceName) != NetInterfaceType.WIFI || interfaceName.startsWith("mon")) {
                // ignore non-wifi or "mon" interfaces
                continue;
            }

            // Get the old wifi config
            WifiInterfaceConfigImpl currentConfig = null;
            if (this.currentNetworkConfiguration != null) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig = this.currentNetworkConfiguration
                        .getNetInterfaceConfig(interfaceName);
                if (netInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                    currentConfig = (WifiInterfaceConfigImpl) netInterfaceConfig;
                }
            }

            // Get the new wifi config
            WifiInterfaceConfigImpl newConfig = null;
            if (this.newNetConfiguration != null) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> newNetInterfaceConfig = this.newNetConfiguration
                        .getNetInterfaceConfig(interfaceName);
                if (newNetInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                    newConfig = (WifiInterfaceConfigImpl) newNetInterfaceConfig;
                }
            }

            if (newConfig != null && currentConfig != null) {
                List<WifiInterfaceAddressConfig> currentInterfaceAddressConfigs = currentConfig
                        .getNetInterfaceAddresses();
                List<WifiInterfaceAddressConfig> newInterfaceAddressConfigs = newConfig.getNetInterfaceAddresses();

                if (currentInterfaceAddressConfigs == null && newInterfaceAddressConfigs == null) {
                    // no config changed - continue
                    continue;
                }

                if (currentInterfaceAddressConfigs == null || newInterfaceAddressConfigs == null) {
                    reconfiguredInterfaces.add(interfaceName);
                    continue;
                }
                // TODO: compare interfaceAddressConfigs
                // FIXME - assuming one InterfaceAddressConfig for now
                WifiInterfaceAddressConfig currentInterfaceAddressConfig = currentInterfaceAddressConfigs.get(0);
                WifiInterfaceAddressConfig newInterfaceAddressConfig = newInterfaceAddressConfigs.get(0);

                if (currentInterfaceAddressConfig.getConfigs() == null
                        && newInterfaceAddressConfig.getConfigs() == null) {
                    continue;
                }

                if (currentInterfaceAddressConfig.getConfigs() == null
                        || newInterfaceAddressConfig.getConfigs() == null) {
                    reconfiguredInterfaces.add(interfaceName);
                    continue;
                }

                // Remove other WifiConfigs that don't match the selected mode, for comparison purposes
                //
                List<NetConfig> currentNetConfigs = new ArrayList<>(currentInterfaceAddressConfig.getConfigs());
                List<NetConfig> newNetConfigs = new ArrayList<>(newInterfaceAddressConfig.getConfigs());

                WifiMode newWifiMode = newInterfaceAddressConfig.getMode();
                WifiMode currentWifiMode = currentInterfaceAddressConfig.getMode();

                if (newWifiMode != currentWifiMode) {
                    reconfiguredInterfaces.add(interfaceName);
                    continue;
                }
                // Modes don't match. We need to compare configs deeply
                internalWifiConfigCompare(reconfiguredInterfaces, interfaceName, currentNetConfigs, newNetConfigs);
            } else if (newConfig != null) {
                // only newConfig - oldConfig is null
                logger.debug("oldConfig was null, adding newConfig");
                reconfiguredInterfaces.add(interfaceName);
            } else if (currentConfig != null) {
                logger.debug("Configuration for {} has changed", interfaceName);
                reconfiguredInterfaces.add(interfaceName);
                logger.debug("Removing {} from list of enabled interfaces because it is not configured", interfaceName);
                this.disabledInterfaces.add(interfaceName);
            } else {
                logger.debug("old and new wifi config are null...");
            }
        }
        updateInterfacesLists(reconfiguredInterfaces);
        return reconfiguredInterfaces;
    }

    private void updateInterfacesLists(Set<String> reconfiguredInterfaces) {
        Set<String> enabledIfaces = new HashSet<>();
        Set<String> disabledIfaces = new HashSet<>();
        Set<String> unmanagedIfaces = new HashSet<>();

        for (String interfaceName : reconfiguredInterfaces) {
            logger.info("WifiMonitor: configuration for {} has changed", interfaceName);
            WifiInterfaceConfigImpl newConfig = null;
            if (this.newNetConfiguration != null) {
                NetInterfaceConfig<? extends NetInterfaceAddressConfig> newNetInterfaceConfig = this.newNetConfiguration
                        .getNetInterfaceConfig(interfaceName);
                if (newNetInterfaceConfig instanceof WifiInterfaceConfigImpl) {
                    newConfig = (WifiInterfaceConfigImpl) newNetInterfaceConfig;
                }
            }

            // do we need to monitor?
            if (isWifiEnabled(newConfig) && !enabledIfaces.contains(interfaceName)) {
                logger.debug("Adding {} to list of enabled interfaces", interfaceName);
                enabledIfaces.add(interfaceName);
            } else if (newConfig != null && !((AbstractNetInterface<?>) newConfig).isInterfaceManaged()
                    && !unmanagedIfaces.contains(interfaceName)) {
                logger.debug("Removing {} from list of enabled interfaces because it is set not to be managed by Kura",
                        interfaceName);
                unmanagedIfaces.add(interfaceName);
            } else if (!disabledIfaces.contains(interfaceName)) {
                logger.debug("Removing {} from list of enabled interfaces because it is disabled", interfaceName);
                disabledIfaces.add(interfaceName);
            }
        }
        if (!reconfiguredInterfaces.isEmpty()) {
            this.enabledInterfaces = enabledIfaces;
            this.disabledInterfaces = disabledIfaces;
            this.unmanagedInterfaces = unmanagedIfaces;
        }
    }

    private void internalWifiConfigCompare(Set<String> reconfiguredInterfaces, String interfaceName,
            List<NetConfig> currentNetConfigs, List<NetConfig> newNetConfigs) {
        for (int i = 0; i < currentNetConfigs.size(); i++) {
            NetConfig currentNetConfig = currentNetConfigs.get(i);
            if (currentNetConfig instanceof FirewallAutoNatConfig) {
                continue; // we don't compare FirewallAutoNatConfig instances
            }

            for (int j = 0; j < newNetConfigs.size(); j++) {
                NetConfig newNetConfig = newNetConfigs.get(j);
                if (newNetConfig instanceof FirewallAutoNatConfig) {
                    continue; // we don't compare FirewallAutoNatConfig instances
                }

                if (newNetConfig.getClass() == currentNetConfig.getClass() && !newNetConfig.equals(currentNetConfig)
                        && !(currentNetConfig instanceof WifiConfig && ((WifiConfig) currentNetConfig)
                                .getMode() != ((WifiConfig) newNetConfig).getMode())) { // ((WifiConfig)
                    // currentNetConfig).getMode()
                    // != newWifiMode

                    // we are not entering here if we are comparing WifiConfig instances and the mode differs. Two
                    // instances
                    // of WifiConfig exist: one with mode= MASTER and one with mode= INFRA.
                    // we try to compare only objects with the same mode, in order to have a correct comparison.
                    logger.debug("\tConfig changed - Old config: {}", currentNetConfig);
                    logger.debug("\tConfig changed - New config: {}", newNetConfig);
                    reconfiguredInterfaces.add(interfaceName);
                    return;
                }
            }

        }
    }

    private boolean isAccessPointAvailable(String interfaceName, String ssid) throws KuraException {
        if (ssid == null) {
            return false;
        }
        boolean available = false;
        IScanTool scanTool = getScanTool(interfaceName);
        if (scanTool != null) {
            List<WifiAccessPoint> wifiAccessPoints = scanTool.scan();
            for (WifiAccessPoint wap : wifiAccessPoints) {
                if (ssid.equals(wap.getSSID())) {
                    logger.trace("isAccessPointAvailable() :: SSID={} is available :: strength={}", ssid,
                            wap.getStrength());
                    available = Math.abs(wap.getStrength()) > 0;
                    break;
                }
            }
        }
        return available;
    }

    @Override
    public int getSignalLevel(String interfaceName, String ssid) throws KuraException {
        int rssi;
        if (wifiUtils != null && !wifiUtils.isKernelModuleLoadedForMode(interfaceName, WifiMode.INFRA)) {
            logger.info("getSignalLevel() :: reload {} kernel module for WiFi mode {}", interfaceName, WifiMode.INFRA);
            reloadKernelModule(interfaceName, WifiMode.INFRA);
        }
        if ((rssi = getSignalLevelWithLinkTool(interfaceName, ssid)) != 0) {
            return rssi;
        }
        return getSignalLevelWithScanTool(interfaceName, ssid);
    }

    private int getSignalLevelWithLinkTool(String interfaceName, String ssid) throws KuraException {
        InterfaceState wifiState = this.interfaceStatuses.get(interfaceName);
        if (wifiState == null || ssid == null) {
            return 0;
        }
        int rssi = 0;
        if (wifiState.isUp()) {
            logger.trace("getSignalLevelWithLinkTool() :: using 'iw dev wlan0 link' command ...");
            LinkTool linkTool = getLinkTool(interfaceName);

            if (linkTool != null && linkTool.get() && linkTool.isLinkDetected()) {
                rssi = linkTool.getSignal();
                logger.debug("getSignalLevelWithLinkTool() :: rssi={} (using 'iw dev wlan0 link')", rssi);
            }
        }
        return rssi;
    }

    private int getSignalLevelWithScanTool(String interfaceName, String ssid) throws KuraException {
        logger.trace("getSignalLevelWithScanTool() :: using 'iw dev wlan0 scan' command ...");
        IScanTool scanTool = getScanTool(interfaceName);
        if (scanTool == null) {
            return 0;
        }
        int rssi = 0;
        List<WifiAccessPoint> wifiAccessPoints = scanTool.scan();
        for (WifiAccessPoint wap : wifiAccessPoints) {
            if (ssid.equals(wap.getSSID())) {
                if (wap.getStrength() > 0) {
                    rssi = 0 - wap.getStrength();
                    logger.debug("getSignalLevelWithScanTool() :: rssi={} (using 'iw dev wlan0 scan')", rssi);
                }
                break;
            }
        }
        return rssi;
    }

    private boolean isAccessPointReachable(String interfaceName, int tout) throws KuraException {
        boolean ret = true;
        RouteService rs = getRouteService();
        RouteConfig rconf = rs.getDefaultRoute(interfaceName);
        if (rconf != null) {
            IPAddress ipAddress = rconf.getGateway();
            String iface = rconf.getInterfaceName();
            if (ipAddress != null && iface != null && iface.equals(interfaceName)) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ipAddress.getHostAddress());
                    ret = inetAddress.isReachable(tout);
                    logger.info("Access point reachable? {}", ret);
                } catch (Exception e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
            }
        }
        return ret;
    }

    private Map<String, InterfaceState> getInterfaceStatuses(Collection<String> interfaceList) throws KuraException {
        Map<String, InterfaceState> statuses = new HashMap<>();

        for (String interfaceName : interfaceList) {
            WifiInterfaceConfigImpl wifiInterfaceConfig = (WifiInterfaceConfigImpl) this.currentNetworkConfiguration
                    .getNetInterfaceConfig(interfaceName);
            if (wifiInterfaceConfig == null) {
                continue;
            }
            WifiConfig wifiConfig = getWifiConfig(wifiInterfaceConfig);
            boolean isL2Only = ((AbstractNetInterface<?>) wifiInterfaceConfig).getIP4config()
                    .getStatus() == NetInterfaceStatus.netIPv4StatusL2Only ? true : false;
            if (wifiConfig != null) {
                statuses.put(interfaceName, new WifiInterfaceState(interfaceName, wifiConfig.getMode(), isL2Only));
            }
        }
        return statuses;
    }

    private boolean resetWifiDevice(String interfaceName) throws KuraException {
        boolean ret = false;
        if (isWifiDeviceOn(interfaceName) && wifiUtils != null) {
            wifiUtils.turnWifiDeviceOff(interfaceName);
        }
        if (isWifiDeviceReady(interfaceName, false, 10) && wifiUtils != null) {
            wifiUtils.turnWifiDeviceOn(interfaceName);
            ret = isWifiDeviceReady(interfaceName, true, 20);
        }
        return ret;
    }

    private boolean isWifiDeviceReady(String interfaceName, boolean expected, int tout) {
        boolean deviceReady = false;
        long tmrStart = System.currentTimeMillis();
        do {
            sleep(1000);
            boolean deviceOn = isWifiDeviceOn(interfaceName);
            logger.trace("isWifiDeviceReady()? :: deviceOn={}, expected={}", deviceOn, expected);
            if (deviceOn == expected) {
                deviceReady = true;
                break;
            }
        } while (System.currentTimeMillis() - tmrStart < tout * 1000);

        logger.debug("isWifiDeviceReady()? :: deviceReady={}", deviceReady);
        return deviceReady;
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

    private void sleep(long timeToSleep) {
        try {
            Thread.sleep(timeToSleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
