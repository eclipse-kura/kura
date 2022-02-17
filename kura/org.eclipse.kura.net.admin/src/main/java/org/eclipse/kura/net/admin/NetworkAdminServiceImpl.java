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
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.net.admin;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.net.AbstractNetInterface;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.modem.ModemInterfaceConfigImpl;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.internal.linux.net.dns.DnsServerService;
import org.eclipse.kura.internal.linux.net.wifi.WifiDriverService;
import org.eclipse.kura.linux.net.dhcp.DhcpClientManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.linux.net.util.DhcpLeaseTool;
import org.eclipse.kura.linux.net.util.IScanTool;
import org.eclipse.kura.linux.net.util.IwCapabilityTool;
import org.eclipse.kura.linux.net.util.LinuxIfconfig;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.util.ScanTool;
import org.eclipse.kura.linux.net.wifi.HostapdManager;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantStatus;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfig6;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkAdminService;
import org.eclipse.kura.net.admin.event.NetworkConfigurationChangeEvent;
import org.eclipse.kura.net.admin.monitor.InterfaceStateBuilder;
import org.eclipse.kura.net.admin.monitor.WifiInterfaceState;
import org.eclipse.kura.net.admin.visitor.linux.WpaSupplicantConfigWriter;
import org.eclipse.kura.net.admin.visitor.linux.WpaSupplicantConfigWriterFactory;
import org.eclipse.kura.net.dhcp.DhcpLease;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.RuleType;
import org.eclipse.kura.net.modem.ModemConfig;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.eclipse.kura.net.wifi.WifiHotspotInfo;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkAdminServiceImpl implements NetworkAdminService, EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NetworkAdminServiceImpl.class);

    private ConfigurationService configurationService;
    private NetworkConfigurationService networkConfigurationService;
    private FirewallConfigurationService firewallConfigurationService;
    private DnsServerService dnsServer;
    private CommandExecutorService executorService;

    private Object wifiClientMonitorServiceLock;

    private WifiDriverService wifiDriverService;

    private boolean pendingNetworkConfigurationChange = false;

    private static final String[] EVENT_TOPICS = new String[] {
            NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC };

    private ComponentContext context;
    private DhcpClientManager dhcpClientManager;
    private DhcpServerManager dhcpServerManager;
    private LinuxNetworkUtil linuxNetworkUtil;
    private WpaSupplicantManager wpaSupplicantManager;
    private HostapdManager hostapdManager;
    private WpaSupplicantConfigWriterFactory wpaSupplicantConfigWriterFactory;

    public NetworkAdminServiceImpl() {
        this.wpaSupplicantConfigWriterFactory = WpaSupplicantConfigWriterFactory.getDefault();
    }

    public NetworkAdminServiceImpl(WpaSupplicantConfigWriterFactory wpaSupplicantConfigWriterFactory) {
        this.wpaSupplicantConfigWriterFactory = wpaSupplicantConfigWriterFactory;
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void unsetConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    public void setNetworkConfigurationService(NetworkConfigurationService networkConfigurationService) {
        this.networkConfigurationService = networkConfigurationService;
    }

    public void unsetNetworkConfigurationService(NetworkConfigurationService networkConfigurationService) {
        this.networkConfigurationService = null;
    }

    public void setFirewallConfigurationService(FirewallConfigurationService firewallConfigurationService) {
        this.firewallConfigurationService = firewallConfigurationService;
    }

    public void unsetFirewallConfigurationService(FirewallConfigurationService firewallConfigurationService) {
        this.firewallConfigurationService = null;
    }

    public void setDnsServerService(DnsServerService dnsServer) {
        this.dnsServer = dnsServer;
    }

    public void unsetDnsServerService(DnsServerService dnsServer) {
        this.dnsServer = null;
    }

    // hack to synchronize verifyWifiCredentials() with WifiClientMonitorService
    public synchronized void setWifiClientMonitorServiceLock(Object lock) {
        this.wifiClientMonitorServiceLock = lock;
    }

    public synchronized void unsetWifiClientMonitorServiceLock() {
        this.wifiClientMonitorServiceLock = null;
    }

    public void setWifiDriverService(WifiDriverService wifiDriverService) {
        this.wifiDriverService = wifiDriverService;
    }

    public void unsetWifiDriverService(WifiDriverService wifiDriverService) {
        this.wifiDriverService = null;
    }

    public void setExecutorService(CommandExecutorService executorService) {
        this.executorService = executorService;
    }

    public void unsetExecutorService(CommandExecutorService executorService) {
        this.executorService = null;
    }

    public void setUserAdmin(final UserAdmin userAdmin) {
        userAdmin.createRole("kura.network.admin", Role.GROUP);
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {

        logger.debug("Activating NetworkAdmin Service...");

        // save the bundle context
        this.context = componentContext;

        // since we are just starting up, start named if needed

        try {
            if (this.dnsServer.isConfigured()) {
                this.dnsServer.stop();
                this.dnsServer.start();
            }
        } catch (KuraException e) {
            logger.warn("Exception while activating NetworkAdmin Service!", e);
        }

        Dictionary<String, String[]> d = new Hashtable<>();
        d.put(EventConstants.EVENT_TOPIC, EVENT_TOPICS);
        this.context.getBundleContext().registerService(EventHandler.class.getName(), this, d);
        this.dhcpClientManager = new DhcpClientManager(this.executorService);
        this.dhcpServerManager = new DhcpServerManager(this.executorService);
        this.linuxNetworkUtil = new LinuxNetworkUtil(this.executorService);
        this.wpaSupplicantManager = new WpaSupplicantManager(this.executorService);
        this.hostapdManager = new HostapdManager(this.executorService);
        logger.debug("Done Activating NetworkAdmin Service...");
    }

    protected void deactivate(ComponentContext componentContext) {
        // Empty method
    }

    protected List<WifiAccessPoint> getWifiAccessPoints(String ifaceName) throws KuraException {
        List<WifiAccessPoint> wifiAccessPoints;

        IScanTool scanTool = ScanTool.get(ifaceName, this.executorService);
        if (scanTool != null) {
            wifiAccessPoints = scanTool.scan();
        } else {
            wifiAccessPoints = new ArrayList<>();
        }

        return wifiAccessPoints;
    }

    @Override
    // FIXME: This api should be deprecated in favor of the following signature:
    // List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs()
    public List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getNetworkInterfaceConfigs()
            throws KuraException {

        try {
            logger.debug("Getting all networkInterfaceConfigs");
            return this.networkConfigurationService.getNetworkConfiguration().getNetInterfaceConfigs();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<NetConfig> getNetworkInterfaceConfigs(String interfaceName) throws KuraException {

        ArrayList<NetConfig> netConfigs = new ArrayList<>();
        NetworkConfiguration networkConfig = this.networkConfigurationService.getNetworkConfiguration();
        if (interfaceName != null && networkConfig != null) {
            try {
                logger.debug("Getting networkInterfaceConfigs for {}", interfaceName);
                if (networkConfig.getNetInterfaceConfigs() != null
                        && !networkConfig.getNetInterfaceConfigs().isEmpty()) {
                    for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : networkConfig
                            .getNetInterfaceConfigs()) {
                        if (interfaceName.equals(netInterfaceConfig.getName())) {
                            List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                                    .getNetInterfaceAddresses();
                            if (netInterfaceAddressConfigs != null && !netInterfaceAddressConfigs.isEmpty()) {
                                for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
                                    netConfigs.addAll(netInterfaceAddressConfig.getConfigs());
                                }
                            }

                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }

        return netConfigs;
    }

    @SuppressWarnings("checkstyle:methodLength")
    @Override
    public void updateEthernetInterfaceConfig(String interfaceName, boolean autoConnect, int mtu,
            List<NetConfig> netConfigs) throws KuraException {

        NetConfigIP4 netConfig4 = null;
        NetConfigIP6 netConfig6 = null;
        DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
        FirewallAutoNatConfig natConfig = null;
        boolean hadNetConfig4 = false;
        boolean hadNetConfig6 = false;
        boolean hadDhcpServerConfigIP4 = false;
        boolean hadNatConfig = false;

        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (!netConfig.isValid()) {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                            "NetConfig Configuration is invalid: " + netConfig.toString());
                }
                if (netConfig instanceof NetConfigIP4) {
                    netConfig4 = (NetConfigIP4) netConfig;
                } else if (netConfig instanceof NetConfigIP6) {
                    netConfig6 = (NetConfigIP6) netConfig;
                } else if (netConfig instanceof DhcpServerConfigIP4) {
                    dhcpServerConfigIP4 = (DhcpServerConfigIP4) netConfig;
                } else if (netConfig instanceof FirewallAutoNatConfig) {
                    natConfig = (FirewallAutoNatConfig) netConfig;
                }
            }
        }

        // validation
        if (netConfig4 == null && netConfig6 == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
                    "Either IPv4 or IPv6 configuration must be defined");
        }

        List<String> modifiedInterfaceNames = new ArrayList<>();
        boolean configurationChanged = false;

        ComponentConfiguration originalNetworkComponentConfiguration = ((SelfConfiguringComponent) this.networkConfigurationService)
                .getConfiguration();
        if (originalNetworkComponentConfiguration == null) {
            logger.debug("Returning for some unknown reason - no existing config???");
            return;
        }
        try {
            NetworkConfiguration newNetworkConfiguration = new NetworkConfiguration(
                    originalNetworkComponentConfiguration.getConfigurationProperties());
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = newNetworkConfiguration
                    .getNetInterfaceConfigs();
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if (netInterfaceConfig.getName().equals(interfaceName)) {
                    // handle MTU
                    if (mtu != netInterfaceConfig.getMTU()) {
                        AbstractNetInterface<?> absNetInterfaceConfig = (AbstractNetInterface<?>) netInterfaceConfig;
                        logger.debug("updating MTU for {}", interfaceName);
                        absNetInterfaceConfig.setMTU(mtu);
                        configurationChanged = true;
                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                            modifiedInterfaceNames.add(interfaceName);
                        }
                    }

                    // handle autoconnect
                    if (autoConnect != netInterfaceConfig.isAutoConnect()) {
                        AbstractNetInterface<?> absNetInterfaceConfig = (AbstractNetInterface<?>) netInterfaceConfig;
                        logger.debug("updating autoConnect for {} to be {}", interfaceName, autoConnect);
                        absNetInterfaceConfig.setAutoConnect(autoConnect);
                        configurationChanged = true;
                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                            modifiedInterfaceNames.add(interfaceName);
                        }
                    }

                    // replace existing configs
                    NetInterfaceAddressConfig netInterfaceAddressConfig = netInterfaceConfig.getNetInterfaceAddresses()
                            .get(0);
                    if (netInterfaceAddressConfig != null) {
                        List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
                        List<NetConfig> newNetConfigs = new ArrayList<>();
                        for (NetConfig netConfig : existingNetConfigs) {
                            logger.debug("looking at existing NetConfig for {} with value: {}", interfaceName,
                                    netConfig);
                            if (netConfig instanceof NetConfigIP4) {
                                if (netConfig4 == null) {
                                    logger.debug("removing NetConfig4 for {}", interfaceName);
                                } else {
                                    hadNetConfig4 = true;
                                    newNetConfigs.add(netConfig4);
                                    if (!netConfig.equals(netConfig4)) {
                                        logger.debug("updating NetConfig4 for {}", interfaceName);
                                        logger.debug("Is new State DHCP? {}", netConfig4.isDhcp());
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating NetConfig4 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof NetConfig6) {
                                if (netConfig6 == null) {
                                    logger.debug("removing NetConfig6 for {}", interfaceName);
                                } else {
                                    hadNetConfig6 = true;
                                    newNetConfigs.add(netConfig6);
                                    if (!netConfig.equals(netConfig6)) {
                                        logger.debug("updating NetConfig6 for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating NetConfig6 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof DhcpServerConfigIP4) {
                                if (dhcpServerConfigIP4 == null) {
                                    logger.debug("removing DhcpServerConfigIP4 for {}", interfaceName);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                } else {
                                    hadDhcpServerConfigIP4 = true;
                                    newNetConfigs.add(dhcpServerConfigIP4);
                                    if (!netConfig.equals(dhcpServerConfigIP4)) {
                                        logger.debug("updating DhcpServerConfigIP4 for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating DhcpServerConfigIP4 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof FirewallAutoNatConfig) {
                                if (natConfig == null) {
                                    logger.debug("removing FirewallAutoNatConfig for {}", interfaceName);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                } else {
                                    hadNatConfig = true;
                                    newNetConfigs.add(natConfig);
                                    if (!netConfig.equals(natConfig)) {
                                        logger.debug("updating FirewallAutoNatConfig for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug(
                                                "not updating FirewallAutoNatConfig for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else {
                                logger.debug("Found unsupported configuration: {}", netConfig.toString());
                            }
                        }

                        // add configs that did not match any in the current configuration
                        if (netConfigs != null) {
                            for (NetConfig netConfig : netConfigs) {
                                if (netConfig instanceof NetConfigIP4 && !hadNetConfig4) {
                                    logger.debug("adding new NetConfig4 to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof NetConfigIP6 && !hadNetConfig6) {
                                    logger.debug("adding new NetConfig6 to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof DhcpServerConfigIP4 && !hadDhcpServerConfigIP4) {
                                    logger.debug("adding new DhcpServerConfigIP4 to existing config for {}",
                                            interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof FirewallAutoNatConfig && !hadNatConfig) {
                                    logger.debug("adding new FirewallAutoNatConfig to existing config for {}",
                                            interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                            }
                        }

                        for (NetConfig netConfig : newNetConfigs) {
                            logger.debug("New NetConfig: {} :: {}", netConfig.getClass().toString(),
                                    netConfig.toString());
                        }

                        // replace with new list
                        ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(newNetConfigs);
                    }
                }
            }

            if (configurationChanged) {
                submitNetworkConfiguration(modifiedInterfaceNames, newNetworkConfiguration);
            }
        } catch (UnknownHostException e) {
            logger.warn("Exception while updating EthernetInterfaceConfig", e);
        }
    }

    @SuppressWarnings("checkstyle:methodLength")
    @Override
    public void updateWifiInterfaceConfig(String interfaceName, boolean autoConnect, WifiAccessPoint accessPoint,
            List<NetConfig> netConfigs) throws KuraException {

        NetConfigIP4 netConfig4 = null;
        NetConfigIP6 netConfig6 = null;
        WifiConfig wifiConfig = null;
        DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
        FirewallAutoNatConfig natConfig = null;
        boolean hadNetConfig4 = false;
        boolean hadNetConfig6 = false;
        boolean hadWifiConfig = false;
        boolean hadDhcpServerConfigIP4 = false;
        boolean hadNatConfig = false;

        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (!netConfig.isValid()) {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                            "NetConfig Configuration is invalid: " + netConfig.toString());
                }

                if (netConfig instanceof NetConfigIP4) {
                    logger.debug("got new NetConfigIP4");
                    netConfig4 = (NetConfigIP4) netConfig;
                } else if (netConfig instanceof NetConfigIP6) {
                    logger.debug("got new NetConfigIP6");
                    netConfig6 = (NetConfigIP6) netConfig;
                } else if (netConfig instanceof WifiConfig) {
                    logger.debug("got new WifiConfig");
                    wifiConfig = (WifiConfig) netConfig;
                } else if (netConfig instanceof DhcpServerConfigIP4) {
                    logger.debug("got new DhcpServerConfigIP4");
                    dhcpServerConfigIP4 = (DhcpServerConfigIP4) netConfig;
                } else if (netConfig instanceof FirewallAutoNatConfig) {
                    logger.debug("got new NatConfig");
                    natConfig = (FirewallAutoNatConfig) netConfig;
                }
            }
        }

        // validation
        if (netConfig4 == null && netConfig6 == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
                    "Either IPv4 or IPv6 configuration must be defined");
        }
        if (wifiConfig == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
                    "WiFi configuration must be defined");
        }

        List<String> modifiedInterfaceNames = new ArrayList<>();
        boolean configurationChanged = false;

        ComponentConfiguration originalNetworkComponentConfiguration = ((SelfConfiguringComponent) this.networkConfigurationService)
                .getConfiguration();
        if (originalNetworkComponentConfiguration == null) {
            return;
        }
        try {
            NetworkConfiguration newNetworkConfiguration = new NetworkConfiguration(
                    originalNetworkComponentConfiguration.getConfigurationProperties());
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = newNetworkConfiguration
                    .getNetInterfaceConfigs();
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if (netInterfaceConfig.getName().equals(interfaceName)) {

                    // replace existing configs
                    NetInterfaceAddressConfig netInterfaceAddressConfig = netInterfaceConfig.getNetInterfaceAddresses()
                            .get(0);
                    if (netInterfaceAddressConfig != null) {
                        List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
                        List<NetConfig> newNetConfigs = new ArrayList<>();
                        WifiMode newWifiMode = wifiConfig != null ? wifiConfig.getMode() : null;
                        for (NetConfig netConfig : existingNetConfigs) {
                            logger.debug("looking at existing NetConfig for {} with value: {}", interfaceName,
                                    netConfig);
                            if (netConfig instanceof NetConfigIP4) {
                                if (netConfig4 == null) {
                                    logger.debug("removing NetConfig4 for {}", interfaceName);
                                } else {
                                    hadNetConfig4 = true;
                                    newNetConfigs.add(netConfig4);
                                    if (!netConfig.equals(netConfig4)) {
                                        logger.debug("updating NetConfig4 for {}", interfaceName);
                                        logger.debug("Is new State DHCP? {}", netConfig4.isDhcp());
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating NetConfig4 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof NetConfig6) {
                                if (netConfig6 == null) {
                                    logger.debug("removing NetConfig6 for {}", interfaceName);
                                } else {
                                    hadNetConfig6 = true;
                                    newNetConfigs.add(netConfig6);
                                    if (!netConfig.equals(netConfig6)) {
                                        logger.debug("updating NetConfig6 for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating NetConfig6 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof WifiConfig) {
                                if (wifiConfig == null) {
                                    logger.debug("removing wifiConfig for {}", interfaceName);
                                } else {
                                    // There should be one new WifiConfig, which indicates the selected mode
                                    // but there may be multiple current wifi configs, one for each mode (infra,
                                    // master, adhoc)
                                    // Check the one corresponding to the newly selected mode, and automatically the
                                    // others
                                    if (newWifiMode.equals(((WifiConfig) netConfig).getMode())) {
                                        hadWifiConfig = true;
                                        newNetConfigs.add(wifiConfig);
                                        logger.debug("checking WifiConfig for {} mode", wifiConfig.getMode());
                                        if (!netConfig.equals(wifiConfig)) {
                                            logger.debug("updating WifiConfig for {}", interfaceName);
                                            configurationChanged = true;
                                            if (!modifiedInterfaceNames.contains(interfaceName)) {
                                                modifiedInterfaceNames.add(interfaceName);
                                            }
                                        } else {
                                            logger.debug("not updating WifiConfig for {} because it is unchanged",
                                                    interfaceName);
                                        }
                                    } else {
                                        // Keep the old WifiConfig for the non-selected wifi modes
                                        logger.debug("adding other WifiConfig: {}", netConfig);
                                        newNetConfigs.add(netConfig);
                                    }
                                }
                            } else if (netConfig instanceof DhcpServerConfigIP4) {
                                if (dhcpServerConfigIP4 == null) {
                                    logger.debug("removing DhcpServerConfigIP4 for {}", interfaceName);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                } else {
                                    hadDhcpServerConfigIP4 = true;
                                    newNetConfigs.add(dhcpServerConfigIP4);
                                    if (!netConfig.equals(dhcpServerConfigIP4)) {
                                        logger.debug("updating DhcpServerConfigIP4 for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating DhcpServerConfigIP4 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof FirewallAutoNatConfig) {
                                if (natConfig == null) {
                                    logger.debug("removing FirewallAutoNatConfig for {}", interfaceName);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                } else {
                                    hadNatConfig = true;
                                    newNetConfigs.add(natConfig);
                                    if (!netConfig.equals(natConfig)) {
                                        logger.debug("updating FirewallAutoNatConfig for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating FirewallNatConfig for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else {
                                logger.debug("Found unsupported configuration: {}", netConfig.toString());
                            }
                        }

                        // add configs that did not match any in the current configuration
                        if (netConfigs != null) {
                            for (NetConfig netConfig : netConfigs) {
                                if (netConfig instanceof NetConfigIP4 && !hadNetConfig4) {
                                    logger.debug("adding new NetConfig4 to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof NetConfigIP6 && !hadNetConfig6) {
                                    logger.debug("adding new NetConfig6 to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof WifiConfig && !hadWifiConfig) {
                                    logger.debug("adding new WifiConfig to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof DhcpServerConfigIP4 && !hadDhcpServerConfigIP4) {
                                    logger.debug("adding new DhcpServerConfigIP4 to existing config for {}",
                                            interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof FirewallAutoNatConfig && !hadNatConfig) {
                                    logger.debug("adding new FirewallAutoNatConfig to existing config for {}",
                                            interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                            }
                        }

                        // Update the wifi mode
                        if (newWifiMode != null) {
                            logger.debug("setting address config wifiMode to: {}", newWifiMode);
                            ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setMode(newWifiMode);
                        }

                        // replace with new list
                        for (NetConfig netConfig : newNetConfigs) {
                            logger.debug("Current NetConfig: {} :: {}", netConfig.getClass(), netConfig);
                        }
                        ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(newNetConfigs);
                    }
                }
            }

            if (configurationChanged) {
                submitNetworkConfiguration(modifiedInterfaceNames, newNetworkConfiguration);
            }
        } catch (UnknownHostException e) {
            logger.warn("Exception while updating WifiInterfaceConfig", e);
        }
    }

    @SuppressWarnings("checkstyle:methodLength")
    @Override
    public void updateModemInterfaceConfig(String interfaceName, String serialNum, String modemId, int pppNumber,
            boolean autoConnect, int mtu, List<NetConfig> netConfigs) throws KuraException {

        NetConfigIP4 netConfig4 = null;
        NetConfigIP6 netConfig6 = null;
        ModemConfig modemConfig = null;
        boolean hadNetConfig4 = false;
        boolean hadNetConfig6 = false;
        boolean hadModemConfig = false;

        if (netConfigs != null) {
            for (NetConfig netConfig : netConfigs) {
                if (!netConfig.isValid()) {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                            "NetConfig Configuration is invalid: " + netConfig.toString());
                }
                if (netConfig instanceof NetConfigIP4) {
                    netConfig4 = (NetConfigIP4) netConfig;
                } else if (netConfig instanceof NetConfigIP6) {
                    netConfig6 = (NetConfigIP6) netConfig;
                } else if (netConfig instanceof ModemConfig) {
                    modemConfig = (ModemConfig) netConfig;
                }
            }
        }

        // validation
        if (netConfig4 == null && netConfig6 == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
                    "Either IPv4 or IPv6 configuration must be defined");
        }
        if (modemConfig == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_REQUIRED_ATTRIBUTE_MISSING,
                    "Modem configuration must be defined");
        }

        List<String> modifiedInterfaceNames = new ArrayList<>();
        boolean configurationChanged = false;

        ComponentConfiguration originalNetworkComponentConfiguration = ((SelfConfiguringComponent) this.networkConfigurationService)
                .getConfiguration();
        if (originalNetworkComponentConfiguration == null) {
            return;
        }
        try {
            NetworkConfiguration newNetworkConfiguration = new NetworkConfiguration(
                    originalNetworkComponentConfiguration.getConfigurationProperties());
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = newNetworkConfiguration
                    .getNetInterfaceConfigs();
            for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                if (netInterfaceConfig.getName().equals(interfaceName)) {
                    // handle MTU
                    if (mtu != netInterfaceConfig.getMTU()) {
                        AbstractNetInterface<?> absNetInterfaceConfig = (AbstractNetInterface<?>) netInterfaceConfig;
                        logger.debug("updating MTU for {}", interfaceName);
                        absNetInterfaceConfig.setMTU(mtu);
                        configurationChanged = true;
                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                            modifiedInterfaceNames.add(interfaceName);
                        }
                    }

                    if (netInterfaceConfig instanceof ModemInterfaceConfigImpl) {
                        ModemInterfaceConfigImpl modemInterfaceConfig = (ModemInterfaceConfigImpl) netInterfaceConfig;
                        if (modemId == null) {
                            modemId = "";
                        }

                        // handle modem id
                        if (!modemId.equals(modemInterfaceConfig.getModemIdentifier())) {
                            logger.debug("updating Modem identifier: {}", modemId);
                            modemInterfaceConfig.setModemIdentifier(modemId);
                            configurationChanged = true;
                            if (!modifiedInterfaceNames.contains(interfaceName)) {
                                modifiedInterfaceNames.add(interfaceName);
                            }
                        }

                        // handle ppp num
                        if (pppNumber != modemInterfaceConfig.getPppNum()) {
                            logger.debug("updating PPP number: {}", pppNumber);
                            modemInterfaceConfig.setPppNum(pppNumber);
                            configurationChanged = true;
                            if (!modifiedInterfaceNames.contains(interfaceName)) {
                                modifiedInterfaceNames.add(interfaceName);
                            }
                        }
                    }

                    // replace existing configs
                    NetInterfaceAddressConfig netInterfaceAddressConfig = netInterfaceConfig.getNetInterfaceAddresses()
                            .get(0);
                    if (netInterfaceAddressConfig != null) {
                        List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
                        List<NetConfig> newNetConfigs = new ArrayList<>();
                        for (NetConfig netConfig : existingNetConfigs) {
                            logger.debug("looking at existing NetConfig for {} with value: {}", interfaceName,
                                    netConfig);
                            if (netConfig instanceof NetConfigIP4) {
                                if (netConfig4 == null) {
                                    logger.debug("removing NetConfig4 for {}", interfaceName);
                                } else {
                                    hadNetConfig4 = true;
                                    newNetConfigs.add(netConfig4);
                                    if (!netConfig.equals(netConfig4)) {
                                        logger.debug("updating NetConfig4 for {}", interfaceName);
                                        logger.debug("Is new State DHCP? {}", netConfig4.isDhcp());
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating NetConfig4 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof NetConfig6) {
                                if (netConfig6 == null) {
                                    logger.debug("removing NetConfig6 for {}", interfaceName);
                                } else {
                                    hadNetConfig6 = true;
                                    newNetConfigs.add(netConfig6);
                                    if (!netConfig.equals(netConfig6)) {
                                        logger.debug("updating NetConfig6 for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating NetConfig6 for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else if (netConfig instanceof ModemConfig) {
                                if (modemConfig == null) {
                                    logger.debug("removing ModemConfig for {}", interfaceName);
                                } else {
                                    hadModemConfig = true;
                                    newNetConfigs.add(modemConfig);
                                    if (!netConfig.equals(modemConfig)) {
                                        logger.debug("updating ModemConfig for {}", interfaceName);
                                        configurationChanged = true;
                                        if (!modifiedInterfaceNames.contains(interfaceName)) {
                                            modifiedInterfaceNames.add(interfaceName);
                                        }
                                    } else {
                                        logger.debug("not updating ModemConfig for {} because it is unchanged",
                                                interfaceName);
                                    }
                                }
                            } else {
                                logger.debug("Found unsupported configuration: {}", netConfig.toString());
                            }
                        }

                        // add configs that did not match any in the current configuration
                        if (netConfigs != null) {
                            for (NetConfig netConfig : netConfigs) {
                                if (netConfig instanceof NetConfigIP4 && !hadNetConfig4) {
                                    logger.debug("adding new NetConfig4 to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof NetConfigIP6 && !hadNetConfig6) {
                                    logger.debug("adding new NetConfig6 to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                                if (netConfig instanceof ModemConfig && !hadModemConfig) {
                                    logger.debug("adding new ModemConfig to existing config for {}", interfaceName);
                                    newNetConfigs.add(netConfig);
                                    configurationChanged = true;
                                    if (!modifiedInterfaceNames.contains(interfaceName)) {
                                        modifiedInterfaceNames.add(interfaceName);
                                    }
                                }
                            }
                        }

                        for (NetConfig netConfig : newNetConfigs) {
                            logger.debug("Current NetConfig: {} :: {}", netConfig.getClass(), netConfig);
                        }

                        // replace with new list
                        ((ModemInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(newNetConfigs);
                    }
                }

                newNetworkConfiguration.addNetInterfaceConfig(netInterfaceConfig);
            }

            if (configurationChanged) {
                submitNetworkConfiguration(modifiedInterfaceNames, newNetworkConfiguration);
            }
        } catch (UnknownHostException e) {
            logger.warn("Exception while updating ModemInterfaceConfig", e);
        }
    }

    @Override
    public void enableInterface(String interfaceName, boolean dhcp) throws KuraException {
        try {
            NetInterfaceType type = this.linuxNetworkUtil.getType(interfaceName);

            NetInterfaceStatus status = NetInterfaceStatus.netIPv4StatusDisabled;
            WifiMode wifiMode = WifiMode.MASTER;
            WifiConfig wifiConfig = null;
            WifiInterfaceState wifiInterfaceState = null;
            if (type == NetInterfaceType.WIFI) {
                List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> wifiNetInterfaceConfigs = getWifiInterfaceConfigs();

                List<? extends NetInterfaceAddressConfig> wifiNetInterfaceAddressConfigs = getWifiNetInterfaceAddressConfigs(
                        interfaceName, wifiNetInterfaceConfigs);

                WifiInterfaceAddressConfig wifiInterfaceAddressConfig = getWifiAddressConfig(
                        wifiNetInterfaceAddressConfigs);

                wifiMode = wifiInterfaceAddressConfig.getMode();
                boolean isL2Only = false;
                for (NetConfig netConfig : wifiInterfaceAddressConfig.getConfigs()) {
                    if (netConfig instanceof NetConfigIP4) {
                        status = ((NetConfigIP4) netConfig).getStatus();
                        isL2Only = ((NetConfigIP4) netConfig).getStatus() == NetInterfaceStatus.netIPv4StatusL2Only;
                        logger.debug("Interface status is set to {}", status);
                    } else if (netConfig instanceof WifiConfig && ((WifiConfig) netConfig).getMode() == wifiMode) {
                        wifiConfig = (WifiConfig) netConfig;
                    }
                }
                InterfaceStateBuilder builder = new InterfaceStateBuilder(this.executorService);
                builder.setInterfaceName(interfaceName);
                builder.setWifiMode(wifiMode);
                builder.setL2OnlyInterface(isL2Only);
                wifiInterfaceState = builder.buildWifiInterfaceState();
            }

            if (!this.linuxNetworkUtil.hasAddress(interfaceName)
                    || type == NetInterfaceType.WIFI && wifiInterfaceState != null && !wifiInterfaceState.isLinkUp()) {

                logger.info("bringing interface {} up", interfaceName);

                if (type == NetInterfaceType.WIFI) {
                    enableWifiInterface(interfaceName, status, wifiMode, wifiConfig);
                }
                if (dhcp) {
                    renewDhcpLease(interfaceName);
                } else {
                    this.linuxNetworkUtil.enableInterface(interfaceName);
                }

                // if it isn't up - at least make sure the Ethernet controller is powered on
                if (!this.linuxNetworkUtil.hasAddress(interfaceName)) {
                    this.linuxNetworkUtil.bringUpDeletingAddress(interfaceName);
                }
            } else {
                logger.info("not bringing interface {} up because it is already up", interfaceName);
                if (dhcp) {
                    renewDhcpLease(interfaceName);
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    private WifiInterfaceAddressConfig getWifiAddressConfig(
            List<? extends NetInterfaceAddressConfig> wifiNetInterfaceAddressConfigs) {
        for (NetInterfaceAddressConfig wifiNetInterfaceAddressConfig : wifiNetInterfaceAddressConfigs) {
            if (wifiNetInterfaceAddressConfig instanceof WifiInterfaceAddressConfig) {
                return (WifiInterfaceAddressConfig) wifiNetInterfaceAddressConfig;
            }
        }
        throw new IllegalArgumentException("Wrong configuration for a wifi interface");
    }

    private List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> getWifiInterfaceConfigs()
            throws KuraException {
        List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = getNetworkInterfaceConfigs();

        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> wifiNetInterfaceConfigs = new ArrayList<>();
        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig.getType() == NetInterfaceType.WIFI) {
                wifiNetInterfaceConfigs.add(netInterfaceConfig);
            }
        }

        return wifiNetInterfaceConfigs;
    }

    private List<? extends NetInterfaceAddressConfig> getWifiNetInterfaceAddressConfigs(String interfaceName,
            List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> wifiNetInterfaceConfigs) {
        List<? extends NetInterfaceAddressConfig> wifiNetInterfaceAddresses = null;

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> wifiNetInterfaceConfig : wifiNetInterfaceConfigs) {
            if (wifiNetInterfaceConfig.getName().equals(interfaceName)) {
                wifiNetInterfaceAddresses = wifiNetInterfaceConfig.getNetInterfaceAddresses();
                break;
            }
        }

        return wifiNetInterfaceAddresses;
    }

    @Override
    public void disableInterface(String interfaceName) throws KuraException {
        if ("lo".equals(interfaceName)) {
            return;
        }
        manageDhcpClient(interfaceName, false);
        manageDhcpServer(interfaceName, false);

        NetInterfaceType type = this.linuxNetworkUtil.getType(interfaceName);
        if (type == NetInterfaceType.WIFI) {
            disableWifiInterface(interfaceName);
        }
        try {
            if (this.linuxNetworkUtil.hasAddress(interfaceName)) {
                logger.info("bringing interface {} down", interfaceName);
                this.linuxNetworkUtil.disableInterface(interfaceName);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public void manageDhcpClient(String interfaceName, boolean enable) throws KuraException {
        this.dhcpClientManager.disable(interfaceName);
        if (enable) {
            renewDhcpLease(interfaceName);
        }
    }

    @Override
    public void manageDhcpServer(String interfaceName, boolean enable) throws KuraException {
        if (enable && !this.dhcpServerManager.isRunning(interfaceName)) {
            logger.debug("Starting DHCP server for {}", interfaceName);
            this.dhcpServerManager.enable(interfaceName);
        } else if (!enable && this.dhcpServerManager.isRunning(interfaceName)) {
            logger.debug("Stopping DHCP server for {}", interfaceName);
            this.dhcpServerManager.disable(interfaceName);
        }
    }

    @Override
    public void renewDhcpLease(String interfaceName) throws KuraException {
        this.dhcpClientManager.releaseCurrentLease(interfaceName);
        this.dhcpClientManager.enable(interfaceName);
    }

    @Override
    public void manageFirewall(String gatewayIface) throws KuraException {
        // get desired NAT rules interfaces
        LinkedHashSet<NATRule> desiredNatRules = null;
        ComponentConfiguration networkComponentConfiguration = ((SelfConfiguringComponent) this.networkConfigurationService)
                .getConfiguration();
        if (gatewayIface != null && networkComponentConfiguration != null) {
            try {
                NetworkConfiguration netConfiguration = new NetworkConfiguration(
                        networkComponentConfiguration.getConfigurationProperties());
                List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = netConfiguration
                        .getNetInterfaceConfigs();
                for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
                    String ifaceName = netInterfaceConfig.getName();
                    List<? extends NetInterfaceAddressConfig> netInterfaceAddressConfigs = netInterfaceConfig
                            .getNetInterfaceAddresses();
                    if (netInterfaceAddressConfigs != null && !netInterfaceAddressConfigs.isEmpty()) {
                        for (NetInterfaceAddressConfig netInterfaceAddressConfig : netInterfaceAddressConfigs) {
                            List<NetConfig> existingNetConfigs = netInterfaceAddressConfig.getConfigs();
                            if (existingNetConfigs != null && !existingNetConfigs.isEmpty()) {
                                for (NetConfig netConfig : existingNetConfigs) {
                                    if (netConfig instanceof FirewallAutoNatConfig) {
                                        if (desiredNatRules == null) {
                                            desiredNatRules = new LinkedHashSet<>();
                                        }
                                        desiredNatRules
                                                .add(new NATRule(ifaceName, gatewayIface, true, RuleType.GENERIC));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (UnknownHostException e) {
                logger.warn("Exception while updating firewall configuration", e);
            }
        }

        LinuxFirewall firewall = LinuxFirewall.getInstance(this.executorService);
        if (desiredNatRules != null) {
            firewall.replaceAllNatRules(desiredNatRules);
        } else {
            firewall.deleteAllAutoNatRules();
        }

        firewall.enable();
    }

    @Override
    public List<NetConfig> getFirewallConfiguration() throws KuraException {
        logger.debug("getting the firewall configuration");
        return this.firewallConfigurationService.getFirewallConfiguration().getConfigs();
    }

    @Override
    public void setFirewallOpenPortConfiguration(
            List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException {
        this.firewallConfigurationService.setFirewallOpenPortConfiguration(firewallConfiguration);
        this.configurationService.snapshot();
    }

    @Override
    public void setFirewallPortForwardingConfiguration(
            List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration) throws KuraException {
        this.firewallConfigurationService.setFirewallPortForwardingConfiguration(firewallConfiguration);
        this.configurationService.snapshot();
    }

    @Override
    public void setFirewallNatConfiguration(List<FirewallNatConfig> natConfigs) throws KuraException {
        this.firewallConfigurationService.setFirewallNatConfiguration(natConfigs);
        this.configurationService.snapshot();
    }

    @Override
    public List<WifiHotspotInfo> getWifiHotspotList(String ifaceName) throws KuraException {
        List<WifiHotspotInfo> wifiHotspotInfoList = new ArrayList<>();
        WifiMode wifiMode = getWifiMode(ifaceName);
        try {
            if (wifiMode == WifiMode.MASTER) {
                ifaceName = switchToDedicatedApInterface(ifaceName);
                startTemporaryWpaSupplicant(ifaceName);
            }

            logger.info("getWifiHotspots() :: scanning for available access points ...");

            List<WifiAccessPoint> wifiAccessPoints = getWifiAccessPoints(ifaceName);
            for (WifiAccessPoint wap : wifiAccessPoints) {
                int frequency = (int) wap.getFrequency();
                int channel = wap.getChannel();

                if (wap.getSSID() == null || wap.getSSID().length() == 0
                        || isHotspotInList(channel, wap.getSSID(), wifiHotspotInfoList)) {
                    logger.debug("Skipping hidden SSID");
                    continue;
                }

                logger.trace("getWifiHotspots() :: SSID={}", wap.getSSID());
                logger.trace("getWifiHotspots() :: Signal={}", wap.getStrength());
                logger.trace("getWifiHotspots() :: Frequency={}", wap.getFrequency());

                String macAddress = getMacAddress(wap.getHardwareAddress());
                WifiSecurity wifiSecurity = getWifiSecurity(wap);
                WifiHotspotInfo wifiHotspotInfo = new WifiHotspotInfo(wap.getSSID(), macAddress, 0 - wap.getStrength(),
                        channel, frequency, wifiSecurity);
                setCiphers(wifiHotspotInfo, wap, wifiSecurity);
                wifiHotspotInfoList.add(wifiHotspotInfo);
            }

            if (wifiMode == WifiMode.MASTER) {
                stopTemporaryWpaSupplicant(ifaceName);
                stopDedicatedApInterface(ifaceName);
            }
        } catch (Throwable t) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, t, "scan operation has failed");
        }

        return wifiHotspotInfoList;
    }

    private String switchToDedicatedApInterface(String ifaceName) throws KuraException {
        String dedicatedApInterface = ifaceName + LinuxNetworkUtil.ACCESS_POINT_INTERFACE_SUFFIX;
        try {
            LinuxIfconfig dedicatedApInterfaceConfig = this.linuxNetworkUtil
                    .getInterfaceConfiguration(dedicatedApInterface);

            if (dedicatedApInterfaceConfig != null) {
                if (!dedicatedApInterfaceConfig.isLinkUp()) {
                    this.linuxNetworkUtil.setNetworkInterfaceLinkUp(dedicatedApInterface);
                }
            } else {
                this.linuxNetworkUtil.createApNetworkInterface(ifaceName, dedicatedApInterface);
                this.linuxNetworkUtil.setNetworkInterfaceMacAddress(dedicatedApInterface);
                this.linuxNetworkUtil.setNetworkInterfaceLinkUp(dedicatedApInterface);
            }

            return dedicatedApInterface;

        } catch (KuraException e) {
            logger.error("Unable to switch to reserved AP interface {}, falling back to the default.",
                    dedicatedApInterface, e);
        }

        return ifaceName;
    }

    private void stopDedicatedApInterface(String ifaceName) throws KuraException {
        this.linuxNetworkUtil.setNetworkInterfaceLinkDown(ifaceName);
    }

    @Override
    public List<WifiChannel> getWifiFrequencies(String ifaceName) throws KuraException {
        return IwCapabilityTool.probeChannels(ifaceName, this.executorService);
    }

    @Override
    public String getWifiCountryCode() throws KuraException {
        return IwCapabilityTool.getWifiCountryCode(this.executorService);
    }

    @Override
    public synchronized boolean verifyWifiCredentials(String ifaceName, WifiConfig wifiConfig, int tout) {

        if (this.wifiClientMonitorServiceLock == null) {
            return false;
        }

        // hack to synchronize with WifiClientMonitorService
        synchronized (this.wifiClientMonitorServiceLock) {
            boolean ret = false;
            WpaSupplicantConfigWriter wpaSupplicantConfigWriter = this.wpaSupplicantConfigWriterFactory.getInstance();
            wpaSupplicantConfigWriter.setExecutorService(executorService);
            try {
                // Kill dhcp, hostapd and wpa_supplicant if running
                manageDhcpClient(ifaceName, false);
                manageDhcpServer(ifaceName, false);
                disableWifiInterface(ifaceName);

                wpaSupplicantConfigWriter.generateTempWpaSupplicantConf(wifiConfig);

                logger.debug("verifyWifiCredentials() :: Starting temporary instance of wpa_supplicant");
                this.wpaSupplicantManager.startTemp(ifaceName, wifiConfig.getDriver());
                wifiModeWait(ifaceName, WifiMode.INFRA, 10);
                ret = isWifiConnectionCompleted(ifaceName, tout);

                // Disable wifi interface again, previous configuration will be restored by WifiMonitorService
                disableWifiInterface(ifaceName);
            } catch (Exception e) {
                logger.warn("Exception while managing the temporary instance of the Wpa supplicant.", e);
            } finally {
                wpaSupplicantConfigWriter.setExecutorService(null);
            }
            return ret;
        }
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("handleEvent - topic: {}", event.getTopic());
        String topic = event.getTopic();
        if (topic.equals(NetworkConfigurationChangeEvent.NETWORK_EVENT_CONFIG_CHANGE_TOPIC)) {
            this.pendingNetworkConfigurationChange = false;
        }
    }

    private boolean isWifiConnectionCompleted(String ifaceName, int tout) throws KuraException {

        boolean ret = false;
        long start = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            WpaSupplicantStatus wpaSupplicantStatus = new WpaSupplicantStatus(ifaceName, this.executorService);
            String wpaState = wpaSupplicantStatus.getWpaState();
            if (wpaState != null && "COMPLETED".equals(wpaState)) {
                ret = true;
                break;
            }
        } while (System.currentTimeMillis() - start < tout * 1000);

        return ret;
    }

    private void wifiModeWait(String ifaceName, WifiMode mode, int tout) {
        long startTimer = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                if (this.linuxNetworkUtil.getWifiMode(ifaceName) == mode) {
                    break;
                }
            } catch (KuraException e) {
                logger.error("wifiModeWait() :: Failed to obtain WiFi mode ", e);
            }
        } while (System.currentTimeMillis() - startTimer < 1000L * tout);
    }

    // ----------------------------------------------------------------
    //
    // Private Methods
    //
    // ----------------------------------------------------------------

    // FIXME: simplify method signature. Probably we could take the mode from the wifiConfig.
    private void enableWifiInterface(String ifaceName, NetInterfaceStatus status, WifiMode wifiMode,
            WifiConfig wifiConfig) throws KuraException {
        // ignore mon.* interface
        // ignore redpine vlan interface
        if (ifaceName.startsWith("mon.") || ifaceName.startsWith("rpine")) {
            return;
        }

        logger.debug("Configuring {} for {} mode", ifaceName, wifiMode);

        logger.debug("Stopping hostapd and wpa_supplicant");
        this.hostapdManager.stop(ifaceName);
        this.wpaSupplicantManager.stop(ifaceName);

        boolean enStatusAp = status == NetInterfaceStatus.netIPv4StatusL2Only
                || status == NetInterfaceStatus.netIPv4StatusEnabledLAN ? true : false;
        boolean enStatusInfra = status == NetInterfaceStatus.netIPv4StatusL2Only
                || status == NetInterfaceStatus.netIPv4StatusEnabledLAN
                || status == NetInterfaceStatus.netIPv4StatusEnabledWAN ? true : false;
        if (enStatusAp && wifiMode == WifiMode.MASTER) {
            logger.debug("Starting hostapd");
            this.hostapdManager.start(ifaceName);
        } else if (enStatusInfra && (wifiMode == WifiMode.INFRA || wifiMode == WifiMode.ADHOC)) {
            if (wifiConfig != null) {
                logger.debug("Starting wpa_supplicant");
                logger.warn("enableWifiInterface() :: Starting wpa_supplicant ... driver={}", wifiConfig.getDriver());
                this.wpaSupplicantManager.start(ifaceName, wifiConfig.getDriver());
                if (isWifiConnectionCompleted(ifaceName, 60)) {
                    logger.debug("WiFi Connection Completed on {} !", ifaceName);
                } else {
                    logger.warn("Failed to complete WiFi Connection on {}", ifaceName);
                }
            } else {
                logger.warn("No WifiConfig configured for mode {}", wifiMode);
            }
        } else {
            logger.debug("Invalid wifi configuration - NetInterfaceStatus: {}, WifiMode:{}", status, wifiMode);
        }
    }

    private void disableWifiInterface(String ifaceName) throws KuraException {
        logger.debug("Stopping hostapd and wpa_supplicant");
        this.hostapdManager.stop(ifaceName);
        this.wpaSupplicantManager.stop(ifaceName);
    }

    // Submit new configuration, waiting for network configuration change event before returning
    private void submitNetworkConfiguration(List<String> modifiedInterfaceNames,
            NetworkConfiguration networkConfiguration) throws KuraException {
        short timeout = 30000; // in milliseconds
        final short sleep = 500;

        this.pendingNetworkConfigurationChange = true;
        if (modifiedInterfaceNames != null && !modifiedInterfaceNames.isEmpty()) {
            networkConfiguration.setModifiedInterfaceNames(modifiedInterfaceNames);
            logger.debug("Set modified interface names: {}", modifiedInterfaceNames.toString());
        }
        this.networkConfigurationService.setNetworkConfiguration(networkConfiguration);
        this.configurationService.snapshot();

        while (this.pendingNetworkConfigurationChange && timeout > 0) {
            timeout -= sleep;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (this.pendingNetworkConfigurationChange) {
            logger.warn("Did not receive a network configuration change event");
            this.pendingNetworkConfigurationChange = false;
        }
    }

    private void stopTemporaryWpaSupplicant(String ifaceName) throws KuraException {
        if (this.wpaSupplicantManager.isTempRunning()) {
            logger.debug("getWifiHotspots() :: stoping temporary instance of wpa_supplicant");
            this.wpaSupplicantManager.stop(ifaceName);
        }
        reloadKernelModule(ifaceName, WifiMode.MASTER);
    }

    private void startTemporaryWpaSupplicant(String ifaceName) throws KuraException {
        reloadKernelModule(ifaceName, WifiMode.INFRA);
        WpaSupplicantConfigWriter wpaSupplicantConfigWriter = this.wpaSupplicantConfigWriterFactory.getInstance();
        wpaSupplicantConfigWriter.setExecutorService(this.executorService);
        wpaSupplicantConfigWriter.generateTempWpaSupplicantConf();

        logger.debug("getWifiHotspots() :: Starting temporary instance of wpa_supplicant");
        StringBuilder key = new StringBuilder("net.interface.").append(ifaceName).append(".config.wifi.infra.driver");
        String driver = (String) this.networkConfigurationService.getNetworkConfiguration().getConfigurationProperties()
                .get(key.toString());

        this.wpaSupplicantManager.startTemp(ifaceName, driver);
        wifiModeWait(ifaceName, WifiMode.INFRA, 10);
    }

    private String getMacAddress(byte[] baMacAddress) {
        StringBuilder sbMacAddress = new StringBuilder();
        for (int i = 0; i < baMacAddress.length; i++) {
            sbMacAddress.append(String.format("%02x", baMacAddress[i] & 0x0ff).toUpperCase());
            if (i < baMacAddress.length - 1) {
                sbMacAddress.append(':');
            }
        }
        return sbMacAddress.toString();
    }

    private void reloadKernelModule(String interfaceName, WifiMode wifiMode) throws KuraException {
        if (this.wifiDriverService == null) {
            return;
        }
        if (!this.wifiDriverService.isKernelModuleLoadedForMode(interfaceName, wifiMode)) {
            logger.info("reloadKernelModule() :: reload {} using kernel module for WiFi mode {}", interfaceName,
                    wifiMode);
            this.wifiDriverService.unloadKernelModule(interfaceName);
            this.wifiDriverService.loadKernelModule(interfaceName, wifiMode);
        }
    }

    private boolean isHotspotInList(int channel, String ssid, List<WifiHotspotInfo> wifiHotspotInfoList) {
        boolean found = false;
        for (WifiHotspotInfo whi : wifiHotspotInfoList) {
            if (ssid.equals(whi.getSsid()) && channel == whi.getChannel()) {
                found = true;
                break;
            }
        }
        return found;
    }

    private WifiMode getWifiMode(String ifaceName) throws KuraException {
        WifiMode wifiMode = WifiMode.UNKNOWN;
        List<? extends NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = getNetworkInterfaceConfigs();
        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            if (netInterfaceConfig.getName().equals(ifaceName)) {
                List<? extends NetInterfaceAddressConfig> netInterfaceAddresses = netInterfaceConfig
                        .getNetInterfaceAddresses();
                if (netInterfaceAddresses != null) {
                    wifiMode = getWifiMode(netInterfaceAddresses);
                }
                break;
            }
        }
        return wifiMode;
    }

    private WifiMode getWifiMode(List<? extends NetInterfaceAddressConfig> netInterfaceAddresses) {
        WifiMode wifiMode = WifiMode.UNKNOWN;
        for (NetInterfaceAddressConfig netInterfaceAddress : netInterfaceAddresses) {
            if (netInterfaceAddress instanceof WifiInterfaceAddressConfig) {
                wifiMode = ((WifiInterfaceAddressConfig) netInterfaceAddress).getMode();
                break;
            }
        }
        return wifiMode;
    }

    private WifiSecurity getWifiSecurity(WifiAccessPoint wap) {
        WifiSecurity wifiSecurity = WifiSecurity.NONE;

        EnumSet<WifiSecurity> esWpaSecurity = wap.getWpaSecurity();
        if (esWpaSecurity != null && !esWpaSecurity.isEmpty()) {
            wifiSecurity = WifiSecurity.SECURITY_WPA;

            logger.trace("getWifiHotspots() :: WPA Security={}", esWpaSecurity);
        }

        EnumSet<WifiSecurity> esRsnSecurity = wap.getRsnSecurity();
        if (esRsnSecurity != null && !esRsnSecurity.isEmpty()) {
            if (wifiSecurity == WifiSecurity.SECURITY_WPA) {
                wifiSecurity = WifiSecurity.SECURITY_WPA_WPA2;
            } else {
                wifiSecurity = WifiSecurity.SECURITY_WPA2;
            }
            logger.trace("getWifiHotspots() :: RSN Security={}", esRsnSecurity);
        }

        if (wifiSecurity == WifiSecurity.NONE) {
            List<String> capabilities = wap.getCapabilities();
            if (capabilities != null && !capabilities.isEmpty() && capabilities.contains("Privacy")) {
                wifiSecurity = WifiSecurity.SECURITY_WEP;
            }
        }

        return wifiSecurity;
    }

    private void setCiphers(WifiHotspotInfo wifiHotspotInfo, WifiAccessPoint wap, WifiSecurity wifiSecurity) {
        EnumSet<WifiSecurity> esSecurity = null;
        EnumSet<WifiSecurity> pairCiphers = EnumSet.noneOf(WifiSecurity.class);
        EnumSet<WifiSecurity> groupCiphers = EnumSet.noneOf(WifiSecurity.class);
        if (wifiSecurity == WifiSecurity.SECURITY_WPA_WPA2) {
            esSecurity = wap.getWpaSecurity();
            esSecurity.addAll(wap.getRsnSecurity());
        } else if (wifiSecurity == WifiSecurity.SECURITY_WPA) {
            esSecurity = wap.getWpaSecurity();
        } else if (wifiSecurity == WifiSecurity.SECURITY_WPA2) {
            esSecurity = wap.getRsnSecurity();
        }
        if (esSecurity != null) {
            getCiphers(esSecurity, pairCiphers, groupCiphers);
        }
        wifiHotspotInfo.setGroupCiphers(groupCiphers);
        wifiHotspotInfo.setPairCiphers(pairCiphers);
    }

    private void getCiphers(EnumSet<WifiSecurity> esSecurity, EnumSet<WifiSecurity> pairCiphers,
            EnumSet<WifiSecurity> groupCiphers) {

        for (WifiSecurity securityEntry : esSecurity) {
            if (securityEntry == WifiSecurity.PAIR_CCMP || securityEntry == WifiSecurity.PAIR_TKIP) {
                pairCiphers.add(securityEntry);
            } else if (securityEntry == WifiSecurity.GROUP_CCMP || securityEntry == WifiSecurity.GROUP_TKIP) {
                groupCiphers.add(securityEntry);
            }
        }
    }

    @Override
    public boolean isWifiDFS(String ifaceName) throws KuraException {
        return IwCapabilityTool.probeCapabilities(ifaceName, executorService).contains(Capability.DFS);
    }

    @Override
    public boolean isWifiIEEE80211AC(String ifaceName) throws KuraException {
        return IwCapabilityTool.probeCapabilities(ifaceName, executorService).contains(Capability.VHT);
    }

    @Override
    public List<DhcpLease> getDhcpLeases() {
        return DhcpLeaseTool.probeLeases(this.executorService);
    }
}
