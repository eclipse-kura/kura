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
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.security;

import java.util.Date;
import java.util.Map.Entry;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtTamperStatus;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Legend;
import org.gwtbootstrap3.client.ui.TextArea;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TamperDetectionEntry extends Composite {

    private static final String TIMESTAMP = "timestamp";

    private static TamperDetectionEntryUiBinder uiBinder = GWT.create(TamperDetectionEntryUiBinder.class);

    interface TamperDetectionEntryUiBinder extends UiBinder<Widget, TamperDetectionEntry> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    Legend name;
    @UiField
    FormControlStatic state;
    @UiField
    FieldSet tamperDetectionFields;

    public TamperDetectionEntry(final GwtTamperStatus status) {
        initWidget(uiBinder.createAndBindUi(this));
        this.name.setText(status.getDisplayName());
        this.state.setText(
                status.isTampered() ? MSGS.securityTamperStateTampered() : MSGS.securityTamperStateNotTampered());
        for (final Entry<String, String> e : status.getProperties().entrySet()) {

            final FormGroup group = new FormGroup();

            final FormLabel label = new FormLabel();
            label.setText(getPropertyName(e.getKey()));
            group.add(label);

            final String propertyValue = getPropertyValue(e.getKey(), e.getValue());

            if (propertyValue.contains("\n")) {
                final TextArea widget = new TextArea();
                widget.setReadOnly(true);
                widget.setValue(propertyValue);
                group.add(widget);
            } else {
                final FormControlStatic widget = new FormControlStatic();
                widget.setText(propertyValue);
                group.add(widget);
            }

            this.tamperDetectionFields.add(group);
        }
    }

    private final String getPropertyName(final String key) {
        if (key.equals(TIMESTAMP)) {
            return MSGS.securityTamperDetectionTamperTimestamp();
        } else {
            return key;
        }
    }

    private final String getPropertyValue(final String key, final String value) {
        if (key.equals(TIMESTAMP)) {
            try {
                return new Date(Long.parseLong(value)).toString();
            } catch (final Exception ex) {
                return value;
            }
        } else {
            return value;
        }
    }
}
