/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.shared.model.GwtModemInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class TabModemGpsUi extends Composite implements NetworkTab {

    private static TabModemGpsUiUiBinder uiBinder = GWT.create(TabModemGpsUiUiBinder.class);

    interface TabModemGpsUiUiBinder extends UiBinder<Widget, TabModemGpsUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    GwtSession session;
    boolean dirty;
    GwtModemInterfaceConfig selectedModemIfConfig;
    boolean formInitialized;

    @UiField
    FormLabel labelGps;

    @UiField
    InlineRadio radio1;
    @UiField
    InlineRadio radio2;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    FieldSet field;

    @UiField
    HelpButton gpsHelp;

    public TabModemGpsUi(GwtSession currentSession) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        initForm();

        this.gpsHelp.setHelpText(MSGS.netModemToolTipEnableGps());
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        this.dirty = true;
        if (config instanceof GwtModemInterfaceConfig) {
            this.selectedModemIfConfig = (GwtModemInterfaceConfig) config;
        }
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtModemInterfaceConfig updatedModemNetIf = (GwtModemInterfaceConfig) updatedNetIf;
        if (this.formInitialized) {
            updatedModemNetIf.setGpsEnabled(this.radio1.getValue());
        } else {
            // initForm hasn't been called yet
            updatedModemNetIf.setGpsEnabled(this.selectedModemIfConfig.isGpsEnabled());
        }
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            if (this.selectedModemIfConfig == null) {
                reset();
            } else {
                update();
            }
        }
    }

    // ----Private Methods----
    private void initForm() {
        // ENABLE GPS
        this.labelGps.setText(MSGS.netModemEnableGps());
        this.radio1.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemGpsUi.this.radio1.isEnabled()) {
                    TabModemGpsUi.this.helpText.clear();
                    TabModemGpsUi.this.helpText.add(new Span(MSGS.netModemToolTipEnableGps()));
                }
            }
        });
        this.radio1.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.radio2.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                if (TabModemGpsUi.this.radio2.isEnabled()) {
                    TabModemGpsUi.this.helpText.clear();
                    TabModemGpsUi.this.helpText.add(new Span(MSGS.netModemToolTipEnableGps()));
                }
            }
        });
        this.radio2.addMouseOutHandler(new MouseOutHandler() {

            @Override
            public void onMouseOut(MouseOutEvent event) {
                resetHelp();
            }
        });
        this.radio1.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                TabModemGpsUi.this.dirty = true;
            }
        });
        this.radio2.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                TabModemGpsUi.this.dirty = true;
            }
        });

        this.helpTitle.setText(MSGS.netHelpTitle());
        this.radio1.setText(MSGS.trueLabel());
        this.radio2.setText(MSGS.falseLabel());
        this.radio1.setValue(true);
        this.radio2.setValue(false);
        this.formInitialized = true;
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void update() {
        if (this.selectedModemIfConfig != null) {
            if (this.selectedModemIfConfig.isGpsEnabled()) {
                this.radio1.setValue(true);
                this.radio2.setValue(false);
            } else {
                this.radio1.setValue(false);
                this.radio2.setValue(true);
            }
        }
        refreshForm();
    }

    private void refreshForm() {
        if (this.selectedModemIfConfig.isGpsSupported()) {
            this.radio1.setEnabled(true);
            this.radio2.setEnabled(true);
        } else {
            this.radio1.setEnabled(false);
            this.radio2.setEnabled(false);
        }
    }

    private void reset() {
        this.radio1.setValue(true);
        this.radio2.setValue(false);
        /*
         * radio1.setActive(false);
         * radio2.setActive(true);
         */
        update();
    }
}
