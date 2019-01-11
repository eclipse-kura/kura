/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
        hideButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });
    }

    public void setContent(String content) {
        helpContent.clear();
        helpContent.add(new Span(content));
    }

    public void show() {
        helpPanel.addStyleName(HELP_PANEL_VISIBLE_CLASS_NAME);
    }

    public void hide() {
        helpPanel.removeStyleName(HELP_PANEL_VISIBLE_CLASS_NAME);
    }
}
