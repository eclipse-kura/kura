/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.messages.ValidationMessages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtNetIfConfigMode;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetIfType;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.IntegerBox;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabTcpIpUi extends Composite implements NetworkTab {

    private static final String DNS_REGEX = "[\\s,;\\n\\t]+";
    private static final String NET_IPV4_STATUS_ENABLED_WAN = "netIPv4StatusEnabledWAN";
    private static final String NET_IPV4_STATUS_ENABLED_LAN = "netIPv4StatusEnabledLAN";
    private static final String NET_IPV4_STATUS_L2_ONLY = "netIPv4StatusL2Only";
    private static final String NET_IPV4_STATUS_UNMANAGED = "netIPv4StatusUnmanaged";
    private static final String NET_IPV4_STATUS_DISABLED = "netIPv4StatusDisabled";
    private static final String IPV4_MODE_MANUAL = GwtNetIfConfigMode.netIPv4ConfigModeManual.name();
    private static final String IPV4_MODE_DHCP = GwtNetIfConfigMode.netIPv4ConfigModeDHCP.name();
    private static final String IPV4_MODE_DHCP_MESSAGE = MessageUtils.get(IPV4_MODE_DHCP);
    private static final String IPV4_STATUS_WAN = GwtNetIfStatus.netIPv4StatusEnabledWAN.name();
    private static final String IPV4_STATUS_WAN_MESSAGE = MessageUtils.get(IPV4_STATUS_WAN);
    private static final String IPV4_STATUS_LAN = GwtNetIfStatus.netIPv4StatusEnabledLAN.name();
    private static final String IPV4_STATUS_LAN_MESSAGE = MessageUtils.get(IPV4_STATUS_LAN);
    private static final String IPV4_STATUS_UNMANAGED = GwtNetIfStatus.netIPv4StatusUnmanaged.name();
    private static final String IPV4_STATUS_L2ONLY = GwtNetIfStatus.netIPv4StatusL2Only.name();

    private static final String IPV4_STATUS_DISABLED = GwtNetIfStatus.netIPv4StatusDisabled.name();
    private static final String IPV4_STATUS_DISABLED_MESSAGE = MessageUtils.get(IPV4_STATUS_DISABLED);

    private static TabTcpIpUiUiBinder uiBinder = GWT.create(TabTcpIpUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(TabTcpIpUi.class.getSimpleName());

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final ValidationMessages VMSGS = GWT.create(ValidationMessages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    interface TabTcpIpUiUiBinder extends UiBinder<Widget, TabTcpIpUi> {
    }

    private final GwtSession session;
    boolean dirty;
    private GwtNetInterfaceConfig selectedNetIfConfig;
    private final NetworkTabsUi tabs;
    private boolean isNet2;

    @UiField
    FormGroup groupPriority;
    @UiField
    FormGroup groupIp;
    @UiField
    FormGroup groupSubnet;
    @UiField
    FormGroup groupGateway;
    @UiField
    FormGroup groupDns;

    @UiField
    FormLabel labelStatus;
    @UiField
    FormLabel labelPriority;
    @UiField
    FormLabel labelConfigure;
    @UiField
    FormLabel labelIp;
    @UiField
    FormLabel labelSubnet;
    @UiField
    FormLabel labelGateway;
    @UiField
    FormLabel labelDns;

    @UiField
    HelpBlock helpPriority;
    @UiField
    HelpBlock helpIp;
    @UiField
    HelpBlock helpSubnet;
    @UiField
    HelpBlock helpGateway;
    @UiField
    HelpBlock helpDns;

    @UiField
    TextBox ip;
    @UiField
    TextBox subnet;
    @UiField
    TextBox gateway;
    @UiField
    TextArea dns;
    @UiField
    ListBox status;
    @UiField
    IntegerBox priority;
    @UiField
    ListBox configure;

    @UiField
    Button renew;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    Form form;

    @UiField
    FormControlStatic dnsRead;

    @UiField
    Modal wanModal;

    @UiField
    Alert multipleWanWarn;

    @UiField
    Text multipleWanWarnText;

    @UiField
    HelpButton statusHelp;
    @UiField
    HelpButton priorityHelp;
    @UiField
    HelpButton configureHelp;
    @UiField
    HelpButton ipHelp;
    @UiField
    HelpButton subnetHelp;
    @UiField
    HelpButton gatewayHelp;
    @UiField
    HelpButton dnsHelp;

    public TabTcpIpUi(GwtSession currentSession, NetworkTabsUi netTabs) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tabs = netTabs;
        this.helpTitle.setText(MSGS.netHelpTitle());
        detectIfNet2();
        initForm();
        this.dnsRead.setVisible(false);

        initModal();

        initHelpButtons();
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.tabs.getButtons() != null) {
            this.tabs.getButtons().setButtonsDirty(flag);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        if (config == null) {
            return;
        }

        if (config.getSubnetMask() != null && "255.255.255.255".equals(config.getSubnetMask())) {
            config.setSubnetMask("");
        }

        this.selectedNetIfConfig = config;
        logger.fine(this.selectedNetIfConfig.getName());
        logger.fine(this.selectedNetIfConfig.getConfigMode());
        logger.fine(this.selectedNetIfConfig.getIpAddress());

        initStatusValues();
    }

    protected void initStatusValues() {
        this.status.clear();
        this.status.addItem(MessageUtils.get(NET_IPV4_STATUS_DISABLED));
        this.status.addItem(MessageUtils.get(NET_IPV4_STATUS_UNMANAGED));

        if (this.selectedNetIfConfig != null && this.selectedNetIfConfig.getHwTypeEnum() != GwtNetIfType.MODEM) {
            this.status.addItem(MessageUtils.get(NET_IPV4_STATUS_L2_ONLY));
            this.status.addItem(MessageUtils.get(NET_IPV4_STATUS_ENABLED_LAN));
        }
        this.status.addItem(MessageUtils.get(NET_IPV4_STATUS_ENABLED_WAN));
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        if (this.form != null) {

            if (this.status.getSelectedItemText().equals(MessageUtils.get(NET_IPV4_STATUS_UNMANAGED))) {
                updatedNetIf.setStatus(IPV4_STATUS_UNMANAGED);
            } else if (this.status.getSelectedItemText().equals(MessageUtils.get(NET_IPV4_STATUS_L2_ONLY))) {
                updatedNetIf.setStatus(IPV4_STATUS_L2ONLY);
            } else if (this.status.getSelectedItemText().equals(MessageUtils.get(NET_IPV4_STATUS_ENABLED_LAN))) {
                updatedNetIf.setStatus(IPV4_STATUS_LAN);
            } else if (this.status.getSelectedItemText().equals(MessageUtils.get(NET_IPV4_STATUS_ENABLED_WAN))) {
                updatedNetIf.setStatus(IPV4_STATUS_WAN);
            } else {
                updatedNetIf.setStatus(IPV4_STATUS_DISABLED);
            }

            if (this.priority.isEnabled() && this.isNet2) {
                updatedNetIf.setWanPriority(this.priority.getValue());
            }

            if (IPV4_MODE_DHCP_MESSAGE.equals(this.configure.getSelectedItemText())) {
                updatedNetIf.setConfigMode(IPV4_MODE_DHCP);
            } else {
                updatedNetIf.setConfigMode(IPV4_MODE_MANUAL);
            }

            if (this.ip.getValue() != null && !"".equals(this.ip.getValue().trim())) {
                updatedNetIf.setIpAddress(this.ip.getValue());
            } else {
                updatedNetIf.setIpAddress("");
            }
            if (this.subnet.getValue() != null && !"".equals(this.subnet.getValue().trim())) {
                updatedNetIf.setSubnetMask(this.subnet.getValue());
            } else {
                updatedNetIf.setSubnetMask("");
            }
            if (this.gateway.getValue() != null && !"".equals(this.gateway.getValue().trim())) {
                updatedNetIf.setGateway(this.gateway.getValue());
            } else {
                updatedNetIf.setGateway("");
            }
            if (this.dns.getValue() != null && !"".equals(this.dns.getValue().trim())) {
                updatedNetIf.setDnsServers(this.dns.getValue());
            } else {
                updatedNetIf.setDnsServers("");
            }
        }
    }

    @Override
    public boolean isValid() {
        boolean flag = true;
        // check and make sure if 'Enabled for WAN' then either DHCP is selected
        // or STATIC and a gateway is set
        if (!IPV4_STATUS_DISABLED_MESSAGE.equals(this.status.getSelectedValue())
                && this.configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeManual())) {
            if ((this.gateway.getValue() == null || "".equals(this.gateway.getValue().trim()))
                    && IPV4_STATUS_WAN_MESSAGE.equals(this.status.getSelectedValue())) {
                this.groupGateway.setValidationState(ValidationState.ERROR);
                this.helpGateway.setText(MSGS.netIPv4InvalidAddress());
                flag = false;
            }
            if (this.ip.getValue() == null || "".equals(this.ip.getValue().trim())) {
                this.groupIp.setValidationState(ValidationState.ERROR);
                this.helpIp.setText(MSGS.netIPv4InvalidAddress());
            }
        }
        if (this.groupPriority.getValidationState().equals(ValidationState.ERROR)
                || this.groupIp.getValidationState().equals(ValidationState.ERROR)
                || this.groupSubnet.getValidationState().equals(ValidationState.ERROR)
                || this.groupGateway.getValidationState().equals(ValidationState.ERROR)
                || this.groupDns.getValidationState().equals(ValidationState.ERROR)) {
            flag = false;
        }
        return flag;
    }

    public boolean isLanEnabled() {
        if (this.status == null) {
            return false;
        }
        return IPV4_STATUS_LAN_MESSAGE.equals(this.status.getSelectedValue());
    }

    public boolean isWanEnabled() {
        if (this.status == null) {
            return false;
        }
        return IPV4_STATUS_WAN_MESSAGE.equals(this.status.getSelectedValue());
    }

    public String getStatus() {
        return this.status.getSelectedValue();
    }

    public boolean isDhcp() {
        if (this.configure == null) {
            logger.log(Level.FINER, "TcpIpConfigTab.isDhcp() - this.configure is null");
            return true;
        }
        return IPV4_MODE_DHCP_MESSAGE.equals(this.configure.getSelectedValue());
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
    public void clear() {
        // Not needed
    }

    // ---------------Private Methods------------

    private void detectIfNet2() {
        this.gwtNetworkService.isNet2(new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                TabTcpIpUi.this.isNet2 = false;
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(Boolean result) {
                TabTcpIpUi.this.isNet2 = result;
                initNet2FeaturesOnly(result);
            }
        });
    }

    private void initNet2FeaturesOnly(boolean isNet2) {
        this.labelPriority.setVisible(isNet2);
        this.priority.setVisible(isNet2);
        this.priorityHelp.setVisible(isNet2);
    }

    private void initHelpButtons() {
        this.statusHelp.setHelpText(MSGS.netIPv4ToolTipStatus());
        this.priorityHelp.setHelpText(MSGS.netIPv4ToolTipPriority());
        this.configureHelp.setHelpText(MSGS.netIPv4ToolTipConfigure());
        this.ipHelp.setHelpText(MSGS.netIPv4ToolTipAddress());
        this.subnetHelp.setHelpText(MSGS.netIPv4ToolTipSubnetMask());
        this.gatewayHelp.setHelpText(MSGS.netIPv4ToolTipGateway());
        this.dnsHelp.setHelpText(MSGS.netIPv4ToolTipDns());
    }

    private void initForm() {

        // Labels
        this.labelStatus.setText(MSGS.netIPv4Status());
        this.labelPriority.setText(MSGS.netIPv4Priority());
        this.labelConfigure.setText(MSGS.netIPv4Configure());
        this.labelIp.setText(MSGS.netIPv4Address());
        this.labelSubnet.setText(MSGS.netIPv4SubnetMask());
        this.labelGateway.setText(MSGS.netIPv4Gateway());
        this.labelDns.setText(MSGS.netIPv4DNSServers());

        for (GwtNetIfConfigMode mode : GwtNetIfConfigMode.values()) {
            this.configure.addItem(MessageUtils.get(mode.name()));
        }

        initStatusField();

        initPriorityField();

        initConfigureField();

        initIpAddressField();

        initSubnetMaskField();

        initGatewayField();

        initDnsServersField();

        initDHCPLeaseField();
    }

    private void initPriorityField() {
        this.priority.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.isNet2 && TabTcpIpUi.this.priority.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipPriority()));
            }
        });
        this.priority.addMouseOutHandler(event -> resetHelp());
        this.priority.addValueChangeHandler(valChangeEvent -> {
            setDirty(true);

            String inputText = TabTcpIpUi.this.priority.getText();
            boolean isInvalidValue = false;

            if (!Objects.isNull(inputText) && !inputText.trim().isEmpty()) {
                try {
                    if (Integer.parseInt(inputText) < -1) {
                        isInvalidValue = true;
                    }
                } catch (NumberFormatException e) {
                    isInvalidValue = true;
                }
            }

            if (isInvalidValue) {
                TabTcpIpUi.this.groupPriority.setValidationState(ValidationState.ERROR);
                TabTcpIpUi.this.helpPriority.setText(MSGS.netIPv4InvalidPriority());
            } else {
                TabTcpIpUi.this.groupPriority.setValidationState(ValidationState.NONE);
                TabTcpIpUi.this.helpPriority.setText("");
            }
        });
    }

    private void initDHCPLeaseField() {
        // Renew DHCP Lease

        this.renew.setText(MSGS.netIPv4RenewDHCPLease());
        this.renew.addClickHandler(event -> {
            EntryClassUi.showWaitModal();
            TabTcpIpUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    EntryClassUi.hideWaitModal();
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    TabTcpIpUi.this.gwtNetworkService.renewDhcpLease(token,
                            TabTcpIpUi.this.selectedNetIfConfig.getName(), new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(ex);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    refresh();
                                    EntryClassUi.hideWaitModal();
                                }
                            });
                }

            });
        });

        this.renew.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.renew.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipRenew()));
            }
        });
        this.renew.addMouseOutHandler(event -> resetHelp());
    }

    private void initDnsServersField() {
        this.dns.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.dns.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipDns()));
            }
        });
        this.dns.addMouseOutHandler(event -> resetHelp());
        this.dns.addValueChangeHandler(event -> {
            setDirty(true);

            if (TabTcpIpUi.this.dns.getText().trim().length() == 0) {
                TabTcpIpUi.this.groupDns.setValidationState(ValidationState.NONE);
                TabTcpIpUi.this.helpDns.setText("");
                return;
            }

            String[] aDnsServers = TabTcpIpUi.this.dns.getText().split(DNS_REGEX);
            boolean validDnsList = true;

            int dnsContentLength = TabTcpIpUi.this.dns.getText().trim().length();
            int aDnsServersSize = aDnsServers.length;
            if (dnsContentLength != 0 && aDnsServersSize == 0) {
                validDnsList = false;
            }

            for (String dnsEntry : aDnsServers) {
                if (dnsEntry.trim().length() >= 0 && !dnsEntry.trim().matches(FieldType.IPv4_ADDRESS.getRegex())) {
                    validDnsList = false;
                    break;
                }
            }
            if (!validDnsList) {
                TabTcpIpUi.this.groupDns.setValidationState(ValidationState.ERROR);
                TabTcpIpUi.this.helpDns.setText(MSGS.netIPv4InvalidAddresses());
            } else {
                TabTcpIpUi.this.groupDns.setValidationState(ValidationState.NONE);
                TabTcpIpUi.this.helpDns.setText("");
            }
        });
    }

    private void initGatewayField() {
        this.gateway.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.gateway.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipGateway()));
            }
        });
        this.gateway.addMouseOutHandler(event -> resetHelp());
        this.gateway.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabTcpIpUi.this.gateway.getText().trim().matches(FieldType.IPv4_ADDRESS.getRegex())
                    && TabTcpIpUi.this.gateway.getText().trim().length() > 0) {
                TabTcpIpUi.this.groupGateway.setValidationState(ValidationState.ERROR);
                TabTcpIpUi.this.helpGateway.setText(MSGS.netIPv4InvalidAddress());
            } else {
                TabTcpIpUi.this.groupGateway.setValidationState(ValidationState.NONE);
                TabTcpIpUi.this.helpGateway.setText("");
            }
        });
    }

    private void initSubnetMaskField() {
        this.subnet.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.subnet.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipSubnetMask()));
            }
        });
        this.subnet.addMouseOutHandler(event -> resetHelp());
        this.subnet.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabTcpIpUi.this.subnet.getText().trim().matches(FieldType.IPv4_ADDRESS.getRegex())
                    && TabTcpIpUi.this.subnet.getText().trim().length() > 0) {
                TabTcpIpUi.this.groupSubnet.setValidationState(ValidationState.ERROR);
                TabTcpIpUi.this.helpSubnet.setText(MSGS.netIPv4InvalidAddress());
            } else {
                TabTcpIpUi.this.groupSubnet.setValidationState(ValidationState.NONE);
                TabTcpIpUi.this.helpSubnet.setText("");
            }
        });
    }

    private void initIpAddressField() {
        this.ip.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.ip.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipAddress()));
            }
        });
        this.ip.addMouseOutHandler(event -> resetHelp());
        this.ip.addValueChangeHandler(event -> {
            setDirty(true);
            if (!TabTcpIpUi.this.ip.getText().trim().matches(FieldType.IPv4_ADDRESS.getRegex())
                    || TabTcpIpUi.this.ip.getText().trim().length() <= 0) {
                TabTcpIpUi.this.groupIp.setValidationState(ValidationState.ERROR);
                TabTcpIpUi.this.helpIp.setText(MSGS.netIPv4InvalidAddress());
            } else {
                TabTcpIpUi.this.groupIp.setValidationState(ValidationState.NONE);
                TabTcpIpUi.this.helpIp.setText("");
            }
        });
    }

    private void initConfigureField() {
        this.configure.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.configure.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipConfigure()));
            }
        });
        this.configure.addMouseOutHandler(event -> resetHelp());
        this.configure.addChangeHandler(event -> {
            setDirty(true);
            TabTcpIpUi.this.tabs.adjustInterfaceTabs();
            refreshForm();
            resetValidations();
        });
        // Initial view of configure
        if (this.configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeDHCP())) {
            // Using DHCP selected
            this.ip.setEnabled(false);
            this.subnet.setEnabled(false);
            this.gateway.setEnabled(false);
            this.renew.setEnabled(true);

        } else if (this.configure.getSelectedItemText().equalsIgnoreCase(VMSGS.netIPv4ConfigModeManual())) {
            // Manually selected
            this.ip.setEnabled(true);
            this.subnet.setEnabled(true);
            this.gateway.setEnabled(true);
            this.renew.setEnabled(false);
        }
    }

    private void initStatusField() {
        initStatusValues();

        this.status.addMouseOverHandler(event -> {
            if (TabTcpIpUi.this.status.isEnabled()) {
                TabTcpIpUi.this.helpText.clear();
                if (TabTcpIpUi.this.selectedNetIfConfig != null
                        && TabTcpIpUi.this.selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
                    TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ModemToolTipStatus()));
                } else {
                    TabTcpIpUi.this.helpText.add(new Span(MSGS.netIPv4ToolTipStatus()));
                }
            }
        });
        this.status.addMouseOutHandler(event -> resetHelp());
        this.status.addChangeHandler(event -> {
            setDirty(true);
            TabTcpIpUi.this.tabs.adjustInterfaceTabs();

            refreshForm();
            resetValidations();

            // Check for other WAN interfaces if current interface is
            // changed to WAN
            if (isWanEnabled()) {
                EntryClassUi.showWaitModal();
                TabTcpIpUi.this.gwtNetworkService.findNetInterfaceConfigurations(false,
                        new AsyncCallback<List<GwtNetInterfaceConfig>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(List<GwtNetInterfaceConfig> result) {
                                EntryClassUi.hideWaitModal();
                                if (!TabTcpIpUi.this.isNet2) {
                                    showMultipleWanModal(result);
                                }
                            }

                        });
            }
        });
    }

    private void showMultipleWanModal(List<GwtNetInterfaceConfig> configs) {
        for (GwtNetInterfaceConfig config : configs) {
            if (config.getStatusEnum().equals(GwtNetIfStatus.netIPv4StatusEnabledWAN)
                    && !config.getName().equals(TabTcpIpUi.this.selectedNetIfConfig.getName())) {
                logger.log(Level.SEVERE, "Error: Status Invalid");
                TabTcpIpUi.this.wanModal.show();
                break;
            }
        }
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void update() {
        if (this.selectedNetIfConfig != null) {
            // Status
            for (int i = 0; i < this.status.getItemCount(); i++) {
                if (this.status.getItemText(i).equals(MessageUtils.get(this.selectedNetIfConfig.getStatus()))) {
                    this.status.setSelectedIndex(i);
                    break;
                }
            }
            // Configure
            for (int i = 0; i < this.configure.getItemCount(); i++) {
                if (this.configure.getValue(i).equals(MessageUtils.get(this.selectedNetIfConfig.getConfigMode()))) {
                    this.configure.setSelectedIndex(i);
                    break;
                }
            }

            if (this.isNet2) {
                this.priority.setValue(this.selectedNetIfConfig.getWanPriority());
            }

            this.tabs.adjustInterfaceTabs();
            this.ip.setText(this.selectedNetIfConfig.getIpAddress());
            this.subnet.setText(this.selectedNetIfConfig.getSubnetMask());
            this.gateway.setText(this.selectedNetIfConfig.getGateway());
            if (this.selectedNetIfConfig.getReadOnlyDnsServers() != null) {
                this.dnsRead.setText(this.selectedNetIfConfig.getReadOnlyDnsServers());
                this.dnsRead.setVisible(true);// ???
            } else {
                this.dnsRead.setText("");
                this.dnsRead.setVisible(false);
            }

            if (this.selectedNetIfConfig.getDnsServers() != null) {
                String dnsServersUi = this.selectedNetIfConfig.getDnsServers().replace(" ", "\n");
                this.dns.setValue(dnsServersUi);
                this.dns.setVisible(true);
            } else {
                this.dns.setVisible(false);
            }
            refreshForm();
        }
    }

    private void refreshForm() {
        this.status.setEnabled(true);
        this.priority.setEnabled(false);
        this.configure.setEnabled(true);
        this.ip.setEnabled(true);
        this.subnet.setEnabled(true);
        this.gateway.setEnabled(true);
        this.dns.setEnabled(true);
        this.renew.setEnabled(true);

        // disabling fields based on the interface
        if (this.selectedNetIfConfig != null && this.selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.MODEM) {
            this.configure.setEnabled(false);
            this.ip.setEnabled(false);
            this.subnet.setEnabled(false);
            this.gateway.setEnabled(false);
            if (VMSGS.netIPv4StatusDisabled().equals(this.status.getSelectedValue())
                    || VMSGS.netIPv4StatusUnmanaged().equals(this.status.getSelectedValue())) {
                this.dns.setEnabled(false);
            }
            this.configure.setSelectedIndex(this.configure.getItemText(0).equals(IPV4_MODE_DHCP_MESSAGE) ? 0 : 1);
        } else if (this.selectedNetIfConfig != null
                && this.selectedNetIfConfig.getHwTypeEnum() == GwtNetIfType.LOOPBACK) {
            // loopback interface should not be editable
            this.status.setEnabled(false);
            this.configure.setEnabled(false);
            this.ip.setEnabled(false);
            this.subnet.setEnabled(false);
            this.gateway.setEnabled(false);
            this.dns.setEnabled(false);
            this.renew.setEnabled(false);
        } else {
            if (VMSGS.netIPv4StatusDisabled().equals(this.status.getSelectedValue())
                    || VMSGS.netIPv4StatusUnmanaged().equals(this.status.getSelectedValue())
                    || VMSGS.netIPv4StatusL2Only().equals(this.status.getSelectedValue())) {
                String configureVal = this.configure.getItemText(0);
                this.configure.setSelectedIndex(configureVal.equals(IPV4_MODE_DHCP_MESSAGE) ? 0 : 1);
                this.ip.setText("");
                this.configure.setEnabled(false);
                this.ip.setEnabled(false);
                this.subnet.setEnabled(false);
                this.gateway.setEnabled(false);
                this.dns.setEnabled(false);
                this.subnet.setText("");
                this.gateway.setText("");
                this.dns.setText("");
                this.renew.setEnabled(false);
            } else {
                String configureValue = this.configure.getSelectedValue();
                if (configureValue.equals(IPV4_MODE_DHCP_MESSAGE)) {
                    this.ip.setEnabled(false);
                    this.subnet.setEnabled(false);
                    this.gateway.setEnabled(false);
                    if (!this.status.getSelectedValue().equals(IPV4_STATUS_WAN_MESSAGE)) {
                        this.dns.setEnabled(false);
                    }
                } else {
                    if (!this.status.getSelectedValue().equals(IPV4_STATUS_WAN_MESSAGE)) {
                        this.gateway.setText("");
                        this.gateway.setEnabled(false);
                        this.dns.setEnabled(false);
                    }
                    this.renew.setEnabled(false);
                }
            }
        }

        if (this.status.getSelectedValue().equals(VMSGS.netIPv4StatusEnabledWAN())) {
            this.priority.setEnabled(true);
        }

        // Show read-only dns field when DHCP is selected and there are no
        // custom DNS entries
        String configureValue = this.configure.getSelectedItemText();
        if (configureValue.equals(IPV4_MODE_DHCP_MESSAGE)
                && (this.dns.getValue() == null || this.dns.getValue().isEmpty())) {
            this.dnsRead.setVisible(true);
        } else {
            this.dnsRead.setVisible(false);
        }
    }

    private void reset() {
        this.status.setSelectedIndex(0);
        this.configure.setSelectedIndex(0);
        this.ip.setText("");
        this.subnet.setText("");
        this.gateway.setText("");
        this.dns.setText("");
        update();
    }

    private void resetValidations() {
        this.groupPriority.setValidationState(ValidationState.NONE);
        this.helpPriority.setText("");
        this.groupIp.setValidationState(ValidationState.NONE);
        this.helpIp.setText("");
        this.groupSubnet.setValidationState(ValidationState.NONE);
        this.helpSubnet.setText("");
        this.groupGateway.setValidationState(ValidationState.NONE);
        this.helpGateway.setText("");
        this.groupDns.setValidationState(ValidationState.NONE);
        this.helpDns.setText("");
    }

    private void initModal() {
        this.wanModal.setTitle(MSGS.warning());
        this.multipleWanWarnText.setText(MSGS.netStatusWarning());
        this.wanModal.addHideHandler(evt -> this.setDirty(true));
    }
}