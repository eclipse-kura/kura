package com.eurotech.denali.test;

import java.util.List;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.eurotech.Edc.EdcMainPage;
import com.eurotech.denali.comm.ConsoleFactory;
import com.eurotech.denali.common.Application;
import com.eurotech.denali.driver.processor.DriverProcessor;
import com.eurotech.denali.objectrepo.BrowserRepository;
import com.eurotech.denali.util.Constants;
import com.eurotech.denali.util.JvmUtil;

public class WifiTest extends DriverProcessor implements BrowserRepository {

	@BeforeClass
	public void beforeClass() {
		Reporter.log("Verifying the Wifi functionalities in "
				+ Constants.FIREFOX + " browser", Constants.ENV_OUTPUT);
	}

	@BeforeMethod
	public void beforeMethod() {
		startDriver();
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
		networkView.selectNetworkView();
		if (!networkView.isWlan0InterfacePresent()) {
			quitDriver();
			throw new SkipException(
					"wlan0 interface is not present for the device");
		}
	}

	@Test
	public void configureWifiAsDHCPServer() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-WIFI-001", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Configure Wifi as access point", Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		networkView.selectWlan0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectTCPEnabledForLAN();
		networkView.selectTCPManually();
		networkView.setNetworkWlan0IPAddress();
		networkView.setNetworkWlan0SubnetMask();
		networkView.clearNetworkDNSServer();
		networkView.clearNetworkGateway();
		Assert.assertTrue(networkView.isWirelessTabEnabled());
		networkView.selectWirelessTab();
		networkView.selectWirelessModeAccessPoint();
		networkView.selectWirelessRatioModeGB();
		networkView.selectWirelessSecurityWPA2();
		networkView.setWirelessDHCPNetworkName();
		networkView.setWirelessDHCPNetworkPassword();
		networkView.setWirelessDHCPNetworkVerifyPassword();
		Assert.assertTrue(networkView.isDHCPNATTabPresent());
		Assert.assertTrue(networkView.isDHCPNATTabEnabled());
		networkView.selectDHCPNATTab();
		networkView.selectDHCPRouterDHCPAndNAT();
		networkView.setDHCPWlan0BeginAddress();
		networkView.setDHCPWlan0EndAddress();
		networkView.setDHCPWlan0SubnetMask();
		networkView.setDHCPWlan0DeafultLeaseTime();
		networkView.setDHCPWlan0MaxLeaseTime();
		networkView.selectDHCPPassDNSServer();
		networkView.clickApplyButton();

		networkView.selectNetworkView();
		ConsoleFactory factory = ConsoleFactory.getInstance(Application
				.getNetworkWlan0StaticIPAddress());
		Reporter.log(
				"Wlan0 Static IP address "
						+ Application.getNetworkWlan0StaticIPAddress(),
				Constants.ENV_OUTPUT);
		boolean connected = JvmUtil.waitForOSGIServices(factory);
		if (!connected) {
			Assert.fail("Applying Wlan0 network configuration to manual is not successful...");
		}
		Reporter.log(
				"Configuring IP address to manually for Eth1 done successfully",
				Constants.ENV_OUTPUT);
		factory.connectShell();
		Assert.assertTrue(factory.isDHCPDServiceRunning(),
				"DHCPD service is not running");
		Assert.assertTrue(factory.isBindServiceRunning(),
				"Bind(named) service is not running");
	}

	@Test(dependsOnMethods = { "configureWifiAsDHCPServer" }, alwaysRun = true)
	public void configureWifi() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-WIFI-002 -003", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Configure Wifi as access point", Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		networkView.selectEth1Interface();
		networkView.selectTCPEnabledForLAN();
		networkView.selectTCPManually();
		networkView.setNetworkEth1IPAddress();
		networkView.setNetworkEth0SubnetMask();
		networkView.setNetworkGateway();
		networkView.setNetworkDNSServer();
		JvmUtil.idle(2);
		networkView.clickApplyButton();
		JvmUtil.idle(5);
		forceQuitDriver();
		ConsoleFactory factory = ConsoleFactory.getInstance();
		Reporter.log(
				"Changing eth1 interface to static ip "
						+ Application.getNetworkEth1DHCPIPAddress(),
				Constants.ENV_OUTPUT);
		boolean connected = JvmUtil.waitForOSGIServices(factory);
		if (!connected) {
			Assert.fail("Applying Eth1 network configuration to manual is not successful...");
		}
		Reporter.log(
				"Configuring IP address to manually for Eth1 done successfully",
				Constants.ENV_OUTPUT);
		startDriver();
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
		Reporter.log(
				"Logged into denali successfully with newly updated IP address",
				Constants.ENV_OUTPUT);

		networkView.selectNetworkView();
		networkView.selectWlan0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectTCPEnabledForWAN();
		networkView.selectTCPDHCP();
		Assert.assertTrue(networkView.isWirelessTabEnabled());
		networkView.selectWirelessTab();
		networkView.selectWirelessModeStation();
		networkView.selectWirelessSecurity();
		networkView.setWirelessNetworkName();
		networkView.setWirelessNetworkPassword();
		networkView.clickWirelessPasswordVerifyButton();
		int i = 0;
		boolean isVerifying = networkView
				.getWirelessPasswordVerifyButtonState();
		while (i++ < 60 && isVerifying) {
			if (!isVerifying) {
				break;
			}
			JvmUtil.idle(5);
			isVerifying = networkView.getWirelessPasswordVerifyButtonState();
		}
		Assert.assertFalse(isVerifying,
				"Verifying wireless password is taking too long");
		if (networkView.getWirelessNetworkPassword().isEmpty())
			Assert.fail("You have entered the invalid password for wireless connection");
		networkView.clickApplyButton();

		factory = ConsoleFactory.getInstance(Application
				.getNetworkWlan0DHCPIPAddress());
		connected = JvmUtil.waitForOSGIServices(factory);
		if (!connected) {
			Assert.fail("Unable to connect to wlan0 in DHCP mode after enabling wireless lan");
		}
		Application.updateDenaliIPInProperty(Application
				.getNetworkWlan0DHCPIPAddress());

		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());

		factory.connectShell();
		String ipAddress = factory.getWlan0IPAddress();
		Assert.assertEquals(Application.getNetworkWlan0DHCPIPAddress(),
				ipAddress);
		Reporter.log(
				"The wlan0 IP address verification is done using ifconfig command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the route command should show a both an appropriate network entry and the gateway entry",
				Constants.ENV_OUTPUT);
		String gatewayEntry = factory.getWlan0GatewayEntry();
		Assert.assertFalse(gatewayEntry.isEmpty(),
				"The gateway entry is not obtained for wlan0 port");
		List<String> dnsServerList = factory.getDNSServers();
		Assert.assertFalse(dnsServerList.isEmpty(),
				"The DNS server is not obtained for wlan0 port");
		Reporter.log("The DNS server is obtained for wlan0 port in DHCP mode",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "configureWifi" }, alwaysRun = true)
	public void connectMQTT() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-WIFI-004", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Configure Wifi as station mode and configure mqtt",
				Constants.ENV_OUTPUT);
		mqttView.selectMQTTTransportView();
		mqttView.setMQTTBrokerURL();
		mqttView.setMQTTAccountName();
		mqttView.setMQTTUsername();
		mqttView.setMQTTPassword();
		mqttView.setMQTTClientID();
		// wait to change the state of the cursor
		JvmUtil.idle(2);
		mqttView.clickApplyButton();
		if (mqttView.isConfirmationPopUpPresent()) {
			mqttView.clickConfirmYesButton();
			Assert.assertTrue(mqttView.isConfirmationPresent(),
					"Confirmation message is not displayed");
			Reporter.log(
					"MqttDataTransport is configured to sandbox/stage broker account with correct credentials",
					Constants.ENV_OUTPUT);
		} else {
			Reporter.log(
					"MqttDataTransport is already configured to sandbox/stage broker account with correct credentials",
					Constants.ENV_OUTPUT);
		}

		Reporter.log(
				"Configuring DataService to enable connect.aut-on-startup",
				Constants.ENV_OUTPUT);
		dataServiceView.selectDataServiceView();
		dataServiceView.selectAutoStartupToTrue();
		// wait to change the state of the cursor
		JvmUtil.idle(2);
		dataServiceView.clickApplyButton();
		if (dataServiceView.isConfirmationPopUpPresent()) {
			dataServiceView.clickConfirmYesButton();
			Assert.assertTrue(dataServiceView.isConfirmationPresent(),
					"DataService Confirmation is not applied");
			Reporter.log("connect.auto-on-startup is enabled in DataService",
					Constants.ENV_OUTPUT);
		} else {
			Reporter.log(
					"connect.auto-on-startup is already enabled in DataService",
					Constants.ENV_OUTPUT);
		}
		JvmUtil.idle(60);
		// wait to update the device in console
		Reporter.log(
				"Waiting to update the device in EC Sandbox/Stage console",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the gateway connects correctly to EC on sandbox/stage console",
				Constants.ENV_OUTPUT);
		EdcMainPage edcMainPage = edcLoginPage.login();
		Reporter.log("Logged into EC sandbox/stage console successfully",
				Constants.ENV_OUTPUT);
		int i = 0;
		edcMainPage.selectEdcDevices();
		Assert.assertTrue(edcMainPage.isDeviceListed(),
				"The client-id is not listed in the console");
		String deviceStatus = edcMainPage.getDeviceStatus();
		while (i++ < 20) {
			if (deviceStatus.equals(Constants.EDC_DEVICE_CONNECTED_STATUS)) {
				break;
			}
			Reporter.log(
					"Waiting to update the device connection in EC Sandbox/Stage console",
					Constants.ENV_OUTPUT);
			JvmUtil.idle(10);
			edcMainPage.refreshDeviceStatus();
			deviceStatus = edcMainPage.getDeviceStatus();
		}
		Assert.assertEquals(deviceStatus,
				Constants.EDC_DEVICE_CONNECTED_STATUS, "Device status is : "
						+ deviceStatus);
		Reporter.log("The gateway " + Application.getMQTTClientID()
				+ " is connected to the EC sandbox/stage console successfully",
				Constants.ENV_OUTPUT);
		edcMainPage.logOut();
	}

	@Test(dependsOnMethods = { "connectMQTT" }, alwaysRun = true)
	public void verifyReconnectConsole() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-WIFI -005", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log(
				"Rebooting the gateway to verify it reconnects on EC console",
				Constants.ENV_OUTPUT);
		ConsoleFactory factory = ConsoleFactory.getInstance();
		factory.connectShell();
		factory.rebootDevice();
		factory.close();
		Reporter.log("Rebooting the gateway done successfully",
				Constants.ENV_OUTPUT);
		Reporter.log("Waiting to start the Kura/ESF services in gateway",
				Constants.ENV_OUTPUT);
		if (!JvmUtil.waitForOSGIServices(factory)) {
			Assert.fail("Unable to connect the gateway after restart");
		}
		Reporter.log("OSGI services is started in gateway successfully",
				Constants.ENV_OUTPUT);
		EdcMainPage edcMainPage = edcLoginPage.login();
		Reporter.log(
				"Logged into EC sandbox/stage console to verify the device status",
				Constants.ENV_OUTPUT);
		edcMainPage.selectEdcDevices();
		Assert.assertTrue(edcMainPage.isDeviceListed());
		String deviceStatus = edcMainPage.getDeviceStatus();
		Assert.assertEquals(deviceStatus,
				Constants.EDC_DEVICE_CONNECTED_STATUS,
				"Device status in console is : " + deviceStatus);
		Reporter.log("Device status in console is " + deviceStatus,
				Constants.ENV_OUTPUT);
		edcMainPage.logOut();
	}

	@AfterMethod
	public void afterMethod() {
		quitDriver();
	}
}
