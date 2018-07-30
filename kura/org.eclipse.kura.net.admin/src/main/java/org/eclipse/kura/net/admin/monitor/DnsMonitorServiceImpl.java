/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates and others
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.admin.NetworkConfigurationService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.event.NetworkStatusChangeEvent;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
import org.eclipse.kura.net.dns.DnsMonitorService;
import org.eclipse.kura.net.dns.DnsServer;
import org.eclipse.kura.net.dns.DnsServerConfig;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsMonitorServiceImpl implements DnsMonitorService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(DnsMonitorServiceImpl.class);

    private static final String[] EVENT_TOPICS = new String[] {
            NetworkStatusChangeEvent.NETWORK_EVENT_STATUS_CHANGE_TOPIC,
            NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC };

    private static final long THREAD_INTERVAL = 60000;
    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds
    private static Future<?> monitorTask;
    private ExecutorService executor;

    private boolean enabled;
    private static AtomicBoolean stopThread;
    private NetworkConfigurationService netConfigService;
    private NetworkConfiguration networkConfiguration;
    private Set<NetworkPair<IP4Address>> allowedNetworks;
    private Set<IP4Address> forwarders;

    private LinuxDns dnsUtil;
    private DnsServer dnsServer;

    public void setNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = netConfigService;
    }

    public void unsetNetworkConfigurationService(NetworkConfigurationService netConfigService) {
        this.netConfigService = null;
    }

    public void setDnsServerService(DnsServer dnsServer) {
        this.dnsServer = dnsServer;
    }

    public void unsetDnsServerService(DnsServer dnsServer) {
        this.dnsServer = null;
    }

    protected void activate(ComponentContext componentContext) {
        logger.debug("Activating DnsProxyMonitor Service...");

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        componentContext.getBundleContext().registerService(EventHandler.class.getName(), this, d);

        try {
            this.networkConfiguration = this.netConfigService.getNetworkConfiguration();
        } catch (KuraException e) {
            logger.error("Could not get initial network configuration", e);
        }

        this.dnsUtil = LinuxDns.getInstance();

        stopThread = new AtomicBoolean();

        // FIXME - brute force handler for DNS updates
        this.executor = Executors.newSingleThreadExecutor();
        stopThread.set(false);
        monitorTask = this.executor.submit(() -> {
            while (!stopThread.get()) {
                Thread.currentThread().setName("DnsMonitorServiceImpl");
                Set<IPAddress> dnsServers = DnsMonitorServiceImpl.this.dnsUtil.getDnServers();

                // Check that resolv.conf matches what is configured
                Set<IPAddress> configuredServers = getConfiguredDnsServers();
                if (!configuredServers.equals(dnsServers)) {
                    setDnsServers(configuredServers);
                    dnsServers = configuredServers;
                }

                Set<IP4Address> fwds = new HashSet<>();
                if (dnsServers != null && !dnsServers.isEmpty()) {
                    for (IPAddress dnsServer : dnsServers) {
                        logger.debug("Found DNS Server: {}", dnsServer.getHostAddress());
                        fwds.add((IP4Address) dnsServer);
                    }
                }

                if (fwds != null && !fwds.isEmpty() && !fwds.equals(DnsMonitorServiceImpl.this.forwarders)) {
                    // there was a change - deal with it
                    logger.info("Detected DNS resolv.conf change - restarting DNS proxy");
                    DnsMonitorServiceImpl.this.forwarders = fwds;

                    DnsServerConfig currentDnsServerConfig = this.dnsServer.getDnsServerConfig();
                    DnsServerConfigIP4 newDnsServerConfig = new DnsServerConfigIP4(
                            DnsMonitorServiceImpl.this.forwarders, DnsMonitorServiceImpl.this.allowedNetworks);

                    if (currentDnsServerConfig.equals(newDnsServerConfig)) {
                        logger.debug("DNS server config has changed - updating from {} to {}", currentDnsServerConfig,
                                newDnsServerConfig);

                        reconfigureDNSProxy(newDnsServerConfig);
                    }

                }
                try {
                    monitorWait();
                } catch (InterruptedException e) {
                    logger.debug("DNS monitor interrupted", e);
                }
            }
        });

    }

    protected void deactivate(ComponentContext componentContext) {
        if (monitorTask != null && !monitorTask.isDone()) {
            stopThread.set(true);
            monitorNotity();
            logger.debug("Cancelling DnsMonitorServiceImpl task ...");
            monitorTask.cancel(true);
            logger.info("DnsMonitorServiceImpl task cancelled? = {}", monitorTask.isDone());
            monitorTask = null;
        }

        if (this.executor != null) {
            logger.debug("Terminating DnsMonitorServiceImpl Thread ...");
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
                Thread.currentThread().interrupt();
            }
            logger.info("DnsMonitorServiceImpl Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }
    }

    protected String getCurrentIpAddress(String interfaceName) throws KuraException {
        return LinuxNetworkUtil.getCurrentIpAddress(interfaceName);
    }

    protected boolean pppHasAddress(int pppNo) throws KuraException {
        return LinuxNetworkUtil.hasAddress("ppp" + pppNo);
    }

    protected void reconfigureDNSProxy(DnsServerConfigIP4 dnsServerConfigIP4) {
        try {

            logger.debug("Disabling DNS proxy");
            this.dnsServer.disable();

            logger.debug("Writing config");
            this.dnsServer.setConfig(dnsServerConfigIP4);

            if (this.enabled) {
                sleep(500);
                logger.debug("Starting DNS proxy");
                this.dnsServer.enable();
            } else {
                logger.debug("DNS proxy not enabled");
            }
        } catch (KuraException e) {
            logger.warn(e.getMessage(), e);
        }
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
                    this.networkConfiguration = new NetworkConfiguration(props);
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            updateDnsResolverConfig();
            updateDnsProxyConfig();
        } else if (topic.equals(NetworkStatusChangeEvent.NETWORK_EVENT_STATUS_CHANGE_TOPIC)) {
            updateDnsResolverConfig();
            updateDnsProxyConfig();
        }
    }

    private void updateDnsResolverConfig() {
        logger.debug("Updating resolver config");
        setDnsServers(getConfiguredDnsServers());
    }

    private void updateDnsProxyConfig() {
        this.enabled = false;

        this.allowedNetworks = new HashSet<>();
        this.forwarders = new HashSet<>();

        if (this.networkConfiguration != null && this.networkConfiguration.getNetInterfaceConfigs() != null) {
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = this.networkConfiguration
                    .getNetInterfaceConfigs();
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if (netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                        || netInterfaceConfig.getType() == NetInterfaceType.WIFI
                        || netInterfaceConfig.getType() == NetInterfaceType.MODEM) {
                    try {
                        getAllowedNetworks(netInterfaceConfig);
                    } catch (KuraException e) {
                        logger.error("Error updating dns proxy", e);
                    }
                }
            }
        }

        Set<IPAddress> dnsServers = this.dnsUtil.getDnServers();
        if (dnsServers != null && !dnsServers.isEmpty()) {
            for (IPAddress dnsServer : dnsServers) {
                logger.debug("Found DNS Server: {}", dnsServer.getHostAddress());
                this.forwarders.add((IP4Address) dnsServer);
            }
        }

        DnsServerConfigIP4 dnsServerConfigIP4 = new DnsServerConfigIP4(this.forwarders, this.allowedNetworks);
        reconfigureDNSProxy(dnsServerConfigIP4);
    }

    private void getAllowedNetworks(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig)
            throws KuraException {

        logger.debug("Getting DNS proxy config for {}", netInterfaceConfig.getName());
        List<NetConfig> netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
        for (NetConfig netConfig : netConfigs) {
            if (netConfig instanceof DhcpServerConfig && ((DhcpServerConfig) netConfig).isPassDns()) {
                logger.debug("Found an allowed network: {}/{}", ((DhcpServerConfig) netConfig).getRouterAddress(),
                        ((DhcpServerConfig) netConfig).getPrefix());
                this.enabled = true;

                // this is an 'allowed network'
                this.allowedNetworks
                        .add(new NetworkPair<>((IP4Address) ((DhcpServerConfig) netConfig).getRouterAddress(),
                                ((DhcpServerConfig) netConfig).getPrefix()));
            }
        }
    }

    private boolean isEnabledForWan(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        return ((AbstractNetInterface<?>) netInterfaceConfig).getInterfaceStatus()
                .equals(NetInterfaceStatus.netIPv4StatusEnabledWAN);
    }

    private void setDnsServers(Set<IPAddress> newServers) {
        LinuxDns linuxDns = this.dnsUtil;
        Set<IPAddress> currentServers = linuxDns.getDnServers();

        if (newServers == null || newServers.isEmpty()) {
            logger.debug("Not Setting DNS servers to empty");
            return;
        }

        if (currentServers != null && !currentServers.isEmpty()) {
            if (!currentServers.equals(newServers)) {
                logger.info("Change to DNS - setting dns servers: {}", newServers);
                linuxDns.setDnServers(newServers);
            } else {
                logger.debug("No change to DNS servers - not updating");
            }
        } else {
            logger.info("Current DNS servers are null - setting dns servers: {}", newServers);
            linuxDns.setDnServers(newServers);
        }
    }

    // Get a list of dns servers for all WAN interfaces
    private Set<IPAddress> getConfiguredDnsServers() {
        LinkedHashSet<IPAddress> serverList = new LinkedHashSet<>();
        if (this.networkConfiguration != null && this.networkConfiguration.getNetInterfaceConfigs() != null) {
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = this.networkConfiguration
                    .getNetInterfaceConfigs();
            // If there are multiple WAN interfaces, their configured DNS servers are all included in no particular
            // order
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if ((netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                        || netInterfaceConfig.getType() == NetInterfaceType.WIFI
                        || netInterfaceConfig.getType() == NetInterfaceType.MODEM)
                        && isEnabledForWan(netInterfaceConfig)) {
                    try {
                        Set<IPAddress> servers = getConfiguredDnsServers(netInterfaceConfig);
                        logger.trace("{} is WAN, adding its dns servers: {}", netInterfaceConfig.getName(), servers);
                        serverList.addAll(servers);
                    } catch (KuraException e) {
                        logger.error("Error adding dns servers for {}", netInterfaceConfig.getName(), e);
                    }
                }
            }
        }
        return serverList;
    }

    // Get a list of dns servers for the specified NetInterfaceConfig
    private Set<IPAddress> getConfiguredDnsServers(
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) throws KuraException {
        String interfaceName = netInterfaceConfig.getName();
        logger.trace("Getting dns servers for {}", interfaceName);
        LinuxDns linuxDns = this.dnsUtil;
        LinkedHashSet<IPAddress> serverList = new LinkedHashSet<>();

        NetConfigIP4 netConfigIP4 = ((AbstractNetInterface<?>) netInterfaceConfig).getIP4config();
        if (netConfigIP4 != null) {
            List<IP4Address> userServers = netConfigIP4.getDnsServers();
            if (netConfigIP4.isDhcp()) {
                // If DHCP but there are user defined entries, use those instead
                if (userServers != null && !userServers.isEmpty()) {
                    logger.debug("Configured for DHCP with user-defined servers - adding: {}", userServers);
                    serverList.addAll(userServers);
                } else {
                    if (netInterfaceConfig.getType().equals(NetInterfaceType.MODEM)) {
                        // FIXME - don't like this
                        // cannot use interfaceName here because it one config behind
                        int pppNo = ((ModemInterfaceConfigImpl) netInterfaceConfig).getPppNum();
                        if (pppHasAddress(pppNo)) {
                            List<IPAddress> servers = linuxDns.getPppDnServers();
                            if (servers != null) {
                                logger.debug("Adding PPP dns servers: {}", servers);
                                serverList.addAll(servers);
                            }
                        }
                    } else {
                        String currentAddress = getCurrentIpAddress(interfaceName);
                        List<IPAddress> servers = linuxDns.getDhcpDnsServers(interfaceName, currentAddress);
                        if (!servers.isEmpty()) {
                            logger.debug("Configured for DHCP - adding DHCP servers: {}", servers);
                            serverList.addAll(servers);
                        }
                    }
                }
            } else {
                // If static, use the user defined entries
                logger.debug("Configured for static - adding user-defined servers: {}", userServers);
                serverList.addAll(userServers);
            }
        }
        return serverList;
    }

    /**
     * Sleep at least <code>millis</code> ms, even if sleep is interrupted
     */
    private static void sleep(int millis) {
        long start = System.currentTimeMillis();
        long now = start;
        long end = start + millis;

        while (now < end) {
            try {
                Thread.sleep(end - now);
            } catch (InterruptedException e) {
                logger.debug("sleep interrupted: ", e);
                Thread.currentThread().interrupt();
            }

            now = System.currentTimeMillis();
        }
    }

    private void monitorNotity() {
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
