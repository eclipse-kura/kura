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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.wires.composer.BlinkEffect;
import org.eclipse.kura.web.client.ui.wires.composer.DropEvent;
import org.eclipse.kura.web.client.ui.wires.composer.Wire;
import org.eclipse.kura.web.client.ui.wires.composer.WireComponent;
import org.eclipse.kura.web.client.ui.wires.composer.WireComposer;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Row;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite
        implements WireComposer.Listener, HasConfiguration.Listener, WiresDialogs.Listener {

    private static final WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    static final String WIRE_ASSET = "WireAsset";
    static final String FACTORY_PID_DROP_PREFIX = "factory://";
    private static final String ASSET_DESCRIPTION_PROP = "asset.desc";
    private static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";
    private static final String DRIVER_PID = "driver.pid";

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
    Panel propertiesPanel;
    @UiField
    PanelBody propertiesPanelBody;
    @UiField
    PanelHeader propertiesPanelHeader;

    @UiField
    Row configurationRow;

    @UiField
    NavPills wireComponentsMenu;

    @UiField
    Widget composer;

    @UiField
    WiresDialogs dialogs;

    @UiField
    AlertDialog confirmDialog;

    interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
    }

    private List<String> components;
    private final Map<String, GwtConfigComponent> configs = new HashMap<>();
    private final List<String> drivers;
    private final List<String> emitters;

    private String graph;
    private final List<String> receivers;
    private String wireComponentsConfigJson;
    private String wireConfigsJson;
    private String wires;

    private WireComposer wireComposer;
    private BlinkEffect blinkEffect;

    private WireComponentDescriptors descriptors = new WireComponentDescriptors();
    private Configurations configurations = new Configurations();

    public WiresPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));
        this.emitters = new ArrayList<>();
        this.receivers = new ArrayList<>();
        this.components = new ArrayList<>();
        this.drivers = new ArrayList<>();

        this.wireComposer = WireComposer.create(composer.getElement());
        this.wireComposer.setListener(this);

        this.blinkEffect = BlinkEffect.create(wireComposer);

        this.dialogs.setListener(this);
        initButtons();
    }

    private void initButtons() {
        this.btnZoomIn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.wireComposer.zoomIn();
            }
        });
        this.btnZoomOut.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.wireComposer.zoomOut();
            }
        });
        this.btnZoomFit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresPanelUi.this.wireComposer.fitContent(true);
            }
        });
        this.btnSave.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                final List<String> invalidConfigurationPids = configurations.getInvalidConfigurationPids();
                if (!invalidConfigurationPids.isEmpty()) {
                    confirmDialog.show(
                            MSGS.wiresConfigurationInvalid(
                                    invalidConfigurationPids.toString().replaceAll("[\\[\\]]", "")),
                            AlertDialog.Severity.ALERT, null);
                    return;
                }
                confirmDialog.show(MSGS.wiresSave(), new AlertDialog.Listener() {

                    @Override
                    public void onConfirm() {

                        WiresRPC.updateWiresConfiguration(getGwtWireGraphConfiguration(),
                                getModifiedDriverConfigurations(), new WiresRPC.Callback<Void>() {

                                    @Override
                                    public void onSuccess(Void result) {
                                        clearDirtyState();
                                    }
                                });
                    }
                });
            }
        });
        this.btnReset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                WiresPanelUi.this.confirmDialog.show(MSGS.deviceConfigDirty(), new AlertDialog.Listener() {

                    @Override
                    public void onConfirm() {
                        WiresPanelUi.this.load();
                    }
                });
            }
        });
        this.btnDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                confirmDialog.show(MSGS.wiresComponentDeleteAlert(), new AlertDialog.Listener() {

                    @Override
                    public void onConfirm() {
                        final WireComponent component = wireComposer.getSelectedWireComponent();
                        if (component != null) {
                            wireComposer.deleteWireComponent(component);
                        }
                    }
                });
            }
        });
        this.btnGraphDelete.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(final ClickEvent event) {
                confirmDialog.show(MSGS.wiresGraphDeleteAlert(), new AlertDialog.Listener() {

                    @Override
                    public void onConfirm() {
                        wireComposer.clear();
                    }
                });
            }
        });
    }

    public boolean isDirty() {
        return this.configurations.isDirty() || this.wireComposer.isDirty();
    }

    public void load() {

        WiresRPC.loadStaticInfo(new WiresRPC.Callback<GwtWireComposerStaticInfo>() {

            @Override
            public void onSuccess(GwtWireComposerStaticInfo result) {
                loadStaticInformation(result);
                WiresRPC.loadWiresConfiguration(new WiresRPC.Callback<GwtWireGraphConfiguration>() {

                    @Override
                    public void onSuccess(GwtWireGraphConfiguration result) {
                        loadGraph(result);
                        dialogs.setDriverPids(configurations.getDriverPids());
                        dialogs.setAssetPids(getAssetsNotInComposer());
                        configurations.setAllActivePids(result.getAllActivePids());
                        blinkEffect.setEnabled(true);
                        clearDirtyState();
                    }
                });
            }
        });
    }

    private void populateComponentsPanel() {
        this.wireComponentsMenu.clear();

        final WireComponentsAnchorListItem.Listener listener = new WireComponentsAnchorListItem.Listener() {

            @Override
            public void onClick(String factoryPid) {
                WiresPanelUi.this.showComponentCreationDialog(factoryPid);
            }
        };

        final List<GwtWireComponentDescriptor> sortedDescriptors = new ArrayList<>();

        for (Entry<String, GwtWireComponentDescriptor> descriptor : descriptors.getDescriptors().entrySet()) {
            sortedDescriptors.add(descriptor.getValue());
        }

        Collections.sort(sortedDescriptors, new Comparator<GwtWireComponentDescriptor>() {

            @Override
            public int compare(GwtWireComponentDescriptor o1, GwtWireComponentDescriptor o2) {
                return Integer.compare(o1.getMinInputPorts() * 2 + o1.getMinOutputPorts(),
                        o2.getMinInputPorts() * 2 + o2.getMinOutputPorts());
            }
        });

        for (GwtWireComponentDescriptor descriptor : sortedDescriptors) {
            final WireComponentsAnchorListItem item = new WireComponentsAnchorListItem(
                    getFormattedPid(descriptor.getFactoryPid()), descriptor.getFactoryPid(),
                    descriptor.getMinInputPorts() > 0, descriptor.getMinOutputPorts() > 0);
            item.setListener(listener);
            this.wireComponentsMenu.add(item);
        }
    }

    private void loadStaticInformation(GwtWireComposerStaticInfo staticInfo) {
        this.configurations.setChannelDescriptiors(staticInfo.getDriverDescriptors());
        this.configurations.setBaseChannelDescriptor(staticInfo.getBaseChannelDescriptor());
        this.configurations.setComponentDefinitions(staticInfo.getComponentDefinitions());
        this.descriptors.setDescriptors(staticInfo.getWireComponentDescriptors());
        this.dialogs.setDriverFactoryPids(configurations.getDriverFactoryPids());
        populateComponentsPanel();
    }

    private void loadGraph(GwtWireGraphConfiguration configuration) {
        wireComposer.clear();
        final List<GwtConfigComponent> configurationList = new ArrayList<>();

        for (GwtWireComponentConfiguration config : configuration.getWireComponentConfigurations()) {
            configurationList.add(config.getConfiguration());
            wireComposer.addWireComponent(WireComponent.fromGwt(config));
        }

        for (GwtWireConfiguration config : configuration.getWires()) {
            wireComposer.addWire(Wire.fromGwt(config));
        }

        configurationList.addAll(configuration.getAdditionalConfigurations());
        configurations.setComponentConfigurations(configurationList);

        wireComposer.fitContent(false);
    }

    private GwtWireGraphConfiguration getGwtWireGraphConfiguration() {
        final GwtWireGraphConfiguration result = new GwtWireGraphConfiguration();

        final List<GwtWireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();

        for (WireComponent component : wireComposer.getWireComponents()) {
            wireComponentConfigurations
                    .add(component.toGwt(configurations.getConfiguration(component.getPid()).getConfiguration()));
        }

        final List<GwtWireConfiguration> wires = new ArrayList<>();

        for (Wire wire : wireComposer.getWires()) {
            wires.add(wire.toGwt());
        }

        result.setWireComponentConfigurations(wireComponentConfigurations);
        result.setWires(wires);

        return result;
    }

    private List<GwtConfigComponent> getModifiedDriverConfigurations() {
        List<GwtConfigComponent> result = new ArrayList<>();
        for (HasConfiguration config : configurations.getConfigurations()) {
            if (config.isDirty() && config.isValid()) {
                final GwtConfigComponent gwtConfig = config.getConfiguration();
                if (gwtConfig.isDriver()) {
                    result.add(gwtConfig);
                }
            }
        }
        return result;
    }

    public void showConfigurationArea(HasConfiguration configuration) {

        Window.scrollTo(0, 0);
        this.configurationRow.setVisible(false);
        this.configurationRow.clear();

        final ConfigurationAreaUi configurationAreaUi = new ConfigurationAreaUi(configuration, this.configurations);
        configurationAreaUi.setListener(this);

        this.configurationRow.add(configurationAreaUi);
        this.configurationRow.setVisible(true);
    }

    public static String getFormattedPid(final String pid) {
        String[] split;
        if (pid.contains(".")) {
            split = pid.split("\\.");
            final String lastString = split[split.length - 1];
            if ("WireAsset".equalsIgnoreCase(lastString)) {
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

    private WireComponent createAsset(String pid, String driverPid) {
        final HasConfiguration assetConfig = configurations.createAndRegisterConfiguration(pid, WIRE_ASSET_PID);
        assetConfig.getConfiguration().getParameter(AssetConstants.ASSET_DRIVER_PROP.value()).setValue(driverPid);
        return descriptors.createNewComponent(pid, WIRE_ASSET_PID);
    }

    public void showComponentCreationDialog(final String factoryPid) {
        if (descriptors.getDescriptor(factoryPid) == null) {
            return;
        }
        this.dialogs.pickComponent(factoryPid, new WiresDialogs.Callback() {

            @Override
            public void onNewComponentCreated(String pid) {
                wireComposer.addWireComponent(descriptors.createNewComponent(pid, factoryPid));
            }

            @Override
            public void onNewAssetCreated(String pid, String driverPid) {
                wireComposer.addWireComponent(createAsset(pid, driverPid));
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void hideConfigurationArea() {
        this.configurationRow.clear();
        this.configurationRow.setVisible(false);
    }

    public void clearDirtyState() {
        this.wireComposer.clearDirtyState();
        this.configurations.clearDirtyState();
        updateDirtyState();
        this.wireComposer.deselectWireCompoent();
    }

    public void updateDirtyState() {
        final boolean isDirty = isDirty();
        this.btnSave.setEnabled(isDirty);
        this.btnReset.setEnabled(isDirty);
    }

    public void unload() {
        blinkEffect.setEnabled(false);
    }

    private List<String> getAssetsNotInComposer() {
        final List<String> assetPids = this.configurations.getFactoryInstancesPids(WIRE_ASSET_PID);
        final Iterator<String> i = assetPids.iterator();
        while (i.hasNext()) {
            final String pid = i.next();
            if (wireComposer.getWireComponent(pid) != null) {
                i.remove();
            }
        }
        return assetPids;
    }

    @Override
    public void onWireCreated(Wire wire) {
        updateDirtyState();
    }

    @Override
    public void onWireDeleted(Wire wire) {
        updateDirtyState();
    }

    @Override
    public void onWireChanged(Wire wire) {
        updateDirtyState();
    }

    @Override
    public void onWireComponentSelected(WireComponent component) {
        this.btnDelete.setEnabled(true);
        final HasConfiguration configuration = configurations.getConfiguration(component.getPid());
        if (configuration != null) {
            showConfigurationArea(configuration);
        }
    }

    @Override
    public void onWireComponentDeselected(WireComponent component) {
        this.btnDelete.setEnabled(false);
        hideConfigurationArea();
    }

    @Override
    public void onWireComponentChanged(WireComponent component) {
        updateDirtyState();
    }

    @Override
    public void onWireComponentCreated(WireComponent component) {
        this.btnGraphDelete.setEnabled(true);
        updateDirtyState();
        if (configurations.getConfiguration(component.getPid()) == null) {
            configurations.createAndRegisterConfiguration(component.getPid(), component.getFactoryPid());
        }
        this.dialogs.setAssetPids(getAssetsNotInComposer());
    }

    @Override
    public void onWireComponentDeleted(WireComponent component) {
        configurations.deleteConfiguration(component.getPid());
        updateDirtyState();
        this.btnGraphDelete.setEnabled(wireComposer.getWireComponentCount() > 0);
        this.dialogs.setAssetPids(getAssetsNotInComposer());
    }

    @Override
    public void onDrop(final DropEvent event) {
        final String attachment = event.getAttachment();
        if (attachment == null || !attachment.startsWith(FACTORY_PID_DROP_PREFIX)) {
            event.cancel();
            return;
        }
        final String factoryPid = attachment.substring(FACTORY_PID_DROP_PREFIX.length());
        this.dialogs.pickComponent(factoryPid, new WiresDialogs.Callback() {

            @Override
            public void onNewComponentCreated(String pid) {
                event.complete(descriptors.createNewComponent(pid, factoryPid));
            }

            @Override
            public void onNewAssetCreated(String pid, String driverPid) {
                event.complete(createAsset(pid, driverPid));
            }

            @Override
            public void onCancel() {
                event.cancel();
            }
        });
    }

    @Override
    public void onConfigurationChanged(HasConfiguration newConfiguration) {
        this.configurations.setConfiguration(newConfiguration);
        updateDirtyState();
    }

    @Override
    public boolean onNewPidInserted(String pid) {
        boolean isPidValid = this.wireComposer.getWireComponent(pid) == null && !this.configurations.isPidExisting(pid);
        if (!isPidValid) {
            confirmDialog.show(MSGS.wiresComponentNameAlreadyUsed(pid), AlertDialog.Severity.ALERT, null);
        }
        return isPidValid;
    }

    @Override
    public void onNewDriverCreated(String pid, String factoryPid, GwtConfigComponent descriptor) {
        configurations.createAndRegisterConfiguration(pid, factoryPid);
        configurations.setChannelDescriptor(pid, descriptor);
        dialogs.setDriverPids(configurations.getDriverPids());
    }

    @Override
    public void onDirtyStateChanged(HasConfiguration hasConfiguration) {
    }

}
