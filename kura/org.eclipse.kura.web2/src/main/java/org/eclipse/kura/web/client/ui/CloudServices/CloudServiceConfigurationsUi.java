/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.List;

import org.eclipse.kura.web.client.util.request.Request;
import org.eclipse.kura.web.client.util.request.RequestContext;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.client.util.request.SuccessCallback;
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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class CloudServiceConfigurationsUi extends Composite {

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

        getCloudStackConfigurations(selection.getCloudFactoryPid(), selection.getCloudServicePid());
    }

    public TabListItem getSelectedTab() {
        return this.currentlySelectedTab;
    }

    public void setSelectedTab(TabListItem tabListItem) {
        this.currentlySelectedTab = tabListItem;
    }

    private void getCloudStackConfigurations(final String factoryPid, final String cloudServicePid) {

        RequestQueue.submit(new Request() {

            @Override
            public void run(final RequestContext context) {
                gwtCloudService.findStackPidsByFactory(factoryPid, cloudServicePid,
                        context.callback(new SuccessCallback<List<String>>() {

                            @Override
                            public void onSuccess(final List<String> pidsResult) {

                                CloudServiceConfigurationsUi.this.gwtXSRFService
                                        .generateSecurityToken(context.callback(new SuccessCallback<GwtXSRFToken>() {

                                            @Override
                                            public void onSuccess(final GwtXSRFToken token) {
                                                CloudServiceConfigurationsUi.this.gwtComponentService
                                                        .findFilteredComponentConfigurations(token, context.callback(
                                                                new SuccessCallback<List<GwtConfigComponent>>() {

                                                                    @Override
                                                                    public void onSuccess(
                                                                            List<GwtConfigComponent> result) {
                                                                        boolean isFirstEntry = true;
                                                                        connectionNavtabs.clear();
                                                                        for (GwtConfigComponent pair : result) {
                                                                            if (pidsResult
                                                                                    .contains(pair.getComponentId())) {
                                                                                renderTabs(pair, isFirstEntry);
                                                                                isFirstEntry = false;
                                                                            }
                                                                        }
                                                                    }
                                                                }));
                                            }
                                        }));
                            }
                        }));
            }
        });
    }

    private void renderTabs(GwtConfigComponent config, boolean isFirstEntry) {
        final String simplifiedComponentName = getSimplifiedComponentName(config);
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

    private String getSimplifiedComponentName(GwtConfigComponent config) {
        String selectedCloudServicePid = config.getComponentId();
        String tempName;
        int start = selectedCloudServicePid.lastIndexOf('.');
        int substringIndex = start + 1;
        if (start != -1 && substringIndex < selectedCloudServicePid.length()) {
            tempName = selectedCloudServicePid.substring(substringIndex);
        } else {
            tempName = selectedCloudServicePid;
        }
        return tempName;
    }
}