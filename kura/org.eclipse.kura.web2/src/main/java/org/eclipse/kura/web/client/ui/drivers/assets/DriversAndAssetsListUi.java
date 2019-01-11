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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.ConfigurableComponentUi;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.constants.AlertType;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.Cell.Context;
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
import com.google.gwt.view.client.SingleSelectionModel;

public class DriversAndAssetsListUi extends Composite {

    private static final String CELL_NOT_VALID = "cell-not-valid";

    private static DriversAndAssetsListUiUiBinder uiBinder = GWT.create(DriversAndAssetsListUiUiBinder.class);

    interface DriversAndAssetsListUiUiBinder extends UiBinder<Widget, DriversAndAssetsListUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

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

        initSelectionModel();
    }

    private void initSelectionModel() {
        this.selectionModel.addSelectionChangeHandler(event -> {
            final DriverAssetInfo selectedInstanceEntry = DriversAndAssetsListUi.this.selectionModel
                    .getSelectedObject();

            if (listener != null) {
                listener.onSelectionChanged(selectedInstanceEntry);
            }

            cleanConfigurationArea();

            if (selectedInstanceEntry == null) {
                return;
            }

            final String pid = selectedInstanceEntry.getPid();

            HasConfiguration hasConfiguration = configurations.getConfiguration(pid);

            if (hasConfiguration == null) {
                showWarning(selectedInstanceEntry.getPid(), MSGS.errorComponentConfigurationMissing(pid));
            } else {

                fillWithConfiguration(selectedInstanceEntry, pid, hasConfiguration);
            }
            configurationArea.setVisible(true);
        });
    }

    private void fillWithConfiguration(final DriverAssetInfo selectedInstanceEntry, final String pid,
            HasConfiguration hasConfiguration) {
        if (!selectedInstanceEntry.isAsset()) {
            ConfigurableComponentUi driverUi = new ConfigurableComponentUi(
                    hasConfiguration.getConfiguration());
            ConfigurationUiButtons buttonBar = createDriverConfigButtonBar(driverUi);
            driverUi.renderForm();
            driverConfigUi = driverUi;
            contentPanelHeader.setText(MSGS.driverLabel(selectedInstanceEntry.getPid()));
            driversAndAssetsMgmtPanel.add(buttonBar);
            if (selectedInstanceEntry.getChannelDescriptor() == null) {
                showWarning(null, MSGS.errorDriverDescriptorMissing(pid));
            }
            driversAndAssetsMgmtPanel.add(driverConfigUi);
        } else if (selectedInstanceEntry.getChannelDescriptor() != null) {
            final AssetMgmtUi assetUi = new AssetMgmtUi(hasConfiguration, configurations);
            assetMgmtUi = assetUi;
            contentPanelHeader.setText(MSGS.assetLabel(selectedInstanceEntry.getPid()));
            driversAndAssetsMgmtPanel.add(assetMgmtUi);
        } else {
            showWarning(MSGS.assetLabel(selectedInstanceEntry.getPid()),
                    MSGS.errorDriverDescriptorMissingForAsset(pid, selectedInstanceEntry.getDriverPid()));
        }
    }

    private void showWarning(String caption, String message) {
        if (caption != null) {
            contentPanelHeader.setText(caption);
        }
        driversAndAssetsMgmtPanel.add(new Alert(message, AlertType.DANGER));
    }

    public void setConfigurations(Configurations configurations) {
        this.configurations = configurations;
    }

    private void initTable() {
        this.driversAssetsListTable.setHeaderBuilder(
                new DefaultHeaderOrFooterBuilder<DriverAssetInfo>(this.driversAssetsListTable, false));

        final Column<DriverAssetInfo, String> c2 = new Column<DriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getCellStyleNames(Context context, DriverAssetInfo object) {
                return object.isValid() ? null : CELL_NOT_VALID;
            }

            @Override
            public String getValue(final DriverAssetInfo object) {
                return object.isAsset() ? " -> " + object.pid : object.pid;
            }
        };

        this.driversAssetsListTable.addColumn(c2, new TextHeader(MSGS.servicePidLabel()));

        final Column<DriverAssetInfo, String> c3 = new Column<DriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getCellStyleNames(Context context, DriverAssetInfo object) {
                return object.isValid() ? null : CELL_NOT_VALID;
            }

            @Override
            public String getValue(final DriverAssetInfo object) {
                return object.isAsset() ? "Asset" : "Driver";
            }
        };

        this.driversAssetsListTable.addColumn(c3, new TextHeader(MSGS.typeLabel()));

        final Column<DriverAssetInfo, String> c4 = new Column<DriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getCellStyleNames(Context context, DriverAssetInfo object) {
                return object.isValid() ? null : CELL_NOT_VALID;
            }

            @Override
            public String getValue(final DriverAssetInfo object) {
                final String factoryPid = object.getFactoryPid();
                return factoryPid != null ? factoryPid : "Unknown";
            }
        };

        this.driversAssetsListTable.addColumn(c4, new TextHeader(MSGS.factoryPidLabel()));
    }

    private List<DriverAssetInfo> getAssetsForDriver(final Map<String, List<DriverAssetInfo>> map,
            final String driverPid) {
        return map.computeIfAbsent(driverPid, pid -> new ArrayList<DriverAssetInfo>());
    }

    private DriverAssetInfo getDriverEntry(String driverPid) {
        final HasConfiguration driverConfig = configurations.getConfiguration(driverPid);
        if (driverConfig == null) {
            return new DriverAssetInfo(driverPid);
        } else {
            return new DriverAssetInfo(driverConfig);
        }
    }

    private List<DriverAssetInfo> buildTableList() {
        final List<DriverAssetInfo> result = new ArrayList<>();
        final Map<String, List<DriverAssetInfo>> grouped = new HashMap<>();

        final Collection<HasConfiguration> configs = configurations.getConfigurations();

        for (HasConfiguration config : configs) {
            final DriverAssetInfo entry = new DriverAssetInfo(config);

            if (entry.isAsset()) {
                getAssetsForDriver(grouped, entry.getDriverPid()).add(entry);
            } else if (config.getConfiguration().isDriver()) {
                getAssetsForDriver(grouped, entry.getPid());
            }
        }

        for (Entry<String, List<DriverAssetInfo>> e : grouped.entrySet()) {
            result.add(getDriverEntry(e.getKey()));
            result.addAll(e.getValue());
        }

        return result;
    }

    public void refresh() {
        final List<DriverAssetInfo> tableList = buildTableList();

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

    public void setListener(Listener listener) {
        this.listener = listener;
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
                DriversAndAssetsRPC.updateConfiguration(configuration, result1 -> {
                    configurations.setConfiguration(configuration);
                    driverUi.setDirty(false);
                });
            }
        });
        return result;
    }

    public interface Listener {

        public void onSelectionChanged(DriverAssetInfo info);
    }

    public class DriverAssetInfo {

        private final String pid;
        private final String factoryPid;
        private final String driverPid;
        private GwtConfigComponent channelDescriptor;

        public DriverAssetInfo(final String pid) {
            this.pid = pid;
            this.factoryPid = null;
            this.driverPid = null;
        }

        public DriverAssetInfo(final HasConfiguration hasConfig) {
            final GwtConfigComponent config = hasConfig.getConfiguration();
            this.pid = config.getComponentId();
            this.factoryPid = config.getFactoryId();
            this.driverPid = config.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());
            this.channelDescriptor = configurations.getChannelDescriptor(isAsset() ? getDriverPid() : pid);
        }

        public String getPid() {
            return pid;
        }

        public String getFactoryPid() {
            return factoryPid;
        }

        public String getDriverPid() {
            return driverPid;
        }

        public boolean isAsset() {
            return driverPid != null;
        }

        public GwtConfigComponent getChannelDescriptor() {
            return channelDescriptor;
        }

        public boolean isValid() {
            return this.factoryPid != null && this.channelDescriptor != null;
        }
    }

}
