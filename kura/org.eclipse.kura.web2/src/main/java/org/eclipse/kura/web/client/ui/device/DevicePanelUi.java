/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

    public DevicePanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.deviceIntro.add(new Span("<p>" + MSGS.deviceIntro() + "</p"));

        this.profile.addClickHandler(new Tab.RefreshHandler(this.profilePanel));
        this.bundles.addClickHandler(new Tab.RefreshHandler(this.bundlesPanel));
        this.threads.addClickHandler(new Tab.RefreshHandler(this.threadsPanel));
        this.packages.addClickHandler(new Tab.RefreshHandler(this.packagesPanel));
        this.systemProperties.addClickHandler(new Tab.RefreshHandler(this.systemPropertiesPanel));
    }

    public void initDevicePanel() {
        this.profilePanel.refresh();
        this.commandPanel.setSession(this.session);
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

}
