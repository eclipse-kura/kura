/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetRouterMode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabDhcpNatUi extends Composite implements NetworkTab {

    private static final String ROUTER_OFF_MESSAGE = MessageUtils.get(GwtNetRouterMode.netRouterOff.name());
    private static final String ROUTER_NAT_MESSAGE = MessageUtils.get(GwtNetRouterMode.netRouterNat.name());
    private static final String WIFI_DISABLED = GwtWifiWirelessMode.netWifiWirelessModeDisabled.name();
    private static final String WIFI_STATION_MODE = GwtWifiWirelessMode.netWifiWirelessModeStation.name();
    private static TabDhcpNatUiUiBinder uiBinder = GWT.create(TabDhcpNatUiUiBinder.class);

    interface TabDhcpNatUiUiBinder extends UiBinder<Widget, TabDhcpNatUi> {
    }

    private static final String REGEX_IPV4 = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
    private static final Messages MSGS = GWT.create(Messages.class);
    GwtSession session;
    TabTcpIpUi tcpTab;
    TabWirelessUi wirelessTab;
    Boolean dirty;
    GwtNetInterfaceConfig selectedNetIfConfig;

    @UiField
    Form form;

    @UiField
    FormLabel labelRouter;
    @UiField
    FormLabel labelBegin;
    @UiField
    FormLabel labelEnd;
    @UiField
    FormLabel labelSubnet;
    @UiField
    FormLabel labelDefaultL;
    @UiField
    FormLabel labelMax;
    @UiField
    FormLabel labelPass;

    @UiField
    ListBox router;

    @UiField
    TextBox begin;
    @UiField
    TextBox end;
    @UiField
    TextBox subnet;
    @UiField
    TextBox defaultL;
    @UiField
    TextBox max;

    @UiField
    InlineRadio radio1;
    @UiField
    InlineRadio radio2;

    @UiField
    FormGroup groupRouter;
    @UiField
    FormGroup groupBegin;
    @UiField
    FormGroup groupEnd;
    @UiField
    FormGroup groupSubnet;
    @UiField
    FormGroup groupDefaultL;
    @UiField
    FormGroup groupMax;

    @UiField
    HelpBlock helpRouter;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    HelpButton routerHelp;
    @UiField
    HelpButton beginHelp;
    @UiField
    HelpButton endHelp;
    @UiField
    HelpButton subnetHelp;
    @UiField
    HelpButton defaultLHelp;
    @UiField
    HelpButton maxHelp;
    @UiField
    HelpButton passHelp;

    public TabDhcpNatUi(GwtSession currentSession, TabTcpIpUi tcp, TabWirelessUi wireless) {
        initWidget(uiBinder.createAndBindUi(this));
        this.tcpTab = tcp;
        this.wirelessTab = wireless;
        this.session = currentSession;
        setDirty(false);
        initForm();

        initHelpButtons();

        this.tcpTab.status.addChangeHandler(event -> update());

        this.wirelessTab.wireless.addChangeHandler(event -> update());
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        if (this.groupRouter.getValidationState().equals(ValidationState.ERROR)
                || this.groupBegin.getValidationState().equals(ValidationState.ERROR)
                || this.groupEnd.getValidationState().equals(ValidationState.ERROR)
                || this.groupSubnet.getValidationState().equals(ValidationState.ERROR)
                || this.groupDefaultL.getValidationState().equals(ValidationState.ERROR)
                || this.groupMax.getValidationState().equals(ValidationState.ERROR)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        this.selectedNetIfConfig = config;
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            resetValidations();
            if (this.selectedNetIfConfig == null) {
                reset();
            } else {
                update();
            }
        }
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        if (this.session != null) {

            for (GwtNetRouterMode mode : GwtNetRouterMode.values()) {
                if (MessageUtils.get(mode.name()).equals(this.router.getSelectedItemText())) {
                    updatedNetIf.setRouterMode(mode.name());
                }
            }
            updatedNetIf.setRouterDhcpBeginAddress(this.begin.getText());
            updatedNetIf.setRouterDhcpEndAddress(this.end.getText());
            updatedNetIf.setRouterDhcpSubnetMask(this.subnet.getText());
            if (this.defaultL.getText() != null && !"".equals(this.defaultL.getText().trim())) {
                updatedNetIf.setRouterDhcpDefaultLease(Integer.parseInt(this.defaultL.getText()));
            }
            if (this.max.getText() != null && !"".equals(this.max.getText().trim())) {
                updatedNetIf.setRouterDhcpMaxLease(Integer.parseInt(this.max.getText()));
            }
            updatedNetIf.setRouterDnsPass(this.radio1.getValue());

        }
    }

    @Override
    public void clear() {
        // Not needed
    }

    // ----------PRIVATE METHODS-------------

    private void update() {
        if (this.selectedNetIfConfig != null) {
            for (int i = 0; i < this.router.getItemCount(); i++) {
                if (this.router.getItemText(i).equals(MessageUtils.get(this.selectedNetIfConfig.getRouterMode()))) {
                    this.router.setSelectedIndex(i);
                    break;
                }
            }
            this.begin.setText(this.selectedNetIfConfig.getRouterDhcpBeginAddress());
            this.end.setText(this.selectedNetIfConfig.getRouterDhcpEndAddress());
            this.subnet.setText(this.selectedNetIfConfig.getRouterDhcpSubnetMask());
            this.defaultL.setText(String.valueOf(this.selectedNetIfConfig.getRouterDhcpDefaultLease()));
            this.max.setText(String.valueOf(this.selectedNetIfConfig.getRouterDhcpMaxLease()));
            this.radio1.setValue(this.selectedNetIfConfig.getRouterDnsPass());
            this.radio2.setValue(!this.selectedNetIfConfig.getRouterDnsPass());

        }
        refreshForm();
    }

    // enable/disable fields depending on values in other tabs
    private void refreshForm() {
        // if (!tcpTab.isLanEnabled()) {
        // router.setEnabled(false);
        // begin.setEnabled(false);
        // end.setEnabled(false);
        // subnet.setEnabled(false);
        // defaultL.setEnabled(false);
        // max.setEnabled(false);
        // radio1.setEnabled(false);
        // radio2.setEnabled(false);
        // } else {
        GwtWifiConfig wifiConfig = this.wirelessTab.activeConfig;
        String wifiMode = null;
        if (wifiConfig != null) {
            wifiMode = wifiConfig.getWirelessMode();
        }
        if (this.selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.WIFI && wifiMode != null
                && (wifiMode.equals(WIFI_STATION_MODE) || wifiMode.equals(WIFI_DISABLED))) {
            this.router.setEnabled(false);
            this.begin.setEnabled(false);
            this.end.setEnabled(false);
            this.subnet.setEnabled(false);
            this.defaultL.setEnabled(false);
            this.max.setEnabled(false);
            this.radio1.setEnabled(false);
            this.radio2.setEnabled(false);
        } else {
            this.router.setEnabled(true);
            this.begin.setEnabled(true);
            this.end.setEnabled(true);
            this.subnet.setEnabled(true);
            this.defaultL.setEnabled(true);
            this.max.setEnabled(true);
            this.radio1.setEnabled(true);
            this.radio2.setEnabled(true);

            String modeValue = this.router.getSelectedItemText();
            if (modeValue.equals(ROUTER_NAT_MESSAGE) || modeValue.equals(ROUTER_OFF_MESSAGE)) {
                this.router.setEnabled(true);
                this.begin.setEnabled(false);
                this.end.setEnabled(false);
                this.subnet.setEnabled(false);
                this.defaultL.setEnabled(false);
                this.max.setEnabled(false);
                this.radio1.setEnabled(false);
                this.radio2.setEnabled(false);
            } else {
                this.router.setEnabled(true);
                this.begin.setEnabled(true);
                this.end.setEnabled(true);
                this.subnet.setEnabled(true);
                this.defaultL.setEnabled(true);
                this.max.setEnabled(true);
                this.radio1.setEnabled(true);
                this.radio2.setEnabled(true);
            }
        }
        // }
    }

    private void reset() {
        this.router.setSelectedIndex(0);
        this.begin.setText("");
        this.end.setText("");
        this.subnet.setText("");
        this.defaultL.setText("");
        this.max.setText("");
        this.radio1.setValue(true);
        this.radio2.setValue(false);
        update();
    }

    private void resetValidations() {
        this.groupRouter.setValidationState(ValidationState.NONE);
        this.groupBegin.setValidationState(ValidationState.NONE);
        this.groupEnd.setValidationState(ValidationState.NONE);
        this.groupSubnet.setValidationState(ValidationState.NONE);
        this.groupDefaultL.setValidationState(ValidationState.NONE);
        this.groupMax.setValidationState(ValidationState.NONE);

        this.helpRouter.setText("");
    }

    private void initHelpButtons() {
        this.routerHelp.setHelpText(MSGS.netRouterToolTipMode());
        this.beginHelp.setHelpText(MSGS.netRouterToolTipDhcpBeginAddr());
        this.endHelp.setHelpText(MSGS.netRouterToolTipDhcpEndAddr());
        this.subnetHelp.setHelpText(MSGS.netRouterToolTipDhcpSubnet());
        this.defaultLHelp.setHelpText(MSGS.netRouterToolTipDhcpDefaultLeaseTime());
        this.maxHelp.setHelpText(MSGS.netRouterToolTipDhcpMaxLeaseTime());
        this.passHelp.setHelpText(MSGS.netRouterToolTipPassDns());
    }

    private void initForm() {
        // Router Mode
        this.labelRouter.setText(MSGS.netRouterMode());
        int i = 0;
        for (GwtNetRouterMode mode : GwtNetRouterMode.values()) {
            this.router.addItem(MessageUtils.get(mode.name()));

            if (this.tcpTab.isDhcp() && mode.equals(GwtNetRouterMode.netRouterOff)) {
                this.router.setSelectedIndex(i);
            }
            i++;
        }
        this.router.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipMode()));
            }
        });
        this.router.addMouseOutHandler(event -> resetHelp());
        this.router.addChangeHandler(event -> {
            setDirty(true);
            ListBox box = (ListBox) event.getSource();
            if (TabDhcpNatUi.this.tcpTab.isDhcp() && !box.getSelectedItemText().equals(ROUTER_OFF_MESSAGE)) { // MessageUtils.get(GwtNetRouterMode.netRouterOff.name())))
                // { TODO:check
                TabDhcpNatUi.this.groupRouter.setValidationState(ValidationState.ERROR);
                TabDhcpNatUi.this.helpRouter.setText(MSGS.netRouterConfiguredForDhcpError());
                TabDhcpNatUi.this.helpRouter.setColor("red");
            } else {
                TabDhcpNatUi.this.groupRouter.setValidationState(ValidationState.NONE);
                TabDhcpNatUi.this.helpRouter.setText("");
            }
            refreshForm();
        });

        // DHCP Beginning Address
        this.labelBegin.setText(MSGS.netRouterDhcpBeginningAddress());
        this.begin.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipDhcpBeginAddr()));
            }
        });
        this.begin.addMouseOutHandler(event -> resetHelp());
        this.begin.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabDhcpNatUi.this.begin.getText().matches(REGEX_IPV4)) {
                TabDhcpNatUi.this.groupBegin.setValidationState(ValidationState.ERROR);
            } else {
                TabDhcpNatUi.this.groupBegin.setValidationState(ValidationState.NONE);
            }
        });

        // DHCP Ending Address
        this.labelEnd.setText(MSGS.netRouterDhcpEndingAddress());
        this.end.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipDhcpEndAddr()));
            }
        });
        this.end.addMouseOutHandler(event -> resetHelp());
        this.end.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabDhcpNatUi.this.end.getText().matches(REGEX_IPV4)) {
                TabDhcpNatUi.this.groupEnd.setValidationState(ValidationState.ERROR);
            } else {
                TabDhcpNatUi.this.groupEnd.setValidationState(ValidationState.NONE);
            }
        });

        // DHCP Subnet Mask
        this.labelSubnet.setText(MSGS.netRouterDhcpSubnetMask());
        this.subnet.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipDhcpSubnet()));
            }
        });
        this.subnet.addMouseOutHandler(event -> resetHelp());
        this.subnet.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabDhcpNatUi.this.subnet.getText().matches(REGEX_IPV4)) {
                TabDhcpNatUi.this.groupSubnet.setValidationState(ValidationState.ERROR);
            } else {
                TabDhcpNatUi.this.groupSubnet.setValidationState(ValidationState.NONE);
            }
        });

        // DHCP Default Lease
        this.labelDefaultL.setText(MSGS.netRouterDhcpDefaultLease());
        this.defaultL.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipDhcpDefaultLeaseTime()));
            }
        });
        this.defaultL.addMouseOutHandler(event -> resetHelp());
        this.defaultL.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabDhcpNatUi.this.defaultL.getText().trim().matches(FieldType.NUMERIC.getRegex())) {
                TabDhcpNatUi.this.groupDefaultL.setValidationState(ValidationState.ERROR);
            } else {
                TabDhcpNatUi.this.groupDefaultL.setValidationState(ValidationState.NONE);
            }
        });

        // DHCP Max Lease
        this.labelMax.setText(MSGS.netRouterDhcpMaxLease());
        this.max.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipDhcpMaxLeaseTime()));
            }
        });
        this.max.addMouseOutHandler(event -> resetHelp());
        this.max.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabDhcpNatUi.this.max.getText().trim().matches(FieldType.NUMERIC.getRegex())) {
                TabDhcpNatUi.this.groupMax.setValidationState(ValidationState.ERROR);
            } else {
                TabDhcpNatUi.this.groupMax.setValidationState(ValidationState.NONE);
            }
        });

        // Pass DNS
        this.labelPass.setText(MSGS.netRouterPassDns());
        this.radio1.setText(MSGS.trueLabel());
        this.radio2.setText(MSGS.falseLabel());
        this.radio1.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipPassDns()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio2.addMouseOverHandler(event -> {
            if (TabDhcpNatUi.this.router.isEnabled()) {
                TabDhcpNatUi.this.helpText.clear();
                TabDhcpNatUi.this.helpText.add(new Span(MSGS.netRouterToolTipPassDns()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());

        this.radio1.addValueChangeHandler(event -> setDirty(true));
        this.radio2.addValueChangeHandler(event -> setDirty(true));
        this.helpTitle.setText(MSGS.netHelpTitle());
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }
}
