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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtBannerService;
import org.eclipse.kura.web.shared.service.GwtBannerServiceAsync;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationService;
import org.eclipse.kura.web.shared.service.GwtPasswordAuthenticationServiceAsync;
import org.eclipse.kura.web2.ext.AlertSeverity;
import org.eclipse.kura.web2.ext.AuthenticationHandler;
import org.eclipse.kura.web2.ext.Context;
import org.eclipse.kura.web2.ext.ExtensionRegistry;
import org.eclipse.kura.web2.ext.WidgetFactory;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.InputGroup;
import org.gwtbootstrap3.client.ui.InputGroupAddon;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.InputType;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class LoginUi extends Composite implements Context {

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
    @UiField
    ListBox authenticationMethod;
    @UiField
    ModalBody loginModalBody;
    @UiField
    FormGroup authenticationMethodGroup;

    private PopupPanel waitModal;

    private Widget authenticationMethodWidget;
    private AuthenticationHandler authenticationHandler;

    private final Map<String, AuthenticationHandler> authenticationHandlers = new HashMap<>();

    public LoginUi() {
        initWaitModal();
        initWidget(uiBinder.createAndBindUi(this));

        addAuthenticationHandler(new PasswordAuthenticationHandler());

        this.authenticationMethod.setSelectedIndex(0);
        this.authenticationMethod
                .addChangeHandler(e -> setAuthenticationMethod(this.authenticationMethod.getSelectedItemText()));

        setAuthenticationMethod("Password");

        ExtensionRegistry.get().addExtensionConsumer(e -> e.onLoad(this));
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
            this.waitModal.show();
            this.authenticationHandler.authenticate(this.usernameInput.getValue(), new Callback<String, String>() {

                @Override
                public void onSuccess(String result) {
                    Window.Location.assign(result);
                }

                @Override
                public void onFailure(String reason) {
                    LoginUi.this.waitModal.hide();
                    LoginUi.this.alertDialog.show(reason, AlertDialog.Severity.ALERT, (ConfirmListener) null);
                }
            });
        });

        this.loginDialog.show();
        initLoginBannerModal();
    }

    private void setAuthenticationMethod(final String authenticationMethod) {

        this.authenticationHandler = this.authenticationHandlers.get(authenticationMethod);

        if (this.authenticationMethodWidget != null) {
            this.loginModalBody.remove(this.authenticationMethodWidget);
            this.authenticationMethodWidget = null;
        }

        final WidgetFactory factory = this.authenticationHandler.getLoginDialogElement();

        if (factory == null) {
            return;
        }

        this.authenticationMethodWidget = factory.buildWidget();

        if (this.authenticationMethodWidget != null) {
            this.loginModalBody.add(this.authenticationMethodWidget);
        }

    }

    private void initWaitModal() {
        this.waitModal = new PopupPanel(false, true);
        Icon icon = new Icon();
        icon.setId("cog");
        icon.setType(IconType.COG);
        icon.setSize(IconSize.TIMES4);
        icon.setSpin(true);
        this.waitModal.setWidget(icon);
        this.waitModal.setGlassEnabled(true);
        this.waitModal.center();
        this.waitModal.hide();
    }

    @Override
    public void addSidenavComponent(String name, String icon, WidgetFactory element) {
        // not supported
    }

    @Override
    public void addSettingsComponent(String name, WidgetFactory element) {
        // not supported
    }

    @Override
    public void addAuthenticationHandler(final AuthenticationHandler authenticationHandler) {
        final String name = authenticationHandler.getName();

        this.authenticationHandlers.put(name, authenticationHandler);
        this.authenticationMethod.addItem(name);

        this.authenticationMethodGroup.setVisible(this.authenticationHandlers.size() > 1);
    }

    @Override
    public void getXSRFToken(Callback<String, String> callback) {
        callback.onFailure("not supported");
    }

    @Override
    public Callback<Void, String> startLongRunningOperation() {
        this.waitModal.show();
        return new Callback<Void, String>() {

            @Override
            public void onFailure(String reason) {
                LoginUi.this.waitModal.hide();
                LoginUi.this.alertDialog.show(reason, AlertDialog.Severity.ALERT, (ConfirmListener) null);
            }

            @Override
            public void onSuccess(Void result) {
                LoginUi.this.waitModal.hide();
            }
        };
    }

    @Override
    public void showAlertDialog(String message, AlertSeverity severity, Consumer<Boolean> callback) {
        this.alertDialog.show(message,
                severity == AlertSeverity.INFO ? AlertDialog.Severity.INFO : AlertDialog.Severity.ALERT,
                callback::accept);
    }

    private class PasswordAuthenticationHandler implements AuthenticationHandler {

        private final InputGroup passwordGroup = new InputGroup();
        private final Input passwordInput = new Input();

        public PasswordAuthenticationHandler() {
            this.passwordGroup.addStyleName("login-input");

            this.passwordInput.setPlaceholder("Enter password");
            this.passwordInput.setType(InputType.PASSWORD);
            this.passwordInput.setAutoComplete(false);
            this.passwordInput.setId("login-password");

            final InputGroupAddon addon = new InputGroupAddon();
            addon.setIcon(IconType.KEY);
            addon.setIconSize(IconSize.LARGE);
            addon.addStyleName("login-icon");

            this.passwordGroup.add(addon);
            this.passwordGroup.add(this.passwordInput);
        }

        @Override
        public String getName() {
            return "Password";
        }

        @Override
        public WidgetFactory getLoginDialogElement() {
            return () -> {
                this.passwordInput.setValue("");
                return this.passwordGroup;
            };
        }

        @Override
        public void authenticate(final String userName, final Callback<String, String> callback) {
            LoginUi.this.pwdAutenticationService.authenticate(userName, this.passwordInput.getValue(),
                    new AsyncCallback<String>() {

                        @Override
                        public void onSuccess(final String redirectPath) {
                            callback.onSuccess(redirectPath);
                        }

                        @Override
                        public void onFailure(final Throwable caught) {

                            if (caught instanceof GwtKuraException) {
                                callback.onFailure("Login failed: The provided credentials are not valid.");
                            } else {
                                callback.onFailure("Login failed: The device is unreachable.");
                            }
                        }
                    });

        }

    }

}
