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

package org.eclipse.kura.web.client.ui;

import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class DropdownNotification extends Composite {

    private static DropdownNotificationUiBinder uiBinder = GWT.create(DropdownNotificationUiBinder.class);

    interface DropdownNotificationUiBinder extends UiBinder<Widget, DropdownNotification> {
    }

    private static final int DEFAULT_DISMISS_TIME_MS = 8000;

    @UiField
    Row dropdownNotification;
    @UiField
    Text dropdownNotificationMessage;

    public DropdownNotification() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void show(String message) {
        show(message, DEFAULT_DISMISS_TIME_MS);
    }

    public void show(String message, int dismissAfterMs) {
        this.dropdownNotificationMessage.setText(message);
        this.dropdownNotification.getElement().setAttribute("style", "max-height:120px");

        new Timer() {

            @Override
            public void run() {
                DropdownNotification.this.dropdownNotification.getElement().setAttribute("style", "max-height:0px");
            }

        }.schedule(dismissAfterMs);
    }
}
