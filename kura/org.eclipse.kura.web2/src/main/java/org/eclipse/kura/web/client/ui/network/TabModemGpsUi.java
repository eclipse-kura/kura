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

    private final GwtSession session;
    private final NetworkTabsUi tabs;
    private boolean dirty;
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

    public TabModemGpsUi(GwtSession currentSession, NetworkTabsUi tabs) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tabs = tabs;
        initForm();

        this.gpsHelp.setHelpText(MSGS.netModemToolTipEnableGps());
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.tabs.getButtons() != null) {
            this.tabs.getButtons().setApplyButtonDirty(flag);
        }
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

    @Override
    public void clear() {
        // Not needed
    }

    // ----Private Methods----
    private void initForm() {
        // ENABLE GPS
        this.labelGps.setText(MSGS.netModemEnableGps());
        this.radio1.addMouseOverHandler(event -> {
            if (TabModemGpsUi.this.radio1.isEnabled()) {
                TabModemGpsUi.this.helpText.clear();
                TabModemGpsUi.this.helpText.add(new Span(MSGS.netModemToolTipEnableGps()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio2.addMouseOverHandler(event -> {
            if (TabModemGpsUi.this.radio2.isEnabled()) {
                TabModemGpsUi.this.helpText.clear();
                TabModemGpsUi.this.helpText.add(new Span(MSGS.netModemToolTipEnableGps()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());
        this.radio1.addValueChangeHandler(event -> setDirty(true));
        this.radio2.addValueChangeHandler(event -> setDirty(true));

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
        update();
    }
}
