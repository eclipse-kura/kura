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
import java.security.Policy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceDescriptor;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.uart.ModemUART;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import jdk.dio.uart.UARTPermission;

/**
 *
 * @test
 * @sources UARTConfigTest.java
 * @executeClass dio.uart.UARTConfigTest
 *
 * @title UART configuration testing
 *
 * @author stanislav.smirnov@oracle.com
 */
public class UARTConfigTest extends UARTTestBase implements Test {

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
        if(!decodeConfig(args)){
            return printFailedStatus("Error occured while decoding input arguments");
        }

        Policy.setPolicy(new UARTPolicy(
                new DeviceMgmtPermission("*:*", "open"),
                new UARTPermission("*:*", "open")));

        List<Integer> result = new LinkedList<>();

        result.add(testUARTConfig().getType());

        result.add(testUARTDesc().getType());

        result.add(testUARTDefault().getType());

        return (result.contains(Status.FAILED)
                ? printFailedStatus("some test cases failed") : Status.passed("OK"));
    }

    /**
     * Testcase to verify uart configuration against initial configuration
     * @return Status passed/failed
     */
    private Status testUARTConfig() {
        Status result = Status.passed(STATUS_OK);
        Status resultFail = null;
        start("Verify UART config against initial configuration");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try (UART device = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open())) {
                int baudRate = device.getBaudRate();
                int cfgBaudRate = devCfg.getBaudRate();
                if (baudRate == cfgBaudRate) {
                    System.out.println("Correct BAUD RATE received: " + baudRate);
                } else {
                    resultFail = printFailedStatus("Unexpected BAUD RATE returned: " + baudRate);
                }
                int dataBits = device.getDataBits();
                int cfgDataBits = devCfg.getDataBits();
                if (dataBits == cfgDataBits) {
                    System.out.println("Correct DATA BITS received: " + printDataBits(dataBits));
                } else {
                    resultFail = printFailedStatus("Unexpected DATA BITS returned: "  + printDataBits(dataBits));
                }
                int parity = device.getParity();
                int cfgParity = devCfg.getParity();
                if (parity == cfgParity) {
                    System.out.println("Correct PARITY received: " + printParity(parity));
                } else {
                    resultFail = printFailedStatus("Unexpected PARITY returned: "  + printParity(parity));
                }
                int stopBits = device.getStopBits();
                int cfgStopBits = devCfg.getStopBits();
                if (stopBits == cfgStopBits) {
                    System.out.println("Correct STOP BITS received: " + printStopBits(cfgStopBits));
                } else {
                    resultFail = printFailedStatus("Unexpected STOP BITS returned: "  + printStopBits(cfgStopBits));
                }
                System.out.println("UART was open/closed OK");
            } catch(IOException ex){
                resultFail = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        if (resultFail != null) {
            return resultFail;
        }
        return result;
    }

    /**
     * Testcase to verify uart configuration against initial configuration using DeviceDescriptor methods
     * @return Status passed/failed
     */
    private Status testUARTDesc() {
        Status result = Status.passed(STATUS_OK);
        Status resultFail = null;
        start("Verify UART config against initial configuration using DeviceDescriptor methods");
        Config initialConfig;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while(devicesList.hasNext()){
            initialConfig = devicesList.next();
            UARTConfig devCfg = initialConfig.getDevCfg();
            try (UART device = (initialConfig.isModemUART() ? (ModemUART) initialConfig.open() : (UART) initialConfig.open())) {
                DeviceDescriptor devDecriptor = (DeviceDescriptor) device.getDescriptor();
                UARTConfig devConfig = (UARTConfig) devDecriptor.getConfiguration();
                int controllerNumber = devConfig.getControllerNumber();
                int cfgControllerNumber = devCfg.getControllerNumber();
                if (controllerNumber == cfgControllerNumber) {
                    System.out.println("Device number is correct: " + controllerNumber);
                } else {
                    resultFail = printFailedStatus("Unexpected device number returned: " + controllerNumber);
                }
                String controllerName = devConfig.getControllerName();
                String cfgControllerName = devCfg.getControllerName();
                if (controllerName == null) {
                    if (cfgControllerName == null) {
                        System.out.println("Device name is null");
                    }
                } else if (controllerName.equals(cfgControllerName)) {
                    System.out.println("Device name is correct: " + controllerName);
                } else {
                    resultFail = printFailedStatus("Not expected Device name: " + controllerName);
                }
                int baudRate = devConfig.getBaudRate();
                int cfgBaudRate = devCfg.getBaudRate();
                if (baudRate == cfgBaudRate) {
                    System.out.println("Correct BAUD RATE received: " + baudRate);
                } else {
                    resultFail = printFailedStatus("Unexpected BAUD RATE returned: " + baudRate);
                }
                int dataBits = devConfig.getDataBits();
                int cfgDataBits = devCfg.getDataBits();
                if (dataBits == cfgDataBits) {
                    System.out.println("Correct DATA BITS received: " + printDataBits(dataBits));
                } else {
                    resultFail = printFailedStatus("Unexpected DATA BITS returned: "  + printDataBits(dataBits));
                }
                int parity = devConfig.getParity();
                int cfgParity = devCfg.getParity();
                if (parity == cfgParity) {
                    System.out.println("Correct PARITY received: " + printParity(parity));
                } else {
                    resultFail = printFailedStatus("Unexpected PARITY returned: "  + printParity(parity));
                }
                int stopBits = devConfig.getStopBits();
                int cfgStopBits = devCfg.getStopBits();
                if (stopBits == cfgStopBits) {
                    System.out.println("Correct STOP BITS received: " + printStopBits(cfgStopBits));
                } else {
                    resultFail = printFailedStatus("Unexpected STOP BITS returned: "  + printStopBits(cfgStopBits));
                }
                int flowControlMode = devConfig.getFlowControlMode();
                int cfgflowControlMode = devCfg.getFlowControlMode();
                if (flowControlMode == cfgflowControlMode) {
                    System.out.println("Correct FLOW CONTROL received: " + printFlowControl(devConfig.getFlowControlMode()));
                } else {
                    resultFail = printFailedStatus("Unexpected STOP BITS returned: " + printFlowControl(devConfig.getFlowControlMode()));
                }
            } catch(IOException ex){
                resultFail = printFailedStatus("Unexpected IOException: " + ex.getClass().getName() + ":" + ex.getMessage());
            }
        }
        stop();
        if (resultFail != null) {
            return resultFail;
        }
        return result;
    }

    /**
     * Testcase to verify uart configuration using DEFAULT parameters where it is possible
     * @return Status passed/failed
     */
    private Status testUARTDefault() {
        Status result = Status.passed(STATUS_OK);
        start("Creating UART configs using DEFAULT parameters where it is possible");
        Config dev;
        UARTConfig cfg;
        UARTConfig defCfg;
        Iterator<Config> devicesList = UARTDevices.iterator();
        while (devicesList.hasNext()) {
            dev = devicesList.next();
            try {
                cfg = dev.getDevCfg();
                defCfg = new UARTConfig(DeviceConfig.DEFAULT, DeviceConfig.DEFAULT, cfg.getBaudRate(), cfg.getDataBits(), cfg.getParity(), cfg.getStopBits(), cfg.getFlowControlMode());
                try (UART device = (dev.isModemUART() ? (ModemUART) DeviceManager.open(defCfg) : (UART) DeviceManager.open(defCfg))) {
                    System.out.println("New uart device was open/closed OK");
                }
            } catch (IOException e) {
                result = printFailedStatus("Unexpected IOException: " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        stop();
        return result;
    }
}
