/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Red Hat Inc 
 *******************************************************************************/
package org.eclipse.kura.emulator.usb;

import java.util.Collections;
import java.util.List;

import javax.usb.UsbServices;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
@Component(
    name = "org.eclipse.kura.usb.UsbService",
    immediate = true,
    service = { org.eclipse.kura.usb.UsbService.class })
public class UsbServiceImpl implements UsbService {

    @Activate
    protected void activate(ComponentContext componentContext) {
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
    }

    @Override
    public UsbServices getUsbServices() throws KuraException {
        return null;
    }

    @Override
    public List<? extends UsbDevice> getUsbDevices() {
        return Collections.emptyList();
    }

    @Override
    public List<UsbBlockDevice> getUsbBlockDevices() {
        return Collections.emptyList();
    }

    @Override
    public List<UsbNetDevice> getUsbNetDevices() {
        return Collections.emptyList();
    }

    @Override
    public List<UsbTtyDevice> getUsbTtyDevices() {
        return Collections.emptyList();
    }

}
