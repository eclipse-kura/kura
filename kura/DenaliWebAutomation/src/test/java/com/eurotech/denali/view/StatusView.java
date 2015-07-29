package com.eurotech.denali.view;
import java.util.List;

import org.openqa.selenium.WebElement;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class StatusView extends WebDriverActions {

	public boolean verifyLogin() {
		boolean status = true;
		try {
			waitUntilElementVisible(BrowserRepository.DEVICE);
		} catch(Exception e) {
			status = false;
		}
		return status;
	}
	
	public void selectStatusView() {
		click(BrowserRepository.STATUS);
	}
	
	public List<WebElement> getStatusDataServiceHeaderInfo() {
		return getWebElements(BrowserRepository.STATUS_CLOUD_SERVICE_INFO_HEADER);
	}
	
	public List<WebElement> getStatusDataServiceValueInfo() {
		return getWebElements(BrowserRepository.STATUS_CLOUD_SERVICE_INFO_VALUE);
	}
	
	public List<WebElement> getStatusEthernetHeaderInfo() {
		return getWebElements(BrowserRepository.STATUS_ETHERNET_INFO_HEADER);
	}
	
	public List<WebElement> getStatusEthernetValueInfo() {
		return getWebElements(BrowserRepository.STATUS_ETHERNET_INFO_VALUE);
	}
	
	public List<WebElement> getStatusPositionHeaderInfo() {
		return getWebElements(BrowserRepository.STATUS_POSITION_INFO_HEADER);
	}
	
	public List<WebElement> getStatusPositionValueInfo() {
		return getWebElements(BrowserRepository.STATUS_POSITION_INFO_VALUE);
	}
}
