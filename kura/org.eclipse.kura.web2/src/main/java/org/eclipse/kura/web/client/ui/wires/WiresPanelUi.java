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
import java.util.List;
import java.util.Map;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.AlertDialog;
import org.eclipse.kura.web.client.ui.wires.composer.BlinkEffect;
import org.eclipse.kura.web.client.ui.wires.composer.DropEvent;
import org.eclipse.kura.web.client.ui.wires.composer.Wire;
import org.eclipse.kura.web.client.ui.wires.composer.WireComponent;
import org.eclipse.kura.web.client.ui.wires.composer.WireComposer;
import org.eclipse.kura.web.shared.AssetConstants;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtWireComponentConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireComponentDescriptor;
import org.eclipse.kura.web.shared.model.GwtWireComposerStaticInfo;
import org.eclipse.kura.web.shared.model.GwtWireConfiguration;
import org.eclipse.kura.web.shared.model.GwtWireGraphConfiguration;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.Row;
import org.gwtbootstrap3.client.ui.html.Strong;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresPanelUi extends Composite implements WireComposer.Listener, ConfigurationChangeListener {

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
    Strong errorAlertText;
    @UiField
    Modal errorModal;

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

    private WireComposer wireComposer;
    private BlinkEffect blinkEffect;

    static final String WIRE_ASSET = "WireAsset";

    interface WiresPanelUiUiBinder extends UiBinder<Widget, WiresPanelUi> {
    }

    private static final WiresPanelUiUiBinder uiBinder = GWT.create(WiresPanelUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    static final String FACTORY_PID_DROP_PREFIX = "factory://";

    private static final String TEMPORARY_ASSET_REG_EXP = "^[0-9]{14}$";
    private static final String ASSET_DESCRIPTION_PROP = "asset.desc";
    private static final String WIRE_ASSET_PID = "org.eclipse.kura.wire.WireAsset";
    private static final String DRIVER_PID = "driver.pid";

    private boolean isDirty;
    private List<String> components;
    private final Map<String, GwtConfigComponent> configs = new HashMap<>();
    private final List<String> drivers;
    private final List<String> emitters;

    private String graph;
    private final List<String> receivers;
    private String wireComponentsConfigJson;
    private String wireConfigsJson;
    private String wires;

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
                confirmDialog.show(MSGS.wiresSave(), new AlertDialog.Listener() {

                    @Override
                    public void onConfirm() {
                        ;
                        WiresRPC.updateWiresConfiguration(toGwtWireGraphConfiguration(), new WiresRPC.Callback<Void>() {

                            @Override
                            public void onSuccess(Void result) {
                                setDirty(false);
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
        return this.isDirty;
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
                        blinkEffect.setEnabled(true);
                        setDirty(false);
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

        for (GwtWireComponentDescriptor descriptor : this.descriptors.getDescriptors().values()) {
            final WireComponentsAnchorListItem item = new WireComponentsAnchorListItem(
                    getFormattedPid(descriptor.getFactoryPid()), descriptor.getFactoryPid(),
                    descriptor.getMinInputPorts() > 0, descriptor.getMinOutputPorts() > 0);
            item.setListener(listener);
            this.wireComponentsMenu.add(item);
        }
    }

    private native void log(Object o) /*-{
                                      console.log(o)
                                      }-*/;

    private void loadStaticInformation(GwtWireComposerStaticInfo staticInfo) {
        this.configurations.setChannelDescriptiors(staticInfo.getDriverDescriptors());
        this.configurations.setBaseChannelDescriptor(staticInfo.getBaseChannelDescriptor());
        this.configurations.setComponentDefinitions(staticInfo.getComponentDefinitions());
        this.descriptors.setDescriptors(staticInfo.getWireComponentDescriptors());
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

        configurationList.addAll(configuration.getDriverConfigurations());
        configurations.setComponentConfigurations(configurationList);

        wireComposer.fitContent(false);
        resetUiState();
    }

    private GwtWireGraphConfiguration toGwtWireGraphConfiguration() {
        final GwtWireGraphConfiguration result = new GwtWireGraphConfiguration();

        final List<GwtWireComponentConfiguration> wireComponentConfigurations = new ArrayList<>();

        for (WireComponent component : wireComposer.getWireComponents()) {
            logConfiguration(configurations.getConfiguration(component.getPid()).getConfiguration());
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

    public void showConfigurationArea(HasConfiguration configuration) {

        Window.scrollTo(0, 0);
        this.configurationRow.setVisible(false);
        this.configurationRow.clear();

        final ConfigurationAreaUi configurationAreaUi = new ConfigurationAreaUi(configuration, this.configurations);
        configurationAreaUi.setListener(this);

        configurationAreaUi.render();
        this.configurationRow.add(configurationAreaUi);
        this.configurationRow.setVisible(true);
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

    public void jsniShowDuplicatePidModal(final String pid) {
        this.errorAlertText.setText(MSGS.wiresComponentNameAlreadyUsed(pid));
        this.errorModal.show();
    }

    private WireComponent createAsset(String pid, String driverPid) {
        final HasConfiguration assetConfig = configurations.createConfiguration(pid, WIRE_ASSET_PID);
        assetConfig.getConfiguration().getParameter(AssetConstants.ASSET_DRIVER_PROP.value()).setValue(driverPid);
        return descriptors.createNewComponent(pid, WIRE_ASSET_PID);
    }

    public void showComponentCreationDialog(final String factoryPid) {
        if (descriptors.getDescriptor(factoryPid) == null) {
            return;
        }
        this.dialogs.pickComponent(factoryPid, new WiresDialogs.Listener() {

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

    private void resetUiState() {
        setDirty(false);
        hideConfigurationArea();
    }

    public void unload() {
        blinkEffect.setEnabled(false);
    }

    @Override
    public void onWireCreated(Wire wire) {
        setDirty(true);
    }

    @Override
    public void onWireDeleted(Wire wire) {
        setDirty(true);
    }

    @Override
    public void onWireChanged(Wire wire) {
        setDirty(true);
    }

    private void logConfiguration(GwtConfigComponent config) {
        for (GwtConfigParameter param : config.getParameters()) {
            log(param.getId());
            log(param.getValue());
        }
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
        this.setDirty(true);
    }

    @Override
    public void onWireComponentCreated(WireComponent component) {
        this.btnGraphDelete.setEnabled(true);
        if (configurations.getConfiguration(component.getPid()) == null) {
            configurations.createConfiguration(component.getPid(), component.getFactoryPid());
        }
    }

    @Override
    public void onWireComponentDeleted(WireComponent component) {
        configurations.deleteConfiguration(component.getPid());
        this.btnGraphDelete.setEnabled(wireComposer.getWireComponentCount() > 0);
    }

    @Override
    public void onDrop(final DropEvent event) {
        final String attachment = event.getAttachment();
        if (attachment == null || !attachment.startsWith(FACTORY_PID_DROP_PREFIX)) {
            event.cancel();
            return;
        }
        final String factoryPid = attachment.substring(FACTORY_PID_DROP_PREFIX.length());
        this.dialogs.pickComponent(factoryPid, new WiresDialogs.Listener() {

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
        log("configuration updated");
        this.configurations.updateConfiguration(newConfiguration);
        setDirty(true);
    }

}
