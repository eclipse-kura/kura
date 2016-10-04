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
package org.eclipse.kura.web.client.ui.Settings;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteEvent;
import org.gwtbootstrap3.client.ui.base.form.AbstractForm.SubmitCompleteHandler;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class ApplicationCertsTabUi extends Composite implements Tab {

    private static ApplicationCertsTabUiUiBinder uiBinder = GWT.create(ApplicationCertsTabUiUiBinder.class);

    interface ApplicationCertsTabUiUiBinder extends UiBinder<Widget, ApplicationCertsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final static String SERVLET_URL = "/" + GWT.getModuleName() + "/file/certificate";

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    private boolean dirty;

    @UiField
    Form appCertsForm;
    @UiField
    FormGroup groupStorageAlias;
    @UiField
    FormGroup groupFormCert;
    @UiField
    HTMLPanel description;
    @UiField
    FormLabel storageAliasLabel;
    @UiField
    FormLabel certificateLabel;
    @UiField
    TextArea formCert;
    @UiField
    Input formStorageAlias;
    @UiField
    Button reset;
    @UiField
    Button apply;

    public ApplicationCertsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        initForm();

        setDirty(false);
        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
    }

    @Override
    public boolean isValid() {
        boolean validAlias = isAliasValid();
        boolean validAppCert = isAppCertValid();
        if (validAlias && validAppCert) {
            return true;
        }
        return false;
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
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            reset();
        }
    }

    private void initForm() {
        this.appCertsForm.setAction(SERVLET_URL);
        this.appCertsForm.setEncoding(com.google.gwt.user.client.ui.FormPanel.ENCODING_MULTIPART);
        this.appCertsForm.setMethod(com.google.gwt.user.client.ui.FormPanel.METHOD_POST);
        this.description.add(new Span("<p>" + MSGS.settingsAddBundleCertsDescription() + "</p>"));
        this.appCertsForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {

            @Override
            public void onSubmitComplete(SubmitCompleteEvent event) {
                ApplicationCertsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                        EntryClassUi.hideWaitModal();
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        ApplicationCertsTabUi.this.gwtCertificatesService.storeApplicationPublicChain(token,
                                ApplicationCertsTabUi.this.formCert.getValue(),
                                ApplicationCertsTabUi.this.formStorageAlias.getValue(), new AsyncCallback<Integer>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                FailureHandler.handle(caught);
                                EntryClassUi.hideWaitModal();
                            }

                            @Override
                            public void onSuccess(Integer certsStored) {
                                reset();
                                setDirty(false);
                                ApplicationCertsTabUi.this.apply.setEnabled(false);
                                ApplicationCertsTabUi.this.reset.setEnabled(false);
                                EntryClassUi.hideWaitModal();
                            }
                        });
                    }
                });
            }
        });

        this.storageAliasLabel.setText(MSGS.settingsStorageAliasLabel());
        this.formStorageAlias.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                isAliasValid();
                setDirty(true);
                ApplicationCertsTabUi.this.apply.setEnabled(true);
                ApplicationCertsTabUi.this.reset.setEnabled(true);
            }
        });

        this.certificateLabel.setText(MSGS.settingsAddCertLabel()); // TODO: this is a wrong label. Change it!
        this.formCert.setVisibleLines(20);
        this.formCert.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                isAppCertValid();
                setDirty(true);
                ApplicationCertsTabUi.this.apply.setEnabled(true);
                ApplicationCertsTabUi.this.reset.setEnabled(true);
            }
        });

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                reset();
                setDirty(false);
                ApplicationCertsTabUi.this.apply.setEnabled(false);
                ApplicationCertsTabUi.this.reset.setEnabled(false);
            }
        });

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (isValid()) {
                    EntryClassUi.showWaitModal();
                    ApplicationCertsTabUi.this.appCertsForm.submit();
                }
            }
        });
    }

    private void reset() {
        this.formStorageAlias.setText("");
        this.formCert.setText("");
    }

    private boolean isAliasValid() {
        if (this.formStorageAlias.getText() == null || "".equals(this.formStorageAlias.getText().trim())) {
            this.groupStorageAlias.setValidationState(ValidationState.ERROR);
            return false;
        } else {
            this.groupStorageAlias.setValidationState(ValidationState.NONE);
            return true;
        }
    }

    private boolean isAppCertValid() {
        if (this.formCert.getText() == null || "".equals(this.formCert.getText().trim())) {
            this.groupFormCert.setValidationState(ValidationState.ERROR);
            return false;
        } else {
            this.groupFormCert.setValidationState(ValidationState.NONE);
            return true;
        }
    }
}