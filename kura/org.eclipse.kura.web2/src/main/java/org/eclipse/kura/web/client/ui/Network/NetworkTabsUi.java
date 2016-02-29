/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Network;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.NavbarNav;
import org.gwtbootstrap3.client.ui.PanelBody;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class NetworkTabsUi extends Composite {

	private static NetworkTabsUiUiBinder uiBinder = GWT.create(NetworkTabsUiUiBinder.class);
	private static final Logger logger = Logger.getLogger(NetworkTabsUi.class.getSimpleName());

	interface NetworkTabsUiUiBinder extends UiBinder<Widget, NetworkTabsUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);

	AnchorListItem hardwareTab;
	AnchorListItem tcpIpTab;
	AnchorListItem dhcpNatTab;
	AnchorListItem wirelessTab;
	AnchorListItem modemTab;
	ArrayList<AnchorListItem> visibleTabs;

	Tab selectedTab;
	TabHardwareUi hardware;
	TabTcpIpUi tcpIp;
	TabDhcpNatUi dhcpNat;
	TabWirelessUi wireless;
	TabModemUi modem;

	GwtNetInterfaceConfig netIfConfig;

	GwtSession session;

	@UiField
	NavbarNav tabsPanel;
	@UiField
	PanelBody content;

	public NetworkTabsUi(GwtSession session) {
		visibleTabs = new ArrayList<AnchorListItem>();
		initWidget(uiBinder.createAndBindUi(this));
		this.session = session;
		initTabs();
	}

	public void setNetInterface(GwtNetInterfaceConfig selection) {
		netIfConfig = selection;
		//initTabs();

		tcpIp.setNetInterface(selection);
		hardware.setNetInterface(selection);
		dhcpNat.setNetInterface(selection);
		wireless.setNetInterface(selection);
		modem.setNetInterface(selection);

		// set the tabs for this interface
		removeInterfaceTabs();

		if (!GwtNetIfStatus.netIPv4StatusDisabled.equals(selection.getStatusEnum())) {
			adjustInterfaceTabs();			
		}

		// refresh all visible tabs
		if (visibleTabs.contains(tcpIpTab)) {
			setSelected(tcpIpTab);
			selectedTab = tcpIp;
			content.clear();
			content.add(tcpIp);
			tcpIp.refresh();
		}
		if (visibleTabs.contains(hardwareTab)) {
			hardware.refresh();
		}
		if (visibleTabs.contains(dhcpNatTab)) {
			dhcpNat.refresh();
		}
		if (visibleTabs.contains(wirelessTab)) {
			wireless.refresh();
		}
		if (visibleTabs.contains(modemTab)) {
			modem.refresh();
		}
	}

	public boolean isDirty() {
		if (tcpIp != null && tcpIp.isDirty()) {
			return true;
		}
		if (hardware !=null && hardware.isDirty()) {
			return true;
		}
		if (dhcpNat !=null && dhcpNat.isDirty()) {
			return true;
		}
		if (wireless != null && wireless.isDirty()) {
			return true;
		}
		if (modem != null && modem.isDirty()) {
			return true;
		}

		return false;
	}

	public void setDirty(boolean b) {
		if (tcpIp != null) tcpIp.setDirty(b);
		if (hardware != null) hardware.setDirty(b);
		if (dhcpNat != null) dhcpNat.setDirty(b);
		if (wireless != null) wireless.setDirty(b);
		if (modem != null) modem.setDirty(b);
	}

	public void refresh() {
		if (tcpIp != null) tcpIp.refresh();
		if (hardware != null) hardware.refresh();
		if (dhcpNat != null) dhcpNat.refresh();
		if (wireless != null) wireless.refresh();
		if (modem != null) modem.refresh();
	}

	// Add/remove tabs based on the selected settings in the various tabs
	public void adjustInterfaceTabs() {
		String netIfStatus = tcpIp.getStatus();
		boolean includeDhcpNat = !tcpIp.isDhcp()
				&& netIfStatus.equals(MessageUtils.get(GwtNetIfStatus.netIPv4StatusEnabledLAN.name()));

		if (netIfConfig instanceof GwtWifiNetInterfaceConfig) {
			// insert Wifi tab
			removeTab(modemTab);
			insertTab(wirelessTab, 1);
			if (!wirelessTab.isEnabled()) {
				wirelessTab.setEnabled(true);
			}
			insertTab(dhcpNatTab, 2);
			// remove Dhcp/Nat Tab if not an access point
			if (!GwtWifiWirelessMode.netWifiWirelessModeAccessPoint	.equals(wireless.getWirelessMode())) {
				includeDhcpNat = false;
			}
		} else if (netIfConfig instanceof GwtModemInterfaceConfig) {
			includeDhcpNat = false;
			removeTab(wirelessTab);
			removeTab(dhcpNatTab);
			// insert Modem tab
			insertTab(modemTab, 1);
			if (!modemTab.isEnabled()) {
				modemTab.setEnabled(true);
			}
		} else {
			removeTab(wirelessTab);
			removeTab(modemTab);

			if (netIfConfig.getHwTypeEnum() == GwtNetIfType.LOOPBACK || netIfConfig.getName().startsWith("mon.wlan")) {
				removeTab(dhcpNatTab);
			} else {
				insertTab(dhcpNatTab, 1);
			}
		}

		if (includeDhcpNat) {
			// enable dhcp/nat tab
			dhcpNatTab.setEnabled(true);
		} else {
			dhcpNatTab.setEnabled(false);
		}

		if (netIfStatus.equals(GwtNetIfStatus.netIPv4StatusDisabled.name())) {
			// disabled - rmove tabs
			disableInterfaceTabs();
		}
	}

	// Get GwtNetInterfaceConfig with current form values updated
	public GwtNetInterfaceConfig getUpdatedInterface() {
		GwtNetInterfaceConfig updatedNetIf = null;
		if (netIfConfig instanceof GwtWifiNetInterfaceConfig) {
			updatedNetIf = new GwtWifiNetInterfaceConfig();
		} else if (netIfConfig instanceof GwtModemInterfaceConfig) {
			updatedNetIf = new GwtModemInterfaceConfig();
		} else {
			updatedNetIf = new GwtNetInterfaceConfig();
		}

		// copy previous values
		updatedNetIf.setProperties(netIfConfig.getProperties());

		// get updated values from visible tabs
		if (visibleTabs.contains(tcpIpTab)) {
			tcpIp.getUpdatedNetInterface(updatedNetIf);
		} 
		if (visibleTabs.contains(hardwareTab)) {
			hardware.getUpdatedNetInterface(updatedNetIf);
		}
		if (visibleTabs.contains(dhcpNatTab)) {
			dhcpNat.getUpdatedNetInterface(updatedNetIf);
		}
		if (visibleTabs.contains(wirelessTab)) {
			wireless.getUpdatedNetInterface(updatedNetIf);
		}
		if (visibleTabs.contains(modemTab)) {
			modem.getUpdatedNetInterface(updatedNetIf);
		}
		return updatedNetIf;
	}

	// return currently selected tab
	public Tab getSelectedTab() {
		return selectedTab;

	}

	// returns true if there are no errors(required fields, invalid values) in
	// all visible tabs
	public boolean isValid() {

		if (visibleTabs.contains(tcpIpTab) && !tcpIp.isValid()) {
			return false;
		}
		if (visibleTabs.contains(hardwareTab) && !hardware.isValid()) {
			return false;
		}
		if (visibleTabs.contains(dhcpNatTab) && !dhcpNat.isValid()) {
			return false;
		}
		if (visibleTabs.contains(wirelessTab) && !wireless.isValid()) {
			return false;
		}
		if (visibleTabs.contains(modemTab) && !modem.isValid()) {
			return false;
		}
		return true;
	}

	// --------Private Methods-----------

	private void initTabs() {

		tabsPanel.clear();
		visibleTabs.clear();

		// Tcp/IP
		tcpIpTab = new AnchorListItem(MSGS.netIPv4());
		visibleTabs.add(tcpIpTab);
		tcpIp = new TabTcpIpUi(session, this);
		tcpIpTab.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setSelected(tcpIpTab);
				selectedTab = tcpIp;
				content.clear();
				content.add(tcpIp);
			}
		});
		tabsPanel.add(tcpIpTab);

		// Wireless
		wirelessTab = new AnchorListItem(MSGS.netWifiWireless());
		visibleTabs.add(wirelessTab);
		wireless = new TabWirelessUi(session, tcpIp, this);
		wirelessTab.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setSelected(wirelessTab);
				selectedTab = wireless;
				content.clear();
				content.add(wireless);
			}
		});
		tabsPanel.add(wirelessTab);

		// Modem
		modemTab = new AnchorListItem(MSGS.netModemCellular());
		visibleTabs.add(modemTab);
		modem = new TabModemUi(session);
		modemTab.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setSelected(modemTab);
				selectedTab = modem;
				content.clear();
				content.add(modem);
			}
		});
		tabsPanel.add(modemTab);

		// DHCP and NAT
		dhcpNatTab = new AnchorListItem(MSGS.netRouter());
		visibleTabs.add(dhcpNatTab);
		dhcpNat = new TabDhcpNatUi(session, tcpIp, wireless);
		dhcpNatTab.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				setSelected(dhcpNatTab);
				selectedTab = dhcpNat;
				content.clear();
				content.add(dhcpNat);
			}
		});
		tabsPanel.add(dhcpNatTab);


		// Hardware
		hardwareTab = new AnchorListItem(MSGS.netHwHardware());
		visibleTabs.add(hardwareTab);
		hardware = new TabHardwareUi(session);
		hardwareTab.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setSelected(hardwareTab);
				selectedTab = hardware;
				content.clear();
				content.add(hardware);
			}
		});
		tabsPanel.add(hardwareTab);

		setSelected(tcpIpTab);
		selectedTab = tcpIp;
		content.clear();
		content.add(tcpIp);

	}

	// Disable wireless,modem and dhcpNat tab
	private void disableInterfaceTabs() {
		visibleTabs.remove(wirelessTab);
		visibleTabs.remove(modemTab);
		visibleTabs.remove(dhcpNatTab);

		wirelessTab.setEnabled(false);
		modemTab.setEnabled(false);
		dhcpNatTab.setEnabled(false);
	}

	private void removeTab(AnchorListItem tab) {
		if (visibleTabs.contains(tab)) {
			visibleTabs.remove(tab);
		}

		if (tabsPanel.getWidgetIndex(tab) > -1) {
			tabsPanel.remove(tab);
		}
	}

	private void insertTab(AnchorListItem tab, int index) {
		logger.info("inserting - " + tab.getText() + " at - " + index);
		if (!visibleTabs.contains(tab)) {
			visibleTabs.add(index, tab);
		}

		if (tabsPanel.getWidgetIndex(tab) == -1) {
			tabsPanel.insert(tab, index);
		}
	}

	// remove wireless,modem and dhcpNat
	private void removeInterfaceTabs() {

		visibleTabs.remove(wirelessTab);
		visibleTabs.remove(modemTab);
		visibleTabs.remove(dhcpNatTab);

		tabsPanel.remove(wirelessTab);
		tabsPanel.remove(modemTab);
		tabsPanel.remove(dhcpNatTab);
	}

	// show the current tab as selected in the UI
	private void setSelected(AnchorListItem item) {
		hardwareTab.setActive(false);
		tcpIpTab.setActive(false);
		dhcpNatTab.setActive(false);
		wirelessTab.setActive(false);
		modemTab.setActive(false);
		item.setActive(true);
	}
}
