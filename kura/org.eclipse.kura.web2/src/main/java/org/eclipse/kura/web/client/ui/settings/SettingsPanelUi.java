/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.settings;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Paragraph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class SettingsPanelUi extends Composite {

    private static SettingsPanelUiUiBinder uiBinder = GWT.create(SettingsPanelUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(SettingsPanelUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityServiceAsync gwtSecurityService = GWT.create(GwtSecurityService.class);

    interface SettingsPanelUiUiBinder extends UiBinder<Widget, SettingsPanelUi> {
    }

    GwtSession session;

    @UiField
    SnapshotsTabUi snapshotsPanel;

    @UiField
    ApplicationCertsTabUi appCertPanel;

    @UiField
    SslTabUi sslConfigPanel;

    @UiField
    ServerCertsTabUi serverCertPanel;

    @UiField
    DeviceCertsTabUi deviceCertPanel;

    @UiField
    SecurityTabUi securityPanel;

    @UiField
    TabListItem snapshots;
    @UiField
    TabListItem appCert;
    @UiField
    TabListItem sslConfig;
    @UiField
    TabListItem serverCert;
    @UiField
    TabListItem deviceCert;
    @UiField
    TabListItem security;

    @UiField
    HTMLPanel settingsIntro;

    public SettingsPanelUi() {
        logger.log(Level.FINER, "Initiating SettingsPanelUI...");

        initWidget(uiBinder.createAndBindUi(this));
        Paragraph description = new Paragraph();
        description.setText(MSGS.settingsIntro());
        this.settingsIntro.add(description);

        this.snapshots.setVisible(true);

        AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    SettingsPanelUi.this.appCert.setVisible(true);
                    SettingsPanelUi.this.security.setVisible(true);
                }
            }
        };
        this.gwtSecurityService.isSecurityServiceAvailable(callback);

        this.snapshots.addClickHandler(new Tab.RefreshHandler(this.snapshotsPanel));
        this.sslConfig.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                SettingsPanelUi.this.sslConfigPanel.load();
            }
        });
        this.serverCert.addClickHandler(new Tab.RefreshHandler(this.serverCertPanel));
        this.deviceCert.addClickHandler(new Tab.RefreshHandler(this.deviceCertPanel));
        this.security.addClickHandler(new Tab.RefreshHandler(this.securityPanel));
    }

    public void load() {
        if (!this.snapshotsPanel.isDirty()) {
            this.snapshotsPanel.refresh();
        }
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public boolean isDirty() {
        boolean snapshotsDirty = this.snapshotsPanel.isDirty();
        boolean appCertDirty = this.appCertPanel.isDirty();
        boolean sslConfigDirty = this.sslConfigPanel.isDirty();
        boolean serverCertDirty = this.serverCertPanel.isDirty();
        boolean deviceCertDirty = this.deviceCertPanel.isDirty();
        boolean securityDirty = this.securityPanel.isDirty();

        return snapshotsDirty || appCertDirty || sslConfigDirty || serverCertDirty || deviceCertDirty || securityDirty;
    }

    public void setDirty(boolean b) {

        this.snapshotsPanel.setDirty(b);
        this.appCertPanel.setDirty(b);
        this.sslConfigPanel.setDirty(b);
        this.serverCertPanel.setDirty(b);
        this.deviceCertPanel.setDirty(b);
        this.securityPanel.setDirty(b);
    }
}
