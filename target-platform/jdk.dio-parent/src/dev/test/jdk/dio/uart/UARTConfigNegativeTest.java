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
import static dio.shared.TestBase.STATUS_OK;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.uart.ModemUART;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import jdk.dio.uart.UARTPermission;

/**
 *
 * @test
 * @sources UARTConfigNegativeTest.java
 * @executeClass dio.uart.UARTConfigNegativeTest
 *
 * @title UART negative configuration testing
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTConfigNegativeTest extends UARTTestBase implements Test {

    /**
     * Standard command-line entry point.
     *
     * @param args command line args (ignored)
     */
    public static void main(String[] args) {
        PrintWriter err = new PrintWriter(System.err, true);
        Test t = new UARTConfigTest();
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
                new DeviceMgmtPermission("*:*", "open"),
                new UARTPermission("*:*", "open")));

        List<Integer> result = new LinkedList<>();

        result.add(testUnsupportedUARTBaudRate().getType());

        result.add(testUnsupportedUARTDataBits().getType());

        result.add(testUnsupportedUARTParity().getType());

        result.add(testUnsupportedUARTStopBits().getType());

        result.add(testUnsupportedUARTFlowControl().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    /**
     * Testcase to verify that attempt to create config and open device using unsupported baud rate will be caught
     * @return Status passed/failed
     */
    private Status testUnsupportedUARTBaudRate() {
        Status result = Status.passed(STATUS_OK);
        start("Checking possibilty to create new UART with unsupported Baud Rates");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                Integer[] unsupportedBaudRates = getUnsupportedBaudRates(devCfg.getBaudRate());
                for (int unsupportedBaudRate : unsupportedBaudRates) {
                    UARTConfig uartConfig = new UARTConfig(
                            devCfg.getControllerNumber(),
                            devCfg.getChannelNumber(),
                            unsupportedBaudRate,
                            devCfg.getDataBits(),
                            devCfg.getParity(),
                            devCfg.getStopBits(),
                            devCfg.getFlowControlMode());

                    try (UART device = (initialConfig.isModemUART() ? (ModemUART) DeviceManager.open(uartConfig) : (UART) DeviceManager.open(uartConfig))) {
                        result = printFailedStatus("UART config was created with unsupported baud rate=" + unsupportedBaudRate + " FAILURE");
                    }
                }
            } catch (InvalidDeviceConfigException ex) {
                System.out.println("InvalidDeviceConfigException was caught as expected.");
            } catch (IllegalArgumentException ex) {
                System.out.println("IllegalArgumentException was caught as expected.");
            } catch (IOException ex) {
                result = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify that attempt to create config and open device using unsupported data bits will be caught
     * @return Status passed/failed
     */
    private Status testUnsupportedUARTDataBits() {
        Status result = Status.passed(STATUS_OK);
        start("Checking possibilty to create new UART with unsupported Data Bits");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                Integer[] unsupportedDataBits = getUnsupportedDataBits(devCfg.getDataBits());
                for (int unsupportedDataBit : unsupportedDataBits) {
                    UARTConfig uartConfig = new UARTConfig(
                            devCfg.getControllerNumber(),
                            devCfg.getChannelNumber(),
                            devCfg.getBaudRate(),
                            unsupportedDataBit,
                            devCfg.getParity(),
                            devCfg.getStopBits(),
                            devCfg.getFlowControlMode());
                    try (UART device = (initialConfig.isModemUART() ? (ModemUART) DeviceManager.open(uartConfig) : (UART) DeviceManager.open(uartConfig))) {
                        result = printFailedStatus("UART config was created with unsupported data bit=" + unsupportedDataBit + " FAILURE");
                    }
                }
            } catch (InvalidDeviceConfigException ex) {
                System.out.println("InvalidDeviceConfigException was caught as expected.");
            } catch (IllegalArgumentException ex) {
                System.out.println("IllegalArgumentException was caught as expected.");
            } catch (IOException ex) {
                result = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify that attempt to create config and open device using unsupported parities will be caught
     * @return Status passed/failed
     */
    private Status testUnsupportedUARTParity() {
        Status result = Status.passed(STATUS_OK);
        start("Checking possibilty to create new UART with unsupported Parities");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                Integer[] unsupportedParities = getUnsupportedParities(devCfg.getParity());
                for (int unsupportedParity : unsupportedParities) {
                    UARTConfig uartConfig = new UARTConfig(
                            devCfg.getControllerNumber(),
                            devCfg.getChannelNumber(),
                            devCfg.getBaudRate(),
                            devCfg.getDataBits(),
                            unsupportedParity,
                            devCfg.getStopBits(),
                            devCfg.getFlowControlMode());
                    try (UART device = (initialConfig.isModemUART() ? (ModemUART) DeviceManager.open(uartConfig) : (UART) DeviceManager.open(uartConfig))) {
                        result = printFailedStatus("UART config was created with unsupported parity=" + unsupportedParity + " FAILURE");
                    }
                }
            } catch (InvalidDeviceConfigException ex) {
                System.out.println("InvalidDeviceConfigException was caught as expected.");
            } catch (IllegalArgumentException ex) {
                System.out.println("IllegalArgumentException was caught as expected.");
            } catch (IOException ex) {
                result = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify that attempt to create config and open device using unsupported stop bits will be caught
     * @return Status passed/failed
     */
    private Status testUnsupportedUARTStopBits() {
        Status result = Status.passed(STATUS_OK);
        start("Checking possibilty to create new UART with unsupported Stop Bits");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                Integer[] unsupportedStopBits = getUnsupportedStopBits(devCfg.getStopBits());
                for (int unsupportedStopBit : unsupportedStopBits) {
                    UARTConfig uartConfig = new UARTConfig(
                            devCfg.getControllerNumber(),
                            devCfg.getChannelNumber(),
                            devCfg.getBaudRate(),
                            devCfg.getDataBits(),
                            devCfg.getParity(),
                            unsupportedStopBit,
                            devCfg.getFlowControlMode());
                    try (UART device = (initialConfig.isModemUART() ? (ModemUART) DeviceManager.open(uartConfig) : (UART) DeviceManager.open(uartConfig))) {
                        result = printFailedStatus("UART config was created with unsupported stop bit=" + unsupportedStopBit + " FAILURE");
                    }
                }
            } catch (InvalidDeviceConfigException ex) {
                System.out.println("InvalidDeviceConfigException was caught as expected.");
            } catch (IllegalArgumentException ex) {
                System.out.println("IllegalArgumentException was caught as expected.");
            } catch (IOException ex) {
                result = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    /**
     * Testcase to verify that attempt to create config and open device using unsupported flow control modes will be caught
     * @return Status passed/failed
     */
    private Status testUnsupportedUARTFlowControl() {
        Status result = Status.passed(STATUS_OK);
        start("Checking possibilty to create new UART with unsupported Flow Control Modes");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try {
                Integer[] unsupportedFlowControlModes = getUnsupportedFlowControlModes(devCfg.getFlowControlMode());
                for (int unsupportedFlowControlMode : unsupportedFlowControlModes) {
                    UARTConfig uartConfig = new UARTConfig(
                            devCfg.getControllerNumber(),
                            devCfg.getChannelNumber(),
                            devCfg.getBaudRate(),
                            devCfg.getDataBits(),
                            devCfg.getParity(),
                            devCfg.getStopBits(),
                            unsupportedFlowControlMode);
                    try (UART device = (initialConfig.isModemUART() ? (ModemUART) DeviceManager.open(uartConfig) : (UART) DeviceManager.open(uartConfig))) {
                        result = printFailedStatus("UART config was created with unsupported flow control mode=" + unsupportedFlowControlMode + " FAILURE");
                    }
                }
            } catch (InvalidDeviceConfigException ex) {
                System.out.println("InvalidDeviceConfigException was caught as expected.");
            } catch (IllegalArgumentException ex) {
                System.out.println("IllegalArgumentException was caught as expected.");
            } catch (IOException ex) {
                result = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        return result;
    }

    private Integer[] getUnsupportedBaudRates(int mode) {
        ArrayList unsupported = new ArrayList();
        unsupported.add(-100);
        unsupported.add(0);
        unsupported.add(Integer.MIN_VALUE);

        ArrayList<Integer> supported = getSupportedBaudRates();

        if (supported.contains(mode)) {
            supported.remove((Integer) mode);
        }

        supported.trimToSize();

        unsupported.addAll(supported);

        return (Integer[]) unsupported.toArray();
    }

    private Integer[] getUnsupportedDataBits(int mode) {
        ArrayList<Integer> unsupported = new ArrayList<>();

        unsupported.add(-100);
        unsupported.add(0);
        unsupported.add(Integer.MAX_VALUE);

        ArrayList<Integer> supported = getSupportedDataBits();

        if (supported.contains(mode)) {
            supported.remove((Integer) mode);
        }

        supported.trimToSize();

        unsupported.addAll(supported);
        return (Integer[]) unsupported.toArray();
    }

    private Integer[] getUnsupportedParities(int mode) {
        ArrayList<Integer> unsupported = new ArrayList<>();

        unsupported.add(-100);
        unsupported.add(+100);
        unsupported.add(Integer.MAX_VALUE);

        ArrayList<Integer> supported = getSupportedParities();

        if (supported.contains(mode)) {
            supported.remove((Integer) mode);
        }

        supported.trimToSize();

        unsupported.addAll(supported);
        return (Integer[]) unsupported.toArray();
    }

    private Integer[] getUnsupportedStopBits(int mode) {
        ArrayList<Integer> unsupported = new ArrayList<>();

        unsupported.add(-100);
        unsupported.add(0);
        unsupported.add(Integer.MAX_VALUE);

        ArrayList<Integer> supported = getSupportedStopBits();

        if (supported.contains(mode)) {
            supported.remove((Integer) mode);
        }

        supported.trimToSize();

        unsupported.addAll(supported);
        return (Integer[]) unsupported.toArray();
    }

    private Integer[] getUnsupportedFlowControlModes(int mode) {
        ArrayList<Integer> unsupported = new ArrayList<>();

        unsupported.add(-100);
        unsupported.add(+100);
        unsupported.add(Integer.MAX_VALUE);

        ArrayList<Integer> supported = getSupportedFlowControlModes();

        if (supported.contains(mode)) {
            supported.remove((Integer) mode);
        }

        supported.trimToSize();

        unsupported.addAll(supported);
        return (Integer[]) unsupported.toArray();
    }

    private boolean dataExchange(UART device) {
        boolean result = true;
        byte[] validArrayWrite = {32, 34, 36, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64};
        ByteBuffer ob = ByteBuffer.wrap(validArrayWrite);
        ByteBuffer ib = ByteBuffer.allocateDirect(validArrayWrite.length);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        System.out.println("Data exchange started ...");
        try {
            device.write(ob);
            System.out.println("Data were sent.");
            device.read(ib);
            System.out.println("Data were read: " + ib.capacity() + "bytes.");
        } catch (IOException e) {
            result = false;
            System.out.println("Some I/O error occured when reading/writing: " + e.getClass().getName() + ":" + e.getMessage());
        }
        System.out.println("Data exchange completed ...");
        return result;
    }
}
