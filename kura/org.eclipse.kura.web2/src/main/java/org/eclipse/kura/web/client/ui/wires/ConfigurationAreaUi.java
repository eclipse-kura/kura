/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.ui.ConfigurableComponentUi;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetConfigurationUi;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetModel;
import org.eclipse.kura.web.client.ui.drivers.assets.AssetModelImpl;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ConfigurationAreaUi extends Composite {

    private static ConfigurationAreaUiUiBinder uiBinder = GWT.create(ConfigurationAreaUiUiBinder.class);

    interface ConfigurationAreaUiUiBinder extends UiBinder<Widget, ConfigurationAreaUi> {
    }

    private ConfigurableComponentUi genericWireComponentUi;
    private AssetConfigurationUi assetWireComponentUi;

    @UiField
    TabListItem tab1NavTab;
    @UiField
    TabListItem tab2NavTab;
    @UiField
    TabPane tab1Pane;
    @UiField
    TabPane tab2Pane;

    public ConfigurationAreaUi(HasConfiguration hasConfiguration, Configurations configurations) {
        initWidget(uiBinder.createAndBindUi(this));

        if (!initFromExistingUi(hasConfiguration)) {
            createNewUi(hasConfiguration, configurations);
        }

        if (this.assetWireComponentUi != null) {
            this.tab1Pane.add(ConfigurationAreaUi.this.assetWireComponentUi);
            this.tab1NavTab.setText(ConfigurationAreaUi.this.assetWireComponentUi.getTitle());

            ConfigurationAreaUi.this.tab2NavTab.setVisible(true);
            ConfigurationAreaUi.this.tab2NavTab.setText(ConfigurationAreaUi.this.genericWireComponentUi.getTitle());
            ConfigurationAreaUi.this.tab2Pane.add(ConfigurationAreaUi.this.genericWireComponentUi);

        } else {
            this.tab2NavTab.setVisible(false);
            this.tab1Pane.add(this.genericWireComponentUi);
            this.tab1NavTab.setText(this.genericWireComponentUi.getTitle());
        }
    }

    private boolean initFromExistingUi(HasConfiguration hasConfiguration) {
        if (hasConfiguration instanceof AssetConfigurationUi) {
            this.assetWireComponentUi = (AssetConfigurationUi) hasConfiguration;
            this.genericWireComponentUi = (ConfigurableComponentUi) this.assetWireComponentUi.getAssociatedView();
            return true;
        } else if (hasConfiguration instanceof ConfigurableComponentUi) {
            this.genericWireComponentUi = (ConfigurableComponentUi) hasConfiguration;
            return true;
        }
        return false;
    }

    private void createNewUi(HasConfiguration hasConfiguration, Configurations configurations) {
        final GwtConfigComponent configuration = hasConfiguration.getConfiguration();
        final String driverPid = configuration.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());

        if (driverPid != null) {
            final HasConfiguration driverConfiguration = configurations.getConfiguration(driverPid);

            if (driverConfiguration instanceof ConfigurableComponentUi) {
                this.genericWireComponentUi = (ConfigurableComponentUi) driverConfiguration;
            } else {
                this.genericWireComponentUi = new ConfigurableComponentUi(driverConfiguration.getConfiguration());
                this.genericWireComponentUi.setTitle("Driver - " + driverPid);
                this.genericWireComponentUi.renderForm();
            }

            final AssetModel assetModel = new AssetModelImpl(hasConfiguration.getConfiguration(),
                    configurations.getChannelDescriptor(driverPid), configurations.getBaseChannelDescriptor());

            this.assetWireComponentUi = new AssetConfigurationUi(assetModel, this.genericWireComponentUi,
                    configurations);
            this.assetWireComponentUi
                    .setTitle(WiresPanelUi.getComponentLabel(configuration) + " - " + configuration.getComponentId());
            this.assetWireComponentUi.renderForm();
        } else {
            this.genericWireComponentUi = new ConfigurableComponentUi(hasConfiguration.getConfiguration());
            this.genericWireComponentUi
                    .setTitle(WiresPanelUi.getComponentLabel(configuration) + " - " + configuration.getComponentId());
            this.genericWireComponentUi.renderForm();
        }
    }

    public void setListener(HasConfiguration.Listener listener) {
        if (this.assetWireComponentUi != null) {
            this.assetWireComponentUi.setListener(listener);
        }
        this.genericWireComponentUi.setListener(listener);
    }
}
