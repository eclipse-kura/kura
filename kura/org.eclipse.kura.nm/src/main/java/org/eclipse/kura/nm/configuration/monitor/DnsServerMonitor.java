/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.dhcp.DhcpServerConfig;
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

        this.enabled = false;

        Set<IPAddress> systemDnsServers = this.dnsUtil.getDnServers();

        Set<IP4Address> forwarders = getForwarders(systemDnsServers);
        Set<NetworkPair<IP4Address>> allowedNetworks = getAllowedNetworks();

        manageDnsProxies(forwarders, allowedNetworks);
        logger.debug("DnsMonitor task stop");

    }

    private Set<NetworkPair<IP4Address>> getAllowedNetworks() {

        Set<NetworkPair<IP4Address>> allowedNetworks = new HashSet<>();

        if (this.networkConfiguration != null && this.networkConfiguration.getNetInterfaceConfigs() != null) {
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = this.networkConfiguration
                    .getNetInterfaceConfigs();

            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if (isSupportedInterfaceType(netInterfaceConfig) && isEnabledForLan(netInterfaceConfig)) {

                    logger.debug("Getting DNS proxy config for {}", netInterfaceConfig.getName());
                    List<NetConfig> netConfigs = ((AbstractNetInterface<?>) netInterfaceConfig).getNetConfigs();
                    for (NetConfig netConfig : netConfigs) {
                        addToAllowedNetworksIfPassDnsEnabled(allowedNetworks, netConfig);
                    }
                }
            }
        }

        return allowedNetworks;

    }

    private void addToAllowedNetworksIfPassDnsEnabled(Set<NetworkPair<IP4Address>> allowedNetworks, NetConfig netConfig) {
        if (isPassDnsEnabled(netConfig)) {

            DhcpServerConfig dhcpServerConfig = (DhcpServerConfig) netConfig;
            IPAddress routerAddress = dhcpServerConfig.getRouterAddress();
            short prefix = dhcpServerConfig.getPrefix();

            logger.debug("Found an allowed network: {}/{}", routerAddress, prefix);
            this.enabled = true;

            // this is an 'allowed network'
            allowedNetworks.add(toNetworkPair(routerAddress, prefix));
        }
    }

    private NetworkPair<IP4Address> toNetworkPair(IPAddress routerAddress, short prefix) {
        return new NetworkPair<>((IP4Address) routerAddress, prefix);
    }

    private boolean isSupportedInterfaceType(
            NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        return netInterfaceConfig.getType() == NetInterfaceType.ETHERNET
                || netInterfaceConfig.getType() == NetInterfaceType.WIFI
                || netInterfaceConfig.getType() == NetInterfaceType.MODEM;
    }

    private boolean isEnabledForLan(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig) {
        return ((AbstractNetInterface<?>) netInterfaceConfig).getInterfaceStatus()
                .equals(NetInterfaceStatus.netIPv4StatusEnabledLAN);
    }

    public boolean isPassDnsEnabled(NetConfig netConfig) {
        return (netConfig instanceof DhcpServerConfig && ((DhcpServerConfig) netConfig).isPassDns());
    }

    private Set<IP4Address> getForwarders(Set<IPAddress> dnsServers) {
        Set<IP4Address> forwarders = new HashSet<>();

        if (dnsServers != null && !dnsServers.isEmpty()) {
            for (IPAddress dnsServerTmp : dnsServers) {
                logger.debug("Found DNS Server: {}", dnsServerTmp.getHostAddress());
                forwarders.add((IP4Address) dnsServerTmp);
            }
        }

        return forwarders;
    }

    private void manageDnsProxies(Set<IP4Address> forwarders, Set<NetworkPair<IP4Address>> allowedNetworks) {

        DnsServerConfig currentDnsServerConfig = this.dnsServerService.getConfig();
        DnsServerConfigIP4 newDnsServerConfig = new DnsServerConfigIP4(forwarders, allowedNetworks);

        if (currentDnsServerConfig == null || !currentDnsServerConfig.equals(newDnsServerConfig)) {
            logger.debug("DNS server config has changed - updating from {} to {}", currentDnsServerConfig,
                    newDnsServerConfig);

            reconfigureDNSProxy(newDnsServerConfig);
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
