/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.settings;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonType;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;

public class SnapshotSelectorActionButton {

    private final Button button;
    private final HandlerRegistration clickHandler;

    public SnapshotSelectorActionButton(String text, String styleName, ButtonType type, ClickHandler event) {
        this.button = new Button(text);
        this.button.addStyleName(styleName);
        this.button.setType(type);
        this.clickHandler = this.button.addClickHandler(event);
    }

    public Button getButton() {
        return this.button;
    }

    public void removeClickHandler() {
        this.clickHandler.removeHandler();
    }

    public void setButtonText(String text) {
        this.button.setText(text);
    }
}