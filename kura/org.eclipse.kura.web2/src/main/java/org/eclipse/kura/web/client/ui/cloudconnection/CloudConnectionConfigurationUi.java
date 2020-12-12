/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.cloudconnection;

import java.util.Iterator;
import java.util.logging.Level;

import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class CloudConnectionConfigurationUi extends AbstractServicesUi {

    private static ServiceConfigurationUiUiBinder uiBinder = GWT.create(ServiceConfigurationUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudConnectionServiceAsync gwtCloudService = GWT.create(GwtCloudConnectionService.class);

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
        this.applyConnectionEdit.addClickHandler(e -> apply());

        this.resetConnectionEdit.setText(MSGS.reset());
        this.resetConnectionEdit.addClickHandler(e -> reset());

        setDirty(false);
        this.applyConnectionEdit.setEnabled(false);
        this.resetConnectionEdit.setEnabled(false);
    }

    @Override
    public void setDirty(boolean flag) {
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
    public void reset() {
        if (isDirty()) {
            this.alertDialog.show(MSGS.deviceConfigDirty(), this::resetVisualization);
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
                this.alertDialog.show(MSGS.deviceConfigConfirmation(this.configurableComponent.getComponentName()),
                        () -> {
                            try {
                                getUpdatedConfiguration();
                            } catch (Exception ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                                return;
                            }
                            RequestQueue.submit(context -> CloudConnectionConfigurationUi.this.gwtXSRFService
                                    .generateSecurityToken(context
                                            .callback(token -> CloudConnectionConfigurationUi.this.gwtCloudService
                                                    .updateStackComponentConfiguration(token,
                                                            CloudConnectionConfigurationUi.this.configurableComponent,
                                                            context.callback(result -> {
                                                                logger.info(MSGS.info() + ": "
                                                                        + MSGS.deviceConfigApplied());
                                                                CloudConnectionConfigurationUi.this.applyConnectionEdit
                                                                        .setEnabled(false);
                                                                CloudConnectionConfigurationUi.this.resetConnectionEdit
                                                                        .setEnabled(false);
                                                                setDirty(false);
                                                                this.originalConfig = CloudConnectionConfigurationUi.this.configurableComponent;
                                                                EntryClassUi.hideWaitModal();
                                                            }))

                                            )));
                        }

                );
            }
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            this.alertDialog.show(MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ALERT,
                    (ConfirmListener) null);
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
