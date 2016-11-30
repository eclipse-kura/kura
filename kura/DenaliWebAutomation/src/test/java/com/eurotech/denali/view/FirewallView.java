package com.eurotech.denali.view;

import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;

public class FirewallView extends WebDriverActions {

	public void selectFirewallView() {
		click(BrowserRepository.FIREWALL);
	}
	
	public void selectPortForwardTab() {
		click(BrowserRepository.FIREWALL_PORT_FORWARD);
	}

	public void selectNATTab() {
		click(BrowserRepository.FIREWALL_NAT);
	}

	public boolean isPortForwardApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_PORT_FORWARD_APPLY_BUTTON);
	}

	public boolean isPortForwardNewButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_PORT_FORWARD_NEW_BUTTON);
	}

	public boolean isPortForwardEditButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_PORT_FORWARD_EDIT_BUTTON);
	}

	public boolean isPortForwardDeleteButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_PORT_FORWARD_DELETE_BUTTON);
	}

	public boolean isOpenPortApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_OPEN_PORT_APPLY_BUTTON);
	}

	public boolean isOpenPortNewButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_OPEN_PORT_NEW_BUTTON);
	}

	public boolean isOpenPortEditButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_OPEN_PORT_EDIT_BUTTON);
	}

	public boolean isOpenPortDeleteButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_OPEN_PORT_DELETE_BUTTON);
	}

	public boolean isNatApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_NAT_APPLY_BUTTON);
	}

	public boolean isNatNewButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_NAT_NEW_BUTTON);
	}

	public boolean isNatEditButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_NAT_EDIT_BUTTON);
	}

	public boolean isNatDeleteButtonEnabled() {
		return isButtonEnabled(BrowserRepository.FIREWALL_NAT_DELETE_BUTTON);
	}

	public int getOpenPortEth0Interface() {
		return getWebElements(BrowserRepository.FIREWALL_OPEN_PORT_ETH0_INTERFACE).size();
	}

	public int getOpenPortEth1Interface() {
		return getWebElements(BrowserRepository.FIREWALL_OPEN_PORT_ETH1_INTERFACE).size();
	}

	public int getOpenPortWlanInterface() {
		return getWebElements(BrowserRepository.FIREWALL_OPEN_PORT_WLAN_INTERFACE).size();
	}
}
