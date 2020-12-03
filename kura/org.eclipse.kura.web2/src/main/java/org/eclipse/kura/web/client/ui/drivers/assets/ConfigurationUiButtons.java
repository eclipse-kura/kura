/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.drivers.assets;

import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
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
            if (this.listener == null) {
                return;
            }
            if (!target.isValid()) {
                this.confirmDialog.show(MSGS.formWithErrorsOrIncomplete(), AlertDialog.Severity.ALERT,
                        (ConfirmListener) null);
                return;
            }
            this.confirmDialog.show(MSGS.deviceConfigConfirmationNoName(), () -> this.listener.onApply());
        });
        this.btnReset.addClickHandler(event -> {
            if (this.listener == null) {
                return;
            }
            this.confirmDialog.show(MSGS.deviceConfigDirty(), () -> this.listener.onReset());
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onConfigurationChanged(HasConfiguration hasConfiguration) {
        // Not needed
    }

    @Override
    public void onDirtyStateChanged(HasConfiguration hasConfiguration) {
        boolean isDirty = hasConfiguration.isDirty();
        this.btnApply.setEnabled(isDirty);
        this.btnReset.setEnabled(isDirty);
    }

    public interface Listener {

        public void onApply();

        public void onReset();
    }
}
