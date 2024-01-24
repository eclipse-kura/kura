/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Areti
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.awt.Color;
import java.nio.channels.AsynchronousCloseException;
import java.util.LinkedList;
import java.util.List;
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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;

public class NetworkTabsUi extends Composite {

    private static final String WIFI_ACCESS_POINT = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name();
    private static final String IPV4_STATUS_DISABLED_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusDisabled.name());
    private static final String IPV4_STATUS_UNMANAGED_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusUnmanaged.name());
    private static final String IPV4_STATUS_ENABLED_LAN_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusEnabledLAN.name());
    private static final String IPV6_STATUS_DISABLED_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv6StatusDisabled.name());

    private static NetworkTabsUiUiBinder uiBinder = GWT.create(NetworkTabsUiUiBinder.class);

    interface NetworkTabsUiUiBinder extends UiBinder<Widget, NetworkTabsUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private boolean isNet2 = false;

    AnchorListItem ip4TabAnchorItem;
    AnchorListItem ip6TabAnchorItem;
    AnchorListItem dhcp4NatTabAnchorItem;
    AnchorListItem wirelessTabAnchorItem;
    AnchorListItem net8021xTabAnchorItem;
    AnchorListItem modemTabAnchorItem;
    AnchorListItem modemGpsTabAnchorItem;
    AnchorListItem modemAntennaTabAnchorItem;
    AnchorListItem hardwareTabAnchorItem;
    AnchorListItem advancedTabAnchorItem;
    List<AnchorListItem> visibleTabs;

    NetworkTab selectedTab;
    TabHardwareUi hardwareTab;
    TabIp4Ui ip4Tab;
    TabIp6Ui ip6Tab;
    TabDhcp4NatUi dhcp4NatTab;
    TabWirelessUi wirelessTab;
    Tab8021xUi set8021xTab;
    TabModemUi modemTab;
    TabModemGpsUi modemGpsTab;
    TabModemAntennaUi modemAntennaTab;
    TabAdvancedUi advancedTab;

    GwtNetInterfaceConfig netIfConfig;
    NetworkButtonBarUi buttons;

    GwtSession session;

    @UiField
    NavbarNav tabsPanel;
    @UiField
    PanelBody content;

    /*
     * Initialization
     */

    public NetworkTabsUi(GwtSession session, final boolean isNet2) {
        initWidget(uiBinder.createAndBindUi(this));
        this.visibleTabs = new LinkedList<>();
        this.session = session;
        this.isNet2 = isNet2;
        initTabs(isNet2);
    }

    private void initTabs(final boolean isNet2) {
        this.tabsPanel.clear();
        this.visibleTabs.clear();

        initIp4Tab(isNet2);
        initIp6Tab();
        initWireless8021xTab();
        initWirelessTab(isNet2);
        initModemTab();
        initModemGpsTab();
        initModemAntennaTab();
        initDhcp4NatTab();
        initHardwareTab();
        initAdvancedTab();

        setSelected(this.ip4TabAnchorItem);
        this.selectedTab = this.ip4Tab;
        this.content.clear();
        this.content.add(this.ip4Tab);
    }

    private void initIp4Tab(final boolean isNet2) {
        this.ip4TabAnchorItem = new AnchorListItem(MSGS.netIPv4());
        this.ip4Tab = new TabIp4Ui(this.session, this, isNet2);

        this.ip4TabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.ip4TabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.ip4Tab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.ip4Tab);
        });
    }

    private void initIp6Tab() {
        this.ip6TabAnchorItem = new AnchorListItem(MSGS.netIPv6());
        this.ip6Tab = new TabIp6Ui(this.session, this);

        this.ip6TabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.ip6TabAnchorItem);
            NetworkTabsUi.this.selectedTab = this.ip6Tab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.ip6Tab);
        });
    }

    private void initWireless8021xTab() {
        this.net8021xTabAnchorItem = new AnchorListItem(MSGS.netWifiWireless8021x());
        this.set8021xTab = new Tab8021xUi(this.session, this, this.ip4Tab, this.ip6Tab);

        this.net8021xTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.net8021xTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.set8021xTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.set8021xTab);
        });
    }

    private void initWirelessTab(final boolean isNet2) {
        this.wirelessTabAnchorItem = new AnchorListItem(MSGS.netWifiWireless());
        this.wirelessTab = new TabWirelessUi(this.session, this.ip4Tab, this.ip6Tab, this.net8021xTabAnchorItem, this,
                isNet2);

        this.wirelessTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.wirelessTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.wirelessTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.wirelessTab);
        });
    }

    private void initModemTab() {
        this.modemTabAnchorItem = new AnchorListItem(MSGS.netModemCellular());
        this.modemTab = new TabModemUi(this.session, this.ip4Tab, this);

        this.modemTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemTab);
        });
    }

    private void initModemGpsTab() {
        this.modemGpsTabAnchorItem = new AnchorListItem(MSGS.netModemGps());
        this.modemGpsTab = new TabModemGpsUi(this.session, this);

        this.modemGpsTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemGpsTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemGpsTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemGpsTab);
        });
    }

    private void initModemAntennaTab() {
        this.modemAntennaTabAnchorItem = new AnchorListItem(MSGS.netModemAntenna());
        this.modemAntennaTab = new TabModemAntennaUi(this.session, this);

        this.modemAntennaTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemAntennaTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemAntennaTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemAntennaTab);
        });
    }

    private void initDhcp4NatTab() {
        this.dhcp4NatTabAnchorItem = new AnchorListItem(MSGS.netRouter());
        this.dhcp4NatTab = new TabDhcp4NatUi(this.session, this.ip4Tab, this.wirelessTab, this);

        this.dhcp4NatTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.dhcp4NatTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.dhcp4NatTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.dhcp4NatTab);
        });
    }

    private void initHardwareTab() {
        this.hardwareTabAnchorItem = new AnchorListItem(MSGS.netHwHardware());
        this.hardwareTab = new TabHardwareUi(this.session);

        this.hardwareTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.hardwareTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.hardwareTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.hardwareTab);
        });
    }

    private void initAdvancedTab() {
        this.advancedTabAnchorItem = new AnchorListItem(MSGS.netAdvanced());
        this.advancedTab = new TabAdvancedUi(this.session, this);

        this.advancedTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.advancedTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.advancedTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.advancedTab);
        });
    }

    public void setButtons(NetworkButtonBarUi buttons) {
        this.buttons = buttons;
    }

    public NetworkButtonBarUi getButtons() {
        return this.buttons;
    }

    /*
     * Tab update
     */

    public void setNetInterface(GwtNetInterfaceConfig selection) {
        this.netIfConfig = selection;

        this.ip4Tab.setNetInterface(selection);
        this.ip6Tab.setNetInterface(selection);
        this.dhcp4NatTab.setNetInterface(selection);
        this.wirelessTab.setNetInterface(selection);
        this.set8021xTab.setNetInterface(selection);
        this.modemTab.setNetInterface(selection);
        this.modemGpsTab.setNetInterface(selection);
        this.modemAntennaTab.setNetInterface(selection);
        this.hardwareTab.setNetInterface(selection);
        this.advancedTab.setNetInterface(selection);

        updateTabs();
        refreshAllVisibleTabs();
    }

    public void updateTabs() {
        removeAllTabs();

        insertTab(this.ip4TabAnchorItem);

        if (this.isNet2) {
            insertTab(this.ip6TabAnchorItem);
        }

        arrangeOptionalTabs();

        if (this.isNet2) {
            insertTab(this.advancedTabAnchorItem);
        }
        insertTab(this.hardwareTabAnchorItem);
    }

    private void removeAllTabs() {
        removeTab(this.ip4TabAnchorItem);
        removeTab(this.ip6TabAnchorItem);
        removeTab(this.wirelessTabAnchorItem);
        removeTab(this.net8021xTabAnchorItem);
        removeTab(this.dhcp4NatTabAnchorItem);
        removeTab(this.modemTabAnchorItem);
        removeTab(this.modemGpsTabAnchorItem);
        removeTab(this.modemAntennaTabAnchorItem);
        removeTab(this.hardwareTabAnchorItem);
        removeTab(this.advancedTabAnchorItem);
    }

    private void arrangeOptionalTabs() {
        boolean isIpv4EnabledLAN = this.ip4Tab.getStatus().equals(IPV4_STATUS_ENABLED_LAN_MESSAGE);
        boolean isIpv4Disabled = this.ip4Tab.getStatus().equals(IPV4_STATUS_DISABLED_MESSAGE);
        boolean isIpv6Disabled = this.ip6Tab.getStatus().equals(IPV6_STATUS_DISABLED_MESSAGE);
        boolean isWirelessAP = this.wirelessTab.getWirelessMode() != null
                && this.wirelessTab.getWirelessMode().name().equals(WIFI_ACCESS_POINT);
        boolean isDhcp = this.ip4Tab.isDhcp();

        boolean includeDhcpNat = !isDhcp && isIpv4EnabledLAN;

        InterfaceConfigWrapper wrapper = new InterfaceConfigWrapper(this.netIfConfig);

        if (wrapper.isWireless()) {
            showWirelessTabs(isIPInterfacesDisabled(isIpv4Disabled, isIpv6Disabled) || isUnmanagedSelected());
            if (!isWirelessAP) {
                includeDhcpNat = false;
            }
        } else if (wrapper.isModem()) {
            includeDhcpNat = false;
            this.modemGpsTabAnchorItem.setEnabled(wrapper.isGpsSupported() && !isUnmanagedSelected());
            showModemTabs(isIPInterfacesDisabled(isIpv4Disabled, isIpv6Disabled) || isUnmanagedSelected());
        } else {
            showEthernetTabs();
            if (wrapper.isLoopback()) {
                removeTab(this.dhcp4NatTabAnchorItem);
            } else {
                insertTab(this.dhcp4NatTabAnchorItem);
            }
        }

        this.dhcp4NatTabAnchorItem.setEnabled(includeDhcpNat);
        this.ip6TabAnchorItem.setEnabled(!isUnmanagedSelected());

        if (isIPInterfacesDisabled(isIpv4Disabled, isIpv6Disabled) || isUnmanagedSelected()) {
            removeOptionalTabs();
        }
    }

    private boolean isIPInterfacesDisabled(boolean isIpv4Disabled, boolean isIpv6Disabled) {
        return isIpv4Disabled && isIpv6Disabled;
    }

    private void showWirelessTabs(boolean interfaceNotEnabled) {
        removeTab(this.modemTabAnchorItem);
        removeTab(this.modemGpsTabAnchorItem);
        removeTab(this.modemAntennaTabAnchorItem);

        this.wirelessTabAnchorItem.setEnabled(!interfaceNotEnabled);

        insertTab(this.wirelessTabAnchorItem);
        if (this.isNet2) {
            if (interfaceNotEnabled) {
                this.net8021xTabAnchorItem.setEnabled(false);
            }
            insertTab(this.net8021xTabAnchorItem);
        }

        insertTab(this.dhcp4NatTabAnchorItem);
    }

    private void showModemTabs(boolean interfaceNotEnabled) {
        removeTab(this.wirelessTabAnchorItem);
        removeTab(this.dhcp4NatTabAnchorItem);

        this.modemTabAnchorItem.setEnabled(!interfaceNotEnabled);
        this.modemAntennaTabAnchorItem.setEnabled(isModemLTE());

        insertTab(this.modemTabAnchorItem);
        insertTab(this.modemGpsTabAnchorItem);
        if (isModemLTE()) {
            insertTab(this.modemAntennaTabAnchorItem);
        }
    }

    private void showEthernetTabs() {
        removeTab(this.wirelessTabAnchorItem);
        removeTab(this.modemTabAnchorItem);
        removeTab(this.modemGpsTabAnchorItem);
        removeTab(this.modemAntennaTabAnchorItem);
    }

    private boolean isModemLTE() {
        for (String techType : ((GwtModemInterfaceConfig) this.netIfConfig).getNetworkTechnology()) {
            if ("LTE".equals(techType)) {
                return true;
            }
        }
        return false;
    }

    private void removeOptionalTabs() {
        this.visibleTabs.remove(this.wirelessTabAnchorItem);
        this.visibleTabs.remove(this.net8021xTabAnchorItem);
        this.visibleTabs.remove(this.dhcp4NatTabAnchorItem);
        this.visibleTabs.remove(this.modemTabAnchorItem);
    }

    private void refreshAllVisibleTabs() {
        if (this.visibleTabs.contains(this.ip4TabAnchorItem)) {
            setSelected(this.ip4TabAnchorItem);
            this.selectedTab = this.ip4Tab;
            this.content.clear();
            this.content.add(this.ip4Tab);
            this.ip4Tab.refresh();
        }
        if (this.visibleTabs.contains(this.ip6TabAnchorItem)) {
            this.ip6Tab.refresh();
        }
        if (this.visibleTabs.contains(this.hardwareTabAnchorItem)) {
            this.hardwareTab.refresh();
        }
        if (this.visibleTabs.contains(this.dhcp4NatTabAnchorItem)) {
            this.dhcp4NatTab.refresh();
        }
        if (this.visibleTabs.contains(this.wirelessTabAnchorItem)) {
            this.wirelessTab.refresh();
        }
        if (this.visibleTabs.contains(this.modemTabAnchorItem)) {
            this.modemTab.refresh();
        }
        if (this.visibleTabs.contains(this.modemGpsTabAnchorItem)) {
            this.modemGpsTab.refresh();
        }
        if (this.visibleTabs.contains(this.modemAntennaTabAnchorItem)) {
            this.modemAntennaTab.refresh();
        }
        if (this.visibleTabs.contains(this.net8021xTabAnchorItem)) {
            this.set8021xTab.refresh();
        }
        if(this.visibleTabs.contains(this.advancedTabAnchorItem)) {
        	this.advancedTab.refresh();
        }
    }

    /*
     * UI methods
     */

    public boolean isDirty() {
        if ((this.visibleTabs.contains(this.ip4TabAnchorItem) && this.ip4Tab.isDirty())
                || (this.visibleTabs.contains(this.ip6TabAnchorItem) && this.ip6Tab.isDirty())) {
            return true;
        }

        if (this.visibleTabs.contains(this.hardwareTabAnchorItem) && this.hardwareTab.isDirty()) {
            return true;
        }

        if (this.visibleTabs.contains(this.dhcp4NatTabAnchorItem) && this.dhcp4NatTab.isDirty()) {
            return true;
        }

        if (this.visibleTabs.contains(this.wirelessTabAnchorItem) && this.wirelessTab.isDirty()) {
            return true;
        }

        if (this.visibleTabs.contains(this.modemTabAnchorItem) && this.modemTab.isDirty()) {
            return true;
        }

        if (this.visibleTabs.contains(this.modemGpsTabAnchorItem) && this.modemGpsTab.isDirty()) {
            return true;
        }

        if (this.visibleTabs.contains(this.modemAntennaTabAnchorItem) && this.modemAntennaTab.isDirty()) {
            return true;
        }

        if (this.visibleTabs.contains(this.net8021xTabAnchorItem) && this.set8021xTab.isDirty()) {
            return true;
        }
        
        if (this.visibleTabs.contains(this.advancedTabAnchorItem) && this.advancedTab.isDirty()) {
            return true;
        }

        return false;
    }

    public void setDirty(boolean isDirty) {
        this.ip4Tab.setDirty(isDirty);
        this.ip6Tab.setDirty(isDirty);
        this.hardwareTab.setDirty(isDirty);
        this.dhcp4NatTab.setDirty(isDirty);
        this.wirelessTab.setDirty(isDirty);
        this.set8021xTab.setDirty(isDirty);
        this.modemTab.setDirty(isDirty);
        this.modemGpsTab.setDirty(isDirty);
        this.modemAntennaTab.setDirty(isDirty);
        this.advancedTab.setDirty(isDirty);
    }

    public void refresh() {
        this.ip4Tab.refresh();
        this.ip6Tab.refresh();
        this.hardwareTab.refresh();
        this.dhcp4NatTab.refresh();
        this.wirelessTab.refresh();
        this.set8021xTab.refresh();
        this.modemTab.refresh();
        this.modemGpsTab.refresh();
        this.modemAntennaTab.refresh();
        this.advancedTab.refresh();
    }

    public GwtNetInterfaceConfig getUpdatedInterface() {
        GwtNetInterfaceConfig updatedNetIf = new GwtNetInterfaceConfig();

        if (this.netIfConfig instanceof GwtWifiNetInterfaceConfig) {
            updatedNetIf = new GwtWifiNetInterfaceConfig();
        } else if (this.netIfConfig instanceof GwtModemInterfaceConfig) {
            updatedNetIf = new GwtModemInterfaceConfig();
        }

        updatedNetIf.setProperties(this.netIfConfig.getProperties());

        if (this.visibleTabs.contains(this.ip4TabAnchorItem)) {
            this.ip4Tab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.ip6TabAnchorItem)) {
            this.ip6Tab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.hardwareTabAnchorItem)) {
            this.hardwareTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.dhcp4NatTabAnchorItem)) {
            this.dhcp4NatTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.wirelessTabAnchorItem)) {
            this.wirelessTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.modemTabAnchorItem)) {
            this.modemTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.modemGpsTabAnchorItem)) {
            this.modemGpsTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.modemAntennaTabAnchorItem)) {
            this.modemAntennaTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.net8021xTabAnchorItem)) {
            this.set8021xTab.getUpdatedNetInterface(updatedNetIf);
        }
        if (this.visibleTabs.contains(this.advancedTabAnchorItem)) {
        	this.advancedTab.getUpdatedNetInterface(updatedNetIf);
        }

        return updatedNetIf;
    }

    public NetworkTab getSelectedTab() {
        return this.selectedTab;
    }

    public boolean isValid() {

        clearErrorTab(this.ip4TabAnchorItem);
        clearErrorTab(this.ip6TabAnchorItem);
        clearErrorTab(this.hardwareTabAnchorItem);
        clearErrorTab(this.dhcp4NatTabAnchorItem);
        clearErrorTab(this.wirelessTabAnchorItem);
        clearErrorTab(this.modemTabAnchorItem);
        clearErrorTab(this.modemGpsTabAnchorItem);
        clearErrorTab(this.modemAntennaTabAnchorItem);
        clearErrorTab(this.net8021xTabAnchorItem);
        clearErrorTab(this.advancedTabAnchorItem);

        if (this.visibleTabs.contains(this.ip4TabAnchorItem) && !this.ip4Tab.isValid()) {
            errorTab(this.ip4TabAnchorItem);
            return false;
        }

        if (this.visibleTabs.contains(this.ip6TabAnchorItem) && this.ip6TabAnchorItem.isEnabled()
                && !this.ip6Tab.isValid()) {
            errorTab(this.ip6TabAnchorItem);
            return false;
        }

        if (this.visibleTabs.contains(this.hardwareTabAnchorItem) && !this.hardwareTab.isValid()) {
            errorTab(this.hardwareTabAnchorItem);
            return false;
        }

        if (this.visibleTabs.contains(this.dhcp4NatTabAnchorItem) && this.dhcp4NatTabAnchorItem.isEnabled()
                && !this.dhcp4NatTab.isValid()) {
            errorTab(this.dhcp4NatTabAnchorItem);
            return false;
        }

        if ((this.visibleTabs.contains(this.wirelessTabAnchorItem) && !this.wirelessTab.isValid())
                || (this.visibleTabs.contains(this.modemTabAnchorItem) && !this.modemTab.isValid())) {
            errorTab(this.wirelessTabAnchorItem);
            errorTab(this.modemTabAnchorItem);
            return false;
        }

        if (this.visibleTabs.contains(this.modemGpsTabAnchorItem) && this.modemGpsTabAnchorItem.isEnabled()
                && !this.modemGpsTab.isValid()) {
            errorTab(this.modemGpsTabAnchorItem);
            return false;
        }

        if (this.visibleTabs.contains(this.modemAntennaTabAnchorItem) && this.modemAntennaTabAnchorItem.isEnabled()
                && !this.modemAntennaTab.isValid()) {
            errorTab(this.modemAntennaTabAnchorItem);
            return false;
        }

        if (this.visibleTabs.contains(this.net8021xTabAnchorItem) && this.net8021xTabAnchorItem.isEnabled()
                && !this.set8021xTab.isValid()) {
            errorTab(this.net8021xTabAnchorItem);
            return false;
        }
        
        if (this.visibleTabs.contains(this.advancedTabAnchorItem) && this.advancedTabAnchorItem.isEnabled()
                && !this.advancedTab.isValid()) {
            errorTab(this.advancedTabAnchorItem);
            return false;
        }

        return true;
    }

    public boolean isUnmanagedSelected() {
        return this.ip4Tab.getStatus().equals(IPV4_STATUS_UNMANAGED_MESSAGE);
    }

    /*
     * Utilities
     */

    private void errorTab(AnchorListItem tab) {
        tab.addStyleName("alert-danger");
    }

    private void clearErrorTab(AnchorListItem tab) {
        tab.removeStyleName("alert-danger");
    }

    private void removeTab(AnchorListItem tab) {
        this.visibleTabs.remove(tab);
        this.tabsPanel.remove(tab);
    }

    private void insertTab(AnchorListItem tab) {
        if (!this.visibleTabs.contains(tab)) {
            this.visibleTabs.add(tab);
        }

        if (this.tabsPanel.getWidgetIndex(tab) == -1) {
            this.tabsPanel.add(tab);
        }
    }

    private void setSelected(AnchorListItem item) {
        this.hardwareTabAnchorItem.setActive(false);
        this.ip4TabAnchorItem.setActive(false);
        this.ip6TabAnchorItem.setActive(false);
        this.dhcp4NatTabAnchorItem.setActive(false);
        this.wirelessTabAnchorItem.setActive(false);
        this.net8021xTabAnchorItem.setActive(false);
        this.modemTabAnchorItem.setActive(false);
        this.modemGpsTabAnchorItem.setActive(false);
        this.modemAntennaTabAnchorItem.setActive(false);
        this.advancedTabAnchorItem.setActive(false);
        item.setActive(true);
    }

    class InterfaceConfigWrapper {

        private final GwtNetInterfaceConfig config;

        public InterfaceConfigWrapper(GwtNetInterfaceConfig config) {
            this.config = config;
        }

        public boolean isWireless() {
            return this.config instanceof GwtWifiNetInterfaceConfig;
        }

        public boolean isModem() {
            return this.config instanceof GwtModemInterfaceConfig;
        }

        public boolean isGpsSupported() {
            if (this.config instanceof GwtModemInterfaceConfig) {
                return ((GwtModemInterfaceConfig) this.config).isGpsSupported();
            }

            return false;
        }

        public boolean isLoopback() {
            return this.config.getHwTypeEnum() == GwtNetIfType.LOOPBACK || this.config.getName().startsWith("mon.wlan");
        }
    }
}
