/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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
        setButtonsEnabled(false);
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
        return this.serverSslCertsForm.validate();
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            reset();
        }
    }

    @Override
    public void clear() {
        reset();
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
            setDirty(true);
            setButtonsEnabled(true);
        });

        this.certificateLabel.setText(MSGS.settingsPublicCertLabel());
        this.certificateInput.setVisibleLines(20);
        this.certificateInput.addChangeHandler(event -> {
            setDirty(true);
            setButtonsEnabled(true);
        });

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            reset();
            setDirty(false);
            setButtonsEnabled(false);
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
                                        setButtonsEnabled(false);
                                        EntryClassUi.hideWaitModal();
                                    }
                                });
                    }
                });
            }
        });
    }

    private void reset() {
        setButtonsEnabled(false);
        this.storageAliasInput.setText("");
        this.certificateInput.setText("");
        this.groupStorageAliasForm.setValidationState(ValidationState.NONE);
        this.groupCertForm.setValidationState(ValidationState.NONE);
    }

    private void setButtonsEnabled(boolean state) {
        ServerCertsTabUi.this.apply.setEnabled(state);
        ServerCertsTabUi.this.reset.setEnabled(state);
    }
}
