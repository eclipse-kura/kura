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
package org.eclipse.kura.nm.configuration.monitor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dns.DnsServerConfig;
import org.eclipse.kura.net.dns.DnsServerConfigIP4;
import org.eclipse.kura.nm.NetworkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsServerMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DnsServerMonitor.class);

    private static final long THREAD_INTERVAL = 60000;
    private static final long THREAD_TERMINATION_TOUT = 60; // in seconds
    private Future<?> monitorTask;
    private ScheduledExecutorService executor;

    private boolean enabled;
    private NetworkConfiguration networkConfiguration;
    private Set<NetworkPair<IP4Address>> allowedNetworks;
    private Set<IP4Address> forwarders;

    private final LinuxDns dnsUtil = LinuxDns.getInstance();

    private final DnsServerService dnsServerService;
    private final CommandExecutorService executorService;
    private NetworkProperties networkProperties;

    private LinuxNetworkUtil linuxNetworkUtil;

    public DnsServerMonitor(DnsServerService dnsServerService, CommandExecutorService executorService) {

        this.dnsServerService = dnsServerService;
        this.executorService = executorService;

        this.linuxNetworkUtil = new LinuxNetworkUtil(this.executorService);

    }

    public void setNetworkProperties(NetworkProperties networkProperties) {
        this.networkProperties = networkProperties;
    }

    public void start() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName("DnsMonitorServiceImpl");
            return t;
        });

        this.monitorTask = this.executor.scheduleWithFixedDelay(this::monitorDnsServerStatus, 0, THREAD_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (this.monitorTask != null && !this.monitorTask.isDone()) {
            logger.debug("Cancelling DnsServerMonitor task ...");
            this.monitorTask.cancel(false);
            logger.info("DnsServerMonitor task cancelled? = {}", this.monitorTask.isDone());
            this.monitorTask = null;
        }

        if (this.executor != null) {
            logger.debug("Terminating DnsServerMonitor Thread ...");
            this.executor.shutdown();
            try {
                this.executor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.warn("Interrupted", e);
                Thread.currentThread().interrupt();
            }
            logger.info("DnsServerMonitor Thread terminated? - {}", this.executor.isTerminated());
            this.executor = null;
        }
    }

    public void clear() {
        this.networkProperties = null;
    }

    private void monitorDnsServerStatus() {

        logger.debug("DnsMonitor task start");

        try {
            this.networkConfiguration = new NetworkConfiguration(this.networkProperties.getProperties());
        } catch (Exception e) {
            logger.error("Could not get initial network configuration", e);
        }

        Set<IPAddress> systemDnsServers = this.dnsUtil.getDnServers();

        // Check that resolv.conf matches what is configured
        Set<IPAddress> configuredServers = getConfiguredDnsServers();
        if (!configuredServers.equals(systemDnsServers)) {
            setDnsServers(configuredServers);
            systemDnsServers = configuredServers;
        }

        manageDnsProxies(systemDnsServers);
        logger.debug("DnsMonitor task stop");

    }

    private void manageDnsProxies(Set<IPAddress> dnsServers) {
        Set<IP4Address> fwds = new HashSet<>();
        if (dnsServers != null && !dnsServers.isEmpty()) {
            for (IPAddress dnsServerTmp : dnsServers) {
                logger.debug("Found DNS Server: {}", dnsServerTmp.getHostAddress());
                fwds.add((IP4Address) dnsServerTmp);
            }
        }

        if (!fwds.isEmpty() && !fwds.equals(this.forwarders)) {
            // there was a change - deal with it
            logger.info("Detected DNS resolv.conf change - restarting DNS proxy");
            this.forwarders = fwds;

            DnsServerConfig currentDnsServerConfig = this.dnsServerService.getConfig();
            DnsServerConfigIP4 newDnsServerConfig = new DnsServerConfigIP4(this.forwarders, this.allowedNetworks);

            if (currentDnsServerConfig != null && !currentDnsServerConfig.equals(newDnsServerConfig)) {
                logger.debug("DNS server config has changed - updating from {} to {}", currentDnsServerConfig,
                        newDnsServerConfig);

                reconfigureDNSProxy(newDnsServerConfig);
            }

        }
    }

    protected String getCurrentIpAddress(String interfaceName) throws KuraException {
        return this.linuxNetworkUtil.getCurrentIpAddress(interfaceName);
    }

    protected boolean pppHasAddress(int pppNo) throws KuraException {
        return this.linuxNetworkUtil.hasAddress("ppp" + pppNo);
    }

    protected void reconfigureDNSProxy(DnsServerConfigIP4 dnsServerConfigIP4) {
        try {

            logger.debug("Disabling DNS proxy");
            this.dnsServerService.stop();

            logger.debug("Writing config");
            this.dnsServerService.setConfig(dnsServerConfigIP4);

            if (this.enabled) {
                sleep(500);
                logger.debug("Starting DNS proxy");
                this.dnsServerService.start();
            } else {
                logger.debug("DNS proxy not enabled");
            }
        } catch (KuraException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private boolean isEnabledForWan(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        return ((AbstractNetInterface<?>) netInterfaceConfig).getInterfaceStatus()
                .equals(NetInterfaceStatus.netIPv4StatusEnabledWAN);
    }

    private void setDnsServers(Set<IPAddress> newServers) {
        LinuxDns linuxDns = this.dnsUtil;
        Set<IPAddress> currentServers = linuxDns.getDnServers();

        if (newServers == null) {
            logger.debug("Invalid DNS servers.");
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
                        fillPppDnsServers(netInterfaceConfig, linuxDns, serverList);
                    } else {
                        fillDnsServers(interfaceName, linuxDns, serverList);
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

    private void fillDnsServers(String interfaceName, LinuxDns linuxDns, LinkedHashSet<IPAddress> serverList)
            throws KuraException {
        String currentAddress = getCurrentIpAddress(interfaceName);
        List<IPAddress> servers = linuxDns.getDhcpDnsServers(interfaceName, currentAddress);
        if (!servers.isEmpty()) {
            logger.debug("Configured for DHCP - adding DHCP servers: {}", servers);
            serverList.addAll(servers);
        }
    }

    private void fillPppDnsServers(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig,
            LinuxDns linuxDns, LinkedHashSet<IPAddress> serverList) throws KuraException {
        int pppNo = ((ModemInterfaceConfigImpl) netInterfaceConfig).getPppNum();
        if (pppHasAddress(pppNo)) {
            List<IPAddress> servers = linuxDns.getPppDnServers();
            if (servers != null) {
                logger.debug("Adding PPP dns servers: {}", servers);
                serverList.addAll(servers);
            }
        }
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

}
