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
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtAssetService;
import org.eclipse.kura.web.shared.service.GwtAssetServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class DriversAndAssetsUi extends Composite {

    private static DriversAndAssetsUiUiBinder uiBinder = GWT.create(DriversAndAssetsUiUiBinder.class);

    interface DriversAndAssetsUiUiBinder extends UiBinder<Widget, DriversAndAssetsUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final Logger logger = Logger.getLogger(EntryClassUi.class.getSimpleName());

    private static final String SELECT_COMPONENT = MSGS.servicesComponentFactorySelectorIdle();
    private static final String ASSET_FACTORY_PID = "org.eclipse.kura.wire.WireAsset";

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private final DriversAndAssetsListUi driverAndAssetsListUi;

    @UiField
    HTMLPanel driversAndAssetsIntro;

    @UiField
    Button newDriverButton;
    @UiField
    Button deleteDriverButton;
    @UiField
    Button newAssetButton;
    @UiField
    Button deleteAssetButton;
    @UiField
    Panel driversAndAssetsList;
    @UiField
    Panel driversAndAssetsMgmtPanel;

    @UiField
    Modal newDriverModal;
    @UiField
    HTMLPanel newDriverModalIntro;
    @UiField
    FormLabel newDriverFactoryFormLabel;
    @UiField
    ListBox driverFactoriesList;
    @UiField
    FormLabel driverInstanceNameLabel;
    @UiField
    TextBox driverName;
    @UiField
    Button buttonNewDriverCancel;
    @UiField
    Button buttonNewDriverApply;

    @UiField
    Modal newAssetModal;
    @UiField
    HTMLPanel newAssetModalIntro;
    @UiField
    Form newAssetForm;
    @UiField
    FormLabel assetInstanceNameLabel;
    @UiField
    TextBox assetName;
    @UiField
    FormLabel driverPidLabel;
    @UiField
    ListBox driverPid;
    @UiField
    Button buttonNewAssetCancel;
    @UiField
    Button buttonNewAssetApply;

    @UiField
    Modal deleteDriverModal;
    @UiField
    HTMLPanel deleteDriverModalIntro;
    @UiField
    FormLabel deleteDriverInstanceNameLabel;
    @UiField
    ListBox deleteDriverInstancesList;
    @UiField
    Button buttonDeleteDriverCancel;
    @UiField
    Button buttonDeleteDriverApply;

    @UiField
    Modal deleteAssetModal;
    @UiField
    HTMLPanel deleteAssetModalIntro;
    @UiField
    FormLabel deleteAssetPidLabel;
    @UiField
    ListBox deleteAssetPid;
    @UiField
    Button buttonDeleteAssetCancel;
    @UiField
    Button buttonDeleteAssetApply;

    public DriversAndAssetsUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.driversAndAssetsIntro.add(new Span("<p>" + MSGS.driversAssetsTabIntro() + "</p>"));

        initButtonBar();
        initNewDriverModal();
        initNewAssetModal();
        initDeleteDriverModal();
        initDeleteAssetModal();

        this.driverAndAssetsListUi = new DriversAndAssetsListUi(this.driversAndAssetsMgmtPanel);

        this.driversAndAssetsList.add(this.driverAndAssetsListUi);
    }

    public void refresh() {
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(List<String> result) {
                        if (result.isEmpty()) {
                            DriversAndAssetsUi.this.deleteDriverButton.setEnabled(false);
                            DriversAndAssetsUi.this.newAssetButton.setEnabled(false);
                            DriversAndAssetsUi.this.deleteAssetButton.setEnabled(false);
                            DriversAndAssetsUi.this.driverAndAssetsListUi.refresh();
                        } else {
                            gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(ex);
                                }

                                @Override
                                public void onSuccess(GwtXSRFToken token) {
                                    gwtAssetService.getAssetInstances(token, new AsyncCallback<List<String>>() {

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            EntryClassUi.hideWaitModal();
                                            FailureHandler.handle(caught);
                                        }

                                        @Override
                                        public void onSuccess(List<String> result) {
                                            DriversAndAssetsUi.this.deleteDriverButton.setEnabled(true);
                                            DriversAndAssetsUi.this.newAssetButton.setEnabled(true);
                                            if (result.isEmpty()) {
                                                DriversAndAssetsUi.this.deleteAssetButton.setEnabled(false);
                                            } else {
                                                DriversAndAssetsUi.this.deleteAssetButton.setEnabled(true);
                                            }
                                            DriversAndAssetsUi.this.driverAndAssetsListUi.refresh();
                                        }
                                    });
                                }
                            });
                        }

                    }
                });
            }
        });
    }

    public boolean isDirty() {
        return this.driverAndAssetsListUi.isDirty();
    }

    public void setDirty(boolean dirty) {
        this.driverAndAssetsListUi.setDirty(dirty);
    }

    private void initButtonBar() {
        this.newDriverButton.setText(MSGS.newDriver());
        this.newDriverButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DriversAndAssetsUi.this.driverName.setValue("");
                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtComponentService.getDriverFactoriesList(token, new AsyncCallback<List<String>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(final List<String> result) {
                                DriversAndAssetsUi.this.driverFactoriesList.clear();
                                DriversAndAssetsUi.this.driverFactoriesList.addItem(SELECT_COMPONENT);
                                for (String driverFactoryPid : result) {
                                    DriversAndAssetsUi.this.driverFactoriesList.addItem(driverFactoryPid);
                                }
                                DriversAndAssetsUi.this.newDriverModal.show();
                            }
                        });
                    }
                });
            }
        });

        this.deleteDriverButton.setText(MSGS.deleteDriver());
        this.deleteDriverButton.setEnabled(false);
        this.deleteDriverButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                EntryClassUi.showWaitModal();
                deleteDriverInstance();
            }
        });

        this.newAssetButton.setText(MSGS.newAsset());
        this.newAssetButton.setEnabled(false);
        this.newAssetButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                DriversAndAssetsUi.this.assetName.setValue("");
                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(List<String> result) {
                                DriversAndAssetsUi.this.driverPid.clear();
                                for (String pid : result) {
                                    DriversAndAssetsUi.this.driverPid.addItem(pid);
                                }
                            }
                        });
                    }
                });
                DriversAndAssetsUi.this.newAssetModal.show();
            }
        });

        this.deleteAssetButton.setText(MSGS.deleteAsset());
        this.deleteAssetButton.setEnabled(false);
        this.deleteAssetButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                EntryClassUi.showWaitModal();
                deleteAssetInstance();
            }
        });
    }

    private void deleteDriverInstance() {
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(List<String> result) {
                        DriversAndAssetsUi.this.deleteDriverInstancesList.clear();
                        for (String tempDriverPid : result) {
                            DriversAndAssetsUi.this.deleteDriverInstancesList.addItem(tempDriverPid);
                        }

                        EntryClassUi.hideWaitModal();
                        DriversAndAssetsUi.this.deleteDriverModal.show();
                    }
                });
            }
        });
    }

    private void deleteAssetInstance() {
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtAssetService.getAssetInstances(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final List<String> assetPids) {
                        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                gwtWireService.getWiresConfiguration(token, new AsyncCallback<GwtWiresConfiguration>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                    }

                                    @Override
                                    public void onSuccess(GwtWiresConfiguration gwtWiresConfiguration) {
                                        DriversAndAssetsUi.this.deleteAssetPid.clear();
                                        List<String> composerWireComponentPids = gwtWiresConfiguration
                                                .getWireComponentPids();

                                        for (String assetPid : assetPids) {
                                            if (!composerWireComponentPids.contains(assetPid)) {
                                                DriversAndAssetsUi.this.deleteAssetPid.addItem(assetPid);
                                            }
                                        }

                                        EntryClassUi.hideWaitModal();
                                        DriversAndAssetsUi.this.deleteAssetModal.show();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void initNewDriverModal() {
        this.newDriverModal.setTitle(MSGS.createNewDriverLabel());
        this.newDriverModalIntro.add(new Span("<p>" + MSGS.createNewDriverIntroLabel() + "</p>"));
        this.newDriverFactoryFormLabel.setText(MSGS.driverFactory());
        this.driverInstanceNameLabel.setText(MSGS.driverName());
        this.buttonNewDriverCancel.setText(MSGS.cancelButton());
        this.buttonNewDriverApply.setText(MSGS.apply());

        this.buttonNewDriverApply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (DriversAndAssetsUi.this.driverName.validate()) {
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            String factoryPid = DriversAndAssetsUi.this.driverFactoriesList.getSelectedValue();
                            String pid = DriversAndAssetsUi.this.driverName.getValue();

                            gwtComponentService.createFactoryComponent(token, factoryPid, pid,
                                    new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                                    FailureHandler.handle(ex, EntryClassUi.class.getName());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    DriversAndAssetsUi.this.newDriverModal.hide();
                                    DriversAndAssetsUi.this.deleteDriverButton.setEnabled(true);
                                    DriversAndAssetsUi.this.newAssetButton.setEnabled(true);
                                    refresh();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void initNewAssetModal() {
        this.newAssetModal.setTitle(MSGS.createNewAssetLabel());
        this.newAssetModalIntro.add(new Span("<p>" + MSGS.createNewAssetIntroLabel() + "</p>"));
        this.assetInstanceNameLabel.setText(MSGS.assetName());
        this.driverPidLabel.setText(MSGS.driverName());
        this.buttonNewAssetCancel.setText(MSGS.cancelButton());
        this.buttonNewAssetApply.setText(MSGS.apply());

        this.buttonNewAssetApply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (DriversAndAssetsUi.this.assetName.validate()) {
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            GwtConfigComponent twinConfig = new GwtConfigComponent();
                            twinConfig.set("asset.desc", "Simple Asset");
                            twinConfig.set("driver.pid", DriversAndAssetsUi.this.driverPid.getSelectedValue());

                            gwtComponentService.createFactoryComponent(token, ASSET_FACTORY_PID,
                                    DriversAndAssetsUi.this.assetName.getValue(), twinConfig,
                                    new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                                    FailureHandler.handle(ex, EntryClassUi.class.getName());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    DriversAndAssetsUi.this.newAssetModal.hide();
                                    DriversAndAssetsUi.this.deleteAssetButton.setEnabled(true);
                                    refresh();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void initDeleteDriverModal() {
        this.deleteDriverModal.setTitle(MSGS.deleteDriverLabel());
        this.deleteDriverModalIntro.add(new Span("<p>" + MSGS.deleteDriverIntroLabel() + "</p>"));
        this.deleteDriverInstanceNameLabel.setText(MSGS.driverName());
        this.buttonDeleteDriverCancel.setText(MSGS.cancelButton());
        this.buttonDeleteDriverApply.setText(MSGS.apply());
        this.buttonDeleteDriverApply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtComponentService.deleteFactoryConfiguration(token,
                                DriversAndAssetsUi.this.deleteDriverInstancesList.getSelectedValue(), true,
                                new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                DriversAndAssetsUi.this.deleteDriverModal.hide();
                                refresh();
                            }
                        });
                    }
                });
            }

        });
    }

    private void initDeleteAssetModal() {
        this.deleteAssetModal.setTitle(MSGS.deleteAssetLabel());
        this.deleteAssetModalIntro.add(new Span("<p>" + MSGS.deleteAssetIntroLabel() + "</p>"));
        this.deleteAssetPidLabel.setText(MSGS.assetName());
        this.buttonDeleteAssetCancel.setText(MSGS.cancelButton());
        this.buttonDeleteAssetApply.setText(MSGS.apply());
        this.buttonDeleteAssetApply.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtComponentService.deleteFactoryConfiguration(token,
                                DriversAndAssetsUi.this.deleteAssetPid.getSelectedValue(), true,
                                new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                DriversAndAssetsUi.this.deleteAssetModal.hide();
                                refresh();
                            }
                        });
                    }
                });
            }

        });
    }
}
