/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.gpio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;
import org.junit.Test;

import jdk.dio.ClosedDeviceException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.PinListener;


public class JdkDioPinTest {

    @Test
    public void testParseFromProperty() throws Throwable {
        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.NONE, pin.getTrigger());
        assertEquals(1, pin.getIndex());
        assertEquals("name", pin.getName());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(8, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(0, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:4,mode:0,trigger:1";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.FALLING_EDGE, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(8, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:0,mode:1,trigger:2";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.INPUT_PULL_UP, pin.getMode());
        assertEquals(KuraGPIODirection.INPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.RAISING_EDGE, pin.getTrigger());

        assertEquals(0, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(2, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:ab,mode:2,trigger:3";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.INPUT_PULL_DOWN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.BOTH_EDGES, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(2, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(3, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:ab,mode:4,trigger:4";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_PUSH_PULL, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.HIGH_LEVEL, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(4, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(4, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:ab,mode:16,trigger:5";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.LOW_LEVEL, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(8, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(5, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:ab,mode:ab,trigger:6";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.BOTH_LEVELS, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(8, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(6, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:ab,mode:ab,trigger:7";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.NONE, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(8, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(0, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));

        // new combination
        description = "name:name,deviceType:gpio.GPIOPin,direction:ab,mode:ab,trigger:ab";

        pin = JdkDioPin.parseFromProperty(index, description);

        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.NONE, pin.getTrigger());

        assertEquals(1, (int) TestUtil.invokePrivate(pin, "getDirectionInternal"));
        assertEquals(8, (int) TestUtil.invokePrivate(pin, "getModeInternal"));
        assertEquals(0, (int) TestUtil.invokePrivate(pin, "getTriggerInternal"));
    }

    @Test
    public void testStatusListeners() throws KuraClosedDeviceException, IOException, NoSuchFieldException {
        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        PinStatusListener listener = mock(PinStatusListener.class);
        pin.addPinStatusListener(listener);

        verify(pinMock, times(1)).setInputListener((PinListener) notNull());

        pin.removePinStatusListener(listener);

        verify(pinMock, times(1)).setInputListener(null);
    }

    @Test
    public void testStatusListenersExceptions() throws KuraClosedDeviceException, IOException, NoSuchFieldException {
        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        doThrow(new ClosedDeviceException()).when(pinMock).setInputListener((PinListener) notNull());
        doThrow(new ClosedDeviceException()).when(pinMock).setInputListener(null);

        PinStatusListener listener = mock(PinStatusListener.class);
        try {
            pin.addPinStatusListener(listener);
            fail("Exception expected.");
        } catch (KuraClosedDeviceException e) {
            // OK
        }

        try {
            pin.removePinStatusListener(listener);
            fail("Exception expected.");
        } catch (KuraClosedDeviceException e) {
            // OK
        }
    }

    @Test
    public void testClose() throws NoSuchFieldException, IOException, KuraClosedDeviceException {
        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        when(pinMock.isOpen()).thenReturn(true);

        PinStatusListener listener = mock(PinStatusListener.class);
        pin.addPinStatusListener(listener);

        pin.close();

        verify(pinMock, times(1)).setInputListener(null);
        verify(pinMock, times(1)).close();
    }

    @Test
    public void testGetValue()
            throws KuraClosedDeviceException, IOException, NoSuchFieldException, KuraUnavailableDeviceException {

        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        when(pinMock.getValue()).thenReturn(true);

        assertTrue(pin.getValue());
    }

    @Test
    public void testGetValueExceptions()
            throws KuraClosedDeviceException, IOException, NoSuchFieldException, KuraUnavailableDeviceException {

        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        doThrow(new UnavailableDeviceException()).doThrow(new ClosedDeviceException()).when(pinMock).getValue();

        try {
            pin.getValue();
            fail("Exception expected.");
        } catch (KuraUnavailableDeviceException e) {
            // OK
        }
        try {
            pin.getValue();
            fail("Exception expected.");
        } catch (KuraClosedDeviceException e) {
            // OK
        }
    }

    @Test
    public void testSetValue()
            throws KuraClosedDeviceException, IOException, NoSuchFieldException, KuraUnavailableDeviceException {

        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        pin.setValue(true);

        verify(pinMock, times(1)).setValue(true);
    }

    @Test
    public void testSetValueExceptions()
            throws KuraClosedDeviceException, IOException, NoSuchFieldException, KuraUnavailableDeviceException {

        String index = "1";
        String description = "name:name,deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        JdkDioPin pin = JdkDioPin.parseFromProperty(index, description);

        GPIOPin pinMock = mock(GPIOPin.class);
        TestUtil.setFieldValue(pin, "thePin", pinMock);

        doThrow(new UnavailableDeviceException()).doThrow(new ClosedDeviceException()).when(pinMock).setValue(true);

        try {
            pin.setValue(true);
            fail("Exception expected.");
        } catch (KuraUnavailableDeviceException e) {
            // OK
        }
        try {
            pin.setValue(true);
            fail("Exception expected.");
        } catch (KuraClosedDeviceException e) {
            // OK
        }
    }
}
