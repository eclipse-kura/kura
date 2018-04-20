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
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.EventService;
import org.eclipse.kura.web.client.util.request.Request;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.client.util.request.SuccessCallback;
import org.eclipse.kura.web.shared.ForwardedEventTopic;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionState;
import org.eclipse.kura.web.shared.model.GwtEventInfo;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
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

public class CloudServicesUi extends Composite {

    private static final Logger logger = Logger.getLogger(CloudServicesUi.class.getSimpleName());
    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtCloudServiceAsync gwtCloudService = GWT.create(GwtCloudService.class);

    private static CloudServicesUiUiBinder uiBinder = GWT.create(CloudServicesUiUiBinder.class);
    private CloudInstancesUi cloudInstancesBinder;
    private CloudServiceConfigurationsUi cloudServiceConfigurationsBinder;

    private GwtCloudConnectionEntry currentlySelectedEntry;
    private TabListItem currentlySelectedTab;

    private static final String CONNECTION_EVENT_PID_PROPERTY_KEY = "cloud.service.pid";

    interface CloudServicesUiUiBinder extends UiBinder<Widget, CloudServicesUi> {
    }

    @UiField
    HTMLPanel cloudServicesIntro;
    @UiField
    Panel cloudInstancesPanel;
    @UiField
    Panel cloudConfigurationsPanel;
    @UiField
    Alert notification;

    public CloudServicesUi() {
        logger.log(Level.FINER, "Initializing StatusPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));

        this.cloudServicesIntro.add(new Span("<p>" + MSGS.cloudServicesTabIntro() + "</p>"));

        cloudInstancesBinder = new CloudInstancesUi(this);
        this.cloudInstancesPanel.add(cloudInstancesBinder);

        cloudServiceConfigurationsBinder = new CloudServiceConfigurationsUi(this);
        this.cloudConfigurationsPanel.add(cloudServiceConfigurationsBinder);

        EventService.subscribe(ForwardedEventTopic.CLOUD_CONNECTION_STATUS_ESTABLISHED, new EventService.Handler() {

            @Override
            public void handleEvent(GwtEventInfo eventInfo) {
                handleConnectionStatusEvent(eventInfo, GwtCloudConnectionState.CONNECTED);
            }
        });
        EventService.subscribe(ForwardedEventTopic.CLOUD_CONNECTION_STATUS_LOST, new EventService.Handler() {

            @Override
            public void handleEvent(GwtEventInfo eventInfo) {
                handleConnectionStatusEvent(eventInfo, GwtCloudConnectionState.DISCONNECTED);
            }
        });
    }

    public void refresh() {
        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtCloudService
                        .findCloudServices(context.callback(new SuccessCallback<List<GwtCloudConnectionEntry>>() {

                            @Override
                            public void onSuccess(final List<GwtCloudConnectionEntry> data) {
                                cloudInstancesBinder.setData(data);
                                currentlySelectedEntry = cloudInstancesBinder.getSelectedObject();
                                currentlySelectedTab = cloudServiceConfigurationsBinder.getSelectedTab();
                                setVisibility();
                            }
                        }));
            }
        });
    }

    public void setDirty(boolean dirty) {
        cloudServiceConfigurationsBinder.setDirty(dirty);
    }

    public boolean isDirty() {
        return cloudServiceConfigurationsBinder.isDirty();
    }

    //
    // Private methods
    //
    private void setVisibility() {
        if (cloudInstancesBinder.getTableSize() == 0) {
            cloudInstancesBinder.setVisibility(false);
            cloudServiceConfigurationsBinder.setVisibility(false);
            this.cloudConfigurationsPanel.setVisible(false);
            this.notification.setVisible(true);
            this.notification.setText(MSGS.noConnectionsAvailable());
        } else {
            cloudInstancesBinder.setVisibility(true);
            cloudServiceConfigurationsBinder.setVisibility(true);
            this.cloudConfigurationsPanel.setVisible(true);
            this.notification.setVisible(false);
        }
    }

    protected void onSelectionChange() {
        GwtCloudConnectionEntry selectedInstanceEntry = cloudInstancesBinder.getSelectedObject();

        if (!isDirty()) {
            if (selectedInstanceEntry != null) {
                this.currentlySelectedEntry = selectedInstanceEntry;
                cloudServiceConfigurationsBinder.selectConnection(selectedInstanceEntry);
            }
        } else {
            if (selectedInstanceEntry != this.currentlySelectedEntry) {
                showDirtyModal();
            }
        }
    }

    protected void onTabSelectionChange(TabListItem newTab) {
        this.currentlySelectedTab = cloudServiceConfigurationsBinder.getSelectedTab();
        if (isDirty() && newTab != this.currentlySelectedTab) {
            showDirtyModal();
        } else {
            this.currentlySelectedTab = newTab;
            cloudServiceConfigurationsBinder.setSelectedTab(this.currentlySelectedTab);
        }
    }

    private void handleConnectionStatusEvent(final GwtEventInfo info, final GwtCloudConnectionState state) {
        final String cloudServicePid = (String) info.get(CONNECTION_EVENT_PID_PROPERTY_KEY);

        if (cloudServicePid == null || !cloudInstancesBinder.setStatus(cloudServicePid, state)) {
            CloudServicesUi.this.refresh();
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
                GwtCloudConnectionEntry selectedInstanceEntry = cloudInstancesBinder.getSelectedObject();
                if (selectedInstanceEntry != null) {
                    CloudServicesUi.this.currentlySelectedEntry = selectedInstanceEntry;
                    cloudServiceConfigurationsBinder.selectConnection(selectedInstanceEntry);
                }

                CloudServiceConfigurationUi dirtyConfig = cloudServiceConfigurationsBinder.getDirtyCloudConfiguration();
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
                cloudInstancesBinder.setSelected(CloudServicesUi.this.currentlySelectedEntry);
                CloudServicesUi.this.currentlySelectedTab.showTab();
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
