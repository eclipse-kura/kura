/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.NewPasswordInput;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.shared.model.Gwt8021xEap;
import org.eclipse.kura.web.shared.model.Gwt8021xPhase2Auth;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiChannelModel;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;
import org.gwtbootstrap3.client.ui.PanelHeader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.ScrollPanel;

public class TabWireless8021xUi extends Composite implements NetworkTab {

    private static TabWireless8021xUiUiBinder uiBinder = GWT.create(TabWireless8021xUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    private static final Logger logger = Logger.getLogger(TabWireless8021xUi.class.getSimpleName());

    interface TabWireless8021xUiUiBinder extends UiBinder<Widget, TabWireless8021xUi> {
    }

    private final GwtSession session;
    private final NetworkTabsUi netTabs;

    GwtWifiConfig activeConfig;
    GwtWifiChannelModel previousSelection;

    private boolean dirty;

    // Labels
    @UiField
    FormLabel labelEap;

    @UiField
    FormLabel labelInnerAuth;

    @UiField
    FormLabel labelUsername;

    @UiField
    FormLabel labelPassword;

    // Fields
    @UiField
    Button buttonTestPassword;

    @UiField
    ListBox eap;

    @UiField
    ListBox innerAuth;

    @UiField
    TextBox username;

    @UiField
    NewPasswordInput password;

    // Help
    @UiField
    HelpButton helpEap;

    @UiField
    HelpButton helpInnerAuth;

    @UiField
    HelpButton helpUsername;

    @UiField
    HelpButton helpPassword;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    public TabWireless8021xUi(GwtSession currentSession, NetworkTabsUi tabs) {
        logger.info("Constructor started.");
        this.session = currentSession;
        this.netTabs = tabs;

        initForm();
        logger.info("Constructor done.");
    }

    @Override
    public void clear() {
        // Not needed
    }

    @Override
    public void refresh() {
        // Not needed
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
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        // GwtWifiNetInterfaceConfig updatedWifiNetIf = (GwtWifiNetInterfaceConfig)
        // updatedNetIf;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        // GwtWifiNetInterfaceConfig wifiConfig = (GwtWifiNetInterfaceConfig) config;
    }

    private void initForm() {
        logger.info("initForm started.");
        initWidget(uiBinder.createAndBindUi(this));
        // set up labels
        labelEap.setText("Enterprise EAP (Extensible Authentication Protocol)");
        for (Gwt8021xEap eap : Gwt8021xEap.values()) {
            this.eap.addItem(eap.name());
        }

        this.eap.addMouseOverHandler(event -> {
            TabWireless8021xUi.this.logger.info("hover detected."); //TODO: replace with real help text
        });

        labelInnerAuth.setText("Inner Authentication (Phase2 Auth)");
        for (Gwt8021xPhase2Auth auth : Gwt8021xPhase2Auth.values()) {
            this.innerAuth.addItem(auth.name());
        }

        this.innerAuth.addMouseOverHandler(event -> {
            TabWireless8021xUi.this.logger.info("hover detected.");
        });

        labelUsername.setText("Identity (Username)");
        this.username.addMouseOverHandler(event -> {
            TabWireless8021xUi.this.logger.info("hover detected.");
        });
        this.username.addBlurHandler(e -> this.username.validate());
        this.username.setAllowBlank(true);
        this.username.addMouseOutHandler(event -> resetHelp());

        labelPassword.setText("Password");
        this.password.addMouseOverHandler(event -> {
            TabWireless8021xUi.this.logger.info("hover detected.");
        });

        this.password.addBlurHandler(e -> this.password.validate());
        this.password.setAllowBlank(true);
        this.password.addMouseOutHandler(event -> resetHelp());

        logger.info("initForm FINISHED.");
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

}
