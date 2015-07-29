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

public class Ethernet1Test extends DriverProcessor implements BrowserRepository {

	@BeforeClass
	public void beforeClass() {
		Reporter.log("Verifying the Ethernet eth1 functionalities in "
				+ Constants.FIREFOX + " browser", Constants.ENV_OUTPUT);
	}

	@BeforeMethod
	public void beforeMethod() {
		startDriver();
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
		networkView.selectNetworkView();
		if (!networkView.isEth1InterfacePresent()) {
			quitDriver();
			throw new SkipException(
					"eth1 interface is not present for the device");
		}
	}

	@Test
	public void connectEth1() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH1-001", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);

		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectTCPEnabledForLAN();
		networkView.selectTCPManually();
		networkView.setNetworkDNSServer();
		networkView.setNetworkGateway();
		networkView.setNetworkEth0IPAddress();
		networkView.setNetworkEth0SubnetMask();
		JvmUtil.idle(2);
		if (networkView.isDHCPNATTabEnabled()) {
			networkView.selectDHCPNATTab();
			networkView.selectDHCPRouterOff();
		}
		networkView.clickApplyButton();
		JvmUtil.idle(5);
		forceQuitDriver();
		ConsoleFactory factory = ConsoleFactory.getInstance(Application
				.getNetworkEth0StaticIPAddress());
		Reporter.log(
				"New update ip " + Application.getNetworkEth0StaticIPAddress(),
				Constants.ENV_OUTPUT);
		boolean connected = JvmUtil.waitForOSGIServices(factory);
		if (!connected) {
			Assert.fail("Applying Eth0 network configuration to manual is not successful...");
		}
		Application.updateDenaliIPInProperty(Application
				.getNetworkEth0StaticIPAddress());
		Reporter.log(
				"Configuring IP address to manually for Eth0 done successfully",
				Constants.ENV_OUTPUT);
		startDriver();
		Reporter.log("Logged into denali with new updated IP address",
				Constants.ENV_OUTPUT);
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
		Reporter.log(
				"Logged into denali successfully with newly updated IP address",
				Constants.ENV_OUTPUT);

		networkView.selectNetworkView();
		networkView.selectEth1Interface();
		networkView.selectTCPEnabledForWAN();
		networkView.selectTCPDHCP();
		JvmUtil.idle(2);
		networkView.clickApplyButton();
		networkView.selectNetworkView();
		int i = 0;
		while (i++ < 15) {
			connected = factory.verifyConnectionStatus();
			if (connected) {
				break;
			}
			JvmUtil.idle(10);
		}
		if (!connected) {
			Assert.fail("Unable to connect to eth0 after enabling the eth1 port");
		}

		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectTCPDisabled();
		JvmUtil.idle(2);
		networkView.clickApplyButton();

		factory = ConsoleFactory.getInstance(Application
				.getNetworkEth1DHCPIPAddress());
		connected = JvmUtil.waitForOSGIServices(factory);
		if (!connected) {
			Assert.fail("Unable to connect to eth1 in DHCP mode after reboot");
		}
		Application.updateDenaliIPInProperty(Application
				.getNetworkEth1DHCPIPAddress());

		factory.connectShell();
		String ipAddress = factory.getEth1IPAddress();
		Assert.assertEquals(Application.getNetworkEth1DHCPIPAddress(),
				ipAddress);
		Reporter.log(
				"The eth1 IP address verification is done using ifconfig command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the route command should show a both an appropriate network entry and the gateway entry",
				Constants.ENV_OUTPUT);
		String gatewayEntry = factory.getEth1GatewayEntry();
		Assert.assertFalse(gatewayEntry.isEmpty(),
				"The gateway entry is not obtained for eth1 port");
		List<String> dnsServerList = factory.getDNSServers();
		Assert.assertFalse(dnsServerList.isEmpty(),
				"The DNS server is not obtained for eth1 port");
		Reporter.log("The DNS server is obtained for eth1 port in DHCP mode",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "connectEth1" }, alwaysRun = true)
	public void connectMQTT() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH1-002", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log(
				"Configuring MqttDataTransport to connect to sandbox/stage broker with correct credentials",
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
		Reporter.log("Test case ID : NET-ETH1-003", Constants.ENV_OUTPUT);
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
	public void afterClass() {
		quitDriver();
	}
}
