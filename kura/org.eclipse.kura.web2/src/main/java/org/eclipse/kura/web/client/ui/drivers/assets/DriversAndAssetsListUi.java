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
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.Panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

public class DriversAndAssetsListUi extends Composite {

    private static DriversAndAssetsListUiUiBinder uiBinder = GWT.create(DriversAndAssetsListUiUiBinder.class);

    interface DriversAndAssetsListUiUiBinder extends UiBinder<Widget, DriversAndAssetsListUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final Logger logger = Logger.getLogger(EntryClassUi.class.getSimpleName());

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private DriverConfigUi driverConfigUi;
    private AssetMgmtUi assetMgmtUi;

    private final Panel driversAndAssetsMgmtPanel;

    @UiField
    Tree driversTree;

    public DriversAndAssetsListUi(Panel driversAndAssetsMgmtPanel) {
        initWidget(uiBinder.createAndBindUi(this));
        this.driversAndAssetsMgmtPanel = driversAndAssetsMgmtPanel;
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
                // load the drivers
                gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final List<String> result) {
                        DriversAndAssetsListUi.this.driversTree.clear();

                        if (result.isEmpty()) {
                            TreeItem root = new TreeItem();
                            root.setText(MSGS.noDriversAvailable());
                            DriversAndAssetsListUi.this.driversTree.addItem(root);
                        } else {
                            for (String tempDriverPid : result) {
                                Anchor driverConfigAnchor = initDriverConfigButton(tempDriverPid);
                                final TreeItem driverRoot = new TreeItem(driverConfigAnchor);
                                DriversAndAssetsListUi.this.driversTree.addItem(driverRoot);

                                gwtAssetService.getAssetInstancesByDriverPid(tempDriverPid,
                                        new AsyncCallback<List<String>>() {

                                    @Override
                                    public void onFailure(final Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                    }

                                    @Override
                                    public void onSuccess(final List<String> result) {
                                        for (String twinPid : result) {
                                            Anchor assetMgmtAnchor = initAssetMgmtButton(twinPid);
                                            final TreeItem assetRoot = new TreeItem(assetMgmtAnchor);
                                            driverRoot.addItem(assetRoot);
                                        }
                                    }
                                });
                            }
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

    private Anchor initDriverConfigButton(final String driverPid) {
        Anchor showDriverConfigAnchor = new Anchor();
        showDriverConfigAnchor.setText(driverPid);
        showDriverConfigAnchor.setTitle(driverPid);
        showDriverConfigAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtComponentService.findFilteredComponentConfiguration(token, driverPid,
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
            }
        });

        return showDriverConfigAnchor;
    }

    private Anchor initAssetMgmtButton(final String assetPid) {
        Anchor showAssetConfigAnchor = new Anchor();
        showAssetConfigAnchor.setText(assetPid);
        showAssetConfigAnchor.setTitle(assetPid);
        showAssetConfigAnchor.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
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
        });

        return showAssetConfigAnchor;
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
