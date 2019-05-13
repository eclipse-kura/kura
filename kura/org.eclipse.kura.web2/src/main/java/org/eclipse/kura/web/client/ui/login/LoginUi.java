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

import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.Severity;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationService;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Modal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class LoginUi extends Composite {

    private static final int ENTER_CHAR_CODE = 13;

    private final GwtPasswordAuthenticationServiceAsync pwdAutenticationService = GWT
            .create(GwtPasswordAuthenticationService.class);

    private static final LoginUiBinder uiBinder = GWT.create(LoginUiBinder.class);

    interface LoginUiBinder extends UiBinder<Widget, LoginUi> {
    }

    @UiField
    Input usernameInput;
    @UiField
    Input passwordInput;
    @UiField
    Button loginButton;
    @UiField
    AlertDialog alertDialog;
    @UiField
    Modal loginDialog;

    public LoginUi() {
        initWidget(uiBinder.createAndBindUi(this));

        loginButton.addClickHandler(e -> login());

        RootPanel.get().addDomHandler(e -> {

            if (e.getCharCode() == ENTER_CHAR_CODE) {
                login();
            }
        }, KeyPressEvent.getType());

        loginDialog.show();
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        usernameInput.setFocus(true);
    }

    private void login() {
        pwdAutenticationService.authenticate(usernameInput.getValue(), passwordInput.getValue(),
                new AsyncCallback<String>() {

                    @Override
                    public void onSuccess(final String redirectPath) {
                        Window.Location.assign(redirectPath);
                    }

                    @Override
                    public void onFailure(final Throwable caught) {
                        if (caught instanceof GwtKuraException) {
                            alertDialog.show("Login failed: The provided credentials are not valid.", Severity.ALERT,
                                    null);
                        } else {
                            alertDialog.show("Login failed: The device is unreachable.", Severity.ALERT, null);
                        }
                    }
                });
    }
}
