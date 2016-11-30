package com.eurotech.denali.view;

import java.util.List;

import org.openqa.selenium.WebElement;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class DeviceView extends WebDriverActions {

	public void selectDeviceView() {
		click(BrowserRepository.DEVICE);
	}

	public boolean isProfileTabDisplayed() {
		return isDisplayed(BrowserRepository.DEVICE_PROFILE);
	}

	public void selectDeviceBundleTab() {
		click(BrowserRepository.DEVICE_BUNDLES);
	}

	public boolean isBundlesTabDisplayed() {
		return isDisplayed(BrowserRepository.DEVICE_BUNDLES);
	}

	public void selectDeviceThreadTab() {
		click(BrowserRepository.DEVICE_THREADS);
	}

	public boolean isThreadTabDisplayed() {
		return isDisplayed(BrowserRepository.DEVICE_THREADS);
	}

	public void selectDeviceSystemPropertyTab() {
		click(BrowserRepository.DEVICE_SYSTEM_PROPERTY);
	}

	public boolean isSystemPropertyTabDisplayed() {
		return isDisplayed(BrowserRepository.DEVICE_SYSTEM_PROPERTY);
	}

	public void selectDeviceCommandTab() {
		click(BrowserRepository.DEVICE_COMMAND);
	}

	public boolean isCommandTabDisplayed() {
		return isDisplayed(BrowserRepository.DEVICE_COMMAND);
	}

	public List<WebElement> getDeviceProfileDeviceHeaderInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_DEVICE_INFO_HEADER);
	}

	public List<WebElement> getDeviceProfileDeviceValueInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_DEVICE_INFO_VALUE);
	}

	public List<WebElement> getDeviceProfileGPSHeaderInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_GPS_INFO_HEADER);
	}

	public List<WebElement> getDeviceProfileGPSValueInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_GPS_INFO_VALUE);
	}

	public List<WebElement> getDeviceProfileHardwareHeaderInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_HARDWARE_INFO_HEADER);
	}

	public List<WebElement> getDeviceProfileHardwareValueInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_HARDWARE_INFO_VALUE);
	}

	public List<WebElement> getDeviceProfileJavaHeaderInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_JAVA_INFO_HEADER);
	}

	public List<WebElement> getDeviceProfileJavaValueInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_JAVA_INFO_VALUE);
	}

	public List<WebElement> getDeviceProfileSoftwareHeaderInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_SOFTWARE_INFO_HEADER);
	}

	public List<WebElement> getDeviceProfileSoftwareValueInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_SOFTWARE_INFO_VALUE);
	}

	public List<WebElement> getDeviceProfileNetworkHeaderInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_NETWORK_INFO_HEADER);
	}

	public List<WebElement> getDeviceProfileNetworkValueInfo() {
		return getWebElements(BrowserRepository.DEVICE_PROFILE_NETWORK_INFO_VALUE);
	}
}
