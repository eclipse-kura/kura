/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
import org.gwtbootstrap3.client.ui.FormControlStatic;
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

public class TabModemAntennaUi extends Composite implements NetworkTab {

    private static TabModemAntennaUiUiBinder uiBinder = GWT.create(TabModemAntennaUiUiBinder.class);

    interface TabModemAntennaUiUiBinder extends UiBinder<Widget, TabModemAntennaUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSession session;
    private final NetworkTabsUi tabs;
    boolean dirty;
    GwtModemInterfaceConfig selectedModemIfConfig;
    boolean formInitialized;
    int tours = 0;

    @UiField
    FormLabel labelAntenna;
    @UiField
    FormLabel labelRegistration;
    @UiField
    FormLabel labelIMSI;
    @UiField
    FormLabel labelICCID;
    @UiField
    FormLabel labelPLMNID;
    @UiField
    FormLabel labelNetwork;
    @UiField
    FormLabel labelRadio;
    @UiField
    FormLabel labelBand;
    @UiField
    FormLabel labelLAC;
    @UiField
    FormLabel labelCI;

    @UiField
    FormControlStatic registration;
    @UiField
    FormControlStatic imsi;
    @UiField
    FormControlStatic iccid;
    @UiField
    FormControlStatic plmnid;
    @UiField
    FormControlStatic network;
    @UiField
    FormControlStatic radio;
    @UiField
    FormControlStatic band;
    @UiField
    FormControlStatic lac;
    @UiField
    FormControlStatic ci;
    @UiField
    InlineRadio radio2;
    @UiField
    InlineRadio radio1;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    FieldSet field;

    @UiField
    HelpButton antennaHelp;

    public TabModemAntennaUi(GwtSession currentSession, NetworkTabsUi tabs) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tabs = tabs;
        initForm();

        this.antennaHelp.setHelpText(MSGS.netModemToolTipAntenna());
        this.labelRegistration.setText(MSGS.netHwRegistration());
        this.labelIMSI.setText(MSGS.netHwIMSI());
        this.labelICCID.setText(MSGS.netHwICCID());
        this.labelPLMNID.setText(MSGS.netHwPLMNID());
        this.labelNetwork.setText(MSGS.netHwNetwork());
        this.labelRadio.setText(MSGS.netHwRadio());
        this.labelBand.setText(MSGS.netHwBand());
        this.labelLAC.setText(MSGS.netHwLAC());
        this.labelCI.setText(MSGS.netHwCI());
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.tabs.getButtons() != null) {
            this.tabs.getButtons().setButtonsDirty(flag);
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
            updatedModemNetIf.setDiversityEnabled(this.radio1.getValue());
        } else {
            // initForm hasn't been called yet
            updatedModemNetIf.setDiversityEnabled(this.selectedModemIfConfig.isDiversityEnabled());
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
        // ENABLE DIVERSITY ANTENNA
        this.labelAntenna.setText(MSGS.netModemEnableCellDiv());
        this.radio2.addMouseOverHandler(event -> {
            if (TabModemAntennaUi.this.radio2.isEnabled()) {
                TabModemAntennaUi.this.helpText.clear();
                TabModemAntennaUi.this.helpText.add(new Span(MSGS.netModemToolTipAntenna()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());
        this.radio1.addMouseOverHandler(event -> {
            if (TabModemAntennaUi.this.radio1.isEnabled()) {
                TabModemAntennaUi.this.helpText.clear();
                TabModemAntennaUi.this.helpText.add(new Span(MSGS.netModemToolTipAntenna()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio2.addValueChangeHandler(event -> setDirty(true));
        this.radio1.addValueChangeHandler(event -> setDirty(true));

        this.helpTitle.setText(MSGS.netHelpTitle());
        this.radio2.setText(MSGS.falseLabel());
        this.radio1.setText(MSGS.trueLabel());
        this.radio2.setValue(true);
        this.radio1.setValue(false);
        this.formInitialized = true;
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void update() {
        if (this.selectedModemIfConfig != null) {
            if (this.selectedModemIfConfig.isDiversityEnabled()) {
                this.radio2.setValue(false);
                this.radio1.setValue(true);
            } else {
                this.radio2.setValue(true);
                this.radio1.setValue(false);
            }
        }
        this.registration.setText(this.selectedModemIfConfig.getHwRegistration());
        this.imsi.setText(this.selectedModemIfConfig.getHwIMSI());
        this.iccid.setText(this.selectedModemIfConfig.getHwICCID());
        this.plmnid.setText(this.selectedModemIfConfig.getHwPLMNID());
        this.network.setText(this.selectedModemIfConfig.getHwNetwork());
        this.radio.setText(this.selectedModemIfConfig.getHwRadio());
        this.band.setText(this.selectedModemIfConfig.getHwBand());
        this.lac.setText(this.selectedModemIfConfig.getHwLAC());
        this.ci.setText(this.selectedModemIfConfig.getHwCI());
        refreshForm();
    }

    private void refreshForm() {
        this.radio2.setEnabled(true);
        this.radio1.setEnabled(true);
    }

    private void reset() {
        this.radio2.setValue(true);
        this.radio1.setValue(false);
        this.registration.setText("");
        this.imsi.setText("");
        this.iccid.setText("");
        this.plmnid.setText("");
        this.network.setText("");
        this.radio.setText("");
        this.band.setText("");
        this.lac.setText("");
        this.ci.setText("");
        update();
    }
}
