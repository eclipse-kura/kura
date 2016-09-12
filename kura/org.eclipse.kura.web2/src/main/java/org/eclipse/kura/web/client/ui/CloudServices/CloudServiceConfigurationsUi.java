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
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class CloudServiceConfigurationsUi extends Composite {

    private static final Logger logger = Logger.getLogger(CloudServiceConfigurationsUi.class.getSimpleName());
    private static final String KURA_CLOUD_SERVICE_FACTORY_PID = "kura.cloud.service.factory.pid";

    private static CloudServiceConfigurationsUiUiBinder uiBinder = GWT
            .create(CloudServiceConfigurationsUiUiBinder.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudServiceAsync gwtCloudService = GWT.create(GwtCloudService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private TabListItem currentlySelectedTab;

    interface CloudServiceConfigurationsUiUiBinder extends UiBinder<Widget, CloudServiceConfigurationsUi> {
    }

    private boolean dirty;
    private final CloudServicesUi cloudServicesUi;

    @UiField
    TabContent connectionTabContent;
    @UiField
    NavTabs connectionNavtabs;

    public CloudServiceConfigurationsUi(CloudServicesUi cloudServicesUi) {
        initWidget(uiBinder.createAndBindUi(this));
        this.cloudServicesUi = cloudServicesUi;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        for (int connectionTabIndex = 0; connectionTabIndex < this.connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) this.connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                CloudServiceConfigurationUi serviceConfigUi = (CloudServiceConfigurationUi) pane.getWidget(paneIndex);
                serviceConfigUi.setDirty(dirty);
            }
        }
    }

    public boolean isDirty() {
        for (int connectionTabIndex = 0; connectionTabIndex < this.connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) this.connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                CloudServiceConfigurationUi serviceConfigUi = (CloudServiceConfigurationUi) pane.getWidget(paneIndex);
                this.dirty = this.dirty || serviceConfigUi.isDirty();
            }
        }
        return this.dirty;
    }

    public CloudServiceConfigurationUi getDirtyCloudConfiguration() {
        for (int connectionTabIndex = 0; connectionTabIndex < this.connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) this.connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                CloudServiceConfigurationUi serviceConfigUi = (CloudServiceConfigurationUi) pane.getWidget(paneIndex);
                if (serviceConfigUi.isDirty()) {
                    return serviceConfigUi;
                }
            }
        }
        return null;
    }

    public void setVisibility(boolean isVisible) {
        this.connectionNavtabs.setVisible(isVisible);
        this.connectionTabContent.setVisible(isVisible);
    }

    public void selectConnection(GwtCloudConnectionEntry selection) {
        this.connectionNavtabs.clear();
        this.connectionTabContent.clear();

        final String selectedCloudServicePid = selection.getCloudServicePid();

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                CloudServiceConfigurationsUi.this.gwtComponentService.findComponentConfiguration(token,
                        selectedCloudServicePid, new AsyncCallback<List<GwtConfigComponent>>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(List<GwtConfigComponent> result) {
                        for (GwtConfigComponent pair : result) {
                            if (selectedCloudServicePid.equals(pair.getComponentId())
                                    && pair.getParameter(KURA_CLOUD_SERVICE_FACTORY_PID) != null) {
                                String factoryPid = pair.getParameter(KURA_CLOUD_SERVICE_FACTORY_PID).getValue();
                                getCloudStackConfigurations(factoryPid, selectedCloudServicePid);
                            }
                        }
                    }
                });
            }
        });
    }

    public TabListItem getSelectedTab() {
        return this.currentlySelectedTab;
    }

    public void setSelectedTab(TabListItem tabListItem) {
        this.currentlySelectedTab = tabListItem;
    }

    private void getCloudStackConfigurations(String factoryPid, String cloudServicePid) {
        this.gwtCloudService.findStackPidsByFactory(factoryPid, cloudServicePid, new AsyncCallback<List<String>>() {

            @Override
            public void onFailure(Throwable ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(List<String> result) {
                final List<String> pidsResult = result;

                CloudServiceConfigurationsUi.this.gwtXSRFService
                        .generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        CloudServiceConfigurationsUi.this.gwtComponentService.findFilteredComponentConfigurations(token,
                                new AsyncCallback<List<GwtConfigComponent>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(List<GwtConfigComponent> result) {
                                boolean isFirstEntry = true;
                                for (GwtConfigComponent pair : result) {
                                    if (pidsResult.contains(pair.getComponentId())) {
                                        renderTabs(pair, isFirstEntry);
                                        isFirstEntry = false;
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void renderTabs(GwtConfigComponent config, boolean isFirstEntry) {
        String selectedCloudServicePid = config.getComponentId();
        String name0;
        int start = selectedCloudServicePid.lastIndexOf('.');
        int substringIndex = start + 1;
        if (start != -1 && substringIndex < selectedCloudServicePid.length()) {
            name0 = selectedCloudServicePid.substring(substringIndex);
        } else {
            name0 = selectedCloudServicePid;
        }
        final String simplifiedComponentName = name0;
        TabListItem item = new TabListItem(simplifiedComponentName);
        item.setDataTarget("#" + simplifiedComponentName);
        item.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Anchor anchor = (Anchor) event.getSource();
                CloudServiceConfigurationsUi.this.cloudServicesUi
                        .onTabSelectionChange((TabListItem) anchor.getParent());
            }
        });
        this.connectionNavtabs.add(item);

        TabPane tabPane = new TabPane();
        tabPane.setId(simplifiedComponentName);
        CloudServiceConfigurationUi serviceConfigurationBinder = new CloudServiceConfigurationUi(config);
        tabPane.add(serviceConfigurationBinder);
        this.connectionTabContent.add(tabPane);

        if (isFirstEntry) {
            this.currentlySelectedTab = item;
            item.setActive(true);
            tabPane.setActive(true);
        }

        serviceConfigurationBinder.renderForm();
    }

}
