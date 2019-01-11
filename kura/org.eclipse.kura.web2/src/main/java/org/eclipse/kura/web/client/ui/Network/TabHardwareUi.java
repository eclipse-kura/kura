/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.gwtbootstrap3.client.ui.FormControlStatic;
import org.gwtbootstrap3.client.ui.FormLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TabHardwareUi extends Composite implements NetworkTab {

    private static TabHardwareUiUiBinder uiBinder = GWT.create(TabHardwareUiUiBinder.class);

    interface TabHardwareUiUiBinder extends UiBinder<Widget, TabHardwareUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);
    GwtSession session;
    GwtNetInterfaceConfig selectedNetIfConfig;

    @UiField
    FormLabel labelState;
    @UiField
    FormLabel labelName;
    @UiField
    FormLabel labelType;
    @UiField
    FormLabel labelHardware;
    @UiField
    FormLabel labelSerial;
    @UiField
    FormLabel labelDriver;
    @UiField
    FormLabel labelVersion;
    @UiField
    FormLabel labelFirmware;
    @UiField
    FormLabel labelMtu;
    @UiField
    FormLabel labelUsb;
    @UiField
    FormLabel labelRssi;

    @UiField
    FormControlStatic state;
    @UiField
    FormControlStatic name;
    @UiField
    FormControlStatic type;
    @UiField
    FormControlStatic hardware;
    @UiField
    FormControlStatic serial;
    @UiField
    FormControlStatic driver;
    @UiField
    FormControlStatic version;
    @UiField
    FormControlStatic firmware;
    @UiField
    FormControlStatic mtu;
    @UiField
    FormControlStatic usb;
    @UiField
    FormControlStatic rssi;

    public TabHardwareUi(GwtSession currentSession) {
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        setDirty(false);

        // Set Labels
        this.labelState.setText(MSGS.netHwState());
        this.labelName.setText(MSGS.netHwName());
        this.labelType.setText(MSGS.netHwType());
        this.labelHardware.setText(MSGS.netHwAddress());
        this.labelSerial.setText(MSGS.netHwSerial());
        this.labelDriver.setText(MSGS.netHwDriver());
        this.labelVersion.setText(MSGS.netHwVersion());
        this.labelFirmware.setText(MSGS.netHwFirmware());
        this.labelMtu.setText(MSGS.netHwMTU());
        this.labelUsb.setText(MSGS.netHwUSBDevice());
        this.labelRssi.setText(MSGS.netHwSignalStrength());
    }

    // Dirty flag not needed here since this tab is not modifiable
    @Override
    public void setDirty(boolean flag) {
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        this.selectedNetIfConfig = config;
    }

    @Override
    public void refresh() {
        if (this.selectedNetIfConfig != null) {
            loadData();
        } else {
            reset();
        }
    }

    /********* Private Methods ********/

    private void loadData() {
        this.state.setText(this.selectedNetIfConfig.getHwState());
        this.name.setText(this.selectedNetIfConfig.getHwName());
        this.type.setText(this.selectedNetIfConfig.getHwType());
        this.hardware.setText(this.selectedNetIfConfig.getHwAddress());
        this.serial.setText(this.selectedNetIfConfig.getHwSerial());
        this.driver.setText(this.selectedNetIfConfig.getHwDriver());
        this.version.setText(this.selectedNetIfConfig.getHwDriverVersion());
        this.firmware.setText(this.selectedNetIfConfig.getHwFirmware());
        this.mtu.setText(String.valueOf(this.selectedNetIfConfig.getHwMTU()));
        this.usb.setText(this.selectedNetIfConfig.getHwUsbDevice());
        this.rssi.setText(this.selectedNetIfConfig.getHwRssi());
    }

    private void reset() {
        this.state.setText("");
        this.name.setText("");
        this.type.setText("");
        this.hardware.setText("");
        this.serial.setText("");
        this.driver.setText("");
        this.version.setText("");
        this.firmware.setText("");
        this.mtu.setText("");
        this.usb.setText("");
        this.rssi.setText("");
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        if (this.session != null) {
            updatedNetIf.setHwState(this.state.getText());
            updatedNetIf.setHwName(this.name.getText());
            updatedNetIf.setHwType(this.type.getText());
            updatedNetIf.setHwAddress(this.hardware.getText());
            updatedNetIf.setHwSerial(this.serial.getText());
            updatedNetIf.setHwDriver(this.driver.getText());
            updatedNetIf.setHwDriverVersion(this.version.getText());
            updatedNetIf.setHwFirmware(this.firmware.getText());
            if (this.mtu.getText() != null) {
                updatedNetIf.setHwMTU(Integer.parseInt(this.mtu.getText()));
            }
            updatedNetIf.setHwUsbDevice(this.usb.getText());
            updatedNetIf.setHwRssi(this.rssi.getText());
        }
    }
}
