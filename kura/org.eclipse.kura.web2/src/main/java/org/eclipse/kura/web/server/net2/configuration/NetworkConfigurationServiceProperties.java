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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.net.util.NetworkUtil;

public class NetworkConfigurationServiceProperties {

    private final Map<String, Object> properties;

    private static final String NA = "N/A";

    public NetworkConfigurationServiceProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /*
     * Common properties
     */

    private static final String NET_INTERFACES = "net.interfaces";
    private static final String NET_INTERFACE_TYPE = "net.interface.%s.type";
    private static final String NET_INTERFACE_CONFIG_WIFI_MODE = "net.interface.%s.config.wifi.mode";
    private static final String NET_INTERFACE_CONFIG_NAT_ENABLED = "net.interface.%s.config.nat.enabled";

    public List<String> getNetInterfaces() {
        String netInterfaces = (String) this.properties.get(NET_INTERFACES);
        String[] interfaces = netInterfaces.split(",");

        List<String> ifnames = new ArrayList<>();

        for (String name : interfaces) {
            ifnames.add(name.trim());
        }

        return ifnames;
    }

    public String getType(String ifname) {
        return (String) this.properties.get(String.format(NET_INTERFACE_TYPE, ifname));
    }

    public Optional<String> getWifiMode(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MODE, ifname)));
    }

    public boolean getNatEnabled(String ifname) {
        return (boolean) this.properties.get(String.format(NET_INTERFACE_CONFIG_NAT_ENABLED, ifname));
    }

    /*
     * IPv4 properties
     */

    private static final String NET_INTERFACE_CONFIG_IP4_STATUS = "net.interface.%s.config.ip4.status";
    private static final String NET_INTERFACE_CONFIG_IP4_ADDRESS = "net.interface.%s.config.ip4.address";
    private static final String NET_INTERFACE_CONFIG_IP4_NETMASK = "net.interface.%s.config.ip4.prefix";
    private static final String NET_INTERFACE_CONFIG_IP4_GATEWAY = "net.interface.%s.config.ip4.gateway";
    private static final String NET_INTERFACE_CONFIG_IP4_DNS_SERVERS = "net.interface.%s.config.ip4.dnsServers";

    public Optional<String> getIp4Status(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_IP4_STATUS, ifname)));
    }

    public String getIp4Address(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP4_ADDRESS, ifname), "");
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

    public String getIp4Gateway(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP4_GATEWAY, ifname), "");
    }

    public String getIp4DnsServers(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IP4_DNS_SERVERS, ifname),
                "");
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

    public String getDhcpServer4RangeStart(String ifname) {
        return (String) this.properties
                .getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_START, ifname), "172.16.0.100");
    }

    public String getDhcpServer4RangeEnd(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_RANGE_END, ifname),
                "172.16.0.110");
    }

    public int getDhcpServer4LeaseTime(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_LEASE_TIME, ifname), 7200);
    }

    public int getDhcpServer4MaxLeaseTime(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_MAX_LEASE_TIME, ifname),
                7200);
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

    public boolean getDhcpServer4PassDns(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DHCP_SERVER_PASS_DNS, ifname),
                false);
    }

    /*
     * IPv4 DHCP Client properties
     */

    private static final String NET_INTERFACE_CONFIG_DHCP_CLIENT_ENABLED = "net.interface.%s.config.dhcpClient4.enabled";

    public boolean getDhcpClient4Enabled(String ifname) {
        return (boolean) this.properties.get(String.format(NET_INTERFACE_CONFIG_DHCP_CLIENT_ENABLED, ifname));
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

    public Password getWifiMasterPassphrase(String ifname) {
        return getPasswordFromProperty(this.properties
                .get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_PASSPHRASE, ifname)));
    }

    public String getWifiMasterSsid(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_SSID, ifname),
                "");
    }

    public Optional<String> getWifiMasterSecurityType(String ifname) {
        return Optional.ofNullable(
                (String) this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_SECURITY_TYPE, ifname)));
    }

    public Optional<String> getWifiMasterMode(String ifname) {
        return Optional.ofNullable(
                (String) this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_MODE, ifname)));
    }

    public String getWifiMasterChannel(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_CHANNEL, ifname),
                "");
    }

    public Optional<String> getWifiMasterRadioMode(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_RADIO_MODE, ifname)));
    }

    public boolean getWifiMasterIgnoreSsid(String ifname) {
        return (boolean) this.properties
                .getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_IGNORE_SSID, ifname), false);
    }

    public Optional<String> getWifiMasterPairwiseCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_PAIRWISE_CIPHERS, ifname)));
    }

    public Optional<String> getWifiMasterGroupCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_MASTER_GROUP_CIPHERS, ifname)));
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
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_PING_AP = "net.interface.%s..config.wifi.infra.pingAccessPoint";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_DRIVER = "net.interface.%s.config.wifi.infra.driver";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_SECURITY_TYPE = "net.interface.%s..config.wifi.infra.securityType";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS = "net.interface.%s.config.wifi.infra.pairwiseCiphers";
    private static final String NET_INTERFACE_CONFIG_WIFI_INFRA_GROUP_CIPHERS = "net.interface.%s.config.wifi.infra.groupCiphers";

    public String getWifiInfraSsid(String ifname) {
        return (String) this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_SSID, ifname));
    }

    public String getWifiInfraChannel(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_CHANNEL, ifname),
                "");
    }

    public Optional<String> getWifiInfraBgscan(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_BGSCAN, ifname)));
    }

    public Password getWifiInfraPassphrase(String ifname) {
        return getPasswordFromProperty(this.properties
                .get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PASSPHRASE, ifname)));
    }

    public boolean getWifiInfraIgnoreSsid(String ifname) {
        return (boolean) this.properties
                .getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_IGNORE_SSID, ifname), false);
    }

    public Optional<String> getWifiInfraMode(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_MODE, ifname)));
    }

    public boolean getWifiInfraPingAP(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PING_AP, ifname),
                false);
    }

    public String getWifiInfraDriver(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_DRIVER, ifname), "");
    }

    public Optional<String> getWifiInfraSecurityType(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_SECURITY_TYPE, ifname)));
    }
    
    public Optional<String> getWifiInfraPairwiseCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_PAIRWISE_CIPHERS, ifname)));
    }

    public Optional<String> getWifiInfraGroupCiphers(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_WIFI_INFRA_GROUP_CIPHERS, ifname)));
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
    private static final String NET_INTERFACE_CONFIG_IPC_ECHO_INTERVAL = "net.interface.%s.config.lpcEchoInterval";
    private static final String NET_INTERFACE_CONFIG_ACTIVE_FILTER = "net.interface.%s.config.activeFilter";
    private static final String NET_INTERFACE_CONFIG_ECHO_FAILURE = "net.interface.%s.config.lpcEchoFailure";
    private static final String NET_INTERFACE_CONFIG_DIVERSITY_ENABLED = "net.interface.%s.config.diversityEnabled";
    private static final String NET_INTERFACE_CONFIG_RESET_TIMEOUT = "net.interface.%s.config.resetTimeout";
    private static final String NET_INTERFACE_CONFIG_GPS_ENABLED = "net.interface.%s.config.gpsEnabled";
    private static final String NET_INTERFACE_CONFIG_PERSIST = "net.interface.%s.config.persist";
    private static final String NET_INTERFACE_CONFIG_APN = "net.interface.%s.config.apn";
    private static final String NET_INTERFACE_CONFIG_DIAL_STRING = "net.interface.%s.config.dialString";
    private static final String NET_INTERFACE_CONFIG_HOLDOFF = "net.interface.%s.config.holdoff";
    private static final String NET_INTERFACE_CONFIG_PPP_NUM = "net.interface.%s.config.pppNum";
    private static final String NET_INTERFACE_CONFIG_CONNECTION_STATUS = "net.interface.%s.config.connection.status";
    private static final String NET_INTERFACE_USB_PRODUCT_NAME = "net.interface.%s.usb.product.name";
    private static final String NET_INTERFACE_USB_VENDOR_ID = "net.interface.%s.usb.vendor.id";
    private static final String NET_INTERFACE_USB_VENDOR_NAME = "net.interface.%s.usb.vendor.name";
    private static final String NET_INTERFACE_USB_BUS_NUMBER = "net.interface.%s.usb.busNumber";
    private static final String NET_INTERFACE_USB_PRODUCT_ID = "net.interface.%s.usb.product.id";
    private static final String NET_INTERFACE_USB_DEVICE_PATH = "net.interface.%s.usb.devicePath";

    public boolean getModemEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_ENABLED, ifname), false);
    }

    public int getModemIdle(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IDLE, ifname), 0);
    }

    public String getModemUsername(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_USERNAME, ifname), "");
    }

    public Password getModemPassword(String ifname) {
        return getPasswordFromProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_PASSWORD, ifname)));
    }

    public Optional<String> getModemPdpType(String ifname) {
        return Optional.ofNullable((String) this.properties.get(String.format(NET_INTERFACE_CONFIG_PDP_TYPE, ifname)));
    }

    public int getModemMaxFail(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_MAX_FAIL, ifname), 0);
    }

    public Optional<String> getModemAuthType(String ifname) {
        return getNonEmptyStringProperty(this.properties.get(String.format(NET_INTERFACE_CONFIG_AUTH_TYPE, ifname)));
    }

    public int getModemIpcEchoInterval(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_IPC_ECHO_INTERVAL, ifname), 0);
    }

    public String getModemActiveFilter(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_ACTIVE_FILTER, ifname), "");
    }

    public int getModemIpcEchoFailure(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_ECHO_FAILURE, ifname), 0);
    }

    public boolean getModemDiversityEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DIVERSITY_ENABLED, ifname),
                false);
    }

    public int getModemResetTimeout(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_RESET_TIMEOUT, ifname), 0);
    }

    public boolean getModemGpsEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_GPS_ENABLED, ifname), false);
    }

    public boolean getModemPersistEnabled(String ifname) {
        return (boolean) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_PERSIST, ifname), false);
    }

    public String getModemApn(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_APN, ifname), "");
    }

    public String getModemDialString(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_DIAL_STRING, ifname), "");
    }

    public int getModemHoldoff(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_HOLDOFF, ifname), 0);
    }

    public int getModemPppNum(String ifname) {
        return (int) this.properties.getOrDefault(String.format(NET_INTERFACE_CONFIG_PPP_NUM, ifname), 0);
    }

    public Optional<String> getModemConnectionStatus(String ifname) {
        return getNonEmptyStringProperty(
                this.properties.get(String.format(NET_INTERFACE_CONFIG_CONNECTION_STATUS, ifname)));
    }

    public String getUsbProductName(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_PRODUCT_NAME, ifname), NA);
    }

    public String getUsbVendorId(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_VENDOR_ID, ifname), NA);
    }

    public String getUsbVendorName(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_VENDOR_NAME, ifname), NA);
    }

    public String getUsbBusNumber(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_BUS_NUMBER, ifname), NA);
    }

    public String getUsbProductId(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_PRODUCT_ID, ifname), NA);
    }

    public String getUsbDevicePath(String ifname) {
        return (String) this.properties.getOrDefault(String.format(NET_INTERFACE_USB_DEVICE_PATH, ifname), NA);
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

}
