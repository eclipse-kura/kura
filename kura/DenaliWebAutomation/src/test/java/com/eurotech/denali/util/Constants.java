package com.eurotech.denali.util;

import java.io.File;

public class Constants {

	public static final String CHROME = "CHROME";
	public static final String FIREFOX = "FIREFOX";
	public static final String IE = "IE";
	public static final String PROPERTIES_FILE = "src/test/resources/application.properties";
	public static final String EMPTY = "";
	public static final String DENALI_IP = "denaliIP";
	public static final String DENALI_USERNAME = "denaliUsername";
	public static final String DENALI_PASSWORD = "denaliPassword";
	public static final String DENALI_NEW_PASSWORD = "denaliNewPassword";
	public static final String DEVICE_USERNAME = "deviceUsername";
	public static final String DEVICE_PASSWORD = "devicePassword";
	public static final String EDC_IP = "edcIP";
	public static final String EDC_USERNAME = "edcUserName";
	public static final String EDC_PASSWORD = "edcPassword";
	public static final String EDC_DEVICE_CONNECTED_STATUS = "Connected";
	public static final String TITLE = "title";
	public static final String NEW_CONSTANT_PASSWORD = "We!come1";
	public static final String ETH0_DISCONNECT_REBOOT_SH_TC_SOURCE_FILE_LOCATION = System
			.getProperty("user.dir")
			+ File.separator
			+ "src/test/resources/net_disconnect_eth0_reboot.sh";
	public static final String ETH0_DISCONNECT_REBOOT_SH_TC_DEST_FILE_LOCATION = "/tmp/net_disconnect_eth0_reboot.sh";
	
	public static final String NETWORK_HARDWARE_STATE = "ACTIVATED";
	public static final String NETWORK_HARDWARE_LOOPBACK_NAME = "lo";
	public static final String NETWORK_HARDWARE_LOOPBACK_TYPE = "LOOPBACK";
	public static final String NETWORK_HARDWARE_ETH0_NAME = "eth0";
	public static final String NETWORK_HARDWARE_ETH0_TYPE = "ETHERNET";
	public static final String NETWORK_TCP_IP_DHCP_CONFIGURATION = "Using DHCP";
	public static final String NETWORK_TCP_IP_MANUAL_CONFIGURATION = "Manually";
	public static final String NETWORK_TCP_IP_STATUS = "Enabled for LAN";
	public static final String NETWORK_TCP_ETH0_DHCP_IP_ADDRESS = "network.eth0.DHCP.ipAddress";
	public static final String NETWORK_TCP_ETH0_STATIC_IP_ADDRESS = "network.eth0.static.ipAddress";
	public static final String NETWORK_TCP_ETH0_STATIC_SUBNET_MASK = "network.eth0.static.subnetMask";
	public static final String NETWORK_TCP_ETH0_STATIC_GATEWAY = "network.eth0.static.gateway";
	public static final String NETWORK_TCP_ETH0_STATIC_DNS_SERVER = "network.eth0.static.dnsServer";
	public static final String NETWORK_TCP_ETH0_STATIC_SUBNET_MASK_SECONDARY = "network.eth0.static.subnetMask.secondary";
	public static final String NETWORK_TCP_ETH0_STATIC_DNS_SERVER_SECONDARY = "network.eth0.static.dnsServer.secondary";
	public static final String NETWORK_DHCP_ETH0_BEGIN_ADDRESS = "network.eth0.dhcp.beginAddress";
	public static final String NETWORK_DHCP_ETH0_END_ADDRESS = "network.eth0.dhcp.endAddress";
	public static final String NETWORK_DHCP_ETH0_SUBNET_MASK = "network.eth0.dhcp.subnetMask";
	public static final String NETWORK_DHCP_ETH0_DEFAULT_LEASE_TIME = "network.eth0.dhcp.default.leaseTime";
	public static final String NETWORK_DHCP_ETH0_MAX_LEASE_TIME = "network.eth0.dhcp.max.leaseTime";
	public static final String NETWORK_TCP_ETH1_DHCP_IP_ADDRESS = "network.eth1.DHCP.ipAddress";
	public static final String NETWORK_TCP_WLAN0_DHCP_IP_ADDRESS = "network.wlan0.DHCP.ipAddress";
	public static final String NETWORK_TCP_WLAN0_STATIC_IP_ADDRESS = "network.wlan0.static.ipAddress";
	public static final String NETWORK_TCP_WLAN0_STATIC_SUBNET_MASK = "network.wlan0.static.subnetMask";
	public static final String NETWORK_DHCP_WLAN0_BEGIN_ADDRESS = "network.wlan0.dhcp.beginAddress";
	public static final String NETWORK_DHCP_WLAN0_END_ADDRESS = "network.wlan0.dhcp.endAddress";
	public static final String NETWORK_DHCP_WLAN0_SUBNET_MASK = "network.wlan0.dhcp.subnetMask";
	public static final String NETWORK_DHCP_WLAN0_DEFAULT_LEASE_TIME = "network.wlan0.dhcp.default.leaseTime";
	public static final String NETWORK_DHCP_WLAN0_MAX_LEASE_TIME = "network.wlan0.dhcp.max.leaseTime";
	public static final String NETWORK_WIRELESS_NAME = "network.wireless.network.name";
	public static final String NETWORK_WIRELESS_PASSWORD = "network.wireless.network.password";
	public static final String NETWORK_WIRELESS_SECURITY_TYPE = "network.wireless.security.type";
	public static final String NETWORK_WIRELESS_DHCP_NAME = "esf_gateway";
	public static final String NETWORK_WIRELESS_DHCP_PASSWORD = "testKEYS";

	public static final String MQTT_BROKER_URL = "mqtt.brokerUrl";
	public static final String MQTT_ACCOUNT_NAME = "mqtt.accountName";
	public static final String MQTT_USERNAME = "mqtt.username";
	public static final String MQTT_PASSWORD = "mqtt.password";
	public static final String MQTT_CLIENT_ID = "mqtt.clientId";

	public static final int FIREWALL_OPEN_PORT_INTERFACE_COUNT = 4;

	public static final boolean ENV_OUTPUT = true;
}
