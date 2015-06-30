package com.eurotech.denali.view;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class DataServiceView extends WebDriverActions {

	public void selectDataServiceView() {
		click(BrowserRepository.DATA_SERVICE);
	}

	public boolean isChangesPresent() {
		return isDisplayed(BrowserRepository.NETWORK_CHANGES);
	}
	
	public void selectAutoStartupToTrue() {
		click(BrowserRepository.DATA_SERVICE_AUTO_START_UP_TRUE);
	}
	
	public boolean isApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.DATA_SERVICE_APPLY_BUTTON);
	}
	
	public void clickApplyButton() {
		click(BrowserRepository.DATA_SERVICE_APPLY_BUTTON);
	}
	
	public void clickConfirmYesButton() {
		click(BrowserRepository.DATA_SERVICE_CONFIRM_YES_BUTTON);
	}
	
	public boolean isConfirmationPresent() {
		boolean status = false;
		int i=0;
		while(i++<1000) {
			status = isPresent(BrowserRepository.DATA_SERVICE_CONFIRMATION);
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
