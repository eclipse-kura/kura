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
package org.eclipse.kura.usb;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * An event raised when a USB device has been removed from the system.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class UsbDeviceRemovedEvent extends Event implements UsbDeviceEvent {

    /** Topic of the UsbDeviceRemovedEvent */
    public static final String USB_EVENT_DEVICE_REMOVED_TOPIC = "org/eclipse/kura/usb/NetworkEvent/device/REMOVED";

    public UsbDeviceRemovedEvent(Map<String, ?> properties) {
        super(USB_EVENT_DEVICE_REMOVED_TOPIC, properties);
    }

    /**
     * Returns the name of the USB port.
     *
     * @return
     */
    public String getUsbPort() {
        return (String) getProperty(USB_EVENT_USB_PORT_PROPERTY);
    }

    /**
     * Returns the name of the USB resource associated with this device.
     *
     * @return
     */
    public String getUsbResource() {
        return (String) getProperty(USB_EVENT_RESOURCE_PROPERTY);
    }

    /**
     * Returns the name of the USB vendor ID associated with this device.
     *
     * @return
     */
    public String getUsbVendorId() {
        return (String) getProperty(USB_EVENT_VENDOR_ID_PROPERTY);
    }

    /**
     * Returns the name of the USB product ID associated with this device.
     *
     * @return
     */
    public String getUsbProductId() {
        return (String) getProperty(USB_EVENT_PRODUCT_ID_PROPERTY);
    }

    /**
     * Returns the name of the USB manufacturer name associated with this device.
     *
     * @return
     */
    public String getUsbManufacturerName() {
        return (String) getProperty(USB_EVENT_MANUFACTURER_NAME_PROPERTY);
    }

    /**
     * Returns the name of the USB product name associated with this device.
     *
     * @return
     */
    public String getUsbProductName() {
        return (String) getProperty(USB_EVENT_PRODUCT_NAME_PROPERTY);
    }

    /**
     * Returns the name of the USB bus number associated with this device.
     *
     * @return
     */
    public String getUsbBusNumber() {
        return (String) getProperty(USB_EVENT_BUS_NUMBER_PROPERTY);
    }

    /**
     * Returns the name of the USB device path associated with this device.
     *
     * @return
     */
    public String getUsbDevicePath() {
        return (String) getProperty(USB_EVENT_DEVICE_PATH_PROPERTY);
    }

    /**
     * Returns the USB device type.
     *
     * @return UsbDeviceType or null if the property is not set
     * @since 1.4
     */
    public UsbDeviceType getUsbDeviceType() {
        return (UsbDeviceType) getProperty(USB_EVENT_DEVICE_TYPE_PROPERTY);
    }
}
