package com.eurotech.Edc;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;
import com.eurotech.denali.util.Constants;

public class EdcMainPage extends WebDriverActions {

	public void selectEdcDevices() {
		click(BrowserRepository.DEVICE);
	}
	
	public boolean isDeviceListed() {
		return isDisplayed(BrowserRepository.EDC_DEVICE_CLIENTID);
	}
	
	public String getDeviceStatus() {
		return getAttribute(BrowserRepository.EDC_DEVICE_STATUS, Constants.TITLE);
	}
	
	public void refreshDeviceStatus() {
		click(BrowserRepository.EDC_REFRESH_DEVICE_STATUS);
	}
	
	public void logOut() {
		click(BrowserRepository.EDC_LOGOUT);
	}
}
