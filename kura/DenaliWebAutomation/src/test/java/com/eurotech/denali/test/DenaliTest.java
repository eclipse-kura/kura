package com.eurotech.denali.test;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.eurotech.denali.common.Application;
import com.eurotech.denali.driver.processor.DriverProcessor;
import com.eurotech.denali.util.Constants;

public class DenaliTest extends DriverProcessor {

	private String browser;
	private boolean isPasswordChanged = false;

	@BeforeClass
	@Parameters({ "browser" })
	public void beforeClass(String browser) {
		Reporter.log(
				"Verifying the \"First boot and General Denali Functions\" in "
						+ browser + " browser", Constants.ENV_OUTPUT);
		this.browser = browser;
		if (browser.equals(Constants.IE) && (!Application.isWindows())) {
			throw new SkipException("IE browser is not supported on this OS");
		}
		startDriver(browser);
		Application.generateAndUpdateNewDenaliPassword(browser);
	}

	@Test(description = "Verify that the web page open a login window with username/password = admin/admin")
	public void verifyWebPageLogin() {
		Reporter.log("================================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : DEN-001 - DEN-003", Constants.ENV_OUTPUT);
		Reporter.log("================================", Constants.ENV_OUTPUT);
		Reporter.log("Verifying web page login", Constants.ENV_OUTPUT);
		statusView.get(Application.getDenaliAppWithDefaultCredential());
		if (browser.equals(Constants.IE))
			statusView.get(Application.getDenaliAppURL());
		Assert.assertTrue(statusView.verifyLogin());
		Reporter.log(
				"Logged into Denali with credentials admin/admin successfully",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "verifyWebPageLogin" }, description = "In Denali browse to the Settings page and make attempt to change the password, log out, and log back in using the newly created password", alwaysRun = true)
	public void verifySettingsView() {
		Reporter.log("======================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : DEN-004", Constants.ENV_OUTPUT);
		Reporter.log("======================", Constants.ENV_OUTPUT);
		Reporter.log("Verifying the Network view->Admin tab",
				Constants.ENV_OUTPUT);
		settingsView.selectSettingView();
		settingsView.selectAdminPasswordTab();
		Assert.assertTrue(settingsView.isCurrentAdminPasswordEnabled());
		settingsView.setCurrentAdminPassword(Application.getDenaliPassword());
		Assert.assertTrue(settingsView.isNewPasswordEnabled());
		settingsView.setNewPassword(Application.getDenaliNewPassword());
		Assert.assertTrue(settingsView.isConfirmPasswordEnabled());
		settingsView.setConfirmPassword(Application.getDenaliNewPassword());
		Assert.assertTrue(settingsView.isApplyButtonEnabled());
		settingsView.clickApply();
		isPasswordChanged = true;
		Assert.assertTrue(settingsView.isConfirmationPresent());
		Reporter.log("New admin password applied successfully",
				Constants.ENV_OUTPUT);
		settingsView.quitDriver();
		Reporter.log(
				"Login back with new credentials "
						+ Application.getDenaliUsername() + "/"
						+ Application.getDenaliNewPassword() + " to verify",
				Constants.ENV_OUTPUT);
		settingsView.startDriver(browser);
		statusView.get(Application.getDenaliAppWithCustomCredential(
				Application.getDenaliUsername(),
				Application.getDenaliNewPassword()));
		if (browser.equalsIgnoreCase(Constants.IE)) {
			statusView.get(Application.getDenaliAppURL());
		}
		Assert.assertTrue(statusView.verifyLogin());
		Reporter.log("Logged into denali with new credentials successfully",
				Constants.ENV_OUTPUT);
	}

	@Test(dependsOnMethods = { "verifySettingsView" }, description = "On the Device->Profile page, verify that all displayed version and status information is correct", alwaysRun = true)
	public void verifyDeviceProfile() {
		Reporter.log("======================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : DEN-005", Constants.ENV_OUTPUT);
		Reporter.log("======================", Constants.ENV_OUTPUT);
		Reporter.log("Verifying the Device view->Profile tab",
				Constants.ENV_OUTPUT);
		deviceView.selectDeviceView();
		Reporter.log("Device Information:", Constants.ENV_OUTPUT);
		Assert.assertTrue(deviceView.isProfileTabDisplayed());

		// Verify Device Information Details
		List<WebElement> headerElement = deviceView
				.getDeviceProfileDeviceHeaderInfo();
		List<WebElement> valueElement = deviceView
				.getDeviceProfileDeviceValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					deviceView.getText(valueElement.get(i)), "The value for "
							+ deviceView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(deviceView.getText(headerElement.get(i)) + " is "
					+ deviceView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify GPS Information Details
		Reporter.log("GPS Information:", Constants.ENV_OUTPUT);
		headerElement = deviceView.getDeviceProfileGPSHeaderInfo();
		valueElement = deviceView.getDeviceProfileGPSValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					deviceView.getText(valueElement.get(i)), "The value for "
							+ deviceView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(deviceView.getText(headerElement.get(i)) + " is "
					+ deviceView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify Hardware Information Details
		Reporter.log("Hardware Information:", Constants.ENV_OUTPUT);
		headerElement = deviceView.getDeviceProfileHardwareHeaderInfo();
		valueElement = deviceView.getDeviceProfileHardwareValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					deviceView.getText(valueElement.get(i)), "The value for "
							+ deviceView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(deviceView.getText(headerElement.get(i)) + " is "
					+ deviceView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify Java Information Details
		Reporter.log("Java Information:", Constants.ENV_OUTPUT);
		headerElement = deviceView.getDeviceProfileJavaHeaderInfo();
		valueElement = deviceView.getDeviceProfileJavaValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					deviceView.getText(valueElement.get(i)), "The value for "
							+ deviceView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(deviceView.getText(headerElement.get(i)) + " is "
					+ deviceView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify Network Information Details
		Reporter.log("Network Information:", Constants.ENV_OUTPUT);
		headerElement = deviceView.getDeviceProfileNetworkHeaderInfo();
		valueElement = deviceView.getDeviceProfileNetworkValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					deviceView.getText(valueElement.get(i)), "The value for "
							+ deviceView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(deviceView.getText(headerElement.get(i)) + " is "
					+ deviceView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify Software Information Details
		Reporter.log("Software Information:", Constants.ENV_OUTPUT);
		headerElement = deviceView.getDeviceProfileSoftwareHeaderInfo();
		valueElement = deviceView.getDeviceProfileSoftwareValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					deviceView.getText(valueElement.get(i)), "The value for "
							+ deviceView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(deviceView.getText(headerElement.get(i)) + " is "
					+ deviceView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		Reporter.log(
				"Verifying the Device view->Bundles, Thread, SystemProperty and Device tab",
				Constants.ENV_OUTPUT);
		deviceView.selectDeviceThreadTab();
		Assert.assertTrue(deviceView.isThreadTabDisplayed());
		deviceView.selectDeviceSystemPropertyTab();
		Assert.assertTrue(deviceView.isSystemPropertyTabDisplayed());
		deviceView.selectDeviceCommandTab();
		Assert.assertTrue(deviceView.isCommandTabDisplayed());
		deviceView.selectDeviceBundleTab();
		Assert.assertTrue(deviceView.isBundlesTabDisplayed());

		Reporter.log("Verifying the Status view", Constants.ENV_OUTPUT);
		statusView.selectStatusView();

		// Verify cloud and Data service Details
		Reporter.log("Cloud and Data Service:", Constants.ENV_OUTPUT);
		headerElement = statusView.getStatusDataServiceHeaderInfo();
		valueElement = statusView.getStatusDataServiceValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					statusView.getText(valueElement.get(i)), "The value for "
							+ statusView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(statusView.getText(headerElement.get(i)) + " is "
					+ statusView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify Ethernet Details
		Reporter.log("Ethernet Settings:", Constants.ENV_OUTPUT);
		headerElement = statusView.getStatusEthernetHeaderInfo();
		valueElement = statusView.getStatusEthernetValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					statusView.getText(valueElement.get(i)), "The value for "
							+ statusView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(statusView.getText(headerElement.get(i)) + " is "
					+ statusView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}

		// Verify Position status Details
		Reporter.log("Position Status:", Constants.ENV_OUTPUT);
		headerElement = statusView.getStatusPositionHeaderInfo();
		valueElement = statusView.getStatusPositionValueInfo();
		for (int i = 0; i < headerElement.size(); i++) {
			Assert.assertNotEquals(Constants.EMPTY,
					statusView.getText(valueElement.get(i)), "The value for "
							+ statusView.getText(headerElement.get(i))
							+ " is empty ");
			Reporter.log(statusView.getText(headerElement.get(i)) + " is "
					+ statusView.getText(valueElement.get(i)),
					Constants.ENV_OUTPUT);
		}
	}

	@Test(dependsOnMethods = { "verifyDeviceProfile" }, description = "Select several of the tabs, to make sure they display", alwaysRun = true)
	public void verifyAllViews() {
		Reporter.log("======================", Constants.ENV_OUTPUT);
		Reporter.log("Test case ID : DEN-006", Constants.ENV_OUTPUT);
		Reporter.log("======================", Constants.ENV_OUTPUT);
		// verify packages view
		Reporter.log("Verifying the Packages view", Constants.ENV_OUTPUT);
		packageView.selectPackagesView();
		Assert.assertTrue(packageView.isInstallButtonEnabled());
		Assert.assertTrue(packageView.isRefreshButtonEnabled());
		Assert.assertFalse(packageView.isUninstallButtonEnabled());

		// verify setting view - snapshots tab
		Reporter.log("Verifying the Settings view-> Snapshot tab",
				Constants.ENV_OUTPUT);
		settingsView.selectSettingView();
		Assert.assertTrue(settingsView.isSnapshotApplyEnabled());
		Assert.assertTrue(settingsView.isSnapshotRefreshEnabled());
		Assert.assertFalse(settingsView.isSnapshotRollbackEnabled());
		Assert.assertFalse(settingsView.isSnapshotDownloadEnabled());

		// verify Firewall view - open ports tab
		Reporter.log("Verifying the Firewall view-> Open Port tab",
				Constants.ENV_OUTPUT);
		firewallView.selectFirewallView();
		Assert.assertTrue(firewallView.isOpenPortNewButtonEnabled());
		Assert.assertFalse(firewallView.isOpenPortApplyButtonEnabled());
		Assert.assertFalse(firewallView.isOpenPortDeleteButtonEnabled());
		Assert.assertFalse(firewallView.isOpenPortEditButtonEnabled());
		Reporter.log("Verifying the eth0, eth1 and wlan permitted interface",
				Constants.ENV_OUTPUT);
		Assert.assertEquals(firewallView.getOpenPortEth0Interface(),
				Constants.FIREWALL_OPEN_PORT_INTERFACE_COUNT);
		// Assert.assertEquals(firewallView.getOpenPortEth1Interface(),
		// Constants.FIREWALL_OPEN_PORT_INTERFACE_COUNT);
		// Assert.assertEquals(firewallView.getOpenPortWlanInterface(),
		// Constants.FIREWALL_OPEN_PORT_INTERFACE_COUNT);

		// verify Firewall view - Port Forwarding tab
		Reporter.log("Verifying the Firewall view-> Port Forwarding tab",
				Constants.ENV_OUTPUT);
		firewallView.selectPortForwardTab();
		Assert.assertTrue(firewallView.isPortForwardNewButtonEnabled());
		Assert.assertFalse(firewallView.isPortForwardEditButtonEnabled());
		Assert.assertFalse(firewallView.isPortForwardDeleteButtonEnabled());
		Assert.assertFalse(firewallView.isPortForwardApplyButtonEnabled());

		// verify Firewall view - NAT tab
		if (Application.isKura()) {
			Reporter.log("Verifying the Firewall view-> NAT tab",
					Constants.ENV_OUTPUT);
			firewallView.selectNATTab();
			Assert.assertTrue(firewallView.isNatNewButtonEnabled());
			Assert.assertFalse(firewallView.isNatApplyButtonEnabled());
			Assert.assertFalse(firewallView.isNatDeleteButtonEnabled());
			Assert.assertFalse(firewallView.isNatEditButtonEnabled());
		}

		// verify Network view - loopback interface
		Reporter.log("Verifying the Network view-> loopback interface",
				Constants.ENV_OUTPUT);
		networkView.selectNetworkView();
		Assert.assertFalse(networkView.isApplyButtonEnabled());
		Assert.assertTrue(networkView.isRefreshButtonEnabled());
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectHardwareTab();
		Assert.assertEquals(networkView.getHardwareState(),
				Constants.NETWORK_HARDWARE_STATE);
		Assert.assertEquals(networkView.getHardwareName(),
				Constants.NETWORK_HARDWARE_LOOPBACK_NAME);
		Assert.assertEquals(networkView.getHardwareType(),
				Constants.NETWORK_HARDWARE_LOOPBACK_TYPE);

		// verify Network view - eth0 interface
		Reporter.log("Verifying the Network view-> eth0 interface",
				Constants.ENV_OUTPUT);
		networkView.selectEth0Interface();
		Assert.assertFalse(networkView.isApplyButtonEnabled());
		Assert.assertTrue(networkView.isRefreshButtonEnabled());
		Assert.assertTrue(networkView.isTCPIPTabDisplayed());
		networkView.selectHardwareTab();
		Assert.assertEquals(networkView.getHardwareState(),
				Constants.NETWORK_HARDWARE_STATE);
		Assert.assertEquals(networkView.getHardwareName(),
				Constants.NETWORK_HARDWARE_ETH0_NAME);
		Assert.assertEquals(networkView.getHardwareType(),
				Constants.NETWORK_HARDWARE_ETH0_TYPE);
	}

	@AfterClass
	public void afterClass() {
		quitDriver();
		if (isPasswordChanged) {
			Application.updateDenaliDefaultPassword(Application
					.getDenaliNewPassword());
		}
	}
}
