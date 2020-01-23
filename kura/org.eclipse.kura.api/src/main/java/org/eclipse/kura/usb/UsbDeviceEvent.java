/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
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

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface UsbDeviceEvent {

    /** Name of the property to access the USB port of this device **/
    public static final String USB_EVENT_USB_PORT_PROPERTY = "usb.port";

    /**
     * Name of the property to access the resource name associated with this USB device (e.g. /dev/ttyUSB3, eth3, etc
     * depending on the device type)
     */
    public static final String USB_EVENT_RESOURCE_PROPERTY = "usb.resource";

    /** Name of the property to access the vendor id **/
    public static final String USB_EVENT_VENDOR_ID_PROPERTY = "usb.vendor.id";

    /** Name of the property to access the product id **/
    public static final String USB_EVENT_PRODUCT_ID_PROPERTY = "usb.product.id";

    /** Name of the property to access the manufacturer name **/
    public static final String USB_EVENT_MANUFACTURER_NAME_PROPERTY = "usb.manufacturer.name";

    /** Name of the property to access the product name **/
    public static final String USB_EVENT_PRODUCT_NAME_PROPERTY = "usb.product.name";

    /** Name of the property to access the USB bus number **/
    public static final String USB_EVENT_BUS_NUMBER_PROPERTY = "usb.bus.number";

    /** Name of the property to access the USB device path **/
    public static final String USB_EVENT_DEVICE_PATH_PROPERTY = "usb.device.path";

    /**
     * Name of the property to access the USB device type
     *
     * @since 1.4
     **/
    public static final String USB_EVENT_DEVICE_TYPE_PROPERTY = "usb.device.type";

    /**
     * Name of the property to access the interface number of a USB device
     *
     * @since 1.4
     **/
    public static final String USB_EVENT_USB_INTERFACE_NUMBER = "usb.interface.number";
}
