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
import java.util.ArrayList;
import java.util.Iterator;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.GPIOPinPermission;

/**
 *
 * @test
 * @sources GPIOPinNegativeTest.java
 * @executeClass dio.gpio.GPIOPinNegativeTest
 *
 * @title Trying to configure all pins in all possible ways mostly negative
 *
 * @author stanislav.smirnov@oracle.com
 */
public class GPIOPinNegativeTest extends GPIOTestBase implements Test {

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new GPIOPinNegativeTest();
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

        return (testUnsupportedDirection().getType() == Status.FAILED
                ? printFailedStatus("some test cases failed") : Status.passed(STATUS_OK));
    }

    /**
     * Testcase to verify that unsupported directions and modes will be caught
     * @return Status passed/failed
     */
    public Status testUnsupportedDirection() {
        Status result = Status.passed(STATUS_OK);
        start("Checking possibilty to create new GPIO pin with invalid Mode");
        GPIOPin device;
        PinConfig initialConfig = GPIOpins.get(0);
        GPIOPinConfig pinConfig = initialConfig.getPinConfig();
        GPIOPinConfig newConfig;
        int mode;
        Integer[] directions = getUnsupportedDirections();
        if (directions.length < 1) {
            stop();
            System.out.println("All directions are supported");
            return Status.passed(STATUS_OK);
        }
        for (Integer unsupported : directions) {
            try {
                switch (unsupported) {
                    case GPIOPinConfig.DIR_INPUT_ONLY:
                        mode = GPIOPinConfig.MODE_INPUT_PULL_DOWN;
                        break;
                    case GPIOPinConfig.DIR_OUTPUT_ONLY:
                        mode = GPIOPinConfig.MODE_OUTPUT_PUSH_PULL;
                        break;
                    case GPIOPinConfig.DIR_BOTH_INIT_INPUT:
                        mode = (GPIOPinConfig.MODE_INPUT_PULL_DOWN) | (GPIOPinConfig.MODE_OUTPUT_PUSH_PULL);
                        break;
                    case GPIOPinConfig.DIR_BOTH_INIT_OUTPUT:
                        mode = (GPIOPinConfig.MODE_INPUT_PULL_DOWN) | (GPIOPinConfig.MODE_OUTPUT_PUSH_PULL);
                        break;
                    default:
                        mode = DeviceConfig.DEFAULT;
                }
                System.out.println("Opening in unsupported direction: " + getPinConfigDirection(unsupported) + " mode: " + getPinDriveMode(mode));
                newConfig = new GPIOPinConfig(pinConfig.getControllerNumber(), pinConfig.getPinNumber(), unsupported, mode, pinConfig.getTrigger(), true);
                System.out.println("New GPIO pin configuration created.");
                device = (GPIOPin) DeviceManager.open(GPIOPin.class, newConfig);
                device.close();
                result = printFailedStatus("Able to open/close invalid pin configuration");
            } catch (InvalidDeviceConfigException e) {
                System.out.println("Expected InvalidDeviceConfigException was thrown: " + e.getClass().getName() + ":" + e.getMessage());
            } catch (IllegalArgumentException e) {
                result = printFailedStatus("Unexpected IllegalArgumentException: " + e.getClass().getName() + ":" + e.getMessage());
            } catch (DeviceNotFoundException e) {
                result = printFailedStatus("Unexpected DeviceNotFoundException: " + e.getClass().getName() + ":" + e.getMessage());
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }

        stop();
        return result;
    }

    private Integer[] getUnsupportedDirections() {
        ArrayList<Integer> supported = new ArrayList<>();
        supported.add(GPIOPinConfig.DIR_BOTH_INIT_INPUT);
        supported.add(GPIOPinConfig.DIR_BOTH_INIT_OUTPUT);
        supported.add(GPIOPinConfig.DIR_INPUT_ONLY);
        supported.add(GPIOPinConfig.DIR_OUTPUT_ONLY);
        PinConfig deviceConfig;
        GPIOPinConfig pinConfig;
        Integer mode;
        Iterator<PinConfig> devicesList = GPIOpins.iterator();
        while (devicesList.hasNext()) {
            deviceConfig = devicesList.next();
            pinConfig = deviceConfig.getPinConfig();
            mode = pinConfig.getDirection();
            if (supported.contains(mode)) {
                supported.remove(mode);
            }
        }
        supported.trimToSize();
        Integer[] unsupported = new Integer[supported.size()];
        return supported.toArray(unsupported);
    }

}
