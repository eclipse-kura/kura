/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.security;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.request.RequestQueue;
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
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class ApplicationCertsTabUi extends Composite implements Tab {

    private static ApplicationCertsTabUiUiBinder uiBinder = GWT.create(ApplicationCertsTabUiUiBinder.class);

    interface ApplicationCertsTabUiUiBinder extends UiBinder<Widget, ApplicationCertsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    private final CertificateModalListener listener;

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

    public ApplicationCertsTabUi(final CertificateModalListener listener) {
        this.listener = listener;
        initWidget(uiBinder.createAndBindUi(this));
        initForm();

        setDirty(false);
        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
    }

    @Override
    public boolean isValid() {
        boolean validAlias = this.formStorageAlias.validate();
        boolean validAppCert = this.formCert.validate();
        return validAlias && validAppCert;
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        this.reset.setEnabled(flag);
        this.apply.setEnabled(flag);
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
        this.description.add(new Span("<p>" + MSGS.settingsAddBundleCertsDescription() + "</p>"));

        final Validator<String> validator = new Validator<String>() {

            @Override
            public int getPriority() {
                return 0;
            }

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                final List<EditorError> result = new ArrayList<>();

                if (value == null || value.isEmpty()) {
                    result.add(new BasicEditorError(editor, value, MSGS.formRequiredParameter()));
                }

                return result;
            }
        };

        this.formCert.addValidator(validator);
        this.formStorageAlias.addValidator(validator);

        this.formCert.addKeyUpHandler(e -> {
            this.formCert.validate();
            setDirty(true);
        });
        this.formStorageAlias.addKeyUpHandler(e -> {
            this.formStorageAlias.validate();
            setDirty(true);
        });

        this.storageAliasLabel.setText(MSGS.settingsStorageAliasLabel());
        this.certificateLabel.setText(MSGS.settingsPublicCertLabel());
        this.formCert.setVisibleLines(20);

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            reset();
            setDirty(false);
        });

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> {
            final boolean isValid = isValid();
            this.listener.onApply(isValid);
            if (isValid()) {
                RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(
                        c.callback(token -> this.gwtCertificatesService.storeApplicationPublicChain(token,
                                this.formCert.getValue(), this.formStorageAlias.getValue(), c.callback(ok -> {
                                    reset();
                                    setDirty(false);
                                    listener.onKeystoreChanged();
                                })))));

            }
        });
    }

    private void reset() {
        this.formStorageAlias.reset();
        this.formCert.reset();
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }
}