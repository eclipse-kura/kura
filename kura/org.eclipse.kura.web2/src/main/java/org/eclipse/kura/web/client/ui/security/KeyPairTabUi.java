/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
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

public class KeyPairTabUi extends Composite implements Tab {

    public enum Type {
        KEY_PAIR,
        CERTIFICATE
    }

    private static KeyPairTabUiUiBinder uiBinder = GWT.create(KeyPairTabUiUiBinder.class);

    interface KeyPairTabUiUiBinder extends UiBinder<Widget, KeyPairTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    private final CertificateModalListener listener;

    private boolean dirty;

    private final Type type;

    @UiField
    HTMLPanel description;
    @UiField
    FormGroup privateKeyInputForm;
    @UiField
    ListBox pidListBox;
    @UiField
    Input storageAliasInput;
    @UiField
    TextArea privateKeyInput;
    @UiField
    TextArea certificateInput;
    @UiField
    Button reset;
    @UiField
    Button apply;

    public KeyPairTabUi(final Type type, final List<String> keyStorePids, final CertificateModalListener listener) {
        this.listener = listener;
        this.type = type;

        initWidget(uiBinder.createAndBindUi(this));
        initForm(keyStorePids);

        setDirty(false);
        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
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
    public boolean isValid() {
        boolean validAlias = this.storageAliasInput.validate();
        boolean validPrivateKey = this.privateKeyInput.validate();
        boolean validDeviceCert = this.certificateInput.validate();
        return validAlias && validPrivateKey && validDeviceCert;
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            reset();
        }
    }

    private void initForm(final List<String> keyStorePids) {
        StringBuilder title = new StringBuilder();
        title.append("<p style=\"margin-right: 5%;\">");
        title.append(type == Type.KEY_PAIR ? MSGS.securityKeyPairDescription() : MSGS.securityCertificateDescription());
        title.append(" ");
        title.append(MSGS.securityCertificateFormat() + " "
                + (type == Type.KEY_PAIR ? MSGS.securityPrivateKeyFormat() : ""));
        title.append("</p>");
        this.description.add(new Span(title.toString()));

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

        for (final String pid : keyStorePids) {
            this.pidListBox.addItem(pid);
        }

        this.storageAliasInput.addValidator(validator);

        this.certificateInput.addValidator(validator);

        this.storageAliasInput.addKeyUpHandler(e -> {
            this.storageAliasInput.validate();
            setDirty(true);
        });

        this.certificateInput.addKeyUpHandler(e -> {
            this.certificateInput.validate();
            setDirty(true);
        });

        this.certificateInput.setVisibleLines(20);

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            reset();
            setDirty(false);
        });

        this.apply.setText(MSGS.apply());

        this.privateKeyInputForm.setVisible(this.type == Type.KEY_PAIR);
        if (this.type == Type.KEY_PAIR) {
            this.privateKeyInput.addValidator(validator);
            this.privateKeyInput.addKeyUpHandler(e -> {
                this.privateKeyInput.validate();
                setDirty(true);
            });
            this.privateKeyInput.setVisibleLines(20);
        }

        this.apply.addClickHandler(event -> {
            final boolean isValid = isValid();
            this.listener.onApply(isValid);
            if (isValid) {
                if (this.type == Type.KEY_PAIR) {
                    storeKeyPair();
                } else {
                    storeCertificate();
                }
            }
        });
    }

    private void storeKeyPair() {
        RequestQueue.submit(c -> this.gwtXSRFService
                .generateSecurityToken(c.callback(token -> this.gwtCertificatesService.storeKeyPair(token,
                        this.pidListBox.getSelectedValue(), this.privateKeyInput.getValue(),
                        this.certificateInput.getValue(), this.storageAliasInput.getValue(), c.callback(ok -> {
                            reset();
                            setDirty(false);
                            listener.onKeystoreChanged();
                        })))));
    }

    private void storeCertificate() {
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(
                token -> this.gwtCertificatesService.storeCertificate(token, this.pidListBox.getSelectedValue(),
                        this.certificateInput.getValue(), this.storageAliasInput.getValue(), c.callback(ok -> {
                            reset();
                            setDirty(false);
                            listener.onKeystoreChanged();
                        })))));
    }

    private void reset() {
        this.storageAliasInput.reset();
        this.privateKeyInput.reset();
        this.certificateInput.reset();
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }
}
