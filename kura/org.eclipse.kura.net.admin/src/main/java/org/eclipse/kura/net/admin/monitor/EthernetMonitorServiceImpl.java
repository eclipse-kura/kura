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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.route.RouteService;
import org.eclipse.kura.linux.net.route.RouteServiceImpl;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.EthernetMonitorService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.route.RouteConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthernetMonitorServiceImpl implements EthernetMonitorService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(EthernetMonitorServiceImpl.class);

    private static final String[] EVENT_TOPICS = new String[] {
            NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC, };
    
    private static final long THREAD_INTERVAL = 30000;
    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds

    private static Object lock = new Object();

    private static Map<String, Future<?>> tasks;
    private static Map<String, AtomicBoolean> stopThreads;

    private EventAdmin eventAdmin;
    private NetworkAdminService netAdminService;
    private NetworkConfigurationService netConfigService;
    private RouteService routeService;

    private final Map<String, InterfaceState> interfaceState = new HashMap<>();
    private final Map<String, NetInterfaceConfig<NetInterfaceAddressConfig>> networkConfiguration = new HashMap<>();
    private final Map<String, NetInterfaceConfig<NetInterfaceAddressConfig>> newNetworkConfiguration = new HashMap<>();
    private ExecutorService executor;

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

    public void setNetworkAdminService(NetworkAdminService netAdminService) {
        this.netAdminService = netAdminService;
    }

    public void unsetNetworkAdminService(NetworkAdminService netAdminService) {
        this.netAdminService = null;
    }

    public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = netConfigService;
    }

    public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {

        logger.debug("Activating EthernetMonitor Service...");

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);

        this.routeService = RouteServiceImpl.getInstance();

        this.executor = Executors.newCachedThreadPool();

        // Get initial configurations
        try {
            NetworkConfiguration netConfiguration = this.netConfigService.getNetworkConfiguration();
            if (netConfiguration != null) {
                for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netConfiguration
                        .getNetInterfaceConfigs()) {
                    if (netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
                        logger.debug("Adding initial ethernet config for {}", netInterfaceConfig.getName());
                        EthernetInterfaceConfigImpl newEthernetConfig = (EthernetInterfaceConfigImpl) netInterfaceConfig;
                        this.networkConfiguration.put(netInterfaceConfig.getName(), newEthernetConfig);
                        this.newNetworkConfiguration.put(netInterfaceConfig.getName(), newEthernetConfig);
                    }
                }
            }
        } catch (KuraException e) {
            logger.error("Could not update list of interfaces", e);
        }

        // Initialize monitors
        initializeMonitors();

        logger.debug("Done Activating EthernetMonitor Service...");
    }

    protected void deactivate(ComponentContext componentContext) {
        for (String key : tasks.keySet()) {
            synchronized (lock) {
                stopMonitor(key);
            }
        }

        if (this.executor != null) {
            logger.debug("Terminating EthernetMonitor Thread ...");
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
                Thread.currentThread().interrupt();
            }
            logger.info("EthernetMonitor Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }
    }

    protected InterfaceState getEthernetInterfaceState(String interfaceName, boolean isL2Only) throws KuraException {
        return new InterfaceState(NetInterfaceType.ETHERNET, interfaceName, isL2Only);
    }

    protected void startInterfaceIfDown(String interfaceName) throws KuraException {
        // Make sure the Ethernet Controllers are powered
        // FIXME:MC it should be possible to refactor this under the InterfaceState to avoid dual checks
        if (!LinuxNetworkUtil.isUp(interfaceName)) {
            LinuxNetworkUtil.bringUpDeletingAddress(interfaceName);
        }
    }

    private void monitor(String interfaceName) {
        synchronized (lock) {
            try {
                List<? extends NetInterfaceAddressConfig> newNiacs;
                List<? extends NetInterfaceAddressConfig> curNiacs = null;
                InterfaceState currentInterfaceState;
                boolean interfaceEnabled;
                boolean isDhcpClient = false;
                IP4Address staticGateway = null;
                boolean dhcpServerEnabled = false;
                boolean postStatusChangeEvent = false;

                NetInterfaceConfig<NetInterfaceAddressConfig> currentInterfaceConfig = this.networkConfiguration
                        .get(interfaceName);
                NetInterfaceConfig<NetInterfaceAddressConfig> newInterfaceConfig = this.newNetworkConfiguration
                        .get(interfaceName);

                startInterfaceIfDown(interfaceName);

                // If a new configuration exists, compare it to the existing configuration
                if (newInterfaceConfig != null) {
                    // Get all configurations for the interface
                    newNiacs = newInterfaceConfig.getNetInterfaceAddresses();
                    if (currentInterfaceConfig != null) {
                        curNiacs = currentInterfaceConfig.getNetInterfaceAddresses();
                    }

                    if (isConfigChanged(newNiacs, curNiacs)) {
                        logger.info("Found a new Ethernet network configuration for {}", interfaceName);

                        if (!((AbstractNetInterface<?>) newInterfaceConfig).isInterfaceManaged()) {
                            logger.info(
                                    "The {} interface is configured not to be managed by Kura and will not be monitored.",
                                    interfaceName);
                            return;
                        }

                        // Disable the interface to be reconfigured below
                        disableInterface(interfaceName);

                        // Set the current config to the new config
                        this.networkConfiguration.put(interfaceName, newInterfaceConfig);
                        currentInterfaceConfig = newInterfaceConfig;

                        // Post a status change event - not to be confusd with the Config Change that I am consuming
                        postStatusChangeEvent = true;
                    }

                    this.newNetworkConfiguration.remove(interfaceName);
                }

                // Monitor for status changes and ensure dhcp server is running when enabled
                interfaceEnabled = ((AbstractNetInterface<?>) currentInterfaceConfig).isInterfaceEnabled();
                InterfaceState prevInterfaceState = this.interfaceState.get(interfaceName);

                // FIXME:MC Deprecate this constructor and prefer the one with the explicit parameters
                // (String interfaceName, boolean up, boolean link, IPAddress ipAddress)
                // It will save a call to determine the iface type and it will keep InterfaceState
                // as a state object as it should be. Maybe introduce an InterfaceStateBuilder.
                boolean isL2Only = ((AbstractNetInterface<?>) currentInterfaceConfig).getIP4config()
                        .getStatus() == NetInterfaceStatus.netIPv4StatusL2Only ? true : false;
                currentInterfaceState = getEthernetInterfaceState(interfaceName, isL2Only);
                if (!currentInterfaceState.equals(prevInterfaceState)) {
                    postStatusChangeEvent = true;
                }

                // Find if DHCP server or DHCP client mode is enabled
                if (currentInterfaceConfig != null) {
                    NetInterfaceStatus netInterfaceStatus = ((AbstractNetInterface<?>) currentInterfaceConfig)
                            .getInterfaceStatus();
                    curNiacs = currentInterfaceConfig.getNetInterfaceAddresses();

                    if (curNiacs != null && !curNiacs.isEmpty()) {
                        for (NetInterfaceAddressConfig niac : curNiacs) {
                            List<NetConfig> netConfigs = niac.getConfigs();
                            if (netConfigs != null) {
                                for (NetConfig netConfig : netConfigs) {
                                    if (netConfig instanceof DhcpServerConfig4) {
                                        // only enable if Enabled for LAN
                                        if (netInterfaceStatus.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)) {
                                            dhcpServerEnabled = ((DhcpServerConfig4) netConfig).isEnabled();
                                        } else {
                                            logger.trace("Not enabling DHCP server for {} since it is set to {}",
                                                    interfaceName, netInterfaceStatus);
                                        }
                                    } else if (netConfig instanceof NetConfigIP4) {
                                        if (!NetInterfaceStatus.netIPv4StatusL2Only
                                                .equals(((NetConfigIP4) netConfig).getStatus())) {
                                            isDhcpClient = ((NetConfigIP4) netConfig).isDhcp();
                                        }
                                        staticGateway = ((NetConfigIP4) netConfig).getGateway();
                                    }
                                }
                            }
                        }
                    } else {
                        logger.debug("No current net interface addresses for {}", interfaceName);
                    }
                }

                // Enable/disable based on configuration and current status
                boolean interfaceStateChanged = false;
                if (interfaceEnabled) {
                    if (currentInterfaceState.isUp()) {
                        if (!currentInterfaceState.isLinkUp()) {
                            logger.debug("link is down - disabling {}", interfaceName);
                            disableInterface(interfaceName);
                            interfaceStateChanged = true;
                        }
                    } else {
                        // State is currently down
                        if (currentInterfaceState.isLinkUp()) {
                            logger.debug("link is up - enabling {}", interfaceName);
                            this.netAdminService.enableInterface(interfaceName, isDhcpClient);
                            interfaceStateChanged = true;
                        }
                    }
                } else {
                    if (currentInterfaceState.isUp()) {
                        logger.debug("{} is currently up - disable interface", interfaceName);
                        disableInterface(interfaceName);
                        interfaceStateChanged = true;
                    }
                }

                // Get the status after all ifdowns and ifups
                // FIXME: reload the configuration IFF one of above enable/disable happened
                if (interfaceStateChanged) {
                    isL2Only = ((AbstractNetInterface<?>) currentInterfaceConfig).getIP4config()
                            .getStatus() == NetInterfaceStatus.netIPv4StatusL2Only ? true : false;
                    currentInterfaceState = getEthernetInterfaceState(interfaceName, isL2Only);
                }

                // Manage the DHCP server and validate routes
                if (currentInterfaceState != null && currentInterfaceState.isUp() && currentInterfaceState.isLinkUp()) {
                    NetInterfaceStatus netInterfaceStatus = ((AbstractNetInterface<?>) currentInterfaceConfig)
                            .getInterfaceStatus();
                    if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledWAN) {
                        // This should be the default gateway - make sure it is
                        boolean found = false;

                        RouteConfig[] routes = this.routeService.getRoutes();
                        if (routes != null && routes.length > 0) {
                            for (RouteConfig route : routes) {
                                if (route.getInterfaceName().equals(interfaceName)
                                        && route.getDestination().equals(IPAddress.parseHostAddress("0.0.0.0"))
                                        && !route.getGateway().equals(IPAddress.parseHostAddress("0.0.0.0"))) {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found && (isDhcpClient || staticGateway != null)) {
                            // disable the interface and reenable - something didn't happen at initialization as it
                            // was supposed to
                            logger.error(
                                    "WAN interface {} did not have a route setting it as the default gateway, restarting it",
                                    interfaceName);
                            this.netAdminService.disableInterface(interfaceName);
                            this.netAdminService.enableInterface(interfaceName, isDhcpClient);
                        }
                    } else if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusEnabledLAN) {
                        if (isDhcpClient) {
                            RouteConfig rconf = this.routeService.getDefaultRoute(interfaceName);
                            if (rconf != null) {
                                logger.debug("{} is configured for LAN/DHCP - removing GATEWAY route ...",
                                        rconf.getInterfaceName());
                                this.routeService.removeStaticRoute(rconf.getDestination(), rconf.getGateway(),
                                        rconf.getNetmask(), rconf.getInterfaceName());
                            }
                        }
                    }

                    if (dhcpServerEnabled && !DhcpServerManager.isRunning(interfaceName)) {
                        logger.debug("Starting DHCP server for {}", interfaceName);
                        this.netAdminService.manageDhcpServer(interfaceName, true);
                    }
                } else if (DhcpServerManager.isRunning(interfaceName)) {
                    logger.debug("Stopping DHCP server for {}", interfaceName);
                    this.netAdminService.manageDhcpServer(interfaceName, false);
                }

                // post event if there were any changes
                if (postStatusChangeEvent) {
                    logger.debug("Posting NetworkStatusChangeEvent for {}: {}", interfaceName, currentInterfaceState);
                    this.eventAdmin.postEvent(new NetworkStatusChangeEvent(interfaceName, currentInterfaceState, null));
                    this.interfaceState.put(interfaceName, currentInterfaceState);
                }

                // If the interface is disabled in Denali, stop the monitor
                if (!interfaceEnabled) {
                    logger.debug("{} is disabled - stopping monitor", interfaceName);
                    stopMonitor(interfaceName);
                }
            } catch (Exception e) {
                logger.warn("Error during Ethernet Monitor", e);
            }
        }
    }

    // On a network config change event, verify the change was for ethernet and add a new ethernet config
    @Override
    public void handleEvent(Event event) {
        String topic = event.getTopic();
        logger.debug("handleEvent - topic: {}", topic);

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
                    NetworkConfiguration newNetworkConfig = new NetworkConfiguration(props);
                    for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : newNetworkConfig
                            .getNetInterfaceConfigs()) {
                        if (netInterfaceConfig instanceof EthernetInterfaceConfigImpl) {
                            logger.debug("handleEvent() :: Adding new ethernet config for {}",
                                    netInterfaceConfig.getName());
                            EthernetInterfaceConfigImpl newEthernetConfig = (EthernetInterfaceConfigImpl) netInterfaceConfig;
                            this.newNetworkConfiguration.put(netInterfaceConfig.getName(), newEthernetConfig);
                            if (((AbstractNetInterface<?>) newEthernetConfig).isInterfaceManaged()) {
                                logger.debug("handleEvent() :: Starting monitor for {} interface",
                                        netInterfaceConfig.getName());
                                startMonitor(netInterfaceConfig.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error during Ethernet Monitor handle event", e);
                }
            }
        }

    }

    // Compare configurations
    private boolean isConfigChanged(List<? extends NetInterfaceAddressConfig> newConfig,
            List<? extends NetInterfaceAddressConfig> currentConfig) {
        if (newConfig == null && currentConfig == null) {
            return false;
        }

        if (newConfig == null || currentConfig == null || newConfig.size() != currentConfig.size()) {
            return true;
        }

        for (NetInterfaceAddressConfig newNetInterfaceAddressConfig : newConfig) {
            for (NetInterfaceAddressConfig currentNetInterfaceAddressConfig : currentConfig) {
                List<NetConfig> newNetConfigs = newNetInterfaceAddressConfig.getConfigs();
                List<NetConfig> currentNetConfigs = currentNetInterfaceAddressConfig.getConfigs();

                if (newNetConfigs == null && currentNetConfigs == null) {
                    continue;
                }

                if (newNetConfigs == null || currentNetConfigs == null
                        || newNetConfigs.size() != currentNetConfigs.size()) {
                    logger.debug("Config changed current - {}", currentNetConfigs);
                    logger.debug("Config changed new     - {}", newNetConfigs);
                    return true;
                }
                return isNetConfigsChanged(newNetConfigs, currentNetConfigs);
            }
        }

        return false;
    }

    private boolean isNetConfigsChanged(List<NetConfig> newNetConfigs, List<NetConfig> currentNetConfigs) {
        for (NetConfig newNetConfig : newNetConfigs) {
            if (newNetConfig instanceof FirewallAutoNatConfig) {
                continue;
            }
            for (NetConfig currentNetConfig : currentNetConfigs) {
                if (newNetConfig instanceof DhcpServerConfigIP4 && currentNetConfig instanceof DhcpServerConfigIP4) {
                    DhcpServerConfigIP4 newDhcpServerConfigIP4 = (DhcpServerConfigIP4) newNetConfig;
                    DhcpServerConfigIP4 currenDhcpServerConfigIP4 = (DhcpServerConfigIP4) currentNetConfig;
                    if (!newDhcpServerConfigIP4.isEnabled() && !currenDhcpServerConfigIP4.isEnabled()) {
                        continue;
                    }
                }
                if (newNetConfig.getClass() == currentNetConfig.getClass() && !newNetConfig.equals(currentNetConfig)) {
                    logger.debug("\tConfig changed - Current config: {}", currentNetConfig.toString());
                    logger.debug("\tConfig changed - New config: {}", newNetConfig.toString());
                    return true;
                }
            }
        }
        return false;
    }

    // Initialize a monitor thread for each ethernet interface
    private void initializeMonitors() {
        for (Entry<String, NetInterfaceConfig<NetInterfaceAddressConfig>> networkConfig : this.networkConfiguration
                .entrySet()) {
            String interfaceName = networkConfig.getKey();
            NetInterfaceConfig<NetInterfaceAddressConfig> ifaceConfig = networkConfig.getValue();
            if (((AbstractNetInterface<?>) ifaceConfig).isInterfaceManaged()) {
                logger.debug("initializeMonitors() :: Starting monitor for {} interface", interfaceName);
                startMonitor(interfaceName);
            }
        }
    }

    // Start a interface specific monitor thread
    private void startMonitor(final String interfaceName) {
        synchronized (lock) {
            if (tasks == null) {
                tasks = new HashMap<>();
            }
            if (stopThreads == null) {
                stopThreads = new HashMap<>();
            }

            // Ensure monitor doesn't already exist for this interface
            if (tasks.get(interfaceName) == null) {
                logger.info("Starting monitor for {}", interfaceName);
                stopThreads.put(interfaceName, new AtomicBoolean(false));
                Future<?> task = this.executor.submit(() -> {
                    Thread.currentThread().setName("EthernetMonitor_" + interfaceName);
                    while (!stopThreads.get(interfaceName).get()) {
                        try {
                            monitor(interfaceName);
                            monitorWait(interfaceName);
                        } catch (InterruptedException interruptedException) {
                            logger.debug("Ethernet monitor interrupted - {}", interruptedException);
                            Thread.currentThread().interrupt();
                        } catch (Throwable t) {
                            logger.error("Exception while monitoring ethernet connection ", t);
                        }
                    }
                });
                tasks.put(interfaceName, task);
            } else {
                // The monitor is already running.
                monitorNotify(interfaceName);
            }
        }
    }

    // Stop a interface specific monitor thread
    private void stopMonitor(String interfaceName) {
        this.interfaceState.remove(interfaceName);

        Future<?> task = tasks.get(interfaceName);
        if (task != null && !task.isDone()) {
            AtomicBoolean stop = stopThreads.get(interfaceName);
            if (stop != null) {
                stop.set(true);
            }
            monitorNotify(interfaceName);
            logger.debug("Stopping monitor for {} ...", interfaceName);
            task.cancel(true);
            logger.info("Monitor for {} cancelled? = {}", interfaceName, task.isDone());
            tasks.put(interfaceName, null);
        }
    }

    private void disableInterface(String interfaceName) throws KuraException {
        this.netAdminService.disableInterface(interfaceName);
        this.netAdminService.manageDhcpServer(interfaceName, false);
    }

    private void monitorNotify(String interfaceName) {
        Object o = stopThreads.get(interfaceName);
        if (o != null) {
            synchronized (o) {
                o.notifyAll();
            }
        }
    }

    private void monitorWait(String interfaceName) throws InterruptedException {
        Object o = stopThreads.get(interfaceName);
        if (o != null) {
            synchronized (o) {
                o.wait(THREAD_INTERVAL);
            }
        }
    }
}
