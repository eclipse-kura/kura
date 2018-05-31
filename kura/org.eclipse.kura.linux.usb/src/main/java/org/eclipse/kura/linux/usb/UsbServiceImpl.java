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
package org.eclipse.kura.linux.usb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.udev.LinuxUdevListener;
import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbDeviceType;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbServiceImpl implements UsbService, LinuxUdevListener {

    private static final Logger logger = LoggerFactory.getLogger(UsbServiceImpl.class);

    private LinuxUdevNative linuxUdevNative;
    private EventAdmin eventAdmin;

    protected void activate(ComponentContext componentContext) {
        // only support Linux
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        if (osName.equals("Linux")) {
            try {
                this.linuxUdevNative = new LinuxUdevNative(this);
            } catch (IOException e) {
                logger.error("Udev native can't be instantiated");
            }
        } else {
            logger.error("This is not Linux! - can not start the USB service.  This is {}", osVersion);
            throw new ComponentException("This is not Linux! - can not start the USB service.  This is " + osVersion);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        this.linuxUdevNative.unbind();
        this.linuxUdevNative = null;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    @Override
    public UsbServices getUsbServices() throws KuraException {
        try {
            return UsbHostManager.getUsbServices();
        } catch (SecurityException e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION, e, (Object[]) null);
        } catch (UsbException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e, (Object[]) null);
        }
    }

    @Override
    public synchronized List<? extends UsbDevice> getUsbDevices() {
        List<UsbDevice> usbDevices = new ArrayList<>();
        usbDevices.addAll(getUsbBlockDevices());
        usbDevices.addAll(getUsbNetDevices());
        usbDevices.addAll(getUsbTtyDevices());

        return usbDevices;
    }

    @Override
    public synchronized List<UsbBlockDevice> getUsbBlockDevices() {
        return LinuxUdevNative.getUsbBlockDevices();
    }

    @Override
    public synchronized List<UsbNetDevice> getUsbNetDevices() {
        return LinuxUdevNative.getUsbNetDevices();
    }

    @Override
    public synchronized List<UsbTtyDevice> getUsbTtyDevices() {
        return LinuxUdevNative.getUsbTtyDevices();
    }

    @Override
    public synchronized void attached(UsbDevice device) {
        logger.debug("firing UsbDeviceAddedEvent for: {}", device);
        Map<String, Object> map = new HashMap<>();
        map.put(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY, device.getUsbPort());
        map.put(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY, device.getVendorId());
        map.put(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY, device.getProductId());
        map.put(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, device.getManufacturerName());
        map.put(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, device.getProductName());
        map.put(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY, device.getUsbBusNumber());
        map.put(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY, device.getUsbDevicePath());

        if (device instanceof UsbBlockDevice) {
            map.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbBlockDevice) device).getDeviceNode());
            map.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_BLOCK_DEVICE);
        } else if (device instanceof UsbNetDevice) {
            map.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbNetDevice) device).getInterfaceName());
            map.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_NET_DEVICE);
        } else if (device instanceof UsbTtyDevice) {
            map.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbTtyDevice) device).getDeviceNode());
            map.put(UsbDeviceEvent.USB_EVENT_USB_INTERFACE_NUMBER, ((UsbTtyDevice) device).getInterfaceNumber());
            map.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_TTY_DEVICE);
        }

        this.eventAdmin.postEvent(new UsbDeviceAddedEvent(map));
    }

    @Override
    public synchronized void detached(UsbDevice device) {
        logger.debug("firing UsbDeviceRemovedEvent for: {}", device);
        Map<String, Object> map = new HashMap<>();
        map.put(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY, device.getUsbPort());
        map.put(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY, device.getVendorId());
        map.put(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY, device.getProductId());
        map.put(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY, device.getManufacturerName());
        map.put(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY, device.getProductName());
        map.put(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY, device.getUsbBusNumber());
        map.put(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY, device.getUsbDevicePath());

        if (device instanceof UsbBlockDevice) {
            map.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbBlockDevice) device).getDeviceNode());
            map.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_BLOCK_DEVICE);
        } else if (device instanceof UsbNetDevice) {
            map.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbNetDevice) device).getInterfaceName());
            map.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_NET_DEVICE);
        } else if (device instanceof UsbTtyDevice) {
            map.put(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY, ((UsbTtyDevice) device).getDeviceNode());
            map.put(UsbDeviceEvent.USB_EVENT_DEVICE_TYPE_PROPERTY, UsbDeviceType.USB_TTY_DEVICE);
        }

        this.eventAdmin.postEvent(new UsbDeviceRemovedEvent(map));
    }
}
