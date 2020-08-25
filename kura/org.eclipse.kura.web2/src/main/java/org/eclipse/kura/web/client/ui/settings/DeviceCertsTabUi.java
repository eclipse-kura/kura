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
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TextArea;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
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
        return deviceSslCertsForm.validate();
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
        title.append(MSGS.settingsMAuthDescription1());
        title.append(" ");
        title.append(MSGS.settingsMAuthDescription2());
        title.append("</p>");
        this.description.add(new Span(title.toString()));

        this.storageAliasLabel.setText(MSGS.settingsStorageAliasLabel());
        this.storageAliasInput.addChangeHandler(event -> {
            setDirty(true);
            setButtonsEnabled(true);
        });

        this.privateKeyLabel.setText(MSGS.settingsPrivateCertLabel());
        this.privateKeyInput.setVisibleLines(20);
        this.privateKeyInput.addChangeHandler(event -> {
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
        this.reset.addClickHandler(event -> reset());

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
        if (isDirty()) {
            // Modal
            Modal modal = new Modal();
            modal.setClosable(false);
            modal.setFade(true);
            modal.setDataKeyboard(true);
            modal.setDataBackdrop(ModalBackdrop.STATIC);

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.confirm());
            modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            modal.add(body);

            ModalFooter footer = new ModalFooter();
            ButtonGroup group = new ButtonGroup();
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addStyleName("fa fa-times");
            no.addClickHandler(event -> modal.hide());
            group.add(no);
            Button yes = new Button();
            yes.setText(MSGS.yesButton());
            yes.addStyleName("fa fa-check");
            yes.addClickHandler(event -> {
                modal.hide();
                setButtonsEnabled(false);
                this.storageAliasInput.setText("");
                this.privateKeyInput.setText("");
                this.certificateInput.setText("");
                this.groupStorageAliasForm.setValidationState(ValidationState.NONE);
                this.groupCertForm.setValidationState(ValidationState.NONE);
                this.groupCertForm.setValidationState(ValidationState.NONE);
                setDirty(false);
            });
            group.add(yes);
            footer.add(group);
            modal.add(footer);
            modal.show();
            no.setFocus(true);
        }
    }

    private void setButtonsEnabled(boolean state) {
        DeviceCertsTabUi.this.apply.setEnabled(state);
        DeviceCertsTabUi.this.reset.setEnabled(state);
    }
}
