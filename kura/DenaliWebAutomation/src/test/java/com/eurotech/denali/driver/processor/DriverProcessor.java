package com.eurotech.denali.driver.processor;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;

import com.eurotech.Edc.EdcLoginPage;
import com.eurotech.denali.common.Application;
import com.eurotech.denali.util.Constants;
import com.eurotech.denali.view.DataServiceView;
import com.eurotech.denali.view.DeviceView;
import com.eurotech.denali.view.FirewallView;
import com.eurotech.denali.view.MQTTDataTransportView;
import com.eurotech.denali.view.NetworkView;
import com.eurotech.denali.view.PackageView;
import com.eurotech.denali.view.SettingsView;
import com.eurotech.denali.view.StatusView;

public class DriverProcessor {

	public static WebDriver driver;
	public static WebDriverWait wait;
	protected StatusView statusView;
	protected NetworkView networkView;
	protected FirewallView firewallView;
	protected PackageView packageView;
	protected DeviceView deviceView;
	protected SettingsView settingsView;
	protected MQTTDataTransportView mqttView;
	protected DataServiceView dataServiceView;
	protected EdcLoginPage edcLoginPage;

	@BeforeClass
	public void objCreator() {
		if (statusView == null) {
			statusView = new StatusView();
		}
		if (deviceView == null) {
			deviceView = new DeviceView();
		}
		if (firewallView == null) {
			firewallView = new FirewallView();
		}
		if (networkView == null) {
			networkView = new NetworkView();
		}
		if (packageView == null) {
			packageView = new PackageView();
		}
		if (settingsView == null) {
			settingsView = new SettingsView();
		}
		if (mqttView == null) {
			mqttView = new MQTTDataTransportView();
		}
		if (dataServiceView == null) {
			dataServiceView = new DataServiceView();
		}
		if (edcLoginPage == null) {
			edcLoginPage = new EdcLoginPage();
		}
	}

	public static WebDriver getDriver() {
		return driver;
	}

	public void refreshBrowser() {
		if (driver != null) {
			driver.navigate().refresh();
		}
	}

	public void quitDriver() {
		if (driver != null) {
			driver.close();
			driver.quit();
		}
		driver = null;
	}

	public void forceQuitDriver() {
		if (driver != null) {
			driver.quit();
		}
		driver = null;
	}

	public void startDriver() {
		startDriver(Constants.FIREFOX);
	}

	public void startDriver(String driverName) {
		if (driver == null) {
			driver = getDriver(driverName);
			wait = new WebDriverWait(driver, 120);
		}
	}

	private WebDriver getDriver(String driverName) {
		String browser = driverName.toUpperCase();

		Reporter.log("Browser in use: " + browser, Constants.ENV_OUTPUT);

		switch (browser) {
			case Constants.CHROME :
				return makeChromeWebDriver();
			case Constants.FIREFOX :
				return makeFirefoxWebDriver();
			case Constants.IE :
				return makeIEWebDriver();
			default :
				throw new IllegalStateException("Improper browser type");
		}
	}

	private WebDriver makeChromeWebDriver() {
		System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY,
				Application.getChromeDriverLocation());
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--start-maximized");
		return new ChromeDriver(options);
	}

	private WebDriver makeFirefoxWebDriver() {
		WebDriver driver = new FirefoxDriver();
		driver.manage().window().maximize();
		return driver;
	}

	private WebDriver makeIEWebDriver() {
		System.setProperty(InternetExplorerDriverService.IE_DRIVER_EXE_PROPERTY,
				Application.getIEDriverLocation());
		DesiredCapabilities capabilitiesIE = DesiredCapabilities.internetExplorer();
		capabilitiesIE.setCapability(
				InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
		WebDriver driver = new InternetExplorerDriver(capabilitiesIE);
		driver.manage().window().maximize();
		return driver;
	}
}
