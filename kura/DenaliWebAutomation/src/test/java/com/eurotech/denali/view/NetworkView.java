package com.eurotech.denali.view;

import com.eurotech.denali.common.Application;
import com.eurotech.denali.driver.processor.WebDriverActions;
import com.eurotech.denali.objectrepo.BrowserRepository;
import com.eurotech.denali.util.Constants;
import com.eurotech.denali.util.JvmUtil;

public class NetworkView extends WebDriverActions {

	public void selectNetworkView() {
		click(BrowserRepository.NETWORK);
	}

	public boolean isChangesPresent() {
		return isDisplayed(BrowserRepository.NETWORK_CHANGES);
	}

	public boolean isLoopBackInterfacePresent() {
		return isDisplayed(BrowserRepository.NETWORK_LO);
	}

	public String getStatus() {
		return getValue(BrowserRepository.NETWORK_TCP_STATUS);
	}

	public String getConfiguration() {
		return getValue(BrowserRepository.NETWORK_TCP_CONFIGURATION);
	}

	public void selectEth0Interface() {
		click(BrowserRepository.NETWORK_ETH0);
	}

	public boolean isEth1InterfacePresent() {
		return isDisplayed(BrowserRepository.NETWORK_ETH1);
	}

	public void selectEth1Interface() {
		click(BrowserRepository.NETWORK_ETH1);
	}

	public boolean isWlan0InterfacePresent() {
		return isDisplayed(BrowserRepository.NETWORK_WLAN0);
	}

	public void selectWlan0Interface() {
		click(BrowserRepository.NETWORK_WLAN0);
	}

	public boolean isApplyButtonEnabled() {
		return isButtonEnabled(BrowserRepository.NETWORK_APPLY_BUTTON);
	}

	public boolean isRefreshButtonEnabled() {
		return isButtonEnabled(BrowserRepository.NETWORK_REFRESH_BUTTON);
	}

	public boolean isTCPIPTabDisplayed() {
		return isDisplayed(BrowserRepository.NETWORK_INTERFACE_TCPIP);
	}

	public void selectDHCPNATTab() {
		click(BrowserRepository.NETWORK_INTERFACE_DHCP_NAT);
	}

	public boolean isDHCPNATTabEnabled() {
		return isButtonEnabled(BrowserRepository.NETWORK_INTERFACE_DHCP_NAT);
	}

	public boolean isDHCPNATTabPresent() {
		return isPresent(BrowserRepository.NETWORK_INTERFACE_DHCP_NAT);
	}

	public void selectWirelessTab() {
		click(BrowserRepository.NETWORK_INTERFACE_WIRELESS);
	}

	public boolean isWirelessTabEnabled() {
		return isButtonEnabled(BrowserRepository.NETWORK_INTERFACE_WIRELESS);
	}

	public void selectHardwareTab() {
		click(BrowserRepository.NETWORK_INTERFACE_HARDWARE);
	}

	public boolean isHardwareTabEnabled() {
		return isButtonEnabled(BrowserRepository.NETWORK_INTERFACE_HARDWARE);
	}

	public String getHardwareState() {
		return getText(BrowserRepository.NETWORK_INTERFACE_STATE);
	}

	public String getHardwareName() {
		return getText(BrowserRepository.NETWORK_INTERFACE_NAME);
	}

	public String getHardwareType() {
		return getText(BrowserRepository.NETWORK_INTERFACE_TYPE);
	}

	public void selectTCPEnabledForLAN() {
		click(BrowserRepository.NETWORK_TCP_STATUS_DROPDOWN_IMG);
		click(BrowserRepository.NETWORK_TCP_STATUS_ENABLED_LAN);
	}

	public void selectTCPEnabledForWAN() {
		click(BrowserRepository.NETWORK_TCP_STATUS_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_TCP_STATUS_ENABLED_WAN);
	}

	public void selectTCPDisabled() {
		click(BrowserRepository.NETWORK_TCP_STATUS_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_TCP_STATUS_DISABLED);
	}

	public void selectTCPDHCP() {
		click(BrowserRepository.NETWORK_TCP_CONFIGURE_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_TCP_CONFIGURE_DHCP);
	}

	public void selectTCPManually() {
		click(BrowserRepository.NETWORK_TCP_CONFIGURE_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_TCP_CONFIGURE_MANUALLY);
	}

	public void selectDHCPRouterDHCPAndNAT() {
		click(BrowserRepository.NETWORK_DHCP_STATUS_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_DHCP_ROUTER_DHCP_NAT);
	}

	public void selectDHCPRouterOff() {
		click(BrowserRepository.NETWORK_DHCP_STATUS_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_DHCP_ROUTER_OFF);
	}

	public void selectWirelessModeStation() {
		click(BrowserRepository.NETWORK_WIRELESS_MODE_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_WIRELESS_STATION_MODE);
	}

	public void selectWirelessModeAccessPoint() {
		click(BrowserRepository.NETWORK_WIRELESS_MODE_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_WIRELESS_ACCESS_POINT);
	}

	public void selectWirelessRatioModeGB() {
		click(BrowserRepository.NETWORK_WIRELESS_RATIO_MODE_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.NETWORK_WIRELESS_RATIO_GB_VALUE);
	}

	public void selectWirelessSecurity() {
		click(BrowserRepository.NETWORK_WIRELESS_SECURITY_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.WirelessSecurityType.valueOf(
				Application.getNetworkWirelessSecurityType()).getWirelessType());
	}

	public void selectWirelessSecurityWPA2() {
		click(BrowserRepository.NETWORK_WIRELESS_SECURITY_DROPDOWN_IMG);
		JvmUtil.idle(2);
		click(BrowserRepository.WirelessSecurityType.wpa2.getWirelessType());
	}

	public void setWirelessNetworkName() {
		sendKeys(BrowserRepository.NETWORK_WIRELESS_NAME,
				Application.getNetworkWirelessName());
	}

	public void setWirelessDHCPNetworkName() {
		sendKeys(BrowserRepository.NETWORK_WIRELESS_NAME,
				Constants.NETWORK_WIRELESS_DHCP_NAME);
	}

	public void setWirelessNetworkPassword() {
		sendKeys(BrowserRepository.NETWORK_WIRELESS_PASSWORD,
				Application.getNetworkWirelessPassword());
	}

	public void setWirelessDHCPNetworkPassword() {
		sendKeys(BrowserRepository.NETWORK_WIRELESS_PASSWORD,
				Constants.NETWORK_WIRELESS_DHCP_PASSWORD);
	}
	
	public void setWirelessDHCPNetworkVerifyPassword() {
		sendKeys(BrowserRepository.NETWORK_WIRELESS_VERIFY_PASSWORD,
				Constants.NETWORK_WIRELESS_DHCP_PASSWORD);
	}
	
	public void clickWirelessPasswordVerifyButton() {
		click(BrowserRepository.NETWORK_WIRELESS_PASSWORD_VERIFY_BUTTON);
	}

	public boolean getWirelessPasswordVerifyButtonState() {
		String state = getAttribute(
				BrowserRepository.NETWORK_WIRELESS_PASSWORD_VERIFY_BUTTON,
				BrowserRepository.NETWORK_WIRELESS_PASSWORD_VERIFY_BUTTON_ATTRIBUTE);
		return Boolean.valueOf(state);
	}

	public String getWirelessNetworkPassword() {
		return getValue(BrowserRepository.NETWORK_WIRELESS_PASSWORD);
	}
	
	public void setNetworkWlan0IPAddress() {
		sendKeys(BrowserRepository.NETWORK_TCP_IP_ADDRESS,
				Application.getNetworkWlan0StaticIPAddress());
	}

	public void setNetworkWlan0SubnetMask() {
		sendKeys(BrowserRepository.NETWORK_TCP_SUBNET_MASK,
				Application.getNetworkWlan0StaticSubnetMask());
	}

	public void setDHCPWlan0BeginAddress() {
		sendKeys(BrowserRepository.NETWORK_DHCP_BEGIN_ADDR,
				Application.getNetworkDHCPWlan0BeginAddress());
	}

	public void setDHCPWlan0EndAddress() {
		sendKeys(BrowserRepository.NETWORK_DHCP_END_ADDR,
				Application.getNetworkDHCPWlan0EndAddress());
	}

	public void setDHCPWlan0SubnetMask() {
		sendKeys(BrowserRepository.NETWORK_DHCP_SUBNET_MASK,
				Application.getNetworkDHCPWlan0SubnetMask());
	}

	public void setDHCPWlan0DeafultLeaseTime() {
		sendKeys(BrowserRepository.NETWORK_DHCP_DEFAULT_LEASE_TIME,
				Application.getNetworkDHCPWlan0DefaultLeaseTime());
	}

	public void setDHCPWlan0MaxLeaseTime() {
		sendKeys(BrowserRepository.NETWORK_DHCP_MAX_LEASE_TIME,
				Application.getNetworkDHCPWlan0MaxLeaseTime());
	}
	
	public void setDHCPEth0BeginAddress() {
		sendKeys(BrowserRepository.NETWORK_DHCP_BEGIN_ADDR,
				Application.getNetworkDHCPEth0BeginAddress());
	}

	public void setDHCPEth0EndAddress() {
		sendKeys(BrowserRepository.NETWORK_DHCP_END_ADDR,
				Application.getNetworkDHCPEth0EndAddress());
	}

	public void setDHCPEth0SubnetMask() {
		sendKeys(BrowserRepository.NETWORK_DHCP_SUBNET_MASK,
				Application.getNetworkDHCPEth0SubnetMask());
	}

	public void setDHCPEth0DeafultLeaseTime() {
		sendKeys(BrowserRepository.NETWORK_DHCP_DEFAULT_LEASE_TIME,
				Application.getNetworkDHCPEth0DefaultLeaseTime());
	}

	public void setDHCPEth0MaxLeaseTime() {
		sendKeys(BrowserRepository.NETWORK_DHCP_MAX_LEASE_TIME,
				Application.getNetworkDHCPEth0MaxLeaseTime());
	}

	public void selectDHCPPassDNSServer() {
		click(BrowserRepository.NETWORK_DHCP_PASS_DNS_SERVER_TRUE);
	}

	public void setNetworkEth0IPAddress() {
		sendKeys(BrowserRepository.NETWORK_TCP_IP_ADDRESS,
				Application.getNetworkEth0StaticIPAddress());
	}

	public void setNetworkGateway() {
		sendKeys(BrowserRepository.NETWORK_TCP_GATEWAY,
				Application.getNetworkEth0StaticGateway());
	}

	public void clearNetworkGateway() {
		sendKeys(BrowserRepository.NETWORK_TCP_GATEWAY, Constants.EMPTY);
	}

	public void setNetworkEth0SubnetMask() {
		sendKeys(BrowserRepository.NETWORK_TCP_SUBNET_MASK,
				Application.getNetworkEth0StaticSubnetMask());
	}

	public void setNetworkDNSServer() {
		sendKeys(BrowserRepository.NETWORK_TCP_DNS_SERVER,
				Application.getNetworkEth0StaticDNSServer());
	}

	public void clearNetworkDNSServer() {
		sendKeys(BrowserRepository.NETWORK_TCP_DNS_SERVER, Constants.EMPTY);
	}

	public void setNetworkSubnetMaskSecondary() {
		sendKeys(BrowserRepository.NETWORK_TCP_SUBNET_MASK,
				Application.getNetworkEth0StaticSubnetMaskSecondary());
	}

	public void setNetworkDNSServerSecondary() {
		sendKeys(BrowserRepository.NETWORK_TCP_DNS_SERVER,
				Application.getNetworkEth0StaticDNSServerSecondary());
	}

	public String getNetworkSubnetMask() {
		return getValue(BrowserRepository.NETWORK_TCP_SUBNET_MASK);
	}

	public String getNetworkDNSServer() {
		return getValue(BrowserRepository.NETWORK_TCP_DNS_SERVER);
	}

	public void setNetworkSearchDomains() {
		sendKeys(BrowserRepository.NETWORK_TCP_SEARCH_DOMAINS, Constants.EMPTY);
	}

	public void setNetworkEth1IPAddress() {
		sendKeys(BrowserRepository.NETWORK_TCP_IP_ADDRESS,
				Application.getNetworkEth1DHCPIPAddress());
	}

	public void clickApplyButton() {
		click(BrowserRepository.NETWORK_APPLY_BUTTON);
	}

	public void clickRefreshButton() {
		click(BrowserRepository.NETWORK_REFRESH_BUTTON);
	}
}
