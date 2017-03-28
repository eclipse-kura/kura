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

import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.ACC_DEVICE;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.CTRL_REG1_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.CTRL_REG2_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.MAG_DEVICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices.DummyDevice;
import org.junit.Test;

public class LSM9DS1Test {

    @Test
    public void testGetIMUSensor() {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        assertNotNull("LSM9DS1 not created", lsm9ds1);
    }

    @Test
    public void testInitDevice() {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        boolean result = lsm9ds1.initDevice(true, true, true);
        assertTrue("LSM9DS1 not inited", result);
    }

    @Test
    public void testCloseDevice() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        lsm9ds1.initDevice(true, true, true);
        KuraI2CDevice accI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        KuraI2CDevice magI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "magI2CDevice");
        assertNotNull(lsm9ds1);
        assertNotNull(accI2CDevice);
        assertNotNull(magI2CDevice);
        LSM9DS1.closeDevice();
        LSM9DS1 sensor = (LSM9DS1) TestUtil.getFieldValue(lsm9ds1, "imuSensor");
        KuraI2CDevice closedAccDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        KuraI2CDevice closedMagDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "magI2CDevice");
        assertNull(sensor);
        assertNull(closedAccDevice);
        assertNull(closedMagDevice);
    }

    @Test
    public void testRead() throws KuraException {
        // we test by writing to the simulated device register the same value as the register address
        // and then we expect the same value
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        lsm9ds1.initDevice(true, true, true);
        int val = LSM9DS1.read(ACC_DEVICE, CTRL_REG1_G);
        assertEquals("Sensor reading not as expected", CTRL_REG1_G, val);
        int magval = LSM9DS1.read(MAG_DEVICE, CTRL_REG2_M);
        assertEquals("Sensor reading not as expected", CTRL_REG2_M, magval);
    }

    @Test
    public void testWrite() throws NoSuchFieldException, KuraException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        lsm9ds1.initDevice(true, true, true);
        LSM9DS1.write(ACC_DEVICE, 0, new byte[] { 22 });
        LSM9DS1.write(MAG_DEVICE, 0, new byte[] { 33 });
        KuraI2CDevice accI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        DummyDevice accSensor = (DummyDevice) TestUtil.getFieldValue(accI2CDevice, "device");
        KuraI2CDevice magI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        DummyDevice magSensor = (DummyDevice) TestUtil.getFieldValue(accI2CDevice, "device");
        int accValue = (int) TestUtil.getFieldValue(accSensor, "register");
        assertEquals("Sensor writing not as expected", 22, accValue);
        int magValue = (int) TestUtil.getFieldValue(magSensor, "register");
        assertEquals("Sensor writing not as expected", 22, magValue);
    }

    @Test
    public void testGetCompassRaw() {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        float[] compasRaw = lsm9ds1.getCompassRaw();
        float[] expected = new float[] { 9.692838F, -17.843777F, -21.380009F };
        assertTrue(Arrays.equals(expected, compasRaw));
    }

    @Test
    public void testGetGyroscopeRaw() {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        float[] gyroRaw = lsm9ds1.getGyroscopeRaw();
        float[] expected = new float[] { -0.024642F, -0.020255F, 0.011905F };
        assertTrue(Arrays.equals(expected, gyroRaw));
    }

    @Test
    public void testGetAccelerometerRaw() {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        float[] accelerometerRaw = lsm9ds1.getAccelerometerRaw();
        float[] expected = new float[] { -0.16921997F, -0.17315522F, 0.17095335F };
        assertTrue(Arrays.equals(expected, accelerometerRaw));
    }

    @Test
    public void testEnableAccelerometer() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        LSM9DS1.enableAccelerometer();
        KuraI2CDevice accI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(accI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals(0x7B, registerValue);
    }

    @Test
    public void testDisableAccelerometer() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        LSM9DS1.disableAccelerometer();
        KuraI2CDevice accI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(accI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals(0x0, registerValue);
    }

    @Test
    public void testEnableGyroscope() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        LSM9DS1.enableGyroscope();
        int gyroSampleRate = (int) TestUtil.getFieldValue(lsm9ds1, "gyroSampleRate");
        assertEquals(119, gyroSampleRate);
        KuraI2CDevice accI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(accI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals(0x44, registerValue);
    }

    @Test
    public void testDisableGyroscope() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        LSM9DS1.disableGyroscope();
        KuraI2CDevice accI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "accI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(accI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals(16, registerValue);
    }

    @Test
    public void testEnableMagnetometer() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        LSM9DS1.enableMagnetometer();
        KuraI2CDevice magI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "magI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(magI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals(0x0, registerValue);
    }

    @Test
    public void testDisableMagnetometer() throws NoSuchFieldException {
        LSM9DS1 lsm9ds1 = LSM9DS1.getIMUSensor(1, 3, 4, 1, 1);
        LSM9DS1.disableMagnetometer();
        KuraI2CDevice magI2CDevice = (KuraI2CDevice) TestUtil.getFieldValue(lsm9ds1, "magI2CDevice");
        DummyDevice dummyyDevice = (DummyDevice) TestUtil.getFieldValue(magI2CDevice, "device");
        int registerValue = (int) TestUtil.getFieldValue(dummyyDevice, "register");
        assertEquals(0x3, registerValue);
    }

}
