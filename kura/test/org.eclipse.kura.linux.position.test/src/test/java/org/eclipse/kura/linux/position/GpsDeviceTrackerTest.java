/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.linux.position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.usb.UsbDeviceAddedEvent;
import org.eclipse.kura.usb.UsbDeviceRemovedEvent;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.junit.Test;
import org.osgi.service.event.Event;

public class GpsDeviceTrackerTest {

    private static final Event usbDeviceAddedEvent = new UsbDeviceAddedEvent(Collections.emptyMap());
    private static final Event usbDeviceRemovedEvent = new UsbDeviceRemovedEvent(Collections.emptyMap());

    private static final class GpsDeviceTrackerTestFixture {

        final List<UsbTtyDevice> usbTtyDevices = new ArrayList<>();
        final UsbService usbService;
        final GpsDeviceTracker tracker;
        boolean serialPortExists = true;

        GpsDeviceTrackerTestFixture() {
            this.usbService = mock(UsbService.class);
            this.tracker = new GpsDeviceTracker() {

                protected boolean serialPortExists(CommURI uri) {
                    return serialPortExists;
                }
            };

            when(usbService.getUsbTtyDevices()).thenReturn(usbTtyDevices);
            tracker.setUsbService(this.usbService);
        }

        void addDeviceMapping(final String usbPort, final String devNode) {
            final UsbTtyDevice result = mock(UsbTtyDevice.class);

            when(result.getDeviceNode()).thenReturn(devNode);
            when(result.getUsbPort()).thenReturn(usbPort);

            usbTtyDevices.add(result);
        }
    }

    private static CommURI getUri(final String port) {
        return new CommURI.Builder(port).withBaudRate(9600).withDataBits(7).withFlowControl(1).withParity(2)
                .withStopBits(1).build();
    }

    @Test
    public void testReturnNullAfterCreation() {
        final GpsDeviceTrackerTestFixture fixture = new GpsDeviceTrackerTestFixture();

        assertNull(fixture.tracker.getTrackedUri());
        assertNull(fixture.tracker.getGpsDeviceUri());
    }

    @Test
    public void testTrackUriWithDeviceNode() {
        final GpsDeviceTrackerTestFixture fixture = new GpsDeviceTrackerTestFixture();

        final CommURI uri = getUri("/dev/ttyFOO");
        final CommURI trackedUri = fixture.tracker.track(uri);

        assertEquals(uri.toString(), trackedUri.toString());
    }

    @Test
    public void testResolveUriWithUsbPath() {
        final GpsDeviceTrackerTestFixture fixture = new GpsDeviceTrackerTestFixture();

        fixture.addDeviceMapping("1-2.1", "/dev/ttyFOO");

        final CommURI uri = getUri("1-2.1");
        final CommURI trackedUri = fixture.tracker.track(uri);

        assertEquals(getUri("/dev/ttyFOO").toString(), trackedUri.toString());
    }

    @Test
    public void testTrackUriWithoutSerialPort() {
        final GpsDeviceTrackerTestFixture fixture = new GpsDeviceTrackerTestFixture();

        fixture.serialPortExists = false;

        final CommURI uri = getUri("/dev/ttyFOO");
        final CommURI resolvedUri = fixture.tracker.track(uri);

        assertNull(resolvedUri);

        final CommURI trackedURI = fixture.tracker.getTrackedUri();
        assertEquals(uri.toString(), trackedURI.toString());
    }

    @Test
    public void testUsbEventsWithDeviceNode() {
        final GpsDeviceTrackerTestFixture fixture = new GpsDeviceTrackerTestFixture();

        fixture.serialPortExists = false;

        final CommURI uri = getUri("COM0");
        fixture.tracker.track(uri);

        final GpsDeviceAvailabilityListener listener = mock(GpsDeviceAvailabilityListener.class);
        fixture.tracker.setListener(listener);

        assertNull(fixture.tracker.getGpsDeviceUri());

        fixture.tracker.handleEvent(usbDeviceAddedEvent);

        assertNull(fixture.tracker.getGpsDeviceUri());
        verify(listener, times(0)).onGpsDeviceAvailabilityChanged();

        fixture.serialPortExists = true;
        fixture.tracker.handleEvent(usbDeviceAddedEvent);

        verify(listener, times(1)).onGpsDeviceAvailabilityChanged();
        assertEquals(uri.toString(), fixture.tracker.getGpsDeviceUri().toString());

        fixture.tracker.handleEvent(usbDeviceAddedEvent);

        verify(listener, times(1)).onGpsDeviceAvailabilityChanged();
        assertEquals(uri.toString(), fixture.tracker.getGpsDeviceUri().toString());

        fixture.serialPortExists = false;

        fixture.tracker.handleEvent(usbDeviceRemovedEvent);

        verify(listener, times(2)).onGpsDeviceAvailabilityChanged();
        assertNull(fixture.tracker.getGpsDeviceUri());

        fixture.tracker.handleEvent(usbDeviceRemovedEvent);

        verify(listener, times(2)).onGpsDeviceAvailabilityChanged();
        assertNull(fixture.tracker.getGpsDeviceUri());
    }

    @Test
    public void testUsbEventsWithUsbPort() {
        final GpsDeviceTrackerTestFixture fixture = new GpsDeviceTrackerTestFixture();

        fixture.serialPortExists = false;

        final CommURI uri = getUri("1-2.1");
        final CommURI resolvedUri = getUri("/dev/ttyFOO");

        fixture.tracker.track(uri);

        final GpsDeviceAvailabilityListener listener = mock(GpsDeviceAvailabilityListener.class);
        fixture.tracker.setListener(listener);

        assertNull(fixture.tracker.getGpsDeviceUri());

        fixture.tracker.handleEvent(usbDeviceAddedEvent);

        assertNull(fixture.tracker.getGpsDeviceUri());
        verify(listener, times(0)).onGpsDeviceAvailabilityChanged();

        fixture.serialPortExists = true;
        fixture.addDeviceMapping("1-2.1", "/dev/ttyFOO");

        fixture.tracker.handleEvent(usbDeviceAddedEvent);

        verify(listener, times(1)).onGpsDeviceAvailabilityChanged();
        assertEquals(resolvedUri.toString(), fixture.tracker.getGpsDeviceUri().toString());

        fixture.tracker.handleEvent(usbDeviceAddedEvent);

        verify(listener, times(1)).onGpsDeviceAvailabilityChanged();
        assertEquals(resolvedUri.toString(), fixture.tracker.getGpsDeviceUri().toString());

        fixture.serialPortExists = false;

        fixture.tracker.handleEvent(usbDeviceRemovedEvent);

        verify(listener, times(2)).onGpsDeviceAvailabilityChanged();
        assertNull(fixture.tracker.getGpsDeviceUri());

        fixture.usbTtyDevices.clear();
        fixture.tracker.handleEvent(usbDeviceRemovedEvent);

        verify(listener, times(2)).onGpsDeviceAvailabilityChanged();
        assertNull(fixture.tracker.getGpsDeviceUri());
    }

}
