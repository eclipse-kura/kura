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
import org.gwtbootstrap3.client.ui.constants.DeviceSize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class HelpButton extends Composite {

    @UiField
    Button helpButton;

    private String helpText;
    private HelpTextProvider helpTextProvider;

    private static HelpButtonUiBinder uiBinder = GWT.create(HelpButtonUiBinder.class);

    interface HelpButtonUiBinder extends UiBinder<Widget, HelpButton> {
    }

    public HelpButton() {
        initWidget(uiBinder.createAndBindUi(this));
        this.helpButton.addClickHandler(event -> {
            if (HelpButton.this.helpTextProvider != null) {
                HelpPanel.show(HelpButton.this.helpTextProvider.getHelpText());
            } else {
                HelpPanel.show(HelpButton.this.helpText);
            }
        });
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public void setHelpTextProvider(HelpTextProvider provider) {
        this.helpTextProvider = provider;
    }

    public void updateHelpText() {
        if (this.helpTextProvider != null) {
            HelpPanel.setHelpText(this.helpTextProvider.getHelpText());
        } else {
            HelpPanel.setHelpText(this.helpText);
        }
    }

    public interface HelpTextProvider {

        public String getHelpText();
    }

    public void setVisibleOn(DeviceSize deviceSize) {
        this.helpButton.setVisibleOn(deviceSize);
    }
}
