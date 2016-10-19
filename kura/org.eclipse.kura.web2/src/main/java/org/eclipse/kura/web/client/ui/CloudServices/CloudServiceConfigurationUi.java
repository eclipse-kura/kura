/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.Iterator;
import java.util.logging.Level;

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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class CloudServiceConfigurationUi extends AbstractServicesUi {

    private static ServiceConfigurationUiUiBinder uiBinder = GWT.create(ServiceConfigurationUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private boolean dirty, initialized;

    interface ServiceConfigurationUiUiBinder extends UiBinder<Widget, CloudServiceConfigurationUi> {
    }

    private Modal modal;
    private final GwtConfigComponent originalConfig;

    @UiField
    Button applyConnectionEdit;
    @UiField
    Button resetConnectionEdit;
    @UiField
    FieldSet connectionEditFields;
    @UiField
    Form connectionEditField;
    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text incompleteFieldsText;

    public CloudServiceConfigurationUi(final GwtConfigComponent addedItem) {
        initWidget(uiBinder.createAndBindUi(this));
        this.initialized = false;
        this.originalConfig = addedItem;
        restoreConfiguration(this.originalConfig);
        this.connectionEditFields.clear();

        this.applyConnectionEdit.setText(MSGS.apply());
        this.applyConnectionEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        this.resetConnectionEdit.setText(MSGS.reset());
        this.resetConnectionEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });

        setDirty(false);
        this.applyConnectionEdit.setEnabled(false);
        this.resetConnectionEdit.setEnabled(false);

        initInvalidDataModal();
    }

    @Override
    protected void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty && this.initialized) {
            this.applyConnectionEdit.setEnabled(true);
            this.resetConnectionEdit.setEnabled(true);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    protected void reset() {
        if (isDirty()) {
            // Modal
            showDirtyModal();
        }        // end is dirty
    }

    @Override
    protected void renderForm() {
        this.connectionEditFields.clear();
        for (GwtConfigParameter param : this.m_configurableComponent.getParameters()) {
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
        this.connectionEditFields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        this.connectionEditFields.add(formGroup);
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.connectionEditFields.add(formGroup);
    }

    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        this.connectionEditFields.add(formGroup);
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
                body.add(new Span(MSGS.deviceConfigConfirmation(this.m_configurableComponent.getComponentName())));
                this.modal.add(body);

                ModalFooter footer = new ModalFooter();
                ButtonGroup group = new ButtonGroup();
                Button yes = new Button();
                yes.setText(MSGS.yesButton());
                yes.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        EntryClassUi.showWaitModal();
                        try {
                            getUpdatedConfiguration();
                        } catch (Exception ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                            return;
                        }
                        CloudServiceConfigurationUi.this.gwtXSRFService
                                .generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                CloudServiceConfigurationUi.this.gwtComponentService.updateComponentConfiguration(token,
                                        CloudServiceConfigurationUi.this.m_configurableComponent,
                                        new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                        errorLogger.log(
                                                Level.SEVERE, caught.getLocalizedMessage() != null
                                                        ? caught.getLocalizedMessage() : caught.getClass().getName(),
                                                caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        CloudServiceConfigurationUi.this.modal.hide();
                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                        CloudServiceConfigurationUi.this.applyConnectionEdit.setEnabled(false);
                                        CloudServiceConfigurationUi.this.resetConnectionEdit.setEnabled(false);
                                        setDirty(false);
                                        EntryClassUi.hideWaitModal();
                                    }
                                });

                            }
                        });
                    }
                });
                group.add(yes);
                Button no = new Button();
                no.setText(MSGS.noButton());
                no.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        CloudServiceConfigurationUi.this.modal.hide();
                    }
                });
                group.add(no);
                footer.add(group);
                this.modal.add(footer);
                this.modal.show();

                // ----

            }                         // end isDirty()
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            this.incompleteFieldsModal.show();
        }                         // end else isValid
    }

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.connectionEditFields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return this.m_configurableComponent;
    }

    private void showDirtyModal() {
        this.modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSGS.confirm());
        this.modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSGS.deviceConfigDirty()));
        this.modal.add(body);

        ModalFooter footer = new ModalFooter();
        ButtonGroup group = new ButtonGroup();
        Button yes = new Button();
        yes.setText(MSGS.yesButton());
        yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                CloudServiceConfigurationUi.this.modal.hide();
                resetVisualization();
            }
        });
        group.add(yes);
        Button no = new Button();
        no.setText(MSGS.noButton());
        no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                CloudServiceConfigurationUi.this.modal.hide();
            }
        });
        group.add(no);
        footer.add(group);
        this.modal.add(footer);
        this.modal.show();
    }

    protected void resetVisualization() {
        restoreConfiguration(this.originalConfig);
        renderForm();
        this.applyConnectionEdit.setEnabled(false);
        this.resetConnectionEdit.setEnabled(false);
        setDirty(false);
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }
}
