/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.ArrayList;

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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class NetworkTabsUi extends Composite {

    private static final String WIFI_ACCESS_POINT = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name();
    private static final String IPV4_STATUS_DISABLED_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusDisabled.name());
    private static final String IPV4_STATUS_UNMANAGED_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusUnmanaged.name());
    private static final String IPV4_STATUS_ENABLED_LAN_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusEnabledLAN.name());

    private static NetworkTabsUiUiBinder uiBinder = GWT.create(NetworkTabsUiUiBinder.class);

    interface NetworkTabsUiUiBinder extends UiBinder<Widget, NetworkTabsUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    AnchorListItem hardwareTab;
    AnchorListItem tcpIpTab;
    AnchorListItem dhcpNatTab;
    AnchorListItem wirelessTab;
    AnchorListItem modemTab;
    AnchorListItem modemGpsTab;
    AnchorListItem modemAntennaTab;
    ArrayList<AnchorListItem> visibleTabs;

    NetworkTab selectedTab;
    TabHardwareUi hardware;
    TabTcpIpUi tcpIp;
    TabDhcpNatUi dhcpNat;
    TabWirelessUi wireless;
    TabModemUi modem;
    TabModemGpsUi modemGps;
    TabModemAntennaUi modemAntenna;

    GwtNetInterfaceConfig netIfConfig;

    GwtSession session;

    @UiField
    NavbarNav tabsPanel;
    @UiField
    PanelBody content;

    public NetworkTabsUi(GwtSession session) {
        this.visibleTabs = new ArrayList<>();
        initWidget(uiBinder.createAndBindUi(this));
        this.session = session;
        initTabs();
    }

    public void setNetInterface(GwtNetInterfaceConfig selection) {
        this.netIfConfig = selection;
        // initTabs();

        this.tcpIp.setNetInterface(selection);
        this.hardware.setNetInterface(selection);
        this.wireless.setNetInterface(selection);
        this.dhcpNat.setNetInterface(selection);
        this.modem.setNetInterface(selection);
        this.modemGps.setNetInterface(selection);
        this.modemAntenna.setNetInterface(selection);

        // set the tabs for this interface
        removeInterfaceTabs();

        if (!GwtNetIfStatus.netIPv4StatusDisabled.equals(selection.getStatusEnum())) {
            adjustInterfaceTabs();
        }

        // refresh all visible tabs
        if (this.visibleTabs.contains(this.tcpIpTab)) {
            setSelected(this.tcpIpTab);
            this.selectedTab = this.tcpIp;
            this.content.clear();
            this.content.add(this.tcpIp);
            this.tcpIp.refresh();
        }
        if (this.visibleTabs.contains(this.hardwareTab)) {
            this.hardware.refresh();
        }
        if (this.visibleTabs.contains(this.dhcpNatTab)) {
            this.dhcpNat.refresh();
        }
        if (this.visibleTabs.contains(this.wirelessTab)) {
            this.wireless.refresh();
        }
        if (this.visibleTabs.contains(this.modemTab)) {
            this.modem.refresh();
        }
        if (this.visibleTabs.contains(this.modemGpsTab)) {
            this.modemGps.refresh();
        }
        if (this.visibleTabs.contains(this.modemAntennaTab)) {
            this.modemAntenna.refresh();
        }
    }

    public boolean isDirty() {
        if (this.tcpIp != null && this.visibleTabs.contains(this.tcpIpTab) && this.tcpIp.isDirty()) {
            return true;
        }
        if (this.hardware != null && this.visibleTabs.contains(this.hardwareTab) && this.hardware.isDirty()) {
            return true;
        }
        if (this.dhcpNat != null && this.visibleTabs.contains(this.dhcpNatTab) && this.dhcpNat.isDirty()) {
            return true;
        }
        if (this.wireless != null && this.visibleTabs.contains(this.wirelessTab) && this.wireless.isDirty()) {
            return true;
        }
        if (this.modem != null && this.visibleTabs.contains(this.modemTab) && this.modem.isDirty()) {
            return true;
        }
        if (this.modemGps != null && this.visibleTabs.contains(this.modemGpsTab) && this.modemGps.isDirty()) {
            return true;
        }
        if (this.modemAntenna != null && this.visibleTabs.contains(this.modemAntennaTab)
                && this.modemAntenna.isDirty()) {
            return true;
        }
        return false;
    }

    public void setDirty(boolean b) {
        if (this.tcpIp != null) {
            this.tcpIp.setDirty(b);
        }
        if (this.hardware != null) {
            this.hardware.setDirty(b);
        }
        if (this.dhcpNat != null) {
            this.dhcpNat.setDirty(b);
        }
        if (this.wireless != null) {
            this.wireless.setDirty(b);
        }
        if (this.modem != null) {
            this.modem.setDirty(b);
        }
        if (this.modemGps != null) {
            this.modemGps.setDirty(b);
        }
        if (this.modemAntenna != null) {
            this.modemAntenna.setDirty(b);
        }
    }

    public void refresh() {
        if (this.tcpIp != null) {
            this.tcpIp.refresh();
        }
        if (this.hardware != null) {
            this.hardware.refresh();
        }
        if (this.dhcpNat != null) {
            this.dhcpNat.refresh();
        }
        if (this.wireless != null) {
            this.wireless.refresh();
        }
        if (this.modem != null) {
            this.modem.refresh();
        }
        if (this.modemGps != null) {
            this.modemGps.refresh();
        }
        if (this.modemAntenna != null) {
            this.modemAntenna.refresh();
        }
    }

    // Add/remove tabs based on the selected settings in the various tabs
    public void adjustInterfaceTabs() {
        String netIfStatus = this.tcpIp.getStatus();
        boolean includeDhcpNat = !this.tcpIp.isDhcp() && netIfStatus.equals(IPV4_STATUS_ENABLED_LAN_MESSAGE);

        if (this.netIfConfig instanceof GwtWifiNetInterfaceConfig) {
            removeTab(this.modemTab);
            removeTab(this.modemGpsTab);
            removeTab(this.modemAntennaTab);
            insertTab(this.wirelessTab, 1);
            if (!this.wirelessTab.isEnabled()) {
                this.wirelessTab.setEnabled(true);
            }
            insertTab(this.dhcpNatTab, 2);
            // remove Dhcp/Nat Tab if not an access point
            String mode = this.wireless.getWirelessMode().name();
            if (mode != null && !mode.equals(WIFI_ACCESS_POINT)) {
                includeDhcpNat = false;
            }
        } else if (this.netIfConfig instanceof GwtModemInterfaceConfig) {
            includeDhcpNat = false;

            removeTab(this.wirelessTab);
            removeTab(this.dhcpNatTab);
            // insert Modem tab
            insertTab(this.modemTab, 1);
            if (!this.modemTab.isEnabled()) {
                this.modemTab.setEnabled(true);
            }
            insertTab(this.modemGpsTab, 2);
            if (isModemLTE()) {
                insertTab(this.modemAntennaTab, 3);
            }
        } else {
            removeTab(this.wirelessTab);
            removeTab(this.modemTab);
            removeTab(this.modemGpsTab);
            removeTab(this.modemAntennaTab);
            if (this.netIfConfig.getHwTypeEnum() == GwtNetIfType.LOOPBACK
                    || this.netIfConfig.getName().startsWith("mon.wlan")) {
                removeTab(this.dhcpNatTab);
            } else {
                insertTab(this.dhcpNatTab, 1);
            }
        }

        if (includeDhcpNat) {
            // enable dhcp/nat tab
            this.dhcpNatTab.setEnabled(true);
        } else {
            this.dhcpNatTab.setEnabled(false);
        }

        if (netIfStatus.equals(IPV4_STATUS_DISABLED_MESSAGE) || netIfStatus.equals(IPV4_STATUS_UNMANAGED_MESSAGE)) {
            // disabled - remove tabs
            disableInterfaceTabs();
        }

        if (this.netIfConfig instanceof GwtModemInterfaceConfig) {
            if (((GwtModemInterfaceConfig) this.netIfConfig).isGpsSupported()
                    && !netIfStatus.equals(IPV4_STATUS_UNMANAGED_MESSAGE)) {
                this.modemGpsTab.setEnabled(true);
            } else {
                this.modemGpsTab.setEnabled(false);
            }
            if (isModemLTE()) {
                this.modemAntennaTab.setEnabled(true);
            } else {
                this.modemAntennaTab.setEnabled(false);
            }
        }
    }

    // Get GwtNetInterfaceConfig with current form values updated
    public GwtNetInterfaceConfig getUpdatedInterface() {
        GwtNetInterfaceConfig updatedNetIf = null;
        if (this.netIfConfig instanceof GwtWifiNetInterfaceConfig) {
            updatedNetIf = new GwtWifiNetInterfaceConfig();
        } else if (this.netIfConfig instanceof GwtModemInterfaceConfig) {
            updatedNetIf = new GwtModemInterfaceConfig();
        } else {
            updatedNetIf = new GwtNetInterfaceConfig();
        }

        // copy previous values
        updatedNetIf.setProperties(this.netIfConfig.getProperties());

        // get updated values from visible tabs
        if (this.visibleTabs.contains(this.tcpIpTab)) {
            this.tcpIp.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.hardwareTab)) {
            this.hardware.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.dhcpNatTab)) {
            this.dhcpNat.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.wirelessTab)) {
            this.wireless.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.modemTab)) {
            this.modem.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.modemGpsTab)) {
            this.modemGps.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.modemAntennaTab)) {
            this.modemAntenna.getUpdatedNetInterface(updatedNetIf);
        }
        return updatedNetIf;
    }

    // return currently selected tab
    public NetworkTab getSelectedTab() {
        return this.selectedTab;

    }

    // returns true if there are no errors(required fields, invalid values) in
    // all visible tabs
    public boolean isValid() {

        if (this.visibleTabs.contains(this.tcpIpTab) && !this.tcpIp.isValid()) {
            return false;
        }
        if (this.visibleTabs.contains(this.hardwareTab) && !this.hardware.isValid()) {
            return false;
        }
        if (this.visibleTabs.contains(this.dhcpNatTab) && !this.dhcpNat.isValid()) {
            return false;
        }
        if (this.visibleTabs.contains(this.wirelessTab) && !this.wireless.isValid()) {
            return false;
        }
        if (this.visibleTabs.contains(this.modemTab) && !this.modem.isValid()) {
            return false;
        }
        if (this.visibleTabs.contains(this.modemGpsTab) && !this.modemGps.isValid()) {
            return false;
        }
        if (this.visibleTabs.contains(this.modemAntennaTab) && !this.modemAntenna.isValid()) {
            return false;
        }
        return true;
    }

    // --------Private Methods-----------

    private void initTabs() {

        this.tabsPanel.clear();
        this.visibleTabs.clear();

        // Tcp/IP
        this.tcpIpTab = new AnchorListItem(MSGS.netIPv4());
        this.visibleTabs.add(this.tcpIpTab);
        this.tcpIp = new TabTcpIpUi(this.session, this);
        this.tcpIpTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.tcpIpTab);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.tcpIp;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.tcpIp);
        });
        this.tabsPanel.add(this.tcpIpTab);

        // Wireless
        this.wirelessTab = new AnchorListItem(MSGS.netWifiWireless());
        this.visibleTabs.add(this.wirelessTab);
        this.wireless = new TabWirelessUi(this.session, this.tcpIp, this);
        this.wirelessTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.wirelessTab);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.wireless;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.wireless);
        });
        this.tabsPanel.add(this.wirelessTab);

        // Modem
        this.modemTab = new AnchorListItem(MSGS.netModemCellular());
        this.visibleTabs.add(this.modemTab);
        this.modem = new TabModemUi(this.session, this.tcpIp);
        this.modemTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemTab);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modem;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modem);
        });
        this.tabsPanel.add(this.modemTab);

        // Modem Gps
        this.modemGpsTab = new AnchorListItem(MSGS.netModemGps());
        this.visibleTabs.add(this.modemGpsTab);
        this.modemGps = new TabModemGpsUi(this.session);
        this.modemGpsTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemGpsTab);
            NetworkTabsUi.this.modemGps.refresh();  // TODO: to check if needed here or can be invoked elsewhere
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemGps;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemGps);
        });
        this.tabsPanel.add(this.modemGpsTab);

        // Modem Antenna
        this.modemAntennaTab = new AnchorListItem(MSGS.netModemAntenna());
        this.visibleTabs.add(this.modemAntennaTab);
        this.modemAntenna = new TabModemAntennaUi(this.session);
        this.modemAntennaTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemAntennaTab);
            NetworkTabsUi.this.modemAntenna.refresh();  // TODO: to check if needed here or can be invoked elsewhere
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemAntenna;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemAntenna);
        });
        this.tabsPanel.add(this.modemAntennaTab);

        // DHCP and NAT
        this.dhcpNatTab = new AnchorListItem(MSGS.netRouter());
        this.visibleTabs.add(this.dhcpNatTab);
        this.dhcpNat = new TabDhcpNatUi(this.session, this.tcpIp, this.wireless);
        this.dhcpNatTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.dhcpNatTab);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.dhcpNat;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.dhcpNat);
        });
        this.tabsPanel.add(this.dhcpNatTab);

        // Hardware
        this.hardwareTab = new AnchorListItem(MSGS.netHwHardware());
        this.visibleTabs.add(this.hardwareTab);
        this.hardware = new TabHardwareUi(this.session);
        this.hardwareTab.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.hardwareTab);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.hardware;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.hardware);
        });
        this.tabsPanel.add(this.hardwareTab);

        setSelected(this.tcpIpTab);
        this.selectedTab = this.tcpIp;
        this.content.clear();
        this.content.add(this.tcpIp);

    }

    // Disable wireless,modem and dhcpNat tab
    private void disableInterfaceTabs() {
        this.visibleTabs.remove(this.wirelessTab);
        this.visibleTabs.remove(this.modemTab);
        this.visibleTabs.remove(this.dhcpNatTab);

        this.wirelessTab.setEnabled(false);
        this.modemTab.setEnabled(false);
        this.modemGpsTab.setEnabled(false);
        this.modemAntennaTab.setEnabled(false);
        this.dhcpNatTab.setEnabled(false);
    }

    private void removeTab(AnchorListItem tab) {
        if (this.visibleTabs.contains(tab)) {
            this.visibleTabs.remove(tab);
        }

        if (this.tabsPanel.getWidgetIndex(tab) > -1) {
            this.tabsPanel.remove(tab);
        }
    }

    private void insertTab(AnchorListItem tab, int index) {
        if (!this.visibleTabs.contains(tab)) {
            this.visibleTabs.add(index, tab);
        }

        if (this.tabsPanel.getWidgetIndex(tab) == -1) {
            this.tabsPanel.insert(tab, index);
        }
    }

    // remove wireless,modem and dhcpNat
    private void removeInterfaceTabs() {

        this.visibleTabs.remove(this.wirelessTab);
        this.visibleTabs.remove(this.modemTab);
        this.visibleTabs.remove(this.modemGpsTab);
        this.visibleTabs.remove(this.modemAntennaTab);
        this.visibleTabs.remove(this.dhcpNatTab);

        this.tabsPanel.remove(this.wirelessTab);
        this.tabsPanel.remove(this.modemTab);
        this.tabsPanel.remove(this.dhcpNatTab);
    }

    // show the current tab as selected in the UI
    private void setSelected(AnchorListItem item) {
        this.hardwareTab.setActive(false);
        this.tcpIpTab.setActive(false);
        this.dhcpNatTab.setActive(false);
        this.wirelessTab.setActive(false);
        this.modemTab.setActive(false);
        this.modemGpsTab.setActive(false);
        this.modemAntennaTab.setActive(false);
        item.setActive(true);
    }

    private boolean isModemLTE() {
        for (String techType : ((GwtModemInterfaceConfig) this.netIfConfig).getNetworkTechnology()) {
            if ("LTE".equals(techType)) {
                return true;
            }
        }
        return false;
    }
}
