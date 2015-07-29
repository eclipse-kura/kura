package com.eurotech.denali.view;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class PackageView extends WebDriverActions {

	public void selectPackagesView() {
		click(BrowserRepository.PACKAGES);
	}
	
	public boolean isRefreshButtonEnabled() {
		return isButtonEnabled(BrowserRepository.PACKAGES_REFRESH_BUTTON);
	}
	
	public boolean isInstallButtonEnabled() {
		return isButtonEnabled(BrowserRepository.PACKAGES_INSTALL_BUTTON);
	}
	
	public boolean isUninstallButtonEnabled() {
		return isButtonEnabled(BrowserRepository.PACKAGES_UNINSTALL_BUTTON);
	}
}
