/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.ConfigurableComponentUi;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class DriversAndAssetsListUi extends Composite {

    private static DriversAndAssetsListUiUiBinder uiBinder = GWT.create(DriversAndAssetsListUiUiBinder.class);

    interface DriversAndAssetsListUiUiBinder extends UiBinder<Widget, DriversAndAssetsListUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final ListDataProvider<DriverAssetInfo> driversAssetsDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<DriverAssetInfo> selectionModel = new SingleSelectionModel<>();

    @UiField
    Well driversAndAssetsMgmtPanel;

    @UiField
    Label emptyListLabel;
    @UiField
    CellTable<DriverAssetInfo> driversAssetsListTable;
    @UiField
    PanelHeader contentPanelHeader;
    @UiField
    Panel configurationArea;

    private Listener listener;
    private Configurations configurations;

    private ConfigurableComponentUi driverConfigUi;
    private AssetMgmtUi assetMgmtUi;

    public DriversAndAssetsListUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.driversAssetsListTable.setSelectionModel(this.selectionModel);
        this.driversAssetsDataProvider.addDataDisplay(this.driversAssetsListTable);

        this.emptyListLabel.setText(MSGS.noDriversAvailable());

        initTable();

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final DriverAssetInfo selectedInstanceEntry = DriversAndAssetsListUi.this.selectionModel
                        .getSelectedObject();
                
                if(selectedInstanceEntry == null) {
                    return;
                }

                if (listener != null) {
                    listener.onSelectionChanged(selectedInstanceEntry);
                }

                cleanConfigurationArea();

                HasConfiguration hasConfiguration = configurations.getConfiguration(selectedInstanceEntry.getPid());

                if (hasConfiguration == null) {
                    return;
                }

                if (!selectedInstanceEntry.isAsset) {
                    ConfigurableComponentUi driverUi = new ConfigurableComponentUi(hasConfiguration.getConfiguration());
                    ConfigurationUiButtons buttonBar = createDriverConfigButtonBar(driverUi);
                    driverUi.renderForm();
                    driverConfigUi = driverUi;
                    contentPanelHeader.setText(MSGS.driverLabel(selectedInstanceEntry.getPid()));
                    driversAndAssetsMgmtPanel.add(buttonBar);
                    driversAndAssetsMgmtPanel.add(driverConfigUi);
                } else {
                    final AssetMgmtUi assetUi = new AssetMgmtUi(hasConfiguration, configurations);
                    assetMgmtUi = assetUi;
                    contentPanelHeader.setText(MSGS.assetLabel(selectedInstanceEntry.getPid()));
                    driversAndAssetsMgmtPanel.add(assetMgmtUi);
                }
                configurationArea.setVisible(true);
            }
        });
    }

    public void setConfigurations(Configurations configurations) {
        this.configurations = configurations;
    }

    private void initTable() {
        this.driversAssetsListTable.setHeaderBuilder(
                new DefaultHeaderOrFooterBuilder<DriverAssetInfo>(this.driversAssetsListTable, false));

        final Column<DriverAssetInfo, String> c2 = new Column<DriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getValue(final DriverAssetInfo object) {
                return object.isAsset ? " -> " + object.pid : object.pid;
            }
        };

        this.driversAssetsListTable.addColumn(c2, new TextHeader(MSGS.servicePidLabel()));

        final Column<DriverAssetInfo, String> c3 = new Column<DriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getValue(final DriverAssetInfo object) {
                return object.isAsset ? "Asset" : "Driver";
            }
        };

        this.driversAssetsListTable.addColumn(c3, new TextHeader(MSGS.typeLabel()));

        final Column<DriverAssetInfo, String> c4 = new Column<DriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getValue(final DriverAssetInfo object) {
                return object.factoryPid;
            }
        };

        this.driversAssetsListTable.addColumn(c4, new TextHeader(MSGS.factoryPidLabel()));
    }

    private void fillAssetEntries(String driverPid, Collection<HasConfiguration> configs,
            List<DriverAssetInfo> result) {
        for (HasConfiguration config : configs) {
            final GwtConfigComponent gwtConfig = config.getConfiguration();
            final String configDriverPid = gwtConfig.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());
            if (driverPid.equals(configDriverPid)) {
                result.add(new DriverAssetInfo(gwtConfig.getComponentId(), gwtConfig.getFactoryId(), true));
            }
        }
    }

    public void refresh() {
        final List<DriverAssetInfo> tableList = new ArrayList<>();
        final Collection<HasConfiguration> configs = configurations.getConfigurations();

        for (HasConfiguration config : configs) {
            final GwtConfigComponent gwtConfig = config.getConfiguration();
            if (gwtConfig.isDriver()) {
                final String driverPid = gwtConfig.getComponentId();
                tableList.add(new DriverAssetInfo(driverPid, gwtConfig.getFactoryId(), false));
                fillAssetEntries(driverPid, configs, tableList);
            }
        }

        if (tableList.isEmpty()) {
            this.emptyListLabel.setVisible(true);
            this.driversAssetsListTable.setVisible(false);
        } else {
            this.emptyListLabel.setVisible(false);
            this.driversAssetsListTable.setVisible(true);

            this.driversAssetsDataProvider.getList().clear();
            this.driversAssetsDataProvider.getList().addAll(tableList);
            this.driversAssetsDataProvider.refresh();

            int size = DriversAndAssetsListUi.this.driversAssetsDataProvider.getList().size();
            this.driversAssetsListTable.setVisibleRange(0, size);
            this.driversAssetsListTable.redraw();
        }
        this.selectionModel.clear();
        if (listener != null) {
            listener.onSelectionChanged(null);
        }
        cleanConfigurationArea();
    }

    private void cleanConfigurationArea() {
        this.configurationArea.setVisible(false);
        this.driversAndAssetsMgmtPanel.clear();
        this.driverConfigUi = null;
        this.assetMgmtUi = null;
    }

    public boolean isDirty() {
        boolean result = false;
        if (this.driverConfigUi != null) {
            result = this.driverConfigUi.isDirty();
        }
        if (this.assetMgmtUi != null) {
            result = result || this.assetMgmtUi.isDirty();
        }
        return result;
    }

    public void setDirty(boolean dirty) {
        if (dirty) {
            if (this.driverConfigUi != null) {
                this.driverConfigUi.setDirty(true);
            }
            if (this.assetMgmtUi != null) {
                this.assetMgmtUi.setDirty(true);
            }
        }
        if (!dirty) {
            this.driverConfigUi = null;
            this.assetMgmtUi = null;
            driversAndAssetsMgmtPanel.clear();
        }
    }

    public DriverAssetInfo getSelectedItem() {
        return this.selectionModel.getSelectedObject();
    }

    public static class DriverAssetInfo {

        private String pid;
        private String factoryPid;
        private boolean isAsset;

        public DriverAssetInfo(String pid, String factoryPid, boolean isAsset) {
            this.pid = pid;
            this.factoryPid = factoryPid;
            this.isAsset = isAsset;
        }

        public String getPid() {
            return pid;
        }

        public String getFactoryPid() {
            return factoryPid;
        }

        public boolean isAsset() {
            return isAsset;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {

        public void onSelectionChanged(DriverAssetInfo info);
    }

    private ConfigurationUiButtons createDriverConfigButtonBar(final ConfigurableComponentUi driverUi) {
        final GwtConfigComponent gwtConfig = driverUi.getConfiguration();
        final ConfigurationUiButtons result = new ConfigurationUiButtons(driverUi);
        result.setListener(new ConfigurationUiButtons.Listener() {

            @Override
            public void onReset() {
                driverUi.setConfiguration(
                        configurations.getConfiguration(gwtConfig.getComponentId()).getConfiguration());
                driverUi.renderForm();
            }

            @Override
            public void onApply() {
                final GwtConfigComponent configuration = driverUi.getConfiguration();
                DriversAndAssetsRPC.updateConfiguration(configuration, new DriversAndAssetsRPC.Callback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        configurations.setConfiguration(configuration);
                        driverUi.setDirty(false);
                    }
                });
            }
        });
        return result;
    }

}
