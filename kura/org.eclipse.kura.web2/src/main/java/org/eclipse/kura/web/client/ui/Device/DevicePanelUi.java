/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
    TabListItem profile, bundles, threads, systemProperties, command;

    @UiField
    ProfileTabUi profilePanel;
    @UiField
    BundlesTabUi bundlesPanel;
    @UiField
    ThreadsTabUi threadsPanel;
    @UiField
    SystemPropertiesTabUi systemPropertiesPanel;
    @UiField
    CommandTabUi commandPanel;

    public DevicePanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        // Profile selected by Default
        this.deviceIntro.add(new Span("<p>" + MSGS.deviceIntro() + "</p"));

        this.profile.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                profilePanel.loadProfileData();
                // test.setSize("12345px", "16512px");
            }
        });

        this.bundles.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                bundlesPanel.loadBundlesData();
            }
        });

        this.threads.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                threadsPanel.loadThreadsData();
            }
        });

        this.systemProperties.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                systemPropertiesPanel.loadSystemPropertiesData();
            }
        });

    }

    public void initDevicePanel() {
        profilePanel.loadProfileData();
        commandPanel.setSession(this.session);
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

}
