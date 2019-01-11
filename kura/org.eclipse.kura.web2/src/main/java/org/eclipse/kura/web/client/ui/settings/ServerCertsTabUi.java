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

public class ServerCertsTabUi extends Composite implements Tab {

    private static ServerCertsTabUiUiBinder uiBinder = GWT.create(ServerCertsTabUiUiBinder.class);

    interface ServerCertsTabUiUiBinder extends UiBinder<Widget, ServerCertsTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    private boolean dirty;

    @UiField
    HTMLPanel description;
    @UiField
    Form serverSslCertsForm;
    @UiField
    FormGroup groupStorageAliasForm;
    @UiField
    FormGroup groupCertForm;
    @UiField
    FormLabel storageAliasLabel;
    @UiField
    FormLabel certificateLabel;
    @UiField
    Input storageAliasInput;
    @UiField
    TextArea certificateInput;
    @UiField
    Button reset;
    @UiField
    Button apply;

    public ServerCertsTabUi() {
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
        boolean validAppCert = isServerCertValid();
        boolean result = false;
        if (validAlias && validAppCert) {
            result = true;
        }
        return result;
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
        title.append(MSGS.settingsAddCertDescription1());
        title.append(" ");
        title.append(MSGS.settingsAddCertDescription2());
        title.append("</p>");
        this.description.add(new Span(title.toString()));

        this.storageAliasLabel.setText(MSGS.settingsStorageAliasLabel());
        this.storageAliasInput.addChangeHandler(event -> {
            isAliasValid();
            setDirty(true);
            ServerCertsTabUi.this.apply.setEnabled(true);
            ServerCertsTabUi.this.reset.setEnabled(true);
        });

        this.certificateLabel.setText(MSGS.settingsPublicCertLabel());
        this.certificateInput.setVisibleLines(20);
        this.certificateInput.addChangeHandler(event -> {
            isServerCertValid();
            setDirty(true);
            ServerCertsTabUi.this.apply.setEnabled(true);
            ServerCertsTabUi.this.reset.setEnabled(true);
        });

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            reset();
            setDirty(false);
            ServerCertsTabUi.this.apply.setEnabled(false);
            ServerCertsTabUi.this.reset.setEnabled(false);
        });

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> {
            if (isValid()) {
                EntryClassUi.showWaitModal();
                ServerCertsTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex);
                        EntryClassUi.hideWaitModal();
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        ServerCertsTabUi.this.gwtCertificatesService.storeSSLPublicChain(token,
                                ServerCertsTabUi.this.certificateInput.getValue(),
                                ServerCertsTabUi.this.storageAliasInput.getValue(), new AsyncCallback<Integer>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        FailureHandler.handle(caught);
                                        EntryClassUi.hideWaitModal();
                                    }

                                    @Override
                                    public void onSuccess(Integer certsStored) {
                                        reset();
                                        setDirty(false);
                                        ServerCertsTabUi.this.apply.setEnabled(false);
                                        ServerCertsTabUi.this.reset.setEnabled(false);
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

    private boolean isServerCertValid() {
        if (this.certificateInput.getText() == null || "".equals(this.certificateInput.getText().trim())) {
            this.groupCertForm.setValidationState(ValidationState.ERROR);
            return false;
        } else {
            this.groupCertForm.setValidationState(ValidationState.NONE);
            return true;
        }
    }
}
