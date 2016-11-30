package com.eurotech.denali.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;

import com.eurotech.denali.util.Constants;

public class Application {

	private static Properties properties = new Properties();

	private Application() {
	}

	public static Properties getProperties() {
		if (properties.isEmpty()) {
			Properties defaultProperties = loadPropertiesFromFile(Constants.PROPERTIES_FILE);
			properties.putAll(defaultProperties);
		}
		return properties;
	}

	private static void updateProperty(String key, String value) {
		try {
			PropertiesConfiguration configuration = new PropertiesConfiguration(
					Constants.PROPERTIES_FILE);
			configuration.setProperty(key, value);
			configuration.save();
			properties.clear();
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Properties loadPropertiesFromFile(String filePath) {
		Properties theProperties = new Properties();
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(filePath);
			theProperties.load(fs);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(fs);
		}
		return theProperties;
	}

	public static String getProperty(String name) {
		return getProperties().getProperty(name);
	}

	public static String getDenaliUsername() {
		return getProperty(Constants.DENALI_USERNAME);
	}

	public static String getDenaliPassword() {
		return getProperty(Constants.DENALI_PASSWORD);
	}

	public static String getDenaliNewPassword() {
		return getProperty(Constants.DENALI_NEW_PASSWORD);
	}

	public static String getDeviceIP() {
		String ip = getDenaliAppURL();
		int beginIndex = ip.indexOf("/") + 2;
		int endIndex = ip.lastIndexOf("/");
		return ip.substring(beginIndex, endIndex);
	}

	public static String getDeviceUsername() {
		return getProperty(Constants.DEVICE_USERNAME);
	}

	public static String getDevicePassword() {
		return getProperty(Constants.DEVICE_PASSWORD);
	}

	public static String getDenaliAppURL() {
		return getProperty(Constants.DENALI_IP);
	}

	public static String getDenaliAppWithDefaultCredential() {
		return getDenaliAppWithCustomCredential(getDenaliUsername(),
				getDenaliPassword());
	}

	public static String getDenaliAppWithCustomCredential(String userName,
			String password) {
		String ip = getProperty(Constants.DENALI_IP);
		int index = ip.indexOf("/") + 2;
		StringBuilder builder = new StringBuilder();
		builder.append(ip.substring(0, index)).append(userName).append(":")
				.append(password).append("@").append(ip.substring(index));
		return builder.toString();
	}

	public static String getEDCAppIP() {
		return getProperty(Constants.EDC_IP);
	}

	public static String getEDCUserName() {
		return getProperty(Constants.EDC_USERNAME);
	}

	public static String getEDCPassword() {
		return getProperty(Constants.EDC_PASSWORD);
	}

	public static String getChromeDriverLocation() {
		StringBuilder chromePath = new StringBuilder(getCurrentLibLocation());
		if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			chromePath.append("windows").append(File.separatorChar)
					.append("googlechrome").append(File.separatorChar)
					.append("32bit").append(File.separatorChar).append("2.12")
					.append(File.separatorChar).append("chromedriver.exe");
		} else if (System.getProperty("os.arch").contains("64")) {
			chromePath.append("linux").append(File.separatorChar)
					.append("googlechrome").append(File.separatorChar)
					.append("64bit").append(File.separatorChar).append("2.12")
					.append(File.separatorChar).append("chromedriver");
		} else {
			chromePath.append("linux").append(File.separatorChar)
					.append("googlechrome").append(File.separatorChar)
					.append("32bit").append(File.separatorChar).append("2.12")
					.append(File.separatorChar).append("chromedriver");
		}
		return chromePath.toString();
	}

	public static String getIEDriverLocation() {
		StringBuilder iePath = new StringBuilder(getCurrentLibLocation());
		if (System.getenv("ProgramFiles(x86)") == null) {
			iePath.append("windows").append(File.separatorChar)
					.append("internetexplorer").append(File.separatorChar)
					.append("32bit").append(File.separatorChar)
					.append("2.44.0").append(File.separatorChar)
					.append("IEDriverServer.exe");
		} else if (System.getProperty("os.arch").contains("64")) {
			iePath.append("windows").append(File.separatorChar)
					.append("internetexplorer").append(File.separatorChar)
					.append("64bit").append(File.separatorChar)
					.append("2.44.0").append(File.separatorChar)
					.append("IEDriverServer.exe");
		} else {
			throw new RuntimeException(
					"Internet Explorer browser only usable on windows platform.");
		}
		return iePath.toString();
	}

	private static String getCurrentLibLocation() {
		return new StringBuilder(getCurrentLocation())
				.append(File.separatorChar).append("lib")
				.append(File.separatorChar).toString();
	}

	public static String getCurrentLocation() {
		return System.getProperty("user.dir");
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase()
				.startsWith("windows") ? true : false;
	}

	public static String getNetworkEth1DHCPIPAddress() {
		return getProperty(Constants.NETWORK_TCP_ETH1_DHCP_IP_ADDRESS);
	}

	public static String getNetworkEth0DHCPIPAddress() {
		return getProperty(Constants.NETWORK_TCP_ETH0_DHCP_IP_ADDRESS);
	}

	public static String getNetworkEth0StaticIPAddress() {
		return getProperty(Constants.NETWORK_TCP_ETH0_STATIC_IP_ADDRESS);
	}

	public static String getNetworkEth0StaticSubnetMask() {
		return getProperty(Constants.NETWORK_TCP_ETH0_STATIC_SUBNET_MASK);
	}

	public static String getNetworkEth0StaticGateway() {
		return getProperty(Constants.NETWORK_TCP_ETH0_STATIC_GATEWAY);
	}

	public static String getNetworkEth0StaticDNSServer() {
		return getProperty(Constants.NETWORK_TCP_ETH0_STATIC_DNS_SERVER);
	}

	public static String getNetworkEth0StaticSubnetMaskSecondary() {
		return getProperty(Constants.NETWORK_TCP_ETH0_STATIC_SUBNET_MASK_SECONDARY);
	}

	public static String getNetworkEth0StaticDNSServerSecondary() {
		return getProperty(Constants.NETWORK_TCP_ETH0_STATIC_DNS_SERVER_SECONDARY);
	}

	public static String getNetworkDHCPEth0BeginAddress() {
		return getProperty(Constants.NETWORK_DHCP_ETH0_BEGIN_ADDRESS);
	}

	public static String getNetworkDHCPEth0EndAddress() {
		return getProperty(Constants.NETWORK_DHCP_ETH0_END_ADDRESS);
	}

	public static String getNetworkDHCPEth0SubnetMask() {
		return getProperty(Constants.NETWORK_DHCP_ETH0_SUBNET_MASK);
	}

	public static String getNetworkDHCPEth0DefaultLeaseTime() {
		return getProperty(Constants.NETWORK_DHCP_ETH0_DEFAULT_LEASE_TIME);
	}

	public static String getNetworkDHCPEth0MaxLeaseTime() {
		return getProperty(Constants.NETWORK_DHCP_ETH0_MAX_LEASE_TIME);
	}

	public static String getNetworkWlan0DHCPIPAddress() {
		return getProperty(Constants.NETWORK_TCP_WLAN0_DHCP_IP_ADDRESS);
	}

	public static String getNetworkWlan0StaticIPAddress() {
		return getProperty(Constants.NETWORK_TCP_WLAN0_STATIC_IP_ADDRESS);
	}

	public static String getNetworkWlan0StaticSubnetMask() {
		return getProperty(Constants.NETWORK_TCP_WLAN0_STATIC_SUBNET_MASK);
	}

	public static String getNetworkDHCPWlan0BeginAddress() {
		return getProperty(Constants.NETWORK_DHCP_WLAN0_BEGIN_ADDRESS);
	}

	public static String getNetworkDHCPWlan0EndAddress() {
		return getProperty(Constants.NETWORK_DHCP_WLAN0_END_ADDRESS);
	}

	public static String getNetworkDHCPWlan0SubnetMask() {
		return getProperty(Constants.NETWORK_DHCP_WLAN0_SUBNET_MASK);
	}

	public static String getNetworkDHCPWlan0DefaultLeaseTime() {
		return getProperty(Constants.NETWORK_DHCP_WLAN0_DEFAULT_LEASE_TIME);
	}

	public static String getNetworkDHCPWlan0MaxLeaseTime() {
		return getProperty(Constants.NETWORK_DHCP_WLAN0_MAX_LEASE_TIME);
	}

	public static String getNetworkWirelessName() {
		return getProperty(Constants.NETWORK_WIRELESS_NAME);
	}

	public static String getNetworkWirelessPassword() {
		return getProperty(Constants.NETWORK_WIRELESS_PASSWORD);
	}

	public static String getNetworkWirelessSecurityType() {
		return getProperty(Constants.NETWORK_WIRELESS_SECURITY_TYPE);
	}

	public static void updateDenaliIPInProperty(String value) {
		StringBuilder url = new StringBuilder();
		url.append("http://").append(value);
		if (isKura()) {
			url.append("/kura");
		} else {
			url.append("/esf");
		}
		updateProperty(Constants.DENALI_IP, url.toString());
	}

	public static void updateDenaliDefaultPassword(String value) {
		updateProperty(Constants.DENALI_PASSWORD, value);
	}

	public static void generateAndUpdateNewDenaliPassword(String value) {
		updateProperty(Constants.DENALI_NEW_PASSWORD,
				Constants.NEW_CONSTANT_PASSWORD + value);
	}

	public static String getMQTTBrokerURL() {
		return getProperty(Constants.MQTT_BROKER_URL);
	}

	public static String getMQTTAccountName() {
		return getProperty(Constants.MQTT_ACCOUNT_NAME);
	}

	public static String getMQTTUsername() {
		return getProperty(Constants.MQTT_USERNAME);
	}

	public static String getMQTTPassword() {
		return getProperty(Constants.MQTT_PASSWORD);
	}

	public static String getMQTTClientID() {
		return getProperty(Constants.MQTT_CLIENT_ID);
	}

	public static boolean isKura() {
		return getDenaliAppURL().contains("kura") ? true : false;
	}
}
