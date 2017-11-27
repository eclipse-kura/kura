/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

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

    private GenericWireComponentUi genericWireComponentUi;
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
            final GwtConfigComponent configuration = assetWireComponentUi.getConfiguration();
            final String driverPid = configuration.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());

            this.tab1Pane.add(ConfigurationAreaUi.this.assetWireComponentUi);
            this.tab1NavTab.setText(WiresPanelUi.getFormattedPid(configuration.getFactoryId()) + " - "
                    + configuration.getComponentId());

            ConfigurationAreaUi.this.tab2NavTab.setVisible(true);
            ConfigurationAreaUi.this.tab2NavTab.setText("Driver - " + driverPid);
            ConfigurationAreaUi.this.tab2Pane.add(ConfigurationAreaUi.this.genericWireComponentUi);

        } else {
            final GwtConfigComponent configuration = genericWireComponentUi.getConfiguration();
            this.tab2NavTab.setVisible(false);
            this.genericWireComponentUi = new GenericWireComponentUi(configuration);
            this.tab1Pane.add(this.genericWireComponentUi);
            this.tab1NavTab.setText(WiresPanelUi.getFormattedPid(configuration.getFactoryId()) + " - "
                    + configuration.getComponentId());
        }
    }

    private boolean initFromExistingUi(HasConfiguration hasConfiguration) {
        if (hasConfiguration instanceof AssetConfigurationUi) {
            this.assetWireComponentUi = (AssetConfigurationUi) hasConfiguration;
            this.genericWireComponentUi = this.assetWireComponentUi.getDriverConfigurationUi();
            return true;
        } else if (hasConfiguration instanceof GenericWireComponentUi) {
            this.genericWireComponentUi = (GenericWireComponentUi) hasConfiguration;
            return true;
        }
        return false;
    }

    private void createNewUi(HasConfiguration hasConfiguration, Configurations configurations) {
        final GwtConfigComponent configuration = hasConfiguration.getConfiguration();
        final String driverPid = configuration.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());

        if (driverPid != null) {
            final HasConfiguration driverConfiguration = configurations.getConfiguration(driverPid);

            if (driverConfiguration instanceof GenericWireComponentUi) {
                this.genericWireComponentUi = (GenericWireComponentUi) driverConfiguration;
            } else {
                this.genericWireComponentUi = new GenericWireComponentUi(driverConfiguration.getConfiguration());
            }

            final AssetModel assetModel = new LegacyAssetModel(hasConfiguration.getConfiguration(),
                    configurations.getChannelDescriptor(driverPid), configurations.getBaseChannelDescriptor());

            this.assetWireComponentUi = new AssetConfigurationUi(assetModel, this.genericWireComponentUi);
        } else {
            this.genericWireComponentUi = new GenericWireComponentUi(hasConfiguration.getConfiguration());
        }
    }

    public void setDirty(boolean dirty) {
        if (this.assetWireComponentUi != null) {
            this.assetWireComponentUi.setDirty(dirty);
        }
        this.genericWireComponentUi.setDirty(dirty);
    }

    public boolean isDirty() {
        if (this.assetWireComponentUi != null) {
            return this.assetWireComponentUi.isDirty() || this.genericWireComponentUi.isDirty();
        } else {
            return this.genericWireComponentUi.isDirty();
        }
    }

    public void render() {
        if (this.assetWireComponentUi != null) {
            this.assetWireComponentUi.renderForm();
        }
        this.genericWireComponentUi.renderForm();
    }

    public void setListener(ConfigurationChangeListener listener) {
        if (this.assetWireComponentUi != null) {
            this.assetWireComponentUi.setListener(listener);
        }
        this.genericWireComponentUi.setListener(listener);
    }
}
