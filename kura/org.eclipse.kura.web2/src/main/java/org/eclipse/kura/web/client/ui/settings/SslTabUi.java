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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class SslTabUi extends AbstractServicesUi {

    private static final SslTabUiUiBinder uiBinder = GWT.create(SslTabUiUiBinder.class);

    interface SslTabUiUiBinder extends UiBinder<Widget, SslTabUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private boolean dirty;
    private boolean initialized;
    private GwtConfigComponent originalConfig;

    private Modal modal;

    @UiField
    Button apply;
    @UiField
    Button reset;
    @UiField
    FieldSet fields;
    @UiField
    Form form;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text incompleteFieldsText;

    public SslTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.initialized = false;

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> apply());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> reset());
    }

    public void load() {
        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                SslTabUi.this.gwtComponentService.findFilteredComponentConfiguration(token,
                        "org.eclipse.kura.ssl.SslManagerService", new AsyncCallback<List<GwtConfigComponent>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(List<GwtConfigComponent> result) {
                                for (GwtConfigComponent config : result) {
                                    SslTabUi.this.originalConfig = config;

                                    restoreConfiguration(SslTabUi.this.originalConfig);
                                    SslTabUi.this.fields.clear();

                                    renderForm();
                                    initInvalidDataModal();

                                    setDirty(false);
                                    SslTabUi.this.apply.setEnabled(false);
                                    SslTabUi.this.reset.setEnabled(false);
                                }
                            }
                        });
            }
        });
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty && this.initialized) {
            this.apply.setEnabled(true);
            this.reset.setEnabled(true);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    private void apply() {
        if (isValid()) {
            if (isDirty()) {
                // TODO ask for confirmation first
                this.modal = new Modal();

                ModalHeader header = new ModalHeader();
                header.setTitle(MSGS.confirm());
                this.modal.add(header);

                ModalBody body = new ModalBody();
                body.add(new Span(MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName())));
                this.modal.add(body);

                ModalFooter footer = new ModalFooter();
                ButtonGroup group = new ButtonGroup();
                Button no = new Button();
                no.setText(MSGS.noButton());
                no.addStyleName("fa fa-times");
                no.addClickHandler(event -> SslTabUi.this.modal.hide());

                group.add(no);
                Button yes = new Button();
                yes.setText(MSGS.yesButton());
                yes.addStyleName("fa fa-check");
                yes.addClickHandler(event -> {
                    EntryClassUi.showWaitModal();
                    try {
                        getUpdatedConfiguration();
                    } catch (Exception ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                        return;
                    }
                    SslTabUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            SslTabUi.this.gwtComponentService.updateComponentConfiguration(token,
                                    SslTabUi.this.configurableComponent, new AsyncCallback<Void>() {

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            EntryClassUi.hideWaitModal();
                                            FailureHandler.handle(caught);
                                            errorLogger.log(Level.SEVERE,
                                                    caught.getLocalizedMessage() != null ? caught.getLocalizedMessage()
                                                            : caught.getClass().getName(),
                                                    caught);
                                        }

                                        @Override
                                        public void onSuccess(Void result) {
                                            SslTabUi.this.modal.hide();
                                            logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                            SslTabUi.this.apply.setEnabled(false);
                                            SslTabUi.this.reset.setEnabled(false);
                                            setDirty(false);
                                            SslTabUi.this.originalConfig = SslTabUi.this.configurableComponent;
                                            EntryClassUi.hideWaitModal();
                                        }
                                    });

                        }
                    });
                });
                group.add(yes);
                footer.add(group);
                this.modal.add(footer);
                this.modal.show();
                no.setFocus(true);
            }
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            this.incompleteFieldsModal.show();
        }
    }

    @Override
    public void reset() {
        if (isDirty()) {
            // Modal
            this.modal = new Modal();

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.confirm());
            this.modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            this.modal.add(body);

            ModalFooter footer = new ModalFooter();
            ButtonGroup group = new ButtonGroup();
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addStyleName("fa fa-times");
            no.addClickHandler(event -> SslTabUi.this.modal.hide());
            group.add(no);
            Button yes = new Button();
            yes.setText(MSGS.yesButton());
            yes.addStyleName("fa fa-check");
            yes.addClickHandler(event -> {
                SslTabUi.this.modal.hide();
                restoreConfiguration(SslTabUi.this.originalConfig);
                renderForm();
                SslTabUi.this.apply.setEnabled(false);
                SslTabUi.this.reset.setEnabled(false);
                setDirty(false);
            });
            group.add(yes);
            footer.add(group);
            this.modal.add(footer);
            this.modal.show();
            no.setFocus(true);
        }
    }

    @Override
    public void renderForm() {
        this.fields.clear();
        for (GwtConfigParameter param : this.configurableComponent.getParameters()) {
            if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                FormGroup formGroup = new FormGroup();
                renderConfigParameter(param, true, formGroup);
            } else {
                renderMultiFieldConfigParameter(param);
            }
        }
        this.initialized = true;
    }

    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return this.configurableComponent;
    }
}
