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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.junit.Test;


public class GPIOServiceImplTest {

    @Test
    public void testActivateDeactivatePinSearch() throws IOException, NoSuchFieldException {
        GPIOServiceImpl svc = new GPIOServiceImpl();

        File f = new File("target/jdk.dio.properties");
        String dioConfigFileName = f.getAbsolutePath();
        System.setProperty("jdk.dio.registry", "file:///" + dioConfigFileName);

        f.createNewFile();

        String led1 = "user led 1";
        String testpin1 = "testpin1";
        FileWriter fw = new FileWriter(f);
        fw.write("1=name:" + led1 + ",deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0\n");
        fw.write("2=name:" + testpin1 + ",deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0");
        fw.close();

        svc.activate(null);

        f.delete();

        Set<JdkDioPin> pins = (Set) TestUtil.getFieldValue(svc, "pins");
        assertEquals(2, pins.size());

        KuraGPIOPin pin = pins.iterator().next();
        assertEquals(KuraGPIOMode.OUTPUT_OPEN_DRAIN, pin.getMode());
        assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
        assertEquals(KuraGPIOTrigger.NONE, pin.getTrigger());
        assertTrue(1 == pin.getIndex() || 2 == pin.getIndex());
        assertTrue(led1.equals(pin.getName()) || testpin1.equals(pin.getName()));

        // get all pins
        Map<Integer, String> availablePins = svc.getAvailablePins();
        assertEquals(pins.size(), availablePins.size());
        assertEquals(led1, availablePins.get(1));
        assertEquals(testpin1, availablePins.get(2));

        // get by name
        pin = svc.getPinByName("user led 2");
        assertNull(pin);

        pin = svc.getPinByName(led1);
        assertNotNull(pin);

        // get by name with pin creation
        KuraGPIOPin pin2 = svc.getPinByName(led1, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN,
                KuraGPIOTrigger.HIGH_LEVEL);

        assertNotEquals(pin, pin2);
        assertEquals(2, pins.size());

        // by terminal (index)
        KuraGPIOPin pin1 = svc.getPinByTerminal(1);

        assertEquals(1, pin1.getIndex());
        assertEquals(2, pins.size());
        assertEquals(pin2, pin1);

        // by terminal, new pin
        pin = svc.getPinByTerminal(3);

        assertEquals(3, pin.getIndex());
        assertEquals(3, pins.size());

        // the other 'by terminal'...
        pin = svc.getPinByTerminal(1, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN,
                KuraGPIOTrigger.HIGH_LEVEL);

        assertEquals(1, pin.getIndex());
        assertEquals(pin1, pin);
        assertEquals(3, pins.size());

        // the other 'by terminal', new pin, overwrite
        pin = svc.getPinByTerminal(1, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);

        assertEquals(1, pin.getIndex());
        assertNotEquals(pin1, pin);
        assertEquals(3, pins.size());

        // the other 'by terminal', entirely new pin
        pin = svc.getPinByTerminal(4, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN, KuraGPIOTrigger.NONE);

        assertEquals(4, pin.getIndex());
        assertEquals(4, pins.size());

        // deactivate
        svc.deactivate(null);
    }

    @Test
    public void testWhenAvailable() throws Throwable {
        GPIOServiceImpl svc = new GPIOServiceImpl();

        String path = null;
        String result = (String) TestUtil.invokePrivate(svc, "whenAvailable", path);

        assertNull(result);

        // bad path
        File f = new File("target/");
        path = f.getAbsolutePath();
        result = (String) TestUtil.invokePrivate(svc, "whenAvailable", path);

        assertNull(result);

        // not a file
        f = new File("target/");
        path = "///" + f.getAbsolutePath();
        result = (String) TestUtil.invokePrivate(svc, "whenAvailable", path);

        assertNull(result);

        // readable file
        f = new File("target/jdk.dio.properties");
        path = "///" + f.getAbsolutePath();
        f.createNewFile();
        result = (String) TestUtil.invokePrivate(svc, "whenAvailable", path);

        assertNotNull(result);
        assertEquals("file:" + path, result);

        f.delete();
    }

}
