package com.eurotech.denali.view;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class SettingsView extends WebDriverActions {

	public void selectSettingView() {
		click(BrowserRepository.SETTINGS);
	}

	public void clickApply() {
		click(BrowserRepository.SETTINGS_APPLY);
	}

	public boolean isApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.SETTINGS_APPLY);
	}
	
	public void selectAdminPasswordTab() {
		click(BrowserRepository.SETTINGS_ADMIN_PASSWORD);
	}
	
	public void setCurrentAdminPassword(String password) {
		sendKeys(BrowserRepository.SETTINGS_CURRENT_PASSWORD, password);
	}
	
	public boolean isCurrentAdminPasswordEnabled() {
		return isTextBoxEnabled(BrowserRepository.SETTINGS_CURRENT_PASSWORD);
	}
	
	public void setNewPassword(String password) {
		sendKeys(BrowserRepository.SETTINGS_NEW_PASSWORD, password);
	}
	
	public boolean isNewPasswordEnabled() {
		return isTextBoxEnabled(BrowserRepository.SETTINGS_NEW_PASSWORD);
	}
	
	public void setConfirmPassword(String password) {
		sendKeys(BrowserRepository.SETTINGS_CONFIRM_PASSWORD, password);
	}
	
	public boolean isConfirmPasswordEnabled() {
		return isTextBoxEnabled(BrowserRepository.SETTINGS_CONFIRM_PASSWORD);
	}
	
	public boolean isConfirmationPresent() {
		boolean status = false;
		int i=0;
		while(i++<50) {
			status = isDisplayed(BrowserRepository.SETTING_CONFIRMATION);
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
	
	public boolean isSnapshotRefreshEnabled() {
		return isButtonEnabled(BrowserRepository.SETTINGS_SNAPSHOT_REFRESH_BUTTON);
	}
	
	public boolean isSnapshotDownloadEnabled() {
		return isButtonEnabled(BrowserRepository.SETTINGS_SNAPSHOT_DOWNLOAD_BUTTON);
	}
	
	public boolean isSnapshotRollbackEnabled() {
		return isButtonEnabled(BrowserRepository.SETTINGS_SNAPSHOT_ROLLBACK_BUTTON);
	}
	
	public boolean isSnapshotApplyEnabled() {
		return isButtonEnabled(BrowserRepository.SETTINGS_SNAPSHOT_APPLY_BUTTON);
	}
}
