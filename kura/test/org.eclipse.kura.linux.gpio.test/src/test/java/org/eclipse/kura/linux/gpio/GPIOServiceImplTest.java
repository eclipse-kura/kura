/*******************************************************************************
 * Copyright (c) 2017, 2024 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.linux.gpio;

import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class GPIOServiceImplTest {

    private Path jdkProperties;
    private Path digitalInSymlink;
    private Path digitalOutSymlink;
    private Path digitalIn;
    private Path digitalOut;

    @Test
    public void testActivateDeactivatePinSearch() throws IOException, NoSuchFieldException {
        GPIOServiceImpl svc = new GPIOServiceImpl();

        String led1 = "user led 1";
        String testpin1 = "testpin1";
        String content = "1=name:" + led1 + ",deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0\n" +
                "2=name:" + testpin1 + ",deviceType:gpio.GPIOPin,direction:1,mode:8,trigger:0";

        createJdkDioPropertiesFile(content);

        svc.activate(null);

        deleteJdkDioPropertiesFile();

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

    @Test
    public void shouldParsePinSymlinks() throws IOException, NoSuchFieldException {
        GPIOServiceImpl svc = new GPIOServiceImpl();

        String content = "target/digital_in_symlink=name:IN1,deviceType:gpio.GPIOPin,direction:0,mode:1,trigger:0\n"
                + "target/digital_out_symlink=name:OUT1,deviceType:gpio.GPIOPin,direction:1,mode:4,trigger:0";

        createJdkDioPropertiesFile(content);
        createGpioFiles();
        createGpioSymlinks();

        svc.activate(null);

        deleteGpioSymlinks();
        deleteGpioFiles();
        deleteJdkDioPropertiesFile();

        Set<JdkDioPin> pins = (Set) TestUtil.getFieldValue(svc, "pins");
        assertEquals(2, pins.size());

        pins.iterator().forEachRemaining(pin -> {
            assertTrue(111 == pin.getIndex() || 123 == pin.getIndex());
            if (pin.getIndex() == 111) {
                assertEquals("IN1", pin.getName());
                assertEquals(KuraGPIODirection.INPUT, pin.getDirection());
                assertEquals(KuraGPIOMode.INPUT_PULL_UP, pin.getMode());
                assertEquals(KuraGPIOTrigger.NONE, pin.getTrigger());
            } else if (pin.getIndex() == 123) {
                assertEquals("OUT1", pin.getName());
                assertEquals(KuraGPIODirection.OUTPUT, pin.getDirection());
                assertEquals(KuraGPIOMode.OUTPUT_PUSH_PULL, pin.getMode());
                assertEquals(KuraGPIOTrigger.NONE, pin.getTrigger());
            }
        });
        svc.deactivate(null);
    }

    private void createJdkDioPropertiesFile(String content) throws IOException {
        this.jdkProperties = Paths.get("target/jdk.dio.properties");
        String dioConfigFileName = this.jdkProperties.toAbsolutePath().toString();
        System.setProperty("jdk.dio.registry", "file:///" + dioConfigFileName);

        Files.write(this.jdkProperties, content.getBytes());
    }

    private void deleteJdkDioPropertiesFile() throws IOException {
        Files.delete(this.jdkProperties);
    }

    private void createGpioFiles() throws IOException {
        this.digitalIn = Paths.get("target/gpio111");
        this.digitalOut = Paths.get("target/gpio123");

        Files.createFile(this.digitalIn);
        Files.createFile(this.digitalOut);
    }

    private void deleteGpioFiles() throws IOException {
       Files.delete(this.digitalIn);
       Files.delete(this.digitalOut);
    }

    private void createGpioSymlinks() throws IOException {
        this.digitalInSymlink = Paths.get("target/digital_in_symlink");
        this.digitalOutSymlink = Paths.get("target/digital_out_symlink");

        Files.createSymbolicLink(this.digitalInSymlink, this.digitalIn.toRealPath());
        Files.createSymbolicLink(this.digitalOutSymlink, this.digitalOut.toRealPath());
    }

    private void deleteGpioSymlinks() throws IOException {
        Files.delete(this.digitalInSymlink);
        Files.delete(this.digitalOutSymlink);
    }
}
