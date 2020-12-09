/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.login;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtLoginInfo;
import org.eclipse.kura.web.shared.service.GwtLoginInfoService;
import org.eclipse.kura.web.shared.service.GwtLoginInfoServiceAsync;
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
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
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

    private final GwtLoginInfoServiceAsync gwtLoginInfoService = GWT.create(GwtLoginInfoService.class);

    interface LoginUiBinder extends UiBinder<Widget, LoginUi> {
    }

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
    @UiField
    Button loginResetButton;

    private PopupPanel waitModal;

    private Widget authenticationMethodWidget;
    private AuthenticationHandler authenticationHandler;

    private Set<String> enabledAuthenticationHandlers = new HashSet<>();

    private final Map<String, AuthenticationHandler> authenticationHandlers = new LinkedHashMap<>();

    public LoginUi() {
        initWaitModal();
        initWidget(uiBinder.createAndBindUi(this));
    }

    private void initLoginBannerModal(final GwtLoginInfo loginInfo) {
        this.accessBannerModal.setTitle(MSGS.warning());
        this.buttonAccessBannerModalOk.setText(MSGS.okButton());

        if (loginInfo.getBannerContent() != null) {
            LoginUi.this.accessBannerModalPannelBody.setText(loginInfo.getBannerContent());
            LoginUi.this.accessBannerModal.show();
        }
    }

    private void initAuthenticationHandlers(final GwtLoginInfo loginInfo) {

        enabledAuthenticationHandlers = loginInfo.getEnabledAuthMethods();

        addAuthenticationHandler(new PasswordAuthenticationHandler());

        if (loginInfo.getCertAuthPort() != null) {
            addAuthenticationHandler(new CertificateAuthenticationHandler(loginInfo.getCertAuthPort()));
        }

        authenticationMethod.addChangeHandler(e -> setAuthenticationMethod(authenticationMethod.getSelectedItemText()));

        ExtensionRegistry.get().addExtensionConsumer(e -> e.onLoad(LoginUi.this));

    }

    @Override
    protected void onAttach() {
        super.onAttach();

        this.loginForm.addSubmitHandler(e -> {
            e.cancel();
            this.waitModal.show();
            this.authenticationHandler.authenticate(new Callback<String, String>() {

                @Override
                public void onSuccess(String result) {
                    Window.Location.assign(result);
                }

                @Override
                public void onFailure(String reason) {
                    LoginUi.this.waitModal.hide();
                    LoginUi.this.alertDialog.show(reason, AlertDialog.Severity.ERROR, (ConfirmListener) null);
                }
            });
        });

        this.loginDialog.show();
        this.gwtLoginInfoService.getLoginInfo(new AsyncCallback<GwtLoginInfo>() {

            @Override
            public void onFailure(final Throwable caught) {
                // do nothing
            }

            @Override
            public void onSuccess(final GwtLoginInfo result) {
                initLoginBannerModal(result);
                initAuthenticationHandlers(result);
            }
        });

    }

    @UiHandler("loginResetButton")
    public void onFormResetClick(ClickEvent event) {
        loginForm.reset();
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

        if (!enabledAuthenticationHandlers.contains(name)) {
            return;
        }

        this.authenticationHandlers.put(name, authenticationHandler);
        this.authenticationMethod.addItem(name);

        if (this.authenticationHandlers.size() == 1) {
            this.authenticationMethod.setSelectedIndex(0);
            this.authenticationMethodGroup.setVisible(false);
            setAuthenticationMethod(authenticationHandler.getName());
        }

        this.authenticationMethodGroup.setVisible(this.authenticationHandlers.size() >= 2);
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

        private final FormGroup group = new FormGroup();

        private final InputGroup usernameGroup = new InputGroup();
        private final Input usernameInput = new Input();

        private final InputGroup passwordGroup = new InputGroup();
        private final Input passwordInput = new Input();

        public PasswordAuthenticationHandler() {

            this.usernameGroup.addStyleName("login-input");

            this.usernameInput.setPlaceholder("Enter username");
            this.usernameInput.setType(InputType.TEXT);
            this.usernameInput.setAutoComplete(false);
            this.usernameInput.setId("login-user");

            final InputGroupAddon usernameAddon = new InputGroupAddon();
            usernameAddon.setIcon(IconType.USER);
            usernameAddon.setIconSize(IconSize.LARGE);
            usernameAddon.addStyleName("login-icon");

            this.usernameGroup.add(usernameAddon);
            this.usernameGroup.add(usernameInput);

            this.passwordGroup.addStyleName("login-input");

            this.passwordInput.setPlaceholder("Enter password");
            this.passwordInput.setType(InputType.PASSWORD);
            this.passwordInput.setAutoComplete(false);
            this.passwordInput.setId("login-password");

            final InputGroupAddon passwordAddon = new InputGroupAddon();
            passwordAddon.setIcon(IconType.KEY);
            passwordAddon.setIconSize(IconSize.LARGE);
            passwordAddon.addStyleName("login-icon");

            this.passwordGroup.add(passwordAddon);
            this.passwordGroup.add(this.passwordInput);

            this.group.add(usernameGroup);
            this.group.add(passwordGroup);
        }

        @Override
        public String getName() {
            return "Password";
        }

        @Override
        public WidgetFactory getLoginDialogElement() {
            return () -> {
                this.passwordInput.setValue("");
                return this.group;
            };
        }

        @Override
        public void authenticate(final Callback<String, String> callback) {
            LoginUi.this.pwdAutenticationService.authenticate(this.usernameInput.getValue(),
                    this.passwordInput.getValue(), new AsyncCallback<String>() {

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

    private class CertificateAuthenticationHandler implements AuthenticationHandler {

        private final int clientAuthPort;

        public CertificateAuthenticationHandler(final int clientAuthPort) {
            this.clientAuthPort = clientAuthPort;
        }

        @Override
        public String getName() {
            return "Certificate";
        }

        @Override
        public WidgetFactory getLoginDialogElement() {
            return () -> {
                final Paragraph paragraph = new Paragraph();
                paragraph.setText("Press Login to perform certificate based authentication.");
                return paragraph;
            };
        }

        @Override
        public void authenticate(Callback<String, String> callback) {
            Window.Location
                    .assign("https://" + Window.Location.getHostName() + ":" + clientAuthPort + "/admin/login/cert");
        }

    }

}
