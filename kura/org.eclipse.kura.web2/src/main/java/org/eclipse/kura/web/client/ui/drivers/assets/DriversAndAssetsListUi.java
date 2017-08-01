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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtDriverAssetInfo;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultHeaderOrFooterBuilder;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
    private static final Logger logger = Logger.getLogger(EntryClassUi.class.getSimpleName());

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final ListDataProvider<GwtDriverAssetInfo> driversAssetsDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtDriverAssetInfo> selectionModel = new SingleSelectionModel<>();

    private final Panel driversAndAssetsMgmtPanel;

    private DriverConfigUi driverConfigUi;
    private AssetMgmtUi assetMgmtUi;

    @UiField
    Label emptyListLabel;
    @UiField
    CellTable<GwtDriverAssetInfo> driversAssetsListTable;

    public DriversAndAssetsListUi(Panel driversAndAssetsMgmtPanel) {
        initWidget(uiBinder.createAndBindUi(this));
        this.driversAndAssetsMgmtPanel = driversAndAssetsMgmtPanel;

        this.driversAssetsListTable.setSelectionModel(this.selectionModel);
        this.driversAssetsDataProvider.addDataDisplay(this.driversAssetsListTable);

        this.emptyListLabel.setText(MSGS.noDriversAvailable());

        initTable();

        this.selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final GwtDriverAssetInfo selectedInstanceEntry = DriversAndAssetsListUi.this.selectionModel
                        .getSelectedObject();

                if (selectedInstanceEntry.getType().equals("Driver")) {
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            gwtComponentService.findFilteredComponentConfiguration(token,
                                    selectedInstanceEntry.getInstancePid(),
                                    new AsyncCallback<List<GwtConfigComponent>>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                                    FailureHandler.handle(ex, EntryClassUi.class.getName());
                                }

                                @Override
                                public void onSuccess(List<GwtConfigComponent> result) {
                                    cleanConfigurationArea();
                                    for (GwtConfigComponent configuration : result) {
                                        DriversAndAssetsListUi.this.driverConfigUi = new DriverConfigUi(configuration);
                                        DriversAndAssetsListUi.this.driversAndAssetsMgmtPanel
                                                .add(DriversAndAssetsListUi.this.driverConfigUi);
                                        DriversAndAssetsListUi.this.driverConfigUi.renderForm();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    final String assetPid = selectedInstanceEntry.getInstancePid().substring(3);
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            gwtComponentService.findFilteredComponentConfiguration(token, assetPid,
                                    new AsyncCallback<List<GwtConfigComponent>>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                                    FailureHandler.handle(ex, EntryClassUi.class.getName());
                                }

                                @Override
                                public void onSuccess(List<GwtConfigComponent> result) {
                                    cleanConfigurationArea();

                                    for (GwtConfigComponent configuration : result) {
                                        DriversAndAssetsListUi.this.assetMgmtUi = new AssetMgmtUi(configuration);
                                        DriversAndAssetsListUi.this.driversAndAssetsMgmtPanel
                                                .add(DriversAndAssetsListUi.this.assetMgmtUi);
                                        DriversAndAssetsListUi.this.assetMgmtUi.refresh();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void initTable() {
        this.driversAssetsListTable.setHeaderBuilder(
                new DefaultHeaderOrFooterBuilder<GwtDriverAssetInfo>(this.driversAssetsListTable, false));

        final Column<GwtDriverAssetInfo, String> c2 = new Column<GwtDriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getValue(final GwtDriverAssetInfo object) {
                return object.getInstancePid();
            }
        };

        this.driversAssetsListTable.addColumn(c2, new TextHeader(MSGS.servicePidLabel()));

        final Column<GwtDriverAssetInfo, String> c3 = new Column<GwtDriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getValue(final GwtDriverAssetInfo object) {
                return object.getType();
            }
        };

        this.driversAssetsListTable.addColumn(c3, new TextHeader(MSGS.typeLabel()));

        final Column<GwtDriverAssetInfo, String> c4 = new Column<GwtDriverAssetInfo, String>(new TextCell()) {

            @Override
            public String getValue(final GwtDriverAssetInfo object) {
                return object.getFactoryPid();
            }
        };

        this.driversAssetsListTable.addColumn(c4, new TextHeader(MSGS.factoryPidLabel()));
    }

    public void refresh() {
        EntryClassUi.showWaitModal();

        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtAssetService.getDriverAssetInstances(token, new AsyncCallback<List<GwtDriverAssetInfo>>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final List<GwtDriverAssetInfo> result) {
                        if (result.isEmpty()) {
                            DriversAndAssetsListUi.this.emptyListLabel.setVisible(true);
                            DriversAndAssetsListUi.this.driversAssetsListTable.setVisible(false);
                        } else {
                            DriversAndAssetsListUi.this.emptyListLabel.setVisible(false);
                            DriversAndAssetsListUi.this.driversAssetsListTable.setVisible(true);

                            DriversAndAssetsListUi.this.driversAssetsDataProvider.getList().clear();
                            DriversAndAssetsListUi.this.driversAssetsDataProvider.getList().addAll(result);
                            DriversAndAssetsListUi.this.driversAssetsDataProvider.refresh();

                            int size = DriversAndAssetsListUi.this.driversAssetsDataProvider.getList().size();
                            DriversAndAssetsListUi.this.driversAssetsListTable.setVisibleRange(0, size);
                            DriversAndAssetsListUi.this.driversAssetsListTable.redraw();

                        }
                        cleanConfigurationArea();
                        EntryClassUi.hideWaitModal();
                    }

                });
            }
        });
    }

    private void cleanConfigurationArea() {
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
        if (this.driverConfigUi != null) {
            this.driverConfigUi.setDirty(dirty);
        }
        if (this.assetMgmtUi != null) {
            this.assetMgmtUi.setDirty(dirty);
        }
    }
}
