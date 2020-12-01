/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.usb;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface for all USB devices
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface UsbDevice {

    /**
     * The vendor ID of the device
     *
     * @return The vendor ID of the device
     */
    public String getVendorId();

    /**
     * The product ID of the device
     *
     * @return The product ID of the device
     */
    public String getProductId();

    /**
     * The manufacturer name of the device
     *
     * @return The manufacturer name of the device
     */
    public String getManufacturerName();

    /**
     * The product name of the device
     *
     * @return The product name of the device
     */
    public String getProductName();

    /**
     * The USB bus number of the device
     *
     * @return The USB bus number of the device
     */
    public String getUsbBusNumber();

    /**
     * The USB device path
     *
     * @return The USB device path
     */
    public String getUsbDevicePath();

    /**
     * The complete USB port (USB bus number plus USB device path)
     *
     * @return The complete USB port (USB bus number plus USB device path)
     */
    public String getUsbPort();

}
