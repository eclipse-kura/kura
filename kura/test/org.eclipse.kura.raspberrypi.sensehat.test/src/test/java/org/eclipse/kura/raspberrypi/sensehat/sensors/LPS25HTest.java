/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.raspberrypi.sensehat.sensors;

import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.PRESS_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.PRESS_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.PRESS_OUT_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.TEMP_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.TEMP_OUT_L;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.DummyDevice;
import org.junit.Test;

public class LPS25HTest {

    @Test
    public void testGetPressureSensor() {
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        assertNotNull("LPS25H not created", lps25h);
    }

    @Test
    public void testRead() {
        // we test by writing to the simualted device register the same value as the register address
        // and then we expect the same value
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        lps25h.initDevice();
        int val = LPS25H.read(TEMP_OUT_H);
        assertEquals("Sensor reading not as expected", TEMP_OUT_H, val);
    }

    @Test
    public void testWrite() throws NoSuchFieldException {
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        lps25h.initDevice();
        LPS25H.write(0, new byte[] { 50 });
        KuraI2CDevice pressureI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lps25h, "pressureI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(pressureI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals("Sensor writing not as expected", 50, registerValue);
    }

    @Test
    public void testInitDevice() {
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        boolean result = lps25h.initDevice();
        assertTrue("Device not inited", result);
    }

    @Test
    public void testCloseDevice() throws NoSuchFieldException {
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        KuraI2CDevice pressureI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lps25h, "pressureI2CDevice");
        lps25h.initDevice();
        assertNotNull(lps25h);
        assertNotNull(pressureI2CDevice);
        LPS25H.closeDevice();
        LPS25H closedPressureSensor = (LPS25H) TestUtil.getFieldValue(lps25h, "pressureSensor");
        KuraI2CDevice closedPressureI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lps25h, "pressureI2CDevice");
        assertNull(closedPressureSensor);
        assertNull(closedPressureI2CDevice);
    }

    @Test
    public void testGetPressure() {
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        lps25h.initDevice();
        float pressure = lps25h.getPressure();
        // simulate reading device registers using the same values that are sent as commands
        assertTrue((PRESS_OUT_H << 16 | PRESS_OUT_L << 8 | PRESS_OUT_XL) / 4096F == pressure);
    }

    @Test
    public void testGetTemperature() {
        LPS25H lps25h = LPS25H.getPressureSensor(1, 1, 1, 1);
        lps25h.initDevice();
        float temperaure = lps25h.getTemperature();
        // simulate reading device registers using the same values that are sent as commands
        assertTrue(42.5F + (TEMP_OUT_H << 8 | TEMP_OUT_L << 0) / 480F == temperaure);
    }

}
