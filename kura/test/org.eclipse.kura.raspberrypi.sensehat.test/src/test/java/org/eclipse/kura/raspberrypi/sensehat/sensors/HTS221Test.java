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

import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.CTRL_REG1;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.HUMIDITY_H_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.HUMIDITY_L_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.TEMP_H_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.TEMP_L_REG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.DummyDevice;
import org.junit.Test;

public class HTS221Test {

    @Test
    public void testGetHumiditySensor() {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        assertNotNull("HTS221 not created", hts221);
    }

    @Test
    public void testInitDevice() {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        boolean result = hts221.initDevice();
        assertTrue("HTS221 not inited", result);
    }

    @Test
    public void testCloseDevice() throws NoSuchFieldException {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        hts221.initDevice();
        assertNotNull(hts221);
        KuraI2CDevice humidityI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(hts221, "humidityI2CDevice");
        assertNotNull(humidityI2CDevice);
        HTS221.closeDevice();
        HTS221 closedHummiditySensor = (HTS221) TestUtil.getFieldValue(hts221, "humiditySensor");
        KuraI2CDevice closedHummidityI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(hts221, "humidityI2CDevice");
        assertNull(closedHummiditySensor);
        assertNull(closedHummidityI2CDevice);
    }

    @Test
    public void testGetHumidity() throws NoSuchFieldException {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        boolean result = hts221.initDevice();
        float humidity = hts221.getHumidity();
        float mh = (float) TestUtil.getFieldValue(hts221, "mh");
        float qh = (float) TestUtil.getFieldValue(hts221, "qh");
        assertTrue(mh * (HUMIDITY_H_REG << 8 | HUMIDITY_L_REG) + qh == humidity);

    }

    @Test
    public void testGetTemperature() throws NoSuchFieldException {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        boolean result = hts221.initDevice();
        float temperature = hts221.getTemperature();
        float mt = (float) TestUtil.getFieldValue(hts221, "mt");
        float qt = (float) TestUtil.getFieldValue(hts221, "qt");
        assertTrue(mt * (TEMP_H_REG << 8 | TEMP_L_REG) + qt == temperature);
    }

    @Test
    public void testIsHumidityReady() {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        hts221.initDevice();
        boolean result = hts221.isHumidityReady();
        assertTrue("Humidity not ready", result);

    }

    @Test
    public void testIsTemperatureReady() {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        hts221.initDevice();
        boolean result = hts221.isTemperatureReady();
        assertTrue("Temperature not ready", result);
    }

    @Test
    public void testRead() {
        // we test by writing to the simualted device register the same value as the register address
        // and then we expect the same value
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        hts221.initDevice();
        int val = HTS221.read(CTRL_REG1);
        assertEquals("Sensor reading not as expected", CTRL_REG1, val);
    }

    @Test
    public void testWrite() throws NoSuchFieldException {
        HTS221 hts221 = HTS221.getHumiditySensor(1, 2, 1, 1);
        hts221.initDevice();
        HTS221.write(0, new byte[] { 50 });
        KuraI2CDevice humidityI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(hts221, "humidityI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(humidityI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals("Sensor writing not as expected", 50, registerValue);
    }

}
