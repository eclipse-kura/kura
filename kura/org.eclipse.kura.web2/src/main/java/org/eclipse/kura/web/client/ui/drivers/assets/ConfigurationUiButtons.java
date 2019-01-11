/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
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

import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.gwtbootstrap3.client.ui.Button;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ConfigurationUiButtons extends Composite implements HasConfiguration.Listener {

    private static ConfigurationUiButtonsUiBinder uiBinder = GWT.create(ConfigurationUiButtonsUiBinder.class);

    interface ConfigurationUiButtonsUiBinder extends UiBinder<Widget, ConfigurationUiButtons> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    Button btnApply;
    @UiField
    Button btnReset;
    @UiField
    AlertDialog confirmDialog;

    private Listener listener;

    public ConfigurationUiButtons(final HasConfiguration target) {
        initWidget(uiBinder.createAndBindUi(this));

        this.btnApply.setEnabled(target.isDirty());
        this.btnReset.setEnabled(target.isDirty());
        target.setListener(this);

        this.btnApply.addClickHandler(event -> {
            if (listener == null) {
                return;
            }
            if (!target.isValid()) {
                confirmDialog.show(MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ALERT, null);
                return;
            }
            confirmDialog.show(MSGS.deviceConfigConfirmationNoName(), () -> listener.onApply());
        });
        this.btnReset.addClickHandler(event -> {
            if (listener == null) {
                return;
            }
            confirmDialog.show(MSGS.deviceConfigDirty(), () -> listener.onReset());
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onConfigurationChanged(HasConfiguration hasConfiguration) {
        //Not needed
    }

    @Override
    public void onDirtyStateChanged(HasConfiguration hasConfiguration) {
        boolean isDirty = hasConfiguration.isDirty();
        btnApply.setEnabled(isDirty);
        btnReset.setEnabled(isDirty);
    }

    public interface Listener {

        public void onApply();

        public void onReset();
    }
}
