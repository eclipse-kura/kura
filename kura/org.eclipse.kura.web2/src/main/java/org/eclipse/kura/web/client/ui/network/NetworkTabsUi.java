/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.NavbarNav;
import org.gwtbootstrap3.client.ui.PanelBody;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
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

    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    private boolean isNet2 = false;
    private boolean isIpv4EnabledLAN = false;
    private boolean isIpv4Disabled = true;
    private boolean isIpv4Unmanaged = false;
    private boolean isWirelessAP = false;
    private boolean isDhcp = false;
    private boolean isGpsSupported = false;

    AnchorListItem hardwareTabAnchorItem;
    AnchorListItem ip4TabAnchorItem;
    AnchorListItem dhcp4NatTabAnchorItem;
    AnchorListItem wirelessTabAnchorItem;
    AnchorListItem modemTabAnchorItem;
    AnchorListItem modemGpsTabAnchorItem;
    AnchorListItem modemAntennaTabAnchorItem;
    ArrayList<AnchorListItem> visibleTabs;

    NetworkTab selectedTab;
    TabHardwareUi hardwareTab;
    TabIp4Ui ip4Tab;
    TabDhcp4NatUi dhcp4NatTab;
    TabWirelessUi wirelessTab;
    TabModemUi modemTab;
    TabModemGpsUi modemGpsTab;
    TabModemAntennaUi modemAntennaTab;

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

    public NetworkTabsUi(GwtSession session) {
        this.visibleTabs = new ArrayList<>();
        initWidget(uiBinder.createAndBindUi(this));
        this.session = session;
        detectIfNet2();
        initTabs();
    }

    private void detectIfNet2() {
        this.gwtNetworkService.isNet2(new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                NetworkTabsUi.this.isNet2 = false;
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(Boolean result) {
                NetworkTabsUi.this.isNet2 = result;
                updateTabs();
            }
        });
    }

    private void initTabs() {
        this.tabsPanel.clear();
        this.visibleTabs.clear();

        initIp4Tab();
        initWirelessTab();
        initModemTab();
        initModemGpsTab();
        initModemAntennaTab();
        initDhcp4NatTab();
        initHardwareTab();

        setSelected(this.ip4TabAnchorItem);
        this.selectedTab = this.ip4Tab;
        this.content.clear();
        this.content.add(this.ip4Tab);
    }

    private void initIp4Tab() {
        this.ip4TabAnchorItem = new AnchorListItem(MSGS.netIPv4());
        this.visibleTabs.add(this.ip4TabAnchorItem);
        this.ip4Tab = new TabIp4Ui(this.session, this);
        this.ip4TabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.ip4TabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.ip4Tab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.ip4Tab);
        });
        this.tabsPanel.add(this.ip4TabAnchorItem);
    }

    private void initWirelessTab() {
        this.wirelessTabAnchorItem = new AnchorListItem(MSGS.netWifiWireless());
        this.visibleTabs.add(this.wirelessTabAnchorItem);
        this.wirelessTab = new TabWirelessUi(this.session, this.ip4Tab, this);
        this.wirelessTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.wirelessTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.wirelessTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.wirelessTab);
        });
        this.tabsPanel.add(this.wirelessTabAnchorItem);
    }

    private void initModemTab() {
        this.modemTabAnchorItem = new AnchorListItem(MSGS.netModemCellular());
        this.visibleTabs.add(this.modemTabAnchorItem);
        this.modemTab = new TabModemUi(this.session, this.ip4Tab, this);
        this.modemTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemTab);
        });
        this.tabsPanel.add(this.modemTabAnchorItem);
    }

    private void initModemGpsTab() {
        this.modemGpsTabAnchorItem = new AnchorListItem(MSGS.netModemGps());
        this.visibleTabs.add(this.modemGpsTabAnchorItem);
        this.modemGpsTab = new TabModemGpsUi(this.session, this);
        this.modemGpsTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemGpsTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemGpsTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemGpsTab);
        });
        this.tabsPanel.add(this.modemGpsTabAnchorItem);
    }

    private void initModemAntennaTab() {
        this.modemAntennaTabAnchorItem = new AnchorListItem(MSGS.netModemAntenna());
        this.visibleTabs.add(this.modemAntennaTabAnchorItem);
        this.modemAntennaTab = new TabModemAntennaUi(this.session, this);
        this.modemAntennaTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.modemAntennaTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.modemAntennaTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.modemAntennaTab);
        });
        this.tabsPanel.add(this.modemAntennaTabAnchorItem);
    }

    private void initDhcp4NatTab() {
        this.dhcp4NatTabAnchorItem = new AnchorListItem(MSGS.netRouter());
        this.visibleTabs.add(this.dhcp4NatTabAnchorItem);
        this.dhcp4NatTab = new TabDhcp4NatUi(this.session, this.ip4Tab, this.wirelessTab, this);
        this.dhcp4NatTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.dhcp4NatTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.dhcp4NatTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.dhcp4NatTab);
        });
        this.tabsPanel.add(this.dhcp4NatTabAnchorItem);
    }

    private void initHardwareTab() {
        this.hardwareTabAnchorItem = new AnchorListItem(MSGS.netHwHardware());
        this.visibleTabs.add(this.hardwareTabAnchorItem);
        this.hardwareTab = new TabHardwareUi(this.session);
        this.hardwareTabAnchorItem.addClickHandler(event -> {
            setSelected(NetworkTabsUi.this.hardwareTabAnchorItem);
            NetworkTabsUi.this.selectedTab = NetworkTabsUi.this.hardwareTab;
            NetworkTabsUi.this.content.clear();
            NetworkTabsUi.this.content.add(NetworkTabsUi.this.hardwareTab);
        });
        this.tabsPanel.add(this.hardwareTabAnchorItem);
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
        this.hardwareTab.setNetInterface(selection);
        this.dhcp4NatTab.setNetInterface(selection);
        this.wirelessTab.setNetInterface(selection);
        this.modemTab.setNetInterface(selection);
        this.modemGpsTab.setNetInterface(selection);
        this.modemAntennaTab.setNetInterface(selection);

        // seems to be not needed
        // this.visibleTabs.clear();
        removeOptionalTabs();
        updateTabs();
        refreshAllVisibleTabs();
    }

    private void removeOptionalTabs() {
        this.tabsPanel.remove(this.wirelessTabAnchorItem);
        this.tabsPanel.remove(this.modemTabAnchorItem);
        this.tabsPanel.remove(this.dhcp4NatTabAnchorItem);
    }

    public void updateTabs() {
        getCurrentConfiguration();

        boolean includeDhcpNat = !this.isDhcp && this.isIpv4EnabledLAN;

        if (this.netIfConfig instanceof GwtWifiNetInterfaceConfig) {
            showWirelessTabs();
            if (!this.isWirelessAP) {
                includeDhcpNat = false;
            }
        } else if (this.netIfConfig instanceof GwtModemInterfaceConfig) {
            includeDhcpNat = false;
            this.modemGpsTabAnchorItem.setEnabled(this.isGpsSupported && !this.isIpv4Unmanaged);
            showModemTabs();
        } else {
            showEthernetTabs();
        }

        this.dhcp4NatTabAnchorItem.setEnabled(includeDhcpNat);

        if (this.isIpv4Disabled || this.isIpv4Unmanaged) {
            disableOptionalTabs();
        }
    }

    private void getCurrentConfiguration() {
        this.isIpv4EnabledLAN = this.ip4Tab.getStatus().equals(IPV4_STATUS_ENABLED_LAN_MESSAGE);
        this.isIpv4Disabled = this.ip4Tab.getStatus().equals(IPV4_STATUS_DISABLED_MESSAGE);
        this.isIpv4Unmanaged = this.ip4Tab.getStatus().equals(IPV4_STATUS_UNMANAGED_MESSAGE);
        this.isWirelessAP = this.wirelessTab.getWirelessMode() != null
                && this.wirelessTab.getWirelessMode().name().equals(WIFI_ACCESS_POINT);
        this.isDhcp = this.ip4Tab.isDhcp();

        if (this.netIfConfig instanceof GwtModemInterfaceConfig) {
            this.isGpsSupported = ((GwtModemInterfaceConfig) this.netIfConfig).isGpsSupported();
        }
    }

    private void showWirelessTabs() {
        removeTab(this.modemTabAnchorItem);
        removeTab(this.modemGpsTabAnchorItem);
        removeTab(this.modemAntennaTabAnchorItem);

        this.wirelessTabAnchorItem.setEnabled(true);

        insertTab(this.wirelessTabAnchorItem, 1);
        insertTab(this.dhcp4NatTabAnchorItem, 2);
    }

    private void showModemTabs() {
        removeTab(this.wirelessTabAnchorItem);
        removeTab(this.dhcp4NatTabAnchorItem);

        this.modemTabAnchorItem.setEnabled(true);
        this.modemAntennaTabAnchorItem.setEnabled(isModemLTE());

        insertTab(this.modemTabAnchorItem, 1);
        insertTab(this.modemGpsTabAnchorItem, 2);
        if (isModemLTE()) {
            insertTab(this.modemAntennaTabAnchorItem, 3);
        }
    }

    private void showEthernetTabs() {
        removeTab(this.wirelessTabAnchorItem);
        removeTab(this.modemTabAnchorItem);
        removeTab(this.modemGpsTabAnchorItem);
        removeTab(this.modemAntennaTabAnchorItem);

        if (this.netIfConfig.getHwTypeEnum() == GwtNetIfType.LOOPBACK
                || this.netIfConfig.getName().startsWith("mon.wlan")) {
            removeTab(this.dhcp4NatTabAnchorItem);
        } else {
            insertTab(this.dhcp4NatTabAnchorItem, 1);
        }
    }

    private boolean isModemLTE() {
        for (String techType : ((GwtModemInterfaceConfig) this.netIfConfig).getNetworkTechnology()) {
            if ("LTE".equals(techType)) {
                return true;
            }
        }
        return false;
    }

    private void disableOptionalTabs() {
        this.visibleTabs.remove(this.wirelessTabAnchorItem);
        this.visibleTabs.remove(this.modemTabAnchorItem);
        this.visibleTabs.remove(this.dhcp4NatTabAnchorItem);

        this.wirelessTabAnchorItem.setEnabled(false);
        this.modemTabAnchorItem.setEnabled(false);
        this.modemGpsTabAnchorItem.setEnabled(false);
        this.modemAntennaTabAnchorItem.setEnabled(false);
        this.dhcp4NatTabAnchorItem.setEnabled(false);
    }

    private void refreshAllVisibleTabs() {
        if (this.visibleTabs.contains(this.ip4TabAnchorItem)) {
            setSelected(this.ip4TabAnchorItem);
            this.selectedTab = this.ip4Tab;
            this.content.clear();
            this.content.add(this.ip4Tab);
            this.ip4Tab.refresh();
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
    }

    /*
     * UI methods
     */

    public boolean isDirty() {
        if (this.visibleTabs.contains(this.ip4TabAnchorItem) && this.ip4Tab.isDirty()) {
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

        return false;
    }

    public void setDirty(boolean isDirty) {
        this.ip4Tab.setDirty(isDirty);
        this.hardwareTab.setDirty(isDirty);
        this.dhcp4NatTab.setDirty(isDirty);
        this.wirelessTab.setDirty(isDirty);
        this.modemTab.setDirty(isDirty);
        this.modemGpsTab.setDirty(isDirty);
        this.modemAntennaTab.setDirty(isDirty);
    }

    public void refresh() {
        this.ip4Tab.refresh();
        this.hardwareTab.refresh();
        this.dhcp4NatTab.refresh();
        this.wirelessTab.refresh();
        this.modemTab.refresh();
        this.modemGpsTab.refresh();
        this.modemAntennaTab.refresh();
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

        return updatedNetIf;
    }

    public NetworkTab getSelectedTab() {
        return this.selectedTab;
    }

    public boolean isValid() {
        if (this.visibleTabs.contains(this.ip4TabAnchorItem) && !this.ip4Tab.isValid()) {
            return false;
        }

        if (this.visibleTabs.contains(this.hardwareTabAnchorItem) && !this.hardwareTab.isValid()) {
            return false;
        }

        if (this.visibleTabs.contains(this.dhcp4NatTabAnchorItem) && !this.dhcp4NatTab.isValid()) {
            return false;
        }

        if (this.visibleTabs.contains(this.wirelessTabAnchorItem) && !this.wirelessTab.isValid()) {
            return false;
        }

        if (this.visibleTabs.contains(this.modemTabAnchorItem) && !this.modemTab.isValid()) {
            return false;
        }

        if (this.visibleTabs.contains(this.modemGpsTabAnchorItem) && !this.modemGpsTab.isValid()) {
            return false;
        }

        if (this.visibleTabs.contains(this.modemAntennaTabAnchorItem) && !this.modemAntennaTab.isValid()) {
            return false;
        }

        return true;
    }

    /*
     * Utilities
     */

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

    // show the current tab as selected in the UI
    private void setSelected(AnchorListItem item) {
        this.hardwareTabAnchorItem.setActive(false);
        this.ip4TabAnchorItem.setActive(false);
        this.dhcp4NatTabAnchorItem.setActive(false);
        this.wirelessTabAnchorItem.setActive(false);
        this.modemTabAnchorItem.setActive(false);
        this.modemGpsTabAnchorItem.setActive(false);
        this.modemAntennaTabAnchorItem.setActive(false);
        item.setActive(true);
    }
}
