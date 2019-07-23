/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.firewall;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class IdsTabUi extends Composite implements Tab {

    private static IdsTabUiUiBinder uiBinder = GWT.create(IdsTabUiUiBinder.class);

    interface IdsTabUiUiBinder extends UiBinder<Widget, IdsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    private boolean dirty;

    @UiField
    Button apply;
    @UiField
    Button reset;

    @UiField
    FormGroup loginProtectionFormGroup;
    @UiField
    FormLabel loginProtectionLabel;
    @UiField
    HelpBlock loginProtectionHelpBlock;
    @UiField
    FlowPanel loginProtectionFlowPanel;
    @UiField
    InlineRadio loginProtectionTrue;
    @UiField
    InlineRadio loginProtectionFalse;

    @UiField
    FormGroup floodingProtectionFormGroup;
    @UiField
    FormLabel floodingProtectionLabel;
    @UiField
    HelpBlock floodingProtectionHelpBlock;
    @UiField
    FlowPanel floodingProtectionFlowPanel;
    @UiField
    InlineRadio floodingProtectionTrue;
    @UiField
    InlineRadio floodingProtectionFalse;

    public IdsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> apply());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> reset());

        this.apply.setEnabled(false);
        this.reset.setEnabled(false);

        this.gwtSecurityService.isLoginProtectionAvailable(new AsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    IdsTabUi.this.loginProtectionFormGroup.setVisible(true);
                    IdsTabUi.this.loginProtectionLabel.setText("Login Protection Status");
                    IdsTabUi.this.loginProtectionHelpBlock.setText("Enable/Disable login protection feature");
                    IdsTabUi.this.loginProtectionTrue.setText(MSGS.trueLabel());
                    IdsTabUi.this.loginProtectionFalse.setText(MSGS.falseLabel());
                    IdsTabUi.this.loginProtectionTrue.addValueChangeHandler(event -> {
                        setDirty(true);
                    });

                    IdsTabUi.this.loginProtectionFalse.addValueChangeHandler(event -> {
                        setDirty(true);
                    });
                } else {
                    IdsTabUi.this.loginProtectionFormGroup.setVisible(false);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                // TODO Auto-generated method stub

            }
        });

        this.gwtSecurityService.isFloodingProtectionAvailable(new AsyncCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    IdsTabUi.this.floodingProtectionFormGroup.setVisible(true);
                    IdsTabUi.this.floodingProtectionLabel.setText("Flooding Protection Status");
                    IdsTabUi.this.floodingProtectionHelpBlock.setText("Enable/Disable flooding protection feature");
                    IdsTabUi.this.floodingProtectionTrue.setText(MSGS.trueLabel());
                    IdsTabUi.this.floodingProtectionFalse.setText(MSGS.falseLabel());
                } else {
                    IdsTabUi.this.floodingProtectionFormGroup.setVisible(false);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                // TODO Auto-generated method stub

            }
        });

    }

    @Override
    public void refresh() {
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(token -> {
            this.gwtSecurityService.getLoginProtectionStatus(token, c.callback(status -> {
                this.loginProtectionTrue.setValue(status);
                this.loginProtectionFalse.setValue(!status);
            }));
        })));
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean b) {
        this.dirty = b;
        if (this.dirty) {
            this.apply.setEnabled(true);
            this.reset.setEnabled(true);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private void apply() {

        if (isDirty()) {
            if (this.loginProtectionFormGroup.isVisible()) {
                RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(
                        c.callback(token -> this.gwtSecurityService.setLoginProtectionStatus(token,
                                this.loginProtectionTrue.getValue(), c.callback(value -> {
                                    this.apply.setEnabled(false);
                                    this.reset.setEnabled(false);
                                    setDirty(false);
                                })))));
            }
            if (this.floodingProtectionFormGroup.isVisible()) {

            }
        }

    }

    private void reset() {
        if (isDirty()) {

        }
    }
}