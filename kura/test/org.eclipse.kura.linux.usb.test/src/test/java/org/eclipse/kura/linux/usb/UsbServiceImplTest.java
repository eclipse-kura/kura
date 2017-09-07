/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.usb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.usb.UsbBlockDevice;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbDeviceEvent;
import org.eclipse.kura.usb.UsbNetDevice;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class UsbServiceImplTest {

    private String deviceNode = "node";
    private String interfaceName = "iface";
    private String manufacturerName = "manufacturer";
    private String productId = "productId";
    private String productName = "product";
    private String usbBusNumber = "busNo";
    private String usbDevicePath = "path";
    private String vendorId = "vendor";

    @Test
    public void testAttachedBlock() {
        UsbServiceImpl svc = new UsbServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/usb/NetworkEvent/device/ADDED", event.getTopic());
            assertEquals(vendorId, event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY));
            assertEquals(productId, event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY));
            assertEquals(manufacturerName, event.getProperty(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY));
            assertEquals(productName, event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY));
            assertEquals(usbBusNumber, event.getProperty(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY));
            assertEquals(usbDevicePath, event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY));
            assertEquals(usbBusNumber + "-" + usbDevicePath,
                    event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));
            assertEquals(deviceNode, event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY));

            return null;
        }).when(eaMock).postEvent(anyObject());

        UsbDevice device = new UsbBlockDevice(vendorId, productId, manufacturerName, productName, usbBusNumber,
                usbDevicePath, deviceNode);

        svc.attached(device);

        verify(eaMock, times(1)).postEvent(anyObject());
    }
    @Test
    public void testAttachedNet() {
        UsbServiceImpl svc = new UsbServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/usb/NetworkEvent/device/ADDED", event.getTopic());
            assertEquals(interfaceName, event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY));

            return null;
        }).when(eaMock).postEvent(anyObject());

        UsbDevice device = new UsbNetDevice(vendorId, productId, manufacturerName, productName, usbBusNumber,
                usbDevicePath, interfaceName);

        svc.attached(device);

        verify(eaMock, times(1)).postEvent(anyObject());
    }
    @Test
    public void testAttachedTty() {
        UsbServiceImpl svc = new UsbServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/usb/NetworkEvent/device/ADDED", event.getTopic());
            assertEquals(deviceNode, event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY));

            return null;
        }).when(eaMock).postEvent(anyObject());

        UsbDevice device = new UsbTtyDevice(vendorId, productId, manufacturerName, productName, usbBusNumber,
                usbDevicePath, deviceNode);

        svc.attached(device);

        verify(eaMock, times(1)).postEvent(anyObject());
    }
    @Test
    public void testDetachedBlock() {
        UsbServiceImpl svc = new UsbServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/usb/NetworkEvent/device/REMOVED", event.getTopic());
            assertEquals(deviceNode, event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY));

            return null;
        }).when(eaMock).postEvent(anyObject());

        UsbDevice device = new UsbBlockDevice(vendorId, productId, manufacturerName, productName, usbBusNumber,
                usbDevicePath, deviceNode);

        svc.detached(device);

        verify(eaMock, times(1)).postEvent(anyObject());
    }

    @Test
    public void testDetachedNet() {
        UsbServiceImpl svc = new UsbServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/usb/NetworkEvent/device/REMOVED", event.getTopic());
            assertEquals(vendorId, event.getProperty(UsbDeviceEvent.USB_EVENT_VENDOR_ID_PROPERTY));
            assertEquals(productId, event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_ID_PROPERTY));
            assertEquals(manufacturerName, event.getProperty(UsbDeviceEvent.USB_EVENT_MANUFACTURER_NAME_PROPERTY));
            assertEquals(productName, event.getProperty(UsbDeviceEvent.USB_EVENT_PRODUCT_NAME_PROPERTY));
            assertEquals(usbBusNumber, event.getProperty(UsbDeviceEvent.USB_EVENT_BUS_NUMBER_PROPERTY));
            assertEquals(usbDevicePath, event.getProperty(UsbDeviceEvent.USB_EVENT_DEVICE_PATH_PROPERTY));
            assertEquals(usbBusNumber + "-" + usbDevicePath,
                    event.getProperty(UsbDeviceEvent.USB_EVENT_USB_PORT_PROPERTY));
            assertEquals(interfaceName, event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY));

            return null;
        }).when(eaMock).postEvent(anyObject());

        UsbDevice device = new UsbNetDevice(vendorId, productId, manufacturerName, productName, usbBusNumber,
                usbDevicePath, interfaceName);

        svc.detached(device);

        verify(eaMock, times(1)).postEvent(anyObject());
    }

    @Test
    public void testDetachedTty() {
        UsbServiceImpl svc = new UsbServiceImpl();

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        doAnswer(invocation -> {
            Event event = invocation.getArgumentAt(0, Event.class);

            assertEquals("org/eclipse/kura/usb/NetworkEvent/device/REMOVED", event.getTopic());
            assertEquals(deviceNode, event.getProperty(UsbDeviceEvent.USB_EVENT_RESOURCE_PROPERTY));

            return null;
        }).when(eaMock).postEvent(anyObject());

        UsbDevice device = new UsbTtyDevice(vendorId, productId, manufacturerName, productName, usbBusNumber,
                usbDevicePath, deviceNode);

        svc.detached(device);

        verify(eaMock, times(1)).postEvent(anyObject());
    }

    @Test
    public void testGetUsbDevices() {
        List<UsbBlockDevice> blockDevices = new ArrayList<>();
        UsbBlockDevice bdev = mock(UsbBlockDevice.class);
        blockDevices.add(bdev);
        List<UsbNetDevice> netDevices = new ArrayList<>();
        UsbNetDevice ndev = mock(UsbNetDevice.class);
        netDevices.add(ndev);
        List<UsbTtyDevice> ttyDevices = new ArrayList<>();
        UsbTtyDevice tdev = mock(UsbTtyDevice.class);
        ttyDevices.add(tdev);

        UsbServiceImpl svc = new UsbServiceImpl() {

            @Override
            public synchronized List<UsbBlockDevice> getUsbBlockDevices() {
                return blockDevices;
            }

            @Override
            public synchronized List<UsbNetDevice> getUsbNetDevices() {
                return netDevices;
            }

            @Override
            public synchronized List<UsbTtyDevice> getUsbTtyDevices() {
                return ttyDevices;
            }
        };

        List<? extends UsbDevice> devices = svc.getUsbDevices();

        assertEquals(3, devices.size());
        assertTrue(devices.contains(bdev));
        assertTrue(devices.contains(ndev));
        assertTrue(devices.contains(tdev));
    }

}
