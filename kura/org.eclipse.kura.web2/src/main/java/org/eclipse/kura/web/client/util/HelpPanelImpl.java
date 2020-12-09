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
package org.eclipse.kura.web.client.util;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

class HelpPanelImpl extends Composite {

    public static final String HELP_PANEL_VISIBLE_CLASS_NAME = "help-panel-visible";

    @UiField
    HTMLPanel helpPanel;

    @UiField
    Button hideButton;

    @UiField
    HTMLPanel helpContent;

    private static HelpPanelImplUiBinder uiBinder = GWT.create(HelpPanelImplUiBinder.class);

    interface HelpPanelImplUiBinder extends UiBinder<Widget, HelpPanelImpl> {
    }

    public HelpPanelImpl() {
        initWidget(uiBinder.createAndBindUi(this));
        this.hideButton.addClickHandler(event -> hide());
    }

    public void setContent(String content) {
        this.helpContent.clear();
        this.helpContent.add(new Span(content));
    }

    public void show() {
        this.helpPanel.addStyleName(HELP_PANEL_VISIBLE_CLASS_NAME);
    }

    public void hide() {
        this.helpPanel.removeStyleName(HELP_PANEL_VISIBLE_CLASS_NAME);
    }
}
