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
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPort;
import jdk.dio.gpio.GPIOPortPermission;

/**
 * @test
 * @executeClass dio.gpio.GPIOPortDirectionsTest
 * @source GPIOPortDirectionsTest.java
 * @title Test of GPIOPort directions
 * @author stanislav.smirnov@oracle.com
 */
public class GPIOPortDirectionsTest extends GPIOTestBase implements Test {

    /**
     * Unexisting GPIOPort direction
     */
    private final int unexistingDirection = 222;

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new GPIOPortDirectionsTest();
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
        if(!decodeConfig(args, Modes.PORT)){
            return printFailedStatus("Error occured while decoding input arguments");
        }

        Policy.setPolicy(new GPIOPolicy(
                new DeviceMgmtPermission("*:*", "open"),
                null,
                new GPIOPortPermission("*:*", "open,setdirection")));

        List<Integer> result = new LinkedList<>();
        result.add(testGetPortDirection().getType());

        result.add(testSetPortDirection().getType());

        result.add(testSetWrongPortDirection().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed(STATUS_OK));
    }

    /**
     * Testcase to verify port getDirection method
     * @return Status passed/failed
     */
    private Status testGetPortDirection() {
        Status result = Status.passed(STATUS_OK);
        start("Getting directions of all GPIO ports");
        PortConfig portConfig;
        Iterator<PortConfig> devicesList = GPIOports.iterator();
        while (devicesList.hasNext()) {
            portConfig = devicesList.next();
            System.out.println("Checking direction of GPIO port \"" + portConfig.getName() + "\"");
            try (GPIOPort port = portConfig.open()) {
                System.out.println("Trying to get direction ...");
                int direction = port.getDirection();
                switch (direction) {
                    case GPIOPort.INPUT: {
                        System.out.println("INPUT direction");
                        break;
                    }
                    case GPIOPort.OUTPUT: {
                        System.out.println("OUPUT direction");
                        break;
                    }
                    default: {
                        result = printFailedStatus("Unknown direction: " + direction);
                    }
                }
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify ability to change port direction using setDirection method
     * @return Status passed/failed
     */
    private Status testSetPortDirection() {
        Status result = Status.passed(STATUS_OK);
        start("Setting directions of all GPIO ports");
        PortConfig portConfig;
        Iterator<PortConfig> devicesList = GPIOports.iterator();
        while (devicesList.hasNext()) {
            portConfig = devicesList.next();
            System.out.println("Trying to set OUTPUT direction of GPIO port \"" + portConfig.getName() + "\"");
            try (GPIOPort port = portConfig.open()) {
                try {
                    port.setDirection(GPIOPin.OUTPUT);
                    System.out.println("Direction was changed ...");
                    if (port.getDirection() != GPIOPin.OUTPUT) {
                        result = printFailedStatus("Direction after change: " + getPinDirection(port.getDirection()));
                    } else {
                        System.out.println("Direction was changed to: " + getPinDirection(port.getDirection()));
                    }
                } catch (UnsupportedOperationException e) {
                    result = printFailedStatus("UnsupportedOperationException during changing direction: " + e.getClass().getName() + ":" + e.getMessage());
                }
                System.out.println("Trying to set INPUT direction of GPIO port \"" + portConfig.getName() + "\"");
                try {
                    port.setDirection(GPIOPort.INPUT);
                    System.out.println("Direction was changed ...");
                    if (port.getDirection() != GPIOPort.INPUT) {
                        result = printFailedStatus("Direction after change: " + getPinDirection(port.getDirection()));
                    } else {
                        System.out.println("Direction was changed to: " + getPinDirection(port.getDirection()));
                    }
                } catch (UnsupportedOperationException e) {
                    result = printFailedStatus("UnsupportedOperationException during changing direction: " + e.getClass().getName() + ":" + e.getMessage());
                }
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify that wrong port direction use will be caught
     * @return Status passed/failed
     */
    private Status testSetWrongPortDirection() {
        Status result = Status.passed(STATUS_OK);
        start("Trying to set unexisting direction");
        PortConfig portConfig;
        Iterator<PortConfig> devicesList = GPIOports.iterator();
        while (devicesList.hasNext()) {
            portConfig = devicesList.next();
            try (GPIOPort port = portConfig.open()) {
                try {
                    port.setDirection(unexistingDirection);
                    result = printFailedStatus("Direction was changed unexpectedly");
                } catch (IllegalArgumentException e) {
                    System.out.println("Expected IllegalArgumentException was thrown: " + e.getClass().getName() + ":" + e.getMessage());
                } catch (IOException e) {
                    result = printFailedStatus("IOException was thrown: " + e.getClass().getName() + ":" + e.getMessage());
                }
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }

        stop();
        return result;
    }

}
