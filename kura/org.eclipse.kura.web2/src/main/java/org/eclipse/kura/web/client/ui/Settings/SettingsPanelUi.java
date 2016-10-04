/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Settings;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtSecurityService;
import org.eclipse.kura.web.shared.service.GwtSecurityServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;
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

    private static SnapshotsTabUi snapshotsBinder = GWT.create(SnapshotsTabUi.class);
    private static ApplicationCertsTabUi appCertBinder = GWT.create(ApplicationCertsTabUi.class);
    private static SslTabUi sslConfigBinder = GWT.create(SslTabUi.class);
    private static ServerCertsTabUi serverCertBinder = GWT.create(ServerCertsTabUi.class);
    private static DeviceCertsTabUi deviceCertBinder = GWT.create(DeviceCertsTabUi.class);
    private static SecurityTabUi securityBinder = GWT.create(SecurityTabUi.class);

    GwtSession Session;
    @UiField
    AnchorListItem snapshots, appCert, sslConfig, serverCert, deviceCert, security;
    @UiField
    Well content;
    @UiField
    HTMLPanel settingsIntro;

    public SettingsPanelUi() {
        logger.log(Level.FINER, "Initiating SettingsPanelUI...");
        initWidget(uiBinder.createAndBindUi(this));
        setSelectedActive(this.snapshots);
        Paragraph description = new Paragraph();
        description.setText(MSGS.settingsIntro());
        this.settingsIntro.add(description);
        this.content.clear();
        this.content.add(snapshotsBinder);

        this.snapshots.setVisible(true);
        this.appCert.setVisible(false);
        this.sslConfig.setVisible(true);
        this.serverCert.setVisible(true);
        this.deviceCert.setVisible(true);
        this.security.setVisible(false);

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

        this.snapshots.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(SettingsPanelUi.this.snapshots);
                SettingsPanelUi.this.content.clear();
                SettingsPanelUi.this.content.add(snapshotsBinder);
                if (!snapshotsBinder.isDirty()) {
                    snapshotsBinder.refresh();
                }
            }
        });

        this.appCert.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(SettingsPanelUi.this.appCert);
                SettingsPanelUi.this.content.clear();
                SettingsPanelUi.this.content.add(appCertBinder);
                if (!appCertBinder.isDirty()) {
                    appCertBinder.refresh();
                }
            }
        });

        this.sslConfig.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(SettingsPanelUi.this.sslConfig);
                SettingsPanelUi.this.content.clear();
                SettingsPanelUi.this.content.add(sslConfigBinder);
                if (!sslConfigBinder.isDirty()) {
                    sslConfigBinder.refresh();
                }
            }
        });

        this.serverCert.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(SettingsPanelUi.this.serverCert);
                SettingsPanelUi.this.content.clear();
                SettingsPanelUi.this.content.add(serverCertBinder);
                if (!serverCertBinder.isDirty()) {
                    serverCertBinder.refresh();
                }
            }
        });

        this.deviceCert.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(SettingsPanelUi.this.deviceCert);
                SettingsPanelUi.this.content.clear();
                SettingsPanelUi.this.content.add(deviceCertBinder);
                if (!deviceCertBinder.isDirty()) {
                    deviceCertBinder.refresh();
                }
            }
        });

        this.security.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(SettingsPanelUi.this.security);
                SettingsPanelUi.this.content.clear();
                SettingsPanelUi.this.content.add(securityBinder);
                if (!securityBinder.isDirty()) {
                    securityBinder.refresh();
                }
            }
        });
    }

    public void load() {
        setSelectedActive(this.snapshots);
        this.content.clear();
        this.content.add(snapshotsBinder);
        if (!snapshotsBinder.isDirty()) {
            snapshotsBinder.refresh();
        }
    }

    public void setSession(GwtSession currentSession) {
        this.Session = currentSession;
    }

    public void setSelectedActive(AnchorListItem item) {
        this.snapshots.setActive(false);
        this.appCert.setActive(false);
        this.sslConfig.setActive(false);
        this.serverCert.setActive(false);
        this.deviceCert.setActive(false);
        this.security.setActive(false);
        item.setActive(true);
    }

    public boolean isDirty() {
        boolean snapshotsDirty = snapshotsBinder.isDirty();
        boolean appCertDirty = appCertBinder.isDirty();
        boolean sslConfigDirty = sslConfigBinder.isDirty();
        boolean serverCertDirty = serverCertBinder.isDirty();
        boolean deviceCertDirty = deviceCertBinder.isDirty();
        boolean securityDirty = securityBinder.isDirty();

        return snapshotsDirty || appCertDirty || sslConfigDirty || serverCertDirty || deviceCertDirty || securityDirty;
    }

    public void setDirty(boolean b) {
        if (snapshotsBinder != null) {
            snapshotsBinder.setDirty(b);
        }
        if (appCertBinder != null) {
            appCertBinder.setDirty(b);
        }
        if (sslConfigBinder != null) {
            sslConfigBinder.setDirty(b);
        }
        if (serverCertBinder != null) {
            serverCertBinder.setDirty(b);
        }
        if (deviceCertBinder != null) {
            deviceCertBinder.setDirty(b);
        }
        if (securityBinder != null) {
            securityBinder.setDirty(b);
        }
    }
}
