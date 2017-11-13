/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.DropSupport;
import org.eclipse.kura.web.client.util.DropSupport.DropEvent;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.AssetConstants;
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
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Heading;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite {

    @UiField
    Button btnSave;
    @UiField
    Button btnReset;
    @UiField
    Button btnZoomIn;
    @UiField
    Button btnZoomOut;
    @UiField
    Button btnZoomFit;
    @UiField
    Button btnDelete;
    @UiField
    Button btnGraphDelete;

    @UiField
    FormLabel componentNameLabel;
    @UiField
    TextBox componentName;

    @UiField
    Modal genericCompModal;
    @UiField
    ModalHeader newAssetModalHeader;

    @UiField
    Button btnComponentModalYes;
    @UiField
    Button btnComponentModalNo;

    @UiField
    Modal saveGraphModal;
    @UiField
    ModalHeader saveGraphModalHeader;
    @UiField
    ModalBody saveGraphModalBody;
    @UiField
    Button btnSaveGraphModalYes;
    @UiField
    Button btnSaveGraphModalNo;

    @UiField
    Modal deleteGraphModal;
    @UiField
    ModalHeader deleteGraphModalHeader;
    @UiField
    ModalBody deleteGraphModalBody;
    @UiField
    Button btnDeleteGraphModalYes;
    @UiField
    Button btnDeleteGraphModalNo;

    @UiField
    Modal deleteCompModal;
    @UiField
    ModalHeader deleteCompModalHeader;
    @UiField
    ModalBody deleteCompModalBody;
    @UiField
    Button btnDeleteCompModalYes;
    @UiField
    Button btnDeleteCompModalNo;

    @UiField
    Strong errorAlertText;
    @UiField
    Modal errorModal;
    @UiField
    Button errorModalClose;
    @UiField
    TextBox factoryPid;

    @UiField
    Panel propertiesPanel;
    @UiField
    PanelBody propertiesPanelBody;
    @UiField
    PanelHeader propertiesPanelHeader;

    @UiField
    static Row configurationRow;

    @UiField
    Heading wiresComposerTitle;
    @UiField
    NavPills wireComponentsMenu;

    @UiField
    Widget composer;

    @UiField
    Modal selectAssetModal;
    @UiField
    HTMLPanel selectAssetModalIntro;
    @UiField
    FormLabel assetInstanceLabel;
    @UiField
    ListBox assetInstance;
    @UiField
    Button buttonSelectAssetCancel;
    @UiField
    Button buttonNewAsset;
    @UiField
    Button buttonSelectAssetOk;

    @UiField
    Modal selectDriverModal;
    @UiField
    HTMLPanel selectDriverModalIntro;
    @UiField
    FormLabel driverInstanceLabel;
    @UiField
    ListBox driverInstance;
    @UiField
    Button buttonSelectDriverCancel;
    @UiField
    Button buttonNewDriver;
    @UiField
    Button buttonSelectDriverOk;

    @UiField
    Modal newAssetModal;
    @UiField
    HTMLPanel newAssetModalIntro;
    @UiField
    FormLabel newAssetNameLabel;
    @UiField
    TextBox newAssetName;
    @UiField
    FormLabel newAssetDriverInstanceLabel;
    @UiField
    TextBox newAssetDriverInstance;
    @UiField
    Button newAssetCancel;
    @UiField
    Button newAssetOk;

    @UiField
    Modal newDriverModal;
    @UiField
    HTMLPanel newDriverModalIntro;
    @UiField
    FormLabel newDriverNameLabel;
    @UiField
    TextBox newDriverName;
    @UiField
    FormLabel newDriverFactoryLabel;
    @UiField
    ListBox newDriverFactory;
    @UiField
    Button newDriverCancel;
    @UiField
    Button newDriverOk;

    @UiField
    AlertDialog refreshAlert;

    static final String WIRE_ASSET = "WireAsset";

    interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
    }

    private static final WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtAssetServiceAsync gwtAssetService = GWT.create(GwtAssetService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private static final Logger logger = Logger.getLogger(WiresPanelUi.class.getSimpleName());

    private static final Messages MSGS = GWT.create(Messages.class);

    static final String FACTORY_PID_DROP_PREFIX = "factory://";

    private static final String TEMPORARY_ASSET_REG_EXP = "^[0-9]{14}$";
    private static final String ASSET_DESCRIPTION_PROP = "asset.desc";
    private static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";
    private static final String SELECT_COMPONENT = MSGS.servicesComponentFactorySelectorIdle();

    private boolean isDirty;
    private List<String> components;
    private final Map<String, GwtConfigComponent> configs = new HashMap<>();
    private final List<String> drivers;
    private final List<String> emitters;

    private String graph;
    private final Map<String, ConfigurationAreaUi> propertiesUis;
    private final List<String> receivers;
    private String wireComponentsConfigJson;
    private String wireConfigsJson;
    private String wires;
    private GwtConfigComponent currentSelection = null;

    private boolean panelLoaded = false;

    public WiresPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.emitters = new ArrayList<>();
        this.receivers = new ArrayList<>();
        this.components = new ArrayList<>();
        this.drivers = new ArrayList<>();
        this.propertiesUis = new HashMap<>();

        initButtons();
        initComposer();
        initAssetModal();
        initComponentDeleteModal();
        initSaveModal();
        initGraphDeleteModal();
        initErrorModal();
        initDragDrop();
        initSelectAssetModal();
        initSelectDriverModal();
        initNewAssetModal();
        initNewDriverModal();

        exportJSNIUpdateWireConfig();
        exportJSNIUpdateSelection();
        exportJSNIShowDuplicatePidModal();
        exportJSNIshowCycleExistenceError();
        exportJSNImakeUiDirty();
        exportJSNIDeactivateNavPils();
    }

    private String getFactoryPidFromDropUrl(String dropUrl) {
        if (dropUrl == null || dropUrl.isEmpty()) {
            return null;
        }
        if (!dropUrl.startsWith(FACTORY_PID_DROP_PREFIX)) {
            return null;
        }
        return dropUrl.substring(FACTORY_PID_DROP_PREFIX.length());
    }

    private void initDragDrop() {
        DropSupport drop = DropSupport.addIfSupported(this.composer);

        if (drop != null) {
            drop.setListener(new DropSupport.Listener() {

                @Override
                public boolean onDrop(DropEvent event) {
                    WiresPanelUi.this.onDrop(event.getClientX(), event.getClientY(),
                            getFactoryPidFromDropUrl(event.getAsText()));
                    final String factoryPid = getFactoryPidFromDropUrl(event.getAsText());
                    if (factoryPid != null) {
                        showComponentCreationDialog(factoryPid);
                    } else {
                        cancelDrag();
                    }
                    return true;
                }

                @Override
                public boolean onDragOver(DropEvent event) {
                    WiresPanelUi.this.onDrag(event.getClientX(), event.getClientY());
                    return true;
                }

                @Override
                public void onDragLeave(DropEvent event) {
                    WiresPanelUi.this.cancelDrag();
                }

            });
        }
    }

    private void initSelectAssetModal() {
        this.selectAssetModal.setTitle(MSGS.selectAssetLabel());
        this.selectAssetModalIntro.add(new Span("<p>" + MSGS.selectAssetIntroLabel() + "</p>"));
        this.assetInstanceLabel.setText(MSGS.assetName());
        this.buttonSelectAssetCancel.setText(MSGS.cancelButton());

        this.buttonNewAsset.setText(MSGS.newAsset());
        this.buttonNewAsset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                EntryClassUi.showWaitModal();

                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(final Throwable ex) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(ex);
                    }

                    @Override
                    public void onSuccess(final GwtXSRFToken token) {
                        gwtWireService.getDriverInstances(token, new AsyncCallback<List<String>>() {

                            @Override
                            public void onFailure(final Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(final List<String> result) {
                                if (result.isEmpty()) {
                                    WiresPanelUi.this.driverInstance.setEnabled(false);
                                    WiresPanelUi.this.buttonSelectDriverOk.setEnabled(false);
                                } else {
                                    WiresPanelUi.this.driverInstance.setEnabled(true);
                                    WiresPanelUi.this.buttonSelectDriverOk.setEnabled(true);
                                }

                                WiresPanelUi.this.driverInstance.clear();
                                for (String driverPid : result) {
                                    WiresPanelUi.this.driverInstance.addItem(driverPid);
                                }
                                EntryClassUi.hideWaitModal();

                                WiresPanelUi.this.selectDriverModal.show();
                            }
                        });
                    }
                });
            }
        });

        this.buttonSelectAssetOk.setText(MSGS.okButton());
        this.buttonSelectAssetOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String wireAssetPid = WiresPanelUi.this.assetInstance.getSelectedValue();
                createWireAsset(wireAssetPid);
            }
        });
        this.buttonSelectAssetCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.cancelDrag();
            }
        });
    }

    private void initSelectDriverModal() {
        this.selectDriverModal.setTitle(MSGS.selectDriverLabel());
        this.selectDriverModalIntro.add(new Span("<p>" + MSGS.selectDriverIntroLabel() + "</p>"));
        this.driverInstanceLabel.setText(MSGS.driverName());
        this.buttonSelectDriverCancel.setText(MSGS.cancelButton());
        this.buttonNewDriver.setText(MSGS.newDriver());
        this.buttonNewDriver.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.newDriverName.setValue("");
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
                                if (result.isEmpty()) {
                                    WiresPanelUi.this.newDriverFactory.setEnabled(false);
                                    WiresPanelUi.this.newDriverOk.setEnabled(false);
                                } else {
                                    WiresPanelUi.this.newDriverFactory.setEnabled(true);
                                    WiresPanelUi.this.newDriverOk.setEnabled(true);
                                }

                                WiresPanelUi.this.newDriverFactory.clear();
                                for (String driverFactoryPid : result) {
                                    WiresPanelUi.this.newDriverFactory.addItem(driverFactoryPid);
                                }
                                WiresPanelUi.this.newDriverName.setText("");
                                WiresPanelUi.this.newDriverModal.show();
                            }
                        });
                    }
                });
            }
        });

        this.buttonSelectDriverOk.setText(MSGS.okButton());
        this.buttonSelectDriverOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String driverPid = WiresPanelUi.this.driverInstance.getSelectedValue();
                WiresPanelUi.this.newAssetDriverInstance.setText(driverPid);
                WiresPanelUi.this.newAssetName.setText("");
                WiresPanelUi.this.newAssetModal.show();
            }
        });
        this.buttonSelectDriverCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.cancelDrag();
            }
        });
    }

    private void createWireAsset(final String pid) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtComponentService.findFilteredComponentConfiguration(token, pid,
                        new AsyncCallback<List<GwtConfigComponent>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(List<GwtConfigComponent> result) {
                                EntryClassUi.hideWaitModal();

                                for (GwtConfigComponent componentConfig : result) {

                                    createNewAssetNative(pid,
                                            (String) componentConfig.getProperties().get("driver.pid"));
                                }
                            }
                        });
            }
        });
    }

    private native void createComponentNative() /*-{
                                                parent.window.kuraWires.createNewComponent()
                                                }-*/;

    private native void createNewAssetNative(String assetPid, String driverPid) /*-{
                                                                                parent.window.kuraWires.createNewAssetComponent(assetPid, driverPid)
                                                                                }-*/;

    private void createComponent(final String pid) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtComponentService.findTrackedPids(token, new AsyncCallback<List<String>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                        cancelDrag();
                    }

                    @Override
                    public void onSuccess(List<String> result) {
                        EntryClassUi.hideWaitModal();
                        if (result.contains(pid)) {
                            jsniShowDuplicatePidModal(pid);
                            cancelDrag();
                        } else {
                            createComponentNative();
                        }
                    }
                });
            }
        });
    }

    private void initNewAssetModal() {
        this.newAssetModal.setTitle(MSGS.newAsset());
        this.newAssetModalIntro.add(new Span("<p>" + MSGS.createNewAssetIntroLabel2() + "</p>"));
        this.newAssetNameLabel.setText(MSGS.assetName());
        this.newAssetDriverInstanceLabel.setText(MSGS.driverName());
        this.newAssetDriverInstance.setReadOnly(true);
        this.newAssetCancel.setText(MSGS.cancelButton());
        this.newAssetOk.setText(MSGS.okButton());

        this.newAssetOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String wireAssetPid = WiresPanelUi.this.newAssetName.getText();
                String driverPid = WiresPanelUi.this.newAssetDriverInstance.getText();
                createNewAssetNative(wireAssetPid, driverPid);
                WiresPanelUi.this.newAssetModal.hide();
                WiresPanelUi.this.cancelDrag();
            }
        });
    }

    private void initNewDriverModal() {
        this.newDriverModal.setTitle(MSGS.newDriver());
        this.newDriverModalIntro.add(new Span("<p>" + MSGS.createNewDriverIntroLabel() + "</p>"));
        this.newDriverNameLabel.setText(MSGS.driverName());
        this.newDriverFactoryLabel.setText(MSGS.driverFactory());
        this.newDriverCancel.setText(MSGS.cancelButton());

        this.newDriverOk.setText(MSGS.okButton());
        this.newDriverOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (WiresPanelUi.this.newDriverName.validate()) {
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            final String driverFactoryPid = WiresPanelUi.this.newDriverFactory.getSelectedValue();
                            final String pid = WiresPanelUi.this.newDriverName.getValue();

                            gwtComponentService.createFactoryComponent(token, driverFactoryPid, pid,
                                    new AsyncCallback<Void>() {

                                        @Override
                                        public void onFailure(Throwable ex) {
                                            logger.log(Level.SEVERE, ex.getMessage(), ex);
                                            FailureHandler.handle(ex, EntryClassUi.class.getName());
                                        }

                                        @Override
                                        public void onSuccess(Void result) {
                                            WiresPanelUi.this.newDriverModal.hide();
                                            WiresPanelUi.this.newAssetDriverInstance.setText(pid);
                                            WiresPanelUi.this.newAssetName.setText("");
                                            WiresPanelUi.this.newAssetModal.show();
                                        }
                                    });
                        }
                    });
                }
            }
        });
        this.newDriverCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.cancelDrag();
            }
        });
    }

    private void initButtons() {
        this.btnSave.setText(MSGS.apply());
        this.btnSave.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.this.saveGraphModal.show();
            }
        });

        this.btnReset.setText(MSGS.reset());
        this.btnReset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.this.refreshAlert.show(MSGS.deviceConfigDirty(), new AlertDialog.Listener() {

                    @Override
                    public void onConfirm() {
                        WiresPanelUi.this.load();
                    }
                });
            }
        });

        this.btnDelete.setText(MSGS.wiresDeleteComponent());
        this.btnDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.this.deleteCompModal.show();
            }
        });

        this.btnGraphDelete.setText(MSGS.wiresDeleteGraph());
        this.btnGraphDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.this.deleteGraphModal.show();
            }
        });
    }

    private void initComposer() {
        this.wiresComposerTitle.setText(MSGS.wiresComposerTitle());
    }

    private void initAssetModal() {
        this.componentNameLabel.setText(MSGS.wiresComponentName());
        this.componentName.setPlaceholder(MSGS.wiresComponentNamePlaceholder());
        this.btnComponentModalYes.setText(MSGS.apply());
        this.btnComponentModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String value = WiresPanelUi.this.componentName.getValue();
                if (value != null && !value.isEmpty()) {
                    createComponent(value);
                }
            }
        });
        this.btnComponentModalNo.setText(MSGS.cancelButton());
        this.btnComponentModalNo.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.cancelDrag();
            }
        });
    }

    private void initComponentDeleteModal() {
        this.deleteCompModalHeader.setTitle(MSGS.confirm());
        this.deleteCompModalBody.add(new Span(MSGS.wiresComponentDeleteAlert()));
        this.btnDeleteCompModalYes.setText(MSGS.apply());
        this.btnDeleteCompModalNo.setText(MSGS.cancelButton());

        this.btnDeleteCompModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                setDirty(true);
                WiresPanelUi.this.deleteCompModal.hide();
            }
        });
    }

    private void initSaveModal() {
        this.saveGraphModalHeader.setTitle(MSGS.confirm());
        this.saveGraphModalBody.add(new Span(MSGS.wiresSave()));
        this.btnSaveGraphModalYes.setText(MSGS.apply());
        this.btnSaveGraphModalNo.setText(MSGS.cancelButton());

        this.btnSaveGraphModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                for (final Map.Entry<String, ConfigurationAreaUi> entry : WiresPanelUi.this.propertiesUis.entrySet()) {
                    final ConfigurationAreaUi ui = entry.getValue();
                    ui.setDirty(false);
                }
                WiresPanelUi.this.saveGraphModal.hide();
            }
        });
    }

    private void initGraphDeleteModal() {
        this.deleteGraphModalHeader.setTitle(MSGS.confirm());
        this.deleteGraphModalBody.add(new Span(MSGS.wiresGraphDeleteAlert()));
        this.btnDeleteGraphModalNo.setText(MSGS.cancelButton());
        this.btnDeleteGraphModalYes.setText(MSGS.apply());
        this.btnDeleteGraphModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                setDirty(true);
                WiresPanelUi.this.deleteGraphModal.hide();
            }
        });
    }

    private void initErrorModal() {
        this.errorModalClose.setText(MSGS.closeButton());
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public void load() {

        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtWireService.getWiresConfiguration(token, new AsyncCallback<GwtWiresConfiguration>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final GwtWiresConfiguration wiresConfig) {

                        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(final Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(final GwtXSRFToken token) {
                                gwtAssetService.getAssetInstances(token, new AsyncCallback<List<String>>() {

                                    @Override
                                    public void onFailure(final Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                    }

                                    @Override
                                    public void onSuccess(final List<String> result) {
                                        WiresPanelUi.this.assetInstance.clear();

                                        if (result.isEmpty()) {
                                            WiresPanelUi.this.assetInstance.setEnabled(false);
                                            WiresPanelUi.this.buttonSelectAssetOk.setEnabled(false);
                                        } else {
                                            WiresPanelUi.this.assetInstance.setEnabled(true);
                                            WiresPanelUi.this.buttonSelectAssetOk.setEnabled(true);
                                        }

                                        for (String wireAssetPid : result) {
                                            WiresPanelUi.this.assetInstance.addItem(wireAssetPid);
                                        }

                                        WiresPanelUi.this.wireComponentsConfigJson = wiresConfig
                                                .getWireComponentsJson();
                                        WiresPanelUi.this.wireConfigsJson = wiresConfig.getWireConfigurationsJson();
                                        WiresPanelUi.this.panelLoaded = true;
                                        internalLoad(wiresConfig);
                                        EntryClassUi.hideWaitModal();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    public void loadGraph() {
        final JSONObject obj = new JSONObject();
        final JSONArray emitters = new JSONArray();
        final JSONArray receivers = new JSONArray();
        final JSONArray drivers = new JSONArray();

        int i = 0;
        for (final String emitter : WiresPanelUi.this.emitters) {
            emitters.set(i, new JSONString(emitter));
            i++;
        }
        i = 0;
        for (final String receiver : WiresPanelUi.this.receivers) {
            receivers.set(i, new JSONString(receiver));
            i++;
        }
        i = 0;
        for (final String driver : WiresPanelUi.this.drivers) {
            drivers.set(i, new JSONString(driver));
            i++;
        }

        obj.put("pFactories", emitters);
        obj.put("cFactories", receivers);
        obj.put("drivers", drivers);
        obj.put("components", createComponentsJson());
        obj.put("wires", JSONParser.parseStrict(this.wires));
        obj.put("pGraph", JSONParser.parseStrict(this.graph));
        obj.put("wireComponentsJson", JSONParser.parseStrict(this.wireComponentsConfigJson));
        obj.put("wireConfigsJson", JSONParser.parseStrict(this.wireConfigsJson));

        wiresOpen(obj.toString());
        resetUiState();
    }

    public void render(final GwtConfigComponent item, String pid) {
        refreshTrackedConfigs();
        ConfigurationAreaUi configurationAreaUi;

        if (item != null) {
            Window.scrollTo(0, 0);
            WiresPanelUi.configurationRow.setVisible(false);
            WiresPanelUi.configurationRow.clear();

            if (!this.propertiesUis.containsKey(pid)) {
                configurationAreaUi = new ConfigurationAreaUi(item, pid, this);
                propertiesUis.put(pid, configurationAreaUi);
            } else {
                configurationAreaUi = this.propertiesUis.get(pid);
            }
            if (pid == null) {
                pid = "";
            }

            configurationAreaUi.render();
            this.currentSelection = item;
            WiresPanelUi.configurationRow.add(configurationAreaUi);
            WiresPanelUi.configurationRow.setVisible(true);
        } else {
            deselectComponent();
        }
    }

    private void refreshTrackedConfigs() {
        for (Entry<String, ConfigurationAreaUi> entry : this.propertiesUis.entrySet()) {
            ConfigurationAreaUi trackedConfig = entry.getValue();
            this.configs.putAll(trackedConfig.getUpdatedConfiguration());
        }
    }

    public static String getFormattedPid(final String pid) {
        String[] split;
        if (pid.contains(".")) {
            split = pid.split("\\.");
            final String lastString = split[split.length - 1];
            // if it's a 14 digit long no, it's a temporary instance of Asset
            // which is handled internally
            if (lastString.matches(TEMPORARY_ASSET_REG_EXP) || "WireAsset".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentAsset();
            } else if ("CloudPublisher".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentPublisher();
            } else if ("CloudSubscriber".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentSubscriber();
            } else if ("DbWireRecordStore".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentDBStore();
            } else if ("DbWireRecordFilter".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentDBFilter();
            } else if ("H2DbWireRecordStore".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentH2DBStore();
            } else if ("H2DbWireRecordFilter".equalsIgnoreCase(lastString)) {
                return MSGS.wiresComponentH2DBFilter();
            }
            return lastString;
        }
        return pid;
    }

    public void setDirty(final boolean flag) {
        if (flag) {
            this.btnSave.setText(MSGS.apply() + " *");
        } else {
            this.btnSave.setText(MSGS.apply());
        }
        this.btnSave.setEnabled(flag);
        this.btnReset.setEnabled(flag);
        this.isDirty = flag;
    }

    private void fillProperties(final GwtConfigComponent config, final String pid) {
        if (config != null && config.getFactoryId() != null && config.getFactoryId().contains(WIRE_ASSET)) {
            config.getProperties().put(AssetConstants.ASSET_DRIVER_PROP.value(), getDriver(pid));
        }
        render(config, pid);
    }

    private static List<String> getCommonElements(final List<String> firstList, final List<String> secondList) {
        final List<String> returnedList = new LinkedList<>();
        for (final String elem : firstList) {
            if (secondList.contains(elem)) {
                returnedList.add(elem);
            }
        }
        return returnedList;
    }

    private void internalLoad(final GwtWiresConfiguration config) {
        if (this.emitters != null) {
            this.emitters.clear();
        }
        if (this.receivers != null) {
            this.receivers.clear();
        }
        if (this.components != null) {
            this.components.clear();
        }
        if (this.configs != null) {
            this.configs.clear();
        }

        for (final String emitter : config.getWireEmitterFactoryPids()) {
            if (this.emitters != null && !this.emitters.contains(emitter)) {
                this.emitters.add(emitter);
            }
        }
        for (final String receiver : config.getWireReceiverFactoryPids()) {
            if (this.receivers != null && !this.receivers.contains(receiver)) {
                this.receivers.add(receiver);
            }
        }

        this.components = config.getWireComponents();
        this.wires = config.getWiresConfigurationJson();
        this.graph = config.getGraph();
        this.wireComponentsConfigJson = config.getWireComponentsJson();
        this.wireConfigsJson = config.getWireConfigurationsJson();

        this.factoryPid.setVisible(false);
        this.btnDelete.setEnabled(false);
        configurationRow.setVisible(false);
        populateComponentsPanel();
        loadGraph();
    }

    // ----------------------------------------------------------------
    //
    // JSNI
    //
    // ----------------------------------------------------------------
    private native void exportJSNIDeactivateNavPils()
    /*-{
    	var self = this
    	parent.window.jsniDeactivateNavPils = function() {
    		self.@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniDeactivateNavPils()()
    	}
    }-*/;

    private native void exportJSNImakeUiDirty()
    /*-{
    	var self = this
    	parent.window.jsniMakeUiDirty = function() {
    		self.@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniMakeUiDirty()()
    	}
    }-*/;

    private native void exportJSNIshowCycleExistenceError()
    /*-{
        var self = this
    	parent.window.jsniShowCycleExistenceError = function() {
    	    self.@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowCycleExistenceError()()
    	}
    }-*/;

    private native void exportJSNIShowDuplicatePidModal()
    /*-{
    	var self = this
    	parent.window.jsniShowDuplicatePidModal = function(a) {
    		self.@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowDuplicatePidModal(Ljava/lang/String;)(a)
    	}
    }-*/;

    private native void exportJSNIUpdateSelection()
    /*-{
    	var self = this
    	parent.window.jsniUpdateSelection = function(a, b) {
    		self.@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateSelection(Ljava/lang/String;Ljava/lang/String;)(a,b)
    	}
    }-*/;

    private native void exportJSNIUpdateWireConfig()
    /*-{
    	var self = this
    	parent.window.jsniUpdateWireConfig = function(a) {
    		self.@org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateWireConfig(Ljava/lang/String;)(a)
    	}
    }-*/;

    private native void onDrag(double clientX, double clientY)
    /*-{
    parent.window.kuraWires.dragHandler.onDrag(clientX, clientY)
    }-*/;

    private native void onDrop(double clientX, double clientY, String factoryPid)
    /*-{
    parent.window.kuraWires.dragHandler.onDrop(clientX, clientY, factoryPid)
    }-*/;

    private native void cancelDrag()
    /*-{
    parent.window.kuraWires.dragHandler.abort()
    }-*/;

    public native String getDriver(String assetPid)
    /*-{
    	return parent.window.kuraWires.getDriver(assetPid);
    }-*/;

    public void jsniDeactivateNavPils() {
        for (int i = 0; i < this.wireComponentsMenu.getWidgetCount(); i++) {
            final AnchorListItem item = (AnchorListItem) this.wireComponentsMenu.getWidget(i);
            if (item.isActive()) {
                item.setActive(false);
            }
        }
        this.componentName.clear();
    }

    public void jsniMakeUiDirty() {
        setDirty(true);
    }

    public void jsniShowCycleExistenceError() {
        this.errorAlertText.setText(MSGS.wiresCycleDetected());
        this.errorModal.show();
    }

    public void jsniShowDuplicatePidModal(final String pid) {
        this.errorAlertText.setText(MSGS.wiresComponentNameAlreadyUsed(pid));
        this.errorModal.show();
        this.genericCompModal.hide();
    }

    public void jsniUpdateSelection(final String pid, final String factoryPid) {
        if ("".equals(factoryPid)) {
            this.btnDelete.setEnabled(false);
            configurationRow.setVisible(false);
            return;
        }
        // enable delete instance button
        this.btnDelete.setEnabled(true);
        // Retrieve GwtComponentConfiguration to use for manipulating the
        // properties. If it is already present in the map, it means the
        // component has already been accessed by the graph, and its
        // configuration has already been gathered from the ConfigurationService.
        final GwtConfigComponent comp = this.configs.get(pid);
        if (comp != null) {
            fillProperties(comp, pid);
            selectionCompleted();
        } else {
            // Retrieve GwtComponentConfiguration to use for manipulating the properties.
            EntryClassUi.showWaitModal();
            gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(final Throwable ex) {
                    EntryClassUi.hideWaitModal();
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(final GwtXSRFToken token) {
                    Map<String, Object> temporaryMap = null;
                    if (WIRE_ASSET_PID.equalsIgnoreCase(factoryPid)) {
                        temporaryMap = new HashMap<>();
                        temporaryMap.put(ASSET_DESCRIPTION_PROP, MSGS.wiresSampleAssetName());
                        temporaryMap.put(AssetConstants.ASSET_DRIVER_PROP.value(), getDriver(pid));
                    }
                    gwtComponentService.findWireComponentConfigurationFromPid(token, pid, factoryPid, temporaryMap,
                            new AsyncCallback<GwtConfigComponent>() {

                                @Override
                                public void onFailure(final Throwable caught) {
                                    EntryClassUi.hideWaitModal();
                                    FailureHandler.handle(caught);
                                }

                                @Override
                                public void onSuccess(final GwtConfigComponent result) {
                                    EntryClassUi.hideWaitModal();
                                    // Component configuration retrieved
                                    // from the Configuration Service
                                    fillProperties(result, pid);
                                    WiresPanelUi.this.configs.put(pid, result);
                                    selectionCompleted();
                                }
                            });

                }
            });
        }
    }

    private static void updateConfiguration(final String pid, final Map<String, Object> properties) {
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtComponentService.updateProperties(token, pid, properties, new AsyncCallback<Boolean>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        EntryClassUi.hideWaitModal();
                    }
                });
            }

        });
    }

    public void showComponentCreationDialog(String factoryPid) {
        if (factoryPid.contains(WiresPanelUi.WIRE_ASSET)) {
            this.selectAssetModal.show();
        } else {
            this.newAssetModalHeader.setTitle(MSGS.wiresComponentNew());
            this.componentNameLabel.setText(MSGS.wiresComponentName());
            WiresPanelUi.this.factoryPid.setValue(factoryPid);
            this.genericCompModal.show();
        }
    }

    public int jsniUpdateWireConfig(final String obj) {

        EntryClassUi.showWaitModal();
        // Create new components
        // "models" hold all existing components in JointJS graph. If the PID is
        // "none", then we need to create
        // component in framework and set PID.
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                refreshTrackedConfigs();

                gwtWireService.updateWireConfiguration(token, obj, WiresPanelUi.this.configs,
                        new AsyncCallback<Void>() {

                            @Override
                            public void onFailure(final Throwable caught) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(caught);
                            }

                            @Override
                            public void onSuccess(final Void result) {
                                EntryClassUi.hideWaitModal();
                                resetUiState();
                            }
                        });
            }
        });

        return 0;
    }

    private void deselectComponent() {
        WiresPanelUi.configurationRow.clear();
        WiresPanelUi.configurationRow.setVisible(false);
        this.currentSelection = null;
    }

    public native void wiresOpen(String obj)
    /*-{
    	parent.window.kuraWires.render(obj);
    }-*/;

    public void unload() {
        if (this.panelLoaded) {
            wiresClose();
        }
        this.panelLoaded = false;
    }

    private native void wiresClose()
    /*-{
    	parent.window.kuraWires.unload();
    }-*/;

    private native void selectionCompleted()
    /*-{
    	parent.window.kuraWires.selectionCompleted();
    }-*/;

    private JSONArray createComponentsJson() {

        final JSONArray components = new JSONArray();
        int i = 0;

        for (final String component : WiresPanelUi.this.components) {
            final JSONObject compObj = new JSONObject();
            final String[] tokens = component.split("\\|");
            compObj.put("fPid", new JSONString(tokens[0]));
            compObj.put("pid", new JSONString(tokens[1]));
            compObj.put("name", new JSONString(tokens[2]));
            compObj.put("type", new JSONString(tokens[3]));
            components.set(i, compObj);
            i++;
        }

        return components;
    }

    private void populateComponentsPanel() {
        final List<String> onlyProducers = new ArrayList<>(this.emitters);
        final List<String> onlyConsumers = new ArrayList<>(this.receivers);
        final List<String> both = getCommonElements(this.emitters, this.receivers);
        onlyProducers.removeAll(both);
        onlyConsumers.removeAll(both);
        this.wireComponentsMenu.clear();
        for (final String fPid : onlyProducers) {
            this.wireComponentsMenu.add(new WireComponentsAnchorListItem(fPid, true, false, this));
        }
        for (final String fPid : onlyConsumers) {
            this.wireComponentsMenu.add(new WireComponentsAnchorListItem(fPid, false, true, this));
        }
        for (final String fPid : both) {
            this.wireComponentsMenu.add(new WireComponentsAnchorListItem(fPid, true, true, this));
        }
    }

    private void resetUiState() {
        setDirty(false);
        if (WiresPanelUi.this.configs != null && !WiresPanelUi.this.configs.isEmpty()) {
            WiresPanelUi.this.configs.clear();
        }
        if (WiresPanelUi.this.propertiesUis != null && !WiresPanelUi.this.propertiesUis.isEmpty()) {
            WiresPanelUi.this.propertiesUis.clear();
        }
        deselectComponent();
    }
}
