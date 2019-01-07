/*******************************************************************************
 * Copyright (c) 2016, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *  Red Hat Inc
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.kura.web.client.configuration.Configurations;
import org.eclipse.kura.web.client.configuration.HasConfiguration;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.wires.composer.BlinkEffect;
import org.eclipse.kura.web.client.ui.wires.composer.DropEvent;
import org.eclipse.kura.web.client.ui.wires.composer.PortNames;
import org.eclipse.kura.web.client.ui.wires.composer.Wire;
import org.eclipse.kura.web.client.ui.wires.composer.WireComponent;
import org.eclipse.kura.web.client.ui.wires.composer.WireComposer;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.IdHelper;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.constants.AlertType;

import com.google.gwt.core.client.GWT;
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
    private static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";
    private static final String DRIVER_PID = "driver.pid";

    @UiField
    Button btnSave;
    @UiField
    Button btnReset;
    @UiField
    Button btnDownload;
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

    private WireComposer wireComposer;
    private BlinkEffect blinkEffect;

    private WireComponentDescriptors descriptors = new WireComponentDescriptors();
    private Configurations configurations = new Configurations();

    public WiresPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.wireComposer = WireComposer.create(composer.getElement());
        this.wireComposer.setListener(this);

        this.blinkEffect = BlinkEffect.create(wireComposer);

        this.dialogs.setListener(this);
        initButtons();
    }

    private void initButtons() {
        this.btnZoomIn.addClickHandler(event -> WiresPanelUi.this.wireComposer.zoomIn());
        this.btnZoomOut.addClickHandler(event -> WiresPanelUi.this.wireComposer.zoomOut());
        this.btnZoomFit.addClickHandler(event -> WiresPanelUi.this.wireComposer.fitContent(true));
        this.btnSave.addClickHandler(event -> {
            final List<String> invalidConfigurationPids = configurations.getInvalidConfigurationPids();
            if (!invalidConfigurationPids.isEmpty()) {
                confirmDialog.show(
                        MSGS.cannotSaveWiresConfigurationInvalid(
                                invalidConfigurationPids.toString().replaceAll("[\\[\\]]", "")),
                        AlertDialog.Severity.ALERT, null);
                return;
            }
            confirmDialog.show(MSGS.wiresSave(), () -> WiresRPC.updateWiresConfiguration(getGwtWireGraphConfiguration(),
                    getModifiedDriverConfigurations(), result -> clearDirtyState()));
        });
        this.btnReset.addClickHandler(
                event -> WiresPanelUi.this.confirmDialog.show(MSGS.deviceConfigDirty(), WiresPanelUi.this::load));
        this.btnDelete.addClickHandler(event -> confirmDialog.show(MSGS.wiresComponentDeleteAlert(), () -> {
            final WireComponent component = wireComposer.getSelectedWireComponent();
            if (component != null) {
                wireComposer.deleteWireComponent(component);
            }
        }));
        this.btnGraphDelete
                .addClickHandler(event -> confirmDialog.show(MSGS.wiresGraphDeleteAlert(), () -> wireComposer.clear()));
        this.btnDownload.addClickHandler(event -> {
            final List<String> invalidConfigurationPids = configurations.getInvalidConfigurationPids();
            if (!invalidConfigurationPids.isEmpty()) {
                confirmDialog.show(
                        MSGS.cannotDownloadSnapshotWiresConfigurationInvalid(
                                invalidConfigurationPids.toString().replaceAll("[\\[\\]]", "")),
                        AlertDialog.Severity.ALERT, null);
                return;
            }
            WiresRPC.downloadWiresSnapshot();
        });
    }

    public boolean isDirty() {
        return this.configurations.isDirty() || this.wireComposer.isDirty();
    }

    public void load() {

        WiresRPC.loadWireGraph(result -> {
            loadStaticInformation(result.getStaticInfo());
            loadGraph(result.getWireGraphConfiguration());
            dialogs.setDriverPids(configurations.getDriverPids());
            dialogs.setAssetPids(getAssetsNotInComposer());
            configurations.setAllActivePids(result.getWireGraphConfiguration().getAllActivePids());
            blinkEffect.setEnabled(true);
            clearDirtyState();
        });

    }

    private void populateComponentsPanel() {
        this.wireComponentsMenu.clear();

        final WireComponentsAnchorListItem.Listener listener = WiresPanelUi.this::showComponentCreationDialog;

        final List<GwtWireComponentDescriptor> sortedDescriptors = new ArrayList<>();

        for (Entry<String, GwtWireComponentDescriptor> descriptor : descriptors.getDescriptors().entrySet()) {
            sortedDescriptors.add(descriptor.getValue());
        }

        Collections.sort(sortedDescriptors,
                (o1, o2) -> Integer.compare(o1.getMinInputPorts() * 2 + o1.getMinOutputPorts(),
                        o2.getMinInputPorts() * 2 + o2.getMinOutputPorts()));

        for (GwtWireComponentDescriptor descriptor : sortedDescriptors) {
            final WireComponentsAnchorListItem item = new WireComponentsAnchorListItem(getComponentLabel(descriptor),
                    descriptor.getFactoryPid(), descriptor.getMinInputPorts() > 0, descriptor.getMinOutputPorts() > 0);
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
            final WireComponent component = WireComponent.fromGwt(config);
            final GwtWireComponentDescriptor descriptor = descriptors.getDescriptor(component.getFactoryPid());
            // TODO: place port names in GwtWireComponentConfiguration
            if (descriptor != null) {
                component.getRenderingProperties().setInputPortNames(PortNames.fromMap(descriptor.getInputPortNames()));
                component.getRenderingProperties()
                        .setOutputPortNames(PortNames.fromMap(descriptor.getOutputPortNames()));
            }
            wireComposer.addWireComponent(component);
        }

        for (GwtWireConfiguration config : configuration.getWires()) {
            wireComposer.addWire(Wire.fromGwt(config));
        }

        configurationList.addAll(configuration.getAdditionalConfigurations());
        configurations.setComponentConfigurations(configurationList);

        for (final WireComponent component : this.wireComposer.getWireComponents()) {
            try {
                validateAndGetWireComponentConfiguration(component.getPid());
                component.setValid(true);
            } catch (Exception e) {
                component.setValid(false);
            }
        }

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

    private void clearConfigurationArea() {
        Window.scrollTo(0, 0);
        this.configurationRow.setVisible(false);
        this.configurationRow.clear();
    }

    private void showConfigurationArea(final Widget content) {
        this.configurationRow.add(content);
        this.configurationRow.setVisible(true);
    }

    public void showConfigurationArea(HasConfiguration configuration) {

        clearConfigurationArea();

        final ConfigurationAreaUi configurationAreaUi = new ConfigurationAreaUi(configuration, this.configurations);
        configurationAreaUi.setListener(this);

        showConfigurationArea(configurationAreaUi);
    }

    public void showConfigurationErrorMessage(String message) {

        clearConfigurationArea();
        showConfigurationArea(new Alert(message, AlertType.DANGER));

    }

    public static String getComponentLabel(final GwtConfigComponent config) {
        if (config.getComponentName() != null) {
            return config.getComponentName();
        }
        return IdHelper.getLastIdComponent(config.getFactoryId());
    }

    public static String getComponentLabel(final GwtWireComponentDescriptor descriptor) {
        if (descriptor.getName() != null) {
            return descriptor.getName();
        }
        return IdHelper.getLastIdComponent(descriptor.getFactoryPid());
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
        this.btnDownload.setEnabled(!isDirty);
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

    private HasConfiguration validateAndGetWireComponentConfiguration(String pid) {
        try {
            final HasConfiguration configuration = configurations.getConfiguration(pid);
            if (configuration == null) {
                throw new IllegalArgumentException(MSGS.errorComponentConfigurationMissing(pid));
            }
            GwtConfigComponent gwtConfig = configuration.getConfiguration();
            if (gwtConfig == null || !gwtConfig.isValid()) {
                throw new IllegalArgumentException(MSGS.errorComponentConfigurationMissing(pid));
            }
            if (WIRE_ASSET_PID.equals(gwtConfig.getFactoryId())) {
                final String driverPid = gwtConfig.getParameterValue(DRIVER_PID);
                if (configurations.getConfiguration(driverPid) == null) {
                    throw new IllegalArgumentException(MSGS.errorDriverConfigurationMissing(pid, driverPid));
                }
                if (configurations.getChannelDescriptor(driverPid) == null) {
                    throw new IllegalArgumentException(MSGS.errorDriverDescriptorMissingForAsset(pid, driverPid));
                }
            }
            return configuration;
        } catch (IllegalArgumentException e) {
            configurations.invalidateConfiguration(pid);
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(MSGS.errorUnexpectedErrorLoadingComponentConfig(pid));
        }
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
        try {
            this.btnDelete.setEnabled(true);
            showConfigurationArea(validateAndGetWireComponentConfiguration(component.getPid()));
        } catch (Exception e) {
            showConfigurationErrorMessage(e.getMessage());
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
        updateDirtyState();
    }

}
