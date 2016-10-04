/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Network;

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
    GwtNetInterfaceConfig SelectednetIfConfig;

    @UiField
    FormLabel labelState, labelName, labelType, labelHardware, labelSerial, labelDriver, labelVersion, labelFirmware,
            labelMtu, labelUsb, labelRssi;
    @UiField
    FormControlStatic state, name, type, hardware, serial, driver, version, firmware, mtu, usb, rssi;

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
        this.SelectednetIfConfig = config;
    }

    @Override
    public void refresh() {
        if (this.SelectednetIfConfig != null) {
            loadData();
        } else {
            reset();
        }
    }

    /********* Private Methods ********/

    private void loadData() {
        this.state.setText(this.SelectednetIfConfig.getHwState());
        this.name.setText(this.SelectednetIfConfig.getHwName());
        this.type.setText(this.SelectednetIfConfig.getHwType());
        this.hardware.setText(this.SelectednetIfConfig.getHwAddress());
        this.serial.setText(this.SelectednetIfConfig.getHwSerial());
        this.driver.setText(this.SelectednetIfConfig.getHwDriver());
        this.version.setText(this.SelectednetIfConfig.getHwDriverVersion());
        this.firmware.setText(this.SelectednetIfConfig.getHwFirmware());
        this.mtu.setText(String.valueOf(this.SelectednetIfConfig.getHwMTU()));
        this.usb.setText(this.SelectednetIfConfig.getHwUsbDevice());
        this.rssi.setText(this.SelectednetIfConfig.getHwRssi());
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
