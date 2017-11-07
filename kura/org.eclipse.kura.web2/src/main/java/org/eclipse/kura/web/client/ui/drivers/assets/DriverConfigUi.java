/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class DriverConfigUi extends AbstractServicesUi {

    private static DriverConfigUiUiBinder uiBinder = GWT.create(DriverConfigUiUiBinder.class);

    interface DriverConfigUiUiBinder extends UiBinder<Widget, DriverConfigUi> {
    }

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private boolean initialized;
    private GwtConfigComponent originalConfig;
    private boolean dirty;

    private Modal modal;

    @UiField
    Panel configurationPanel;
    @UiField
    PanelHeader contentPanelHeader;
    
    @UiField
    TabListItem tab1NavTab;

    @UiField
    Button applyConfigurationEdit;
    @UiField
    Button resetConfigurationEdit;

    @UiField
    FieldSet configurationEditFields;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Text incompleteFieldsText;

    public DriverConfigUi(final GwtConfigComponent addedItem) {
        initWidget(uiBinder.createAndBindUi(this));

        this.initialized = false;
        this.originalConfig = addedItem;
        restoreConfiguration(this.originalConfig);
        setDirty(false);
        this.configurationPanel.setVisible(true);
        this.contentPanelHeader.setText(MSGS.driverLabel(addedItem.getComponentName()));
        this.applyConfigurationEdit.setEnabled(false);
        this.resetConfigurationEdit.setEnabled(false);

        initInvalidDataModal();
        initButtons();
        initTabs();

        renderForm();
    }

    private void initButtons() {
        this.applyConfigurationEdit.setText(MSGS.apply());
        this.applyConfigurationEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        this.resetConfigurationEdit.setText(MSGS.reset());
        this.resetConfigurationEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });

        this.applyConfigurationEdit.setEnabled(false);
        this.resetConfigurationEdit.setEnabled(false);

    }
    
    private void initTabs(){
        tab1NavTab.setText(MSGS.driverConfig());
    }

    private void apply() {
        if (isValid()) {
            if (isDirty()) {
                // TODO: maybe this can be declared in the xml?
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
                no.addClickHandler(new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        DriverConfigUi.this.modal.hide();
                    }
                });
                group.add(no);

                Button yes = new Button();
                yes.setText(MSGS.yesButton());
                yes.addStyleName("fa fa-check");
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
                        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                gwtComponentService.updateComponentConfiguration(token,
                                        DriverConfigUi.this.configurableComponent, new AsyncCallback<Void>() {

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
                                        DriverConfigUi.this.modal.hide();
                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                        DriverConfigUi.this.applyConfigurationEdit.setEnabled(false);
                                        DriverConfigUi.this.resetConfigurationEdit.setEnabled(false);
                                        setDirty(false);
                                        DriverConfigUi.this.originalConfig = DriverConfigUi.this.configurableComponent;
                                        EntryClassUi.hideWaitModal();
                                    }
                                });

                            }
                        });
                    }
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

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = this.configurationEditFields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return this.configurableComponent;
    }

    @Override
    protected void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.dirty && this.initialized) {
            this.applyConfigurationEdit.setEnabled(true);
            this.resetConfigurationEdit.setEnabled(true);
        }
    }

    @Override
    protected boolean isDirty() {
        return this.dirty;
    }

    @Override
    protected void reset() {
        if (isDirty()) {
            showDirtyModal();
        }
    }

    private void showDirtyModal() {
        // TODO: Maybe this can be declared in the xml?
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
        no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DriverConfigUi.this.modal.hide();
            }
        });
        group.add(no);

        Button yes = new Button();
        yes.setText(MSGS.yesButton());
        yes.addStyleName("fa fa-check");
        yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DriverConfigUi.this.modal.hide();
                resetVisualization();
            }
        });
        group.add(yes);
        footer.add(group);
        this.modal.add(footer);
        this.modal.show();
        no.setFocus(true);
    }

    protected void resetVisualization() {
        restoreConfiguration(this.originalConfig);
        renderForm();
        this.applyConfigurationEdit.setEnabled(false);
        this.resetConfigurationEdit.setEnabled(false);
        setDirty(false);
    }

    @Override
    protected void renderForm() {
        if (this.configurableComponent == null) {
            return;
        }

        this.configurationEditFields.clear();
        for (GwtConfigParameter param : this.configurableComponent.getParameters()) {
            if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                FormGroup formGroup = new FormGroup();
                renderConfigParameter(param, true, formGroup);
            } else {
                renderMultiFieldConfigParameter(param);
            }
        }
        gwtAssetService.getAssetInstancesByDriverPid(this.configurableComponent.getComponentId(),
                new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final List<String> result) {
                        DriverConfigUi.this.initialized = true;
                    }
                });
    }

    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        this.configurationEditFields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        this.configurationEditFields.add(formGroup);
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.configurationEditFields.add(formGroup);
    }

    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        this.configurationEditFields.add(formGroup);
    }

    private void initInvalidDataModal() {
        this.incompleteFieldsModal.setTitle(MSGS.warning());
        this.incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }
}
