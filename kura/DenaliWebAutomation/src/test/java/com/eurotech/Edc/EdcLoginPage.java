package com.eurotech.Edc;

import com.eurotech.denali.common.Application;
import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class EdcLoginPage extends WebDriverActions {
	
	private void setUserName() {
		sendKeys(BrowserRepository.EDC_USERNAME, Application.getEDCUserName());
	}

	private void setPassword() {
		sendKeys(BrowserRepository.EDC_PASSWORD, Application.getEDCPassword());
	}
	
	private void clickLogin() {
		click(BrowserRepository.EDC_LOGIN_BUTTON);
	}
	
	public EdcMainPage login() {
		get(Application.getEDCAppIP());
		setUserName();
		setPassword();
		clickLogin();
		return new EdcMainPage();
	}
}
