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
