/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui;

import java.util.Iterator;

import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormGroup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ConfigurableComponentUi extends AbstractServicesUi implements HasConfiguration {

    interface GenericWireComponentUiUiBinder extends UiBinder<Widget, ConfigurableComponentUi> {
    }

    private static GenericWireComponentUiUiBinder uiBinder = GWT.create(GenericWireComponentUiUiBinder.class);

    private boolean dirty;

    private HasConfiguration.Listener listener;

    @UiField
    FieldSet fields;
    @UiField
    Label componentDescription;

    public ConfigurableComponentUi(final GwtConfigComponent originalConfig) {
        initWidget(uiBinder.createAndBindUi(this));
        setConfiguration(originalConfig);
    }

    public void setConfiguration(GwtConfigComponent originalConfig) {
        final String description = originalConfig.getComponentDescription();
        this.componentDescription.setText(description != null ? description : "");
        restoreConfiguration(originalConfig);
        this.fields.clear();

        renderForm();
        setDirty(false);
    }

    @Override
    public void setDirty(final boolean flag) {
        boolean isDirtyStateChanged = flag != this.dirty;
        this.dirty = flag;
        if (this.listener != null) {
            if (isDirtyStateChanged) {
                this.listener.onDirtyStateChanged(this);
            }
            if (isValid()) {
                this.listener.onConfigurationChanged(this);
            }
        }
    }

    @Override
    protected void reset() {
        // Nothing to do
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

    @Override
    public void setListener(HasConfiguration.Listener listener) {
        this.listener = listener;
        listener.onConfigurationChanged(this);
    }

    protected GwtConfigComponent getUpdatedConfiguration() {
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
    public GwtConfigComponent getConfiguration() {
        final GwtConfigComponent result = getUpdatedConfiguration();
        result.getProperties().putAll(this.configurableComponent.getProperties());
        return result;
    }

    @Override
    public void clearDirtyState() {
        this.dirty = false;
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void markAsDirty() {
        setDirty(true);
    }
}
