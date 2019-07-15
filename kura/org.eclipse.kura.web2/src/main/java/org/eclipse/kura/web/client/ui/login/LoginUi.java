/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.login;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.Severity;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtBannerService;
import org.eclipse.kura.web.shared.service.GwtBannerServiceAsync;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationService;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;

public class LoginUi extends Composite {

    private final GwtPasswordAuthenticationServiceAsync pwdAutenticationService = GWT
            .create(GwtPasswordAuthenticationService.class);

    private static final LoginUiBinder uiBinder = GWT.create(LoginUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtBannerServiceAsync gwtBannerService = GWT.create(GwtBannerService.class);

    interface LoginUiBinder extends UiBinder<Widget, LoginUi> {
    }

    @UiField
    Input usernameInput;
    @UiField
    Input passwordInput;
    @UiField
    AlertDialog alertDialog;
    @UiField
    Modal loginDialog;
    @UiField
    FormPanel loginForm;
    @UiField
    Modal accessBannerModal;
    @UiField
    Button buttonAccessBannerModalOk;
    @UiField
    Strong accessBannerModalPannelBody;

    public LoginUi() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    private void initLoginBannerModal() {
        this.accessBannerModal.setTitle(MSGS.warning());
        this.buttonAccessBannerModalOk.setText(MSGS.okButton());

        this.gwtBannerService.getLoginBanner(new AsyncCallback<String>() {

            @Override
            public void onFailure(Throwable caught) {
                // Nothing to do
            }

            @Override
            public void onSuccess(String banner) {
                if (banner != null) {
                    LoginUi.this.accessBannerModalPannelBody.setText(banner);
                    LoginUi.this.accessBannerModal.show();
                }
            }
        });
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        this.usernameInput.setFocus(true);

        this.loginForm.addSubmitHandler(e -> {
            e.cancel();
            login();
        });

        this.loginDialog.show();
        initLoginBannerModal();
    }

    private void login() {
        this.pwdAutenticationService.authenticate(this.usernameInput.getValue(), this.passwordInput.getValue(),
                new AsyncCallback<String>() {

                    @Override
                    public void onSuccess(final String redirectPath) {
                        Window.Location.assign(redirectPath);
                    }

                    @Override
                    public void onFailure(final Throwable caught) {
                        if (caught instanceof GwtKuraException) {
                            LoginUi.this.alertDialog.show("Login failed: The provided credentials are not valid.",
                                    Severity.ALERT, null);
                        } else {
                            LoginUi.this.alertDialog.show("Login failed: The device is unreachable.", Severity.ALERT,
                                    null);
                        }
                    }
                });
    }
}
