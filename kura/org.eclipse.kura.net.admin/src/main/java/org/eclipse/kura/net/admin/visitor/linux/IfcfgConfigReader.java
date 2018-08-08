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
 *     Benjamin Cabé - fix for GH issue #299
 *******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.NetworkConfigurationVisitor;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.internal.linux.net.NetInterfaceConfigSerializationService;
import org.eclipse.kura.linux.net.dhcp.DhcpClientLeases;
import org.eclipse.kura.linux.net.dns.LinuxDns;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.admin.visitor.linux.util.KuranetConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfcfgConfigReader implements NetworkConfigurationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(IfcfgConfigReader.class);

    private static final String ONBOOT_PROP_NAME = "ONBOOT";
    private static final String BOOTPROTO_PROP_NAME = "BOOTPROTO";
    private static final String IPADDR_PROP_NAME = "IPADDR";
    private static final String PREFIX_PROP_NAME = "PREFIX";
    private static final String GATEWAY_PROP_NAME = "GATEWAY";
    private static final String DEFROUTE_PROP_NAME = "DEFROUTE";
    private static final String NETMASK_PROP_NAME = "NETMASK";

    private static IfcfgConfigReader instance;

    private static NetInterfaceConfigSerializationService netConfigManager; // TODO: can be null

    public static IfcfgConfigReader getInstance() {
        if (instance == null) {
            instance = new IfcfgConfigReader();
            BundleContext context = FrameworkUtil.getBundle(IfcfgConfigWriter.class).getBundleContext();
            ServiceReference<NetInterfaceConfigSerializationService> netConfigManagerSR = context
                    .getServiceReference(NetInterfaceConfigSerializationService.class);
            netConfigManager = context.getService(netConfigManagerSR);
        }
        return instance;
    }

    @Override
    public void visit(NetworkConfiguration config) throws KuraException {
        List<NetInterfaceConfig<? extends NetInterfaceAddressConfig>> netInterfaceConfigs = config
                .getNetInterfaceConfigs();

        Properties kuraExtendedProps = getKuranetProperties();

        for (NetInterfaceConfig<? extends NetInterfaceAddressConfig> netInterfaceConfig : netInterfaceConfigs) {
            String interfaceName = netInterfaceConfig.getName();

            NetInterfaceType type = netInterfaceConfig.getType();
            if (type != NetInterfaceType.ETHERNET && type != NetInterfaceType.WIFI
                    && type != NetInterfaceType.LOOPBACK) {
                logger.info("The {} interface is not supported", interfaceName);
                continue;
            }

            Properties kuraProps = netConfigManager.read(interfaceName);

            IfaceConfig ifaceConfig = getIfaceConfig(interfaceName, kuraProps, kuraExtendedProps);

            NetInterfaceAddressConfig netInterfaceAddressConfig = netInterfaceConfig.getNetInterfaceAddresses().get(0);
            if (netInterfaceAddressConfig == null) {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "InterfaceAddressConfig list is null");
            }
            List<NetConfig> netConfigs = netInterfaceAddressConfig.getConfigs();
            if (netConfigs == null) {
                netConfigs = new ArrayList<>();
                setNetInterfaceAddressConfigs(interfaceName, ifaceConfig.isDhcp(), netConfigs,
                        netInterfaceAddressConfig);
            }
            NetConfigIP4 netConfig = new NetConfigIP4(ifaceConfig.getNetInterfaceStatus(), ifaceConfig.isAutoConnect());
            setNetConfigIP4(netConfig, ifaceConfig.isDhcp(), ifaceConfig.getAddress(), ifaceConfig.getGateway(),
                    ifaceConfig.getPrefixString(), ifaceConfig.getNetmask(), kuraProps);
            logger.debug("NetConfig: {}", netConfig);
            netConfigs.add(netConfig);
        }
    }

    private IfaceConfig getIfaceConfig(String ifaceName, Properties kuraProps, Properties kuraExtendedProps)
            throws KuraException {
        boolean autoConnect = false;
        boolean dhcp = false;
        IP4Address address = null;
        String prefixString = null;
        String netmask = null;
        String gateway = null;
        NetInterfaceStatus netInterfaceStatus = getNetInterfaceStatus(ifaceName, kuraExtendedProps);
        if (kuraProps != null) {
            autoConnect = "yes".equals(kuraProps.getProperty(ONBOOT_PROP_NAME));
            boolean defroute = "yes".equals(kuraProps.getProperty(DEFROUTE_PROP_NAME));

            String bootproto = kuraProps.getProperty(BOOTPROTO_PROP_NAME);
            if (bootproto == null) {
                bootproto = "static";
            }

            // correct the status if needed by validating against the actual properties
            netInterfaceStatus = getNetInterfaceStatus(netInterfaceStatus, autoConnect, defroute);

            // check for dhcp or static configuration
            gateway = kuraProps.getProperty(GATEWAY_PROP_NAME);
            logger.debug("got gateway for {}: {}", ifaceName, gateway);

            if ("dhcp".equals(bootproto)) {
                logger.debug(
                        "getIfaceConfig() :: Interface configuration mode for the {} interface is currently set for DHCP",
                        ifaceName);
                dhcp = true;
            } else if ("static".equals(bootproto)) {
                logger.debug(
                        "getIfaceConfig() :: Interface configuration mode for the {} interface is currently set for static IP address",
                        ifaceName);
                String ipAddress = kuraProps.getProperty(IPADDR_PROP_NAME);
                prefixString = kuraProps.getProperty(PREFIX_PROP_NAME);
                netmask = kuraProps.getProperty(NETMASK_PROP_NAME);
                if (autoConnect) {
                    // make sure at least prefix or netmask is present
                    address = parseIpAddress(ipAddress, netmask, prefixString);
                }
            }
        }
        return new IfaceConfig(netInterfaceStatus, autoConnect, dhcp, address, prefixString, netmask, gateway);
    }

    private void setNetInterfaceAddressConfigs(String interfaceName, boolean dhcp, List<NetConfig> netConfigs,
            NetInterfaceAddressConfig netInterfaceAddressConfig) {
        if (netInterfaceAddressConfig instanceof NetInterfaceAddressConfigImpl) {
            setNetInterfaceAddressConfigs(interfaceName, netConfigs, netInterfaceAddressConfig, dhcp);
        } else if (netInterfaceAddressConfig instanceof WifiInterfaceAddressConfigImpl) {
            setWifiNetInterfaceAddressConfigs(interfaceName, netConfigs, netInterfaceAddressConfig, dhcp);
        }
    }

    private void setNetInterfaceAddressConfigs(String interfaceName, List<NetConfig> netConfigs,
            NetInterfaceAddressConfig netInterfaceAddressConfig, boolean dhcp) {
        ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
        if (dhcp) {
            // obtain gateway provided by DHCP server
            List<? extends IPAddress> dhcpRouters = getDhcpRouters(interfaceName,
                    netInterfaceAddressConfig.getAddress());
            if (!dhcpRouters.isEmpty()) {
                ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setGateway(dhcpRouters.get(0));
            }
            // Replace with DNS provided by DHCP server (displayed as read-only in Denali)
            List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                    netInterfaceAddressConfig.getAddress());
            if (!dhcpDnsServers.isEmpty()) {
                ((NetInterfaceAddressConfigImpl) netInterfaceAddressConfig).setDnsServers(dhcpDnsServers);
            }
        }
    }

    private void setWifiNetInterfaceAddressConfigs(String interfaceName, List<NetConfig> netConfigs,
            NetInterfaceAddressConfig netInterfaceAddressConfig, boolean dhcp) {
        ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setNetConfigs(netConfigs);
        if (dhcp) {
            // obtain gateway provided by DHCP server
            List<? extends IPAddress> dhcpRouters = getDhcpRouters(interfaceName,
                    netInterfaceAddressConfig.getAddress());
            if (!dhcpRouters.isEmpty()) {
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setGateway(dhcpRouters.get(0));
            }
            // Replace with DNS provided by DHCP server (displayed as read-only in Denali)
            List<? extends IPAddress> dhcpDnsServers = getDhcpDnsServers(interfaceName,
                    netInterfaceAddressConfig.getAddress());
            if (!dhcpDnsServers.isEmpty()) {
                ((WifiInterfaceAddressConfigImpl) netInterfaceAddressConfig).setDnsServers(dhcpDnsServers);
            }
        }
    }

    private void setNetConfigIP4(NetConfigIP4 netConfig, boolean dhcp, IP4Address address, String gateway,
            String prefixString, String netmask, Properties kuraProps) throws KuraException {

        netConfig.setDhcp(dhcp);
        if (kuraProps == null) {
            return;
        }
        netConfig.setDnsServers(getDnsServers(kuraProps));
        if (!dhcp) {
            netConfig.setAddress(address);
            if (gateway != null && !gateway.isEmpty()) {
                try {
                    netConfig.setGateway((IP4Address) IPAddress.parseHostAddress(gateway));
                } catch (UnknownHostException e) {
                    logger.error("Could not parse address: " + gateway, e);
                }
            }
            if (prefixString != null) {
                short prefix = Short.parseShort(prefixString);
                netConfig.setNetworkPrefixLength(prefix);
            }
            if (netmask != null) {
                netConfig.setNetworkPrefixLength(NetworkUtil.getNetmaskShortForm(netmask));
            }
        }
    }

    private NetInterfaceStatus getNetInterfaceStatus(String ifaceName, Properties kuraExtendedProps) {
        NetInterfaceStatus netInterfaceStatus;
        StringBuilder sb = new StringBuilder().append("net.interface.").append(ifaceName).append(".config.ip4.status");
        if (kuraExtendedProps != null && kuraExtendedProps.getProperty(sb.toString()) != null) {
            netInterfaceStatus = NetInterfaceStatus.valueOf(kuraExtendedProps.getProperty(sb.toString()));
        } else {
            netInterfaceStatus = NetInterfaceStatus.netIPv4StatusDisabled;
        }
        logger.debug("Setting NetInterfaceStatus to {} for {}", netInterfaceStatus, ifaceName);
        return netInterfaceStatus;
    }

    private NetInterfaceStatus getNetInterfaceStatus(NetInterfaceStatus netInterfaceStatus, boolean autoConnect,
            boolean defroute) {

        NetInterfaceStatus ret = netInterfaceStatus;
        if (netInterfaceStatus == NetInterfaceStatus.netIPv4StatusDisabled && autoConnect) {
            if (!defroute) {
                ret = NetInterfaceStatus.netIPv4StatusEnabledLAN;
            } else {
                ret = NetInterfaceStatus.netIPv4StatusEnabledWAN;
            }
        }
        return ret;
    }

    private IP4Address parseIpAddress(String ipAddress, String netmask, String prefix) throws KuraException {
        if (netmask == null && prefix == null) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                    "malformatted network interface configuration file: must contain NETMASK and/or PREFIX");
        }
        IP4Address address = null;
        if (ipAddress != null && !ipAddress.isEmpty()) {
            try {
                address = (IP4Address) IPAddress.parseHostAddress(ipAddress);
            } catch (UnknownHostException e) {
                logger.warn("parseIpAddress() :: Error parsing address: {}", ipAddress, e);
            }
        }
        return address;
    }

    private List<? extends IPAddress> getDhcpRouters(String interfaceName, IPAddress address) {
        List<IPAddress> routers = null;
        if (address != null) {
            DhcpClientLeases dhcpClientLeases = DhcpClientLeases.getInstance();
            try {
                routers = dhcpClientLeases.getDhcpGateways(interfaceName, address);
            } catch (KuraException e) {
                logger.error("Error getting DHCP DNS servers", e);
            }
        }
        if (routers == null) {
            routers = new ArrayList<>();
        }
        return routers;
    }

    private List<? extends IPAddress> getDhcpDnsServers(String interfaceName, IPAddress address) {
        List<IPAddress> dnsServers = null;
        if (address != null) {
            LinuxDns linuxDns = LinuxDns.getInstance();
            try {
                dnsServers = linuxDns.getDhcpDnsServers(interfaceName, address);
            } catch (KuraException e) {
                logger.error("Error getting DHCP DNS servers", e);
            }
        }
        if (dnsServers == null) {
            dnsServers = new ArrayList<>();
        }
        return dnsServers;
    }

    private List<IP4Address> getDnsServers(Properties kuraProps) {
        List<IP4Address> dnsServers = new ArrayList<>();
        int count = 1;
        while (true) {
            String dns;
            if ((dns = kuraProps.getProperty("DNS" + count)) != null) {
                try {
                    dnsServers.add((IP4Address) IPAddress.parseHostAddress(dns));
                } catch (UnknownHostException e) {
                    logger.error("Could not parse address: {}", dns, e);
                }
                count++;
            } else {
                break;
            }
        }
        return dnsServers;
    }

    protected Properties getKuranetProperties() {
        return KuranetConfig.getProperties();
    }

    private class IfaceConfig {

        private NetInterfaceStatus netInterfaceStatus = null;
        private boolean autoConnect = false;
        private boolean dhcp = false;
        private IP4Address address = null;
        private String prefixString = null;
        private String netmask = null;
        private String gateway = null;

        public IfaceConfig(NetInterfaceStatus netInterfaceStatus, boolean autoConnect, boolean dhcp, IP4Address address,
                String prefixString, String netmask, String gateway) {
            this.netInterfaceStatus = netInterfaceStatus;
            this.autoConnect = autoConnect;
            this.dhcp = dhcp;
            this.address = address;
            this.prefixString = prefixString;
            this.netmask = netmask;
            this.gateway = gateway;
        }

        public NetInterfaceStatus getNetInterfaceStatus() {
            return this.netInterfaceStatus;
        }

        public boolean isAutoConnect() {
            return this.autoConnect;
        }

        public boolean isDhcp() {
            return this.dhcp;
        }

        public IP4Address getAddress() {
            return this.address;
        }

        public String getPrefixString() {
            return this.prefixString;
        }

        public String getNetmask() {
            return this.netmask;
        }

        public String getGateway() {
            return this.gateway;
        }
    }
}
