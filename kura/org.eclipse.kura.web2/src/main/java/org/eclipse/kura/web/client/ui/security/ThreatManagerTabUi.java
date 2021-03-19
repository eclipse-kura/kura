/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Eurotech
 ******************************************************************************/
package org.eclipse.kura.web.client.ui.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.ui.cloudconnection.CloudConnectionsUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.FilterBuilder;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class ThreatManagerTabUi extends AbstractServicesUi implements Tab {

    private static IdsTabUiUiBinder uiBinder = GWT.create(IdsTabUiUiBinder.class);

    interface IdsTabUiUiBinder extends UiBinder<Widget, ThreatManagerTabUi> {
    }

    private static final Logger logger = Logger.getLogger(CloudConnectionsUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private static final String SERVICES_FILTER = FilterBuilder
            .of("objectClass=org.eclipse.kura.security.ThreatManagerService");

    private boolean dirty;
    private boolean initialized;
    private GwtConfigComponent originalConfig;
    private final List<GwtConfigComponent> configurations = new ArrayList<>();

    @UiField
    Button apply;
    @UiField
    Button reset;

    @UiField
    FieldSet fields;
    @UiField
    Form form;

    @UiField
    Modal notificationModal;
    @UiField
    ModalHeader notificationModalHeader;
    @UiField
    ModalBody notificationModalBody;
    @UiField
    Button cancelButton;
    @UiField
    Button applyButton;

    @UiField
    AlertDialog alertDialog;

    public ThreatManagerTabUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.initialized = false;

        this.apply.setText(MSGS.apply());
        this.apply.addClickHandler(event -> apply());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> this.alertDialog.show(MSGS.deviceConfigDirty(), this::refresh));

        this.apply.setEnabled(false);
        this.reset.setEnabled(false);
        
        logger.info("ready to init modal");

        initNotificationModal();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean b) {
        this.dirty = b;
        if (this.dirty && this.initialized) {
            this.apply.setEnabled(true);
            this.reset.setEnabled(true);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void clear() {
        // Not needed
    }

    private void apply() {
        if (isDirty()) {
            this.notificationModalBody.clear();
            this.notificationModalBody
            .add(new Span(MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName())));
            this.notificationModal.show();
            this.cancelButton.setFocus(true);
        }
    }

    private void initNotificationModal() {
        this.notificationModalHeader.setTitle(MSGS.confirm());

        this.cancelButton.setText(MSGS.noButton());
        this.cancelButton.addClickHandler(event -> this.notificationModal.hide());

        this.applyButton.setText(MSGS.yesButton());
        this.applyButton.addClickHandler(event -> {
            EntryClassUi.showWaitModal();
            try {
                getUpdatedConfiguration();
            } catch (Exception ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
                return;
            }
            for (GwtConfigComponent config : ThreatManagerTabUi.this.configurations) {
                config.setParameters(ThreatManagerTabUi.this.configurableComponent.getParameters());
            }
            RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context
                    .callback(token -> ThreatManagerTabUi.this.gwtComponentService.updateComponentConfigurations(token,
                            ThreatManagerTabUi.this.configurations, context.callback(data -> {
                                ThreatManagerTabUi.this.notificationModal.hide();
                                logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                ThreatManagerTabUi.this.apply.setEnabled(false);
                                ThreatManagerTabUi.this.reset.setEnabled(false);
                                setDirty(false);
                                ThreatManagerTabUi.this.originalConfig = ThreatManagerTabUi.this.configurableComponent;
                                EntryClassUi.hideWaitModal();
                            })))));
        });
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

    @Override
    protected void reset() {
        refresh();
    }

    @Override
    protected void renderForm() {
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
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        this.fields.add(formGroup);
    }

    @Override
    public void refresh() {
        RequestQueue.submit(context -> this.gwtXSRFService
                .generateSecurityToken(context.callback(token -> ThreatManagerTabUi.this.gwtComponentService
                        .findComponentConfigurations(token, SERVICES_FILTER, context.callback(data -> {
                            this.configurations.clear();
                            if (!data.isEmpty()) {
                                GwtConfigComponent firstConfig = data.get(0);
                                // Save the configuration of all registered services
                                this.configurations.addAll(data);
                                for (int index = 1; index < data.size(); index++) {
                                    firstConfig.getParameters().addAll(data.get(index).getParameters());
                                }
                                ThreatManagerTabUi.this.originalConfig = firstConfig;
                                restoreConfiguration(ThreatManagerTabUi.this.originalConfig);
                                ThreatManagerTabUi.this.fields.clear();

                                renderForm();

                                setDirty(false);
                                ThreatManagerTabUi.this.apply.setEnabled(false);
                                ThreatManagerTabUi.this.reset.setEnabled(false);
                            }
                        })))));

    }

}