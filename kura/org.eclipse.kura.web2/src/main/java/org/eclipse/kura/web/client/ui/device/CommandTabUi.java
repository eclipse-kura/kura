/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Widget;

public class CommandTabUi extends Composite {

    private static CommandTabUiUiBinder uiBinder = GWT.create(CommandTabUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String SERVLET_URL = "/" + GWT.getModuleName() + "/file/command";

    @SuppressWarnings("unused")
    private GwtSession session;

    interface CommandTabUiUiBinder extends UiBinder<Widget, CommandTabUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    @UiField
    TextBox formExecute;
    @UiField
    Input formPassword;
    @UiField
    Button reset;
    @UiField
    Button execute;
    @UiField
    FormPanel commandForm;
    @UiField
    FileUpload docPath;
    @UiField
    PanelBody resultPanel;
    @UiField
    Hidden xsrfTokenField;

    String command;
    String password;
    SafeHtmlBuilder safeHtml = new SafeHtmlBuilder();

    public CommandTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.formExecute.clear();
        this.formExecute.setFocus(true);
        this.formExecute.setName("command");
        this.formExecute.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                CommandTabUi.this.formPassword.setFocus(true);
            }
        });

        this.xsrfTokenField.setID("xsrfToken");
        this.xsrfTokenField.setName("xsrfToken");
        this.xsrfTokenField.setValue("");

        this.formPassword.setText(null);
        this.formPassword.setName("password");
        this.formPassword.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                CommandTabUi.this.execute.setFocus(true);
            }
        });

        this.docPath.setName("file");

        display(MSGS.deviceCommandNoOutput());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            CommandTabUi.this.commandForm.reset();
            CommandTabUi.this.formExecute.setFocus(true);
            display(MSGS.deviceCommandNoOutput());
        });

        this.execute.setText(MSGS.deviceCommandExecute());
        this.execute.addClickHandler(event -> CommandTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CommandTabUi.this.xsrfTokenField.setValue(token.getToken());
                CommandTabUi.this.commandForm.submit();
                CommandTabUi.this.formExecute.setFocus(true);
            }
        }));

        this.commandForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.commandForm.setMethod(FormPanel.METHOD_POST);
        this.commandForm.setAction(SERVLET_URL);

        this.commandForm.addSubmitCompleteHandler(event -> {

            String result = event.getResults();

            if (result.contains("HTTP ERROR")) {
                display(MSGS.fileUploadFailure());
            } else {
                EntryClassUi.showWaitModal();
                CommandTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        CommandTabUi.this.gwtDeviceService.executeCommand(token,
                                CommandTabUi.this.formExecute.getText(), CommandTabUi.this.formPassword.getText(),
                                new AsyncCallback<String>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                if (caught.getLocalizedMessage()
                                        .equals(GwtKuraErrorCode.SERVICE_NOT_ENABLED.toString())) {
                                    display(MSGS.error() + "\n" + MSGS.commandServiceNotEnabled());
                                } else if (caught.getLocalizedMessage()
                                        .equals(GwtKuraErrorCode.ILLEGAL_ARGUMENT.toString())) {
                                    display(MSGS.error() + "\n" + MSGS.commandPasswordNotCorrect());
                                } else {
                                    display(MSGS.error() + "\n" + caught.getLocalizedMessage());
                                }
                                EntryClassUi.hideWaitModal();
                            }

                            @Override
                            public void onSuccess(String result) {
                                display(result);
                                EntryClassUi.hideWaitModal();
                            }

                        });
                    }

                });
            }

        });

    }

    public void display(String string) {
        this.resultPanel.clear();
        this.resultPanel.add(new HTML(new SafeHtmlBuilder().appendEscapedLines(string).toSafeHtml()));
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

}
