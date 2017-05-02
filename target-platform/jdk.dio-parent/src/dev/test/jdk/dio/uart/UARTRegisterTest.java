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

package dio.uart;

import com.sun.javatest.Status;
import com.sun.javatest.Test;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.DeviceAlreadyExistsException;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import jdk.dio.uart.UARTPermission;

/**
 *
 * @test
 * @sources UARTRegisterTest.java
 * @executeClass dio.uart.UARTRegisterTest
 *
 * @title UART new device registration
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTRegisterTest extends UARTTestBase implements Test {

    private static int freeID = 78000;

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new UARTRegisterTest();
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
        if (!decodeConfig(args)) {
            return printFailedStatus("Error occured while decoding input arguments");
        }

        Policy.setPolicy(new UARTPolicy(
                new DeviceMgmtPermission("*:*", "open,register,unregister"),
                new UARTPermission("*:*", "open")));

        List<Integer> result = new LinkedList<>();

        result.add(testUARTConfig().getType());

        result.add(testUARTSystemDefined().getType());

        result.add(testUARTFixedId().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    /**
     * Testcase to verify possibility to open all UART devices without registering
     * @return Status passed/failed
     */
    private Status testUARTConfig() {
        Status result = Status.passed("OK");
        start("Checking possibility to open all UART devices without registering");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try (UART uart = (UART) DeviceManager.open(devCfg)) {
                System.out.println("New UART was open/closed OK");
            } catch (InvalidDeviceConfigException ex) {
                result = printFailedStatus("UART configuration is not valid/supported.");
            } catch (DeviceAlreadyExistsException ex) {
                result = printFailedStatus("ID is already assigned to a Device device.");
            } catch (IOException ex) {
                result = Status.failed("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify possibility to create all UART devices from config and register with system defined ID
     * @return Status passed/failed
     */
    private Status testUARTSystemDefined() {
        Status result = Status.passed("OK");
        start("Checking possibility to create all UART devices from config and register with system defined ID");
        int i = 0;
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        ArrayList<Integer> registeredDevices = new ArrayList<>();
        int newID;
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                i++;
                //example: UARTConfig devCfg = new UARTConfig("ttyAMA0", 0, 19200, 8, 0, 1, 0);
                newID = DeviceManager.register(DeviceManager.UNSPECIFIED_ID, UART.class, devCfg, "virtualCounter" + i);
                System.out.println("New UART registered: " + newID);
                try(UART uart = DeviceManager.open(newID)){
                    System.out.println("New UART was open/closed OK");
                    registeredDevices.add(newID);
                }
            } catch (UnavailableDeviceException e) {
                result = printFailedStatus("UART in not currently available.");
            } catch (InvalidDeviceConfigException e) {
                result = printFailedStatus("UART configuration is not valid/supported.");
            } catch (DeviceAlreadyExistsException e) {
                result = printFailedStatus("ID is already assigned to a Device device.");
            } catch (IOException ex) {
                result = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }

        Iterator<Integer> regDevicesList = registeredDevices.iterator();
        while (regDevicesList.hasNext()) {
            i = regDevicesList.next();
            try {
                DeviceManager.unregister(i);
                System.out.println("New UART was unregistered OK: " + i);
            } catch (IllegalArgumentException ex) {
                result = printFailedStatus("Unexpected IllegalArgumentException: " + ex.getClass().getName() + ":" + ex.getMessage());
            } catch (SecurityException ex) {
                result = printFailedStatus("Unexpected SecurityException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify possibility to open all UART devices by config and register with designated ID
     * @return Status passed/failed
     */
    private Status testUARTFixedId() {
        Status result = Status.passed("OK");
        start("Checking possibilty to open all UART devices by config and register with designated ID");
        Config initialConfig;
        int i = 0;
        Iterator<Config> devicesList = UARTDevices.iterator();
        int newID;
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                i++;
                freeID++;
                //example: UARTConfig devCfg = new UARTConfig("ttyAMA0", 0, 19200, 8, 0, 1, 0);
                newID = DeviceManager.register(freeID, UART.class, devCfg, "virtualUART" + i);
                System.out.println("New UART was created: " + newID);
                try(UART uart = DeviceManager.open(newID)){
                    System.out.println("New UART was open/closed OK");
                }
                DeviceManager.unregister(newID);
                System.out.println("New UART was unregistered OK.");
            } catch (DeviceNotFoundException ex) {
                result = printFailedStatus("Designated UART not found.");
            } catch (UnavailableDeviceException ex) {
                result = printFailedStatus("UART in not currently available.");
            } catch (InvalidDeviceConfigException e) {
                result = printFailedStatus("UART configuration is not valid/supported.");
            } catch (DeviceAlreadyExistsException e) {
                result = printFailedStatus("ID is already assigned to a Device device.");
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        return result;
    }
}
