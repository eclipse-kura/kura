/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.net;

import static java.util.Objects.isNull;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpConfigurationInterpreter {

    private static final Logger logger = LoggerFactory.getLogger(IpConfigurationInterpreter.class);

    private static final String NET_INTERFACE = "net.interface.";

    private IpConfigurationInterpreter() {

    }

    public static List<NetConfig> populateConfiguration(Map<String, Object> props, String interfaceName,
            IPAddress netInterfaceAddress, boolean virtualInterface) throws UnknownHostException {
        List<NetConfig> netConfigs = new ArrayList<>();

        if (isNull(props)) {
            return netConfigs;
        }

        // POPULATE NetConfigs
        // dhcp4

        NetConfigIP4 netConfigIP4 = getIp4NetConfig(props, interfaceName, virtualInterface);

        List<IP4Address> dnsAddreses = getIp4Dns(props, interfaceName);
        netConfigIP4.setDnsServers(dnsAddreses);

        List<IP4Address> winsIPs = getIp4WinsServers(props, interfaceName);
        netConfigIP4.setWinsServers(winsIPs);

        List<String> domainNames = getIp4Domains(props, interfaceName);
        netConfigIP4.setDomains(domainNames);

        netConfigs.add(netConfigIP4);

        // FirewallNatConfig - see if NAT is enabled
        String configNatEnabled = NET_INTERFACE + interfaceName + ".config.nat.enabled";
        if (props.containsKey(configNatEnabled)) {
            boolean natEnabled = (Boolean) props.get(configNatEnabled);
            logger.trace("NAT enabled? {}", natEnabled);

            if (natEnabled) {
                FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig(interfaceName, "unknown", true);
                netConfigs.add(natConfig);
            }
        }

        DhcpServerConfigIP4 dhcpServerConfigIP4 = getDhcpServerIp4(props, interfaceName, netInterfaceAddress,
                netConfigIP4);
        if (!isNull(dhcpServerConfigIP4)) {
            netConfigs.add(dhcpServerConfigIP4);
        }

        NetConfigIP6 netConfigIP6 = getIp6NetConfig(props, interfaceName);
        if (!isNull(netConfigIP6)) {
            netConfigs.add(getIp6NetConfig(props, interfaceName));
        }

        return netConfigs;

    }

    private static DhcpServerConfigIP4 getDhcpServerIp4(Map<String, Object> props, String interfaceName,
            IPAddress netInterfaceAddress, NetConfigIP4 netConfigIP4) throws UnknownHostException {
        DhcpServerConfigIP4 dhcpServerConfigIP4 = null;
        String configDhcpServerEnabled = NET_INTERFACE + interfaceName + ".config.dhcpServer4.enabled";
        if (props.containsKey(configDhcpServerEnabled)) {

            boolean dhcpEnabled = isDhcpClient4Enabled(props, interfaceName);
            IP4Address routerAddress = dhcpEnabled ? (IP4Address) netInterfaceAddress : netConfigIP4.getAddress();

            short prefix = getDhcpServer4Prefix(props, interfaceName);

            IP4Address rangeStart = getDhcpServer4RangeStart(props, interfaceName);

            IP4Address rangeEnd = getDncpServer4RangeEnd(props, interfaceName);

            int defaultLeaseTime = getDhcpServer4DefaultLeaseTime(props, interfaceName);

            int maximumLeaseTime = getDhcpServerMaxLeaseTime(props, interfaceName);

            boolean passDns = isDhcpServerPassDns(props, interfaceName);

            if (routerAddress != null && rangeStart != null && rangeEnd != null) {
                // get the netmask and subnet
                int prefixInt = prefix;
                int mask = ~((1 << 32 - prefixInt) - 1);
                String subnetMaskString = NetworkUtil.dottedQuad(mask);
                String subnetString = NetworkUtil.calculateNetwork(routerAddress.getHostAddress(), subnetMaskString);
                IP4Address subnet = (IP4Address) IPAddress.parseHostAddress(subnetString);
                IP4Address subnetMask = (IP4Address) IPAddress.parseHostAddress(subnetMaskString);

                List<IP4Address> dnServers = new ArrayList<>();
                dnServers.add(routerAddress);

                boolean dhcpServerEnabled = (Boolean) props.get(configDhcpServerEnabled);
                DhcpServerCfg dhcpServerCfg = new DhcpServerCfg(interfaceName, dhcpServerEnabled, defaultLeaseTime,
                        maximumLeaseTime, passDns);
                DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(subnet, subnetMask, prefix, routerAddress,
                        rangeStart, rangeEnd, dnServers);

                try {
                    dhcpServerConfigIP4 = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);
                } catch (KuraException e) {
                    logger.warn("This invalid DhcpServerCfgIP4 configuration is ignored - {}, {}", dhcpServerCfg,
                            dhcpServerCfgIP4);
                }
            }
        }
        return dhcpServerConfigIP4;
    }

    private static SystemService getSystemService() {
        BundleContext context = FrameworkUtil.getBundle(NetworkConfiguration.class).getBundleContext();
        ServiceReference<SystemService> systemServiceSR = context.getServiceReference(SystemService.class);
        return context.getService(systemServiceSR);
    }

    private static List<String> getIp4Domains(Map<String, Object> props, String interfaceName) {
        // domains
        String configDomains = NET_INTERFACE + interfaceName + ".config.ip4.domains";
        List<String> domainNames = new ArrayList<>();
        if (props.containsKey(configDomains)) {

            String domainsAll = (String) props.get(configDomains);
            String[] domains = domainsAll.split(",");
            for (String domain : domains) {
                logger.trace("IPv4 Domain: {}", domain);
                domainNames.add(domain);
            }
        }
        return domainNames;
    }

    private static List<IP4Address> getIp4WinsServers(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        // win servers
        String configWINSs = NET_INTERFACE + interfaceName + ".config.ip4.winsServers";
        List<IP4Address> winsIPs = new ArrayList<>();
        if (props.containsKey(configWINSs)) {

            String winsAll = (String) props.get(configWINSs);
            String[] winss = winsAll.split(",");
            for (String wins : winss) {
                logger.trace("WINS: {}", wins);
                IP4Address winsIp4 = (IP4Address) IPAddress.parseHostAddress(wins);
                winsIPs.add(winsIp4);
            }
        }
        return winsIPs;
    }

    private static List<IP4Address> getIp4Dns(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        // dns servers
        String configDNSs = NET_INTERFACE + interfaceName + ".config.ip4.dnsServers";
        List<IP4Address> dnsIPs = new ArrayList<>();
        if (props.containsKey(configDNSs)) {

            String dnsAll = (String) props.get(configDNSs);
            String[] dnss = dnsAll.split(",");
            for (String dns : dnss) {
                String trimmedDns = dns.trim();
                if (trimmedDns != null && trimmedDns.length() > 0) {
                    logger.trace("IPv4 DNS: {}", trimmedDns);
                    IP4Address dnsIp4 = (IP4Address) IPAddress.parseHostAddress(trimmedDns);
                    dnsIPs.add(dnsIp4);
                }
            }
        }
        return dnsIPs;
    }

    public static UsbDevice getUsbDeviceInfo(Map<String, Object> props, String interfaceName) {
        UsbDevice usbDevice = null;

        if (isNull(props)) {
            return usbDevice;
        }

        StringBuilder sbPrefix = new StringBuilder();
        String netIfReadOnlyPrefix = sbPrefix.append(NET_INTERFACE).append(interfaceName).append(".").toString();
        // USB
        String vendorId = (String) props.get(netIfReadOnlyPrefix + "usb.vendor.id");
        String vendorName = (String) props.get(netIfReadOnlyPrefix + "usb.vendor.name");
        String productId = (String) props.get(netIfReadOnlyPrefix + "usb.product.id");
        String productName = (String) props.get(netIfReadOnlyPrefix + "usb.product.name");
        String usbBusNumber = (String) props.get(netIfReadOnlyPrefix + "usb.busNumber");
        String usbDevicePath = (String) props.get(netIfReadOnlyPrefix + "usb.devicePath");

        if (vendorId != null && productId != null) {
            usbDevice = new UsbNetDevice(vendorId, productId, vendorName, productName, usbBusNumber, usbDevicePath,
                    interfaceName);
            logger.trace("adding usbDevice: {}, port: {}", usbDevice, usbDevice.getUsbPort());
        }
        return usbDevice;
    }

    private static NetConfigIP4 getIp4NetConfig(Map<String, Object> props, String interfaceName,
            boolean virtualInterface) throws UnknownHostException {

        NetInterfaceStatus status4 = getIp4Status(props, interfaceName, virtualInterface);
        NetConfigIP4 netConfigIP4 = new NetConfigIP4(status4, getAutoConnectProperty(status4));

        boolean dhcpEnabled = isDhcpClient4Enabled(props, interfaceName);
        if (dhcpEnabled) {
            netConfigIP4.setDhcp(true);
        } else {
            IP4Address ip4Address = getIp4StaticAddress(props, interfaceName);
            if (!isNull(ip4Address)) {
                netConfigIP4.setAddress(ip4Address);
            }

            try {
                Short networkPrefixLength = getIp4StaticPrefix(props, interfaceName);
                if (!isNull(networkPrefixLength)) {
                    netConfigIP4.setNetworkPrefixLength(networkPrefixLength);
                }
            } catch (KuraException e) {
                logger.error("Exception while setting Network Prefix length!", e);
            }

            IP4Address ip4Gateway = getIp4StaticGateway(props, interfaceName);
            if (!isNull(ip4Gateway)) {
                netConfigIP4.setGateway(ip4Gateway);
            }
        }
        return netConfigIP4;
    }

    private static NetInterfaceStatus getIp4Status(Map<String, Object> props, String interfaceName,
            boolean virtualInterface) {
        String configStatus4 = null;
        String configStatus4Key = NET_INTERFACE + interfaceName + ".config.ip4.status";
        if (props.containsKey(configStatus4Key)) {
            configStatus4 = (String) props.get(configStatus4Key);
        } else {
            configStatus4 = NetInterfaceStatus.netIPv4StatusDisabled.name();
            if (virtualInterface) {
                SystemService service = getSystemService();
                if (service != null) {
                    configStatus4 = NetInterfaceStatus.valueOf(service.getNetVirtualDevicesConfig()).name();
                }
            }
        }
        logger.trace("Status Ipv4? {}", configStatus4);

        return NetInterfaceStatus.valueOf(configStatus4);
    }

    private static IP4Address getIp4StaticGateway(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        // gateway
        String configIp4Gateway = NET_INTERFACE + interfaceName + ".config.ip4.gateway";
        String gatewayIp4 = (String) props.get(configIp4Gateway);
        IP4Address ip4Gateway = null;
        if (!isNull(gatewayIp4) && !gatewayIp4.trim().isEmpty()) {

            logger.trace("IPv4 gateway: {}", gatewayIp4);

            ip4Gateway = (IP4Address) IPAddress.parseHostAddress(gatewayIp4);
        }
        return ip4Gateway;
    }

    private static Short getIp4StaticPrefix(Map<String, Object> props, String interfaceName) {
        // prefix
        String configIp4Prefix = NET_INTERFACE + interfaceName + ".config.ip4.prefix";
        Short networkPrefixLength = null;
        Object ip4PrefixObj = props.get(configIp4Prefix);
        if (!isNull(ip4PrefixObj)) {
            if (ip4PrefixObj instanceof Short) {
                networkPrefixLength = (Short) ip4PrefixObj;
            } else if (ip4PrefixObj instanceof String) {
                networkPrefixLength = Short.parseShort((String) ip4PrefixObj);
            }
        }

        return networkPrefixLength;
    }

    private static IP4Address getIp4StaticAddress(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        // NetConfigIP4
        String configIp4 = NET_INTERFACE + interfaceName + ".config.ip4.address";
        String addressIp4 = (String) props.get(configIp4);
        IP4Address ip4Address = null;
        if (!isNull(addressIp4) && !addressIp4.trim().isEmpty()) {
            logger.trace("IPv4 address: {}", addressIp4);

            ip4Address = (IP4Address) IPAddress.parseHostAddress(addressIp4);
        }
        return ip4Address;
    }

    private static boolean isDhcpServerPassDns(Map<String, Object> props, String interfaceName) {
        boolean passDns = false;
        // passDns
        String configDhcpServerPassDns = NET_INTERFACE + interfaceName + ".config.dhcpServer4.passDns";
        if (props.containsKey(configDhcpServerPassDns)) {
            if (props.get(configDhcpServerPassDns) instanceof Boolean) {
                passDns = (Boolean) props.get(configDhcpServerPassDns);
            } else if (props.get(configDhcpServerPassDns) instanceof String) {
                passDns = Boolean.parseBoolean((String) props.get(configDhcpServerPassDns));
            }
            logger.trace("DHCP Server Pass DNS?: {}", passDns);
        }
        return passDns;
    }

    private static int getDhcpServerMaxLeaseTime(Map<String, Object> props, String interfaceName) {
        int maximumLeaseTime = -1;
        // max lease time
        String configDhcpServerMaxLeaseTime = NET_INTERFACE + interfaceName + ".config.dhcpServer4.maxLeaseTime";
        if (props.containsKey(configDhcpServerMaxLeaseTime)) {
            if (props.get(configDhcpServerMaxLeaseTime) instanceof Integer) {
                maximumLeaseTime = (Integer) props.get(configDhcpServerMaxLeaseTime);
            } else if (props.get(configDhcpServerMaxLeaseTime) instanceof String) {
                maximumLeaseTime = Integer.parseInt((String) props.get(configDhcpServerMaxLeaseTime));
            }
            logger.trace("DHCP Server Maximum Lease Time: {}", maximumLeaseTime);
        }
        return maximumLeaseTime;
    }

    private static int getDhcpServer4DefaultLeaseTime(Map<String, Object> props, String interfaceName) {
        int defaultLeaseTime = -1;
        // default lease time
        String configDhcpServerDefaultLeaseTime = NET_INTERFACE + interfaceName
                + ".config.dhcpServer4.defaultLeaseTime";
        if (props.containsKey(configDhcpServerDefaultLeaseTime)) {
            if (props.get(configDhcpServerDefaultLeaseTime) instanceof Integer) {
                defaultLeaseTime = (Integer) props.get(configDhcpServerDefaultLeaseTime);
            } else if (props.get(configDhcpServerDefaultLeaseTime) instanceof String) {
                defaultLeaseTime = Integer.parseInt((String) props.get(configDhcpServerDefaultLeaseTime));
            }
            logger.trace("DHCP Server Default Lease Time: {}", defaultLeaseTime);
        }
        return defaultLeaseTime;
    }

    private static IP4Address getDncpServer4RangeEnd(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        IP4Address rangeEnd = null;
        // rangeEnd
        String configDhcpServerRangeEnd = NET_INTERFACE + interfaceName + ".config.dhcpServer4.rangeEnd";
        if (props.containsKey(configDhcpServerRangeEnd)) {
            String dhcpServerRangeEnd = (String) props.get(configDhcpServerRangeEnd);
            logger.trace("DHCP Server Range End: {}", dhcpServerRangeEnd);
            if (dhcpServerRangeEnd != null && !dhcpServerRangeEnd.isEmpty()) {
                rangeEnd = (IP4Address) IPAddress.parseHostAddress(dhcpServerRangeEnd);
            }
        }
        return rangeEnd;
    }

    private static IP4Address getDhcpServer4RangeStart(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {

        IP4Address rangeStart = null;
        // rangeStart
        String configDhcpServerRangeStart = NET_INTERFACE + interfaceName + ".config.dhcpServer4.rangeStart";
        if (props.containsKey(configDhcpServerRangeStart)) {
            String dhcpServerRangeStart = (String) props.get(configDhcpServerRangeStart);
            logger.trace("DHCP Server Range Start: {}", dhcpServerRangeStart);
            if (dhcpServerRangeStart != null && !dhcpServerRangeStart.isEmpty()) {
                rangeStart = (IP4Address) IPAddress.parseHostAddress(dhcpServerRangeStart);
            }
        }
        return rangeStart;
    }

    private static short getDhcpServer4Prefix(Map<String, Object> props, String interfaceName) {
        short prefix = -1;
        // prefix
        String configDhcpServerPrefix = NET_INTERFACE + interfaceName + ".config.dhcpServer4.prefix";
        if (props.containsKey(configDhcpServerPrefix)) {
            if (props.get(configDhcpServerPrefix) instanceof Short) {
                prefix = (Short) props.get(configDhcpServerPrefix);
            } else if (props.get(configDhcpServerPrefix) instanceof String) {
                prefix = Short.parseShort((String) props.get(configDhcpServerPrefix));
            }
            logger.trace("DHCP Server prefix: {}", prefix);
        }
        return prefix;
    }

    private static NetConfigIP6 getIp6NetConfig(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {

        String configStatus6 = NetInterfaceStatus.netIPv6StatusDisabled.name();
        String configStatus6Key = NET_INTERFACE + interfaceName + ".config.ip6.status";
        Object value = props.get(configStatus6Key);
        if (!isNull(value) && value instanceof String) {
            configStatus6 = (String) value;
        }

        // dhcp6
        String configDhcp6 = NET_INTERFACE + interfaceName + ".config.dhcpClient6.enabled";
        NetConfigIP6 netConfigIP6 = null;
        boolean dhcp6Enabled = false;
        value = props.get(configDhcp6);
        if (!isNull(value) && value instanceof Boolean) {
            dhcp6Enabled = (Boolean) value;
        }

        if (!dhcp6Enabled) {
            // ip6
            NetInterfaceStatus status6 = NetInterfaceStatus.valueOf(configStatus6);
            netConfigIP6 = new NetConfigIP6(status6, getAutoConnectProperty(status6), dhcp6Enabled);

            IP6Address ip6Address = getIp6StaticAddress(props, interfaceName);
            if (!isNull(ip6Address)) {
                netConfigIP6.setAddress(ip6Address);
            }

            List<IP6Address> dns6IPs = getIp6StaticDnsServers(props, interfaceName);
            if (!dns6IPs.isEmpty()) {
                netConfigIP6.setDnsServers(dns6IPs);
            }

            List<String> ip6DomainNames = getIp6StaticDomains(props, interfaceName);
            if (!ip6DomainNames.isEmpty()) {
                netConfigIP6.setDomains(ip6DomainNames);
            }

        }
        return netConfigIP6;
    }

    private static List<String> getIp6StaticDomains(Map<String, Object> props, String interfaceName) {
        // domains
        String configDomains6 = NET_INTERFACE + interfaceName + ".config.ip6.domains";
        String domainsAll = (String) props.get(configDomains6);
        List<String> domainNames = new ArrayList<>();
        if (!isNull(domainsAll) && !domainsAll.trim().isEmpty()) {

            String[] domains = domainsAll.split(",");
            for (String domain : domains) {
                logger.trace("IPv6 Domain: {}", domain);
                domainNames.add(domain);
            }
        }
        return domainNames;
    }

    private static List<IP6Address> getIp6StaticDnsServers(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        // dns servers
        String configDNSs6 = NET_INTERFACE + interfaceName + ".config.ip6.dnsServers";
        String dnsAll = (String) props.get(configDNSs6);
        List<IP6Address> dnsIPs = new ArrayList<>();
        if (!isNull(dnsAll) && !dnsAll.trim().isEmpty()) {

            String[] dnss = dnsAll.split(",");
            for (String dns : dnss) {
                logger.trace("IPv6 DNS: {}", dns);
                IP6Address dnsIp6 = (IP6Address) IPAddress.parseHostAddress(dns);
                dnsIPs.add(dnsIp6);
            }
        }
        return dnsIPs;
    }

    private static IP6Address getIp6StaticAddress(Map<String, Object> props, String interfaceName)
            throws UnknownHostException {
        String configIp6 = NET_INTERFACE + interfaceName + ".config.ip6.address";
        String addressIp6 = (String) props.get(configIp6);
        IP6Address ip6Address = null;
        if (!isNull(addressIp6) && !addressIp6.trim().isEmpty()) {
            logger.trace("IPv6 address: {}", addressIp6);

            ip6Address = (IP6Address) IPAddress.parseHostAddress(addressIp6);
        }
        return ip6Address;
    }

    private static boolean isDhcpClient4Enabled(Map<String, Object> props, String interfaceName) {
        String configDhcp4 = NET_INTERFACE + interfaceName + ".config.dhcpClient4.enabled";
        boolean dhcpEnabled = false;
        if (props.containsKey(configDhcp4)) {
            dhcpEnabled = (Boolean) props.get(configDhcp4);
            logger.trace("DHCP 4 enabled? {}", dhcpEnabled);
        }
        return dhcpEnabled;
    }

    private static boolean getAutoConnectProperty(NetInterfaceStatus status) {
        boolean autoconnect = false;
        if (status.equals(NetInterfaceStatus.netIPv4StatusEnabledLAN)
                || status.equals(NetInterfaceStatus.netIPv4StatusEnabledWAN)
                || status.equals(NetInterfaceStatus.netIPv4StatusL2Only)
                || status.equals(NetInterfaceStatus.netIPv6StatusEnabledLAN)
                || status.equals(NetInterfaceStatus.netIPv6StatusEnabledWAN)
                || status.equals(NetInterfaceStatus.netIPv6StatusL2Only)) {
            autoconnect = true;
        }
        return autoconnect;
    }
}
