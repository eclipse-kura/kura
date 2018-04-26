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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.net.modem.ModemGpsDisabledEvent;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.junit.Test;
import org.osgi.service.event.Event;

public class ModemGpsStatusTrackerTest {

    private static Event getModemEnabledEvent() {
        final HashMap<String, Object> properties = new HashMap<>();

        properties.put(ModemGpsEnabledEvent.Port, "modemPort");
        properties.put(ModemGpsEnabledEvent.BaudRate, 115000);
        properties.put(ModemGpsEnabledEvent.DataBits, 8);
        properties.put(ModemGpsEnabledEvent.StopBits, 0);
        properties.put(ModemGpsEnabledEvent.Parity, 0);

        return new ModemGpsEnabledEvent(properties);
    }

    private static Event getModemDisabledEvent() {
        return new ModemGpsDisabledEvent(Collections.emptyMap());
    }

    private static void verifyHasGps(final ModemGpsStatusTracker tracker) {
        final CommURI uri = tracker.getGpsDeviceUri();

        assertNotNull(uri);
        assertEquals("modemPort", uri.getPort());
    }

    @Test
    public void testWithoutModem() throws KuraException {
        final ModemGpsStatusTracker tracker = new ModemGpsStatusTracker();

        assertNull(tracker.getGpsDeviceUri());
    }

    @Test
    public void testModemGpsEnabledEvent() throws KuraException {
        final ModemGpsStatusTracker tracker = new ModemGpsStatusTracker();

        final GpsDeviceAvailabilityListener listener = mock(GpsDeviceAvailabilityListener.class);
        tracker.setListener(listener);

        tracker.handleEvent(getModemEnabledEvent());

        verifyHasGps(tracker);
        verify(listener, times(1)).onGpsDeviceAvailabilityChanged();
    }

    @Test
    public void testModemGpsDisabledEvent() throws KuraException {
        final ModemGpsStatusTracker tracker = new ModemGpsStatusTracker();

        final GpsDeviceAvailabilityListener listener = mock(GpsDeviceAvailabilityListener.class);
        tracker.setListener(listener);

        tracker.handleEvent(getModemEnabledEvent());

        verifyHasGps(tracker);
        verify(listener, times(1)).onGpsDeviceAvailabilityChanged();

        tracker.handleEvent(getModemDisabledEvent());

        assertNull(tracker.getGpsDeviceUri());
        verify(listener, times(2)).onGpsDeviceAvailabilityChanged();
    }
}
