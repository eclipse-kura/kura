/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.usb;

/**
 * UsbDeviceType represents the type of USB device.
 * Possible values are:
 * UsbBlockDevice, UsbModemDevice, UsbNetDevice and UsbTtyDevice
 * 
 * @since 1.4
 */
public enum UsbDeviceType {

    USB_BLOCK_DEVICE,
    USB_MODEM_DEVICE,
    USB_NET_DEVICE,
    USB_TTY_DEVICE
}
