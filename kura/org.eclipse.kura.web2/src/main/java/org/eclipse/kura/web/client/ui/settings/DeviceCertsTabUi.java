/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.settings;

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
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class DeviceCertsTabUi extends Composite implements Tab {

    private static DeviceCertsTabUiUiBinder uiBinder = GWT.create(DeviceCertsTabUiUiBinder.class);

    interface DeviceCertsTabUiUiBinder extends UiBinder<Widget, DeviceCertsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    private boolean dirty;

    @UiField
    HTMLPanel description;
    @UiField
    Form deviceSslCertsForm;
    @UiField
    FormGroup groupStorageAliasForm;
    @UiField
    FormGroup groupPrivateKeyForm;
    @UiField
    FormGroup groupCertForm;
    @UiField
    FormLabel storageAliasLabel;
    @UiField
    FormLabel privateKeyLabel;
    @UiField
    FormLabel certificateLabel;
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

    public DeviceCertsTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        initForm();

        setDirty(false);
        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
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
    public boolean isValid() {
        boolean validAlias = isAliasValid();
        boolean validPrivateKey = isPrivateKeyValid();
        boolean validDeviceCert = isDeviceCertValid();
        return validAlias && validPrivateKey && validDeviceCert;
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            reset();
        }
    }

    private void initForm() {
        StringBuilder title = new StringBuilder();
        title.append("<p>");
        title.append(MSGS.settingsMAuthDescription1());
        title.append(" ");
        title.append(MSGS.settingsMAuthDescription2());
        title.append("</p>");
        this.description.add(new Span(title.toString()));

        this.storageAliasLabel.setText(MSGS.settingsStorageAliasLabel());
        this.storageAliasInput.addChangeHandler(event -> {
            isAliasValid();
            setDirty(true);
            DeviceCertsTabUi.this.apply.setEnabled(true);
            DeviceCertsTabUi.this.reset.setEnabled(true);
        });

        this.privateKeyLabel.setText(MSGS.settingsPrivateCertLabel());
        this.privateKeyInput.setVisibleLines(20);
        this.privateKeyInput.addChangeHandler(event -> {
            isPrivateKeyValid();
            setDirty(true);
            DeviceCertsTabUi.this.apply.setEnabled(true);
            DeviceCertsTabUi.this.reset.setEnabled(true);
        });

        this.certificateLabel.setText(MSGS.settingsPublicCertLabel());
        this.certificateInput.setVisibleLines(20);
        this.certificateInput.addChangeHandler(event -> {
            isDeviceCertValid();
            setDirty(true);
            DeviceCertsTabUi.this.apply.setEnabled(true);
            DeviceCertsTabUi.this.reset.setEnabled(true);
        });

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            reset();
            setDirty(false);
            DeviceCertsTabUi.this.apply.setEnabled(false);
            DeviceCertsTabUi.this.reset.setEnabled(false);
        });

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> {
            if (isValid()) {
                EntryClassUi.showWaitModal();
                DeviceCertsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                        EntryClassUi.hideWaitModal();
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        DeviceCertsTabUi.this.gwtCertificatesService.storePublicPrivateKeys(token,
                                DeviceCertsTabUi.this.privateKeyInput.getValue(),
                                DeviceCertsTabUi.this.certificateInput.getValue(), null,
                                DeviceCertsTabUi.this.storageAliasInput.getValue(), new AsyncCallback<Integer>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        FailureHandler.handle(caught);
                                        EntryClassUi.hideWaitModal();
                                    }

                                    @Override
                                    public void onSuccess(Integer certsStored) {
                                        reset();
                                        setDirty(false);
                                        DeviceCertsTabUi.this.apply.setEnabled(false);
                                        DeviceCertsTabUi.this.reset.setEnabled(false);
                                        EntryClassUi.hideWaitModal();
                                    }
                                });
                    }
                });
            }
        });
    }

    private void reset() {
        this.storageAliasInput.setText("");
        this.privateKeyInput.setText("");
        this.certificateInput.setText("");
    }

    private boolean isAliasValid() {
        if (this.storageAliasInput.getText() == null || "".equals(this.storageAliasInput.getText().trim())) {
            this.groupStorageAliasForm.setValidationState(ValidationState.ERROR);
            return false;
        } else {
            this.groupStorageAliasForm.setValidationState(ValidationState.NONE);
            return true;
        }
    }

    private boolean isPrivateKeyValid() {
        if (this.certificateInput.getText() == null || "".equals(this.certificateInput.getText().trim())) {
            this.groupCertForm.setValidationState(ValidationState.ERROR);
            return false;
        } else {
            this.groupCertForm.setValidationState(ValidationState.NONE);
            return true;
        }
    }

    private boolean isDeviceCertValid() {
        if (this.certificateInput.getText() == null || "".equals(this.certificateInput.getText().trim())) {
            this.groupCertForm.setValidationState(ValidationState.ERROR);
            return false;
        } else {
            this.groupCertForm.setValidationState(ValidationState.NONE);
            return true;
        }
    }
}
