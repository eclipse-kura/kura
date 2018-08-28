/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.cloudconnection;

import java.util.Iterator;
import java.util.logging.Level;

import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.request.Request;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.client.util.request.SuccessCallback;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class CloudConnectionConfigurationUi extends AbstractServicesUi {

    private static ServiceConfigurationUiUiBinder uiBinder = GWT.create(ServiceConfigurationUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private boolean dirty;
    private boolean initialized;

    interface ServiceConfigurationUiUiBinder extends UiBinder<Widget, CloudConnectionConfigurationUi> {
    }

    private GwtConfigComponent originalConfig;

    @UiField
    Button applyConnectionEdit;
    @UiField
    Button resetConnectionEdit;
    @UiField
    FieldSet connectionEditFields;
    @UiField
    Form connectionEditField;
    @UiField
    AlertDialog alertDialog;

    public CloudConnectionConfigurationUi(final GwtConfigComponent addedItem) {
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
            alertDialog.show(MSGS.deviceConfigDirty(), new AlertDialog.Listener() {

                @Override
                public void onConfirm() {
                    resetVisualization();
                }
            });
        }
    }

    @Override
    protected void renderForm() {
        this.connectionEditFields.clear();
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
                alertDialog.show(MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName()),
                        new AlertDialog.Listener() {

                            @Override
                            public void onConfirm() {
                                try {
                                    getUpdatedConfiguration();
                                } catch (Exception ex) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(ex);
                                    return;
                                }
                                RequestQueue.submit(new Request() {

                                    @Override
                                    public void run(final RequestContext context) {
                                        CloudConnectionConfigurationUi.this.gwtXSRFService.generateSecurityToken(
                                                context.callback(new SuccessCallback<GwtXSRFToken>() {

                                                    @Override
                                                    public void onSuccess(GwtXSRFToken token) {
                                                        CloudConnectionConfigurationUi.this.gwtComponentService
                                                                .updateComponentConfiguration(token,
                                                                        CloudConnectionConfigurationUi.this.configurableComponent,
                                                                        context.callback(new SuccessCallback<Void>() {

                                                                            @Override
                                                                            public void onSuccess(Void result) {
                                                                                logger.info(MSGS.info() + ": "
                                                                                        + MSGS.deviceConfigApplied());
                                                                                CloudConnectionConfigurationUi.this.applyConnectionEdit
                                                                                        .setEnabled(false);
                                                                                CloudConnectionConfigurationUi.this.resetConnectionEdit
                                                                                        .setEnabled(false);
                                                                                setDirty(false);
                                                                                originalConfig = CloudConnectionConfigurationUi.this.configurableComponent;
                                                                                EntryClassUi.hideWaitModal();
                                                                            }
                                                                        }));

                                                    }
                                                }));
                                    }
                                });
                            }

                        });
            }
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            alertDialog.show(MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ALERT, null);
        }
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
        return this.configurableComponent;
    }

    protected void resetVisualization() {
        restoreConfiguration(this.originalConfig);
        renderForm();
        this.applyConnectionEdit.setEnabled(false);
        this.resetConnectionEdit.setEnabled(false);
        setDirty(false);
    }
}
