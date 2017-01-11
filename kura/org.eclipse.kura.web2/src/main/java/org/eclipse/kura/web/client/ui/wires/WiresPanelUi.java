/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.ui.EntryClassUi;
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
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.AlertType;
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

    interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
    }

    @UiField
    public static Modal assetModal;
    @UiField
    public static Button btnDelete;
    @UiField
    public static Button btnDeleteGraphYes;
    @UiField
    public static Button btnDeleteYes;
    @UiField
    public static Button btnGraphDelete;
    @UiField
    public static Button btnSave;

    @UiField
    public static Button btnSaveYes;
    @UiField
    public static TextBox componentName;
    private static GwtConfigComponent currentSelection = null;
    @UiField
    public static Modal deleteGraphModal;
    @UiField
    public static Modal deleteModal;
    @UiField
    public static FormGroup driverInstanceForm;
    @UiField
    public static ListBox driverPids;
    @UiField
    public static Strong errorAlertText;
    @UiField
    public static Modal errorModal;
    @UiField
    public static TextBox factoryPid;
    private static final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private static final GwtWireServiceAsync gwtWireService = GWT.create(GwtWireService.class);
    private static final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private static boolean isDirty;

    private static final Logger logger = Logger.getLogger(WiresPanelUi.class.getSimpleName());
    private static List<String> m_components;
    private static Map<String, GwtConfigComponent> m_configs = new HashMap<String, GwtConfigComponent>();
    private static List<String> m_drivers;
    private static List<String> m_emitters;

    private static String m_graph;
    private static Map<String, PropertiesUi> m_propertiesUis;
    private static List<String> m_receivers;
    private static String m_wireComponentsConfigJson;
    private static String m_wireConfigsJson;
    private static String m_wires;

    @UiField
    public static Panel propertiesPanel;
    @UiField
    public static PanelBody propertiesPanelBody;
    @UiField
    public static PanelHeader propertiesPanelHeader;
    private static PropertiesUi propertiesUi;
    @UiField
    public static Alert saveGraphAlert;

    @UiField
    public static Strong saveGraphAlertText;
    @UiField
    public static Modal saveModal;
    private static WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);

    @UiField
    static NavPills wireComponentsMenu;

    private static WiresPanelUi wiresPanelUi;

    public static void clearUnsavedPanelChanges() {
        btnSave.setEnabled(false);
        btnSave.setText("Save");
        isDirty = false;
        m_configs.clear();
        resetDeleteComponentState();
    }

    private static JSONArray createComponentsJson() {

        final JSONArray components = new JSONArray();
        int i = 0;

        for (final String component : m_components) {
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

    public static native void exportJSNIDeactivateNavPils()
    /*-{
    $wnd.jsniDeactivateNavPils = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniDeactivateNavPils()
    );
    }-*/;

    public static native void exportJSNImakeUiDirty()
    /*-{
    $wnd.jsniMakeUiDirty = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniMakeUiDirty()
    );
    }-*/;

    public static native void exportJSNIShowAddNotAllowedModal()
    /*-{
    $wnd.jsniShowAddNotAllowedModal = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowAddNotAllowedModal()
    );
    }-*/;

    public static native void exportJSNIshowCycleExistenceError()
    /*-{
    $wnd.jsniShowCycleExistenceError = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowCycleExistenceError()
    );
    }-*/;

    public static native void exportJSNIShowDuplicatePidModal()
    /*-{
    $wnd.jsniShowDuplicatePidModal = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniShowDuplicatePidModal(Ljava/lang/String;)
    );
    }-*/;

    public static native void exportJSNIUpdateSelection()
    /*-{
    $wnd.jsniUpdateSelection = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateSelection(Ljava/lang/String;Ljava/lang/String;)
    );
    }-*/;

    public static native void exportJSNIUpdateWireConfig()
    /*-{
    $wnd.jsniUpdateWireConfig = $entry(
    @org.eclipse.kura.web.client.ui.wires.WiresPanelUi::jsniUpdateWireConfig(Ljava/lang/String;)
    );
    }-*/;

    private static void fillProperties(final GwtConfigComponent config, final String pid) {
        if (config != null && config.getFactoryId() != null && config.getFactoryId().contains("WireAsset")) {
            config.getProperties().put("driver.pid", getDriver(pid));
        }
        render(config, pid);
    }

    private static List<String> getCommonElements(final List<String> firstList, final List<String> secondList) {
        final List<String> returnedList = new LinkedList<String>();
        for (final String elem : firstList) {
            if (secondList.contains(elem)) {
                returnedList.add(elem);
            }
        }
        return returnedList;
    }

    public static native String getDriver(String assetPid)
    /*-{
    	return $wnd.kuraWires.getDriver(assetPid);
    }-*/;

    public static String getFormattedPid(final String pid) {
        if ("org.eclipse.kura.wire.WireAsset".equalsIgnoreCase(pid)) {
            return "Asset";
        }
        if ("org.eclipse.kura.wire.DbWireRecordStore".equalsIgnoreCase(pid)) {
            return "DB Store";
        }
        if ("org.eclipse.kura.wire.DbWireRecordFilter".equalsIgnoreCase(pid)) {
            return "DB Filter";
        }
        if ("org.eclipse.kura.wire.CloudPublisher".equalsIgnoreCase(pid)) {
            return "Publisher";
        }
        if ("org.eclipse.kura.wire.CloudSubscriber".equalsIgnoreCase(pid)) {
            return "Subscriber";
        }
        String[] split;
        if (pid.contains(".")) {
            split = pid.split("\\.");
            final String lastString = split[split.length - 1];
            // if it's a 14 digit long no, it's a temporary instance of Asset
            // which is handled internally
            if (lastString.matches("^[0-9]{14}$")) {
                return "Asset";
            }
            if ("CloudPublisher".equalsIgnoreCase(lastString)) {
                return "Publisher";
            }
            if ("CloudSubscriber".equalsIgnoreCase(lastString)) {
                return "Subscriber";
            }
            if ("DbWireRecordStore".equalsIgnoreCase(lastString)) {
                return "DB Store";
            }
            if ("DbWireRecordFilter".equalsIgnoreCase(lastString)) {
                return "DB Filter";
            }
            return lastString;
        }
        return pid;
    }

    private static void internalLoad(final GwtWiresConfiguration config) {
        if (m_emitters != null) {
            m_emitters.clear();
        }
        if (m_receivers != null) {
            m_receivers.clear();
        }
        if (m_components != null) {
            m_components.clear();
        }
        if (m_configs != null) {
            m_configs.clear();
        }

        for (final String emitter : config.getWireEmitterFactoryPids()) {
            if ((m_emitters != null) && !m_emitters.contains(emitter)) {
                m_emitters.add(emitter);
            }
        }
        for (final String receiver : config.getWireReceiverFactoryPids()) {
            if ((m_receivers != null) && !m_receivers.contains(receiver)) {
                m_receivers.add(receiver);
            }
        }

        m_components = config.getWireComponents();
        m_wires = config.getWiresConfigurationJson();
        m_graph = config.getGraph();
        m_wireComponentsConfigJson = config.getWireComponentsJson();
        m_wireConfigsJson = config.getWireConfigurationsJson();

        factoryPid.setVisible(false);
        btnDelete.setEnabled(false);
        btnDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                deleteModal.show();
            }
        });
        btnGraphDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                deleteGraphModal.show();
            }
        });
        btnDeleteGraphYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.setDirty(true);
                deleteGraphModal.hide();
            }
        });
        btnSave.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final List<String> pids = new ArrayList<String>();
                for (final Map.Entry<String, PropertiesUi> entry : m_propertiesUis.entrySet()) {
                    final PropertiesUi ui = entry.getValue();
                    final String componentId = ui.getConfiguration().getComponentId();
                    if (ui.isDirty() && !ui.isNonValidated()) {
                        pids.add(getFormattedPid(componentId));
                    }
                }
                if (!pids.isEmpty()) {
                    saveGraphAlert.setType(AlertType.DANGER);
                    saveGraphAlertText.setText(
                            "Some Wire Component instances' configurations are dirty. Do you still want to save the Wire Graph?");
                } else {
                    saveGraphAlert.setType(AlertType.INFO);
                    saveGraphAlertText.setText("Are you sure you want to save the Wire Graph?");
                }
                saveModal.show();
            }
        });
        btnDeleteYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.setDirty(true);
                deleteModal.hide();
            }
        });
        btnSaveYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                for (final Map.Entry<String, PropertiesUi> entry : m_propertiesUis.entrySet()) {
                    final PropertiesUi ui = entry.getValue();
                    ui.setDirty(false);
                }
                saveModal.hide();
            }
        });
        propertiesPanel.setVisible(false);
        populateDrivers();
        populateComponentsPanel();
        loadGraph();
        exportJSNIUpdateWireConfig();
        exportJSNIUpdateSelection();
        exportJSNIShowDuplicatePidModal();
        exportJSNIShowAddNotAllowedModal();
        exportJSNIshowCycleExistenceError();
        exportJSNImakeUiDirty();
        exportJSNIDeactivateNavPils();
    }

    public static boolean isDirty() {
        return isDirty;
    }

    // ----------------------------------------------------------------
    //
    // JSNI
    //
    // ----------------------------------------------------------------
    public static void jsniDeactivateNavPils() {
        for (int i = 0; i < wireComponentsMenu.getWidgetCount(); i++) {
            final AnchorListItem item = (AnchorListItem) wireComponentsMenu.getWidget(i);
            if (item.isActive()) {
                item.setActive(false);
            }
        }
        componentName.clear();
    }

    public static void jsniMakeUiDirty() {
        WiresPanelUi.setDirty(true);
    }

    public static void jsniShowAddNotAllowedModal() {
        errorAlertText.setText(
                "You have deleted a Wire Component instance. Please save the Wire Graph first to continue adding further Wire Component instance(s) to the Wire Graph.");
        errorModal.show();
        assetModal.hide();
    }

    public static void jsniShowCycleExistenceError() {
        errorAlertText
                .setText("There exists cycle(s) in the created Wire Graph. Please remove the cycle(s) to continue.");
        errorModal.show();
    }

    public static void jsniShowDuplicatePidModal(final String pid) {
        errorAlertText.setText("The given name " + pid
                + " has already been assigned to an existing Wire Component instance. Please use something different.");
        errorModal.show();
        assetModal.hide();
    }

    public static void jsniUpdateSelection(final String pid, final String factoryPid) {
        if ("".equals(pid)) {
            btnDelete.setEnabled(false);
            propertiesPanel.setVisible(false);
            return;
        }
        // enable delete instance button
        btnDelete.setEnabled(true);
        // Retrieve GwtComponentConfiguration to use for manipulating the
        // properties.
        // If it is already present in the map, it means the component has
        // already been
        // accessed by the graph, and its configuration has already been
        // gathered from the ConfigurationService.
        if (m_configs.containsKey(pid)) {
            fillProperties(m_configs.get(pid), pid);
        } else {
            // else we get the GwtComponentConfiguration from the
            // ConfigurationService
            gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(final Throwable ex) {
                    EntryClassUi.hideWaitModal();
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(final GwtXSRFToken token) {
                    Map<String, Object> temporaryMap = null;
                    if ("org.eclipse.kura.wire.WireAsset".equalsIgnoreCase(factoryPid)) {
                        temporaryMap = new HashMap<String, Object>();
                        temporaryMap.put("asset.desc", "Sample Asset");
                        temporaryMap.put("driver.pid", getDriver(pid));
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
                            m_configs.put(pid, result);
                            if (m_propertiesUis.containsKey(pid)) {
                                m_propertiesUis.remove(pid);
                            }
                            EntryClassUi.hideWaitModal();
                        }
                    });
                }
            });
        }
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
                if ((currentSelection != null) && (propertiesUi != null)) {
                    propertiesUi.getUpdatedConfiguration();
                }
                gwtWireService.updateWireConfiguration(token, obj, m_configs,
                        new AsyncCallback<GwtWiresConfiguration>() {

                    @Override
                    public void onFailure(final Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(final GwtWiresConfiguration result) {
                        internalLoad(result);
                        EntryClassUi.hideWaitModal();
                        btnSave.setEnabled(false);
                        btnSave.setText("Save");
                        isDirty = false;
                        if ((m_configs != null) && !m_configs.isEmpty()) {
                            m_configs.clear();
                        }
                        if ((m_propertiesUis != null) && !m_propertiesUis.isEmpty()) {
                            m_propertiesUis.clear();
                        }
                    }
                });
            }
        });

        return 0;
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
                        m_wireComponentsConfigJson = result.getWireComponentsJson();
                        m_wireConfigsJson = result.getWireConfigurationsJson();
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
        for (final String emitter : m_emitters) {
            emitters.set(i, new JSONString(emitter));
            i++;
        }
        i = 0;
        for (final String receiver : m_receivers) {
            receivers.set(i, new JSONString(receiver));
            i++;
        }
        i = 0;
        for (final String driver : m_drivers) {
            drivers.set(i, new JSONString(driver));
            i++;
        }

        obj.put("pFactories", emitters);
        obj.put("cFactories", receivers);
        obj.put("drivers", drivers);
        obj.put("components", createComponentsJson());
        obj.put("wires", JSONParser.parseStrict(m_wires));
        obj.put("pGraph", JSONParser.parseStrict(m_graph));
        obj.put("wireComponentsJson", JSONParser.parseStrict(m_wireComponentsConfigJson));
        obj.put("wireConfigsJson", JSONParser.parseStrict(m_wireConfigsJson));

        wiresOpen(obj.toString());
        btnSave.setEnabled(false);
    }

    private static void populateComponentsPanel() {
        final List<String> onlyProducers = new ArrayList<String>(m_emitters);
        final List<String> onlyConsumers = new ArrayList<String>(m_receivers);
        final List<String> both = getCommonElements(m_emitters, m_receivers);
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
        driverPids.clear();
        driverPids.addItem("--- Select Driver ---");
        for (final String driver : m_drivers) {
            driverPids.addItem(driver);
        }
    }

    public static void render(final GwtConfigComponent item, String pid) {
        if ((currentSelection != null) && (propertiesUi != null)) {
            propertiesUi.getUpdatedConfiguration();
        }
        if (item != null) {
            propertiesPanelBody.clear();

            if (!m_propertiesUis.containsKey(pid)) {
                propertiesUi = new PropertiesUi(item, pid);
                m_propertiesUis.put(pid, propertiesUi);
            } else {
                propertiesUi = m_propertiesUis.get(pid);
            }
            propertiesPanel.setVisible(true);
            if (pid == null) {
                pid = "";
            }

            if (propertiesUi.isDirty()) {
                propertiesPanelHeader.setText(getFormattedPid(item.getFactoryId()) + " - " + pid + "*");
            } else {
                propertiesPanelHeader.setText(getFormattedPid(item.getFactoryId()) + " - " + pid);
            }
            currentSelection = item;
            propertiesPanelBody.add(propertiesUi);
        } else {
            propertiesPanelBody.clear();
            propertiesPanelHeader.setText("No component selected");
            propertiesPanel.setVisible(false);
            currentSelection = null;
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
                        m_drivers.clear();
                        m_drivers.addAll(result);
                        internalLoad(configuration);
                        EntryClassUi.hideWaitModal();
                    }
                });
            }
        });
    }

    public static native void resetDeleteComponentState()
    /*-{
        $wnd.kuraWires.resetDeleteComponentState();
    }-*/;

    public static void setDirty(final boolean flag) {
        // if WiresPanelUI is already dirty, there is no need to make it dirty
        // again and again
        if (!isDirty) {
            btnSave.setEnabled(true);
            btnSave.setText("Save*");
            isDirty = flag;
        }
    }

    public static native void wiresOpen(String obj)
    /*-{
    	$wnd.kuraWires.render(obj);
    }-*/;

    public WiresPanelUi() {
        this.initWidget(uiBinder.createAndBindUi(this));
        m_emitters = new ArrayList<String>();
        m_receivers = new ArrayList<String>();
        m_components = new ArrayList<String>();
        m_drivers = new ArrayList<String>();
        m_propertiesUis = new HashMap<String, PropertiesUi>();
        wiresPanelUi = this;
    }

}