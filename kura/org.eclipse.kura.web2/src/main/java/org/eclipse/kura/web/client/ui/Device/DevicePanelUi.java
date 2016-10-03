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
package org.eclipse.kura.web.client.ui.Device;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Well;
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
    private static ProfileTabUi profileBinder = GWT.create(ProfileTabUi.class);
    private static BundlesTabUi bundlesBinder = GWT.create(BundlesTabUi.class);
    private static ThreadsTabUi threadsBinder = GWT.create(ThreadsTabUi.class);

    private static SystemPropertiesTabUi systemPropertiesBinder = GWT.create(SystemPropertiesTabUi.class);
    private static CommandTabUi commandBinder = GWT.create(CommandTabUi.class);

    private GwtSession session;

    interface DevicePanelUiUiBinder extends UiBinder<Widget, DevicePanelUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    @UiField
    Well content;
    @UiField
    HTMLPanel deviceIntro;

    @UiField
    AnchorListItem profile, bundles, threads, systemProperties, command;

    public DevicePanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        // Profile selected by Default
        this.deviceIntro.add(new Span("<p>" + MSGS.deviceIntro() + "</p"));
        this.content.clear();
        setSelectedActive(this.profile);
        this.content.add(profileBinder);

        this.profile.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(DevicePanelUi.this.profile);
                DevicePanelUi.this.content.clear();
                DevicePanelUi.this.content.add(profileBinder);
                profileBinder.loadProfileData();
                // test.setSize("12345px", "16512px");
            }
        });

        this.bundles.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(DevicePanelUi.this.bundles);
                DevicePanelUi.this.content.clear();
                DevicePanelUi.this.content.add(bundlesBinder);
                bundlesBinder.loadBundlesData();
            }
        });

        this.threads.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(DevicePanelUi.this.threads);
                DevicePanelUi.this.content.clear();
                DevicePanelUi.this.content.add(threadsBinder);
                threadsBinder.loadThreadsData();
            }
        });

        this.systemProperties.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(DevicePanelUi.this.systemProperties);
                DevicePanelUi.this.content.clear();
                DevicePanelUi.this.content.add(systemPropertiesBinder);
                systemPropertiesBinder.loadSystemPropertiesData();
            }
        });

        this.command.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setSelectedActive(DevicePanelUi.this.command);
                DevicePanelUi.this.content.clear();
                DevicePanelUi.this.content.add(commandBinder);
            }
        });

    }

    public void initDevicePanel() {
        profileBinder.loadProfileData();
        commandBinder.setSession(this.session);
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

    public void setSelectedActive(AnchorListItem item) {
        this.profile.setActive(false);
        this.bundles.setActive(false);
        this.threads.setActive(false);
        this.systemProperties.setActive(false);
        this.command.setActive(false);
        item.setActive(true);

    }

}
