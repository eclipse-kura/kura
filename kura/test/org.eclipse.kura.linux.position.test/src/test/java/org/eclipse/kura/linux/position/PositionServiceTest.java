/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.io.ConnectionFactory;

public class PositionServiceTest {
    private static final double EPS = 0.000001;

    @Test
    public void testActivateStatic() {
        PositionServiceImpl ps = new PositionServiceImpl();

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        ps.setEventAdmin(eventAdminMock);

        BundleContext bundleContextMock = mock(BundleContext.class);
        when(bundleContextMock.registerService(eq(EventHandler.class.getName()), anyObject(), anyObject()))
                .thenReturn(null);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleContextMock);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", true);
        properties.put("longitude", 15.0);
        properties.put("latitude", 46.0);
        properties.put("altitude", 300.0);

        ps.activate(ctxMock, properties);

        verify(eventAdminMock, times(1)).postEvent((PositionLockedEvent) anyObject());

        ps.deactivate(ctxMock);
    }

    @Test
    public void testActivateDefault() throws IOException {
        PositionServiceImpl ps = new PositionServiceImpl();

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        ps.setEventAdmin(eventAdminMock);

        UsbService usbServiceMock = mock(UsbService.class);
        ps.setUsbService(usbServiceMock);

        List<UsbTtyDevice> usbDevices = new ArrayList<UsbTtyDevice>();
        UsbTtyDevice usbDev = mock(UsbTtyDevice.class);
        when(usbDev.getUsbPort()).thenReturn("port");
        when(usbDev.getDeviceNode()).thenReturn("node");
        usbDevices.add(usbDev);
        when(usbServiceMock.getUsbTtyDevices()).thenReturn(usbDevices);

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);
        ps.setConnectionFactory(connFactoryMock);

        BundleContext bundleContextMock = mock(BundleContext.class);
        when(bundleContextMock.registerService(eq(EventHandler.class.getName()), anyObject(), anyObject()))
                .thenReturn(null);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleContextMock);

        InputStream is = new ByteArrayInputStream("".getBytes());
        when(connMock.openInputStream()).thenReturn(is);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", false);
        properties.put("port", "port");
        properties.put("baudRate", 9600);
        properties.put("stopBits", 0);
        properties.put("parity", 0);
        properties.put("bitsPerWord", 1);

        ps.activate(ctxMock, properties);

        assertFalse(ps.isLocked());
        assertNotNull(ps.getPosition());
        assertEquals(0.0, ps.getPosition().getLatitude().getValue(), EPS);
        assertEquals(0.0, ps.getPosition().getLongitude().getValue(), EPS);
        assertEquals(0.0, ps.getPosition().getAltitude().getValue(), EPS);

        ps.deactivate(ctxMock);
    }

    @Test
    public void testActivateDeactivate() throws IOException, InterruptedException {
        PositionServiceImpl ps = new PositionServiceImpl();

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        ps.setEventAdmin(eventAdminMock);

        UsbService usbServiceMock = mock(UsbService.class);
        ps.setUsbService(usbServiceMock);

        List<UsbTtyDevice> usbDevices = new ArrayList<UsbTtyDevice>();
        UsbTtyDevice usbDev = mock(UsbTtyDevice.class);
        when(usbDev.getUsbPort()).thenReturn("port");
        when(usbDev.getDeviceNode()).thenReturn("node");
        usbDevices.add(usbDev);
        when(usbServiceMock.getUsbTtyDevices()).thenReturn(usbDevices);

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);
        ps.setConnectionFactory(connFactoryMock);

        BundleContext bundleContextMock = mock(BundleContext.class);
        when(bundleContextMock.registerService(eq(EventHandler.class.getName()), anyObject(), anyObject()))
                .thenReturn(null);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleContextMock);

        String nmeaStr = "$GPGGA,121041.000,4655.3772,N,01513.6390,E,1,06,1.7,478.3,M,44.7,M,,0000*5d\n";
        InputStream is = new ByteArrayInputStream(nmeaStr.getBytes());
        when(connMock.openInputStream()).thenReturn(is);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", false);
        properties.put("port", "port");
        properties.put("baudRate", 9600);
        properties.put("stopBits", 0);
        properties.put("parity", 0);
        properties.put("bitsPerWord", 1);

        final Object o = new Object();

        doAnswer(invocation -> {
            synchronized (o) {
                o.notifyAll();
            }

            return null;
        }).when(eventAdminMock).postEvent(anyObject());

        ps.activate(ctxMock, properties);

        // it takes some time for the event to be sent...
        synchronized (o) {
            o.wait(1000);
        }

        verify(eventAdminMock, times(1)).postEvent(anyObject());

        assertTrue(ps.isLocked());
        assertNotNull(ps.getNmeaPosition());
        assertEquals(46.922953, ps.getNmeaPosition().getLatitude(), 0.000001);

        ps.deactivate(ctxMock);

        assertFalse(ps.isLocked());
    }

    @Test
    public void testHandleEventUsb() throws IOException, NoSuchFieldException {
        PositionServiceImpl ps = new PositionServiceImpl();

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        ps.setEventAdmin(eventAdminMock);

        UsbService usbServiceMock = mock(UsbService.class);
        ps.setUsbService(usbServiceMock);

        List<UsbTtyDevice> usbDevices = new ArrayList<UsbTtyDevice>();
        UsbTtyDevice usbDev = mock(UsbTtyDevice.class);
        when(usbDev.getUsbPort()).thenReturn("port").thenReturn("port");
        when(usbDev.getDeviceNode()).thenReturn("node").thenReturn("node");
        usbDevices.add(usbDev);
        when(usbServiceMock.getUsbTtyDevices()).thenReturn(usbDevices).thenReturn(usbDevices).thenReturn(usbDevices)
                .thenReturn(new ArrayList<UsbTtyDevice>());

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);
        ps.setConnectionFactory(connFactoryMock);

        BundleContext bundleContextMock = mock(BundleContext.class);
        when(bundleContextMock.registerService(eq(EventHandler.class.getName()), anyObject(), anyObject()))
                .thenReturn(null);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleContextMock);

        InputStream is = new ByteArrayInputStream("".getBytes());
        when(connMock.openInputStream()).thenReturn(is);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", false);
        properties.put("port", "port");
        properties.put("baudRate", 9600);
        properties.put("stopBits", 0);
        properties.put("parity", 0);
        properties.put("bitsPerWord", 1);

        ps.activate(ctxMock, properties);

        TestUtil.setFieldValue(ps, "isRunning", false);

        String topic = UsbDeviceAddedEvent.USB_EVENT_DEVICE_ADDED_TOPIC;
        properties = new HashMap<String, Object>();
        Event event = new Event(topic, properties);

        ps.handleEvent(event);

        assertTrue((boolean) TestUtil.getFieldValue(ps, "isRunning"));

        topic = UsbDeviceRemovedEvent.USB_EVENT_DEVICE_REMOVED_TOPIC;
        properties = new HashMap<String, Object>();
        event = new Event(topic, properties);

        ps.handleEvent(event);
        assertFalse((boolean) TestUtil.getFieldValue(ps, "isRunning"));
        assertFalse(ps.isLocked());

        ps.deactivate(ctxMock);
    }

    @Test
    public void testHandleEventModem() throws IOException, NoSuchFieldException {
        PositionServiceImpl ps = new PositionServiceImpl();

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        ps.setEventAdmin(eventAdminMock);

        UsbService usbServiceMock = mock(UsbService.class);
        ps.setUsbService(usbServiceMock);

        List<UsbTtyDevice> usbDevices = new ArrayList<UsbTtyDevice>();
        UsbTtyDevice usbDev = mock(UsbTtyDevice.class);
        when(usbDev.getUsbPort()).thenReturn("port").thenReturn("port");
        when(usbDev.getDeviceNode()).thenReturn("node").thenReturn("node");
        usbDevices.add(usbDev);
        when(usbServiceMock.getUsbTtyDevices()).thenReturn(usbDevices).thenReturn(usbDevices).thenReturn(usbDevices)
                .thenReturn(new ArrayList<UsbTtyDevice>());

        ConnectionFactory connFactoryMock = mock(ConnectionFactory.class);
        CommConnection connMock = mock(CommConnection.class);
        when(connFactoryMock.createConnection(anyString(), eq(1), eq(false))).thenReturn(connMock);
        ps.setConnectionFactory(connFactoryMock);

        BundleContext bundleContextMock = mock(BundleContext.class);
        when(bundleContextMock.registerService(eq(EventHandler.class.getName()), anyObject(), anyObject()))
                .thenReturn(null);

        ComponentContext ctxMock = mock(ComponentContext.class);
        when(ctxMock.getBundleContext()).thenReturn(bundleContextMock);

        InputStream is = new ByteArrayInputStream("".getBytes());
        when(connMock.openInputStream()).thenReturn(is);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", false);
        properties.put("port", "port");
        properties.put("baudRate", 9600);
        properties.put("stopBits", 0);
        properties.put("parity", 0);
        properties.put("bitsPerWord", 1);

        ps.activate(ctxMock, properties);

        TestUtil.setFieldValue(ps, "isRunning", false);

        String topic = ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC;
        properties = new HashMap<String, Object>();
        properties.put("enabled", true);
        properties.put("static", false);
        properties.put("port", "portt");
        properties.put("baudRate", 19200);
        properties.put("stopBits", 1);
        properties.put("parity", 1);
        properties.put("bitsPerWord", 8);
        Event event = new Event(topic, properties);

        ps.handleEvent(event);

        Map<String, Object> props = (Map<String, Object>) TestUtil.getFieldValue(ps, "properties");
        assertEquals("portt", properties.get("port"));
        assertEquals(19200, properties.get("baudRate"));
        assertEquals(1, properties.get("stopBits"));
        assertEquals(1, properties.get("parity"));
        assertEquals(8, properties.get("bitsPerWord"));

    }
}
