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
import org.eclipse.kura.web.client.ui.AlertDialog.ConfirmListener;
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

    private final WireComposer wireComposer;
    private final BlinkEffect blinkEffect;

    private final WireComponentDescriptors descriptors = new WireComponentDescriptors();
    private final Configurations configurations = new Configurations();

    public WiresPanelUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.wireComposer = WireComposer.create(this.composer.getElement());
        this.wireComposer.setListener(this);

        this.blinkEffect = BlinkEffect.create(this.wireComposer);

        this.dialogs.setListener(this);
        initButtons();
    }

    private void initButtons() {
        this.btnZoomIn.addClickHandler(event -> WiresPanelUi.this.wireComposer.zoomIn());
        this.btnZoomOut.addClickHandler(event -> WiresPanelUi.this.wireComposer.zoomOut());
        this.btnZoomFit.addClickHandler(event -> WiresPanelUi.this.wireComposer.fitContent(true));
        this.btnSave.addClickHandler(event -> {
            final List<String> invalidConfigurationPids = this.configurations.getInvalidConfigurationPids();
            if (!invalidConfigurationPids.isEmpty()) {
                this.confirmDialog.show(
                        MSGS.cannotSaveWiresConfigurationInvalid(
                                invalidConfigurationPids.toString().replaceAll("[\\[\\]]", "")),
                        AlertDialog.Severity.ALERT, (ConfirmListener) null);
                return;
            }
            this.confirmDialog.show(MSGS.wiresSave(),
                    () -> WiresRPC.updateWiresConfiguration(getGwtWireGraphConfiguration(),
                            getModifiedDriverConfigurations(), result -> clearDirtyState()));
        });
        this.btnReset.addClickHandler(
                event -> WiresPanelUi.this.confirmDialog.show(MSGS.deviceConfigDirty(), WiresPanelUi.this::load));
        this.btnDelete.addClickHandler(event -> this.confirmDialog.show(MSGS.wiresComponentDeleteAlert(), () -> {
            final WireComponent component = this.wireComposer.getSelectedWireComponent();
            if (component != null) {
                this.wireComposer.deleteWireComponent(component);
            }
        }));
        this.btnGraphDelete.addClickHandler(
                event -> this.confirmDialog.show(MSGS.wiresGraphDeleteAlert(), () -> this.wireComposer.clear()));
        this.btnDownload.addClickHandler(event -> {
            final List<String> invalidConfigurationPids = this.configurations.getInvalidConfigurationPids();
            if (!invalidConfigurationPids.isEmpty()) {
                this.confirmDialog.show(
                        MSGS.cannotDownloadSnapshotWiresConfigurationInvalid(
                                invalidConfigurationPids.toString().replaceAll("[\\[\\]]", "")),
                        AlertDialog.Severity.ALERT, (ConfirmListener) null);
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
            this.dialogs.setDriverPids(this.configurations.getDriverPids());
            this.dialogs.setAssetPids(getAssetsNotInComposer());
            this.configurations.setAllActivePids(result.getWireGraphConfiguration().getAllActivePids());
            this.blinkEffect.setEnabled(true);
            clearDirtyState();
        });

    }

    private void populateComponentsPanel() {
        this.wireComponentsMenu.clear();

        final WireComponentsAnchorListItem.Listener listener = WiresPanelUi.this::showComponentCreationDialog;

        final List<GwtWireComponentDescriptor> sortedDescriptors = new ArrayList<>();

        for (Entry<String, GwtWireComponentDescriptor> descriptor : this.descriptors.getDescriptors().entrySet()) {
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
        this.dialogs.setDriverFactoryPids(this.configurations.getDriverFactoryPids());
        populateComponentsPanel();
    }

    private void loadGraph(GwtWireGraphConfiguration configuration) {
        this.wireComposer.clear();
        final List<GwtConfigComponent> configurationList = new ArrayList<>();

        for (GwtWireComponentConfiguration config : configuration.getWireComponentConfigurations()) {
            configurationList.add(config.getConfiguration());
            final WireComponent component = WireComponent.fromGwt(config);
            final GwtWireComponentDescriptor descriptor = this.descriptors.getDescriptor(component.getFactoryPid());
            // TODO: place port names in GwtWireComponentConfiguration
            if (descriptor != null) {
                component.getRenderingProperties().setInputPortNames(PortNames.fromMap(descriptor.getInputPortNames()));
                component.getRenderingProperties()
                        .setOutputPortNames(PortNames.fromMap(descriptor.getOutputPortNames()));
            }
            this.wireComposer.addWireComponent(component);
        }

        for (GwtWireConfiguration config : configuration.getWires()) {
            this.wireComposer.addWire(Wire.fromGwt(config));
        }

        configurationList.addAll(configuration.getAdditionalConfigurations());
        this.configurations.setComponentConfigurations(configurationList);

        for (final WireComponent component : this.wireComposer.getWireComponents()) {
            try {
                validateAndGetWireComponentConfiguration(component.getPid());
                component.setValid(true);
            } catch (Exception e) {
                component.setValid(false);
            }
        }

        this.wireComposer.fitContent(false);
    }

    private GwtWireGraphConfiguration getGwtWireGraphConfiguration() {
        final GwtWireGraphConfiguration result = new GwtWireGraphConfiguration();

        final List<GwtWireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();

        for (WireComponent component : this.wireComposer.getWireComponents()) {
            wireComponentConfigurations
                    .add(component.toGwt(this.configurations.getConfiguration(component.getPid()).getConfiguration()));
        }

        final List<GwtWireConfiguration> wires = new ArrayList<>();

        for (Wire wire : this.wireComposer.getWires()) {
            wires.add(wire.toGwt());
        }

        result.setWireComponentConfigurations(wireComponentConfigurations);
        result.setWires(wires);

        return result;
    }

    private List<GwtConfigComponent> getModifiedDriverConfigurations() {
        List<GwtConfigComponent> result = new ArrayList<>();
        for (HasConfiguration config : this.configurations.getConfigurations()) {
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
        final HasConfiguration assetConfig = this.configurations.createAndRegisterConfiguration(pid, WIRE_ASSET_PID);
        assetConfig.getConfiguration().getParameter(AssetConstants.ASSET_DRIVER_PROP.value()).setValue(driverPid);
        return this.descriptors.createNewComponent(pid, WIRE_ASSET_PID);
    }

    public void showComponentCreationDialog(final String factoryPid) {
        if (this.descriptors.getDescriptor(factoryPid) == null) {
            return;
        }
        this.dialogs.pickComponent(factoryPid, new WiresDialogs.Callback() {

            @Override
            public void onNewComponentCreated(String pid) {
                WiresPanelUi.this.wireComposer
                        .addWireComponent(WiresPanelUi.this.descriptors.createNewComponent(pid, factoryPid));
            }

            @Override
            public void onNewAssetCreated(String pid, String driverPid) {
                WiresPanelUi.this.wireComposer.addWireComponent(createAsset(pid, driverPid));
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
        this.blinkEffect.setEnabled(false);
    }

    private List<String> getAssetsNotInComposer() {
        final List<String> assetPids = this.configurations.getFactoryInstancesPids(WIRE_ASSET_PID);
        final Iterator<String> i = assetPids.iterator();
        while (i.hasNext()) {
            final String pid = i.next();
            if (this.wireComposer.getWireComponent(pid) != null) {
                i.remove();
            }
        }
        return assetPids;
    }

    private HasConfiguration validateAndGetWireComponentConfiguration(String pid) {
        try {
            final HasConfiguration configuration = this.configurations.getConfiguration(pid);
            if (configuration == null) {
                throw new IllegalArgumentException(MSGS.errorComponentConfigurationMissing(pid));
            }
            GwtConfigComponent gwtConfig = configuration.getConfiguration();
            if (gwtConfig == null || !gwtConfig.isValid()) {
                throw new IllegalArgumentException(MSGS.errorComponentConfigurationMissing(pid));
            }
            if (WIRE_ASSET_PID.equals(gwtConfig.getFactoryId())) {
                final String driverPid = gwtConfig.getParameterValue(DRIVER_PID);
                if (this.configurations.getConfiguration(driverPid) == null) {
                    throw new IllegalArgumentException(MSGS.errorDriverConfigurationMissing(pid, driverPid));
                }
                if (this.configurations.getChannelDescriptor(driverPid) == null) {
                    throw new IllegalArgumentException(MSGS.errorDriverDescriptorMissingForAsset(pid, driverPid));
                }
            }
            return configuration;
        } catch (IllegalArgumentException e) {
            this.configurations.invalidateConfiguration(pid);
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
        if (this.configurations.getConfiguration(component.getPid()) == null) {
            this.configurations.createAndRegisterConfiguration(component.getPid(), component.getFactoryPid());
        }
        this.dialogs.setAssetPids(getAssetsNotInComposer());
    }

    @Override
    public void onWireComponentDeleted(WireComponent component) {
        if (!WIRE_ASSET_PID.equals(component.getFactoryPid())) {
            this.configurations.deleteConfiguration(component.getPid());
        }
        updateDirtyState();
        this.btnGraphDelete.setEnabled(this.wireComposer.getWireComponentCount() > 0);
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
                event.complete(WiresPanelUi.this.descriptors.createNewComponent(pid, factoryPid));
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
            this.confirmDialog.show(MSGS.wiresComponentNameAlreadyUsed(pid), AlertDialog.Severity.ALERT,
                    (ConfirmListener) null);
        }
        return isPidValid;
    }

    @Override
    public void onNewDriverCreated(String pid, String factoryPid, GwtConfigComponent descriptor) {
        this.configurations.createAndRegisterConfiguration(pid, factoryPid);
        this.configurations.setChannelDescriptor(pid, descriptor);
        this.dialogs.setDriverPids(this.configurations.getDriverPids());
    }

    @Override
    public void onDirtyStateChanged(HasConfiguration hasConfiguration) {
        updateDirtyState();
    }

}
