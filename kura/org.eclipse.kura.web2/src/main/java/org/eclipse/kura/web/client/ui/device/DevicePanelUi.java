/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class DevicePanelUi extends Composite {

    private static DevicePanelUiUiBinder uiBinder = GWT.create(DevicePanelUiUiBinder.class);

    private GwtSession session;

    interface DevicePanelUiUiBinder extends UiBinder<Widget, DevicePanelUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    HTMLPanel deviceIntro;

    @UiField
    TabListItem profile;
    @UiField
    TabListItem bundles;
    @UiField
    TabListItem threads;
    @UiField
    TabListItem packages;
    @UiField
    TabListItem systemProperties;
    @UiField
    TabListItem containers;

    @UiField
    ProfileTabUi profilePanel;
    @UiField
    BundlesTabUi bundlesPanel;
    @UiField
    ThreadsTabUi threadsPanel;
    @UiField
    SystemPackagesTabUi packagesPanel;
    @UiField
    SystemPropertiesTabUi systemPropertiesPanel;
    @UiField
    CommandTabUi commandPanel;
    @UiField
    LogTabUi logPanel;
    @UiField
    DockerContainersTabUi dockerContainersPanel;

    public DevicePanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.deviceIntro.add(new Span("<p>" + MSGS.deviceIntro() + "</p"));

        this.profile.addClickHandler(new Tab.RefreshHandler(this.profilePanel));
        this.bundles.addClickHandler(new Tab.RefreshHandler(this.bundlesPanel));
        this.threads.addClickHandler(new Tab.RefreshHandler(this.threadsPanel));
        this.packages.addClickHandler(new Tab.RefreshHandler(this.packagesPanel));
        this.systemProperties.addClickHandler(new Tab.RefreshHandler(this.systemPropertiesPanel));
        this.containers.addClickHandler(new Tab.RefreshHandler(this.dockerContainersPanel));

        this.containers.setVisible(false); // hidden by default
    }

    public void initDevicePanel() {
        this.profilePanel.refresh();
        this.commandPanel.setSession(this.session);
        this.logPanel.initialize();
        checkIfContainerOrchestratorIsAvaliable();
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public void checkIfContainerOrchestratorIsAvaliable() {

        final GwtDeviceServiceAsync deviceService = GWT.create(GwtDeviceService.class);
        final GwtSecurityTokenServiceAsync securityTokenService = GWT.create(GwtSecurityTokenService.class);

        securityTokenService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                deviceService.checkIfContainerOrchestratorIsActive(token, new AsyncCallback<Boolean>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        DevicePanelUi.this.containers.setVisible(false);
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        DevicePanelUi.this.containers.setVisible(result);
                    }
                });
            }
        });
    }
}
