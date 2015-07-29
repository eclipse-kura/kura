package com.eurotech.denali.objectrepo;

import org.openqa.selenium.By;

import com.eurotech.denali.common.Application;

public interface BrowserRepository {
	
	public enum WirelessSecurityType{
		none("None"), wpa("WPA"), wep("WEP"), wpa2("WPA/WPA2");

		private String type;

		WirelessSecurityType(String type) {
			this.type = type;
		}

		public By getWirelessType() {
			return By.xpath(NETWORK_WIRELESS_SECURITY_TYPE.replace(VALUE,
					this.type));
		}
	}
	
	// Denali
	// status tab
	By STATUS = By.xpath("//label[contains(text(),'Status')]");
	By STATUS_CLOUD_SERVICE_INFO_HEADER = By.xpath("//div/div[contains(text(),'Cloud and Data Service')]/parent::div/following-sibling::div//div//td[1]/div");
	By STATUS_CLOUD_SERVICE_INFO_VALUE = By.xpath("//div/div[contains(text(),'Cloud and Data Service')]/parent::div/following-sibling::div//div//td[2]/div");
	By STATUS_ETHERNET_INFO_HEADER = By.xpath("//div/div[contains(text(),'Ethernet ')]/parent::div/following-sibling::div//div//td[1]/div");
	By STATUS_ETHERNET_INFO_VALUE = By.xpath("//div/div[contains(text(),'Ethernet ')]/parent::div/following-sibling::div//div//td[2]/div");
	By STATUS_POSITION_INFO_HEADER = By.xpath("//div/div[contains(text(),'Position')]/parent::div/following-sibling::div//div//td[1]/div");
	By STATUS_POSITION_INFO_VALUE = By.xpath("//div/div[contains(text(),'Position')]/parent::div/following-sibling::div//div//td[2]/div");
	
	// Device tab
	By DEVICE = By.xpath("//label[contains(text(),'Device')]");
	By DEVICE_PROFILE = By.xpath("//span[contains(text(),'Profile')]");
	By DEVICE_BUNDLES = By.xpath("//span[contains(text(),'Bundles')]");
	By DEVICE_THREADS = By.xpath("//span[contains(text(),'Threads')]");
	By DEVICE_SYSTEM_PROPERTY = By.xpath("//span[contains(text(),'System Properties')]");
	By DEVICE_COMMAND = By.xpath("//span[contains(text(),'Command')]");
	By DEVICE_PROFILE_DEVICE_INFO_HEADER = By.xpath("//div/div[contains(text(),'Device Information')]/parent::div/following-sibling::div//div//td[1]/div");
	By DEVICE_PROFILE_DEVICE_INFO_VALUE = By.xpath("//div/div[contains(text(),'Device Information')]/parent::div/following-sibling::div//div//td[2]/div");
	By DEVICE_PROFILE_GPS_INFO_HEADER = By.xpath("//div/div[contains(text(),'GPS Information')]/parent::div/following-sibling::div//div//td[1]/div");
	By DEVICE_PROFILE_GPS_INFO_VALUE = By.xpath("//div/div[contains(text(),'GPS Information')]/parent::div/following-sibling::div//div//td[2]/div");
	By DEVICE_PROFILE_HARDWARE_INFO_HEADER = By.xpath("//div/div[contains(text(),'Hardware Information')]/parent::div/following-sibling::div//div//td[1]/div");
	By DEVICE_PROFILE_HARDWARE_INFO_VALUE = By.xpath("//div/div[contains(text(),'Hardware Information')]/parent::div/following-sibling::div//div//td[2]/div");
	By DEVICE_PROFILE_SOFTWARE_INFO_HEADER = By.xpath("//div/div[contains(text(),'Software Information')]/parent::div/following-sibling::div//div//td[1]/div");
	By DEVICE_PROFILE_SOFTWARE_INFO_VALUE = By.xpath("//div/div[contains(text(),'Software Information')]/parent::div/following-sibling::div//div//td[2]/div");
	By DEVICE_PROFILE_JAVA_INFO_HEADER = By.xpath("//div/div[contains(text(),'Java Information')]/parent::div/following-sibling::div//div//td[1]/div");
	By DEVICE_PROFILE_JAVA_INFO_VALUE = By.xpath("//div/div[contains(text(),'Java Information')]/parent::div/following-sibling::div//div//td[2]/div");
	By DEVICE_PROFILE_NETWORK_INFO_HEADER = By.xpath("//div/div[contains(text(),'Network Information')]/parent::div/following-sibling::div//div//td[1]/div");
	By DEVICE_PROFILE_NETWORK_INFO_VALUE = By.xpath("//div/div[contains(text(),'Network Information')]/parent::div/following-sibling::div//div//td[2]/div");
	
	// Network tab
	By NETWORK = By.xpath("//label[contains(text(),'Network')]");
	By NETWORK_CHANGES = By.xpath("//div[contains(@class,'x-grid3-dirty-cell')]");
	By NETWORK_APPLY_BUTTON = By.xpath("//button[contains(text(),'Apply')]");
	By NETWORK_REFRESH_BUTTON = By.xpath("//button[contains(text(),'Refresh')]");
	By NETWORK_LO = By.xpath("//div[contains(text(),'lo')]");
	By NETWORK_ETH0 = By.xpath("//div[contains(@class,'x-grid')]//div[contains(text(),'eth0')]");
	By NETWORK_ETH1 = By.xpath("//div[contains(@class,'x-grid')]//div[contains(text(),'eth1')]");
	By NETWORK_WLAN0 = By.xpath("//div[contains(@class,'x-grid')]//div[contains(text(),'wlan0')]");
	By NETWORK_INTERFACE_TCPIP = By.xpath("//span[contains(text(),'TCP/IP')]");
	By NETWORK_INTERFACE_WIRELESS = By.xpath("//span[contains(text(),'Wireless')]");
	By NETWORK_INTERFACE_DHCP_NAT = By.xpath("//span[contains(text(),'DHCP & NAT')]");
	By NETWORK_INTERFACE_HARDWARE = By.xpath("//span[contains(text(),'Hardware')]");
	By NETWORK_INTERFACE_STATE = By.xpath("//label[contains(text(),'State')]/following-sibling::div/div");
	By NETWORK_INTERFACE_NAME = By.xpath("//label[contains(text(),'Name')]/following-sibling::div/div");
	By NETWORK_INTERFACE_TYPE = By.xpath("//label[contains(text(),'Type')]/following-sibling::div/div");
	By NETWORK_TCP_STATUS = By.name("comboStatus");
	By NETWORK_TCP_CONFIGURATION = By.name("comboConfigure");
	By NETWORK_TCP_STATUS_DROPDOWN_IMG = By.xpath("//label[contains(text(),'Status:')]/following-sibling::div//img");
	By NETWORK_TCP_STATUS_ENABLED_LAN = By.xpath("//div[@qtitle='Enabled for LAN']");
	By NETWORK_TCP_STATUS_ENABLED_WAN = By.xpath("//div[@qtitle='Enabled for WAN']");
	By NETWORK_TCP_STATUS_DISABLED = By.xpath("//div[@qtitle='Disabled']");
	By NETWORK_TCP_CONFIGURE_DROPDOWN_IMG = By.xpath("//label[contains(text(),'Configure:')]/following-sibling::div//img");
	By NETWORK_TCP_CONFIGURE_DHCP = By.xpath("//div[contains(@class,'x-combo-list')]//div[contains(text(),'Using DHCP')]");
	By NETWORK_TCP_CONFIGURE_MANUALLY = By.xpath("//div[contains(text(),'Manually')]");
	By NETWORK_TCP_IP_ADDRESS = By.name("ipAddress");
	By NETWORK_TCP_SUBNET_MASK = By.name("subnetMask");
	By NETWORK_TCP_GATEWAY = By.name("gateway");
	By NETWORK_TCP_DNS_SERVER = By.name("dnsServers");
	By NETWORK_TCP_SEARCH_DOMAINS = By.name("searchDomains");
	By NETWORK_DHCP_STATUS_DROPDOWN_IMG = By.xpath("//label[contains(text(),'Router Mode:')]/following-sibling::div//img");
	By NETWORK_DHCP_ROUTER_DHCP_NAT = By.xpath("//div[contains(text(),'DHCP and NAT')]");
	By NETWORK_DHCP_ROUTER_OFF = By.xpath("//div[contains(text(),'Off')]");
	By NETWORK_DHCP_BEGIN_ADDR = By.name("dhcpBeginAddress");
	By NETWORK_DHCP_END_ADDR = By.name("dhcpEndAddress");
	By NETWORK_DHCP_SUBNET_MASK = By.name("dhcpSubnetMask");
	By NETWORK_DHCP_DEFAULT_LEASE_TIME = By.name("dhcpDefaultLease");
	By NETWORK_DHCP_MAX_LEASE_TIME = By.name("dhcpMaxLease");
	By NETWORK_DHCP_PASS_DNS_SERVER_TRUE = By.xpath("//label[contains(text(),'Pass DNS Servers through DHCP:')]/following-sibling::div//label[contains(text(),'true')]/preceding-sibling::input");
	By NETWORK_WIRELESS_MODE_DROPDOWN_IMG = By.xpath("//label[contains(text(),'Wireless Mode:')]/following-sibling::div//img");
	By NETWORK_WIRELESS_STATION_MODE = By.xpath("//div[@qtitle='Station Mode']");
	By NETWORK_WIRELESS_ACCESS_POINT = By.xpath("//div[@qtitle='Access Point']");
	By NETWORK_WIRELESS_NAME = By.name("ssid");
	By NETWORK_WIRELESS_RATIO_MODE_DROPDOWN_IMG = By.xpath("//label[contains(text(),'Radio Mode:')]/following-sibling::div//img");
	By NETWORK_WIRELESS_RATIO_GB_VALUE = By.xpath("//div[contains(text(),'802.11g/b')]");
	By NETWORK_WIRELESS_SECURITY_DROPDOWN_IMG = By.xpath("//label[contains(text(),'Wireless Security:')]/following-sibling::div//img");
	String VALUE = "value";
	String NETWORK_WIRELESS_SECURITY_TYPE = "//div[contains(text(),'"+VALUE+"')]";
	By NETWORK_WIRELESS_PASSWORD = By.name("password");
	By NETWORK_WIRELESS_VERIFY_PASSWORD = By.name("verifyPassword");
	By NETWORK_WIRELESS_PASSWORD_VERIFY_BUTTON = By.xpath("//input[@name='password']/following-sibling::table//button");
	String NETWORK_WIRELESS_PASSWORD_VERIFY_BUTTON_ATTRIBUTE = "aria-disabled";
	
	// Firewall Tab
	By FIREWALL = By.xpath("//label[contains(text(),'Firewall')]");
	By FIREWALL_OPEN_PORTS = By.xpath("//span[contains(text(),'Open Ports')]");
	By FIREWALL_PORT_FORWARD = By.xpath("//span[contains(text(),'Port Forwarding')]");
	By FIREWALL_NAT = By.xpath("//span[contains(text(),'IP Forwarding/Masquerading')]");
	By FIREWALL_OPEN_PORT_APPLY_BUTTON = By.xpath("//div[@id='firewall-open-ports-toolbar']//button[contains(text(),'Apply')]");
	By FIREWALL_OPEN_PORT_NEW_BUTTON = By.xpath("//div[@id='firewall-open-ports-toolbar']//button[contains(text(),'New')]");
	By FIREWALL_OPEN_PORT_EDIT_BUTTON = By.xpath("//div[@id='firewall-open-ports-toolbar']//button[contains(text(),'Edit')]");
	By FIREWALL_OPEN_PORT_DELETE_BUTTON = By.xpath("//div[@id='firewall-open-ports-toolbar']//button[contains(text(),'Delete')]");
	By FIREWALL_OPEN_PORT_ETH0_INTERFACE = By.xpath("//div[contains(text(),'eth0')]");
	By FIREWALL_OPEN_PORT_ETH1_INTERFACE = By.xpath("//div[contains(text(),'eth1')]");
	By FIREWALL_OPEN_PORT_WLAN_INTERFACE = By.xpath("//div[contains(text(),'wlan')]");
	By FIREWALL_PORT_FORWARD_APPLY_BUTTON = By.xpath("//div[@id='firewall-port-forwarding-toolbar']//button[contains(text(),'Apply')]");
	By FIREWALL_PORT_FORWARD_NEW_BUTTON = By.xpath("//div[@id='firewall-port-forwarding-toolbar']//button[contains(text(),'New')]");
	By FIREWALL_PORT_FORWARD_EDIT_BUTTON = By.xpath("//div[@id='firewall-port-forwarding-toolbar']//button[contains(text(),'Edit')]");
	By FIREWALL_PORT_FORWARD_DELETE_BUTTON = By.xpath("//div[@id='firewall-port-forwarding-toolbar']//button[contains(text(),'Delete')]");
	By FIREWALL_NAT_APPLY_BUTTON = By.xpath("//div[@id='nat-toolbar']//button[contains(text(),'Apply')]");
	By FIREWALL_NAT_NEW_BUTTON = By.xpath("//div[@id='nat-toolbar']//button[contains(text(),'New')]");
	By FIREWALL_NAT_EDIT_BUTTON = By.xpath("//div[@id='nat-toolbar']//button[contains(text(),'Edit')]");
	By FIREWALL_NAT_DELETE_BUTTON = By.xpath("//div[@id='nat-toolbar']//button[contains(text(),'Delete')]");
	
	// Packages tab
	By PACKAGES = By.xpath("//label[contains(text(),'Packages')]");
	By PACKAGES_REFRESH_BUTTON = By.xpath("//div//button[contains(text(),'Refresh')]");
	By PACKAGES_INSTALL_BUTTON = By.xpath("//div//button[contains(text(),'Install/Upgrade')]");
	By PACKAGES_UNINSTALL_BUTTON = By.xpath("//div//button[contains(text(),'Uninstall')]");
	
	// Setting tab
	By SETTINGS = By.xpath("//label[contains(text(),'Settings')]");
	By SETTINGS_APPLY = By.xpath("//div[@id='settings-admin-password']//button[contains(text(),'Apply')]");
	By SETTINGS_ADMIN_PASSWORD = By.xpath("//span[contains(text(),'Admin Password')]");
	By SETTINGS_CURRENT_PASSWORD = By.name("currentPassword");
	By SETTINGS_NEW_PASSWORD = By.name("newPassword");
	By SETTINGS_CONFIRM_PASSWORD = By.name("confirmPassword");
	By SETTING_CONFIRMATION = By.xpath("//div//span[contains(text(),'Confirmation')]");
	By SETTINGS_SNAPSHOT_REFRESH_BUTTON = By.xpath("//button[contains(text(),'Refresh')]");
	By SETTINGS_SNAPSHOT_DOWNLOAD_BUTTON = By.xpath("//button[contains(text(),'Download')]");
	By SETTINGS_SNAPSHOT_ROLLBACK_BUTTON = By.xpath("//button[contains(text(),'Rollback')]");
	By SETTINGS_SNAPSHOT_APPLY_BUTTON = By.xpath("//button[contains(text(),'Apply')]");
	
	// MQTT Data transport view
	By MQTT = By.xpath("//label[contains(text(),'MqttDataTransport')]");
	By MQTT_CHANGES = By.xpath("//div[contains(@class,'x-grid3-dirty-cell')]");
	By MQTT_BROKER_URL = By.name("broker-url");
	By MQTT_ACCOUNT_NAME = By.name("topic.context.account-name");
	By MQTT_USERNAME = By.name("username");
	By MQTT_PASSWORD = By.name("password");
	By MQTT_CLIENT_ID = By.name("client-id");
	By MQTT_APPLY_BUTTON = By.xpath("//button[contains(text(),'Apply')]");
	By MQTT_CONFIRM_YES_BUTTON = By.xpath("//button[contains(text(),'Yes')]");
	By MQTT_CONFIRMATION = By.xpath("//div//span[contains(text(),'Confirmation')]");
	
	// Data Service view
	By DATA_SERVICE = By.xpath("//label[contains(text(),'DataService')]");
	By DATA_SERVICE_CHANGES = By.xpath("//div[contains(@class,'x-grid3-dirty-cell')]");
	By DATA_SERVICE_AUTO_START_UP_TRUE = By.xpath("//label[contains(text(),'connect.auto-on-startup')]/following-sibling::div/div//label[contains(text(),'true')]/preceding-sibling::input");
	By DATA_SERVICE_APPLY_BUTTON = By.xpath("//button[contains(text(),'Apply')]");
	By DATA_SERVICE_CONFIRM_YES_BUTTON = By.xpath("//button[contains(text(),'Yes')]");
	By DATA_SERVICE_CONFIRMATION = By.xpath("//div//span[contains(text(),'Confirmation')]");
	
	By CONFIRM_POP_UP = By.xpath("//span[contains(text(),'Confirm')]");
	
	// EveryWhere cloud
	By EDC_USERNAME = By.xpath("//div/label[contains(text(),'Username')]/following-sibling::div//input");
	By EDC_PASSWORD = By.xpath("//div/label[contains(text(),'Password')]/following-sibling::div//input");
	By EDC_LOGIN_BUTTON = By.xpath("//button[contains(text(),'Login')]");
	By EDC_REFRESH_DEVICE_STATUS = By.xpath("//button[contains(text(),'Refresh')]");
	By EDC_DEVICE_STATUS = By.xpath("//table//td/div[contains(text(),'"+Application.getMQTTClientID()+"')]/parent::td/preceding-sibling::td/div/img");
	By EDC_DEVICE_CLIENTID = By.xpath("//table//td/div[contains(text(),'"+Application.getMQTTClientID()+"')]");	
	By EDC_LOGOUT = By.xpath("//a[contains(text(),'Logout')]");	
}
