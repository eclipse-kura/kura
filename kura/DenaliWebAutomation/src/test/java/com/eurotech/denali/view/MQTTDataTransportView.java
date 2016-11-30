package com.eurotech.denali.view;

import com.eurotech.denali.common.Application;
import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class MQTTDataTransportView extends WebDriverActions {

	public void selectMQTTTransportView() {
		click(BrowserRepository.MQTT);
	}
	
	public boolean isChangesPresent() {
		return isDisplayed(BrowserRepository.NETWORK_CHANGES);
	}
	
	public void setMQTTBrokerURL() {
		sendKeys(BrowserRepository.MQTT_BROKER_URL, Application.getMQTTBrokerURL());
	}
	
	public void setMQTTAccountName() {
		sendKeys(BrowserRepository.MQTT_ACCOUNT_NAME, Application.getMQTTAccountName());
	}
	
	public void setMQTTUsername() {
		sendKeys(BrowserRepository.MQTT_USERNAME, Application.getMQTTUsername());
	}
	
	public void setMQTTPassword() {
		sendKeys(BrowserRepository.MQTT_PASSWORD, Application.getMQTTPassword());
	}
	
	public void setMQTTClientID() {
		sendKeys(BrowserRepository.MQTT_CLIENT_ID, Application.getMQTTClientID());
	}
	
	public void setMQTTClientID(String clientID)
	{
		sendKeys(BrowserRepository.MQTT_CLIENT_ID, clientID);
	}
	
	public boolean isApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.MQTT_APPLY_BUTTON);
	}
	
	public void clickApplyButton() {
		click(BrowserRepository.MQTT_APPLY_BUTTON);
	}
	
	public void clickConfirmYesButton() {
		click(BrowserRepository.MQTT_CONFIRM_YES_BUTTON);
	}
	
	public boolean isConfirmationPresent() {
		boolean status = false;
		int i=0;
		while(i++<1000) {
			status = isPresent(BrowserRepository.MQTT_CONFIRMATION);
			if(status ==true)
				break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return status;
	}
	
	public boolean isConfirmationPopUpPresent() {
		boolean status = false;
		int i=0;
		while(i++<100) {
			status = isPresent(BrowserRepository.CONFIRM_POP_UP);
			if(status ==true)
				break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return status;
	}
}
