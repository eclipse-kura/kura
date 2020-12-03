/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.wires;

import java.util.List;
import java.util.Optional;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.PidTextBox;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.gwtbootstrap3.client.shared.event.ModalHideEvent;
import org.gwtbootstrap3.client.shared.event.ModalHideHandler;
import org.gwtbootstrap3.client.shared.event.ModalShowEvent;
import org.gwtbootstrap3.client.shared.event.ModalShowHandler;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
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
    private final ModalVisibilityHandler modalVisibilityHandler = new ModalVisibilityHandler();
    private Optional<Callback> pickCallback = Optional.empty();

    public WiresDialogs() {
        initWidget(uiBinder.createAndBindUi(this));

        initSelectAssetModal();
        initSelectDriverModal();
        initGenericComponentModal();
        initNewAssetModal();
        initNewDriverModal();
    }

    public void setDriverPids(List<String> driverPids) {
        this.driverInstance.clear();
        for (String driverPid : driverPids) {
            this.driverInstance.addItem(driverPid);
        }

        this.driverInstance.setEnabled(!driverPids.isEmpty());
        this.buttonSelectDriverOk.setEnabled(!driverPids.isEmpty());
    }

    public void setDriverFactoryPids(List<String> driverFactoryPids) {
        this.newDriverFactory.clear();
        for (String driverPid : driverFactoryPids) {
            this.newDriverFactory.addItem(driverPid);
        }

        this.newDriverFactory.setEnabled(!driverFactoryPids.isEmpty());
        this.newDriverOk.setEnabled(!driverFactoryPids.isEmpty());
    }

    public void setAssetPids(List<String> assetPids) {
        this.assetInstance.clear();
        for (String assetPid : assetPids) {
            this.assetInstance.addItem(assetPid);
        }

        this.buttonSelectAssetOk.setEnabled(!assetPids.isEmpty());
    }

    private void initSelectAssetModal() {

        this.buttonNewAsset.addClickHandler(event -> {
            WiresDialogs.this.selectDriverModal.show();
            WiresDialogs.this.selectAssetModal.hide();
        });

        this.buttonSelectAssetOk.addClickHandler(event -> {
            if (WiresDialogs.this.pickCallback.isPresent()) {
                WiresDialogs.this.pickCallback.get()
                        .onNewComponentCreated(WiresDialogs.this.assetInstance.getSelectedValue());
                WiresDialogs.this.pickCallback = Optional.empty();
            }
            WiresDialogs.this.selectAssetModal.hide();
        });

        this.selectAssetModal.addHideHandler(this.modalVisibilityHandler);
        this.selectAssetModal.addShowHandler(modalVisibilityHandler);
    }

    private void initSelectDriverModal() {
        this.buttonNewDriver.addClickHandler(event -> {
            WiresDialogs.this.newDriverName.setValue("");
            WiresDialogs.this.newDriverName.setText("");
            WiresDialogs.this.newDriverModal.show();
            WiresDialogs.this.selectDriverModal.hide();
        });

        this.buttonSelectDriverOk.addClickHandler(event -> {
            String driverPid = WiresDialogs.this.driverInstance.getSelectedValue();
            WiresDialogs.this.newAssetDriverInstance.setText(driverPid);
            WiresDialogs.this.newAssetName.setText("");
            WiresDialogs.this.newAssetModal.show();
            WiresDialogs.this.selectDriverModal.hide();
        });

        this.selectDriverModal.addHideHandler(this.modalVisibilityHandler);
        this.selectDriverModal.addShowHandler(this.modalVisibilityHandler);
    }

    private void initNewAssetModal() {
        this.newAssetDriverInstance.setReadOnly(true);

        this.newAssetOk.addClickHandler(event -> {
            String wireAssetPid = WiresDialogs.this.newAssetName.getPid();
            if (wireAssetPid == null || !WiresDialogs.this.listener.onNewPidInserted(wireAssetPid)) {
                return;
            }
            String driverPid = WiresDialogs.this.newAssetDriverInstance.getText();
            if (WiresDialogs.this.pickCallback.isPresent()) {
                WiresDialogs.this.pickCallback.get().onNewAssetCreated(wireAssetPid, driverPid);
                WiresDialogs.this.pickCallback = Optional.empty();
            }
            WiresDialogs.this.newAssetModal.hide();
        });

        this.newAssetModal.addHideHandler(this.modalVisibilityHandler);
        this.newAssetModal.addShowHandler(this.modalVisibilityHandler);
    }

    private void initNewDriverModal() {

        this.newDriverOk.addClickHandler(event -> {
            final String pid = WiresDialogs.this.newDriverName.getPid();
            if (pid == null) {
                return;
            }
            if (WiresDialogs.this.listener == null || !WiresDialogs.this.listener.onNewPidInserted(pid)) {
                return;
            }
            final String factoryPid = WiresDialogs.this.newDriverFactory.getSelectedValue();
            WiresRPC.createNewDriver(factoryPid, pid, result -> {
                WiresDialogs.this.newAssetDriverInstance.setText(pid);
                WiresDialogs.this.newAssetName.setText("");
                WiresDialogs.this.newAssetModal.show();
                WiresDialogs.this.newDriverModal.hide();
                if (WiresDialogs.this.listener != null) {
                    WiresDialogs.this.listener.onNewDriverCreated(pid, factoryPid, result);
                }
            });
        });

        this.newDriverModal.addHideHandler(this.modalVisibilityHandler);
        this.newDriverModal.addShowHandler(this.modalVisibilityHandler);
    }

    private void initGenericComponentModal() {
        this.componentNameLabel.setText(MSGS.wiresComponentName());
        this.btnComponentModalYes.addClickHandler(event -> {
            String value = WiresDialogs.this.componentName.getPid();
            if (value != null) {
                if (WiresDialogs.this.listener == null || !WiresDialogs.this.listener.onNewPidInserted(value)) {
                    return;
                }
                if (WiresDialogs.this.pickCallback.isPresent()) {
                    WiresDialogs.this.pickCallback.get().onNewComponentCreated(value);
                    WiresDialogs.this.pickCallback = Optional.empty();
                }
                WiresDialogs.this.genericCompModal.hide();
                WiresDialogs.this.componentName.clear();
            }
        });

        this.genericCompModal.addHideHandler(this.modalVisibilityHandler);
        this.genericCompModal.addShowHandler(this.modalVisibilityHandler);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void pickComponent(String factoryPid, Callback callback) {
        this.pickCallback = Optional.ofNullable(callback);
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

    private class ModalVisibilityHandler implements ModalShowHandler, ModalHideHandler {

        private int modalCount;

        @Override
        public void onHide(ModalHideEvent evt) {
            modalCount--;
            if (modalCount == 0 && pickCallback.isPresent()) {
                pickCallback.get().onCancel();
            }
        }

        @Override
        public void onShow(ModalShowEvent evt) {
            modalCount++;
        }

    }
}
