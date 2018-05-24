/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.cloudconnection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry.GwtCloudConnectionState;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.Panel;
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

public class CloudConnectionsUi extends Composite {

    private static final Logger logger = Logger.getLogger(CloudConnectionsUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtCloudConnectionServiceAsync gwtCloudService = GWT.create(GwtCloudConnectionService.class);

    private static CloudServicesUiUiBinder uiBinder = GWT.create(CloudServicesUiUiBinder.class);
    private final CloudInstancesUi cloudInstancesBinder;
    private final CloudConnectionConfigurationsUi cloudServiceConfigurationsBinder;

    private GwtCloudEntry currentlySelectedEntry;
    private TabListItem currentlySelectedTab;

    private static final String CONNECTION_EVENT_PID_PROPERTY_KEY = "cloud.service.pid";

    interface CloudServicesUiUiBinder extends UiBinder<Widget, CloudConnectionsUi> {
    }

    @UiField
    HTMLPanel cloudServicesIntro;
    @UiField
    Panel cloudInstancesPanel;
    @UiField
    Panel cloudConfigurationsPanel;
    @UiField
    Alert notification;

    public CloudConnectionsUi() {
        logger.log(Level.FINER, "Initializing StatusPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));

        this.cloudServicesIntro.add(new Span("<p>" + MSGS.cloudServicesTabIntro() + "</p>"));

        this.cloudInstancesBinder = new CloudInstancesUi(this);
        this.cloudInstancesPanel.add(this.cloudInstancesBinder);

        this.cloudServiceConfigurationsBinder = new CloudConnectionConfigurationsUi(this);
        this.cloudConfigurationsPanel.add(this.cloudServiceConfigurationsBinder);

        EventService.subscribe(ForwardedEventTopic.CLOUD_CONNECTION_STATUS_ESTABLISHED,
                eventInfo -> handleConnectionStatusEvent(eventInfo, GwtCloudConnectionState.CONNECTED));

        EventService.subscribe(ForwardedEventTopic.CLOUD_CONNECTION_STATUS_LOST,
                eventInfo -> handleConnectionStatusEvent(eventInfo, GwtCloudConnectionState.DISCONNECTED));
    }

    public void refresh() {
        RequestQueue.submit(context -> this.gwtCloudService.findCloudEntries(context.callback(data -> {
            this.cloudInstancesBinder.setData(data);
            this.currentlySelectedEntry = this.cloudInstancesBinder.getSelectedObject();
            this.currentlySelectedTab = this.cloudServiceConfigurationsBinder.getSelectedTab();
            setVisibility();

            this.gwtCloudService
                    .getCloudComponentFactories(context.callback(this.cloudInstancesBinder::setFactoryInfo));
        })));
    }

    public void setDirty(boolean dirty) {
        this.cloudServiceConfigurationsBinder.setDirty(dirty);
    }

    public boolean isDirty() {
        return this.cloudServiceConfigurationsBinder.isDirty();
    }

    //
    // Private methods
    //
    private void setVisibility() {
        if (this.cloudInstancesBinder.getTableSize() == 0) {
            this.cloudInstancesBinder.setVisibility(false);
            this.cloudServiceConfigurationsBinder.setVisibility(false);
            this.cloudConfigurationsPanel.setVisible(false);
            this.notification.setVisible(true);
            this.notification.setText(MSGS.noConnectionsAvailable());
        } else {
            this.cloudInstancesBinder.setVisibility(true);
            this.cloudServiceConfigurationsBinder.setVisibility(true);
            this.cloudConfigurationsPanel.setVisible(true);
            this.notification.setVisible(false);
        }
    }

    protected void onSelectionChange() {
        GwtCloudEntry selectedInstanceEntry = this.cloudInstancesBinder.getSelectedObject();

        if (!isDirty()) {
            if (selectedInstanceEntry != null) {
                this.currentlySelectedEntry = selectedInstanceEntry;
                this.cloudServiceConfigurationsBinder.selectEntry(selectedInstanceEntry);
            }
        } else {
            if (selectedInstanceEntry != this.currentlySelectedEntry) {
                showDirtyModal();
            }
        }
    }

    protected void onTabSelectionChange(TabListItem newTab) {
        this.currentlySelectedTab = this.cloudServiceConfigurationsBinder.getSelectedTab();
        if (isDirty() && newTab != this.currentlySelectedTab) {
            showDirtyModal();
        } else {
            this.currentlySelectedTab = newTab;
            this.cloudServiceConfigurationsBinder.setSelectedTab(this.currentlySelectedTab);
        }
    }

    private void handleConnectionStatusEvent(final GwtEventInfo info, final GwtCloudConnectionState state) {
        final String cloudServicePid = (String) info.get(CONNECTION_EVENT_PID_PROPERTY_KEY);

        if (cloudServicePid == null || !this.cloudInstancesBinder.setStatus(cloudServicePid, state)) {
            CloudConnectionsUi.this.refresh();
        }
    }

    private void showDirtyModal() {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSGS.confirm());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSGS.deviceConfigDirty()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        ButtonGroup group = new ButtonGroup();
        Button yes = new Button();
        yes.setText(MSGS.yesButton());
        yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
                GwtCloudEntry selectedInstanceEntry = CloudConnectionsUi.this.cloudInstancesBinder.getSelectedObject();
                if (selectedInstanceEntry != null) {
                    CloudConnectionsUi.this.currentlySelectedEntry = selectedInstanceEntry;
                    CloudConnectionsUi.this.cloudServiceConfigurationsBinder.selectEntry(selectedInstanceEntry);
                }

                CloudConnectionConfigurationUi dirtyConfig = CloudConnectionsUi.this.cloudServiceConfigurationsBinder
                        .getDirtyCloudConfiguration();
                if (dirtyConfig != null) {
                    dirtyConfig.resetVisualization();
                }

                setDirty(false);
            }
        });
        Button no = new Button();
        no.setText(MSGS.noButton());
        no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                CloudConnectionsUi.this.cloudInstancesBinder.setSelected(CloudConnectionsUi.this.currentlySelectedEntry);
                CloudConnectionsUi.this.currentlySelectedTab.showTab();
                modal.hide();
            }
        });
        group.add(no);
        group.add(yes);
        footer.add(group);
        modal.add(footer);
        modal.show();
        no.setFocus(true);
    }
}
