/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
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
import java.util.List;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.drivers.assets.DriversAndAssetsListUi.DriverAssetInfo;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class DriversAndAssetsUi extends Composite implements DriversAndAssetsListUi.Listener {

    private static DriversAndAssetsUiUiBinder uiBinder = GWT.create(DriversAndAssetsUiUiBinder.class);

    interface DriversAndAssetsUiUiBinder extends UiBinder<Widget, DriversAndAssetsUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private static final String SELECT_COMPONENT = MSGS.servicesComponentFactorySelectorIdle();
    private static final String ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    @UiField
    DriversAndAssetsListUi driverAndAssetsListUi;

    @UiField
    Button newDriverButton;
    @UiField
    Button newAssetButton;
    @UiField
    Button deleteButton;

    @UiField
    Modal newDriverModal;
    @UiField
    ListBox driverFactoriesList;
    @UiField
    PidTextBox driverName;
    @UiField
    Button buttonNewDriverCancel;
    @UiField
    Button buttonNewDriverApply;

    @UiField
    Modal newAssetModal;
    @UiField
    PidTextBox assetName;
    @UiField
    TextBox driverPid;
    @UiField
    Button buttonNewAssetCancel;
    @UiField
    Button buttonNewAssetApply;

    @UiField
    AlertDialog confirmDialog;

    private Configurations configurations = new Configurations();

    public DriversAndAssetsUi() {
        initWidget(uiBinder.createAndBindUi(this));

        initButtonBar();
        initNewDriverModal();
        initNewAssetModal();

        this.driverAndAssetsListUi.setConfigurations(configurations);
        this.driverAndAssetsListUi.setListener(this);
    }

    public void refresh() {
        configurations.clear();
        DriversAndAssetsRPC.loadWireGraph(result -> {
            final GwtWireComposerStaticInfo staticInfo = result.getStaticInfo();

            configurations.setChannelDescriptiors(staticInfo.getDriverDescriptors());
            configurations.setBaseChannelDescriptor(staticInfo.getBaseChannelDescriptor());
            configurations.setComponentDefinitions(staticInfo.getComponentDefinitions());

            final GwtWireGraphConfiguration wireConfigurations = result.getWireGraphConfiguration();

            final List<GwtConfigComponent> configurationList = new ArrayList<>();

            for (GwtWireComponentConfiguration config : wireConfigurations.getWireComponentConfigurations()) {
                configurationList.add(config.getConfiguration());
            }

            configurationList.addAll(wireConfigurations.getAdditionalConfigurations());

            configurations.setComponentConfigurations(configurationList);
            configurations.setAllActivePids(wireConfigurations.getAllActivePids());

            init();
        });
    }

    private void init() {
        DriversAndAssetsUi.this.driverFactoriesList.clear();
        DriversAndAssetsUi.this.driverFactoriesList.addItem(SELECT_COMPONENT);
        for (String driverFactoryPid : configurations.getDriverFactoryPids()) {
            DriversAndAssetsUi.this.driverFactoriesList.addItem(driverFactoryPid);
        }

        clearDirtyState();
        driverAndAssetsListUi.refresh();
    }

    public void clearDirtyState() {
        this.driverAndAssetsListUi.setDirty(false);
    }

    public boolean isDirty() {
        return driverAndAssetsListUi.isDirty();
    }

    private void initButtonBar() {

        this.newDriverButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DriversAndAssetsUi.this.driverName.setValue("");
                DriversAndAssetsUi.this.newDriverModal.show();
            }
        });

        this.newAssetButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DriversAndAssetsUi.this.driverPid.setValue(driverAndAssetsListUi.getSelectedItem().getPid());
                DriversAndAssetsUi.this.newAssetModal.show();
            }
        });

        this.deleteButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                final DriverAssetInfo info = driverAndAssetsListUi.getSelectedItem();

                if (info == null) {
                    return;
                }

                if (info.isAsset()) {
                    deleteAsset(info.getPid());
                } else {
                    deleteDriver(info.getPid());
                }
            }
        });
    }

    private void deleteComponent(final String pid) {
        DriversAndAssetsRPC.deleteFactoryConfiguration(pid, new DriversAndAssetsRPC.Callback<Void>() {

            @Override
            public void onSuccess(Void result) {
                configurations.deleteConfiguration(pid);
                driverAndAssetsListUi.refresh();
            }
        });
    }

    private void deleteDriver(final String pid) {

        for (HasConfiguration hasConfiguration : configurations.getConfigurations()) {
            final GwtConfigComponent gwtConfig = hasConfiguration.getConfiguration();
            final String driverPid = gwtConfig.getParameterValue(AssetConstants.ASSET_DRIVER_PROP.value());
            if (pid.equals(driverPid)) {
                confirmDialog.show(MSGS.driversAssetsDeletingDriverWithAssets(), AlertDialog.Severity.ALERT, null);
                return;
            }
        }

        confirmDialog.show(MSGS.driversAssetsConfirmDeleteDriver(), new AlertDialog.Listener() {

            @Override
            public void onConfirm() {
                deleteComponent(pid);
            }
        });
    }

    private void deleteAsset(final String pid) {
        final HasConfiguration config = this.configurations.getConfiguration(pid);
        final GwtConfigComponent gwtConfig = config.getConfiguration();

        if (gwtConfig.isWireComponent()) {
            confirmDialog.show(MSGS.driversAssetsAssetInComposer(), AlertDialog.Severity.ALERT, null);
            return;
        }

        confirmDialog.show(MSGS.driversAssetsConfirmDeleteAsset(), new AlertDialog.Listener() {

            @Override
            public void onConfirm() {
                deleteComponent(pid);
            }
        });
    }

    private void createAsset(final String pid, final String driverPid) {
        final HasConfiguration assetConfig = configurations.createConfiguration(pid, ASSET_FACTORY_PID);
        assetConfig.getConfiguration().getParameter(AssetConstants.ASSET_DRIVER_PROP.value()).setValue(driverPid);
        DriversAndAssetsRPC.createFactoryConfiguration(pid, ASSET_FACTORY_PID, assetConfig.getConfiguration(),
                new DriversAndAssetsRPC.Callback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        configurations.setConfiguration(assetConfig.getConfiguration());
                        newAssetModal.hide();
                        driverAndAssetsListUi.refresh();
                    }
                });
    }

    private void initNewDriverModal() {
        this.buttonNewDriverApply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                final String pid = DriversAndAssetsUi.this.driverName.getPid();

                if (pid == null) {
                    return;
                }

                if (driverFactoriesList.getSelectedIndex() == 0) {
                    confirmDialog.show(MSGS.driversAssetsInvalidDriverFactory(), AlertDialog.Severity.ALERT, null);
                    return;
                }

                if (configurations.isPidExisting(pid)) {
                    confirmDialog.show(MSGS.wiresComponentNameAlreadyUsed(pid), AlertDialog.Severity.ALERT, null);
                    return;
                }

                final String factoryPid = DriversAndAssetsUi.this.driverFactoriesList.getSelectedValue();

                DriversAndAssetsRPC.createNewDriver(factoryPid, pid,
                        new DriversAndAssetsRPC.Callback<GwtConfigComponent>() {

                            @Override
                            public void onSuccess(GwtConfigComponent result) {
                                configurations.createAndRegisterConfiguration(pid, factoryPid);
                                configurations.setChannelDescriptor(pid, result);
                                newDriverModal.hide();
                                driverAndAssetsListUi.refresh();
                            }
                        });
            }
        });
    }

    private void initNewAssetModal() {

        this.buttonNewAssetApply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                final String pid = assetName.getPid();

                if (pid == null) {
                    return;
                }

                if (configurations.isPidExisting(pid)) {
                    confirmDialog.show(MSGS.wiresComponentNameAlreadyUsed(pid), AlertDialog.Severity.ALERT, null);
                    return;
                }

                final String newDriverPid = driverAndAssetsListUi.getSelectedItem().getPid();

                createAsset(pid, newDriverPid);
            }
        });
    }

    @Override
    public void onSelectionChanged(DriverAssetInfo info) {
        if (info != null) {
            this.deleteButton.setEnabled(true);
            this.newAssetButton.setEnabled(!info.isAsset() && info.isValid());
        } else {
            this.deleteButton.setEnabled(false);
            this.newAssetButton.setEnabled(false);
        }
    }

}
