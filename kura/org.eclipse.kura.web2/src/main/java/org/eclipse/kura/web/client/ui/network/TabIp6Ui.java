/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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

import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.IntegerBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabIp6Ui extends Composite implements NetworkTab {

    private static final String STATUS_UNMANAGED = "netIPv6StatusUnmanaged";
    private static final String STATUS_DISABLED = "netIPv6StatusDisabled";
    private static final String STATUS_LAN = "netIPv6StatusEnabledLAN";
    private static final String STATUS_WAN = "netIPv6StatusEnabledWAN";
    private static final String CONFIGURE_AUTO = "netIPv6MethodAuto";
    private static final String CONFIGURE_DHCP = "netIPv6MethodDhcp";
    private static final String CONFIGURE_MANUAL = "netIPv6MethodManual";
    private static final String AUTOCONF_EUI64 = "netIPv6AddressGenModeEUI64";
    private static final String AUTOCONF_STABLEPRIVACY = "netIPv6AddressGenModeStablePrivacy";
    private static final String PRIVACY_DISABLED = "netIPv6PrivacyDisabled";
    private static final String PRIVACY_PREFER_PUB = "netIPv6PrivacyEnabledPubAdd";
    private static final String PRIVACY_PREFER_TEMP = "netIPv6PrivacyEnabledTempAdd";

    private static final String IPV6_ADDRESS_REGEX = "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
    private static final String DNS_REGEX = "[\\s,;\\n\\t]+";

    interface TabIp6UiUiBinder extends UiBinder<Widget, TabIp6Ui> {
    }

    @UiField
    FormGroup groupStatus;
    @UiField
    FormGroup groupPriority;
    @UiField
    FormGroup groupConfigure;
    @UiField
    FormGroup groupAutoconfigurationMode;
    @UiField
    FormGroup groupIp;
    @UiField
    FormGroup groupSubnet;
    @UiField
    FormGroup groupGateway;
    @UiField
    FormGroup groupDns;
    @UiField
    FormGroup groupPrivacy;

    @UiField
    FormLabel labelStatus;
    @UiField
    FormLabel labelPriority;
    @UiField
    FormLabel labelConfigure;
    @UiField
    FormLabel labelAutoconfiguration;
    @UiField
    FormLabel labelIp;
    @UiField
    FormLabel labelSubnet;
    @UiField
    FormLabel labelGateway;
    @UiField
    FormLabel labelDns;
    @UiField
    FormLabel labelPrivacy;

    @UiField
    HelpBlock wrongInputPriority;
    @UiField
    HelpBlock wrongInputIp;
    @UiField
    HelpBlock wrongInputSubnet;
    @UiField
    HelpBlock wrongInputGateway;
    @UiField
    HelpBlock wrongInputDns;

    @UiField
    HelpButton helpButtonStatus;
    @UiField
    HelpButton helpButtonPriority;
    @UiField
    HelpButton helpButtonConfigure;
    @UiField
    HelpButton helpButtonAutoconfiguration;
    @UiField
    HelpButton helpButtonIp;
    @UiField
    HelpButton helpButtonSubnet;
    @UiField
    HelpButton helpButtonGateway;
    @UiField
    HelpButton helpButtonDns;
    @UiField
    HelpButton helpButtonPrivacy;

    @UiField
    ListBox status;
    @UiField
    IntegerBox priority;
    @UiField
    ListBox configure;
    @UiField
    ListBox autoconfiguration;
    @UiField
    TextBox ip;
    @UiField
    IntegerBox subnet;
    @UiField
    TextBox gateway;
    @UiField
    TextArea dns;
    @UiField
    ListBox privacy;

    @UiField
    PanelHeader helpTitle;
    @UiField
    ScrollPanel helpText;

    @UiField
    Form form;

    @UiField
    FormControlStatic dnsRead;

    private static TabIp6UiUiBinder uiBinder = GWT.create(TabIp6UiUiBinder.class);
    private static final Messages MSGS = GWT.create(Messages.class);

    private boolean dirty = false;
    private final NetworkTabsUi tabs;
    private Optional<GwtNetInterfaceConfig> selectedNetIfConfig = Optional.empty();

    public TabIp6Ui(GwtSession currentSession, NetworkTabsUi netTabs) {
        initWidget(uiBinder.createAndBindUi(this));

        this.helpTitle.setText(MSGS.netHelpTitle());
        this.tabs = netTabs;
        this.dnsRead.setVisible(false);

        initLabels();
        initHelpButtons();
        initListBoxes();
        initTextBoxes();
    }

    private void initLabels() {
        this.labelStatus.setText(MSGS.netIPv6Status());
        this.labelPriority.setText(MSGS.netIPv6Priority());
        this.labelConfigure.setText(MSGS.netIPv6Configure());
        this.labelAutoconfiguration.setText(MSGS.netIPv6AutoconfigurationMode());
        this.labelIp.setText(MSGS.netIPv6Address());
        this.labelSubnet.setText(MSGS.netIPv6SubnetMask());
        this.labelGateway.setText(MSGS.netIPv6Gateway());
        this.labelDns.setText(MSGS.netIPv6DNSServers());
        this.labelPrivacy.setText(MSGS.netIPv6Privacy());
    }

    private void initHelpButtons() {
        this.helpButtonStatus.setHelpText(MSGS.netIPv6ToolTipStatus());
        this.helpButtonPriority.setHelpText(MSGS.netIPv6ToolTipPriority());
        this.helpButtonConfigure.setHelpText(MSGS.netIPv6ToolTipConfigure());
        this.helpButtonAutoconfiguration.setHelpText(MSGS.netIPv6ToolTipAutoconfiguration());
        this.helpButtonIp.setHelpText(MSGS.netIPv6ToolTipAddress());
        this.helpButtonSubnet.setHelpText(MSGS.netIPv6ToolTipSubnetMask());
        this.helpButtonGateway.setHelpText(MSGS.netIPv6ToolTipGateway());
        this.helpButtonDns.setHelpText(MSGS.netIPv6ToolTipDns());
        this.helpButtonPrivacy.setHelpText(MSGS.netIPv6ToolTipPrivacy());
    }

    private void initListBoxes() {
        initStatusField();
        initConfigureField();
        initAutoconfigurationField();
        initPrivacyField();
    }

    private void initStatusField() {
        this.status.addItem(MessageUtils.get(STATUS_DISABLED), STATUS_DISABLED);
        this.status.addItem(MessageUtils.get(STATUS_LAN), STATUS_LAN);
        this.status.addItem(MessageUtils.get(STATUS_WAN), STATUS_WAN);

        this.status.addMouseOverHandler(event -> {
            if (this.status.isEnabled()) {
                if (this.selectedNetIfConfig.isPresent()
                        && this.selectedNetIfConfig.get().getHwTypeEnum() == GwtNetIfType.MODEM) {
                    setHelpText(MSGS.netIPv6ModemToolTipStatus());
                } else {
                    setHelpText(MSGS.netIPv6ToolTipStatus());
                }
            }
        });

        this.status.addMouseOutHandler(event -> resetHelpText());

        this.status.addChangeHandler(event -> {
            setDirty(true);
            this.tabs.updateTabs();

            refreshForm();
            resetValidations();
        });
    }

    private void initConfigureField() {
        this.configure.addItem(MessageUtils.get(CONFIGURE_AUTO), CONFIGURE_AUTO);
        this.configure.addItem(MessageUtils.get(CONFIGURE_DHCP), CONFIGURE_DHCP);
        this.configure.addItem(MessageUtils.get(CONFIGURE_MANUAL), CONFIGURE_MANUAL);

        this.configure.addMouseOverHandler(event -> {
            if (this.configure.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipConfigure());
            }
        });

        this.configure.addMouseOutHandler(event -> resetHelpText());

        this.configure.addChangeHandler(event -> {
            setDirty(true);
            this.tabs.updateTabs();

            refreshForm();
            resetValidations();
        });
    }

    private void initAutoconfigurationField() {
        this.autoconfiguration.addItem(MessageUtils.get(AUTOCONF_EUI64), AUTOCONF_EUI64);
        this.autoconfiguration.addItem(MessageUtils.get(AUTOCONF_STABLEPRIVACY), AUTOCONF_STABLEPRIVACY);

        this.autoconfiguration.addMouseOverHandler(event -> {
            if (this.autoconfiguration.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipAutoconfiguration());
            }
        });

        this.autoconfiguration.addMouseOutHandler(event -> resetHelpText());

        this.autoconfiguration.addChangeHandler(event -> {
            setDirty(true);
            this.tabs.updateTabs();

            refreshForm();
            resetValidations();
        });
    }

    private void initPrivacyField() {
        this.privacy.addItem(MessageUtils.get(PRIVACY_DISABLED), PRIVACY_DISABLED);
        this.privacy.addItem(MessageUtils.get(PRIVACY_PREFER_PUB), PRIVACY_PREFER_PUB);
        this.privacy.addItem(MessageUtils.get(PRIVACY_PREFER_TEMP), PRIVACY_PREFER_TEMP);

        this.privacy.addMouseOverHandler(event -> {
            if (this.privacy.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipPrivacy());
            }
        });

        this.privacy.addMouseOutHandler(event -> resetHelpText());

        this.privacy.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
            resetValidations();
            this.tabs.updateTabs();
        });
    }

    private void initTextBoxes() {
        initPriorityField();
        initIpField();
        initSubnetField();
        initGatewayField();
        initDnsField();
    }

    private void initPriorityField() {
        this.priority.addMouseOverHandler(event -> {
            if (this.priority.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipPriority());
            }
        });
        this.priority.addMouseOutHandler(event -> resetHelpText());

        this.priority.addValueChangeHandler(valChangeEvent -> {
            setDirty(true);

            String inputText = this.priority.getText();
            boolean isValidValue = false;

            if (inputText != null) {
                if (inputText.trim().isEmpty()) {
                    isValidValue = true;
                } else {
                    isValidValue = isValidIntegerInRange(inputText, -1, Integer.MAX_VALUE);
                }
            }

            if (isValidValue) {
                this.groupPriority.setValidationState(ValidationState.NONE);
                this.wrongInputPriority.setText("");
            } else {
                this.groupPriority.setValidationState(ValidationState.ERROR);
                this.wrongInputPriority.setText(MSGS.netIPv6InvalidPriority());
            }
        });
    }

    private boolean isValidIntegerInRange(String integerText, int min, int max) {
        try {
            int value = Integer.parseInt(integerText.trim());
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void initIpField() {
        this.ip.addMouseOverHandler(event -> {
            if (this.ip.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipAddress());
            }
        });
        this.ip.addMouseOutHandler(event -> resetHelpText());

        this.ip.addValueChangeHandler(event -> {
            setDirty(true);

            String address = this.ip.getText();
            boolean isValid = address != null && address.trim().length() > 0
                    && address.trim().matches(IPV6_ADDRESS_REGEX);

            if (!isValid) {
                this.groupIp.setValidationState(ValidationState.ERROR);
                this.wrongInputIp.setText(MSGS.netIPv6InvalidAddress());
            } else {
                this.groupIp.setValidationState(ValidationState.NONE);
                this.wrongInputIp.setText("");
            }
        });
    }

    private void initSubnetField() {
        this.subnet.addMouseOverHandler(event -> {
            if (this.subnet.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipSubnetMask());
            }
        });
        this.subnet.addMouseOutHandler(event -> resetHelpText());

        this.subnet.addValueChangeHandler(event -> {
            setDirty(true);
            if (this.subnet.getValue() == null || this.subnet.getValue() < 1 || this.subnet.getValue() > 128) {
                this.groupSubnet.setValidationState(ValidationState.ERROR);
                this.wrongInputSubnet.setText(MSGS.netIpv6InvalidSubnet());
            } else {
                this.groupSubnet.setValidationState(ValidationState.NONE);
                this.wrongInputSubnet.setText("");
            }
        });
    }

    private void initGatewayField() {
        this.gateway.addMouseOverHandler(event -> {
            if (this.gateway.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipGateway());
            }
        });
        this.gateway.addMouseOutHandler(event -> resetHelpText());

        this.gateway.addValueChangeHandler(event -> {
            setDirty(true);

            String address = this.gateway.getText();
            boolean isValid = address != null && address.trim().length() > 0
                    && address.trim().matches(IPV6_ADDRESS_REGEX);

            if (!isValid) {
                this.groupGateway.setValidationState(ValidationState.ERROR);
                this.wrongInputGateway.setText(MSGS.netIPv6InvalidAddress());
            } else {
                this.groupGateway.setValidationState(ValidationState.NONE);
                this.wrongInputGateway.setText("");
            }
        });
    }

    private void initDnsField() {
        this.dns.addMouseOverHandler(event -> {
            if (this.dns.isEnabled()) {
                setHelpText(MSGS.netIPv6ToolTipDns());
            }
        });
        this.dns.addMouseOutHandler(event -> resetHelpText());

        this.dns.addValueChangeHandler(event -> {
            setDirty(true);

            if (this.dns.getText().trim().length() == 0) {
                this.groupDns.setValidationState(ValidationState.NONE);
                this.wrongInputDns.setText("");
                return;
            }

            String[] addresses = this.dns.getText().trim().split(DNS_REGEX);
            boolean isValid = addresses.length > 0;

            for (String address : addresses) {
                isValid = isValid && address.matches(IPV6_ADDRESS_REGEX);
            }

            if (!isValid) {
                this.groupDns.setValidationState(ValidationState.ERROR);
                this.wrongInputDns.setText(MSGS.netIPv6InvalidAddress());
            } else {
                this.groupDns.setValidationState(ValidationState.NONE);
                this.wrongInputDns.setText("");
            }
        });
    }

    private void setHelpText(String message) {
        this.helpText.clear();
        this.helpText.add(new Span(message));
    }

    private void resetHelpText() {
        this.helpText.clear();
        setHelpText(MSGS.netHelpDefaultHint());
    }

    private void resetValidations() {
        this.groupPriority.setValidationState(ValidationState.NONE);
        this.wrongInputPriority.setText("");
        this.groupIp.setValidationState(ValidationState.NONE);
        this.wrongInputIp.setText("");
        this.groupSubnet.setValidationState(ValidationState.NONE);
        this.wrongInputSubnet.setText("");
        this.groupGateway.setValidationState(ValidationState.NONE);
        this.wrongInputGateway.setText("");
        this.groupDns.setValidationState(ValidationState.NONE);
        this.wrongInputDns.setText("");
    }

    private void refreshForm() {
        this.status.setEnabled(true);
        this.priority.setEnabled(true);
        this.configure.setEnabled(true);
        this.autoconfiguration.setEnabled(true);
        this.ip.setEnabled(true);
        this.subnet.setEnabled(true);
        this.gateway.setEnabled(true);
        this.dns.setEnabled(true);
        this.privacy.setEnabled(true);

        if (this.selectedNetIfConfig.isPresent()) {
            refreshFieldsBasedOnInterface(this.selectedNetIfConfig.get());
            refreshFieldsBasedOnSelectedValues();
        }
    }

    private void refreshFieldsBasedOnInterface(GwtNetInterfaceConfig config) {
        switch (config.getHwTypeEnum()) {
        case ETHERNET:
            break;
        case LOOPBACK:
            this.status.setEnabled(false);
            this.priority.setEnabled(false);
            this.configure.setEnabled(false);
            this.autoconfiguration.setEnabled(false);
            this.ip.setEnabled(false);
            this.subnet.setEnabled(false);
            this.gateway.setEnabled(false);
            this.dns.setEnabled(false);
            this.privacy.setEnabled(false);
            break;
        case MODEM:
            this.configure.setEnabled(false);
            this.configure.setSelectedIndex(0);
            this.ip.setEnabled(false);
            this.subnet.setEnabled(false);
            this.gateway.setEnabled(false);
            break;
        case WIFI:
            break;
        default:
            break;

        }
    }

    private void refreshFieldsBasedOnSelectedValues() {
        if (this.status.getSelectedValue().equals(STATUS_DISABLED)) {
            this.priority.setEnabled(false);
            this.priority.setText("");
            this.configure.setEnabled(false);
            this.configure.setSelectedIndex(0);
            this.autoconfiguration.setEnabled(false);
            this.autoconfiguration.setSelectedIndex(0);
            this.ip.setEnabled(false);
            this.ip.setText("");
            this.subnet.setEnabled(false);
            this.subnet.setText("");
            this.gateway.setEnabled(false);
            this.gateway.setText("");
            this.dns.setEnabled(false);
            this.dns.setText("");
            this.privacy.setEnabled(false);
            this.privacy.setSelectedIndex(0);
        }

        if (this.status.getSelectedValue().equals(STATUS_LAN)) {
            this.priority.setEnabled(false);
            this.priority.setText("");
            this.gateway.setEnabled(false);
            this.gateway.setText("");
            this.dns.setEnabled(false);
            this.dns.setText("");
        }

        if (this.configure.getSelectedValue().equals(CONFIGURE_AUTO)
                || this.configure.getSelectedValue().equals(CONFIGURE_DHCP)) {
            this.ip.setEnabled(false);
            this.subnet.setEnabled(false);
            this.gateway.setEnabled(false);
        }

        if (this.configure.getSelectedValue().equals(CONFIGURE_MANUAL)
                || this.configure.getSelectedValue().equals(CONFIGURE_DHCP)) {
            this.autoconfiguration.setEnabled(false);
            this.privacy.setEnabled(false);
        }

        // Show read-only dns field when there are no custom DNS entries
        this.dnsRead.setVisible(this.dns.getValue() == null || this.dns.getValue().isEmpty());
    }

    @Override
    public void setDirty(boolean isDirty) {
        this.dirty = isDirty;
        if (this.tabs.getButtons() != null) {
            this.tabs.getButtons().setButtonsDirty(isDirty);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        this.selectedNetIfConfig = Optional.of(config);
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        if (this.form != null) {
            if (this.tabs.isUnmanagedSelected()) {
                updatedNetIf.setIpv6Status(STATUS_UNMANAGED);
            } else {
                updateConfigWithSelectedValues(updatedNetIf);
            }
        }
    }

    private void updateConfigWithSelectedValues(GwtNetInterfaceConfig updatedNetIf) {
        updatedNetIf.setIpv6Status(this.status.getSelectedValue());

        if (this.priority.getValue() != null) {
            updatedNetIf.setIpv6WanPriority(this.priority.getValue());
        }

        updatedNetIf.setIpv6ConfigMode(this.configure.getSelectedValue());
        updatedNetIf.setIpv6AutoconfigurationMode(this.autoconfiguration.getSelectedValue());

        if (!nullOrEmpty(this.ip.getValue())) {
            updatedNetIf.setIpv6Address(this.ip.getValue().trim());
        } else {
            updatedNetIf.setIpv6Address("");
        }
        if (this.subnet.getValue() != null) {
            updatedNetIf.setIpv6SubnetMask(this.subnet.getValue());
        } else {
            updatedNetIf.setIpv6SubnetMask(0);
        }
        if (!nullOrEmpty(this.gateway.getValue())) {
            updatedNetIf.setIpv6Gateway(this.gateway.getValue().trim());
        } else {
            updatedNetIf.setIpv6Gateway("");
        }
        if (!nullOrEmpty(this.dns.getValue())) {
            updatedNetIf.setIpv6DnsServers(this.dns.getValue().trim());
        } else {
            updatedNetIf.setIpv6DnsServers("");
        }

        updatedNetIf.setIpv6Privacy(this.privacy.getSelectedValue());
    }

    private boolean nullOrEmpty(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }

    @Override
    public boolean isValid() {
        boolean isWan = this.status.getSelectedValue().equals(STATUS_WAN);
        boolean isManual = this.configure.getSelectedValue().equals(CONFIGURE_MANUAL);

        if (isWan && isManual) {
            if (nullOrEmpty(this.ip.getValue()) || this.subnet.getValue() == null
                    || nullOrEmpty(this.gateway.getValue())) {
                this.groupIp.setValidationState(ValidationState.ERROR);
                this.groupSubnet.setValidationState(ValidationState.ERROR);
                this.groupGateway.setValidationState(ValidationState.ERROR);
                this.wrongInputIp.setText(MSGS.netIPv6InvalidAddress());
                this.wrongInputGateway.setText(MSGS.netIPv6InvalidAddress());
                return false;
            }
        }

        if (this.groupPriority.getValidationState().equals(ValidationState.ERROR)
                || this.groupIp.getValidationState().equals(ValidationState.ERROR)
                || this.groupSubnet.getValidationState().equals(ValidationState.ERROR)
                || this.groupGateway.getValidationState().equals(ValidationState.ERROR)
                || this.groupDns.getValidationState().equals(ValidationState.ERROR)) {
            return false;
        }

        return true;
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            resetValidations();

            if (this.selectedNetIfConfig.isPresent()) {
                fillFormWithCachedConfig();
            } else {
                reset();
            }
        }
    }

    private void reset() {
        this.status.setSelectedIndex(0);
        this.priority.setText("");
        this.configure.setSelectedIndex(0);
        this.autoconfiguration.setSelectedIndex(0);
        this.ip.setText("");
        this.subnet.setText("");
        this.gateway.setText("");
        this.dns.setText("");
        this.privacy.setSelectedIndex(0);
    }

    private void fillFormWithCachedConfig() {
        for (int i = 0; i < this.status.getItemCount(); i++) {
            if (this.status.getValue(i).equals(this.selectedNetIfConfig.get().getIpv6Status())) {
                this.status.setSelectedIndex(i);
                break;
            }
        }

        Integer wanPriority = this.selectedNetIfConfig.get().getIpv6WanPriority();
        if (wanPriority != null) {
            this.priority.setText(wanPriority.toString());
        }

        for (int i = 0; i < this.configure.getItemCount(); i++) {
            if (this.configure.getValue(i).equals(this.selectedNetIfConfig.get().getIpv6ConfigMode())) {
                this.configure.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.autoconfiguration.getItemCount(); i++) {
            if (this.autoconfiguration.getValue(i)
                    .equals(this.selectedNetIfConfig.get().getIpv6AutoconfigurationMode())) {
                this.autoconfiguration.setSelectedIndex(i);
                break;
            }
        }

        this.ip.setText(this.selectedNetIfConfig.get().getIpv6Address());
        this.subnet.setValue(this.selectedNetIfConfig.get().getIpv6SubnetMask());
        this.gateway.setText(this.selectedNetIfConfig.get().getIpv6Gateway());

        if (this.selectedNetIfConfig.get().getIpv6ReadOnlyDnsServers() != null) {
            this.dnsRead.setText(this.selectedNetIfConfig.get().getIpv6ReadOnlyDnsServers());
            this.dnsRead.setVisible(true);
        } else {
            this.dnsRead.setText("");
            this.dnsRead.setVisible(false);
        }

        if (this.selectedNetIfConfig.get().getIpv6DnsServers() != null) {
            String dnsServersUi = this.selectedNetIfConfig.get().getIpv6DnsServers().replace(" ", "\n");
            this.dns.setValue(dnsServersUi);
        }

        for (int i = 0; i < this.privacy.getItemCount(); i++) {
            if (this.privacy.getValue(i).equals(this.selectedNetIfConfig.get().getIpv6Privacy())) {
                this.privacy.setSelectedIndex(i);
                break;
            }
        }

        this.tabs.updateTabs();
        refreshForm();
    }

    @Override
    public void clear() {
        // Not needed
    }

}