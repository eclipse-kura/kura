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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ConfigurationAreaUi extends Composite {

    private static ConfigurationAreaUiUiBinder uiBinder = GWT.create(ConfigurationAreaUiUiBinder.class);

    interface ConfigurationAreaUiUiBinder extends UiBinder<Widget, ConfigurationAreaUi> {
    }

    private static final Logger logger = Logger.getLogger(ConfigurationAreaUi.class.getSimpleName());

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private final boolean isWireAsset;
    private final GwtConfigComponent configurableComponent;
    private final String pid;

    private GenericWireComponentUi genericWireComponentUi;
    private AssetConfigurationUi assetWireComponentUi;
    private boolean initialized;

    @UiField
    TabListItem tab1NavTab;
    @UiField
    TabListItem tab2NavTab;
    @UiField
    TabPane tab1Pane;
    @UiField
    TabPane tab2Pane;

    public ConfigurationAreaUi(final GwtConfigComponent addedItem, final String pid, final WiresPanelUi parent) {
        initWidget(uiBinder.createAndBindUi(this));
        this.initialized = false;

        this.pid = pid;
        this.configurableComponent = addedItem;
        this.isWireAsset = this.configurableComponent.getFactoryId() != null
                && this.configurableComponent.getFactoryId().contains("WireAsset");

        if (this.isWireAsset) {
            final String driverPid = this.configurableComponent.get(AssetConstants.ASSET_DRIVER_PROP.value())
                    .toString();

            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex, EntryClassUi.class.getName());
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    ConfigurationAreaUi.this.gwtComponentService.findFilteredComponentConfiguration(token, driverPid,
                            new AsyncCallback<List<GwtConfigComponent>>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            logger.log(Level.SEVERE, ex.getMessage(), ex);
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(List<GwtConfigComponent> result) {
                            for (GwtConfigComponent pair : result) {
                                ConfigurationAreaUi.this.genericWireComponentUi = new GenericWireComponentUi(pair,
                                        parent);
                            }

                            ConfigurationAreaUi.this.assetWireComponentUi = new AssetConfigurationUi(
                                    ConfigurationAreaUi.this.configurableComponent, parent);
                            ConfigurationAreaUi.this.tab1Pane.add(ConfigurationAreaUi.this.assetWireComponentUi);
                            ConfigurationAreaUi.this.tab1NavTab.setText(WiresPanelUi
                                    .getFormattedPid(ConfigurationAreaUi.this.configurableComponent.getFactoryId())
                                    + " - " + ConfigurationAreaUi.this.pid);

                            ConfigurationAreaUi.this.tab2NavTab.setVisible(true);
                            ConfigurationAreaUi.this.tab2NavTab.setText("Driver - " + driverPid);
                            ConfigurationAreaUi.this.tab2Pane.add(ConfigurationAreaUi.this.genericWireComponentUi);

                            ConfigurationAreaUi.this.initialized = true;
                        }
                    });
                }
            });
        } else {
            this.tab2NavTab.setVisible(false);
            this.genericWireComponentUi = new GenericWireComponentUi(this.configurableComponent, parent);
            this.tab1Pane.add(this.genericWireComponentUi);
            this.tab1NavTab.setText(
                    WiresPanelUi.getFormattedPid(this.configurableComponent.getFactoryId()) + " - " + this.pid);
            ConfigurationAreaUi.this.initialized = true;
        }
    }

    protected Map<String, GwtConfigComponent> getUpdatedConfiguration() {
        if (!this.initialized) {
            return Collections.emptyMap();
        }
        Map<String, GwtConfigComponent> updatedConfigs = new HashMap<>();
        if (this.isWireAsset) {
            final String driverPid = this.configurableComponent.get(AssetConstants.ASSET_DRIVER_PROP.value())
                    .toString();
            updatedConfigs.put(this.pid, this.assetWireComponentUi.getUpdatedConfiguration());
            updatedConfigs.put(driverPid, this.genericWireComponentUi.getUpdatedConfiguration());
        } else {
            updatedConfigs.put(this.pid, this.genericWireComponentUi.getUpdatedConfiguration());
        }
        return updatedConfigs;
    }

    protected void setGenericWireComponentUi(GenericWireComponentUi genericWireComponentUi) {
        this.genericWireComponentUi = genericWireComponentUi;
        this.tab2Pane.add(genericWireComponentUi);
    }

    public void setDirty(boolean dirty) {
        if (!this.initialized) {
            return;
        }
        if (this.isWireAsset) {
            this.assetWireComponentUi.setDirty(dirty);
        }
        this.genericWireComponentUi.setDirty(dirty);
    }

    public boolean isDirty() {
        if (!this.initialized) {
            return false;
        }
        if (this.isWireAsset) {
            return this.assetWireComponentUi.isDirty() || this.genericWireComponentUi.isDirty();
        } else {
            return this.genericWireComponentUi.isDirty();
        }
    }

    public void render() {
        if (!this.initialized) {
            return;
        }
        if (this.isWireAsset) {
            this.assetWireComponentUi.renderForm();
        }
        this.genericWireComponentUi.renderForm();
    }
}
