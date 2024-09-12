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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class TabModemUi extends Composite implements NetworkTab {

    private static final String DE910 = "DE910";
    private static final String LE910 = "LE910";
    private static final String HE910 = "HE910";
    private static final String MODEM_AUTH_NONE_MESSAGE = MessageUtils.get(GwtModemAuthType.netModemAuthNONE.name());
    private static TabModemUiUiBinder uiBinder = GWT.create(TabModemUiUiBinder.class);

    interface TabModemUiUiBinder extends UiBinder<Widget, TabModemUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String REGEX_NUM = "(?:\\d*)?\\d+";

    private final GwtSession session;
    private final TabIp4Ui tcpTab;
    private final NetworkTabsUi tabs;
    private boolean dirty;
    private GwtModemInterfaceConfig selectedNetIfConfig;

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);


    @UiField
    FormGroup groupReset;
    @UiField
    FormGroup groupHoldoff;
    @UiField
    FormGroup groupMaxfail;
    @UiField
    FormGroup groupIdle;
    @UiField
    FormGroup groupInterval;
    @UiField
    FormGroup groupFailure;
    @UiField
    FormGroup groupNumber;
    @UiField
    FormGroup groupApn;

    @UiField
    FormLabel labelModel;
    @UiField
    FormLabel labelNetwork;
    @UiField
    FormLabel labelService;
    @UiField
    FormLabel labelModem;
    @UiField
    FormLabel labelNumber;
    @UiField
    FormLabel labelApn;
    @UiField
    FormLabel labelAuth;
    @UiField
    FormLabel labelUsername;
    @UiField
    FormLabel labelPassword;
    @UiField
    FormLabel labelReset;
    @UiField
    FormLabel labelPersist;
    @UiField
    FormLabel labelHoldoff;
    @UiField
    FormLabel labelMaxfail;
    @UiField
    FormLabel labelIdle;
    @UiField
    FormLabel labelActive;
    @UiField
    FormLabel labelInterval;
    @UiField
    FormLabel labelFailure;

    @UiField
    HelpBlock helpReset;
    @UiField
    HelpBlock helpHoldoff;
    @UiField
    HelpBlock helpMaxfail;
    @UiField
    HelpBlock helpIdle;
    @UiField
    HelpBlock helpInterval;
    @UiField
    HelpBlock helpFailure;
    @UiField
    HelpBlock helpNumber;

    @UiField
    ListBox network;
    @UiField
    ListBox auth;

    @UiField
    TextBox modem;
    @UiField
    TextBox number;
    @UiField
    TextBox apn;
    @UiField
    TextBox username;
    @UiField
    TextBox reset;
    @UiField
    TextBox holdoff;
    @UiField
    TextBox maxfail;
    @UiField
    TextBox idle;
    @UiField
    TextBox active;
    @UiField
    TextBox interval;
    @UiField
    TextBox failure;

    @UiField
    FormControlStatic model;
    @UiField
    FormControlStatic service;

    @UiField
    Input password;

    @UiField
    InlineRadio radio1;
    @UiField
    InlineRadio radio2;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    FieldSet field;

    @UiField
    HelpButton networkHelp;
    @UiField
    HelpButton modemHelp;
    @UiField
    HelpButton numberHelp;
    @UiField
    HelpButton apnHelp;
    @UiField
    HelpButton authHelp;
    @UiField
    HelpButton usernameHelp;
    @UiField
    HelpButton passwordHelp;
    @UiField
    HelpButton resetHelp;
    @UiField
    HelpButton persistHelp;
    @UiField
    HelpButton holdoffHelp;
    @UiField
    HelpButton maxfailHelp;
    @UiField
    HelpButton idleHelp;
    @UiField
    HelpButton activeHelp;
    @UiField
    HelpButton intervalHelp;
    @UiField
    HelpButton failureHelp;

    public TabModemUi(GwtSession currentSession, TabIp4Ui tcp, NetworkTabsUi tabs) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tcpTab = tcp;
        this.tabs = tabs;

        initForm();

        initHelpButtons();

        this.tcpTab.status.addChangeHandler(event -> update());
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
    public boolean isValid() {
        if (this.maxfail.getText() == null || "".equals(this.maxfail.getText().trim())) {
            this.groupMaxfail.setValidationState(ValidationState.ERROR);
        }
        if (this.holdoff.getText() == null || "".equals(this.holdoff.getText().trim())) {
            this.groupHoldoff.setValidationState(ValidationState.ERROR);
        }
        if (this.idle.getText() == null || "".equals(this.idle.getText().trim())) {
            this.groupIdle.setValidationState(ValidationState.ERROR);
        }
        if (this.interval.getText() == null || "".equals(this.interval.getText().trim())) {
            this.groupInterval.setValidationState(ValidationState.ERROR);
        }
        if (this.failure.getText() == null || "".equals(this.failure.getText().trim())) {
            this.groupFailure.setValidationState(ValidationState.ERROR);
        }

        if (this.groupNumber.getValidationState().equals(ValidationState.ERROR)
                || this.groupApn.getValidationState().equals(ValidationState.ERROR)
                || this.groupMaxfail.getValidationState().equals(ValidationState.ERROR)
                || this.groupHoldoff.getValidationState().equals(ValidationState.ERROR)
                || this.groupIdle.getValidationState().equals(ValidationState.ERROR)
                || this.groupInterval.getValidationState().equals(ValidationState.ERROR)
                || this.groupFailure.getValidationState().equals(ValidationState.ERROR)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        if (config instanceof GwtModemInterfaceConfig) {
            this.selectedNetIfConfig = (GwtModemInterfaceConfig) config;
        }
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

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtModemInterfaceConfig updatedModemNetIf = (GwtModemInterfaceConfig) updatedNetIf;

        if (this.model.getText() != null && this.service.getText() != null) {
            // note - status is set in tcp/ip tab
            updatedModemNetIf.setPppNum(Integer.parseInt(this.number.getText()));
            updatedModemNetIf.setModemId(this.modem.getText().trim() != null ? this.modem.getText().trim() : "");
            updatedModemNetIf.setApn(this.apn.getText().trim() != null ? this.apn.getText().trim() : "");

            String authValue = this.auth.getSelectedValue();
            for (GwtModemAuthType authT : GwtModemAuthType.values()) {
                if (MessageUtils.get(authT.name()).equals(authValue)) {
                    updatedModemNetIf.setAuthType(authT);
                }
            }

            if (updatedModemNetIf.getAuthType() != GwtModemAuthType.netModemAuthNONE) {
                updatedModemNetIf
                        .setUsername(this.username.getText().trim() != null ? this.username.getText().trim() : "");
                updatedModemNetIf
                        .setPassword(this.password.getText().trim() != null ? this.password.getText().trim() : "");
            }

            updatedModemNetIf.setResetTimeout(Integer.parseInt(this.reset.getValue().trim()));
            updatedModemNetIf.setPersist(this.radio1.getValue());
            updatedModemNetIf.setMaxFail(Integer.parseInt(this.maxfail.getText().trim()));
            updatedModemNetIf.setHoldoff(Integer.parseInt(this.holdoff.getText().trim()));
            updatedModemNetIf.setIdle(Integer.parseInt(this.idle.getText().trim()));
            updatedModemNetIf.setActiveFilter(this.active.getText() != "" ? this.active.getText().trim() : "");
            updatedModemNetIf.setLcpEchoInterval(Integer.parseInt(this.interval.getText().trim()));
            updatedModemNetIf.setLcpEchoFailure(Integer.parseInt(this.failure.getText().trim()));
            // ---
        } else {
            // initForm hasn't been called yet

            updatedModemNetIf.setPppNum(this.selectedNetIfConfig.getPppNum());
            updatedModemNetIf.setModemId(this.selectedNetIfConfig.getModemId());
            updatedModemNetIf.setApn(this.selectedNetIfConfig.getApn());
            updatedModemNetIf.setAuthType(this.selectedNetIfConfig.getAuthType());
            updatedModemNetIf.setUsername(this.selectedNetIfConfig.getUsername());
            updatedModemNetIf.setPassword(this.selectedNetIfConfig.getPassword());
            updatedModemNetIf.setResetTimeout(this.selectedNetIfConfig.getResetTimeout());
            updatedModemNetIf.setPersist(this.selectedNetIfConfig.isPersist());
            updatedModemNetIf.setMaxFail(this.selectedNetIfConfig.getMaxFail());
            updatedModemNetIf.setHoldoff(this.selectedNetIfConfig.getHoldoff());
            updatedModemNetIf.setIdle(this.selectedNetIfConfig.getIdle());
            updatedModemNetIf.setActiveFilter(this.selectedNetIfConfig.getActiveFilter());
            updatedModemNetIf.setLcpEchoInterval(this.selectedNetIfConfig.getLcpEchoInterval());
            updatedModemNetIf.setLcpEchoFailure(this.selectedNetIfConfig.getLcpEchoFailure());
        }

    }

    // ----Private Methods----
    private void initHelpButtons() {
        this.networkHelp.setHelpText(MSGS.netModemToolTipNetworkTopology());
        this.modemHelp.setHelpText(MSGS.netModemToolTipModemIndentifier());
        this.numberHelp.setHelpText(MSGS.netModemToolTipModemInterfaceNumber());
        this.apnHelp.setHelpText(MSGS.netModemToolTipApn());
        this.authHelp.setHelpText(MSGS.netModemToolTipAuthentication());
        this.usernameHelp.setHelpText(MSGS.netModemToolTipUsername());
        this.passwordHelp.setHelpText(MSGS.netModemToolTipPassword());
        this.resetHelp.setHelpText(MSGS.netModemToolTipResetTimeout());
        this.persistHelp.setHelpText(MSGS.netModemToolTipPersist());
        this.maxfailHelp.setHelpText(MSGS.netModemToolTipMaxFail());
        this.holdoffHelp.setHelpText(MSGS.netModemToolTipHoldoff());
        this.activeHelp.setHelpText(MSGS.netModemToolTipActiveFilter());
        this.idleHelp.setHelpText(MSGS.netModemToolTipIdle());
        this.intervalHelp.setHelpText(MSGS.netModemToolTipLcpEchoInterval());
        this.failureHelp.setHelpText(MSGS.netModemToolTipLcpEchoFailure());
    }

    private void initForm() {

        // MODEL
        this.labelModel.setText(MSGS.netModemModel());

        // NETWORK TECHNOLOGY
        this.labelNetwork.setText(MSGS.netModemNetworkTechnology());
        this.network.addItem(MSGS.unknown());
        this.network.addMouseOverHandler(event -> {
            if (TabModemUi.this.network.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipNetworkTopology()));
            }
        });
        this.network.addMouseOutHandler(event -> resetHelp());
        this.network.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // CONNECTION TYPE
        this.labelService.setText(MSGS.netModemConnectionType());

        // MODEM IDENTIFIER
        this.labelModem.setText(MSGS.netModemIdentifier());
        this.modem.addMouseOverHandler(event -> {
            if (TabModemUi.this.modem.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipModemIndentifier()));
            }
        });
        this.modem.addMouseOutHandler(event -> resetHelp());
        this.modem.addValueChangeHandler(event -> setDirty(true));

        // INTERFACE NUMBER
        this.labelNumber.setText(MSGS.netModemInterfaceNum());
        this.number.setEnabled(false);
        this.number.addMouseOverHandler(event -> {
            TabModemUi.this.helpText.clear();
            TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipModemInterfaceNumber()));
        });
        this.number.addMouseOutHandler(event -> resetHelp());

        // APN
        this.labelApn.setText(MSGS.netModemAPN());
        this.apn.addMouseOverHandler(event -> {
            if (TabModemUi.this.apn.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipApn()));
            }
        });
        this.apn.addMouseOutHandler(event -> resetHelp());
        this.apn.addValueChangeHandler(event -> setDirty(true));

        // AUTH TYPE
        this.labelAuth.setText(MSGS.netModemAuthType());
        for (GwtModemAuthType a : GwtModemAuthType.values()) {
            this.auth.addItem(MessageUtils.get(a.name()));
        }
        this.auth.addMouseOverHandler(event -> {
            if (TabModemUi.this.auth.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipAuthentication()));
            }
        });
        this.auth.addMouseOutHandler(event -> resetHelp());
        this.auth.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // USERNAME
        this.labelUsername.setText(MSGS.netModemUsername());
        this.username.addMouseOverHandler(event -> {
            if (TabModemUi.this.username.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipUsername()));
            }
        });
        this.username.addMouseOutHandler(event -> resetHelp());
        this.username.addValueChangeHandler(event -> setDirty(true));

        // PASSWORD
        this.labelPassword.setText(MSGS.netModemPassword());
        this.password.addMouseOverHandler(event -> {
            if (TabModemUi.this.network.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipPassword()));
            }
        });
        this.password.addMouseOutHandler(event -> resetHelp());
        this.password.addValueChangeHandler(event -> setDirty(true));

        // MODEM RESET TIMEOUT
        this.labelReset.setText(MSGS.netModemResetTimeout() + "*");
        this.reset.addMouseOverHandler(event -> {
            if (TabModemUi.this.reset.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipResetTimeout()));
            }
        });
        this.reset.addMouseOutHandler(event -> resetHelp());
        this.reset.addValueChangeHandler(event -> {
            setDirty(true);
            if (TabModemUi.this.reset.getText().trim() != null
                    && (!TabModemUi.this.reset.getText().trim().matches(REGEX_NUM)
                            || Integer.parseInt(TabModemUi.this.reset.getText()) < 0
                            || Integer.parseInt(TabModemUi.this.reset.getText()) == 1)) {
                TabModemUi.this.helpReset.setText(MSGS.netModemInvalidResetTimeout());
                TabModemUi.this.groupReset.setValidationState(ValidationState.ERROR);
            } else {
                TabModemUi.this.helpReset.setText("");
                TabModemUi.this.groupReset.setValidationState(ValidationState.NONE);
            }
        });

        // REOPEN CONNECTION ON TERMINATION
        this.labelPersist.setText(MSGS.netModemPersist());
        this.radio1.addMouseOverHandler(event -> {
            if (TabModemUi.this.radio1.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipPersist()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio2.addMouseOverHandler(event -> {
            if (TabModemUi.this.radio2.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipPersist()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());

        this.radio1.addValueChangeHandler(event -> setDirty(true));
        this.radio2.addValueChangeHandler(event -> setDirty(true));

        // CONNECTION ATTEMPTS
        this.labelMaxfail.setText(MSGS.netModemMaxFail() + "*");
        this.maxfail.addMouseOverHandler(event -> {
            if (TabModemUi.this.maxfail.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipMaxFail()));
            }
        });
        this.maxfail.addMouseOutHandler(event -> resetHelp());
        this.maxfail.addValueChangeHandler(event -> {
            setDirty(true);
            if (TabModemUi.this.maxfail.getText().trim() != null
                    && (!TabModemUi.this.maxfail.getText().trim().matches(REGEX_NUM)
                            || Integer.parseInt(TabModemUi.this.maxfail.getText()) < 0)
                    || TabModemUi.this.maxfail.getText().trim().length() <= 0) {
                TabModemUi.this.helpMaxfail.setText(MSGS.netModemInvalidMaxFail());
                TabModemUi.this.groupMaxfail.setValidationState(ValidationState.ERROR);
            } else {
                TabModemUi.this.helpMaxfail.setText("");
                TabModemUi.this.groupMaxfail.setValidationState(ValidationState.NONE);
            }
        });

        this.labelHoldoff.setText(MSGS.netModemHoldoff() + "*");
        this.holdoff.addMouseOverHandler(event -> {
            if (TabModemUi.this.holdoff.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipHoldoff()));
            }
        });
        this.holdoff.addMouseOutHandler(event -> resetHelp());
        this.holdoff.addValueChangeHandler(event -> {
            setDirty(true);
            if (TabModemUi.this.holdoff.getText().trim() != null
                    && (!TabModemUi.this.holdoff.getText().trim().matches(REGEX_NUM)
                            || Integer.parseInt(TabModemUi.this.holdoff.getText()) < 0)
                    || TabModemUi.this.holdoff.getText().trim().length() <= 0) {
                TabModemUi.this.helpHoldoff.setText(MSGS.netModemInvalidHoldoff());
                TabModemUi.this.groupHoldoff.setValidationState(ValidationState.ERROR);
            } else {
                TabModemUi.this.helpHoldoff.setText("");
                TabModemUi.this.groupHoldoff.setValidationState(ValidationState.NONE);
            }
        });

        // DISCONNET IF IDLE
        this.labelIdle.setText(MSGS.netModemIdle() + "*");
        this.idle.addMouseOverHandler(event -> {
            if (TabModemUi.this.idle.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipIdle()));
            }
        });
        this.idle.addMouseOutHandler(event -> resetHelp());
        this.idle.addValueChangeHandler(event -> {
            setDirty(true);
            if (TabModemUi.this.idle.getText().trim() != null
                    && (!TabModemUi.this.idle.getText().trim().matches(REGEX_NUM)
                            || Integer.parseInt(TabModemUi.this.idle.getText()) < 0)) {
                TabModemUi.this.helpIdle.setText(MSGS.netModemInvalidIdle());
                TabModemUi.this.groupIdle.setValidationState(ValidationState.ERROR);
            } else {
                TabModemUi.this.helpIdle.setText("");
                TabModemUi.this.groupIdle.setValidationState(ValidationState.NONE);
            }
        });

        // ACTIVE FILTER
        this.labelActive.setText(MSGS.netModemActiveFilter());
        this.active.addMouseOverHandler(event -> {
            if (TabModemUi.this.active.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipActiveFilter()));
            }
        });
        this.active.addMouseOutHandler(event -> resetHelp());
        this.active.addValueChangeHandler(event -> setDirty(true));

        // LCP ECHO INTERVAL
        this.labelInterval.setText(MSGS.netModemLcpEchoInterval() + "*");
        this.interval.addMouseOverHandler(event -> {
            if (TabModemUi.this.interval.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipLcpEchoInterval()));
            }
        });
        this.interval.addMouseOutHandler(event -> resetHelp());
        this.interval.addValueChangeHandler(event -> {
            setDirty(true);
            if (TabModemUi.this.interval.getText().trim() != null
                    && (!TabModemUi.this.interval.getText().trim().matches(REGEX_NUM)
                            || Integer.parseInt(TabModemUi.this.interval.getText()) < 0)) {
                TabModemUi.this.helpInterval.setText(MSGS.netModemInvalidLcpEchoInterval());
                TabModemUi.this.groupInterval.setValidationState(ValidationState.ERROR);
            } else {
                TabModemUi.this.groupInterval.setValidationState(ValidationState.NONE);
            }
        });

        // LCP ECHO FAILURE
        this.labelFailure.setText(MSGS.netModemLcpEchoFailure() + "*");
        this.failure.addMouseOverHandler(event -> {
            if (TabModemUi.this.failure.isEnabled()) {
                TabModemUi.this.helpText.clear();
                TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipLcpEchoFailure()));
            }
        });
        this.failure.addMouseOutHandler(event -> resetHelp());
        this.failure.addValueChangeHandler(event -> {
            setDirty(true);
            if (TabModemUi.this.failure.getText().trim() != null
                    && (!TabModemUi.this.failure.getText().trim().matches(REGEX_NUM)
                            || Integer.parseInt(TabModemUi.this.failure.getText()) < 0)) {
                TabModemUi.this.helpFailure.setText(MSGS.netModemInvalidLcpEchoFailure());
                TabModemUi.this.groupFailure.setValidationState(ValidationState.ERROR);
            } else {
                TabModemUi.this.helpFailure.setText("");
                TabModemUi.this.groupFailure.setValidationState(ValidationState.NONE);
            }
        });

        this.helpTitle.setText(MSGS.netHelpTitle());
        this.radio1.setText(MSGS.trueLabel());
        this.radio2.setText(MSGS.falseLabel());

        this.radio1.setValue(true);
        this.radio2.setValue(false);
    }

    private void resetValidations() {
        this.groupApn.setValidationState(ValidationState.NONE);
        this.groupFailure.setValidationState(ValidationState.NONE);
        this.groupIdle.setValidationState(ValidationState.NONE);
        this.groupInterval.setValidationState(ValidationState.NONE);
        this.groupMaxfail.setValidationState(ValidationState.NONE);
        this.groupHoldoff.setValidationState(ValidationState.NONE);
        this.groupNumber.setValidationState(ValidationState.NONE);
        this.groupReset.setValidationState(ValidationState.NONE);

        this.helpReset.setText("");
        this.helpMaxfail.setText("");
        this.helpHoldoff.setText("");
        this.helpIdle.setText("");
        this.helpInterval.setText("");
        this.helpFailure.setText("");
        this.helpNumber.setText("");
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void update() {
        if (this.selectedNetIfConfig != null) {
            this.model.setText(this.selectedNetIfConfig.getManufacturer() + "-" + this.selectedNetIfConfig.getModel());
            this.network.clear();
            List<String> networkTechnologies = this.selectedNetIfConfig.getNetworkTechnology();
            if (networkTechnologies != null && !networkTechnologies.isEmpty()) {
                for (String techType : this.selectedNetIfConfig.getNetworkTechnology()) {
                    this.network.addItem(techType);
                }
            } else {
                this.network.addItem(MSGS.unknown());
            }
            this.service.setText(this.selectedNetIfConfig.getConnectionType());
            this.modem.setText(this.selectedNetIfConfig.getModemId());
            this.number.setText(String.valueOf(this.selectedNetIfConfig.getPppNum()));
            this.apn.setText(this.selectedNetIfConfig.getApn());

            GwtModemAuthType authType = GwtModemAuthType.netModemAuthNONE;
            if (this.selectedNetIfConfig.getAuthType() != null) {
                authType = this.selectedNetIfConfig.getAuthType();
            }
            for (int i = 0; i < this.auth.getItemCount(); i++) {
                if (this.auth.getItemText(i).equals(MessageUtils.get(authType.name()))) {
                    this.auth.setSelectedIndex(i);
                }
            }

            this.username.setText(this.selectedNetIfConfig.getUsername());
            this.password.setText(this.selectedNetIfConfig.getPassword());
            this.reset.setText(String.valueOf(this.selectedNetIfConfig.getResetTimeout()));

            if (this.selectedNetIfConfig.isPersist()) {
                this.radio1.setValue(true);
                this.radio2.setValue(false);
            } else {
                this.radio1.setValue(false);
                this.radio2.setValue(true);
            }

            this.maxfail.setText(String.valueOf(this.selectedNetIfConfig.getMaxFail()));
            this.holdoff.setText(String.valueOf(this.selectedNetIfConfig.getHoldoff()));
            this.idle.setText(String.valueOf(this.selectedNetIfConfig.getIdle()));
            this.active.setText(this.selectedNetIfConfig.getActiveFilter());
            this.interval.setText(String.valueOf(this.selectedNetIfConfig.getLcpEchoInterval()));
            this.failure.setText(String.valueOf(this.selectedNetIfConfig.getLcpEchoFailure()));
        }
        refreshForm();
    }

    private void refreshForm() {
        this.network.setEnabled(true);
        this.modem.setEnabled(true);
        this.number.setEnabled(false);
        this.apn.setEnabled(true);
        this.auth.setEnabled(true);
        this.username.setEnabled(true);
        this.password.setEnabled(true);
        this.reset.setEnabled(true);
        this.radio1.setEnabled(true);
        this.radio2.setEnabled(true);
        this.maxfail.setEnabled(true);
        this.holdoff.setEnabled(true);
        this.idle.setEnabled(true);
        this.active.setEnabled(true);
        this.interval.setEnabled(true);
        this.failure.setEnabled(true);

        String authTypeVal = this.auth.getSelectedItemText().trim();

        if (authTypeVal == null || authTypeVal.equalsIgnoreCase(MODEM_AUTH_NONE_MESSAGE)) {
            this.username.setEnabled(false);
            this.password.setEnabled(false);
        } else {
            this.username.setEnabled(true);
            this.password.setEnabled(true);
        }

        if (this.selectedNetIfConfig != null) {
            for (String techType : this.selectedNetIfConfig.getNetworkTechnology()) {
                if ("EVDO".equals(techType) || "CDMA".equals(techType)) {
                    this.apn.setEnabled(false);
                    this.auth.setEnabled(false);
                    this.username.setEnabled(false);
                    this.password.setEnabled(false);
                }
            }
        }
    }

    private void reset() {
        this.model.setText(null);
        this.network.setSelectedIndex(0);
        this.service.setText(null);
        this.modem.setText(null);
        this.number.setText(null);
        this.apn.setText(null);
        this.auth.setSelectedIndex(1);
        this.username.setText(null);
        this.password.setText(null);
        this.reset.setText(null);
        this.radio1.setValue(true);
        this.radio2.setValue(false);
        this.maxfail.setText(null);
        this.holdoff.setText(null);
        this.idle.setText(null);
        this.active.setText(null);
        this.interval.setText(null);
        this.failure.setText(null);
        update();
    }

}
