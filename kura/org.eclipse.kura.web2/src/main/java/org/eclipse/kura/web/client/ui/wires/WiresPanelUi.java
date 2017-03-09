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

import static org.eclipse.kura.web.shared.service.GwtWireService.DELETED_WIRE_COMPONENT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.DropSupport;
import org.eclipse.kura.web.client.util.DropSupport.DropEvent;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.web.shared.service.GwtWireServiceAsync;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite {

    @UiField
    static Button btnSave;
    @UiField
    static Button btnZoomIn;
    @UiField
    static Button btnZoomOut;
    @UiField
    static Button btnDelete;
    @UiField
    static Button btnGraphDelete;

    @UiField
    static FormLabel wiresComponentName;
    @UiField
    static TextBox componentName;

    @UiField
    static Modal assetModal;
    @UiField
    static Button btnAssetModalYes;
    @UiField
    static Button btnAssetModalNo;

    @UiField
    static Modal saveGraphModal;
    @UiField
    static ModalHeader saveGraphModalHeader;
    @UiField
    static ModalBody saveGraphModalBody;
    @UiField
    static Button btnSaveGraphModalYes;
    @UiField
    static Button btnSaveGraphModalNo;

    @UiField
    static Modal deleteGraphModal;
    @UiField
    static ModalHeader deleteGraphModalHeader;
    @UiField
    static ModalBody deleteGraphModalBody;
    @UiField
    static Button btnDeleteGraphModalYes;
    @UiField
    static Button btnDeleteGraphModalNo;

    @UiField
    static Modal deleteCompModal;
    @UiField
    static ModalHeader deleteCompModalHeader;
    @UiField
    static ModalBody deleteCompModalBody;
    @UiField
    static Button btnDeleteCompModalYes;
    @UiField
    static Button btnDeleteCompModalNo;

    @UiField
    static FormGroup driverInstanceForm;
    @UiField
    static FormLabel wiresAvailableDrivers;
    @UiField
    static ListBox driverPids;
    @UiField
    static Strong errorAlertText;
    @UiField
    static Modal errorModal;
    @UiField
    static Button errorModalClose;
    @UiField
    static TextBox factoryPid;

    @UiField
    static Panel propertiesPanel;
    @UiField
    static PanelBody propertiesPanelBody;
    @UiField
    static PanelHeader propertiesPanelHeader;

    @UiField
    static Heading wiresComposerTitle;
    @UiField
    static NavPills wireComponentsMenu;

    @UiField
    Widget composer;

    static final String WIRE_ASSET = "WireAsset";

    interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
    }

    private static final WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);

    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private static final Logger logger = Logger.getLogger(WiresPanelUi.class.getSimpleName());

    private static final Messages MSGS = GWT.create(Messages.class);

    static final String FACTORY_PID_DROP_PREFIX = "factory://";

    private static final String TEMPORARY_ASSET_REG_EXP = "^[0-9]{14}$";
    private static final String ASSET_DESCRIPTION_PROP = "asset.desc";
    private static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";

    private static boolean isDirty;
    private static List<String> components;
    private static Map<String, GwtConfigComponent> configs = new HashMap<>();
    private static List<String> drivers;
    private static List<String> emitters;

    private static String graph;
    private static Map<String, PropertiesUi> propertiesUis;
    private static List<String> receivers;
    private static String wireComponentsConfigJson;
    private static String wireConfigsJson;
    private static String wires;
    private static GwtConfigComponent currentSelection = null;
    private static PropertiesUi propertiesUi;

    private static String driverPidProp = "driver.pid";

    private static boolean panelLoaded = false;

    public WiresPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        emitters = new ArrayList<>();
        receivers = new ArrayList<>();
        components = new ArrayList<>();
        drivers = new ArrayList<>();
        propertiesUis = new HashMap<>();

        initButtons();
        initComposer();
        initAssetModal();
        initComponentDeleteModal();
        initSaveModal();
        initGraphDeleteModal();
        initErrorModal();
        initDragDrop();

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
                    String factoryPid = getFactoryPidFromDropUrl(event.getAsText());
                    if (factoryPid != null) {
                        WiresPanelUi.showComponentCreationDialog(factoryPid);
                    }
                    return true;
                }

                @Override
                public boolean onDragOver(DropEvent event) {
                    return true;
                }
            });
        }
    }

    private native void createComponentNative() /*-{
                                                          parent.window.kuraWires.createNewComponent()
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
                    }

                    @Override
                    public void onSuccess(List<String> result) {
                        EntryClassUi.hideWaitModal();
                        if (result.contains(pid)) {
                            jsniShowDuplicatePidModal(pid);
                        } else {
                            createComponentNative();
                        }
                    }
                });
            }
        });
    }

    private void initButtons() {
        btnSave.setText(MSGS.apply());
        btnSave.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final List<String> pids = new ArrayList<>();
                for (final Map.Entry<String, PropertiesUi> entry : propertiesUis.entrySet()) {
                    final PropertiesUi ui = entry.getValue();
                    final String componentId = ui.getConfiguration().getComponentId();
                    if (ui.isDirty() && !ui.isNonValidated()) {
                        pids.add(getFormattedPid(componentId));
                    }
                }

                WiresPanelUi.saveGraphModal.show();
            }
        });

        btnZoomIn.setText(MSGS.wiresZoomIn());
        btnZoomOut.setText(MSGS.wiresZoomOut());

        btnDelete.setText(MSGS.wiresDeleteComponent());
        btnDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.deleteCompModal.show();
            }
        });

        btnGraphDelete.setText(MSGS.wiresDeleteGraph());
        btnGraphDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.deleteGraphModal.show();
            }
        });
    }

    private void initComposer() {
        wiresComposerTitle.setText(MSGS.wiresComposerTitle());
    }

    private void initAssetModal() {
        wiresAvailableDrivers.setText(MSGS.wiresAvailableDrivers());
        wiresComponentName.setText(MSGS.wiresComponentName());
        componentName.setPlaceholder(MSGS.wiresComponentNamePlaceholder());
        btnAssetModalYes.setText(MSGS.apply());
        btnAssetModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String value = componentName.getValue();
                if (value != null && !value.isEmpty()) {
                    createComponent(value);
                }
            }
        });
        btnAssetModalNo.setText(MSGS.cancelButton());
    }

    private void initComponentDeleteModal() {
        deleteCompModalHeader.setTitle(MSGS.confirm());
        deleteCompModalBody.add(new Span(MSGS.wiresComponentDeleteAlert()));
        btnDeleteCompModalYes.setText(MSGS.apply());
        btnDeleteCompModalNo.setText(MSGS.cancelButton());

        btnDeleteCompModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.setDirty(true);
                WiresPanelUi.deleteCompModal.hide();
            }
        });
    }

    private void initSaveModal() {
        saveGraphModalHeader.setTitle(MSGS.confirm());
        saveGraphModalBody.add(new Span(MSGS.wiresSave()));
        btnSaveGraphModalYes.setText(MSGS.apply());
        btnSaveGraphModalNo.setText(MSGS.cancelButton());

        btnSaveGraphModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                for (final Map.Entry<String, PropertiesUi> entry : propertiesUis.entrySet()) {
                    final PropertiesUi ui = entry.getValue();
                    ui.setDirty(false);
                }
                WiresPanelUi.saveGraphModal.hide();
            }
        });
    }

    private void initGraphDeleteModal() {
        deleteGraphModalHeader.setTitle(MSGS.confirm());
        deleteGraphModalBody.add(new Span(MSGS.wiresGraphDeleteAlert()));
        btnDeleteGraphModalNo.setText(MSGS.cancelButton());
        btnDeleteGraphModalYes.setText(MSGS.apply());
        btnDeleteGraphModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.setDirty(true);
                WiresPanelUi.deleteGraphModal.hide();
            }
        });
    }

    private void initErrorModal() {
        errorModalClose.setText(MSGS.closeButton());
    }

    public static void clearUnsavedPanelChanges() {
        btnSave.setEnabled(false);
        btnSave.setText(MSGS.apply());
        isDirty = false;
        configs.clear();
        resetDeleteComponentState();
    }

    public static boolean isDirty() {
        return isDirty;
    }

    public static void load() {
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
                    public void onSuccess(final GwtWiresConfiguration result) {
                        requestDriverInstances(result);
                        wireComponentsConfigJson = result.getWireComponentsJson();
                        wireConfigsJson = result.getWireConfigurationsJson();
                        panelLoaded = true;
                    }
                });
            }
        });
    }

    public static void loadGraph() {
        final JSONObject obj = new JSONObject();
        final JSONArray emitters = new JSONArray();
        final JSONArray receivers = new JSONArray();
        final JSONArray drivers = new JSONArray();

        int i = 0;
        for (final String emitter : WiresPanelUi.emitters) {
            emitters.set(i, new JSONString(emitter));
            i++;
        }
        i = 0;
        for (final String receiver : WiresPanelUi.receivers) {
            receivers.set(i, new JSONString(receiver));
            i++;
        }
        i = 0;
        for (final String driver : WiresPanelUi.drivers) {
            drivers.set(i, new JSONString(driver));
            i++;
        }

        obj.put("pFactories", emitters);
        obj.put("cFactories", receivers);
        obj.put("drivers", drivers);
        obj.put("components", createComponentsJson());
        obj.put("wires", JSONParser.parseStrict(wires));
        obj.put("pGraph", JSONParser.parseStrict(graph));
        obj.put("wireComponentsJson", JSONParser.parseStrict(wireComponentsConfigJson));
        obj.put("wireConfigsJson", JSONParser.parseStrict(wireConfigsJson));

        wiresOpen(obj.toString());
        btnSave.setEnabled(false);
    }

    public static void render(final GwtConfigComponent item, String pid) {
        if (currentSelection != null && WiresPanelUi.propertiesUi != null) {
            WiresPanelUi.propertiesUi.getUpdatedConfiguration();
        }
        if (item != null) {
            WiresPanelUi.propertiesPanelBody.clear();

            if (!propertiesUis.containsKey(pid)) {
                WiresPanelUi.propertiesUi = new PropertiesUi(item, pid);
                propertiesUis.put(pid, WiresPanelUi.propertiesUi);
            } else {
                WiresPanelUi.propertiesUi = propertiesUis.get(pid);
            }
            WiresPanelUi.propertiesPanel.setVisible(true);
            if (pid == null) {
                pid = "";
            }

            if (WiresPanelUi.propertiesUi.isDirty()) {
                propertiesPanelHeader.setText(getFormattedPid(item.getFactoryId()) + " - " + pid + "*");
            } else {
                propertiesPanelHeader.setText(getFormattedPid(item.getFactoryId()) + " - " + pid);
            }
            currentSelection = item;
            WiresPanelUi.propertiesPanelBody.add(WiresPanelUi.propertiesUi);
        } else {
            deselectComponent();
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
            }
            return lastString;
        }
        return pid;
    }

    public static void setDirty(final boolean flag) {
        if (flag) {
            btnSave.setEnabled(true);
            btnSave.setText(MSGS.apply() + " *");
        } else {
            btnSave.setEnabled(false);
            btnSave.setText(MSGS.apply());
        }
        isDirty = flag;
    }

    private static void fillProperties(final GwtConfigComponent config, final String pid) {
        gwtWireService.getDriverPidProp(new AsyncCallback<String>() {

            @Override
            public void onFailure(final Throwable caught) {
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(String result) {
                driverPidProp = result;
                if (config != null && config.getFactoryId() != null && config.getFactoryId().contains(WIRE_ASSET)) {
                    config.getProperties().put(driverPidProp, getDriver(pid));
                }
                render(config, pid);
            }
        });
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

    private static void internalLoad(final GwtWiresConfiguration config) {
        if (emitters != null) {
            emitters.clear();
        }
        if (receivers != null) {
            receivers.clear();
        }
        if (components != null) {
            components.clear();
        }
        if (configs != null) {
            configs.clear();
        }

        for (final String emitter : config.getWireEmitterFactoryPids()) {
            if (emitters != null && !emitters.contains(emitter)) {
                emitters.add(emitter);
            }
        }
        for (final String receiver : config.getWireReceiverFactoryPids()) {
            if (receivers != null && !receivers.contains(receiver)) {
                receivers.add(receiver);
            }
        }

        components = config.getWireComponents();
        wires = config.getWiresConfigurationJson();
        graph = config.getGraph();
        wireComponentsConfigJson = config.getWireComponentsJson();
        wireConfigsJson = config.getWireConfigurationsJson();

        factoryPid.setVisible(false);
        WiresPanelUi.btnDelete.setEnabled(false);
        WiresPanelUi.propertiesPanel.setVisible(false);
        populateDrivers();
        populateComponentsPanel();
        loadGraph();
    }

    // ----------------------------------------------------------------
    //
    // JSNI
    //
    // ----------------------------------------------------------------
    private static native void exportJSNIDeactivateNavPils()
    /*-{
    parent.window.jsniDeactivateNavPils = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniDeactivateNavPils()
    );
    }-*/;

    private static native void exportJSNImakeUiDirty()
    /*-{
    parent.window.jsniMakeUiDirty = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniMakeUiDirty()
    );
    }-*/;

    private static native void exportJSNIshowCycleExistenceError()
    /*-{
    parent.window.jsniShowCycleExistenceError = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowCycleExistenceError()
    );
    }-*/;

    private static native void exportJSNIShowDuplicatePidModal()
    /*-{
    parent.window.jsniShowDuplicatePidModal = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowDuplicatePidModal(Ljava/lang/String;)
    );
    }-*/;

    private static native void exportJSNIUpdateSelection()
    /*-{
    parent.window.jsniUpdateSelection = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateSelection(Ljava/lang/String;Ljava/lang/String;)
    );
    }-*/;

    private static native void exportJSNIUpdateWireConfig()
    /*-{
    parent.window.jsniUpdateWireConfig = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateWireConfig(Ljava/lang/String;)
    );
    }-*/;

    public static native String getDriver(String assetPid)
    /*-{
        return parent.window.kuraWires.getDriver(assetPid);
    }-*/;

    public static void jsniDeactivateNavPils() {
        for (int i = 0; i < wireComponentsMenu.getWidgetCount(); i++) {
            final AnchorListItem item = (AnchorListItem) wireComponentsMenu.getWidget(i);
            if (item.isActive()) {
                item.setActive(false);
            }
        }
        WiresPanelUi.componentName.clear();
    }

    public static void jsniMakeUiDirty() {
        WiresPanelUi.setDirty(true);
    }

    public static void jsniShowCycleExistenceError() {
        WiresPanelUi.errorAlertText.setText(MSGS.wiresCycleDetected());
        WiresPanelUi.errorModal.show();
    }

    public static void jsniShowDuplicatePidModal(final String pid) {
        WiresPanelUi.errorAlertText.setText(MSGS.wiresComponentNameAlreadyUsed(pid));
        WiresPanelUi.errorModal.show();
        assetModal.hide();
    }

    public static void jsniUpdateSelection(final String pid, final String factoryPid) {
        if ("".equals(factoryPid)) {
            WiresPanelUi.btnDelete.setEnabled(false);
            WiresPanelUi.propertiesPanel.setVisible(false);
            if (!"".equals(pid)) {
                updateDeletedWireComponent(pid);
            }
            return;
        }
        // enable delete instance button
        WiresPanelUi.btnDelete.setEnabled(true);
        // Retrieve GwtComponentConfiguration to use for manipulating the properties.
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
                    temporaryMap.put(driverPidProp, getDriver(pid));
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
                        // Component configuration retrieved
                        // from the Configuration Service
                        fillProperties(result, pid);
                        configs.put(pid, result);
                        if (propertiesUis.containsKey(pid)) {
                            propertiesUis.remove(pid);
                        }
                        EntryClassUi.hideWaitModal();
                    }
                });
            }
        });
    }

    private static void updateDeletedWireComponent(final String pid) {
        if (configs.containsKey(pid)) {
            configs.remove(pid);
        }
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(final Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(final GwtXSRFToken token) {
                gwtComponentService.findComponentConfiguration(token, pid,
                        new AsyncCallback<List<GwtConfigComponent>>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final List<GwtConfigComponent> components) {
                        if (!components.isEmpty()) {
                            final GwtConfigComponent component = components.get(0);
                            Map<String, Object> props = new HashMap<>();
                            props.put(DELETED_WIRE_COMPONENT, true);
                            updateConfiguration(component.getComponentId(), props);
                        }
                        EntryClassUi.hideWaitModal();
                    }
                });
            }

        });
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

    public static void showComponentCreationDialog(String factoryPid) {
        if (factoryPid.contains(WiresPanelUi.WIRE_ASSET)) {
            WiresPanelUi.driverInstanceForm.setVisible(true);
        } else {
            WiresPanelUi.driverInstanceForm.setVisible(false);
        }
        WiresPanelUi.factoryPid.setValue(factoryPid);
        WiresPanelUi.assetModal.show();
    }

    public static int jsniUpdateWireConfig(final String obj) {

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
                if (currentSelection != null && WiresPanelUi.propertiesUi != null) {
                    WiresPanelUi.propertiesUi.getUpdatedConfiguration();
                }
                gwtWireService.updateWireConfiguration(token, obj, configs, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final Void result) {
                        EntryClassUi.hideWaitModal();
                        setDirty(false);
                        if (configs != null && !configs.isEmpty()) {
                            configs.clear();
                        }
                        if (propertiesUis != null && !propertiesUis.isEmpty()) {
                            propertiesUis.clear();
                        }
                        deselectComponent();
                    }
                });
            }
        });

        return 0;
    }

    private static void deselectComponent() {
        WiresPanelUi.propertiesPanelBody.clear();
        propertiesPanelHeader.setText(MSGS.wiresNoComponentSelected());
        WiresPanelUi.propertiesPanel.setVisible(false);
        currentSelection = null;
    }

    public static native void resetDeleteComponentState()
    /*-{
        parent.window.kuraWires.resetDeleteComponentState();
    }-*/;

    public static native void wiresOpen(String obj)
    /*-{
        parent.window.kuraWires.render(obj);
    }-*/;

    public static void unload() {
        if (panelLoaded) {
            wiresClose();
        }
        panelLoaded = false;
    }

    private static native void wiresClose()
    /*-{
        parent.window.kuraWires.unload();
    }-*/;

    private static JSONArray createComponentsJson() {

        final JSONArray components = new JSONArray();
        int i = 0;

        for (final String component : WiresPanelUi.components) {
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

    private static void populateComponentsPanel() {
        final List<String> onlyProducers = new ArrayList<>(emitters);
        final List<String> onlyConsumers = new ArrayList<>(receivers);
        final List<String> both = getCommonElements(emitters, receivers);
        onlyProducers.removeAll(both);
        onlyConsumers.removeAll(both);
        wireComponentsMenu.clear();
        for (final String fPid : onlyProducers) {
            wireComponentsMenu.add(new WireComponentsAnchorListItem(fPid, true, false));
        }
        for (final String fPid : onlyConsumers) {
            wireComponentsMenu.add(new WireComponentsAnchorListItem(fPid, false, true));
        }
        for (final String fPid : both) {
            wireComponentsMenu.add(new WireComponentsAnchorListItem(fPid, true, true));
        }
    }

    private static void populateDrivers() {
        WiresPanelUi.driverPids.clear();
        WiresPanelUi.driverPids.addItem(MSGS.wiresDriverSelection());
        for (final String driver : drivers) {
            WiresPanelUi.driverPids.addItem(driver);
        }
    }

    private static void requestDriverInstances(final GwtWiresConfiguration configuration) {
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
                        drivers.clear();
                        drivers.addAll(result);
                        internalLoad(configuration);
                        EntryClassUi.hideWaitModal();
                    }
                });
            }
        });
    }
}