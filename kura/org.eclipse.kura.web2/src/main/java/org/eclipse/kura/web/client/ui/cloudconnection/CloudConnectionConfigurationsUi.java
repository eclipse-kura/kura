/*******************************************************************************
 * Copyright (c) 2016, 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.cloudconnection;

import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtCloudEntry;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionService;
import org.eclipse.kura.web.shared.service.GwtCloudConnectionServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class CloudConnectionConfigurationsUi extends Composite {

    private static CloudServiceConfigurationsUiUiBinder uiBinder = GWT
            .create(CloudServiceConfigurationsUiUiBinder.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudConnectionServiceAsync gwtCloudService = GWT.create(GwtCloudConnectionService.class);

    private TabListItem currentlySelectedTab;

    interface CloudServiceConfigurationsUiUiBinder extends UiBinder<Widget, CloudConnectionConfigurationsUi> {
    }

    private final CloudConnectionsUi cloudServicesUi;

    @UiField
    TabContent connectionTabContent;
    @UiField
    NavTabs connectionNavtabs;

    public CloudConnectionConfigurationsUi(CloudConnectionsUi cloudServicesUi) {
        initWidget(uiBinder.createAndBindUi(this));
        this.cloudServicesUi = cloudServicesUi;
    }

    public void setDirty(boolean dirty) {
        for (int connectionTabIndex = 0; connectionTabIndex < this.connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) this.connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                CloudConnectionConfigurationUi serviceConfigUi = (CloudConnectionConfigurationUi) pane
                        .getWidget(paneIndex);
                serviceConfigUi.setDirty(dirty);
            }
        }
    }

    public boolean isDirty() {
        boolean dirty = false;
        for (int connectionTabIndex = 0; connectionTabIndex < this.connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) this.connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                CloudConnectionConfigurationUi serviceConfigUi = (CloudConnectionConfigurationUi) pane
                        .getWidget(paneIndex);
                boolean dirtyTab = serviceConfigUi.isDirty();
                dirty = dirty || dirtyTab;
            }
        }
        return dirty;
    }

    public CloudConnectionConfigurationUi getDirtyCloudConfiguration() {
        for (int connectionTabIndex = 0; connectionTabIndex < this.connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) this.connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                CloudConnectionConfigurationUi serviceConfigUi = (CloudConnectionConfigurationUi) pane
                        .getWidget(paneIndex);
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

    public void selectEntry(GwtCloudEntry selection) {
        this.connectionNavtabs.clear();
        this.connectionTabContent.clear();

        if (selection instanceof GwtCloudConnectionEntry) {
            getCloudStackConfigurations(((GwtCloudConnectionEntry) selection).getCloudConnectionFactoryPid(),
                    selection.getPid());
        } else {
            getPubSubConfiguration(selection.getPid());
        }
    }

    public TabListItem getSelectedTab() {
        return this.currentlySelectedTab;
    }

    public void setSelectedTab(TabListItem tabListItem) {
        this.currentlySelectedTab = tabListItem;
    }

    private void getPubSubConfiguration(final String pid) {
        RequestQueue.submit(context -> this.gwtXSRFService.generateSecurityToken(context
                .callback(token -> this.gwtCloudService.getPubSubConfiguration(token, pid, context.callback(conf -> {
                    this.connectionNavtabs.clear();
                    renderTabs(conf, true);
                })))));
    }

    private void getCloudStackConfigurations(final String factoryPid, final String cloudServicePid) {
        RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(token -> this.gwtCloudService
                .getStackConfigurationsByFactory(factoryPid, cloudServicePid, c.callback(result -> {
                    if (result.isEmpty()) {
                        return;
                    }

                    final ArrayList<GwtConfigComponent> sorted = new ArrayList<>(result);
                    sorted.sort(Comparator.comparing(this::getSimplifiedComponentName));
                    this.connectionNavtabs.clear();
                    boolean isFirstEntry = true;
                    for (GwtConfigComponent pair : sorted) {
                        renderTabs(pair, isFirstEntry);
                        isFirstEntry = false;
                    }
                })))));
    }

    private void renderTabs(GwtConfigComponent config, boolean isFirstEntry) {
        final String simplifiedComponentName = getSimplifiedComponentName(config);
        TabListItem item = new TabListItem(simplifiedComponentName);
        item.setDataTarget("#" + simplifiedComponentName);
        item.addClickHandler(event -> {
            Anchor anchor = (Anchor) event.getSource();
            CloudConnectionConfigurationsUi.this.cloudServicesUi.onTabSelectionChange((TabListItem) anchor.getParent());
        });
        this.connectionNavtabs.add(item);

        TabPane tabPane = new TabPane();
        tabPane.setId(simplifiedComponentName);
        CloudConnectionConfigurationUi serviceConfigurationBinder = new CloudConnectionConfigurationUi(config);
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
        return tempName.substring(0, 1).toUpperCase() + tempName.substring(1);
    }
}
