/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package dio.gpio;

import com.sun.javatest.Status;
import com.sun.javatest.Test;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Policy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.*;
import jdk.dio.gpio.*;

/**
 *
 * @test
 * @sources GPIOPinConfigTest.java
 * @executeClass dio.gpio.GPIOPinConfigTest
 *
 * @title GPIO pin configuration testing
 *
 * @author stanislav.smirnov@oracle.com
 */
public class GPIOPinConfigTest extends GPIOTestBase implements Test {

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new GPIOPinConfigTest();
        Status s = t.run(args, null, err);
        s.exit();
    }

    /**
     * Main test method. The test consists of a series of test cases; the test
     * passes only if all the individual test cases pass.
     *
     * @param args ignored
     * @param out ignored
     * @param err a stream to which to write details about test failures
     * @return a Status object indicating if the test passed or failed
     */
    @Override
    public Status run(String[] args, PrintWriter out, PrintWriter err) {
        if(!decodeConfig(args, Modes.PIN)){
            return printFailedStatus("Error occured while decoding input arguments");
        }

        Policy.setPolicy(new GPIOPolicy(
                new DeviceMgmtPermission("*:*", "open"),
                new GPIOPinPermission("*:*", "open"),
                null));

        List<Integer> result = new LinkedList<>();
        result.add(testGPIOPinConfig().getType());

        result.add(testGPIOPinByDescriptor().getType());

        result.add(testGPIOPinDefault().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed(STATUS_OK));
    }

    /**
     * Testcase to verify GPIO pins configuration against initial configuration using GPIO methods
     * @return Status passed/failed
     */
    public Status testGPIOPinConfig() {
        Status result = Status.passed(STATUS_OK);
        Status resultFail = null;
        start("Verify GPIO pins config against initial configuration using GPIO methods");
        PinConfig initialConfig;
        GPIOPinConfig pinConfig;
        Iterator<PinConfig> devicesList = GPIOpins.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            pinConfig = initialConfig.getPinConfig();
            try (GPIOPin device = (GPIOPin) DeviceManager.open(GPIOPin.class, pinConfig)) {
                System.out.println("Checking GPIO pin : \"" + initialConfig.getName() + "\"");
                int direction = device.getDirection();
                int cfgDir = pinConfig.getDirection();
                if (direction == GPIOPin.INPUT) {
                    if ((cfgDir == GPIOPinConfig.DIR_INPUT_ONLY) | (cfgDir == GPIOPinConfig.DIR_BOTH_INIT_INPUT)) {
                        System.out.println("INPUT direction: " + getPinConfigDirection(cfgDir));

                    } else {
                        resultFail = printFailedStatus("Not expected direction: " + getPinConfigDirection(cfgDir));
                    }
                } else if (direction == GPIOPin.OUTPUT) {
                    if ((cfgDir == GPIOPinConfig.DIR_OUTPUT_ONLY) | (cfgDir == GPIOPinConfig.DIR_BOTH_INIT_OUTPUT)) {
                        System.out.println("OUTPUT direction: " + getPinConfigDirection(cfgDir));
                    } else {
                        resultFail = printFailedStatus("Not expected direction: " + getPinConfigDirection(cfgDir));
                    }
                } else {
                    resultFail = printFailedStatus("Unknown direction: " + direction);
                }
                int interrupttrigger = device.getTrigger();
                int cfgTrigger = pinConfig.getTrigger();
                if (interrupttrigger == cfgTrigger) {
                    System.out.println("Correct TRIGGER received");
                } else {
                    resultFail = printFailedStatus("Wrong Interrupt Trigger: " + interrupttrigger);
                }

                System.out.println("New pin was opened/closed OK");

            } catch (IOException e) {
                resultFail = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        if (resultFail != null) {
            return resultFail;
        }
        return result;
    }

    /**
     * Testcase to verify GPIO pins configuration against declared configuration using DeviceDescriptor methods
     * @return Status passed/failed
     */
    public Status testGPIOPinByDescriptor() {
        Status result = Status.passed(STATUS_OK);
        Status resultFail = null;
        start("Verify GPIO pins config against declared configuration using DeviceDescriptor methods");
        PinConfig initialConfig;
        GPIOPinConfig pinConfig;
        DeviceDescriptor deviceDecriptor;
        GPIOPinConfig deviceConfig;
        Iterator<PinConfig> devicesList = GPIOpins.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            pinConfig = initialConfig.getPinConfig();
            try (GPIOPin device = (GPIOPin) DeviceManager.open(GPIOPin.class, pinConfig)) {
                deviceDecriptor = (DeviceDescriptor) device.getDescriptor();
                deviceConfig = (GPIOPinConfig) deviceDecriptor.getConfiguration();
                System.out.println("Checking configuration of [" + deviceDecriptor.getName() + "]");
                if (deviceConfig.getPinNumber() == pinConfig.getPinNumber()) {
                    System.out.println("Pin number is correct: " + deviceConfig.getPinNumber());
                } else {
                    resultFail = printFailedStatus("Not expected pin number: " + deviceConfig.getPinNumber());
                }
                if (deviceConfig.getControllerNumber() == pinConfig.getControllerNumber()) {
                    System.out.println("Device number is correct: " + deviceConfig.getControllerNumber());
                } else {
                    resultFail = printFailedStatus("Not expected Device number: " + deviceConfig.getControllerNumber());
                }
                if (deviceConfig.getDirection() == pinConfig.getDirection()) {
                    System.out.println("Direction is correct: " + getPinConfigDirection(deviceConfig.getDirection()));
                } else {
                    resultFail = printFailedStatus("Not expected direction: " + getPinConfigDirection(deviceConfig.getDirection()));
                }
                if (deviceConfig.getDriveMode() == pinConfig.getDriveMode()) {
                    System.out.println("Drive mode is correct: " + getPinDriveMode(deviceConfig.getDriveMode()));
                } else {
                    resultFail = printFailedStatus("Not expected drive mode: " + getPinDriveMode(deviceConfig.getDriveMode()));
                }
                if (deviceConfig.getTrigger() == pinConfig.getTrigger()) {
                    System.out.println("Interrupt trigger is correct: " + getPinConfigTrigger(deviceConfig.getTrigger()));
                } else {
                    resultFail = printFailedStatus("Not expected Interrupt trigger: " + getPinConfigTrigger(deviceConfig.getTrigger()));
                }

                System.out.println("Pin was checked OK");

            } catch (IOException e) {
                resultFail = printFailedStatus("An I/O error occured " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        if (resultFail != null) {
            return resultFail;
        }
        return result;
    }

    /**
     * Testcase to verify GPIO pins configuration using DEFAULT parameters where it is possible
     * @return Status passed/failed
     */
    public Status testGPIOPinDefault() {
        Status result = Status.passed(STATUS_OK);
        Status resultFail = null;
        start("Creating GPIO pins config using DEFAULT parameters where it is possible");
        GPIOPin myDev;
        PinConfig dev;
        GPIOPinConfig cfg;
        GPIOPinConfig defCfg;
        Iterator<PinConfig> devicesList = GPIOpins.iterator();
        while (devicesList.hasNext()) {
            dev = devicesList.next();
            try {
                cfg = dev.getPinConfig();
                defCfg = new GPIOPinConfig(DeviceConfig.DEFAULT, DeviceConfig.DEFAULT, cfg.getDirection(),DeviceConfig.DEFAULT,cfg.getTrigger(),true);
                myDev = (GPIOPin) DeviceManager.open(defCfg);
                myDev.close();
                System.out.println("New pin was open/closed OK");
            } catch (IOException e) {
                resultFail = printFailedStatus("Unexpected Throwable: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        if (resultFail != null) {
            return resultFail;
        }
        return result;
    }
}