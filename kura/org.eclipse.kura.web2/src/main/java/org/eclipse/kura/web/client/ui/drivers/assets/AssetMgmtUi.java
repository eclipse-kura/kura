/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.drivers.assets;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AssetMgmtUi extends Composite {

    private static AssetMgmtUiUiBinder uiBinder = GWT.create(AssetMgmtUiUiBinder.class);

    interface AssetMgmtUiUiBinder extends UiBinder<Widget, AssetMgmtUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private AssetConfigurationUi assetConfigUi;
    private AssetDataUi assetDataUi;

    @UiField
    TabListItem tab1NavTab;
    @UiField
    TabListItem tab2NavTab;
    @UiField
    TabPane tab1Pane;
    @UiField
    TabPane tab2Pane;
    @UiField
    AlertDialog alertDialog;

    public AssetMgmtUi(final HasConfiguration hasConfiguration, Configurations configurations) {
        initWidget(uiBinder.createAndBindUi(this));

        final GwtConfigComponent assetConfiguration = hasConfiguration.getConfiguration();
        final String driverPid = assetConfiguration.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());
        final GwtConfigComponent channelDescriptor = configurations.getChannelDescriptor(driverPid);

        if (channelDescriptor == null) {
            return;
        }

        final AssetModel model = new AssetModelImpl(new GwtConfigComponent(assetConfiguration), channelDescriptor,
                configurations.getBaseChannelDescriptor());
        this.assetDataUi = new AssetDataUi(model);
        this.assetConfigUi = new AssetConfigurationUi(model, this.assetDataUi, configurations);
        final ConfigurationUiButtons buttonBar = createAssetUiButtonBar(this.assetConfigUi, configurations);

        final Panel panel = new Panel();
        panel.add(buttonBar);
        panel.add(this.assetConfigUi);
        this.tab1Pane.add(panel);
        this.tab2Pane.add(this.assetDataUi);

        this.tab2NavTab.addClickHandler(event -> {
            if (AssetMgmtUi.this.assetConfigUi.isDirty()) {
                AssetMgmtUi.this.alertDialog.show(MSGS.driversAssetsAssetConfigDirty(), AlertDialog.Severity.ALERT,
                        null);
                event.stopPropagation();
                event.preventDefault();
                return;
            }
            AssetMgmtUi.this.assetDataUi.renderForm();
        });
    }

    public void refresh() {
        if (this.tab1NavTab.isActive()) {
            this.assetConfigUi.renderForm();
        } else {
            this.assetDataUi.renderForm();
        }
    }

    public void setDirty(boolean flag) {
        this.assetConfigUi.setDirty(flag);
        this.assetDataUi.setDirty(flag);
    }

    public boolean isDirty() {
        return this.assetConfigUi.isDirty() || this.assetDataUi.isDirty();
    }

    public ConfigurationUiButtons createAssetUiButtonBar(final AssetConfigurationUi assetUi,
            final Configurations configurations) {
        final GwtConfigComponent gwtConfig = assetUi.getConfiguration();
        final ConfigurationUiButtons result = new ConfigurationUiButtons(assetUi);
        result.setListener(new ConfigurationUiButtons.Listener() {

            @Override
            public void onReset() {
                final String driverPid = gwtConfig.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());
                final AssetModel model = new AssetModelImpl(
                        new GwtConfigComponent(
                                configurations.getConfiguration(gwtConfig.getComponentId()).getConfiguration()),
                        configurations.getChannelDescriptor(driverPid), configurations.getBaseChannelDescriptor());
                AssetMgmtUi.this.assetDataUi.setModel(model);
                assetUi.setModel(model);
            }

            @Override
            public void onApply() {
                final GwtConfigComponent newConfig = assetUi.getConfiguration();
                DriversAndAssetsRPC.deleteFactoryConfiguration(newConfig.getComponentId(),
                        result2 -> DriversAndAssetsRPC.createFactoryConfiguration(newConfig.getComponentId(),
                                newConfig.getFactoryId(), newConfig, result1 -> {
                                    configurations.setConfiguration(new GwtConfigComponent(newConfig));
                                    assetUi.setDirty(false);
                                }));
            }
        });
        return result;
    }

}
