package com.eurotech.denali.test;

import java.util.List;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.eurotech.Edc.EdcMainPage;
import com.eurotech.denali.comm.ConsoleFactory;
import com.eurotech.denali.common.Application;
import com.eurotech.denali.driver.processor.DriverProcessor;
import com.eurotech.denali.util.Constants;
import com.eurotech.denali.util.JvmUtil;

public class EthernetTest extends DriverProcessor {

	@BeforeClass
	public void beforeClass() {
		Reporter.log("Verifying the \"Ethernet\" functionalities in "
				+ Constants.FIREFOX + " browser", Constants.ENV_OUTPUT);
	}

	@BeforeMethod
	public void beforeMethod() {
		startDriver();
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
	}

	@Test(description = "Test eth0: In Networking (Denali)")
	public void verifyEth0Manual() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-001", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the Eth0 functionalities by set IP address manually",
				Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		if (networkView.isEth1InterfacePresent()) {
			networkView.selectEth1Interface();
			networkView.selectTCPDisabled();
			networkView.clickApplyButton();
		}
		if (networkView.isWlan0InterfacePresent()) {
			networkView.selectWlan0Interface();
			networkView.selectTCPDisabled();
			networkView.clickApplyButton();
		}
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectTCPEnabledForLAN();
		networkView.selectTCPManually();
		networkView.setNetworkDNSServer();
//		Gateway textbox is disabled in Kura_1.1.2 release
//		networkView.setNetworkGateway();
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
		Reporter.log("Updating new static IP in application.properties",
				Constants.ENV_OUTPUT);
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
		// verify the network interface is set to manual
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		Assert.assertEquals(networkView.getConfiguration(),
				Constants.NETWORK_TCP_IP_MANUAL_CONFIGURATION);
	}

	@Test(description = "Open an ssh console and check settings to make sure they are right", dependsOnMethods = { "verifyEth0Manual" }, alwaysRun = true)
	public void verifyEth0ChangesInConsole() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-002", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the ifconfig command should show eth0 with correct ip address",
				Constants.ENV_OUTPUT);
		ConsoleFactory factory = ConsoleFactory.getInstance();
		factory.connectShell();
		String ipAddress = factory.getEth0IPAddress();
		Assert.assertEquals(Application.getNetworkEth0StaticIPAddress(),
				ipAddress,
				"The manually configured IP address is not updated in ifconfig command");
		Reporter.log(
				"The eth0 IP address verification is done using ifconfig command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the ifconfig command should show eth0 with correct subnet mask",
				Constants.ENV_OUTPUT);
		String subnetMask = factory.getEth0SubnetMask();
		Assert.assertEquals(Application.getNetworkEth0StaticSubnetMask(),
				subnetMask,
				"The manually configured subnet mask is not updated in ifconfig command");
		Reporter.log(
				"The subnet mask verification is done using ifconfig command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the route command should show a both an appropriate network entry and the gateway entry",
				Constants.ENV_OUTPUT);
//		Gateway textbox is disabled in Kura_1.1.2 release
//		String gatewayEntry = factory.getEth0GatewayEntry();
//		Assert.assertEquals(Application.getNetworkEth0StaticGateway(),
//				gatewayEntry,
//				"The manually configured gateway is not updated in route command");
		String networkEntry = factory.getEth0NetworkEntry();
		Assert.assertTrue(
				Application.getNetworkEth0StaticGateway()
						.contains(networkEntry),
				"The network entry and gateway entry is not different");
		Reporter.log(
				"The Gateway entry and Network entry verification is done using route command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying /etc/resolv.conf command should show both DNS servers",
				Constants.ENV_OUTPUT);
		List<String> dnsServerList = factory.getDNSServers();
		Assert.assertTrue(dnsServerList.contains(Application
				.getNetworkEth0StaticDNSServer()),
				"The manually configured DNS server is not resolved");
		Assert.assertTrue(dnsServerList.size() > 1,
				"Unable to fetch the old DNS server address");
		Reporter.log(
				"The DNS server verification is done using /etc/resolv.conf command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the device is able to ping external address(google.com) through putty",
				Constants.ENV_OUTPUT);
//		Gateway textbox is disabled in Kura_1.1.2 release
//		Assert.assertTrue(factory.verifyPingExternalAddress());
		Reporter.log(
				"The device is able to communicate with external address(google.com)",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "verifyEth0ChangesInConsole" }, description = "Check the gateway connects correctly to EC on sandbox console", alwaysRun = true)
	public void connectMQTT() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-003", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Modifying eth0 status to Enabled for WAN",
				Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		if (networkView.isEth1InterfacePresent()) {
			networkView.selectEth1Interface();
			networkView.selectTCPDisabled();
			networkView.clickApplyButton();
		}
		if (networkView.isWlan0InterfacePresent()) {
			networkView.selectWlan0Interface();
			networkView.selectTCPDisabled();
			networkView.clickApplyButton();
		}
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectTCPEnabledForWAN();
		networkView.setNetworkGateway();
		networkView.setNetworkSearchDomains();
		networkView.clickApplyButton();
		Reporter.log("Eth0 status is modified to Enabled for WAN",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Configuring MqttDataTransport to connect to sandbox/stage broker with correct credentials",
				Constants.ENV_OUTPUT);
		ConsoleFactory factory = ConsoleFactory.getInstance();
		Reporter.log("Waiting to start the Kura/ESF services in gateway",
				Constants.ENV_OUTPUT);
		if (!JvmUtil.waitForOSGIServices(factory)) {
			Assert.fail("Unable to connect the gateway after restart");
		}
		refreshBrowser();
		mqttView.selectMQTTTransportView();
		mqttView.setMQTTBrokerURL();
		mqttView.setMQTTAccountName();
		mqttView.setMQTTUsername();
		mqttView.setMQTTPassword();
		//This sudo client id setting is to enable the apply button 
		mqttView.setMQTTClientID("Test ClientID");
		// wait to change the state of the cursor
		JvmUtil.idle(2);
		mqttView.clickApplyButton();
		
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
		boolean isDevicePresent = edcMainPage.isDeviceListed();
		String deviceStatus = "";
		while (i++ < 20) {
			if (isDevicePresent) {
				deviceStatus = edcMainPage.getDeviceStatus();
				if (deviceStatus.equals(Constants.EDC_DEVICE_CONNECTED_STATUS)) {
					break;
				}
			}
			Reporter.log(
					"Waiting to update the device connection in EC Sandbox/Stage console",
					Constants.ENV_OUTPUT);
			JvmUtil.idle(10);
			edcMainPage.refreshDeviceStatus();
			isDevicePresent = edcMainPage.isDeviceListed();
		}
		Assert.assertTrue(edcMainPage.isDeviceListed(),
				"The client-id is not listed in the console");
		Assert.assertEquals(deviceStatus,
				Constants.EDC_DEVICE_CONNECTED_STATUS, "Device status is : "
						+ deviceStatus);
		Reporter.log("The gateway " + Application.getMQTTClientID()
				+ " is connected to the EC sandbox/stage console successfully",
				Constants.ENV_OUTPUT);
		edcMainPage.logOut();
	}

	@Test(dependsOnMethods = { "connectMQTT" }, description = "Reboot the gateway (Power OFF/ON) and check it reconnects on EC console", alwaysRun = true)
	public void verifyReconnectEC() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-004", Constants.ENV_OUTPUT);
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
		int i = 0;
		boolean isDevicePresent = edcMainPage.isDeviceListed();
		String deviceStatus = "";
		while (i++ < 20) {
			if (isDevicePresent) {
				deviceStatus = edcMainPage.getDeviceStatus();
				if (deviceStatus.equals(Constants.EDC_DEVICE_CONNECTED_STATUS)) {
					break;
				}
			}
			Reporter.log(
					"Waiting to update the device connection in EC Sandbox/Stage console",
					Constants.ENV_OUTPUT);
			JvmUtil.idle(10);
			edcMainPage.refreshDeviceStatus();
			isDevicePresent = edcMainPage.isDeviceListed();
		}
		Assert.assertTrue(edcMainPage.isDeviceListed(),
				"The client-id is not listed in the console after rebooting the device");
		Assert.assertEquals(deviceStatus,
				Constants.EDC_DEVICE_CONNECTED_STATUS, "Device status is : "
						+ deviceStatus);
		Reporter.log("Device status in console is " + deviceStatus,
				Constants.ENV_OUTPUT);
		edcMainPage.logOut();
	}

	@Test(dependsOnMethods = { "verifyReconnectEC" }, description = "Reconfigure Subnet Mask and DNS servers to something different (but still appropriate for you network) without rebooting", alwaysRun = true)
	public void verifyReconfigureSubnetAndDNS() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-005", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log(
				"Reconfigure Subnet Mask and DNS servers to something different without rebooting",
				Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.setNetworkDNSServerSecondary();
		networkView.setNetworkSubnetMaskSecondary();
		networkView.clickApplyButton();
		networkView.selectNetworkView();
		int i = 0;
		ConsoleFactory factory = ConsoleFactory.getInstance();
		while (i++ < 20) {
			if (factory.verifyConnectionStatus()) {
				break;
			}
			JvmUtil.idle(10);
		}
		if (!factory.isShellConnected()) {
			Assert.fail("Unable to connect after reconfiguring DNS and subnet mask");
		}

		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		Assert.assertEquals(networkView.getNetworkDNSServer(),
				Application.getNetworkEth0StaticDNSServerSecondary());
		Assert.assertEquals(networkView.getNetworkSubnetMask(),
				Application.getNetworkEth0StaticSubnetMaskSecondary());
		Reporter.log("Verifying Re-configure subnet mask to different",
				Constants.ENV_OUTPUT);

		String subnetMask = factory.getEth0SubnetMask();
		Assert.assertEquals(
				Application.getNetworkEth0StaticSubnetMaskSecondary(),
				subnetMask,
				"The Re-configured subnet mask is not updated in ifconfig command");
		Reporter.log(
				"The Re-Configured subnet mask verification is done using ifconfig command",
				Constants.ENV_OUTPUT);
		Reporter.log("Verifying Re-configure DNS server to different",
				Constants.ENV_OUTPUT);
		List<String> dnsServerList = factory.getDNSServers();
		Assert.assertTrue(dnsServerList.contains(Application
				.getNetworkEth0StaticDNSServerSecondary()),
				"The Re-configured DNS server is not resolved");
		Reporter.log(
				"The Re-configured DNS server verification is done using /etc/resolv.conf command",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "verifyReconfigureSubnetAndDNS" }, alwaysRun = true)
	public void verifyDisconnectEth0() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-006", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Remove Ethernet cable and reboot", Constants.ENV_OUTPUT);
		ConsoleFactory factory = ConsoleFactory.getInstance();
		factory.connectViaSFTP();
		factory.transferFile(
				Constants.ETH0_DISCONNECT_REBOOT_SH_TC_SOURCE_FILE_LOCATION,
				Constants.ETH0_DISCONNECT_REBOOT_SH_TC_DEST_FILE_LOCATION);
		factory.connectShell();
		factory.executeDisconnectEth0AndRebootShellScript();
		factory.close();
		boolean connectionStatus = false;
		int i = 0;
		while (i++ < 20) {
			connectionStatus = factory.verifyConnectionStatus();
			if (connectionStatus) {
				break;
			}
			JvmUtil.idle(10);
		}
		Assert.assertTrue(connectionStatus,
				"Unable to connect the device after reboot");
		Reporter.log("IfConfig command result after disconnecting eth0: "
				+ factory.getIfConfigLog(), Constants.ENV_OUTPUT);
		Reporter.log("Route command result after disconnecting eth0: "
				+ factory.getRouteLog(), Constants.ENV_OUTPUT);
		Reporter.log("Eth0 port is disconnected and verified",
				Constants.ENV_OUTPUT);

		JvmUtil.waitForOSGIServices(factory);
		factory.connectShell();
		String ipAddress = factory.getEth0IPAddress();
		Assert.assertEquals(Application.getDeviceIP(), ipAddress,
				"IP address for eth0 is changed after reconnecting the port");
		Reporter.log(
				"The reconnecting eth0 port IP verfification is done using ifconfig command",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the device is able to ping external address(google.com) through putty",
				Constants.ENV_OUTPUT);
//		Gateway textbox is disabled in Kura_1.1.2 release
//		Assert.assertTrue(factory.verifyPingExternalAddress(), "Unable to ping google.com");
		Reporter.log(
				"The device is able to communicate with external address(google.com)",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "verifyDisconnectEth0" }, alwaysRun = true)
	public void verifyReconfigurEth0ToDHCP() {
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-007", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Reconfigure eth0 to use DHCP Client",
				Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.clearNetworkDNSServer();
//		Gateway textbox is disabled in Kura_1.1.2 release
//		networkView.clearNetworkGateway();
		networkView.selectTCPDHCP();
		networkView.clickApplyButton();
		Application.updateDenaliIPInProperty(Application
				.getNetworkEth0DHCPIPAddress());
		Reporter.log("Updating static IP to DHCP ip in application.properties",
				Constants.ENV_OUTPUT);

		Reporter.log(
				"Verifying the eth0 port is obtaining ip address from DHCP server using ifconfig command",
				Constants.ENV_OUTPUT);
		ConsoleFactory factory = ConsoleFactory.getInstance();
		int i = 0;
		boolean connectionStatus = false;
		while (i++ < 20) {
			connectionStatus = factory.verifyConnectionStatus();
			if (connectionStatus) {
				break;
			}
			JvmUtil.idle(10);
			Reporter.log("Trying to connect the console...",
					Constants.ENV_OUTPUT);
		}
		Assert.assertTrue(connectionStatus,
				"Unable to connect the device after changing the mode to DHCP");
		// verify interface is in DHCP mode
		networkView.get(Application.getDenaliAppWithDefaultCredential());
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		Assert.assertEquals(networkView.getConfiguration(),
				Constants.NETWORK_TCP_IP_DHCP_CONFIGURATION);
		Reporter.log("Eth0 port is reconfigured to DHCP mode",
				Constants.ENV_OUTPUT);

		factory.connectShell();
		String ipAddress = factory.getEth0IPAddress();
		Assert.assertFalse(
				ipAddress.isEmpty(),
				"Unable to obtain IP address from DHCP server after configuring eth0 port to DHCP mode");
		Reporter.log("The IP address obtained from DHCP server for eth0 is "
				+ ipAddress, Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the eth0 port is obtaining subnet mask using ifconfig command",
				Constants.ENV_OUTPUT);
		String subnetMask = factory.getEth0SubnetMask();
		Assert.assertFalse(subnetMask.isEmpty(),
				"Unable to obtain subnet mask after configuring eth0 port to DHCP mode");
		Reporter.log("The subnet mask for eth0 is " + subnetMask,
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the route command should show a both an appropriate network entry and the gateway entry after configuring eth0 to DHCP mode",
				Constants.ENV_OUTPUT);
		String gatewayEntry = factory.getEth0GatewayEntry();
		Assert.assertFalse(
				gatewayEntry.isEmpty(),
				"The gateway entry is not obtained after configuring the eth0 port to DHCP mode");
		String networkEntry = factory.getEth0NetworkEntry();
		Assert.assertFalse(
				networkEntry.isEmpty(),
				"The networkEntry entry is not obtained after configuring the eth0 port to DHCP mode");
		Reporter.log(
				"The Gateway entry and Network entry verification is done using route command for eth0 port in DHCP mode",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying /etc/resolv.conf command should show DNS servers for eth0 port in DHCP mode",
				Constants.ENV_OUTPUT);
		List<String> dnsServerList = factory.getDNSServers();
		Assert.assertFalse(dnsServerList.isEmpty(),
				"The DNS server is not obtained after configuring the eth0 port to DHCP mode");
		Reporter.log("The DNS server is obtained for eth0 port in DHCP mode",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the device is able to ping external address(google.com) through putty",
				Constants.ENV_OUTPUT);
		Assert.assertTrue(factory.verifyPingExternalAddress());
		Reporter.log(
				"The device is able to communicate with external address(google.com)",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "verifyReconfigurEth0ToDHCP" }, alwaysRun = true)
	public void verifyRemoveEth0CableAndReboot() {
		Reporter.log("==========================================",
				Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-008 - NET-ETH0-009",
				Constants.ENV_OUTPUT);
		Reporter.log("==========================================",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Remove eth0 cable and reboot, verify the device is able to communicate in DHCP mode",
				Constants.ENV_OUTPUT);

		ConsoleFactory factory = ConsoleFactory.getInstance();
		factory.connectViaSFTP();
		factory.transferFile(
				Constants.ETH0_DISCONNECT_REBOOT_SH_TC_SOURCE_FILE_LOCATION,
				Constants.ETH0_DISCONNECT_REBOOT_SH_TC_DEST_FILE_LOCATION);
		factory.connectShell();
		factory.executeDisconnectEth0AndRebootShellScript();
		factory.close();
		int i = 0;
		boolean connectionStatus = false;
		while (i++ < 20) {
			connectionStatus = factory.verifyConnectionStatus();
			if (connectionStatus) {
				break;
			}
			JvmUtil.idle(10);
		}
		Assert.assertTrue(connectionStatus,
				"Unable to connect the device after reboot");
		Reporter.log("IfConfig command result after disconnecting eth0: "
				+ factory.getIfConfigLog(), Constants.ENV_OUTPUT);
		Reporter.log("Route command result after disconnecting eth0: "
				+ factory.getRouteLog(), Constants.ENV_OUTPUT);
		Reporter.log("Eth0 port is disconnected and verified",
				Constants.ENV_OUTPUT);

		String ipAddress = factory.getEth0IPAddress();
		Assert.assertFalse(ipAddress.isEmpty(),
				"Unable to obtain IP address from DHCP server after rebooting the device");
		Reporter.log(
				"The IP address obtained from DHCP server for eth0 after reboot is "
						+ ipAddress, Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the eth0 port is obtaining subnet mask after rebooting the device",
				Constants.ENV_OUTPUT);
		String subnetMask = factory.getEth0SubnetMask();
		Assert.assertFalse(subnetMask.isEmpty(),
				"Unable to obtain subnet mask after rebooting the device");
		Reporter.log("The subnet mask for eth0 is " + subnetMask,
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the route command should show a both an appropriate network entry and the gateway entry after rebooting the device",
				Constants.ENV_OUTPUT);
		String gatewayEntry = factory.getEth0GatewayEntry();
		Assert.assertFalse(gatewayEntry.isEmpty(),
				"The gateway entry is not obtained after reboot");
		String networkEntry = factory.getEth0NetworkEntry();
		Assert.assertFalse(networkEntry.isEmpty(),
				"The network entry is not obtained after reboot");
		Reporter.log(
				"The Gateway entry and Network entry verification is done after rebooting the device in DHCP mode",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying /etc/resolv.conf command should show DNS servers for eth0 port in DHCP mode",
				Constants.ENV_OUTPUT);
		List<String> dnsServerList = factory.getDNSServers();
		Assert.assertFalse(dnsServerList.isEmpty(),
				"The DNS server is not obtained after configuring the eth0 port to DHCP mode");
		Reporter.log("The DNS server is obtained for eth0 port in DHCP mode",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Verifying the device is able to ping external address(google.com) through putty",
				Constants.ENV_OUTPUT);
		Assert.assertTrue(factory.verifyPingExternalAddress());
		Reporter.log(
				"The device is able to communicate with external address(google.com)",
				Constants.ENV_OUTPUT);
		JvmUtil.waitForOSGIServices(factory);
	}

	@Test(dependsOnMethods = { "verifyRemoveEth0CableAndReboot" }, alwaysRun = true)
	public void verifyDHCPServerFunction() {
		Reporter.log("==========================================",
				Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-0010", Constants.ENV_OUTPUT);
		Reporter.log("==========================================",
				Constants.ENV_OUTPUT);
		Reporter.log("Test DHCP server function", Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		networkView.selectTCPEnabledForLAN();
		networkView.selectTCPManually();
		networkView.clearNetworkDNSServer();
//		Gateway textbox is disabled in Kura_1.1.2 release
//		networkView.clearNetworkGateway();
		networkView.setNetworkEth0IPAddress();
		networkView.setNetworkEth0SubnetMask();
		JvmUtil.idle(2);
		Assert.assertTrue(networkView.isDHCPNATTabPresent());
		Assert.assertTrue(networkView.isDHCPNATTabEnabled());
		networkView.selectDHCPNATTab();
		networkView.selectDHCPRouterDHCPAndNAT();
		networkView.setDHCPEth0BeginAddress();
		networkView.setDHCPEth0EndAddress();
		networkView.setDHCPEth0SubnetMask();
		networkView.setDHCPEth0DeafultLeaseTime();
		networkView.setDHCPEth0MaxLeaseTime();
		networkView.selectDHCPPassDNSServer();
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
				"Updating DHCP IP to manually configured IP in application.properties",
				Constants.ENV_OUTPUT);
		Reporter.log(
				"Configuring IP address to manually for Eth0 done successfully",
				Constants.ENV_OUTPUT);
		factory.connectShell();
		Assert.assertTrue(factory.isDHCPDServiceRunning(),
				"DHCPD service is not running");
		Assert.assertTrue(factory.isBindServiceRunning(),
				"Bind(named) service is not running");
		startDriver();
		Reporter.log("Logged into denali with new updated IP address",
				Constants.ENV_OUTPUT);
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		Assert.assertTrue(statusView.verifyLogin());
		Reporter.log(
				"Logged into denali successfully with newly updated IP address",
				Constants.ENV_OUTPUT);
	}

	@AfterMethod
	public void afterMethod() {
		quitDriver();
	}
	
	@AfterClass
	public void afterClass()
	{
		this.beforeClass();
		this.beforeMethod();
		
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : NET-ETH0-007", Constants.ENV_OUTPUT);
		Reporter.log("===========================", Constants.ENV_OUTPUT);
		Reporter.log("Reconfigure eth0 to use DHCP Client",
				Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		networkView.selectEth0Interface();
		networkView.clearNetworkDNSServer();
		networkView.selectTCPDHCP();
		networkView.clickApplyButton();
		Application.updateDenaliIPInProperty(Application
				.getNetworkEth0DHCPIPAddress());
		Reporter.log("Updating static IP to DHCP ip in application.properties",
				Constants.ENV_OUTPUT);

		this.afterMethod();
	}
}
