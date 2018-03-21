/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class WiresDialogs extends Composite {

    interface WiresDialogsUiBinder extends UiBinder<Widget, WiresDialogs> {
    }

    private static final WiresDialogsUiBinder uiBinder = GWT.create(WiresDialogsUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);

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
    PidTextBox newAssetName;
    @UiField
    TextBox newAssetDriverInstance;
    @UiField
    Button newAssetOk;
    @UiField
    Button newAssetCancel;
    @UiField
    Modal newDriverModal;
    @UiField
    PidTextBox newDriverName;
    @UiField
    ListBox newDriverFactory;
    @UiField
    Button newDriverCancel;
    @UiField
    Button newDriverOk;
    @UiField
    Modal selectAssetModal;
    @UiField
    ModalHeader newAssetModalHeader;
    @UiField
    FormLabel componentNameLabel;
    @UiField
    Modal genericCompModal;
    @UiField
    Button btnComponentModalYes;
    @UiField
    Button btnComponentModalNo;
    @UiField
    PidTextBox componentName;

    private Listener listener;
    private Callback pickCallback;

    public WiresDialogs() {
        initWidget(uiBinder.createAndBindUi(this));

        initSelectAssetModal();
        initSelectDriverModal();
        initAssetModal();
        initNewAssetModal();
        initNewDriverModal();
    }

    public void setDriverPids(List<String> driverPids) {
        this.driverInstance.clear();
        for (String driverPid : driverPids) {
            this.driverInstance.addItem(driverPid);
        }

        driverInstance.setEnabled(!driverPids.isEmpty());
        buttonSelectDriverOk.setEnabled(!driverPids.isEmpty());
    }

    public void setDriverFactoryPids(List<String> driverFactoryPids) {
        this.newDriverFactory.clear();
        for (String driverPid : driverFactoryPids) {
            this.newDriverFactory.addItem(driverPid);
        }

        newDriverFactory.setEnabled(!driverFactoryPids.isEmpty());
        newDriverOk.setEnabled(!driverFactoryPids.isEmpty());
    }

    public void setAssetPids(List<String> assetPids) {
        this.assetInstance.clear();
        for (String assetPid : assetPids) {
            this.assetInstance.addItem(assetPid);
        }

        buttonSelectAssetOk.setEnabled(!assetPids.isEmpty());
    }

    private void initSelectAssetModal() {

        this.buttonNewAsset.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresDialogs.this.selectDriverModal.show();
            }
        });

        this.buttonSelectAssetOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (pickCallback != null) {
                    pickCallback.onNewComponentCreated(WiresDialogs.this.assetInstance.getSelectedValue());
                }
                selectAssetModal.hide();
            }
        });
        this.buttonSelectAssetCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (pickCallback != null) {
                    pickCallback.onCancel();
                }
            }
        });
    }

    private void initSelectDriverModal() {
        this.buttonNewDriver.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                WiresDialogs.this.newDriverName.setValue("");
                WiresDialogs.this.newDriverName.setText("");
                WiresDialogs.this.newDriverModal.show();
            }
        });

        this.buttonSelectDriverOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String driverPid = WiresDialogs.this.driverInstance.getSelectedValue();
                WiresDialogs.this.newAssetDriverInstance.setText(driverPid);
                WiresDialogs.this.newAssetName.setText("");
                WiresDialogs.this.newAssetModal.show();
            }
        });
        this.buttonSelectDriverCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (pickCallback != null) {
                    pickCallback.onCancel();
                }
            }
        });
    }

    private void initNewAssetModal() {
        this.newAssetDriverInstance.setReadOnly(true);

        this.newAssetOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String wireAssetPid = WiresDialogs.this.newAssetName.getPid();
                if (wireAssetPid == null || !listener.onNewPidInserted(wireAssetPid)) {
                    return;
                }
                String driverPid = WiresDialogs.this.newAssetDriverInstance.getText();
                WiresDialogs.this.newAssetModal.hide();
                if (pickCallback != null) {
                    pickCallback.onNewAssetCreated(wireAssetPid, driverPid);
                }
            }
        });

        this.newAssetCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (pickCallback != null) {
                    pickCallback.onCancel();
                }
            }
        });
    }

    private void initNewDriverModal() {

        this.newDriverOk.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                final String pid = WiresDialogs.this.newDriverName.getPid();
                if (pid == null) {
                    return;
                }
                if (listener == null || !listener.onNewPidInserted(pid)) {
                    return;
                }
                final String factoryPid = WiresDialogs.this.newDriverFactory.getSelectedValue();
                WiresRPC.createNewDriver(factoryPid, pid, new WiresRPC.Callback<GwtConfigComponent>() {

                    @Override
                    public void onSuccess(GwtConfigComponent result) {
                        newDriverModal.hide();
                        newAssetDriverInstance.setText(pid);
                        newAssetName.setText("");
                        newAssetModal.show();
                        if (listener != null) {
                            listener.onNewDriverCreated(pid, factoryPid, result);
                        }
                    }
                });
            }
        });

        this.newDriverCancel.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (pickCallback != null) {
                    pickCallback.onCancel();
                }
            }

        });
    }

    private void initAssetModal() {
        this.componentNameLabel.setText(MSGS.wiresComponentName());
        this.btnComponentModalYes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String value = WiresDialogs.this.componentName.getPid();
                if (value != null) {
                    if (listener == null || !listener.onNewPidInserted(value)) {
                        return;
                    }
                    if (pickCallback != null) {
                        pickCallback.onNewComponentCreated(value);
                    }
                    genericCompModal.hide();
                    componentName.clear();
                }
            }
        });
        this.btnComponentModalNo.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (pickCallback != null) {
                    pickCallback.onCancel();
                }
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void pickComponent(String factoryPid, Callback callback) {
        this.pickCallback = callback;
        if (factoryPid.contains(WiresPanelUi.WIRE_ASSET)) {
            if (this.assetInstance.getItemCount() > 0) {
                this.selectAssetModal.show();
            } else {
                this.selectDriverModal.show();
            }
        } else {
            this.newAssetModalHeader.setTitle(MSGS.wiresComponentNew());
            this.componentNameLabel.setText(MSGS.wiresComponentName());
            this.componentName.clear();
            this.newAssetName.clear();
            this.newDriverName.clear();
            this.genericCompModal.show();
        }
    }

    public interface Listener {

        public boolean onNewPidInserted(String pid);

        public void onNewDriverCreated(String pid, String factoryPid, GwtConfigComponent descriptor);
    }

    public interface Callback {

        public void onNewAssetCreated(String pid, String driverPid);

        public void onNewComponentCreated(String pid);

        public void onCancel();
    }

}
