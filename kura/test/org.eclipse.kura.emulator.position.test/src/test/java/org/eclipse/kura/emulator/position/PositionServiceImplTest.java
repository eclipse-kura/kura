/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.emulator.position;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.position.NmeaPosition;
import org.eclipse.kura.position.PositionLockedEvent;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.position.Position;

public class PositionServiceImplTest {

    private static final String USE_GPSD = "useGpsd";

    @Test
    public void testActivateReadDeactivate() throws MalformedURLException, NoSuchFieldException {
        // test service activation, wait for position, deactivate

        PositionServiceImpl svc = new PositionServiceImpl();

        Bundle bMock = mock(Bundle.class);
        String name = "boston.gpx";
        URL url = new URL("file:../../emulator/org.eclipse.kura.emulator.position/src/main/resources/" + name);
        when(bMock.getResource(name)).thenReturn(url);

        BundleContext bcMock = mock(BundleContext.class);
        when(bcMock.getBundle()).thenReturn(bMock);

        ComponentContext ccMock = mock(ComponentContext.class);
        when(ccMock.getBundleContext()).thenReturn(bcMock);

        EventAdmin eaMock = mock(EventAdmin.class);
        svc.setEventAdmin(eaMock);

        Map<String, Object> properties = new HashMap<>();
        properties.put(USE_GPSD, true);

        svc.activate(ccMock, properties);

        while (((int) TestUtil.getFieldValue(svc, "index")) < 1) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // don't worry
            }
        }

        svc.deactivate(ccMock);

        assumeTrue(((int) TestUtil.getFieldValue(svc, "index")) == 1);

        verify(eaMock, times(1)).postEvent(isA(PositionLockedEvent.class));

        assertNull(TestUtil.getFieldValue(svc, "handle"));

        Position currentPosition = svc.getPosition();
        NmeaPosition currentNmeaPosition = svc.getNmeaPosition();

        double eps = 0.0000001;
        assertEquals(0.7370467, currentPosition.getLatitude().getValue(), eps);
        assertEquals(-1.2482312, currentPosition.getLongitude().getValue(), eps);
        assertEquals(149.6, currentPosition.getAltitude().getValue(), eps);

        eps *= 10.0;
        assertEquals(42.229664, currentNmeaPosition.getLatitude(), eps);
        assertEquals(-71.518378, currentNmeaPosition.getLongitude(), eps);
        assertEquals(149.6, currentNmeaPosition.getAltitude(), eps);
    }

    @Test
    public void testUpdate() throws MalformedURLException, NoSuchFieldException {
        // test service update

        PositionServiceImpl svc = new PositionServiceImpl();

        Map<String, Object> properties = new HashMap<>();
        properties.put(USE_GPSD, true);

        svc.updated(properties);

        assertTrue((boolean) TestUtil.getFieldValue(svc, "useGpsd"));

        properties.put(USE_GPSD, false);

        svc.updated(properties);

        assertFalse((boolean) TestUtil.getFieldValue(svc, "useGpsd"));
    }
}
