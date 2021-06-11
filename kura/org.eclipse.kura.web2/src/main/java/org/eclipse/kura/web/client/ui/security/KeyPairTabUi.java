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

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.NotEmptyValidator;
import org.eclipse.kura.web.client.ui.NotInListValidator;
import org.eclipse.kura.web.client.ui.PEMValidator;
import org.eclipse.kura.web.client.ui.PKCS8Validator;
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
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
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

    private final Button resetButton;

    private final Button applyButton;

    public KeyPairTabUi(final Type type, final List<String> keyStorePids, final List<String> usedAliases,
            final CertificateModalListener listener, Button resetButton, Button applyButton) {
        this.listener = listener;
        this.type = type;

        this.applyButton = applyButton;
        this.resetButton = resetButton;

        initWidget(uiBinder.createAndBindUi(this));
        initForm(keyStorePids, usedAliases);

        setDirty(false);
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;

        this.resetButton.setEnabled(flag);
        this.applyButton.setEnabled(flag);
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

    private void initForm(final List<String> keyStorePids, List<String> usedAliases) {
        StringBuilder title = new StringBuilder();
        title.append("<p style=\"margin-right: 5%;\">");
        title.append(
                this.type == Type.KEY_PAIR ? MSGS.securityKeyPairDescription() : MSGS.securityCertificateDescription());
        title.append(" ");
        title.append(MSGS.securityCertificateFormat() + " "
                + (this.type == Type.KEY_PAIR ? MSGS.securityPrivateKeyFormat() : ""));
        title.append("</p>");
        this.description.add(new Span(title.toString()));

        for (final String pid : keyStorePids) {
            this.pidListBox.addItem(pid);
        }

        NotEmptyValidator notEmptyValidator = new NotEmptyValidator(MSGS.formRequiredParameter());

        this.storageAliasInput.addValidator(notEmptyValidator);
        this.storageAliasInput.addValidator(new NotInListValidator<>(usedAliases, MSGS.certificateAliasUsed()));

        this.certificateInput.addValidator(notEmptyValidator);
        this.certificateInput.addValidator(new PEMValidator(MSGS.securityCertificateFormat()));

        this.storageAliasInput.addKeyUpHandler(e -> {
            this.storageAliasInput.validate();
            setDirty(true);
        });

        this.storageAliasInput.addBlurHandler(e -> {
            this.storageAliasInput.validate();
            setDirty(true);
        });

        this.certificateInput.addKeyUpHandler(e -> {
            this.certificateInput.validate();
            setDirty(true);
        });

        this.certificateInput.addBlurHandler(e -> {
            this.certificateInput.validate();
            setDirty(true);
        });

        this.certificateInput.setVisibleLines(20);

        this.resetButton.setText(MSGS.reset());
        this.resetButton.addClickHandler(event -> {
            reset();
            setDirty(false);
        });

        this.applyButton.setText(MSGS.apply());

        this.privateKeyInputForm.setVisible(this.type == Type.KEY_PAIR);
        if (this.type == Type.KEY_PAIR) {
            this.privateKeyInput.addValidator(notEmptyValidator);
            this.privateKeyInput.addValidator(new PKCS8Validator(MSGS.securityPrivateKeyFormat()));

            this.privateKeyInput.addKeyUpHandler(e -> {
                this.privateKeyInput.validate();
                setDirty(true);
            });

            this.privateKeyInput.addBlurHandler(e -> {
                this.privateKeyInput.validate();
                setDirty(true);
            });

            this.privateKeyInput.setVisibleLines(20);
        }

        this.applyButton.addClickHandler(event -> {

            final boolean isValid = isValid();

            if (isValid) {
                if (this.type == Type.KEY_PAIR) {
                    storeKeyPair();
                } else {
                    storeCertificate();
                }
                this.listener.onApply(isValid);
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
                            this.listener.onKeystoreChanged();
                        })))));
    }

    private void storeCertificate() {
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(
                token -> this.gwtCertificatesService.storeCertificate(token, this.pidListBox.getSelectedValue(),
                        this.certificateInput.getValue(), this.storageAliasInput.getValue(), c.callback(ok -> {
                            reset();
                            setDirty(false);
                            this.listener.onKeystoreChanged();
                        })))));
    }

    private void reset() {
        this.storageAliasInput.reset();
        this.privateKeyInput.reset();
        this.certificateInput.reset();
    }

    @Override
    public void clear() {
        // nothing to clear

    }

}
