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
package org.eclipse.kura.web.server.net2.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfigurationServiceProperties {

    private final Map<String, Object> properties;

    private static final String NA = "N/A";
    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationServiceProperties.class);

    public NetworkConfigurationServiceProperties() {
        this.properties = new HashMap<>();
    }

    public NetworkConfigurationServiceProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /*
     * Common properties
     */

    private static final String NET_INTERFACE_TYPE = "net.interface.%s.type";
    private static final String NET_INTERFACE_CONFIG_WIFI_MODE = "net.interface.%s.config.wifi.mode";
    private static final String NET_INTERFACE_CONFIG_NAT_ENABLED = "net.interface.%s.config.nat.enabled";

    public Optional<String> getType(String ifname) {
        return Optional.ofNullable((String) this.properties.get(String.format(NET_INTERFACE_TYPE, ifname)));
    }

    public void setType(String ifname, String type) {
        this.properties.put(String.format(NET_INTERFACE_TYPE, ifname), type);
    }

    public Optional<String> getWifiMode(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MODE, ifname)));
    }

    public void setWifiMode(String ifname, String wifiMode) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MODE, ifname), wifiMode);
    }

    public boolean getNatEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_NAT_ENABLED, ifname), false);
    }

    public void setNatEnabled(String ifname, boolean natEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_NAT_ENABLED, ifname), natEnabled);
    }

    /*
     * IPv4 properties
     */

    private static final String NET_INTERFACE_CONFIG_IP4_STATUS = "net.interface.%s.config.ip4.status";
    private static final String NET_INTERFACE_CONFIG_IP4_WAN_PRIORITY = "net.interface.%s.config.ip4.wan.priority";
    private static final String NET_INTERFACE_CONFIG_IP4_ADDRESS = "net.interface.%s.config.ip4.address";
    private static final String NET_INTERFACE_CONFIG_IP4_NETMASK = "net.interface.%s.config.ip4.prefix";
    private static final String NET_INTERFACE_CONFIG_IP4_GATEWAY = "net.interface.%s.config.ip4.gateway";
    private static final String NET_INTERFACE_CONFIG_IP4_DNS_SERVERS = "net.interface.%s.config.ip4.dnsServers";

    public Optional<String> getIp4Status(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_IP4_STATUS, ifname)));
    }

    public void setIp4Status(String ifname, String status) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP4_STATUS, ifname), status);
    }

    public Optional<Integer> getIp4WanPriority(String ifname) {
        return Optional.ofNullable(
                (Integer) this.properties.get(String.format(NET_INTERFACE_CONFIG_IP4_WAN_PRIORITY, ifname)));
    }

    public void setIp4WanPriority(String ifname, int priority) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP4_WAN_PRIORITY, ifname), priority);
    }

    public String getIp4Address(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP4_ADDRESS, ifname), "");
    }

    public void setIp4Address(String ifname, String address) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP4_ADDRESS, ifname), address);
    }

    public String getIp4Netmask(String ifname) {
        if (this.properties.containsKey(String.format(NET_INTERFACE_CONFIG_IP4_NETMASK, ifname))) {
            Short prefix = (short) this.properties.get(String.format(NET_INTERFACE_CONFIG_IP4_NETMASK, ifname));

            if (prefix > 0 && prefix < 33) {
                return NetworkUtil.getNetmaskStringForm(prefix.intValue());
            }
        }

        return "";
    }

    public void setIp4Netmask(String ifname, String netmask) {
        short prefix = NetworkUtil.getNetmaskShortForm(netmask);
        if (prefix > 0 && prefix < 33) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_IP4_NETMASK, ifname), prefix);
        }
    }

    public String getIp4Gateway(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP4_GATEWAY, ifname), "");
    }

    public void setIp4Gateway(String ifname, String gateway) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP4_GATEWAY, ifname), gateway);
    }

    public String getIp4DnsServers(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP4_DNS_SERVERS, ifname), "");
    }

    public void setIp4DnsServers(String ifname, String dnsServers) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP4_DNS_SERVERS, ifname), dnsServers);
    }

    /*
     * IPv6 properties
     */

    private static final String NET_INTERFACE_CONFIG_IP6_STATUS = "net.interface.%s.config.ip6.status";
    private static final String NET_INTERFACE_CONFIG_IP6_WAN_PRIORITY = "net.interface.%s.config.ip6.wan.priority";
    private static final String NET_INTERFACE_CONFIG_IP6_ADDRESS_METHOD = "net.interface.%s.config.ip6.address.method";
    private static final String NET_INTERFACE_CONFIG_IP6_ADDR_GEN_MODE = "net.interface.%s.config.ip6.addr.gen.mode";
    private static final String NET_INTERFACE_CONFIG_IP6_PRIVACY = "net.interface.%s.config.ip6.privacy";
    private static final String NET_INTERFACE_CONFIG_IP6_ADDRESS = "net.interface.%s.config.ip6.address";
    private static final String NET_INTERFACE_CONFIG_IP6_NETMASK = "net.interface.%s.config.ip6.prefix";
    private static final String NET_INTERFACE_CONFIG_IP6_GATEWAY = "net.interface.%s.config.ip6.gateway";
    private static final String NET_INTERFACE_CONFIG_IP6_DNS_SERVERS = "net.interface.%s.config.ip6.dnsServers";

    public Optional<String> getIp6Status(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_IP6_STATUS, ifname)));
    }

    public void setIp6Status(String ifname, String status) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_STATUS, ifname), status);
    }

    public Optional<Integer> getIp6WanPriority(String ifname) {
        return Optional.ofNullable(
                (Integer) this.properties.get(String.format(NET_INTERFACE_CONFIG_IP6_WAN_PRIORITY, ifname)));
    }

    public void setIp6WanPriority(String ifname, int priority) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_WAN_PRIORITY, ifname), priority);
    }

    public Optional<String> getIp6AddressMethod(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_IP6_ADDRESS_METHOD, ifname)));
    }

    public void setIp6AddressMethod(String ifname, String addressMethod) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_ADDRESS_METHOD, ifname), addressMethod);
    }

    public Optional<String> getIp6AddressGenMode(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_IP6_ADDR_GEN_MODE, ifname)));
    }

    public void setIp6AddressGenMode(String ifname, String addrGenMode) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_ADDR_GEN_MODE, ifname), addrGenMode);
    }

    public Optional<String> getIp6Privacy(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_IP6_PRIVACY, ifname)));
    }

    public void setIp6Privacy(String ifname, String privacy) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_PRIVACY, ifname), privacy);
    }

    public String getIp6Address(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP6_ADDRESS, ifname), "");
    }

    public void setIp6Address(String ifname, String address) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_ADDRESS, ifname), address);
    }

    public Optional<Integer> getIp6Netmask(String ifname) {
        if (this.properties.containsKey(String.format(NET_INTERFACE_CONFIG_IP6_NETMASK, ifname))) {
            Short netmask = (Short) this.properties.get(String.format(NET_INTERFACE_CONFIG_IP6_NETMASK, ifname));
            return Optional.ofNullable((Integer) netmask.intValue());
        }

        return Optional.empty();
    }

    public void setIp6Netmask(String ifname, int netmask) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_NETMASK, ifname), ((Integer) netmask).shortValue());
    }

    public String getIp6Gateway(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP6_GATEWAY, ifname), "");
    }

    public void setIp6Gateway(String ifname, String gateway) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_GATEWAY, ifname), gateway);
    }

    public String getIp6DnsServers(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP6_DNS_SERVERS, ifname), "");
    }

    public void setIp6DnsServers(String ifname, String dnsServers) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IP6_DNS_SERVERS, ifname), dnsServers);
    }

    /*
     * IPv4 DHCP Server properties
     */

    private static final String NET_INTERFACE_CONFIG_DHCP_SERVER_ENABLED = "net.interface.%s.config.dhcpServer4.enabled";
    private static final String NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_START = "net.interface.%s.config.dhcpServer4.rangeStart";
    private static final String NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_END = "net.interface.%s.config.dhcpServer4.rangeEnd";
    private static final String NET_INTERFACE_CONFIG_DHCP_LEASE_TIME = "net.interface.%s.config.dhcpServer4.defaultLeaseTime";
    private static final String NET_INTERFACE_CONFIG_DHCP_MAX_LEASE_TIME = "net.interface.%s.config.dhcpServer4.maxLeaseTime";
    private static final String NET_INTERFACE_CONFIG_DHCP_SERVER_NETMASK = "net.interface.%s.config.dhcpServer4.prefix";
    private static final String NET_INTERFACE_CONFIG_DHCP_SERVER_PASS_DNS = "net.interface.%s.config.dhcpServer4.passDns";

    public boolean getDhcpServer4Enabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_ENABLED, ifname),
                false);
    }

    public void setDhcpServer4Enabled(String ifname, boolean isDhcpServerEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_ENABLED, ifname), isDhcpServerEnabled);
    }

    public String getDhcpServer4RangeStart(String ifname) {
        return (String) this.properties
                .getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_START, ifname), "172.16.0.100");
    }

    public void setDhcpServer4RangeStart(String ifname, String rangeStart) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_START, ifname), rangeStart);
    }

    public String getDhcpServer4RangeEnd(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_END, ifname),
                "172.16.0.110");
    }

    public void setDhcpServer4RangeEnd(String ifname, String rangeEnd) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_END, ifname), rangeEnd);
    }

    public int getDhcpServer4LeaseTime(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_LEASE_TIME, ifname), 7200);
    }

    public void setDhcpServer4LeaseTime(String ifname, int leaseTime) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_LEASE_TIME, ifname), leaseTime);
    }

    public int getDhcpServer4MaxLeaseTime(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_MAX_LEASE_TIME, ifname),
                7200);
    }

    public void setDhcpServer4MaxLeaseTime(String ifname, int maxLeaseTime) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_MAX_LEASE_TIME, ifname), maxLeaseTime);
    }

    public String getDhcpServer4Netmask(String ifname) {
        if (this.properties.containsKey(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_NETMASK, ifname))) {
            Short prefix = (short) this.properties.get(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_NETMASK, ifname));

            if (prefix > 0 && prefix < 33) {
                return NetworkUtil.getNetmaskStringForm(prefix.intValue());
            }
        }
        return "";
    }

    public void setDhcpServer4Netmask(String ifname, String netmask) {
        short prefix = NetworkUtil.getNetmaskShortForm(netmask);
        if (prefix > 0 && prefix < 33) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_NETMASK, ifname), prefix);
        }
    }

    public boolean getDhcpServer4PassDns(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_PASS_DNS, ifname),
                false);
    }

    public void setDhcpServer4PassDns(String ifname, boolean isPassDns) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_PASS_DNS, ifname), isPassDns);
    }

    /*
     * IPv4 DHCP Client properties
     */

    private static final String NET_INTERFACE_CONFIG_DHCP_CLIENT_ENABLED = "net.interface.%s.config.dhcpClient4.enabled";

    public boolean getDhcpClient4Enabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_CLIENT_ENABLED, ifname),
                true);
    }

    public void setDhcpClient4Enabled(String ifname, boolean isDhcpClientEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DHCP_CLIENT_ENABLED, ifname), isDhcpClientEnabled);
    }

    /*
     * WiFi Master (Access Point) properties
     */

    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_DRIVER = "net.interface.%s.config.wifi.master.driver";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_PASSPHRASE = "net.interface.%s.config.wifi.master.passphrase";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_SSID = "net.interface.%s.config.wifi.master.ssid";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_SECURITY_TYPE = "net.interface.%s.config.wifi.master.securityType";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_MODE = "net.interface.%s.config.wifi.master.mode";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_CHANNEL = "net.interface.%s.config.wifi.master.channel";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_RADIO_MODE = "net.interface.%s.config.wifi.master.radioMode";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_IGNORE_SSID = "net.interface.%s.config.wifi.master.ignoreSSID";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_PAIRWISE_CIPHERS = "net.interface.%s.config.wifi.master.pairwiseCiphers";
    private static final String NET_INTERFACE_CONFIG_WIFI_MASTER_GROUP_CIPHERS = "net.interface.%s.config.wifi.master.groupCiphers";

    public String getWifiMasterDriver(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_DRIVER, ifname),
                "");
    }

    public void setWifiMasterDriver(String ifname, String driver) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_DRIVER, ifname), driver);
    }

    public Password getWifiMasterPassphrase(String ifname) {
        return getPasswordFromProperty(this.properties
                .get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_PASSPHRASE, ifname)));
    }

    public void setWifiMasterPassphrase(String ifname, String passphrase) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_PASSPHRASE, ifname),
                new Password(passphrase));
    }

    public String getWifiMasterSsid(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_SSID, ifname),
                "");
    }

    public void setWifiMasterSsid(String ifname, String ssid) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_SSID, ifname), ssid);
    }

    public Optional<String> getWifiMasterSecurityType(String ifname) {
        return Optional.ofNullable(
                (String) this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_SECURITY_TYPE, ifname)));
    }

    public void setWifiMasterSecurityType(String ifname, String securityType) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_SECURITY_TYPE, ifname), securityType);
    }

    public Optional<String> getWifiMasterMode(String ifname) {
        return Optional.ofNullable(
                (String) this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_MODE, ifname)));
    }

    public void setWifiMasterMode(String ifname, String mode) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_MODE, ifname), mode);
    }

    public List<Integer> getWifiMasterChannel(String ifname) {
        return channelsAsIntegersList(
                (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_CHANNEL, ifname),
                        ""));
    }

    public void setWifiMasterChannel(String ifname, List<Integer> channels) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_CHANNEL, ifname),
                integerListAsChannels(channels));
    }

    public Optional<String> getWifiMasterRadioMode(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_RADIO_MODE, ifname)));
    }

    public void setWifiMasterRadioMode(String ifname, Optional<String> mode) {
        if (mode.isPresent()) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_RADIO_MODE, ifname), mode.get());
        }
    }

    public boolean getWifiMasterIgnoreSsid(String ifname) {
        return (boolean) this.properties
                .getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_IGNORE_SSID, ifname), false);
    }

    public void setWifiMasterIgnoreSsid(String ifname, boolean ignoreSsid) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_IGNORE_SSID, ifname), ignoreSsid);
    }

    public Optional<String> getWifiMasterPairwiseCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_PAIRWISE_CIPHERS, ifname)));
    }

    public void setWifiMasterPairwiseCiphers(String ifname, Optional<String> ciphers) {
        if (ciphers.isPresent()) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_PAIRWISE_CIPHERS, ifname),
                    ciphers.get());
        }
    }

    public Optional<String> getWifiMasterGroupCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_GROUP_CIPHERS, ifname)));
    }

    public void setWifiMasterGroupCiphers(String ifname, Optional<String> ciphers) {
        if (ciphers.isPresent()) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_GROUP_CIPHERS, ifname), ciphers.get());
        }
    }

    /*
     * WiFi Infra (Station Mode) properties
     */

    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_SSID = "net.interface.%s.config.wifi.infra.ssid";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_CHANNEL = "net.interface.%s.config.wifi.infra.channel";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_BGSCAN = "net.interface.%s.config.wifi.infra.bgscan";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_PASSPHRASE = "net.interface.%s.config.wifi.infra.passphrase";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_IGNORE_SSID = "net.interface.%s.config.wifi.infra.ignoreSSID";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_MODE = "net.interface.%s.config.wifi.infra.mode";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_PING_AP = "net.interface.%s.config.wifi.infra.pingAccessPoint";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_DRIVER = "net.interface.%s.config.wifi.infra.driver";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_SECURITY_TYPE = "net.interface.%s.config.wifi.infra.securityType";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS = "net.interface.%s.config.wifi.infra.pairwiseCiphers";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_GROUP_CIPHERS = "net.interface.%s.config.wifi.infra.groupCiphers";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_RADIO_MODE = "net.interface.%s.config.wifi.infra.radioMode";

    public String getWifiInfraSsid(String ifname) {
        return (String) this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_SSID, ifname));
    }

    public void setWifiInfraSsid(String ifname, String ssid) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_SSID, ifname), ssid);
    }

    public List<Integer> getWifiInfraChannel(String ifname) {
        return channelsAsIntegersList(
                (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_CHANNEL, ifname),
                        ""));
    }

    public void setWifiInfraChannel(String ifname, List<Integer> channels) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_CHANNEL, ifname),
                integerListAsChannels(channels));
    }

    public Optional<String> getWifiInfraBgscan(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_BGSCAN, ifname)));
    }

    public void setWifiInfraBgscan(String ifname, String bgScan) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_BGSCAN, ifname), bgScan);
    }

    public Password getWifiInfraPassphrase(String ifname) {
        return getPasswordFromProperty(this.properties
                .get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PASSPHRASE, ifname)));
    }

    public void setWifiInfraPassphrase(String ifname, String passphrase) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PASSPHRASE, ifname),
                new Password(passphrase));
    }

    public boolean getWifiInfraIgnoreSsid(String ifname) {
        return (boolean) this.properties
                .getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_IGNORE_SSID, ifname), false);
    }

    public void setWifiInfraIgnoreSsid(String ifname, boolean ignoreSsid) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_IGNORE_SSID, ifname), ignoreSsid);
    }

    public Optional<String> getWifiInfraMode(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_MODE, ifname)));
    }

    public void setWifiInfraMode(String ifname, String mode) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_MODE, ifname), mode);
    }

    public boolean getWifiInfraPingAP(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PING_AP, ifname),
                false);
    }

    public void setWifiInfraPingAP(String ifname, boolean isPingAP) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PING_AP, ifname), isPingAP);
    }

    public String getWifiInfraDriver(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_DRIVER, ifname), "");
    }

    public void setWifiInfraDriver(String ifname, String driver) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_DRIVER, ifname), driver);
    }

    public Optional<String> getWifiInfraSecurityType(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_SECURITY_TYPE, ifname)));
    }

    public void setWifiInfraSecurityType(String ifname, String securityType) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_SECURITY_TYPE, ifname), securityType);
    }

    public Optional<String> getWifiInfraPairwiseCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS, ifname)));
    }

    public void setWifiInfraPairwiseCiphers(String ifname, Optional<String> pairwiseCiphers) {
        if (pairwiseCiphers.isPresent()) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS, ifname),
                    pairwiseCiphers.get());
        }
    }

    public Optional<String> getWifiInfraGroupCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_GROUP_CIPHERS, ifname)));
    }

    public void setWifiInfraGroupCiphers(String ifname, Optional<String> groupCiphers) {
        if (groupCiphers.isPresent()) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_GROUP_CIPHERS, ifname),
                    groupCiphers.get());
        }
    }

    public Optional<String> getWifiInfraRadioMode(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_RADIO_MODE, ifname)));
    }

    public void setWifiInfraRadioMode(String ifname, Optional<String> mode) {
        if (mode.isPresent()) {
            this.properties.put(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_RADIO_MODE, ifname), mode.get());
        }
    }

    /*
     * Cellular Modem properties
     */

    private static final String NET_INTERFACE_CONFIG_ENABLED = "net.interface.%s.config.enabled";
    private static final String NET_INTERFACE_CONFIG_IDLE = "net.interface.%s.config.idle";
    private static final String NET_INTERFACE_CONFIG_USERNAME = "net.interface.%s.config.username";
    private static final String NET_INTERFACE_CONFIG_PASSWORD = "net.interface.%s.config.password";
    private static final String NET_INTERFACE_CONFIG_PDP_TYPE = "net.interface.%s.config.pdpType";
    private static final String NET_INTERFACE_CONFIG_MAX_FAIL = "net.interface.%s.config.maxFail";
    private static final String NET_INTERFACE_CONFIG_AUTH_TYPE = "net.interface.%s.config.authType";
    private static final String NET_INTERFACE_CONFIG_LPC_ECHO_INTERVAL = "net.interface.%s.config.lpcEchoInterval";
    private static final String NET_INTERFACE_CONFIG_ACTIVE_FILTER = "net.interface.%s.config.activeFilter";
    private static final String NET_INTERFACE_CONFIG_LPC_ECHO_FAILURE = "net.interface.%s.config.lpcEchoFailure";
    private static final String NET_INTERFACE_CONFIG_DIVERSITY_ENABLED = "net.interface.%s.config.diversityEnabled";
    private static final String NET_INTERFACE_CONFIG_RESET_TIMEOUT = "net.interface.%s.config.resetTimeout";
    private static final String NET_INTERFACE_CONFIG_GPS_ENABLED = "net.interface.%s.config.gpsEnabled";
    private static final String NET_INTERFACE_CONFIG_PERSIST = "net.interface.%s.config.persist";
    private static final String NET_INTERFACE_CONFIG_APN = "net.interface.%s.config.apn";
    private static final String NET_INTERFACE_CONFIG_DIAL_STRING = "net.interface.%s.config.dialString";
    private static final String NET_INTERFACE_CONFIG_HOLDOFF = "net.interface.%s.config.holdoff";
    private static final String NET_INTERFACE_CONFIG_PPP_NUM = "net.interface.%s.config.pppNum";
    private static final String NET_INTERFACE_USB_PRODUCT_NAME = "net.interface.%s.usb.product.name";
    private static final String NET_INTERFACE_USB_VENDOR_ID = "net.interface.%s.usb.vendor.id";
    private static final String NET_INTERFACE_USB_VENDOR_NAME = "net.interface.%s.usb.vendor.name";
    private static final String NET_INTERFACE_USB_BUS_NUMBER = "net.interface.%s.usb.busNumber";
    private static final String NET_INTERFACE_USB_PRODUCT_ID = "net.interface.%s.usb.product.id";
    private static final String NET_INTERFACE_USB_DEVICE_PATH = "net.interface.%s.usb.devicePath";

    public boolean getModemEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_ENABLED, ifname), false);
    }

    public void setModemEnabled(String ifname, boolean isModemEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_ENABLED, ifname), isModemEnabled);
    }

    public int getModemIdle(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IDLE, ifname), 0);
    }

    public void setModemIdle(String ifname, int modemIdle) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_IDLE, ifname), modemIdle);
    }

    public String getModemUsername(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_USERNAME, ifname), "");
    }

    public void setModemUsername(String ifname, String username) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_USERNAME, ifname), username);
    }

    public Password getModemPassword(String ifname) {
        return getPasswordFromProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_PASSWORD, ifname)));
    }

    public void setModemPassword(String ifname, String password) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_PASSWORD, ifname), new Password(password));
    }

    public Optional<String> getModemPdpType(String ifname) {
        return Optional.ofNullable((String) this.properties.get(String.format(NET_INTERFACE_CONFIG_PDP_TYPE, ifname)));
    }

    public void setModemPdpType(String ifname, String modemPdpType) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_PDP_TYPE, ifname), modemPdpType);
    }

    public int getModemMaxFail(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_MAX_FAIL, ifname), 1);
    }

    public void setModemMaxFail(String ifname, int maxFail) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_MAX_FAIL, ifname), maxFail);
    }

    public Optional<String> getModemAuthType(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_AUTH_TYPE, ifname)));
    }

    public void setModemAuthType(String ifname, String authType) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_AUTH_TYPE, ifname), authType);
    }

    public int getModemLpcEchoInterval(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_LPC_ECHO_INTERVAL, ifname), 0);
    }

    public void setModemLpcEchoInterval(String ifname, int echoInterval) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_LPC_ECHO_INTERVAL, ifname), echoInterval);
    }

    public String getModemActiveFilter(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_ACTIVE_FILTER, ifname), "");
    }

    public void setModemActiveFilter(String ifname, String activeFilter) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_ACTIVE_FILTER, ifname), activeFilter);
    }

    public int getModemLpcEchoFailure(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_LPC_ECHO_FAILURE, ifname), 0);
    }

    public void setModemLpcEchoFailure(String ifname, int echoFailure) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_LPC_ECHO_FAILURE, ifname), echoFailure);
    }

    public boolean getModemDiversityEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DIVERSITY_ENABLED, ifname),
                false);
    }

    public void setModemDiversityEnabled(String ifname, boolean isDiversityEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DIVERSITY_ENABLED, ifname), isDiversityEnabled);
    }

    public int getModemResetTimeout(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_RESET_TIMEOUT, ifname), 5);
    }

    public void setModemResetTimeout(String ifname, int modemResetTimeout) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_RESET_TIMEOUT, ifname), modemResetTimeout);
    }

    public boolean getModemGpsEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_GPS_ENABLED, ifname), false);
    }

    public void setModemGpsEnabled(String ifname, boolean isGpsEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_GPS_ENABLED, ifname), isGpsEnabled);
    }

    public boolean getModemPersistEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_PERSIST, ifname), false);
    }

    public void setModemPersistEnabled(String ifname, boolean isPersistEnabled) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_PERSIST, ifname), isPersistEnabled);
    }

    public String getModemApn(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_APN, ifname), "");
    }

    public void setModemApn(String ifname, String apn) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_APN, ifname), apn);
    }

    public String getModemDialString(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DIAL_STRING, ifname), "");
    }

    public void setModemDialString(String ifname, String dialString) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_DIAL_STRING, ifname), dialString);
    }

    public int getModemHoldoff(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_HOLDOFF, ifname), 0);
    }

    public void setModemHoldoff(String ifname, int holdoff) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_HOLDOFF, ifname), holdoff);
    }

    public int getModemPppNum(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_PPP_NUM, ifname), 0);
    }

    public void setModemPppNum(String ifname, int pppNum) {
        this.properties.put(String.format(NET_INTERFACE_CONFIG_PPP_NUM, ifname), pppNum);
    }

    public String getUsbProductName(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_PRODUCT_NAME, ifname), NA);
    }

    public void setUsbProductName(String ifname, String productName) {
        this.properties.put(String.format(NET_INTERFACE_USB_PRODUCT_NAME, ifname), productName);
    }

    public String getUsbVendorId(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_VENDOR_ID, ifname), NA);
    }

    public void getUsbVendorId(String ifname, String vendorId) {
        this.properties.put(String.format(NET_INTERFACE_USB_VENDOR_ID, ifname), vendorId);
    }

    public String getUsbVendorName(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_VENDOR_NAME, ifname), NA);
    }

    public void setUsbVendorName(String ifname, String vendorName) {
        this.properties.put(String.format(NET_INTERFACE_USB_VENDOR_NAME, ifname), vendorName);
    }

    public String getUsbBusNumber(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_BUS_NUMBER, ifname), NA);
    }

    public void setUsbBusNumber(String ifname, String busNumber) {
        this.properties.put(String.format(NET_INTERFACE_USB_BUS_NUMBER, ifname), busNumber);
    }

    public String getUsbProductId(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_PRODUCT_ID, ifname), NA);
    }

    public void setUsbProductId(String ifname, String productId) {
        this.properties.put(String.format(NET_INTERFACE_USB_PRODUCT_ID, ifname), productId);
    }

    public String getUsbDevicePath(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_DEVICE_PATH, ifname), NA);
    }

    public void setUsbDevicePath(String ifname, String path) {
        this.properties.put(String.format(NET_INTERFACE_USB_DEVICE_PATH, ifname), path);
    }

    /**
     * Password properties of the NetworkConfigurationService can be empty strings.
     * This method deals with that.
     */
    private Password getPasswordFromProperty(Object potentialStringPassword) {
        Optional<String> password = getNonEmptyStringProperty(potentialStringPassword);

        if (password.isPresent()) {
            return new Password(password.get());
        }

        if (potentialStringPassword instanceof Password) {
            return (Password) potentialStringPassword;
        }

        return new Password("");
    }

    /**
     * Properties of the NetworkConfigurationService might be empty strings.
     * This method return an empty Optional if the property is null, not a string,
     * or and empty string.
     */
    private Optional<String> getNonEmptyStringProperty(Object property) {
        if (property instanceof String && !((String) property).isEmpty()) {
            return Optional.of((String) property);
        }

        return Optional.empty();
    }

    private List<Integer> channelsAsIntegersList(String channelValue) {
        List<Integer> channels = new ArrayList<>();

        String[] split = channelValue.split(" ");

        for (String channel : split) {
            if (!channel.trim().isEmpty()) {
                try {
                    channels.add(Integer.parseInt(channel.trim()));
                } catch (NumberFormatException e) {
                    logger.error("Error parsing channel property '" + channelValue + "'", e);
                }
            }
        }

        return channels;
    }

    private String integerListAsChannels(List<Integer> channels) {
        StringBuilder result = new StringBuilder();
        for (int channel : channels) {
            result.append(channel);
            result.append(" ");
        }

        return result.toString().trim();
    }

}
