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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.HelpButton.HelpTextProvider;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.model.GwtModemAuthType;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabModemUi extends Composite implements NetworkTab {

    private static final String MODEM_AUTH_NONE_MESSAGE = MessageUtils.get(GwtModemAuthType.netModemAuthNONE.name());
    private static TabModemUiUiBinder uiBinder = GWT.create(TabModemUiUiBinder.class);

    interface TabModemUiUiBinder extends UiBinder<Widget, TabModemUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String REGEX_NUM = "(?:\\d*)?\\d+";

    private final GwtSession session;
    private final TabTcpIpUi tcpTab;
    private boolean dirty;
    private GwtModemInterfaceConfig selectedNetIfConfig;
    private final Map<String, String> defaultDialString = new HashMap<String, String>();
    private String dialString;

    @UiField
    FormGroup groupReset, groupMaxfail, groupIdle, groupInterval, groupFailure, groupNumber, groupDial, groupApn;
    @UiField
    FormLabel labelModel, labelNetwork, labelService, labelModem, labelNumber, labelDial, labelApn, labelAuth,
            labelUsername, labelPassword, labelReset, labelPersist, labelMaxfail, labelIdle, labelActive, labelInterval,
            labelFailure;
    @UiField
    HelpBlock helpReset, helpMaxfail, helpIdle, helpInterval, helpFailure, helpNumber;

    @UiField
    ListBox network, auth;
    @UiField
    TextBox modem, number, dial, apn, username, reset, maxfail, idle, active, interval, failure;
    @UiField
    FormControlStatic model, service;
    @UiField
    Input password;
    @UiField
    InlineRadio radio1, radio2;
    @UiField
    PanelHeader helpTitle;
    @UiField
    ScrollPanel helpText;
    @UiField
    FieldSet field;

    @UiField
    HelpButton networkHelp, modemHelp, numberHelp, dialHelp, apnHelp, authHelp, usernameHelp, passwordHelp, resetHelp,
            persistHelp, maxfailHelp, idleHelp, activeHelp, intervalHelp, failureHelp;

    public TabModemUi(GwtSession currentSession, TabTcpIpUi tcp) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tcpTab = tcp;

        this.defaultDialString.put("HE910", "atd*99***1#");
        this.defaultDialString.put("DE910", "atd#777");
        initForm();

        initHelpButtons();

        this.tcpTab.status.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                update();
            }
        });
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
        if (this.number.getText() == null || "".equals(this.number.getText().trim())) {
            this.groupNumber.setValidationState(ValidationState.ERROR);
        }
        if (this.dial.getText() == null || "".equals(this.dial.getText().trim())) {
            this.groupDial.setValidationState(ValidationState.ERROR);
        }
        if (this.apn.getText() == null || "".equals(this.apn.getText().trim())) {
            if (this.apn.isEnabled()) {
                this.groupApn.setValidationState(ValidationState.ERROR);
            }
        }
        if (this.maxfail.getText() == null || "".equals(this.maxfail.getText().trim())) {
            this.groupMaxfail.setValidationState(ValidationState.ERROR);
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
                || this.groupDial.getValidationState().equals(ValidationState.ERROR)
                || this.groupApn.getValidationState().equals(ValidationState.ERROR)
                || this.groupMaxfail.getValidationState().equals(ValidationState.ERROR)
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
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtModemInterfaceConfig updatedModemNetIf = (GwtModemInterfaceConfig) updatedNetIf;

        if (this.model.getText() != null && this.service.getText() != null) {
            // note - status is set in tcp/ip tab
            updatedModemNetIf.setPppNum(Integer.parseInt(this.number.getText()));
            updatedModemNetIf.setModemId(this.modem.getText().trim() != null ? this.modem.getText().trim() : "");
            updatedModemNetIf.setDialString(this.dial.getText().trim() != null ? this.dial.getText().trim() : "");
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
            updatedModemNetIf.setIdle(Integer.parseInt(this.idle.getText().trim()));
            updatedModemNetIf.setActiveFilter(this.active.getText() != "" ? this.active.getText().trim() : "");
            updatedModemNetIf.setLcpEchoInterval(Integer.parseInt(this.interval.getText().trim()));
            updatedModemNetIf.setLcpEchoFailure(Integer.parseInt(this.failure.getText().trim()));
            // ---
        } else {
            // initForm hasn't been called yet

            updatedModemNetIf.setPppNum(this.selectedNetIfConfig.getPppNum());
            updatedModemNetIf.setModemId(this.selectedNetIfConfig.getModemId());
            updatedModemNetIf.setDialString(this.selectedNetIfConfig.getDialString());
            updatedModemNetIf.setApn(this.selectedNetIfConfig.getApn());
            updatedModemNetIf.setAuthType(this.selectedNetIfConfig.getAuthType());
            updatedModemNetIf.setUsername(this.selectedNetIfConfig.getUsername());
            updatedModemNetIf.setPassword(this.selectedNetIfConfig.getPassword());
            updatedModemNetIf.setResetTimeout(this.selectedNetIfConfig.getResetTimeout());
            updatedModemNetIf.setPersist(this.selectedNetIfConfig.isPersist());
            updatedModemNetIf.setMaxFail(this.selectedNetIfConfig.getMaxFail());
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
        this.dialHelp.setHelpTextProvider(new HelpTextProvider() {

            @Override
            public String getHelpText() {
                if ("".equals(dialString)) {
                    return MSGS.netModemToolTipDialStringDefault();
                } else {
                    return MSGS.netModemToolTipDialString(dial.getText());
                }
            }
        });
        this.apnHelp.setHelpText(MSGS.netModemToolTipApn());
        this.authHelp.setHelpText(MSGS.netModemToolTipAuthentication());
        this.usernameHelp.setHelpText(MSGS.netModemToolTipUsername());
        this.passwordHelp.setHelpText(MSGS.netModemToolTipPassword());
        this.resetHelp.setHelpText(MSGS.netModemToolTipResetTimeout());
        this.persistHelp.setHelpText(MSGS.netModemToolTipPersist());
        this.maxfailHelp.setHelpText(MSGS.netModemToolTipMaxFail());
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
        this.network.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.network.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipNetworkTopology()));
                }
            }
        });
        this.network.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.network.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                setDirty(true);
                refreshForm();
            }
        });

        // CONNECTION TYPE
        this.labelService.setText(MSGS.netModemConnectionType());

        // MODEM IDENTIFIER
        this.labelModem.setText(MSGS.netModemIdentifier());
        this.modem.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.modem.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipModemIndentifier()));
                }
            }
        });
        this.modem.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.modem.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                setDirty(true);
            }
        });

        // INTERFACE NUMBER
        this.labelNumber.setText(MSGS.netModemInterfaceNum() + "*");
        this.number.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                setDirty(true);
                if (TabModemUi.this.number.getText().trim() != null
                        && (!TabModemUi.this.number.getText().trim().matches(REGEX_NUM)
                                || Integer.parseInt(TabModemUi.this.number.getText()) < 0)) {
                    TabModemUi.this.helpNumber.setText("This Field requires a numeric input");
                    TabModemUi.this.groupNumber.setValidationState(ValidationState.ERROR);
                } else {
                    TabModemUi.this.helpNumber.setText("");
                    TabModemUi.this.groupNumber.setValidationState(ValidationState.NONE);
                }
            }
        });
        this.number.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.number.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipModemInterfaceNumber()));
                }
            }
        });
        this.number.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });

        // DIAL STRING
        this.labelDial.setText(MSGS.netModemDialString() + "*");

        this.dial.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.dialString = "";
        String model = "";
        if (this.selectedNetIfConfig != null) {
            model = this.selectedNetIfConfig.getModel();
            if (model != null && model.length() > 0) {
                if (model.contains("HE910")) {
                    this.dialString = this.defaultDialString.get("HE910");
                } else if (model.contains("DE910")) {
                    this.dialString = this.defaultDialString.get("DE910");
                } else {
                    this.dialString = "";
                }
            }
        }
        this.dial.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.dial.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    if (TabModemUi.this.dialString.equals("")) {
                        TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipDialStringDefault()));
                    } else {
                        TabModemUi.this.helpText
                                .add(new Span(MSGS.netModemToolTipDialString(TabModemUi.this.dial.getText())));
                    }
                }
            }
        });
        this.dial.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                setDirty(true);
                if (TabModemUi.this.dial.getText() == null || "".equals(TabModemUi.this.dial.getText().trim())) {
                    TabModemUi.this.groupDial.setValidationState(ValidationState.ERROR);
                } else {
                    TabModemUi.this.groupDial.setValidationState(ValidationState.NONE);
                }
            }
        });

        // APN
        this.labelApn.setText(MSGS.netModemAPN() + "*");
        this.apn.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.apn.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipApn()));
                }
            }
        });
        this.apn.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.apn.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                setDirty(true);
                if (TabModemUi.this.apn.getText() == null || "".equals(TabModemUi.this.apn.getText().trim())) {
                    if (TabModemUi.this.apn.isEnabled()) {
                        TabModemUi.this.groupApn.setValidationState(ValidationState.ERROR);
                    } else {
                        TabModemUi.this.groupApn.setValidationState(ValidationState.NONE);
                    }
                } else {
                    TabModemUi.this.groupApn.setValidationState(ValidationState.NONE);
                }
            }
        });

        // AUTH TYPE
        this.labelAuth.setText(MSGS.netModemAuthType());
        for (GwtModemAuthType a : GwtModemAuthType.values()) {
            this.auth.addItem(MessageUtils.get(a.name()));
        }
        this.auth.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.auth.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipAuthentication()));
                }
            }
        });
        this.auth.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.auth.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                setDirty(true);
                refreshForm();
            }
        });

        // USERNAME
        this.labelUsername.setText(MSGS.netModemUsername());
        this.username.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.username.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipUsername()));
                }
            }
        });
        this.username.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.username.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                setDirty(true);
            }
        });

        // PASSWORD
        this.labelPassword.setText(MSGS.netModemPassword());
        this.password.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.network.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipPassword()));
                }
            }
        });
        this.password.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.password.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                setDirty(true);
            }
        });

        // MODEM RESET TIMEOUT
        this.labelReset.setText(MSGS.netModemResetTimeout() + "*");
        this.reset.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.reset.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipResetTimeout()));
                }
            }
        });
        this.reset.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.reset.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
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
            }
        });

        // REOPEN CONNECTION ON TERMINATION
        this.labelPersist.setText(MSGS.netModemPersist());
        this.radio1.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.radio1.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipPersist()));
                }
            }
        });
        this.radio1.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.radio2.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.radio2.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipPersist()));
                }
            }
        });
        this.radio2.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });

        this.radio1.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                setDirty(true);
            }
        });
        this.radio2.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                setDirty(true);
            }
        });

        // CONNECTION ATTEMPTS
        this.labelMaxfail.setText(MSGS.netModemMaxFail() + "*");
        this.maxfail.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.maxfail.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipMaxFail()));
                }
            }
        });
        this.maxfail.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.maxfail.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                setDirty(true);
                if (TabModemUi.this.maxfail.getText().trim() != null
                        && (!TabModemUi.this.maxfail.getText().trim().matches(REGEX_NUM)
                                || Integer.parseInt(TabModemUi.this.maxfail.getText()) <= 0)
                        || TabModemUi.this.maxfail.getText().trim().length() <= 0) {
                    TabModemUi.this.helpMaxfail.setText(MSGS.netModemInvalidMaxFail());
                    TabModemUi.this.groupMaxfail.setValidationState(ValidationState.ERROR);
                } else {
                    TabModemUi.this.helpMaxfail.setText("");
                    TabModemUi.this.groupMaxfail.setValidationState(ValidationState.NONE);
                }
            }
        });

        // DISCONNET IF IDLE
        this.labelIdle.setText(MSGS.netModemIdle() + "*");
        this.idle.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.idle.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipIdle()));
                }
            }
        });
        this.idle.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.idle.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
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
            }
        });

        // ACTIVE FILTER
        this.labelActive.setText(MSGS.netModemActiveFilter());
        this.active.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.active.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipActiveFilter()));
                }
            }
        });
        this.active.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.active.addValueChangeHandler(new ValueChangeHandler<String>() {

            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                setDirty(true);
            }
        });

        // LCP ECHO INTERVAL
        this.labelInterval.setText(MSGS.netModemLcpEchoInterval() + "*");
        this.interval.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.interval.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipLcpEchoInterval()));
                }
            }
        });
        this.interval.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.interval.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                setDirty(true);
                if (TabModemUi.this.interval.getText().trim() != null
                        && (!TabModemUi.this.interval.getText().trim().matches(REGEX_NUM)
                                || Integer.parseInt(TabModemUi.this.interval.getText()) < 0)) {
                    TabModemUi.this.helpInterval.setText(MSGS.netModemInvalidLcpEchoInterval());
                    TabModemUi.this.groupInterval.setValidationState(ValidationState.ERROR);
                } else {
                    TabModemUi.this.groupInterval.setValidationState(ValidationState.NONE);
                }
            }
        });

        // LCP ECHO FAILURE
        this.labelFailure.setText(MSGS.netModemLcpEchoFailure() + "*");
        this.failure.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemUi.this.failure.isEnabled()) {
                    TabModemUi.this.helpText.clear();
                    TabModemUi.this.helpText.add(new Span(MSGS.netModemToolTipLcpEchoFailure()));
                }
            }
        });
        this.failure.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.failure.addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
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
        this.groupDial.setValidationState(ValidationState.NONE);
        this.groupFailure.setValidationState(ValidationState.NONE);
        this.groupIdle.setValidationState(ValidationState.NONE);
        this.groupInterval.setValidationState(ValidationState.NONE);
        this.groupMaxfail.setValidationState(ValidationState.NONE);
        this.groupNumber.setValidationState(ValidationState.NONE);
        this.groupReset.setValidationState(ValidationState.NONE);

        this.helpReset.setText("");
        this.helpMaxfail.setText("");
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
            this.dial.setText(this.selectedNetIfConfig.getDialString());
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
        this.number.setEnabled(true);
        this.dial.setEnabled(true);
        this.apn.setEnabled(true);
        this.auth.setEnabled(true);
        this.username.setEnabled(true);
        this.password.setEnabled(true);
        this.reset.setEnabled(true);
        this.radio1.setEnabled(true);
        this.radio2.setEnabled(true);
        this.maxfail.setEnabled(true);
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

        /*
         * if (selectedNetIfConfig.isGpsSupported()) {
         * radio1.setEnabled(true);
         * radio2.setEnabled(true);
         * } else {
         * radio1.setEnabled(false);
         * radio2.setEnabled(false);
         * }
         */

        if (this.selectedNetIfConfig != null) {
            for (String techType : this.selectedNetIfConfig.getNetworkTechnology()) {
                if (techType.equals("EVDO") || techType.equals("CDMA")) {
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
        this.dial.setText(null);
        this.apn.setText(null);
        this.auth.setSelectedIndex(1);
        this.username.setText(null);
        this.password.setText(null);
        this.reset.setText(null);
        this.radio1.setValue(true);
        this.radio2.setValue(false);
        this.maxfail.setText(null);
        this.idle.setText(null);
        this.active.setText(null);
        this.interval.setText(null);
        this.failure.setText(null);
        update();
    }
}
